/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import org.nuxeo.ecm.core.redis.retry.ExponentialBackofDelay;
import org.nuxeo.ecm.core.redis.retry.Retry;
import org.nuxeo.ecm.core.redis.retry.Retry.ContinueException;
import org.nuxeo.ecm.core.redis.retry.Retry.FailException;

import org.nuxeo.ecm.core.redis.retry.SimpleDelay;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
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
        // Retry the operation with an exponential backoff limited by a configurable timeout
        return executeWithRetryPolicy(callable, new ExponentialBackofDelay(1, timeout));
    }

    @Override
    public void subscribe(JedisPubSub subscriber, String channel) throws JedisConnectionException {
        // Here it is a long running operation, we never give up retry every 2s
        executeWithRetryPolicy(jedis -> {
            jedis.subscribe(subscriber, channel);
            return null;
        }, new SimpleDelay(2000, Integer.MAX_VALUE));
    }

    protected <T> T executeWithRetryPolicy(final RedisCallable<T> callable, Retry.Policy policy) {
        try {
            return new Retry().retry(new Retry.Block<T>() {

                @Override
                public T retry() throws ContinueException, FailException {
                    try {
                        return executor.execute(callable);
                    } catch (JedisConnectionException cause) {
                        throw new ContinueException(cause);
                    }
                }

            }, policy);
        } catch (FailException cause) {
            throw new JedisConnectionException("Cannot reconnect to jedis ..", cause);
        }
    }

    @Override
    public Pool<Jedis> getPool() {
        return executor.getPool();
    }

}
