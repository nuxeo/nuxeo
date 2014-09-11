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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

/**
 * Descriptor for a Redis configuration.
 *
 * @since 5.8
 */
@XObject("redis")
public class RedisConfigurationDescriptor {

    @XNode("@disabled")
    public boolean disabled = false;

    @XNode("prefix")
    public String prefix = "nuxeo:";

    @XNode("password")
    public String password;

    @XNode("database")
    public int database = Protocol.DEFAULT_DATABASE;

    @XNode("timeout")
    public int timeout = Protocol.DEFAULT_TIMEOUT;

    @XNode("hosts")
    public RedisConfigurationHostDescriptor[] hosts = new RedisConfigurationHostDescriptor[0];

    @XNode("master")
    public String master;

    @XNode("failoverTimeout")
    public long failoverTimeout = 3000;

    @XNode("host")
    public void setHost(String name) {
        if (hosts.length == 0) {
            hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                    name, Protocol.DEFAULT_PORT) };
        } else {
            hosts[0].name = name;
        }
    }

    @XNode("port")
    public void setHost(int port) {
        if (hosts.length == 0) {
            hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                    "localhost", port) };
        } else {
            hosts[0].port = port;
        }
    }

    protected boolean isSentinel() {
        return StringUtils.isNotBlank(master);
    }

    protected Pool<Jedis> pool;

    protected RedisExecutor executor;

    public boolean start() {
        if (hosts.length == 0) {
            throw new RuntimeException("Missing Redis host");
        }
        if (!canConnect()) {
            LogFactory.getLog(RedisConfigurationDescriptor.class).info(
                    "Disabling redis, cannot connect to server");
            return false;
        }
        if (isSentinel()) {
            return activateSentinel();
        }
        return activateServer();
    }

    protected boolean activateServer() {
        pool = new JedisPool(new JedisPoolConfig(), hosts[0].name,
                hosts[0].port, timeout, StringUtils.defaultIfBlank(password,
                        null), database);
        executor = new RedisPoolExecutor(pool);
        return true;
    }

    protected boolean activateSentinel() throws RuntimeException {
        try {
            pool = new JedisSentinelPool(master, toSentinels(hosts),
                    new JedisPoolConfig(), timeout, StringUtils.defaultIfBlank(
                            password, null), database);
            executor = new RedisFailoverExecutor(failoverTimeout,
                    new RedisPoolExecutor(pool));
        } catch (Exception cause) {
            throw new RuntimeException("Cannot connect to redis", cause);
        }
        return true;
    }

    protected Set<String> toSentinels(RedisConfigurationHostDescriptor[] hosts) {
        Set<String> sentinels = new HashSet<String>();
        for (RedisConfigurationHostDescriptor host : hosts) {
            sentinels.add(host.name + ":" + host.port);
        }
        return sentinels;
    }

    protected boolean canConnect() {
        for (RedisConfigurationHostDescriptor host : hosts) {
            if (canConnect(host.name, host.port)) {
                return true;
            }
        }
        return false;
    }

    protected boolean canConnect(String name, int port) {
        try (Jedis jedis = new Jedis(name, port)) {
            return canPing(jedis);
        }
    }

    protected boolean canPing(Jedis jedis) {
        try {
            String pong = jedis.ping();
            return "PONG".equals(pong);
        } catch (Exception cause) {
            return false;
        }
    }

    public void stop() {
        if (pool == null) {
            return;
        }
        try {
            pool.destroy();
        } finally {
            pool = null;
        }
    }
}
