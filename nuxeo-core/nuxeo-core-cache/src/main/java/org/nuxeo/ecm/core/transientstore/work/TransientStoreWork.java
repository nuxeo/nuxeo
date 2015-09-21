/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.transientstore.work;

import org.nuxeo.ecm.core.transientstore.StorageEntryImpl;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * A work allowing to store a result in the {@link TransientStore}.
 *
 * @since 7.4
 */
public abstract class TransientStoreWork extends AbstractWork {

    public static final String STORE_NAME = "transientStoreWorkCache";

    public static final String KEY_SUFFIX = "_result";

    protected String entryKey;

    protected transient StorageEntry entry;

    /**
     * Returns a storage entry given its {@code key} from the transient store used by the {@code TransientStoreWork}.
     */
    public static StorageEntry getStorageEntry(String key) {
        TransientStore store = getStore();
        return store.get(key);
    }

    /**
     * Remove a storage entry given its {@code key} from the transient store used by the {@code TransientStoreWork}.
     */
    public static void removeStorageEntry(String key) {
        getStore().remove(key);
    }

    protected static void saveStorageEntry(StorageEntry storageEntry) {
        getStore().put(storageEntry);
    }

    protected static TransientStore getStore() {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        return transientStoreService.getStore(STORE_NAME);
    }

    public TransientStoreWork() {
        super();
        computeEntryKey();
    }

    public TransientStoreWork(String id) {
        super(id);
        computeEntryKey();
    }

    protected void computeEntryKey() {
        entryKey = getId() + KEY_SUFFIX;
    }

    protected StorageEntry getStorageEntry() {
        if (entry == null) {
            entry = getStorageEntry(entryKey);
            if (entry == null) {
                entry = new StorageEntryImpl(entryKey);
            }
        }
        return entry;
    }

    protected void saveStorageEntry() {
        if (entry != null) {
            saveStorageEntry(entry);
        }
    }

    @Override
    public String getWorkInstanceResult() {
        return entryKey;
    }
}
