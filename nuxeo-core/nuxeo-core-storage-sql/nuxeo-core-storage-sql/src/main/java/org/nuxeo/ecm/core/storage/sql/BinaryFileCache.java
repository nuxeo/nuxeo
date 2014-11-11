/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
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
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.file.LRUFileCache;

/**
 * @author "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
 *
 */
public abstract class BinaryFileCache extends LRUFileCache {

    protected static final String LEN_DIGEST_SUFFIX = "-len";

    public BinaryFileCache(File dir, long maxSize) {
        super(dir, maxSize);
    }

    /**
     * Fetches the file from the cache or the remote database.
     * <p>
     * If something is retrieved from the database, it is put in cache.
     *
     * @param key identify the file in cache
     * @param tmp the temporary file to use to store the file
     * @return {@code true} if a file was fetched, {@code false} if the expected
     *         blob is missing from the database.
     */
    public abstract boolean fetchFile(String key, File tmp);

    /**
     * Fetches the length from the remote database.
     *
     * @param key identify the file in cache
     * @return the length, or {@code null} if the expected blob is missing from
     *         the database.
     */
    public abstract Long fetchLength(String key);

    protected Long lengthFromCache(String key) {
        File f = super.getFile(key);
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
            LogFactory.getLog(BinaryFileCache.class).error(
                    "Cannot computle length of file " + f.getPath(), e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LogFactory.getLog(BinaryFileCache.class).error(
                            "Cannot close input file for " + f.getPath(), e);
                }
            }
        }
    }

    /**
     * Puts the length for the digest in the cache.
     *
     * @param len the length
     */
    protected void putLengthInCache(String key, Long len) {
        OutputStream out = null;
        File tmp = null;
        try {
            tmp = getTempFile();
            out = new FileOutputStream(tmp);
            Writer writer = new OutputStreamWriter(out);
            writer.write(len.toString());
            writer.flush();
            out.close();
            out = null;
            putFile(key + LEN_DIGEST_SUFFIX, tmp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LogFactory.getLog(BinaryFileCache.class).error("Cannot close output stream for " + tmp.getPath(), e);
                }
            }
        }
    }

    public Long getLength(String key) {
        Long length = lengthFromCache(key);
        if (length != null) {
            return length;
        }
        return fetchLength(key);

    }

    @Override
    public synchronized File getFile(String key) {

        // get file from cache
        File file = super.getFile(key);
        if (file != null) {
            return file;
        }

        // fetch file from storage
        File tmp = null;
        try {
            tmp = getTempFile();
            if (fetchFile(key, tmp)) {
                file = putFile(key, tmp);
                return file; // fetched file from storage
            }
        } catch (IOException e) {
            LogFactory.getLog(BinaryFileCache.class).error(
                    "IO error while fetching " + key + " on storage", e);
        }

        // file not in storage
        if (tmp != null) {
            tmp.delete();
        }
        return null;

    }
}
