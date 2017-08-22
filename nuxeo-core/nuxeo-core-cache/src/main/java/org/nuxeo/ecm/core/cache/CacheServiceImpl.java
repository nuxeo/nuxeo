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

import static org.nuxeo.ecm.core.cache.CacheDescriptor.OPTION_MAX_SIZE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Cache service implementation to manage nuxeo cache
 *
 * @since 6.0
 */
public class CacheServiceImpl extends DefaultComponent implements CacheService {

    /**
     * @since 8.2
     */
    public static final String DEFAULT_CACHE_ID = "default-cache";

    public static final ComponentName NAME = new ComponentName(CacheServiceImpl.class.getName());

    private static final Log log = LogFactory.getLog(CacheServiceImpl.class);

    protected final CacheDescriptorRegistry registry = new CacheDescriptorRegistry();

    /** Currently registered caches. */
    protected final Map<String, CacheManagement> caches = new ConcurrentHashMap<>();

    // SimpleContributionRegistry is overkill and does not deal well with a "remove" feature
    protected static class CacheDescriptorRegistry {

        protected Map<String, List<CacheDescriptor>> allDescriptors = new HashMap<>();

        protected Map<String, CacheDescriptor> effectiveDescriptors = new HashMap<>();

        public void addContribution(CacheDescriptor descriptor) {
            String name = descriptor.name;
            allDescriptors.computeIfAbsent(name, n -> new ArrayList<>()).add(descriptor);
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
    }

    @Override
    public void registerContribution(Object contrib, String extensionPoint, ComponentInstance contributor) {
        registerCacheDescriptor((CacheDescriptor) contrib);
    }

    @Override
    public void registerCache(String name, int maxSize, int timeout) {
        // start from default or empty
        CacheDescriptor defaultDescriptor = registry.getCacheDescriptor(DEFAULT_CACHE_ID);
        CacheDescriptor desc = defaultDescriptor == null ? new CacheDescriptor() : defaultDescriptor.clone();
        // add explicit configuration
        desc.name = name;
        desc.ttl = Long.valueOf(timeout);
        desc.options.put(OPTION_MAX_SIZE, String.valueOf(maxSize));
        // add to registry (merging if needed)
        registerCacheDescriptor(desc);
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
        for (CacheDescriptor desc : registry.getCacheDescriptors()) {
            CacheManagement cache = desc.newInstance();
            cache.start();
            caches.put(desc.name, cache);
        }
    }

    @Override
    public void stop(ComponentContext context) {
        for (CacheManagement cache : caches.values()) {
            cache.stop();
        }
        caches.clear();
    }

    protected void maybeStart(String name) {
        if (!Framework.getRuntime().getComponentManager().isStarted()) {
            return;
        }
        CacheManagement cache = caches.get(name);
        if (cache != null) {
            cache.stop();
        }
        cache = registry.getCacheDescriptor(name).newInstance();
        cache.start();
        caches.put(name, cache);
    }

    // --------------- API ---------------

    @Override
    public Cache getCache(String name) {
        return caches.get(name);
    }

}
