/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.file.FileCache;
import org.nuxeo.common.file.LRUFileCache;
import org.nuxeo.common.utils.SizeUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class for a {@link BinaryManager} that uses a cache for its files
 * because fetching them is expensive.
 * <p>
 * Initialization of the {@link BinaryManager} must call
 * {@link #initializeCache} from the {@link #initialize} method.
 *
 * @since 5.7
 */
public abstract class CachingBinaryManager extends AbstractBinaryManager {

    private static final Log log = LogFactory.getLog(CachingBinaryManager.class);

    protected static final String LEN_DIGEST_SUFFIX = "-len";

    public FileCache fileCache;

    protected FileStorage fileStorage;

    @Override
    public void initialize(BinaryManagerDescriptor binaryManagerDescriptor)
            throws IOException {
        repositoryName = binaryManagerDescriptor.repositoryName;
        descriptor = new BinaryManagerRootDescriptor();
        descriptor.digest = getDigest();
        log.info("Repository '" + repositoryName + "' using "
                + getClass().getSimpleName());
    }

    @Override
    public void close() {
        fileCache.clear();
    }

    /**
     * Initialize the cache.
     *
     * @param dir the directory to use to store cached files
     * @param maxSize the maximum size of the cache (in bytes)
     * @param fileStorage the file storage mechanism to use to store and fetch
     *            files
     *
     * @since 5.9.2
     */
    public void initializeCache(File dir, long maxSize,
            @SuppressWarnings("hiding") FileStorage fileStorage) {
        fileCache = new LRUFileCache(dir, maxSize);
        this.fileStorage = fileStorage;
    }

    /**
     * Initialize the cache.
     *
     * @param cacheSizeStr the maximum size of the cache (as a String)
     * @param fileStorage the file storage mechanism to use to store and fetch
     *            files
     * @since 5.9.6
     * @see #initializeCache(File, long, FileStorage)
     * @see SizeUtils#parseSizeInBytes(String)
     */
    public void initializeCache(String cacheSizeStr,
            @SuppressWarnings("hiding") FileStorage fileStorage)
            throws IOException {
        File dir = File.createTempFile("nxbincache.", "", null);
        dir.delete();
        dir.mkdir();
        Framework.trackFile(dir, dir);
        long cacheSize = SizeUtils.parseSizeInBytes(cacheSizeStr);
        initializeCache(dir, cacheSize, fileStorage);
        log.info("Using binary cache directory: " + dir.getPath() + " size: "
                + cacheSizeStr);
    }

    @Override
    public Binary getBinary(InputStream in) throws IOException {
        // write the input stream to a temporary file, while computing a digest
        File tmp = fileCache.getTempFile();
        OutputStream out = new FileOutputStream(tmp);
        String digest;
        try {
            digest = storeAndDigest(in, out);
        } finally {
            in.close();
            out.close();
        }

        File cachedFile = fileCache.getFile(digest);
        if (cachedFile != null) {
            // file already in cache
            if (Framework.isTestModeSet()) {
                Framework.getProperties().setProperty("cachedBinary", digest);
            }
            // delete tmp file, not needed anymore
            tmp.delete();
            return new Binary(cachedFile, digest, repositoryName);
        }

        // send the file to storage
        fileStorage.storeFile(digest, tmp);

        // register the file in the file cache if all went well
        File file = fileCache.putFile(digest, tmp);

        return new Binary(file, digest, repositoryName);
    }

    @Override
    public Binary getBinary(String digest) {
        // Check in the cache
        File file = fileCache.getFile(digest);
        if (file == null) {
            return new LazyBinary(digest, repositoryName, this);
        } else {
            return new Binary(file, digest, repositoryName);
        }
    }

    /* =============== Methods used by LazyBinary =============== */

    /**
     * Gets a file from cache or storage.
     * <p>
     * Used by {@link LazyBinary}.
     */
    public File getFile(String digest) throws IOException {
        // get file from cache
        File file = fileCache.getFile(digest);
        if (file != null) {
            return file;
        }
        // fetch file from storage
        File tmp = fileCache.getTempFile();
        if (fileStorage.fetchFile(digest, tmp)) {
            // put file in cache
            file = fileCache.putFile(digest, tmp);
            return file;
        } else {
            // file not in storage
            tmp.delete();
            return null;
        }
    }

    /**
     * Gets a file length from cache or storage.
     * <p>
     * Use by {@link LazyBinary}.
     */
    public Long getLength(String digest) throws IOException {
        // get length from cache
        Long length = getLengthFromCache(digest);
        if (length != null) {
            return length;
        }
        // fetch length from storage
        length = fileStorage.fetchLength(digest);
        // put length in cache
        putLengthInCache(digest, length);
        return length;
    }

    protected Long getLengthFromCache(String digest) throws IOException {
        File f = fileCache.getFile(digest + LEN_DIGEST_SUFFIX);
        if (f == null) {
            return null;
        }
        // read decimal length from file
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            String len = IOUtils.toString(in);
            return Long.valueOf(len);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid length in " + f, e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    protected void putLengthInCache(String digest, Long len) throws IOException {
        // write decimal length in file
        OutputStream out = null;
        try {
            File tmp = fileCache.getTempFile();
            out = new FileOutputStream(tmp);
            Writer writer = new OutputStreamWriter(out);
            writer.write(len.toString());
            writer.flush();
            writer.close();
            fileCache.putFile(digest + LEN_DIGEST_SUFFIX, tmp);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

}
