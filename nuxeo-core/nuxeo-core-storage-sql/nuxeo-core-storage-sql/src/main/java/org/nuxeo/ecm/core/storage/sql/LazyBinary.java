/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

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

/**
 * Base class for a lazy Binary that fetches its remote stream on first access.
 * <p>
 * The methods {@link #fetchFile} and {@link #fetchLength} must be implemented
 * by the concrete class.
 */
public abstract class LazyBinary extends Binary {

    private static final Log log = LogFactory.getLog(LazyBinary.class);

    private static final long serialVersionUID = 1L;

    protected static final String LEN_DIGEST_SUFFIX = "-len";

    protected final FileCache fileCache;

    protected boolean hasLength;

    /**
     * A lazy binary for the given digest.
     *
     * @param digest the digest for the binary
     * @param fileCache a file cache used by the length-caching methods
     */
    public LazyBinary(String digest, FileCache fileCache) {
        super(digest);
        this.fileCache = fileCache;
    }

    @Override
    public InputStream getStream() throws IOException {
        if (file == null) {
            file = fileCache.getFile(digest);
            if (file == null) {
                File tmp = fileCache.getTempFile();
                if (fetchFile(tmp)) {
                    file = fileCache.putFile(digest, tmp);
                } else {
                    tmp.delete();
                }
            }
            if (file != null) {
                length = file.length();
                hasLength = true;
            }
        }
        return file == null ? null : new FileInputStream(file);
    }

    /**
     * Fetches the file from the cache or the remote database.
     * <p>
     * If something is retrieved from the database, it is put in cache.
     *
     * @param tmp the temporary file to use to store the file
     * @return {@code true} if a file was fetched, {@code false} if the expected
     *         blob is missing from the database.
     */
    protected abstract boolean fetchFile(File tmp);

    @Override
    public long getLength() {
        if (!hasLength) {
            Long len = getLengthFromCache();
            if (len == null) {
                len = fetchLength();
                if (len != null) {
                    putLengthInCache(len);
                }
            }
            length = len == null ? 0 : len.longValue();
            hasLength = true;
        }
        return length;
    }

    /**
     * Fetches the length from the remote database.
     *
     * @return the length, or {@code null} if the expected blob is missing from
     *         the database.
     */
    protected abstract Long fetchLength();

    /**
     * Gets the length for the digest from the cache.
     *
     * @return the length, or {@code null} if not in cache
     */
    protected Long getLengthFromCache() {
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
        } catch (Exception e) {
            log.error(e, e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e, e);
                }
            }
        }
    }

    /**
     * Puts the length for the digest in the cache.
     *
     * @param len the length
     */
    protected void putLengthInCache(Long len) {
        OutputStream out = null;
        try {
            File tmp = fileCache.getTempFile();
            out = new FileOutputStream(tmp);
            Writer writer = new OutputStreamWriter(out);
            writer.write(len.toString());
            writer.flush();
            out.close();
            out = null;
            fileCache.putFile(digest + LEN_DIGEST_SUFFIX, tmp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e, e);
                }
            }
        }
    }
}
