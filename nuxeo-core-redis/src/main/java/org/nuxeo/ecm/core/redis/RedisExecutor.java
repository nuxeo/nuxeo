package org.nuxeo.ecm.core.redis;

import java.io.IOException;

import redis.clients.jedis.exceptions.JedisException;

public interface RedisExecutor {

    /**
     * Invoke the jedis statement
     *
     * @since 5.9.6
     */
    <T> T execute(RedisCallable<T> call) throws IOException, JedisException;
}
