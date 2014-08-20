/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Registry to register cache
 * 
 * @since 5.9.6
 */
public final class CacheRegistry extends
        ContributionFragmentRegistry<CacheDescriptor> {

    private static final Log log = LogFactory.getLog(CacheRegistry.class);

    // cache of cacheManager
    protected Map<String, Cache> caches = new HashMap<String, Cache>();

    
    
    @Override
    public String getContributionId(CacheDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id,
            CacheDescriptor descriptor,
            CacheDescriptor newOrigContrib) {
        String name = descriptor.name;
        if (descriptor.remove) {
            contributionRemoved(id, descriptor);
        } else {
            Class<?> klass = descriptor.getImplClass();
            if (klass == null) {
                throw new RuntimeException(String.format(
                        "No class specified for the cacheManager ",
                        descriptor.name));
            } else {
                AbstractCache cache = null;
                try {
                    cache = (AbstractCache) klass.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("Failed to instantiate class "
                            + klass, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to instantiate class "
                            + klass, e);
                }

                cache.setName(name);
                cache.setMaxSize(descriptor.maxSize);
                cache.setConcurrencyLevel(descriptor.concurrencyLevel);
                cache.setTtl(descriptor.ttl);

                if (caches.containsKey(name)) {
                    log.warn(String.format(
                            "Another cacheManager has already been registered for the given name %s, the cache will be overriden",
                            name));
                    // TODO : destroy/fire event to remove the former instance
                    // cache
                } else {
                    log.info("CacheManager registered: " + name);
                }
                addListener(cache);
                caches.put(name, cache);
            }
        }
    }
    
    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public void contributionRemoved(String id,
            CacheDescriptor origContrib) {
        String cacheManagerName = origContrib.name;
        Cache cache = caches.remove(cacheManagerName);
        if (cache != null) {
            try {
                // TODO : destroy/fire event to remove the former instance cache
                // cacheManager.shutdown();
                
                
            } catch (RuntimeException e) {
                log.error(String.format(
                        "Error while removing cacheManager '%s'",
                        cacheManagerName), e);
            }
        }
        log.info("CacheManager removed: " + cacheManagerName);
    }

    protected void addListener (Cache cache)
    {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.addListener(Cache.CACHE_TOPIC,
                (EventListener) cache);
    }
    protected void removeListener(Cache cache)
    {
        EventService eventService = Framework.getLocalService(EventService.class);
        if (eventService != null) {
            eventService.removeListener(Cache.CACHE_TOPIC,
                    (EventListener) cache);
        }
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
    
    public void removeAllCache()
    {
        for (Cache cache : caches.values()) {
            removeListener(cache);
        }
        caches = new HashMap<String, Cache>();
    }

    public Cache getCache(String name) {
        return caches.get(name);
    }

    public List<Cache> getCacheManagers() {
        List<Cache> res = new ArrayList<Cache>(
                caches.size());
        res.addAll(caches.values());
        return res;
    }

}
