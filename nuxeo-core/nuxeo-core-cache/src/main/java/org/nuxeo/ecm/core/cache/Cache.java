/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import java.io.Serializable;
import java.util.Set;

/**
 * The nuxeo cache interface that define generic methods to use cache technologies
 *
 * @since 6.0
 */
public interface Cache {

    /**
     * Get cache name as specified in the descriptor
     *
     * @return the cache name
     * @since 6.0
     */
    public String getName();

    /**
     * Get method to retrieve value from cache Must not raise exception if the key is null, but return null
     *
     * @param key the string key
     * @return the {@link Serializable} value, return null if the key does not exist or if the key is null
     * @since 6.0
     */
    public Serializable get(String key);

    /**
     * Returns the set of all keys stored in the cache.
     *
     * @return the {@link Set} of all keys
     * @since 8.3
     */
    public Set<String> keySet();

    /**
     * Invalidate the given key
     *
     * @param key, the key to remove from the cache, if null will do nothing
     * @since 6.0
     */
    public void invalidate(String key);

    /**
     * Invalidate all key-value stored in the cache
     *
     * @since 6.0
     */
    public void invalidateAll();

    /**
     * Put method to store a {@link Serializable} value
     *
     * @param key the string key, if null, the value will not be stored
     * @param value the value to store, if null, the value will not be stored
     * @since 6.0
     */
    public void put(String key, Serializable value);

    /**
     * Check if a given key is present inside the cache. Compared to the get() method, this method must not update
     * internal cache state and change TTL
     *
     * @param key the string key
     * @return true if a corresponding entry exists, false otherwise
     * @since 7.2
     */
    public boolean hasEntry(String key);

    /**
     * Return this cache size
     * @since 9.1
     */
    public long getSize();

}
