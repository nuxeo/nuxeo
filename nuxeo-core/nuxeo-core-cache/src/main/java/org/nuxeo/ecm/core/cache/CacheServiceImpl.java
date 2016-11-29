/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins
 *
 */

package org.nuxeo.ecm.core.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
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
    public CacheAttributesChecker getCache(String name) {
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
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP != event.id) {
                    return;
                }
                Framework.removeListener(this);
                cacheRegistry.stop();
            }
        });
        cacheRegistry.start();
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
