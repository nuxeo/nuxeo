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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry to register cache
 *
 * @since 6.0
 */
public final class CacheRegistry extends ContributionFragmentRegistry<CacheDescriptor> {

    private static final Log log = LogFactory.getLog(CacheRegistry.class);

    protected final Map<String, CacheDescriptor> configs = new HashMap<>();

    protected final CacheFactory factory;

    public CacheRegistry(CacheFactory factory) {
        this.factory = factory;
    }

    boolean started;

    void start() {
        started = true;
        NuxeoException errors = new NuxeoException("starting caches");
        for (CacheDescriptor config : configs.values()) {
            try {
                config.cache = factory.createCache(config);
            } catch (NuxeoException cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    void stop() {
        started = false;
        NuxeoException errors = new NuxeoException("stopping caches");
        for (CacheDescriptor config : configs.values()) {
            if (config.cache == null) {
                continue;
            }
            try {
                factory.destroyCache(config.cache);
            } catch (NuxeoException cause) {
                errors.addSuppressed(cause);
            } finally {
                config.cache = null;
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    @Override
    public String getContributionId(CacheDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id, CacheDescriptor config, CacheDescriptor newOrigContrib) {
        String name = config.name;
        if (name == null) {
            throw new RuntimeException("The cache name must not be null!");
        }
        if (config.remove) {
            contributionRemoved(id, config);
            return;
        }
        configs.put(name, config);
        if (started) {
            config.cache = factory.createCache(config);
        }
        log.info("cache registered: " + name);
    }

    @Override
    public void contributionRemoved(String id, CacheDescriptor origContrib) {
        String name = origContrib.name;
        CacheDescriptor config = configs.remove(name);
        if (config == null) {
            throw new IllegalStateException("No such cache registered" + name);
        }
        log.info("cache removed: " + name);
        if (config.cache != null) {
            try {
                factory.destroyCache(config.cache);
            } finally {
                config.cache = null;
            }
        }
    }

    @Override
    public CacheDescriptor clone(CacheDescriptor orig) {
        CacheDescriptor clone = factory.createConfig(orig.context, orig.name);
        return CacheDescriptor.class.cast(factory.xmap(clone).load(orig.context, orig.document));
    }

    @Override
    public void merge(CacheDescriptor src, CacheDescriptor dst) {
        factory.merge(src, dst);
    }

    public CacheDescriptor getConfig(String name) {
        return configs.get(name);
    }

}
