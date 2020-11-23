/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.kv;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation for the Key/Value Service.
 *
 * @since 9.1
 */
public class KeyValueServiceImpl extends DefaultComponent implements KeyValueService {

    public static final String XP_CONFIG = "configuration";

    public static final String DEFAULT_STORE_ID = "default";

    public static final int APPLICATION_STARTED_ORDER = -500;

    protected Map<String, KeyValueStoreProvider> providers = new ConcurrentHashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        return APPLICATION_STARTED_ORDER;
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        providers.values().forEach(KeyValueStoreProvider::close);
        providers.clear();
        super.stop(context);
    }

    // ===== KeyValueService =====

    @Override
    public synchronized KeyValueStore getKeyValueStore(String name) {
        KeyValueStoreProvider provider = providers.get(name);
        if (provider == null) {
            KeyValueStoreDescriptor desc;
            Optional<KeyValueStoreDescriptor> optDesc = getRegistryContribution(XP_CONFIG, name);
            if (optDesc.isEmpty()) {
                // instantiate a copy of the default descriptor
                optDesc = getRegistryContribution(XP_CONFIG, DEFAULT_STORE_ID);
                if (optDesc.isEmpty()) {
                    throw new RuntimeException("Missing configuration for default key/value store");
                }
                desc = new KeyValueStoreDescriptor(optDesc.get()); // copy
                desc.name = name;
                desc.namespace = name; // set new namespace in copy
            } else {
                desc = optDesc.get();
            }
            try {
                provider = desc.klass.getDeclaredConstructor().newInstance();
                provider.initialize(desc);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            providers.put(name, provider);
        }
        return provider;
    }

}
