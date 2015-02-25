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

import java.io.IOException;
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

    protected Map<String, TransientStoreConfig> configs = new HashMap<String, TransientStoreConfig>();

    protected Map<String, TransientStore> stores = new HashMap<String, TransientStore>();

    public static final String EP_STORE = "store";

    @Override
    public TransientStore getStore(String name) {
        return stores.get(name);
    }

    @Override
    public TransientStoreConfig getStoreConfig(String name) throws IOException {
        TransientStore store = getStore(name);
        if (store != null) {
            return store.getConfig();
        }
        return null;
    }

    public TransientStore registerStore(TransientStoreConfig config) {
        try {
            TransientStore store = config.getStore();
            stores.put(config.getName(), store);
            return store;
        } catch (Exception e) {
            throw new RuntimeException("Unable to register Store", e);
        }
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
    public void applicationStarted(ComponentContext context) {
        for (TransientStoreConfig config : configs.values()) {
            registerStore(config);
        }
    }

    public void doGC() {
        for (TransientStore store : stores.values()) {
            store.doGC();
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        for (TransientStore store : stores.values()) {
            store.shutdown();
        }
        super.deactivate(context);
    }


    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EP_STORE.equals(extensionPoint)) {
            TransientStoreConfig config = (TransientStoreConfig) contribution;
            TransientStore store = stores.get(config.getName());
            store.shutdown();
            super.unregisterContribution(contribution, extensionPoint, contributor);
        }
    }



}
