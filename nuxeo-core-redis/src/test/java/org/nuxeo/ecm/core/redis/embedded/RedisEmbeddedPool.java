package org.nuxeo.ecm.core.redis.embedded;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class RedisEmbeddedPool extends Pool<Jedis> {
    public RedisEmbeddedPool(GenericObjectPoolConfig config) {
        super(config, new ReddisEmbeddedFactory());
    }

}
