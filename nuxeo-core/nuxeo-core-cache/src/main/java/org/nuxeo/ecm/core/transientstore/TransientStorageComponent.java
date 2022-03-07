/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.transientstore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Component exposing the {@link TransientStoreService} and managing the underlying extension point
 *
 * @since 7.2
 */
public class TransientStorageComponent extends DefaultComponent implements TransientStoreService {

    private static final Logger log = LogManager.getLogger(TransientStorageComponent.class);

    protected Map<String, TransientStoreProvider> stores = new HashMap<>();

    public static final String EP_STORE = "store";

    public static final String DEFAULT_STORE_NAME = "default";

    @Override
    public synchronized TransientStore getStore(String name) {
        Objects.requireNonNull(name, "Transient store name cannot be null");
        TransientStore store = stores.get(name);
        if (store == null) {
            TransientStoreConfig descriptor = getDescriptor(EP_STORE, name);
            if (descriptor == null) {
                // instantiate a copy of the default descriptor
                descriptor = new TransientStoreConfig(getDefaultDescriptor()); // copy
                descriptor.name = name; // set new name in copy
            } else if (!DEFAULT_STORE_NAME.equals(name)) {
                // make sure descriptor inherits config from default
                descriptor = getDefaultDescriptor().merge(descriptor);
            }
            TransientStoreProvider provider;
            try {
                Class<? extends TransientStoreProvider> klass = descriptor.implClass;
                if (klass == null) {
                    klass = SimpleTransientStore.class;
                }
                provider = klass.getDeclaredConstructor().newInstance();
                provider.init(descriptor);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            stores.put(name, provider);
            store = provider;
        }
        return store;
    }

    protected TransientStoreConfig getDefaultDescriptor() {
        TransientStoreConfig descriptor = getDescriptor(EP_STORE, DEFAULT_STORE_NAME);
        if (descriptor == null) {
            // TODO make this a hard error
            String message = "Missing configuration for default transient store, using in-memory";
            log.warn(message);
            Framework.getRuntime().getMessageHandler().addWarning(message);
            // use in-memory store
            descriptor = new TransientStoreConfig(DEFAULT_STORE_NAME);
        }
        return descriptor;
    }

    @Override
    public void doGC() {
        // keep backward compat: run GC only on stores that have been used
        stores.values().forEach(TransientStoreProvider::doGC);
    }

    @Override
    public void doGC(String name) {
        TransientStoreProvider provider = (TransientStoreProvider) getStore(name);
        provider.doGC();
    }

    @Override
    public Set<String> listStores() {
        // concat static and dynamic stores
        Set<String> ret = getDescriptors(EP_STORE).stream()
                                                  .map(descriptor -> descriptor.getId())
                                                  .collect(Collectors.toSet());
        stores.forEach((k, v) -> ret.add(k));
        return ret;
    }

    @Override
    protected boolean unregister(String xp, Descriptor descriptor) {
        boolean removed = super.unregister(xp, descriptor);
        if (removed) {
            TransientStoreProvider store = stores.remove(descriptor.getId());
            if (store != null) {
                store.shutdown();
            }
        }
        return removed;
    }

    @Override
    public void start(ComponentContext context) {
        // make sure we have a default store
        getStore(DEFAULT_STORE_NAME);
        // instantiate all registered stores
        getDescriptors(EP_STORE).forEach(desc -> getStore(desc.getId()));
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        stores.values().forEach(TransientStoreProvider::shutdown);
        super.stop(context);
    }

    @Override
    public void deactivate(ComponentContext context) {
        stores.clear();
        super.deactivate(context);
    }

    public void cleanUpStores() {
        stores.values().forEach(TransientStoreProvider::removeAll);
    }

}
