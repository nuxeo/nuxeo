/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 *
 */
package org.nuxeo.ecm.core.cache;

/**
 * Nuxeo cache interface
 *
 * @since 6.0
 */
public interface CacheService {

    /**
     * Gets the cache with the given name.
     *
     * @param name the cache name
     * @return the cache, or {@code null} if it does not exist
     */
    Cache getCache(String name);

    /**
     * Programmatically registers a cache with the given characteristics.
     *
     * @param name the cache name
     * @param size the maximum number of elements
     * @param timeout the entry timeout (in minutes)
     * @since 8.2
     * @deprecated since 9.3, seems unused, use {@link #registerCache(String)} instead.
     */
    @Deprecated void registerCache(String name, int size, int timeout);

    /**
     * Programmatically registers a cache with the given name, with the the size and the timeout given by the default
     * cache.
     *
     * @param name the cache name
     * @since 9.3
     */
    void registerCache(String name);
}
