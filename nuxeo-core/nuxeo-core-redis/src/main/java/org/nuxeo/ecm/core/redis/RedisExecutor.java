/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

/**
 * Execute the jedis statement
 *
 * @since 6.0
 */
public interface RedisExecutor {

    public static final RedisExecutor NOOP = new RedisAbstractExecutor() {

        @Override
        public <T> T execute(RedisCallable<T> call) throws JedisException {
            throw new UnsupportedOperationException("No redis executor available");
        }

        @Override
        public Pool<Jedis> getPool() {
            throw new UnsupportedOperationException("No pool available");
        }

    };

    /**
     * Loads the script into Redis.
     *
     * @return the script SHA1
     * @since 8.10
     */
    String scriptLoad(String script) throws JedisException;

    /**
     * Evaluates the script of the given SHA1 with the given keys and arguments.
     * <p>
     * Can reload the script if the Redis instance restarted and the script isn't available anymore.
     *
     * @param sha1 the script SHA1
     * @param keys the keys
     * @param args the arguments
     * @return the SHA1
     * @since 8.10
     */
    Object evalsha(String sha1, List<String> keys, List<String> args) throws JedisException;

    /**
     * Evaluates the script of the given SHA1 with the given keys and arguments.
     * <p>
     * Can reload the script if the Redis instance restarted and the script isn't available anymore.
     *
     * @param sha1 the script SHA1
     * @param keys the keys
     * @param args the arguments
     * @return the SHA1
     * @since 8.10
     */
    Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args) throws JedisException;

    <T> T execute(RedisCallable<T> call) throws JedisException;

    /**
     * Run a subscriber, do not return.
     */
    default void subscribe(JedisPubSub subscriber, String channel) throws JedisException {
        execute(jedis -> {
            jedis.subscribe(subscriber, channel);
            return null;
        });
    }

    Pool<Jedis> getPool();

    /**
     * Start to trace Redis activity only for debug purpose.
     * @since 8.1
     */
    default void startMonitor() {
    }

    /**
     * Stop tracing Redis activity.
     * @since 8.1
     */
    default void stopMonitor() {
    }

}
