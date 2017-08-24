/*
 * (C) Copyright 2017 Nuxeo(http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.cache;

/**
 * Management-related APIs for a {@link Cache}.
 *
 * @since 9.3
 */
public interface CacheManagement extends Cache {

    /**
     * Starts this cache.
     */
    void start();

    /**
     * Stops this cache and releases related resources.
     */
    void stop();

    /**
     * Returns this cache size (approximate number of entries), or {@code -1} if the number of entries is unknown or too
     * expensive to compute.
     *
     * @return the approximate number of entries, or {@code -1}
     * @since 9.1
     */
    long getSize();

}
