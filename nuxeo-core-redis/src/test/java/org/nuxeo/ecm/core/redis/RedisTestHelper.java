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
package org.nuxeo.ecm.core.redis;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.redis.RedisConfigurationDescriptor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

/**
 * This defines system properties that can be used to run Redis tests with a
 * given Redis host configured, independently of XML configuration.
 *
 * @since 5.8
 */
public class RedisTestHelper {

    public static final String REDIS_HOST_PROP = "nuxeo.test.redis.host";

    public static final String REDIS_PORT_PROP = "nuxeo.test.redis.port";

    public static final String REDIS_PREFIX_PROP = "nuxeo.test.redis.prefix";

    // SET THIS TO RUN REDIS TESTS
    public static final String REDIS_HOST_DEFAULT = null;

    public static final String REDIS_PORT_DEFAULT = null;

    public static final String REDIS_PREFIX_DEFAULT = null;

    static {
        setPropertyDefault(REDIS_HOST_PROP, REDIS_HOST_DEFAULT);
        setPropertyDefault(REDIS_PORT_PROP, REDIS_PORT_DEFAULT);
        setPropertyDefault(REDIS_PREFIX_PROP, REDIS_PREFIX_DEFAULT);
    }

    public static String setPropertyDefault(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("")
                || value.equals("${" + name + "}")) {
            if (def == null) {
                System.getProperties().remove(name);
            } else {
                System.setProperty(name, def);
            }
        }
        return value;
    }

    public static String getHost() {
        return System.getProperty(REDIS_HOST_PROP);
    }

    public static int getPort() {
        String port = System.getProperty(REDIS_PORT_PROP);
        return StringUtils.isBlank(port) ? 0 : Integer.parseInt(port);
    }

    public static String getPrefix() {
        return System.getProperty(REDIS_PREFIX_PROP);
    }

    public static RedisConfigurationDescriptor getRedisConfigurationDescriptor() {
        String host = getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }
        RedisConfigurationDescriptor desc = new RedisConfigurationDescriptor();
        desc.host = host;
        desc.port = getPort();
        desc.prefix = getPrefix();
        return desc;
    }

    public static void clearRedis(RedisService redisService) {
        JedisPool jedisPool = redisService.getJedisPool();
        Jedis jedis = jedisPool.getResource();
        try {
            delKeys(redisService.getPrefix(), jedis);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    protected static void delKeys(String prefix, Jedis jedis) {
        Set<String> keys = jedis.keys(prefix + "*");
        Pipeline pipe = jedis.pipelined();
        for (String key : keys) {
            pipe.del(key);
        }
        pipe.sync();
    }

}
