/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

@XObject("sentinel")
public class RedisSentinelDescriptor extends RedisServerDescriptor {

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
