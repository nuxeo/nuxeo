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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

@XObject("sentinel")
public class RedisSentinelDescriptor extends RedisPoolDescriptor {

    @XNodeList(value = "host", type = RedisHostDescriptor[].class, componentType = RedisHostDescriptor.class)
    public RedisHostDescriptor[] hosts = new RedisHostDescriptor[0];

    @XNode("master")
    public String master = "master";

    @XNode("failoverTimeout")
    public int failoverTimeout = 300;

    @Override
    public RedisExecutor newExecutor() throws RuntimeException {
        RedisExecutor base = new RedisPoolExecutor(new JedisSentinelPool(master, toSentinels(hosts),
                new JedisPoolConfig(), timeout, password, database));
        return new RedisFailoverExecutor(failoverTimeout, base);
    }

    protected Set<String> toSentinels(RedisHostDescriptor[] hosts) {
        Set<String> sentinels = new HashSet<String>();
        for (RedisHostDescriptor host : hosts) {
            sentinels.add(host.name + ":" + host.port);
        }
        return sentinels;
    }
}
