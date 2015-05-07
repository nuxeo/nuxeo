/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Andre Justo
 */

package org.nuxeo.ecm.platform.oauth2.tokens;

import com.google.api.client.util.Maps;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OAuth2TokenStoreFactory implements DataStoreFactory {

    /** Lock on access to the data store map. */
    private final Lock lock = new ReentrantLock();

    /** Map of data store ID to data store. */
    private final Map<String, DataStore<? extends Serializable>> dataStoreMap = Maps.newHashMap();

    /** Returns a global thread-safe instance. */
    public static OAuth2TokenStoreFactory getDefaultInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public <V extends Serializable> DataStore<V> getDataStore(String id) throws IOException {
        lock.lock();
        try {
            DataStore<V> dataStore = (DataStore<V>) dataStoreMap.get(id);
            if (dataStore == null) {
                dataStore = createDataStore(id);
                dataStoreMap.put(id, dataStore);
            }
            return dataStore;
        } finally {
            lock.unlock();
        }
    }

    /** Holder for the result of {@link #getDefaultInstance()}. */
    static class InstanceHolder {
        static final OAuth2TokenStoreFactory INSTANCE = new OAuth2TokenStoreFactory();
    }

    protected <V extends Serializable> DataStore<V> createDataStore(String id) throws IOException {
        return new OAuth2TokenStore(id, this);
    }
}
