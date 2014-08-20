/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.cache.redis.test;


import static org.junit.Assume.assumeTrue;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.redis.RedisCacheImpl;
import org.nuxeo.ecm.core.redis.RedisConfigurationDescriptor;
import org.nuxeo.ecm.core.redis.RedisService;
import org.nuxeo.ecm.core.redis.RedisServiceImpl;
import org.nuxeo.ecm.core.redis.RedisTestHelper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * Unit test of cache implementation on tof of redis
 * 
 * since 5.9.6
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.core.cache",
        "org.nuxeo.ecm.core.redis" })
public class TestRedisCacheService extends NXRuntimeTestCase {

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.core.redis.tests";

    protected Cache redisCache;

    @Inject
    protected CacheService cacheService;

    @Inject
    protected RuntimeHarness harness;

    private RedisConfigurationDescriptor redisConfigurationDescriptor;

    protected static String CACHE_NAME = "redis-test-cache";

    @Before
    public void setUp() throws Exception {

        
        
        redisConfigurationDescriptor = RedisTestHelper.getRedisConfigurationDescriptor();
        boolean enabled = redisConfigurationDescriptor != null;
        assumeTrue(enabled);
        RedisServiceImpl redisService = (RedisServiceImpl) Framework.getLocalService(RedisService.class);
        redisService.registerConfiguration(redisConfigurationDescriptor);
        RedisTestHelper.clearRedis(redisService);
        

        // Config for the tested bundle
        harness.deployContrib(TEST_BUNDLE, "OSGI-INF/redis-cache-config.xml");
        
        cacheService = Framework.getLocalService(CacheService.class);
    }

    @Test
    public void getCache() {
        redisCache = (RedisCacheImpl) cacheService.getCache(CACHE_NAME);

        Assert.assertNotNull(redisCache);
        
         redisCache.put("key1", "val1");
        
         String testGet = (String)redisCache.get("key1");
         Assert.assertNotNull(testGet);
         Assert.assertEquals(testGet, "val1");

    }

    @After
    public void tearDown() throws Exception {
        // cacheManagerService.unregisterExtension();
    }

}
