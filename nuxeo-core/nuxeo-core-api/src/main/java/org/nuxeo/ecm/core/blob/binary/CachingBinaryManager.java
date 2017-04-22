/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob.binary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.file.FileCache;
import org.nuxeo.common.file.LRUFileCache;
import org.nuxeo.common.utils.SizeUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.trackers.files.FileEventTracker;

/**
 * Abstract class for a {@link BinaryManager} that uses a cache for its files because fetching them is expensive.
 * <p>
 * Initialization of the {@link BinaryManager} must call {@link #initializeCache} from the {@link #initialize} method.
 *
 * @since 5.7
 */
public abstract class CachingBinaryManager extends AbstractBinaryManager {

    private static final Log log = LogFactory.getLog(CachingBinaryManager.class);

    protected File cachedir;

    public FileCache fileCache;

    protected FileStorage fileStorage;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        descriptor = new BinaryManagerRootDescriptor();
        descriptor.digest = getDefaultDigestAlgorithm();
        log.info("Registering binary manager '" + blobProviderId + "' using " + getClass().getSimpleName());
    }

    /**
     * Initialize the cache.
     *
     * @param dir the directory to use to store cached files
     * @param maxSize the maximum size of the cache (in bytes)
     * @param maxCount the maximum number of files in the cache
     * @param minAge the minimum age of a file in the cache to be eligible for removal (in seconds)
     * @param fileStorage the file storage mechanism to use to store and fetch files
     * @since 5.9.2
     */
    protected void initializeCache(File dir, long maxSize, long maxCount, long minAge, FileStorage fileStorage) {
        fileCache = new LRUFileCache(dir, maxSize, maxCount, minAge);
        this.fileStorage = fileStorage;
    }

    /**
     * Initialize the cache.
     *
     * @param maxSizeStr the maximum size of the cache (as a String)
     * @param fileStorage the file storage mechanism to use to store and fetch files
     * @see #initializeCache(String, String, String, FileStorage)
     * @since 6.0
     */
    public void initializeCache(String maxSizeStr, FileStorage fileStorage) throws IOException {
        String maxCountStr = "10000"; // default for legacy code
        String minAgeStr = "3600"; // default for legacy code
        initializeCache(maxSizeStr, maxCountStr, minAgeStr, fileStorage);
    }

    /**
     * Initializes the cache.
     *
     * @param maxSizeStr the maximum size of the cache (as a String)
     * @param maxCountStr the maximum number of files in the cache
     * @param minAgeStr the minimum age of a file in the cache to be eligible for removal (in seconds)
     * @param fileStorage the file storage mechanism to use to store and fetch files
     * @see SizeUtils#parseSizeInBytes(String)
     * @since 7.10-HF03, 8.1
     */
    public void initializeCache(String maxSizeStr, String maxCountStr, String minAgeStr, FileStorage fileStorage)
            throws IOException {
        cachedir = Framework.createTempFile("nxbincache.", "");
        cachedir.delete();
        cachedir.mkdir();
        long maxSize = SizeUtils.parseSizeInBytes(maxSizeStr);
        long maxCount = Long.parseLong(maxCountStr);
        long minAge = Long.parseLong(minAgeStr);
        initializeCache(cachedir, maxSize, maxCount, minAge, fileStorage);
        log.info("Using binary cache directory: " + cachedir.getPath() + " size: " + maxSizeStr + " maxCount: "
                + maxCount + " minAge: " + minAge);

        // be sure FileTracker won't steal our files !
        FileEventTracker.registerProtectedPath(cachedir.getAbsolutePath());
    }

    @Override
    public void close() {
        fileCache.clear();
        if (cachedir != null) {
            try {
                FileUtils.deleteDirectory(cachedir);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
    }

    @Override
    protected Binary getBinary(InputStream in) throws IOException {
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
        } else {
            // send the file to storage
            fileStorage.storeFile(digest, tmp);
            // register the file in the file cache
            fileCache.putFile(digest, tmp);
        }
        return getBinary(digest);
    }

    @Override
    public Binary getBinary(String digest) {
        return new LazyBinary(digest, blobProviderId, this);
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

}
