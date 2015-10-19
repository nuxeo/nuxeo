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
        TransientStoreConfig defaultConfig = new TransientStoreConfig(DEFAULT_STORE_NAME);
        TransientStore store = defaultConfig.getStore();
        stores.put(defaultConfig.getName(), store);
        return store;
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
