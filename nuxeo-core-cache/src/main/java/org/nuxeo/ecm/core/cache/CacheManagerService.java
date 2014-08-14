/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Maxime Hilaire
 *
 * $Id: MultiDirectoryFactory.java 29587 2008-01-23 21:52:30Z jcarsique $
 */

package org.nuxeo.ecm.core.cache;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author Maxime Hilaire
 */
public class CacheManagerService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            CacheManagerService.class.getName());

    private static final Log log = LogFactory.getLog(CacheManagerService.class);

    protected static CacheManagerRegistry cacheRegistry;

    public CacheManager getCacheManager(String name) {
        return cacheRegistry.getCacheManager(name);
    }

    @Override
    public void activate(ComponentContext context) {
        cacheRegistry = new CacheManagerRegistry();
        log.info("CacheManagerService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        
        log.info("CacheManagerService deactivated");
        if (cacheRegistry != null) {
            cacheRegistry.removeAllCacheManager();
        }
        cacheRegistry = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            CacheManagerDescriptor descriptor = (CacheManagerDescriptor) contrib;
            cacheRegistry.addContribution(descriptor);
        }
    }

    @Override
    public void unregisterExtension(Extension extension)
            throws RuntimeException {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            CacheManagerDescriptor descriptor = (CacheManagerDescriptor) contrib;
            cacheRegistry.removeContribution(descriptor);
        }
    }


    public List<CacheManager> getCacheManagers() {
        return new ArrayList<CacheManager>(cacheRegistry.getCacheManagers());
    }

}
