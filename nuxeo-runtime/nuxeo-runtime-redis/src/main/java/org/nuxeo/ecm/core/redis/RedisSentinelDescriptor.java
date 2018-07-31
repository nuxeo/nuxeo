/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.redis;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

@XObject("sentinel")
public class RedisSentinelDescriptor extends RedisPoolDescriptor {

    @XObject("host")
    public static class RedisHostDescriptor implements Descriptor {

        @XNode("@name")
        public String name;

        @XNode("@port")
        public int port;

        /** Empty constructor. */
        public RedisHostDescriptor() {
        }

        protected RedisHostDescriptor(String name, int port) {
            this.name = name;
            this.port = port;
        }

        @Override
        public String getId() {
            return name;
        }
    }

    @XNodeList(value = "host", type = RedisHostDescriptor[].class, componentType = RedisHostDescriptor.class)
    public RedisHostDescriptor[] hosts = new RedisHostDescriptor[0];

    @XNode("master")
    public String master = "master";

    @XNode("failoverTimeout")
    public int failoverTimeout = 300;

    @Override
    public RedisExecutor newExecutor() throws RuntimeException {
        Set<String> sentinels = new HashSet<>();
        for (RedisHostDescriptor host : hosts) {
            sentinels.add(host.name + ":" + host.port);
        }
        GenericObjectPoolConfig cfg = new JedisPoolConfig();
        Pool<Jedis> sentinel = new JedisSentinelPool(master, sentinels, cfg, timeout, password, database);
        RedisExecutor base = new RedisPoolExecutor(sentinel);
        return new RedisFailoverExecutor(failoverTimeout, base);
    }

}
