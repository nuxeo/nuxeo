/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

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

    @XNode("host")
    public String host;

    @XNode("port")
    public int port = Protocol.DEFAULT_PORT;

    @XNode("failoverTimeout")
    public int failoverTimeout = 300;

    @Override
    public RedisExecutor newExecutor() {
        try (Jedis jedis = new Jedis(host, port)) {
            if (StringUtils.isNotBlank(password)) {
                jedis.auth(password);
            }
            String pong = jedis.ping();
            if (!"PONG".equals(pong)) {
                throw new RuntimeException("Cannot connect to Redis host: " + host + ":" + port);
            }
        }

        JedisPoolConfig conf = new JedisPoolConfig();
        conf.setMaxTotal(maxTotal);
        conf.setMaxIdle(maxIdle);
        RedisExecutor base = new RedisPoolExecutor(new JedisPool(conf, host, port, timeout,
                StringUtils.defaultIfBlank(password, null), database));
        return new RedisFailoverExecutor(failoverTimeout, base);
    }

}
