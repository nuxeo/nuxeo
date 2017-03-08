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

    /**
     * Runs a subscriber to the given patterns.
     *
     * @param subscriber the subscriber
     * @param patterns the channel patterns
     * @since 9.1
     */
    default void psubscribe(JedisPubSub subscriber, String... patterns) throws JedisException {
        execute(jedis -> {
            jedis.psubscribe(subscriber, patterns);
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
