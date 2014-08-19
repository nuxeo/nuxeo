/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * Implementation of the Redis Service holding the configured Jedis pool.
 *
 * @since 5.8
 */
public class RedisServiceImpl extends DefaultComponent implements RedisService {

    private static final Log log = LogFactory.getLog(RedisServiceImpl.class);

    public static final String DEFAULT_PREFIX = "nuxeo:";

    protected RedisConfigurationDescriptor redisConfigurationDescriptor;

    protected JedisPool jedisPool;

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            RedisConfigurationDescriptor desc = (RedisConfigurationDescriptor) contrib;
            if (!desc.disabled) {
                registerConfiguration(desc);
            } else {
                unregisterConfiguration(desc);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            RedisConfigurationDescriptor desc = (RedisConfigurationDescriptor) contrib;
            if (!desc.disabled) {
                unregisterConfiguration(desc);
            }
        }
    }

    public void registerConfiguration(RedisConfigurationDescriptor desc) {
        log.info("Registering Redis configuration");
        if (StringUtils.isBlank(desc.host)) {
            throw new RuntimeException("Missing Redis host");
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        String host = desc.host;
        String password = StringUtils.defaultIfBlank(desc.password, null);
        int port = desc.port == 0 ? Protocol.DEFAULT_PORT : desc.port;
        int timeout = desc.timeout == 0 ? Protocol.DEFAULT_TIMEOUT
                : desc.timeout;
        int database = desc.database == 0 ? Protocol.DEFAULT_DATABASE
                : desc.database;
        redisConfigurationDescriptor = desc;
        jedisPool = new JedisPool(poolConfig, host, port, timeout, password,
                database);
    }

    public void unregisterConfiguration(RedisConfigurationDescriptor desc) {
        log.info("Unregistering Redis configuration");
        if (jedisPool != null) {
            jedisPool.destroy();
            jedisPool = null;
            redisConfigurationDescriptor = null;
        }
    }

    @Override
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    @Override
    public String getPrefix() {
        if (redisConfigurationDescriptor == null) {
            return null;
        }
        String prefix = redisConfigurationDescriptor.prefix;
        if ("NULL".equals(prefix)) {
            prefix = "";
        } else if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_PREFIX;
        }
        return prefix;
    }

}
