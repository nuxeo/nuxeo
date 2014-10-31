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
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Cache service implementation to manage nuxeo cache
 *
 * @since 6.0
 */
public class CacheServiceImpl extends DefaultComponent implements CacheService {

    public static final ComponentName NAME = new ComponentName(
            CacheServiceImpl.class.getName());

    private static final Log log = LogFactory.getLog(CacheServiceImpl.class);

    protected final CacheRegistry cacheRegistry = new CacheRegistry();

    @Override
    public CacheAttributesChecker getCache(String name) {
        return cacheRegistry.getCache(name);
    }

    @Override
    public void deactivate(ComponentContext context) {
        if (cacheRegistry.caches.size() > 0) {
            Map<String,CacheDescriptor> descriptors = new HashMap<String, CacheDescriptor>(cacheRegistry.caches);
            for (CacheDescriptor desc : descriptors.values()) {
                log.warn("Unregistery leaked contribution " + desc.name);
                cacheRegistry.contributionRemoved(desc.name, desc);
            }
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (RuntimeServiceEvent.RUNTIME_ABOUT_TO_START != event.id) {
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
            cacheRegistry.addContribution(descriptor);
        }
    }

    @Override
    public void unregisterExtension(Extension extension)
            throws RuntimeException {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            CacheDescriptor descriptor = (CacheDescriptor) contrib;
            cacheRegistry.removeContribution(descriptor);
        }
    }

}
