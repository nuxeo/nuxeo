/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work.redis;

import static org.junit.Assume.assumeTrue;

import java.util.Set;

import org.junit.After;
import org.nuxeo.ecm.core.redis.RedisConfigurationDescriptor;
import org.nuxeo.ecm.core.redis.RedisService;
import org.nuxeo.ecm.core.redis.RedisServiceImpl;
import org.nuxeo.ecm.core.work.WorkManagerTest;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

/**
 * Test of the WorkManager using Redis. Does not run if no Redis is configured
 * through the properties of {@link RedisTestHelper}.
 *
 * @since 5.8
 */
public class RedisWorkManagerTest extends WorkManagerTest {

    protected RedisConfigurationDescriptor redisConfigurationDescriptor;

    @Override
    public boolean persistent() {
        return true;
    }

    @Override
    protected void doDeploy() throws Exception {
        super.doDeploy();
        deployContrib("org.nuxeo.ecm.core.event.test",
                "test-workmanager-redis-config.xml");
        redisConfigurationDescriptor = RedisTestHelper.getRedisConfigurationDescriptor();
        boolean enabled = redisConfigurationDescriptor != null;
        assumeTrue(enabled);
        RedisServiceImpl redisService = (RedisServiceImpl) Framework.getLocalService(RedisService.class);
        redisService.registerConfiguration(redisConfigurationDescriptor);
        clearRedis(redisService);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        RedisServiceImpl redisService = (RedisServiceImpl) Framework.getLocalService(RedisService.class);
        super.tearDown();
        redisService.unregisterConfiguration(redisConfigurationDescriptor);
    }

    protected void clearRedis(RedisService redisService) {
        JedisPool jedisPool = redisService.getJedisPool();
        Jedis jedis = jedisPool.getResource();
        try {
            delKeys(redisService.getPrefix(), jedis);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    protected void delKeys(String prefix, Jedis jedis) {
        Set<String> keys = jedis.keys(prefix + "*");
        Pipeline pipe = jedis.pipelined();
        for (String key : keys) {
            pipe.del(key);
        }
        pipe.sync();
    }

}
