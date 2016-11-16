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

import org.nuxeo.ecm.core.redis.retry.ExponentialBackofDelay;
import org.nuxeo.ecm.core.redis.retry.Retry;
import org.nuxeo.ecm.core.redis.retry.Retry.ContinueException;
import org.nuxeo.ecm.core.redis.retry.Retry.FailException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.util.Pool;

public class RedisFailoverExecutor extends RedisAbstractExecutor {

    protected final int timeout;

    protected final RedisExecutor executor;

    public RedisFailoverExecutor(int timeout, RedisExecutor base) {
        this.timeout = timeout;
        executor = base;
    }

    @Override
    public <T> T execute(final RedisCallable<T> callable) throws JedisConnectionException {
        try {
            return new Retry().retry(new Retry.Block<T>() {

                @Override
                public T retry() throws ContinueException, FailException {
                    try {
                        return executor.execute(callable);
                    } catch (JedisConnectionException cause) {
                        throw new Retry.ContinueException(cause);
                    }
                }

            }, new ExponentialBackofDelay(1, timeout));
        } catch (FailException cause) {
            throw new JedisConnectionException("Cannot reconnect to jedis ..", cause);
        }
    }

    @Override
    public Pool<Jedis> getPool() {
        return executor.getPool();
    }

}
