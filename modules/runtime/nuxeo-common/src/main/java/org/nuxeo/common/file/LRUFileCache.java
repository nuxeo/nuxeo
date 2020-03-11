/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.common.file;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A LRU cache of {@link File}s with maximum filesystem size.
 * <p>
 * Cache entries that are old enough and whose size makes the cache bigger than its maximum size are deleted.
 * <p>
 * The cache keys are restricted to a subset of ASCII: letters, digits and dashes. Usually a MD5 or SHA1 hash is used.
 */
public class LRUFileCache implements FileCache {

    private static final Log log = LogFactory.getLog(LRUFileCache.class);

    /** Allowed key pattern, used as file path. */
    public static final Pattern SIMPLE_ASCII = Pattern.compile("[-_.@a-zA-Z0-9]+");

    private static final String TMP_PREFIX = "nxbin_";

    private static final String TMP_SUFFIX = ".tmp";

    public static final long CLEAR_OLD_ENTRIES_INTERVAL_MILLIS_DEFAULT = 5000; // 5 s

    protected long clearOldEntriesIntervalMillis = CLEAR_OLD_ENTRIES_INTERVAL_MILLIS_DEFAULT;

    protected static class PathInfo implements Comparable<PathInfo> {

        protected final Path path;

        protected final long time;

        protected final long size;

        public PathInfo(Path path) throws IOException {
            this.path = path;
            this.time = Files.getLastModifiedTime(path).toMillis();
            this.size = Files.size(path);
        }

        @Override
        public int compareTo(PathInfo other) {
            return Long.compare(other.time, time); // compare in reverse order (most recent first)
        }
    }

    protected final Path dir;

    protected final long maxSize;

    protected final long maxCount;

    protected final long minAgeMillis;

    protected Lock clearOldEntriesLock = new ReentrantLock();

    protected long clearOldEntriesLast;

    /**
     * Constructs a cache in the given directory with the given maximum size (in bytes).
     *
     * @param dir the directory to use to store cached files
     * @param maxSize the maximum size of the cache (in bytes)
     * @param maxCount the maximum number of files in the cache
     * @param minAge the minimum age of a file in the cache to be eligible for removal (in seconds)
     */
    public LRUFileCache(File dir, long maxSize, long maxCount, long minAge) {
        this.dir = dir.toPath();
        this.maxSize = maxSize;
        this.maxCount = maxCount;
        this.minAgeMillis = minAge * 1000;
    }

    // for tests
    public void setClearOldEntriesIntervalMillis(long millis) {
        clearOldEntriesIntervalMillis = millis;
    }

    /**
     * Filter keeping regular files that aren't temporary.
     */
    protected static class RegularFileFilter implements DirectoryStream.Filter<Path> {

        protected static final RegularFileFilter INSTANCE = new RegularFileFilter();

        @Override
        public boolean accept(Path path) {
            if (!Files.isRegularFile(path)) {
                return false;
            }
            String filename = path.getFileName().toString();
            if (filename.startsWith(TMP_PREFIX) && filename.endsWith(TMP_SUFFIX)) {
                return false;
            }
            return true;
        }
    }

    @Override
    public long getSize() {
        long size = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, RegularFileFilter.INSTANCE)) {
            for (Path path : ds) {
                size += Files.size(path);
            }
        } catch (IOException e) {
            log.error(e, e);
        }
        return size;
    }

    @Override
    public int getNumberOfItems() {
        int count = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, RegularFileFilter.INSTANCE)) {
            for (Path path : ds) {
                count++;
            }
        } catch (IOException e) {
            log.error(e, e);
        }
        return count;
    }

    @Override
    public void clear() {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, RegularFileFilter.INSTANCE)) {
            for (Path path : ds) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.error(e, e);
                }
            }
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    /**
     * Clears cache entries if they are old enough and their size makes the cache bigger than its maximum size.
     */
    protected void clearOldEntries() {
        if (clearOldEntriesLock.tryLock()) {
            try {
                if (System.currentTimeMillis() > clearOldEntriesLast + clearOldEntriesIntervalMillis) {
                    doClearOldEntries();
                    clearOldEntriesLast = System.currentTimeMillis();
                    return;
                }
            } finally {
                clearOldEntriesLock.unlock();
            }
        }
        // else don't do anything, another thread is already clearing old entries
    }

    protected void doClearOldEntries() {
        List<PathInfo> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, RegularFileFilter.INSTANCE)) {
            for (Path path : ds) {
                try {
                    files.add(new PathInfo(path));
                } catch (IOException e) {
                    log.error(e, e);
                }
            }
        } catch (IOException e) {
            log.error(e, e);
        }
        Collections.sort(files); // sort by most recent first

        long size = 0;
        long count = 0;
        long threshold = System.currentTimeMillis() - minAgeMillis;
        for (PathInfo pi : files) {
            size += pi.size;
            count++;
            if (pi.time < threshold) {
                // old enough to be candidate
                if (size > maxSize || count > maxCount) {
                    // delete file
                    try {
                        Files.delete(pi.path);
                        size -= pi.size;
                        count--;
                    } catch (IOException e) {
                        log.error(e, e);
                    }
                }
            }
        }
    }

    @Override
    public File getTempFile() throws IOException {
        // make sure we have a temporary directory
        // even if it's been deleted by an external process doing cleanup
        Files.createDirectories(dir);
        return Files.createTempFile(dir, TMP_PREFIX, TMP_SUFFIX).toFile();
    }

    protected void checkKey(String key) throws IllegalArgumentException {
        if (!SIMPLE_ASCII.matcher(key).matches() || ".".equals(key) || "..".equals(key)) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The key is used as a file name in the directory cache.
     */
    @Override
    public File putFile(String key, InputStream in) throws IOException {
        File tmp;
        try {
            // check the cache
            checkKey(key);
            Path path = dir.resolve(key);
            if (Files.exists(path)) {
                recordAccess(path);
                return path.toFile();
            }

            // store the stream in a temporary file
            tmp = getTempFile();
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                IOUtils.copy(in, out);
            }
        } finally {
            in.close();
        }
        return putFile(key, tmp);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The key is used as a file name in the directory cache.
     */
    @Override
    public File putFile(String key, File file) throws IllegalArgumentException, IOException {
        Path source = file.toPath();

        // put file in cache
        checkKey(key);
        Path path = dir.resolve(key);
        try {
            Files.move(source, path, ATOMIC_MOVE, REPLACE_EXISTING);
            recordAccess(path);
            clearOldEntries();
        } catch (FileAlreadyExistsException faee) {
            // already something there
            recordAccess(path);
            // remove unused tmp file
            try {
                Files.delete(source);
            } catch (IOException e) {
                log.error(e, e);
            }
        }
        return path.toFile();
    }

    @Override
    public File getFile(String key) {
        checkKey(key);
        Path path = dir.resolve(key);
        if (!Files.exists(path)) {
            return null;
        }
        recordAccess(path);
        return path.toFile();
    }

    /** Records access to a file by changing its modification time. */
    protected void recordAccess(Path path) {
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException e) {
            log.error(e, e);
        }
    }

}
