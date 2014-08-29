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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.cache.AbstractTestCache;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.redis.RedisTestHelper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Unit test of cache implementation on top of redis
 *
 * since 5.9.6
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, RedisTestHelper.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({  "org.nuxeo.ecm.core.cache","org.nuxeo.ecm.core.redis" })
@LocalDeploy("org.nuxeo.ecm.core.cache:OSGI-INF/redis-cache-config.xml")
public class TestRedisCacheService extends AbstractTestCache{

    protected Cache redisCache;


    @Test
    public void castTest() {
        redisCache = cacheService.getCache(DEFAULT_TEST_CACHE_NAME);
        Assert.assertNotNull(redisCache);
    }


}
