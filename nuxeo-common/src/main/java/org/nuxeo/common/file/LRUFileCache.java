/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.common.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.IOUtils;

/**
 * A LRU cache of {@link File}s with capped filesystem size.
 * <p>
 * When a new file is put in the cache, if the total size becomes more that the
 * maximum size then least recently access entries are removed until the new
 * file fits.
 * <p>
 * A file will never be actually removed from the filesystem while the File
 * object returned by {@link #getFile} is still referenced.
 * <p>
 * The cache keys are restricted to a subset of ASCII: letters, digits and
 * dashes. Usually a MD5 or SHA1 hash is used.
 */
public class LRUFileCache implements FileCache {

    /** Allowed key pattern, used as file path. */
    public static final Pattern SIMPLE_ASCII = Pattern.compile("[-_a-zA-Z0-9]+");

    protected final File dir;

    protected final long maxSize;

    /** Cached files. */
    protected final Map<String, LRUFileCacheEntry> cache;

    /** Size of the cached files. */
    protected long cacheSize;

    /** Most recently used entries from the cache are first. */
    protected final LinkedList<String> lru;

    // this creates a new thread
    private static final FileCleaningTracker fileCleaningTracker = new FileCleaningTracker();

    /**
     * In-memory entry for a cached file.
     */
    protected static class LRUFileCacheEntry {
        public File file;

        public long size;
    }

    /**
     * Constructs a cache in the given directory with the given maximum size (in
     * bytes).
     *
     * @param dir the directory to use to store cached files
     * @param maxSize the maximum size of the cache (in bytes)
     */
    public LRUFileCache(File dir, long maxSize) {
        this.dir = dir;
        this.maxSize = maxSize;
        cache = new HashMap<String, LRUFileCacheEntry>();
        lru = new LinkedList<String>();
    }

    @Override
    public long getSize() {
        return cacheSize;
    }

    @Override
    public int getNumberOfItems() {
        return lru.size();
    }

    @Override
    public File getTempFile() throws IOException {
        File tmp = File.createTempFile("nxbin_", null, dir);
        tmp.deleteOnExit();
        return tmp;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The key is used as a file name in the directory cache.
     */
    @Override
    public synchronized File putFile(String key, InputStream in)
            throws IOException {
        try {
            LRUFileCacheEntry entry = cache.get(key);
            if (entry != null) {
                return entry.file;
            }
            File file = getTempFile();
            FileOutputStream out = new FileOutputStream(file);
            try {
                IOUtils.copy(in, out);
            } finally {
                out.close();
            }
            return putFile(key, file);
        } finally {
            in.close();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The key is used as a file name in the directory cache.
     */
    @Override
    public synchronized File putFile(String key, File file)
            throws IllegalArgumentException, IOException {
        LRUFileCacheEntry entry = cache.get(key);
        if (entry != null) {
            file.delete(); // tmp file not used
            return entry.file;
        }

        // put file in cache with standard name
        checkKey(key);
        File dest = new File(dir, key);
        if (!file.renameTo(dest)) {
            // already something there
            file.delete();
        }
        file = dest;
        long size = file.length();

        // remove oldest entries until size fits
        ensureCapacity(size);

        // put new entry in cache
        entry = new LRUFileCacheEntry();
        entry.size = size;
        entry.file = file;
        add(key, entry);

        return file;
    }

    protected void checkKey(String key) throws IllegalArgumentException {
        if (!SIMPLE_ASCII.matcher(key).matches() || ".".equals(key)
                || "..".equals(key)) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
    }

    @Override
    public synchronized File getFile(String key) {
        LRUFileCacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        // note access in most recently used list
        recordAccess(key);
        return entry.file;
    }

    @Override
    public synchronized void clear() {
        for (String key : lru) {
            remove(key);
        }
        lru.clear();
    }

    protected void recordAccess(String key) {
        lru.remove(key); // TODO does a linear scan
        lru.addFirst(key);
    }

    protected void add(String key, LRUFileCacheEntry entry) {
        cache.put(key, entry);
        lru.addFirst(key);
        cacheSize += entry.size;
    }

    protected void remove(String key) {
        LRUFileCacheEntry entry = cache.remove(key);
        cacheSize -= entry.size;
        // delete file when not referenced anymore
        fileCleaningTracker.track(entry.file, entry.file);
    }

    protected void ensureCapacity(long size) {
        while (cacheSize + size > maxSize && !lru.isEmpty()) {
            remove(lru.removeLast());
        }
    }

}
