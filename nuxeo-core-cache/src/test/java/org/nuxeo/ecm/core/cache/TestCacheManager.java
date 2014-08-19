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
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.common.cache.Cache;
import com.google.inject.Inject;

/**
 * @author Maxime Hilaire
 *
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.core.cache" })
public class TestCacheManager<T> {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.core.cache.tests";

    @Inject
    protected CacheManagerService cacheManagerService;

    @Inject
    protected RuntimeHarness harness;

    protected CacheManager<String, String> cacheManager;

    protected static String CACHEMANAGER_NAME = "default-test-cachemanager";

    @Before
    public void setUp() throws Exception {

        // Config for the tested bundle
        harness.deployContrib(TEST_BUNDLE, "OSGI-INF/cachemanager-config.xml");

    }

    @SuppressWarnings("unchecked")
    @Test
    public void getCacheManager() {

        if (cacheManagerService.getCacheManager(CACHEMANAGER_NAME) instanceof CacheManagerImpl<?, ?>) {
            cacheManager = (CacheManagerImpl<String, String>) cacheManagerService.getCacheManager(CACHEMANAGER_NAME);
        }

        Cache<String, String> cache = null;

        cache = ((CacheManagerImpl<String, String>) cacheManager).getCache();

        Assert.assertNotNull(cacheManager);

        if (cacheManager instanceof CacheManagerImpl<?, ?>) {
            cache = ((CacheManagerImpl<String, String>) cacheManager).getCache();
        }
        cacheManager.put("test", "toto");
        Assert.assertNotNull(cache);
        cache.put("test2", "toto2");
        
        String testGet = cacheManager.get("test2");
        Assert.assertNotNull(testGet);

    }

    @After
    public void tearDown() throws Exception {
        // cacheManagerService.unregisterExtension();
    }

}
