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
import static org.nuxeo.ecm.core.cache.CacheDescriptor.OPTION_MAX_SIZE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.pubsub.AbstractPubSubBroker;
import org.nuxeo.runtime.pubsub.SerializableMessage;

/**
 * Cache service implementation to manage nuxeo cache
 *
 * @since 6.0
 */
public class CacheServiceImpl extends DefaultComponent implements CacheService {

    /**
     * @since 10.3
     */
    public static final String XP_CACHES = "caches";

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

    /** Currently registered caches. */
    protected final Map<String, CacheManagement> caches = new ConcurrentHashMap<>();

    protected CachePubSubInvalidator invalidator;

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
    @Deprecated
    public void registerCache(String name, int maxSize, int timeout) {
        registerCache(name);
    }

    @Override
    public void registerCache(String name) {
        CacheDescriptor defaultDescriptor = getCacheDescriptor(DEFAULT_CACHE_ID);
        if (defaultDescriptor == null) {
            defaultDescriptor = new CacheDescriptor();
            defaultDescriptor.name = DEFAULT_CACHE_ID;
            defaultDescriptor.options.put(OPTION_MAX_SIZE, String.valueOf(DEFAULT_MAX_SIZE));
            register(XP_CACHES, defaultDescriptor);
        }
        CacheDescriptor newDescriptor = (CacheDescriptor) new CacheDescriptor().merge(defaultDescriptor);
        newDescriptor.name = name;
        // add to registry (merging if needed)
        register(XP_CACHES, newDescriptor);
        // start if needed
        maybeStart(name);
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
        super.start(context);
        if (Framework.isBooleanPropertyTrue(CLUSTERING_ENABLED_PROP)) {
            // register cache invalidator
            String nodeId = Framework.getProperty(NODE_ID_PROP);
            if (StringUtils.isBlank(nodeId)) {
                nodeId = String.valueOf(RANDOM.nextLong());
                getLog().warn("Missing cluster node id configuration, please define it explicitly "
                        + "(usually through repository.clustering.id). Using random cluster node id instead: "
                        + nodeId);
            } else {
                nodeId = nodeId.trim();
            }
            invalidator = new CachePubSubInvalidator();
            invalidator.initialize(CACHE_INVAL_PUBSUB_TOPIC, nodeId);
            getLog().info("Registered cache invalidator for node: " + nodeId);
        } else {
            getLog().info("Not registering a cache invalidator because clustering is not enabled");
        }
        // create and starts caches
        Collection<CacheDescriptor> descriptors = getDescriptors(XP_CACHES);
        descriptors.forEach(this::startCacheDescriptor);
    }

    /** Creates and starts the cache. */
    protected void startCacheDescriptor(CacheDescriptor desc) {
        CacheManagement cache;
        if (desc.klass == null) {
            cache = new InMemoryCacheImpl(desc); // default cache implementation
        } else {
            try {
                cache = desc.klass.getConstructor(CacheDescriptor.class).newInstance(desc);
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Failed to instantiate class: " + desc.klass + " for cache: " + desc.name, e);
            }
        }
        // wrap with checker, metrics and invalidator
        cache = new CacheAttributesChecker(cache);
        cache = new CacheMetrics(cache);
        if (invalidator != null) {
            cache = new CacheInvalidator(cache, invalidator);
        }
        cache.start();
        caches.put(desc.name, cache);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
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
        startCacheDescriptor(getCacheDescriptor(name));
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
        return getDescriptor(XP_CACHES, descriptor);
    }

}
