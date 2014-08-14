/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 *     Maxime Hilaire
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
 * @since 5.6
 */
public class CacheManagerRegistry extends
        ContributionFragmentRegistry<CacheManagerDescriptor> {

    private static final Log log = LogFactory.getLog(CacheManagerRegistry.class);

    // cache of directories
    protected Map<String, CacheManager> cacheManagers = new HashMap<String, CacheManager>();

    @Override
    public String getContributionId(CacheManagerDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id,
            CacheManagerDescriptor descriptor,
            CacheManagerDescriptor newOrigContrib) {
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
                CacheManager cacheManager = null;
                try {
                    cacheManager = (CacheManager) klass.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("Failed to instantiate class "
                            + klass, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to instantiate class "
                            + klass, e);
                }

                cacheManager.setName(name);
                cacheManager.setMaxSize(descriptor.maxSize);
                cacheManager.setConcurrencyLevel(descriptor.concurrencyLevel);
                cacheManager.setTtl(descriptor.ttl);

                if (cacheManagers.containsKey(name)) {
                    log.warn(String.format(
                            "Another cacheManager has already been registered for the given name %s, the cache will be overriden",
                            name));
                    // TODO : destroy/fire event to remove the former instance
                    // cache
                } else {
                    log.info("CacheManager registered: " + name);
                }
                addListener(cacheManager);
                cacheManagers.put(name, cacheManager);
            }
        }
    }

    @Override
    public void contributionRemoved(String id,
            CacheManagerDescriptor origContrib) {
        String cacheManagerName = origContrib.name;
        CacheManager cacheManager = cacheManagers.remove(cacheManagerName);
        if (cacheManager != null) {
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

    protected void addListener (CacheManager cacheManager)
    {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.addListener(CacheManager.CORECACHEMANAGER_TOPIC,
                (EventListener) cacheManager);
    }
    protected void removeListener(CacheManager cacheManager)
    {
        EventService eventService = Framework.getLocalService(EventService.class);
        if (eventService != null) {
            eventService.removeListener(CacheManager.CORECACHEMANAGER_TOPIC,
                    (EventListener) cacheManager);
        }
    }
    @Override
    public CacheManagerDescriptor clone(CacheManagerDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(CacheManagerDescriptor src, CacheManagerDescriptor dst) {
        boolean remove = src.remove;
        // keep old remove info: if old contribution was removed, new one
        // should replace the old one completely
        if (remove) {
            dst.remove = remove;
            // don't bother merging
            return;
        }

    }
    
    public void removeAllCacheManager()
    {
        for (CacheManager cacheManager : cacheManagers.values()) {
            removeListener(cacheManager);
        }
        cacheManagers = new HashMap<String, CacheManager>();
    }

    public CacheManager getCacheManager(String name) {
        return cacheManagers.get(name);
    }

    public List<CacheManager> getCacheManagers() {
        List<CacheManager> res = new ArrayList<CacheManager>(
                cacheManagers.size());
        res.addAll(cacheManagers.values());
        return res;
    }

}
