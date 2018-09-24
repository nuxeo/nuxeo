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
package org.nuxeo.runtime.pubsub;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation for the Publish/Subscribe Service.
 *
 * @since 9.1
 */
public class PubSubServiceImpl extends DefaultComponent implements PubSubService {

    public static final String XP_CONFIG = "configuration";

    /** The currently-configured provider. */
    protected PubSubProvider provider;

    /** List of subscribers for each topic. */
    protected Map<String, List<BiConsumer<String, byte[]>>> subscribers = new ConcurrentHashMap<>();

    protected Map<String, String> options;

    @Override
    public void deactivate(ComponentContext context) {
        subscribers.clear();
        provider.close();
        provider = null;
        super.deactivate(context);
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        if (provider != null) {
            provider.close();
        }
        List<PubSubProviderDescriptor> descs = getDescriptors(XP_CONFIG);
        PubSubProviderDescriptor providerDescriptor = descs.isEmpty() ? null : descs.get(descs.size() - 1);
        if (providerDescriptor == null) {
            provider = new MemPubSubProvider(); // default implementation
            options = Collections.emptyMap();
        } else {
            Class<? extends PubSubProvider> klass = providerDescriptor.klass;
            // dynamic class check, the generics aren't enough
            if (!PubSubProvider.class.isAssignableFrom(klass)) {
                throw new RuntimeException("Class does not implement PubSubServiceProvider: " + klass.getName());
            }
            try {
                provider = klass.getDeclaredConstructor().newInstance();
                options = providerDescriptor.options;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        provider.initialize(options, subscribers);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        if (provider == null) {
            return;
        }
        provider.close();
    }

    @Override
    public int getApplicationStartedOrder() {
        // let RedisComponent start before us (Redis starts before WorkManager that starts before events)
        return -500 + 10;
    }

    // ===== delegation to actual implementation =====

    @Override
    public void publish(String topic, byte[] message) {
        provider.publish(topic, message);
    }

    @Override
    public void registerSubscriber(String topic, BiConsumer<String, byte[]> subscriber) {
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    @Override
    public void unregisterSubscriber(String topic, BiConsumer<String, byte[]> subscriber) {
        // use computeIfAbsent also for removal to avoid thread-safety issues
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).remove(subscriber);
    }

}
