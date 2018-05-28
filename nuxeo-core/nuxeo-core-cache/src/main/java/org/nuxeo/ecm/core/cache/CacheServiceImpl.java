/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Thierry Martins
 */
package org.nuxeo.ecm.core.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.core.cache.CacheDescriptor.DEFAULT_MAX_SIZE;
import static org.nuxeo.ecm.core.cache.CacheDescriptor.DEFAULT_TTL;
import static org.nuxeo.ecm.core.cache.CacheDescriptor.OPTION_MAX_SIZE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.pubsub.AbstractPubSubBroker;
import org.nuxeo.runtime.pubsub.SerializableMessage;

/**
 * Cache service implementation to manage nuxeo cache
 *
 * @since 6.0
 */
public class CacheServiceImpl extends DefaultComponent implements CacheService {

    private static final Log log = LogFactory.getLog(CacheServiceImpl.class);

    public static final ComponentName NAME = new ComponentName(CacheServiceImpl.class.getName());

    protected static final Random RANDOM = new Random();

    /**
     * @since 8.2
     */
    public static final String DEFAULT_CACHE_ID = "default-cache";

    /** @since 9.3 */
    public static final String CACHE_INVAL_PUBSUB_TOPIC = "cacheinval";

    /**
     * Framework property defining whether clustering is enabled.
     *
     * @since 9.3
     */
    public static final String CLUSTERING_ENABLED_PROP = "repository.clustering.enabled";

    /**
     * Framework property containing the node id.
     *
     * @since 9.3
     */
    public static final String NODE_ID_PROP = "repository.clustering.id";

    protected final CacheDescriptorRegistry registry = new CacheDescriptorRegistry();

    /** Currently registered caches. */
    protected final Map<String, CacheManagement> caches = new ConcurrentHashMap<>();

    protected CachePubSubInvalidator invalidator;

    // SimpleContributionRegistry is overkill and does not deal well with a "remove" feature
    protected static class CacheDescriptorRegistry {

        protected Map<String, List<CacheDescriptor>> allDescriptors = new ConcurrentHashMap<>();

        protected Map<String, CacheDescriptor> effectiveDescriptors = new ConcurrentHashMap<>();

        public void addContribution(CacheDescriptor descriptor) {
            String name = descriptor.name;
            allDescriptors.computeIfAbsent(name, n -> new CopyOnWriteArrayList<>()).add(descriptor);
            recompute(name);
        }

        public void removeContribution(CacheDescriptor descriptor) {
            String name = descriptor.name;
            allDescriptors.getOrDefault(name, Collections.emptyList()).remove(descriptor);
            recompute(name);
        }

        protected void recompute(String name) {
            CacheDescriptor desc = null;
            for (CacheDescriptor d : allDescriptors.getOrDefault(name, Collections.emptyList())) {
                if (d.remove) {
                    desc = null;
                } else {
                    if (desc == null) {
                        desc = d.clone();
                    } else {
                        desc.merge(d);
                    }
                }
            }
            if (desc == null) {
                effectiveDescriptors.remove(name);
            } else {
                effectiveDescriptors.put(name, desc);
            }
        }

        public CacheDescriptor getCacheDescriptor(String name) {
            return effectiveDescriptors.get(name);
        }

        public Collection<CacheDescriptor> getCacheDescriptors() {
            return effectiveDescriptors.values();
        }

        public CacheDescriptor getDefaultDescriptor() {
            CacheDescriptor defaultDescriptor = getCacheDescriptor(DEFAULT_CACHE_ID);
            if (defaultDescriptor == null) {
                defaultDescriptor = new CacheDescriptor();
                defaultDescriptor.ttl = DEFAULT_TTL;
                defaultDescriptor.options.put(OPTION_MAX_SIZE, String.valueOf(DEFAULT_MAX_SIZE));
            }
            return defaultDescriptor;
        }
    }

    public static class CacheInvalidation implements SerializableMessage {

        private static final long serialVersionUID = 1L;

        protected static final String SEP = "/";

        public final String cacheName;

        public final String key;

        public CacheInvalidation(String name, String key) {
            this.cacheName = name;
            this.key = key;
        }

        @Override
        public void serialize(OutputStream out) throws IOException {
            String string = cacheName + SEP + key;
            IOUtils.write(string, out, UTF_8);
        }

        public static CacheInvalidation deserialize(InputStream in) throws IOException {
            String string = IOUtils.toString(in, UTF_8);
            String[] parts = string.split(SEP, 2);
            if (parts.length != 2) {
                throw new IOException("Invalid invalidation: " + string);
            }
            String cacheName = parts[0];
            String key = parts[1];
            return new CacheInvalidation(cacheName, key);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + cacheName + "," + key + ")";
        }
    }

    public static abstract class AbstractCachePubSubInvalidator extends AbstractPubSubBroker<CacheInvalidation> {

