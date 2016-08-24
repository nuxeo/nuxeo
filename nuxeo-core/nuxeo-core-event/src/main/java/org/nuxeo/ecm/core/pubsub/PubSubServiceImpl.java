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
package org.nuxeo.ecm.core.pubsub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.EventServiceComponent;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation for the Publish/Subscribe Service.
 *
 * @since 9.1
 */
public class PubSubServiceImpl extends DefaultComponent implements PubSubService {

    public static final String CONFIG_XP = "configuration";

    /** All the registered descriptors. */
    protected List<PubSubProviderDescriptor> providerDescriptors = new CopyOnWriteArrayList<>();

    /** The currently-configured provider. */
    protected PubSubProvider provider;

    /** The descriptor for the currently-configured provider, or {@code null} if it's the default. */
    protected PubSubProviderDescriptor providerDescriptor;

    /** List of subscribers for each topic. */
    protected Map<String, List<BiConsumer<String, byte[]>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void activate(ComponentContext context) {
        providerDescriptorChanged();
    }

    @Override
    public void deactivate(ComponentContext context) {
        subscribers.clear();
        provider.close();
        provider = null;
    }

    @Override
    public void start(ComponentContext context) {
        if (provider == null) {
            return;
        }
        provider.initialize(subscribers);
    }

    @Override
    public void stop(ComponentContext context) {
        if (provider == null) {
            return;
        }
        provider.close();
    }

    @Override
    public int getApplicationStartedOrder() {
        // let RedisComponent start before us (Redis starts before WorkManager that starts before events)
        return EventServiceComponent.APPLICATION_STARTED_ORDER + 10;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIG_XP.equals(extensionPoint)) {
            registerProvider((PubSubProviderDescriptor) contribution);
        } else {
            throw new NuxeoException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIG_XP.equals(extensionPoint)) {
            unregisterProvider((PubSubProviderDescriptor) contribution);
        }
    }

    protected void registerProvider(PubSubProviderDescriptor descriptor) {
        providerDescriptors.add(descriptor);
        providerDescriptor = descriptor;
        providerDescriptorChanged();
    }

    protected void unregisterProvider(PubSubProviderDescriptor descriptor) {
        providerDescriptors.remove(descriptor);
        if (descriptor == providerDescriptor) {
            // we removed the current provider, find a new one
            int size = providerDescriptors.size();
            providerDescriptor = size == 0 ? null : providerDescriptors.get(size - 1);
            providerDescriptorChanged();
        }
    }

    protected void providerDescriptorChanged() {
        if (provider != null) {
            provider.close();
        }
        if (providerDescriptor == null) {
            provider = new MemPubSubProvider(); // default implementation
        } else {
            provider = providerDescriptor.getInstance();
        }
        // initialize later, in applicationStarted
        // provider.initialize(subscribers);
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
