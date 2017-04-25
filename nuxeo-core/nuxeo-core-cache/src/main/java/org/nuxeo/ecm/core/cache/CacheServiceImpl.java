/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.core.cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

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

    protected final CacheRegistry cacheRegistry = new CacheRegistry();

    /**
     * Contains the names of all caches which have not been registered from an extension
     */
    protected final List<String> autoregisteredCacheNames = new ArrayList<String>();

    @Override
    public Cache getCache(String name) {
        return cacheRegistry.getCache(name);
    }

    @Override
    public void deactivate(ComponentContext context) {
        if (cacheRegistry.caches.size() > 0) {
            Map<String, CacheDescriptor> descriptors = new HashMap<String, CacheDescriptor>(cacheRegistry.caches);
            for (CacheDescriptor desc : descriptors.values()) {
                cacheRegistry.contributionRemoved(desc.name, desc);
                if (!autoregisteredCacheNames.remove(desc.name)) {
                    log.warn("Unregistery leaked contribution " + desc.name);
                }
            }
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        ComponentInstance repositoryComponent = Framework.getRuntime().getComponentInstance(
                "org.nuxeo.ecm.core.repository.RepositoryServiceComponent");
        if (repositoryComponent == null) {
            return super.getApplicationStartedOrder();
        }
        return ((DefaultComponent) repositoryComponent.getInstance()).getApplicationStartedOrder() - 5;
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        cacheRegistry.start();
    }


    @Override
    public void applicationStandby(ComponentContext context, Instant instant) {
        cacheRegistry.stop();
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            CacheDescriptor descriptor = (CacheDescriptor) contrib;
            registerCache(descriptor);
        }
    }

    public void registerCache(CacheDescriptor descriptor) {
        cacheRegistry.addContribution(descriptor);
    }

    @Override
    public void registerCache(String name, int maxSize, int timeout) {
        CacheDescriptor desc;
        if (cacheRegistry.caches.get(DEFAULT_CACHE_ID) != null) {
            desc = new CacheDescriptor(cacheRegistry.caches.get(DEFAULT_CACHE_ID));
        } else {
            desc = new CacheDescriptor();
        }
        desc.name = name;
        desc.ttl = timeout;
        desc.options.put("maxSize", String.valueOf(maxSize));
        if (cacheRegistry.caches.get(name) == null) {
            registerCache(desc);
            autoregisteredCacheNames.add(name);
        } else {
            CacheDescriptor oldDesc = cacheRegistry.caches.get(name);
            cacheRegistry.merge(oldDesc, desc);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws RuntimeException {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            CacheDescriptor descriptor = (CacheDescriptor) contrib;
            cacheRegistry.removeContribution(descriptor);
        }
    }

    public void unregisterCache(CacheDescriptor descriptor) {
        cacheRegistry.removeContribution(descriptor);
    }


    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(CacheRegistry.class)) {
            return adapter.cast(cacheRegistry);
        }
        return super.getAdapter(adapter);
    }
}
