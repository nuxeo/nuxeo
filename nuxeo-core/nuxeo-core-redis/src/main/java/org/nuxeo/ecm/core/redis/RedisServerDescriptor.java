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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 * Descriptor for a Redis configuration.
 *
 * @since 5.8
 */
@XObject("server")
public class RedisServerDescriptor extends RedisPoolDescriptor {

    @XNode("hosts")
    public RedisHostDescriptor[] hosts = new RedisHostDescriptor[0];

    @XNode("host")
    public void setHost(String name) {
        if (hosts.length == 0) {
            hosts = new RedisHostDescriptor[] { new RedisHostDescriptor(name,
                    Protocol.DEFAULT_PORT) };
        } else {
            hosts[0].name = name;
        }
    }

    @XNode("port")
    public void setHost(int port) {
        if (hosts.length == 0) {
            hosts = new RedisHostDescriptor[] { new RedisHostDescriptor(
                    "localhost", port) };
        } else {
            hosts[0].port = port;
        }
    }

    public RedisHostDescriptor selectHost() {
        for (RedisHostDescriptor host : hosts) {
            if (canConnect(host.name, host.port)) {
                return host;
            }
        }
        throw new NuxeoException("Cannot connect to jedis hosts");
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

    @Override
    public RedisExecutor newExecutor() {
        if (hosts.length == 0) {
            throw new RuntimeException("Missing Redis host");
        }
        if (hosts.length > 1) {
            throw new RuntimeException("Only one host supported");
        }
        RedisHostDescriptor host = selectHost();
        return new RedisPoolExecutor(new JedisPool(new JedisPoolConfig(), host.name, host.port,
                timeout, StringUtils.defaultIfBlank(password, null),
                database));

    }
}
