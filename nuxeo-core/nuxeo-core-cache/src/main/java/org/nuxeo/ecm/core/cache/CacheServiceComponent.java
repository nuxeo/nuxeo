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

import java.io.IOException;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Cache service implementation to manage nuxeo cache
 *
 * @since 6.0
 */
public class CacheServiceComponent extends DefaultComponent implements CacheService {

    protected final CacheFactoryRegistry factoryRegistry = new CacheFactoryRegistry();

    protected final CacheRegistry cacheRegistry = new CacheRegistry(new CacheFactory() {

        @Override
        public XMap xmap(CacheDescriptor config) {
            return factoryRegistry.select(config.type).factory.xmap(config);
        }

        @Override
        public CacheDescriptor clone(CacheDescriptor config) {
            return factoryRegistry.select(config.type).factory.clone(config);
        }

        @Override
        public void merge(CacheDescriptor src, CacheDescriptor dst) {
            CacheFactory factory = factoryRegistry.select(src.type).factory;
            src = factory.clone(src);
            factory.merge(src, dst);
        }

        @Override
        public Cache createCache(CacheDescriptor cacheConfig) {
            CacheFactoryDescriptor factoryConfig = factoryRegistry.select(cacheConfig.type);
            CacheFactory factory = factoryConfig.factory;
            if (!factory.isConfigType(cacheConfig.getClass())){
                cacheConfig = factory.clone(factoryConfig.context, cacheConfig);
            }
            Cache cache = factory.createCache(cacheConfig);
            return new CacheAttributesChecker(cache);
        }

        @Override
        public void destroyCache(Cache cache) {
            NuxeoException errors = new NuxeoException("Destroying cache " + cache.getName());
            try {
                cache.invalidateAll();
            } catch (IOException cause) {
                errors.addSuppressed(cause);
            } finally {
                try {
                    factoryRegistry.select(cache.getConfig().type).factory.destroyCache(
                            ((CacheAttributesChecker) cache).cache);
                } catch (RuntimeException cause) {
                    errors.addSuppressed(cause);
                }
            }
            if (errors.getSuppressed().length > 0) {
                throw errors;
            }
        }

        @Override
        public boolean isInstanceType(Class<? extends Cache> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConfigType(Class<? extends CacheDescriptor> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CacheDescriptor createConfig(String name) {
            return factoryRegistry.defaultConfig.factory.createConfig(name);
        }

    });

    @Override
    public Cache getCache(String name) {
        return cacheRegistry.configs.get(name).cache;
    }

    @Override
    public void createCacheIfNotExist(String name) {
        if (cacheRegistry.configs.containsKey(name)) {
            return;
        }
        CacheDescriptor config = cacheRegistry.factory.createConfig(name);
        cacheRegistry.addContribution(config);
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
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof CacheDescriptor) {
            cacheRegistry.addContribution((CacheDescriptor) contribution);
        } else if (contribution instanceof CacheFactoryDescriptor) {
            factoryRegistry.addContribution((CacheFactoryDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof CacheDescriptor) {
            cacheRegistry.removeContribution((CacheDescriptor) contribution);
        } else if (contribution instanceof CacheFactoryDescriptor) {
            factoryRegistry.removeContribution((CacheFactoryDescriptor) contribution);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(CacheRegistry.class)) {
            return adapter.cast(cacheRegistry);
        }
        if (adapter.isAssignableFrom(CacheFactoryRegistry.class)) {
            return adapter.cast(factoryRegistry);
        }
        return super.getAdapter(adapter);
    };

}
