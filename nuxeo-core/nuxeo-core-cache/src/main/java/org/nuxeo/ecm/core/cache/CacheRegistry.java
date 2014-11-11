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
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry to register cache
 *
 * @since 5.9.6
 */
public final class CacheRegistry extends
        ContributionFragmentRegistry<CacheDescriptor> {

    private static final Log log = LogFactory.getLog(CacheRegistry.class);

    // map of cache
    protected final Map<String, CacheDescriptor> caches = new HashMap<String, CacheDescriptor>();

    @Override
    public String getContributionId(CacheDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id, CacheDescriptor descriptor,
            CacheDescriptor newOrigContrib) {
        String name = descriptor.name;
        if (name == null) {
            throw new RuntimeException("The cache name must not be null!");
        }
        if (descriptor.remove) {
            contributionRemoved(id, descriptor);
            return;
        }

        if (caches.containsKey(name)) {
            throw new IllegalStateException(
                    String.format(
                            "Another cache has already been registered for the given name %s",
                            name));
        }

        caches.put(name, descriptor);
        log.info("cache registered: " + name);
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

    public CacheAttributesChecker getCache(String name) {
        if(caches.containsKey(name))
        {
            return caches.get(name).cacheChecker;
        }
        return null;
    }

    public List<CacheAttributesChecker> getCaches() {
        List<CacheAttributesChecker> res = new ArrayList<CacheAttributesChecker>(
                caches.size());
        for (CacheDescriptor desc : caches.values()) {
            res.add(desc.cacheChecker);
        }
        return res;
    }

    public void start() {
        RuntimeException errors = new RuntimeException(
                "Cannot start caches, check suppressed error");
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
    }

    public void stop() {
        RuntimeException errors = new RuntimeException(
                "Cannot stop caches, check suppressed error");
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
    }

}
