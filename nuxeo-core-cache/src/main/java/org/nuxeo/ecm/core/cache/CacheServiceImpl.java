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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * 
 * Cache service implementation to manage nuxeo cache
 *
 * @since 5.9.6
 */
public class CacheServiceImpl extends DefaultComponent implements
        CacheService {

    public static final ComponentName NAME = new ComponentName(
            CacheServiceImpl.class.getName());

    private static final Log log = LogFactory.getLog(CacheServiceImpl.class);


    protected static CacheRegistry cacheRegistry;

    @Override
    public Cache getCache(String name) {
        return cacheRegistry.getCache(name);
    }

    @Override
    public void activate(ComponentContext context) {
        if (cacheRegistry == null) {
            cacheRegistry = new CacheRegistry();
        }

        log.info("CacheManagerService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {

        log.info("CacheManagerService deactivated");
        if (cacheRegistry != null) {
            cacheRegistry.removeAllCache();
        }
        cacheRegistry = null;
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
