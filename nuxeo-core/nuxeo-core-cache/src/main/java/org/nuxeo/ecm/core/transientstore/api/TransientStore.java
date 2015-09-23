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

import org.nuxeo.common.annotation.Experimental;

/**
 * Service Interface for managing a transient store.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@Experimental(comment = "https://jira.nuxeo.com/browse/NXP-16577")
public interface TransientStore {

    /**
     * Adds a new {@link StorageEntry}.
     */
    void put(StorageEntry entry);

    /**
     * Returns a {@link StorageEntry} given its {@code key}, or null if not found.
     */
    StorageEntry get(String key);

    /**
     * Removes a {@link StorageEntry} given its {@code key}.
     */
    void remove(String key);

    /**
     * Informs the Store that the entry can be released if TTL or GC parameters requires to do some cleanup.
     */
    void release(String key);

    /**
     * Runs the Garbage Collecting to delete the Filesystem resources that may correspond to cache entries that were
     * removed
     */
    void doGC();

    /**
     * Returns the size of the disk storage used in MB
     */
    int getStorageSizeMB();

    /**
     * Shutdown the store.
     */
    void shutdown();

    /**
     * Initialize the store from the configuration
     */
    void init(TransientStoreConfig config);

}
