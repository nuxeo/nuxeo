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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

/**
 * Execute the jedis statement
 *
 * @since 6.0
 */
public interface RedisExecutor {

    public static final RedisExecutor NOOP = new RedisExecutor() {

        @Override
        public <T> T execute(RedisCallable<T> call) throws JedisException {
            throw new UnsupportedOperationException("No redis executor available");
        }

        @Override
        public Pool<Jedis> getPool() {
            throw new UnsupportedOperationException("No pool available");
        }

    };

    <T> T execute(RedisCallable<T> call) throws JedisException;

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
