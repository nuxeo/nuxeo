/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.transientstore;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component exposing the {@link TransientStoreService} and managing the unerlying extension point
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class TransientStorageComponent extends DefaultComponent implements TransientStoreService {

    protected Map<String, TransientStoreConfig> configs = new HashMap<>();

    protected Map<String, TransientStore> stores = new HashMap<>();

    public static final String EP_STORE = "store";

    public static final String DEFAULT_STORE_NAME = "default";

    @Override
    public TransientStore getStore(String name) {
        TransientStore store = stores.get(name);
        if (store == null) {
            store = stores.get(DEFAULT_STORE_NAME);
            if (store == null) {
                store = registerDefaultStore();
            }
        }
        return store;
    }

    protected TransientStore registerDefaultStore() {
        synchronized (this) {
            TransientStore defaultStore = stores.get(DEFAULT_STORE_NAME);
            if (defaultStore == null) {
                TransientStoreConfig defaultConfig = new TransientStoreConfig(DEFAULT_STORE_NAME);
                defaultStore = defaultConfig.getStore();
                stores.put(defaultConfig.getName(), defaultStore);
            }
            return defaultStore;
        }
    }

    public void doGC() {
        stores.values().forEach(TransientStore::doGC);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EP_STORE.equals(extensionPoint)) {
            TransientStoreConfig config = (TransientStoreConfig) contribution;
            // XXX merge
            configs.put(config.getName(), config);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EP_STORE.equals(extensionPoint)) {
            TransientStoreConfig config = (TransientStoreConfig) contribution;
            TransientStore store = stores.get(config.getName());
            store.shutdown();
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        for (TransientStoreConfig config : configs.values()) {
            registerStore(config);
        }
    }

    protected TransientStore registerStore(TransientStoreConfig config) {
        TransientStore store = config.getStore();
        stores.put(config.getName(), store);
        return store;
    }

    @Override
    public void deactivate(ComponentContext context) {
        stores.values().forEach(TransientStore::shutdown);
        super.deactivate(context);
    }

    public void cleanUpStores() {
        stores.values().forEach(TransientStore::removeAll);
    }

}
