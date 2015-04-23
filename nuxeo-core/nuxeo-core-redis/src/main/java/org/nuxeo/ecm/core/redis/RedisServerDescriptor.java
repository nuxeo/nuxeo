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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Descriptor for a Redis configuration.
 *
 * @since 5.8
 */
@XObject("server")
public class RedisServerDescriptor extends RedisPoolDescriptor {

    private static final Log log = LogFactory.getLog(RedisServerDescriptor.class);

    @XNode("host")
    public String host;

    @XNode("port")
    public int port = Protocol.DEFAULT_PORT;

    protected boolean canConnect(String name, int port) {
        try (Jedis jedis = new Jedis(name, port)) {
            if (StringUtils.isNotBlank(password)) {
                jedis.auth(password);
            }
            return canPing(jedis);
        }
    }

    protected boolean canPing(Jedis jedis) {
        try {
            String pong = jedis.ping();
            return "PONG".equals(pong);
        } catch (JedisException cause) {
            log.debug("Exception during ping", cause);
            return false;
        }
    }

    @Override
    public RedisExecutor newExecutor() {
        if (!canConnect(host, port)) {
            throw new NuxeoException("Cannot connect to Jedis host: " + host + ":" + port);
        }
        return new RedisPoolExecutor(new JedisPool(new JedisPoolConfig(), host, port, timeout,
                StringUtils.defaultIfBlank(password, null), database));
    }

}
