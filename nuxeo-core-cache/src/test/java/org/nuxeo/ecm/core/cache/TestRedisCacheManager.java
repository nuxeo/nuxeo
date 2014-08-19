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
 *     mhilaire
 *
 * $Id: TestMultiDirectory.java 30378 2008-02-20 17:37:26Z gracinet $
 */

package org.nuxeo.ecm.core.cache;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Maxime Hilaire
 *
 */


public class TestRedisCacheManager extends NXRuntimeTestCase{

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.core.cache.tests";


    protected CacheManager<String, String> cacheManager;


    private CacheManagerService cacheManagerService;

    protected static String CACHEMANAGER_NAME = "redis-test-cachemanager";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.redis");
        deployBundle("org.nuxeo.ecm.core.cache");
        
        // Config for the tested bundle
        deployContrib(TEST_BUNDLE, "OSGI-INF/redis-cachemanager-config.xml");

        cacheManagerService = Framework.getLocalService(CacheManagerService.class);
    }

    
    @Test
    @SuppressWarnings("unchecked")
    public void getCacheManager() {
        Assert.assertNotNull(cacheManagerService);

    }

    @After
    public void tearDown() throws Exception {
        // cacheManagerService.unregisterExtension();
    }

}
