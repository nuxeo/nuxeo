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

    public static final ComponentName NAME = new ComponentName(CacheServiceImpl.class.getName());

    private static final Log log = LogFactory.getLog(CacheServiceImpl.class);

    protected final CacheRegistry cacheRegistry = new CacheRegistry();

    @Override
    public CacheAttributesChecker getCache(String name) {
        return cacheRegistry.getCache(name);
    }

    @Override
    public void deactivate(ComponentContext context) {
        if (cacheRegistry.caches.size() > 0) {
            Map<String, CacheDescriptor> descriptors = new HashMap<String, CacheDescriptor>(cacheRegistry.caches);
            for (CacheDescriptor desc : descriptors.values()) {
                log.warn("Unregistery leaked contribution " + desc.name);
                cacheRegistry.contributionRemoved(desc.name, desc);
            }
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
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
            registerCache(descriptor);
        }
    }

    public void registerCache(CacheDescriptor descriptor) {
        cacheRegistry.addContribution(descriptor);
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

}