        public static final String ALL_KEYS = "__ALL__";

        @Override
        public CacheInvalidation deserialize(InputStream in) throws IOException {
            return CacheInvalidation.deserialize(in);
        }

        public void sendInvalidation(String cacheName, String key) {
            sendMessage(new CacheInvalidation(cacheName, key));
        }

        public void sendInvalidationsAll(String cacheName) {
            sendMessage(new CacheInvalidation(cacheName, ALL_KEYS));
        }

        @Override
        public void receivedMessage(CacheInvalidation invalidation) {
            CacheManagement cache = (CacheManagement) getCache(invalidation.cacheName);
            if (cache != null) {
                String key = invalidation.key;
                if (ALL_KEYS.equals(key)) {
                    cache.invalidateLocalAll();
                } else {
                    cache.invalidateLocal(key);
                }
            }
        }

        // for testability, we want an alternative implementation to return a test cache
        protected abstract Cache getCache(String name);
    }

    protected class CachePubSubInvalidator extends AbstractCachePubSubInvalidator {

        @Override
        protected Cache getCache(String name) {
            return CacheServiceImpl.this.getCache(name);
        }
    }

    @Override
    public void registerContribution(Object contrib, String extensionPoint, ComponentInstance contributor) {
        registerCacheDescriptor((CacheDescriptor) contrib);
    }

    @Override
    @Deprecated
    public void registerCache(String name, int maxSize, int timeout) {
        registerCache(name);
    }

    @Override
    public void registerCache(String name) {
        CacheDescriptor defaultDescriptor = registry.getDefaultDescriptor().clone();
        defaultDescriptor.name = name;
        // add to registry (merging if needed)
        registerCacheDescriptor(defaultDescriptor);
        // start if needed
        maybeStart(name);
    }

    public void registerCacheDescriptor(CacheDescriptor descriptor) {
        registry.addContribution(descriptor);
        log.info("Cache registered: " + descriptor.name);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        unregisterCacheDescriptor((CacheDescriptor) contribution);
    }

    public void unregisterCacheDescriptor(CacheDescriptor descriptor) {
        registry.removeContribution(descriptor);
        log.info("Cache unregistered: " + descriptor.name);
    }

    @Override
    public int getApplicationStartedOrder() {
        ComponentInstance repositoryComponent = Framework.getRuntime().getComponentInstance(
                "org.nuxeo.ecm.core.repository.RepositoryServiceComponent");
        if (repositoryComponent == null || repositoryComponent.getInstance() == null) {
            return super.getApplicationStartedOrder();
        }
        return ((DefaultComponent) repositoryComponent.getInstance()).getApplicationStartedOrder() - 5;
    }

    @Override
    public void start(ComponentContext context) {
        if (Framework.isBooleanPropertyTrue(CLUSTERING_ENABLED_PROP)) {
            // register cache invalidator
            String nodeId = Framework.getProperty(NODE_ID_PROP);
            if (StringUtils.isBlank(nodeId)) {
                nodeId = String.valueOf(RANDOM.nextLong());
                log.warn("Missing cluster node id configuration, please define it explicitly "
                        + "(usually through repository.clustering.id). Using random cluster node id instead: "
                        + nodeId);
            } else {
                nodeId = nodeId.trim();
            }
            invalidator = new CachePubSubInvalidator();
            invalidator.initialize(CACHE_INVAL_PUBSUB_TOPIC, nodeId);
            log.info("Registered cache invalidator for node: " + nodeId);
        } else {
            log.info("Not registering a cache invalidator because clustering is not enabled");
        }
        // create and starts caches
        registry.getCacheDescriptors().forEach(this::startCacheDescriptor);
    }

    /** Creates and starts the cache. */
    protected void startCacheDescriptor(CacheDescriptor desc) {
        CacheManagement cache = desc.newInstance(invalidator);
        cache.start();
        caches.put(desc.name, cache);
    }

    @Override
    public void stop(ComponentContext context) {
        if (invalidator != null) {
            invalidator.close();
            invalidator = null;
        }
        for (CacheManagement cache : caches.values()) {
            cache.stop();
        }
        caches.clear();
    }

    protected void maybeStart(String name) {
        if (!Framework.getRuntime().getComponentManager().isStarted()) {
            return;
        }
        // stop previous
        CacheManagement cache = caches.get(name);
        if (cache != null) {
            cache.stop();
        }
        // start new one
        startCacheDescriptor(registry.getCacheDescriptor(name));
    }

    // --------------- API ---------------

    @Override
    public Cache getCache(String name) {
        return caches.get(name);
    }

    /**
     * @since 9.3
     */
    public CacheDescriptor getCacheDescriptor(String descriptor) {
        return registry.getCacheDescriptor(descriptor);
    }

}
