/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.ecm.core.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry to register cache
 *
 * @since 6.0
 */
public final class CacheRegistry extends ContributionFragmentRegistry<CacheDescriptor> {

    private static final Log log = LogFactory.getLog(CacheRegistry.class);

    // map of cache
    protected final Map<String, CacheDescriptor> caches = new HashMap<String, CacheDescriptor>();

    protected boolean started;

    @Override
    public String getContributionId(CacheDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id, CacheDescriptor descriptor, CacheDescriptor newOrigContrib) {
        String name = descriptor.name;
        if (name == null) {
            throw new RuntimeException("The cache name must not be null!");
        }
        if (descriptor.remove) {
            contributionRemoved(id, descriptor);
            return;
        }

        if (caches.containsKey(name)) {
            throw new IllegalStateException(String.format(
                    "Another cache has already been registered for the given name %s", name));
        }

        caches.put(name, descriptor);
        log.info("cache registered: " + name);
        if (started) {
            descriptor.start();
        }
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public void contributionRemoved(String id, CacheDescriptor origContrib) {
        String name = origContrib.name;
        CacheDescriptor cache = caches.remove(name);
        if (cache == null) {
            throw new IllegalStateException("No such cache registered" + name);
        }
        try {
            cache.stop();
        } catch (RuntimeException e) {
            log.error(String.format("Error while removing cache '%s'", name), e);
        }
        log.info("cache removed: " + name);
    }

    @Override
    public CacheDescriptor clone(CacheDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(CacheDescriptor src, CacheDescriptor dst) {
        boolean remove = src.remove;
        // keep old remove info: if old contribution was removed, new one
        // should replace the old one completely
        if (remove) {
            dst.remove = remove;
            // don't bother merging
            return;
        }

    }

    public Cache getCache(String name) {
        if (caches.containsKey(name)) {
            return caches.get(name).cache;
        }
        return null;
    }

    public List<Cache> getCaches() {
        List<Cache> res = new ArrayList<>(caches.size());
        for (CacheDescriptor desc : caches.values()) {
            res.add(desc.cache);
        }
        return res;
    }

    /**
     * Invalidate all caches
     * 
     * @since 9.1
     */
    public void invalidateAll() {
        caches.values().forEach(CacheDescriptor::invalidateAll);
    }

    public void start() {
        RuntimeException errors = new RuntimeException("Cannot start caches, check suppressed error");
        for (CacheDescriptor desc : caches.values()) {
            try {
                desc.start();
            } catch (RuntimeException cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
        started = true;
    }

    public void stop() {
        RuntimeException errors = new RuntimeException("Cannot stop caches, check suppressed error");
        for (CacheDescriptor desc : caches.values()) {
            try {
                desc.stop();
            } catch (RuntimeException cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
        started = false;
    }

}
