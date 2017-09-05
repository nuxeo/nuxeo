/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.transientstore.api;

import java.util.Set;

/**
 * Transient Store SPI.
 *
 * @since 9.3
 */
public interface TransientStoreProvider extends TransientStore {

    /**
     * Initializes the store from the given {@code config}.
     *
     * @since 7.2
     */
    void init(TransientStoreConfig config);

    /**
     * Shuts down the store.
     *
     * @since 7.2
     */
    void shutdown();

    /**
     * Returns the set of keys for all entries.
     *
     * @since 8.3
     */
    Set<String> keySet();

    /**
     * Returns the size (in MB) of the disk storage used for blobs.
     *
     * @return the number of MB (rounded down) used by stored blobs
     * @since 7.2
     * @deprecated since 9.3 because it is imprecise, use {@link #getStorageSize} instead
     */
    @Deprecated
    default int getStorageSizeMB() {
        return (int) (getStorageSize() / 1024 / 1024);
    }

    /**
     * Returns the size (in bytes) of the disk storage used for blobs.
     *
     * @return the number of bytes used by stored blobs
     * @since 9.3
     */
    long getStorageSize();

    /**
     * Runs garbage collecting to delete the file system resources that are associated with entries that were removed.
     *
     * @since 7.2
     */
    void doGC();

    /**
     * Removes all entries from the store.
     *
     * @since 7.2
     */
    void removeAll();

}
