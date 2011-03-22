/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A cache of {@link File}s.
 * <p>
 * The cache uses application-chosen keys.
 * <p>
 * To check presence in the cache, use {@link #getFile}.
 * <p>
 * To put a new {@link InputStream} in the cache, use
 * {@link #putFile(String, InputStream)}. Or if you'd like a {@link File} object
 * into which to write some data, get one using {@link #getTempFile}, put the
 * actual binary in it, then pass this file to {@link #putFile(String, File)}.
 *
 * @see LRUFileCache
 */
public interface FileCache {

    /**
     * Gets the size of the cache, in bytes.
     */
    long getSize();

    /**
     * Gets the number of items in the cache.
     */
    int getNumberOfItems();

    /**
     * Creates a temporary file.
     */
    File getTempFile() throws IOException;

    /**
     * Puts a file in the cache.
     *
     * @param key the cache key
     * @param in the input stream to cache (closed afterwards)
     * @return the cached file
     * @throws IllegalArgumentException if the key is illegal
     */
    File putFile(String key, InputStream in) throws IOException;

    /**
     * Puts a file in the cache.
     * <p>
     * The file must have been created through {@link #getTempFile()}. The file
     * is "given" to this method, who will delete it or rename it.
     *
     * @param key the cache key
     * @param file the file to cache
     * @return the cached file
     * @throws IllegalArgumentException if the key is illegal
     */
    File putFile(String key, File file) throws IOException;

    /**
     * Gets a file from the cache.
     * <p>
     * A returned file will never be deleted from the filesystem while the
     * returned file object is still referenced, although it may be purged from
     * the cache.
     *
     * @param key the cache key
     * @return the cached file, or {@code null} if absent
     */
    File getFile(String key);

    /**
     * Clears the cache.
     * <p>
     * Files will not be deleted from the filesystm while the returned file
     * objects are still referenced.
     */
    void clear();

}
