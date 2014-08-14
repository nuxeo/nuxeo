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
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @author Maxime Hilaire
 *
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.core.cache"})
public class TestCacheManager {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.core.cache.tests";

    @Inject
    protected CacheManagerService cacheManagerService;

    @Inject
    protected RuntimeHarness harness;

    protected CacheManager cacheManager;

    protected static String CACHEMANAGER_NAME = "default-test-cachemanager";

    @Before
    public void setUp() throws Exception {
        
        
        // Config for the tested bundle
        harness.deployContrib(TEST_BUNDLE,
                "cachemanager-config.xml");

        
    }

    @Test
    public void getCacheManager() {
        cacheManager = cacheManagerService.getCacheManager(CACHEMANAGER_NAME);
        Assert.assertNotNull(cacheManager);
    }

    @After
    public void tearDown() throws Exception {
        // cacheManagerService.unregisterExtension();
    }

}
