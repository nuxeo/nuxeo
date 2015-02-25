/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.transientstore.api;

import java.io.IOException;

/**
 *
 * Service Interface for managing a transient store.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public interface TransientStore {

    /**
     * Add a new entry in the Store
     *
     * @param entry
     */
    void put(StorageEntry entry) throws IOException;

    /**
     * Retrieve a new entry inside the Store
     *
     * @param key
     * @return the corresponding {@link StorageEntry} or null
     */
    StorageEntry get(String key) throws IOException;

    /**
     * Remove an entry from the Store
     *
     * @param key
     */
    void remove(String key) throws IOException;

    /**
     * Informs the Store that the entry can be deleted if TTL or GC parameters requires to do some cleanup
     *
     * @param key
     */
    void canDelete(String key) throws IOException;

    /**
     * Deletes all entries inside the Store
     *
     * @throws IOException *
     */
    void removeAll() throws IOException;

    /**
     * Returns the Store configuration
     *
     * @return the {@link TransientStoreConfig}
     * @throws IOException
     */
    TransientStoreConfig getConfig() throws IOException;

    /**
     * Runs the Garbage Collecting to delete the Filesystem resources that may correspond
     * to cache entries that were removed
     *
     */
    void doGC();

    /**
     * Return the size of the disk storage used
     *
     * @return the size of the disk storage used in MB
     */
    int getStorageSizeMB();


    void shutdown();

    /**
     * Initialize the store from the configuration
     *
     * @param config
     */
    void init(TransientStoreConfig config);


}
