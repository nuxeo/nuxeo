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
package org.nuxeo.ecm.core.storage.kv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation for the Key/Value Service.
 *
 * @since 9.1
 */
public class KeyValueServiceImpl extends DefaultComponent implements KeyValueService {

    private static final Log log = LogFactory.getLog(KeyValueServiceImpl.class);

    public static final String CONFIG_XP = "configuration";

    public static final String DEFAULT_STORE_ID = "default";

    protected final KeyValueStoreRegistry registry = new KeyValueStoreRegistry();

    protected Map<String, KeyValueStoreProvider> providers = new ConcurrentHashMap<>();

    protected KeyValueStore defaultStore = new MemKeyValueStore();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case CONFIG_XP:
            registerKeyValueStore((KeyValueStoreDescriptor) contribution);
            break;
        default:
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        switch (extensionPoint) {
        case CONFIG_XP:
            unregisterKeyValueStore((KeyValueStoreDescriptor) contribution);
            break;
        }
    }

    public void registerKeyValueStore(KeyValueStoreDescriptor descriptor) {
        registry.addContribution(descriptor);
        descriptorChanged(descriptor.name);
        log.info("Registered key/value store: " + descriptor.name);
    }

    public void unregisterKeyValueStore(KeyValueStoreDescriptor descriptor) {
        registry.removeContribution(descriptor);
        descriptorChanged(descriptor.name);
        log.info("Unregistered key/value store: " + descriptor.name);
    }

    // ===== KeyValueService =====

    @Override
    public synchronized KeyValueStore getKeyValueStore(String name) {
        KeyValueStoreProvider provider = providers.get(name);
        if (provider == null) {
            KeyValueStoreDescriptor descriptor = registry.getKeyValueStoreDescriptor(name);
            if (descriptor == null) {
                descriptor = registry.getKeyValueStoreDescriptor(DEFAULT_STORE_ID);
                if (descriptor == null) {
                    return defaultStore;
                }
            }
            try {
                provider = descriptor.getKlass().newInstance();
                provider.initialize(descriptor);
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
            providers.put(name, provider);
        }
        return provider;
    }

    /* Close previous provider if we're overwriting it. */
    protected synchronized void descriptorChanged(String name) {
        KeyValueStoreProvider provider = providers.remove(name);
        if (provider != null) {
            provider.close();
        }
    }

}
