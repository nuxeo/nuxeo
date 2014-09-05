package org.nuxeo.ecm.core.redis.embedded;

import org.nuxeo.ecm.core.redis.RedisConfigurationDescriptor;
import org.nuxeo.ecm.core.redis.RedisFailoverExecutor;
import org.nuxeo.ecm.core.redis.RedisPoolExecutor;

import redis.clients.jedis.JedisPoolConfig;

public class RedisEmbeddedConfigurationDescriptor extends
        RedisConfigurationDescriptor {

    @Override
    public boolean start() {
        pool = new RedisEmbeddedPool(new JedisPoolConfig());
        executor = new RedisFailoverExecutor(failoverTimeout, new RedisPoolExecutor(pool, prefix));
        return true;
    }

    @Override
    public void stop() {
        try {
            pool.destroy();
        } finally {
            pool = null;
            executor = null;
        }
    }
}
