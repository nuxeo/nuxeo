/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import java.io.IOException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

public class RedisPoolExecutor implements RedisExecutor {

    protected final Pool<Jedis> pool;

    protected final String prefix;

    public RedisPoolExecutor(Pool<Jedis> pool, String prefix) {
        this.pool = pool;
        this.prefix = prefix;
    }

    @Override
    public <T> T execute(RedisCallable<T> callable) throws IOException,
            JedisException {
        Jedis jedis = pool.getResource();
        boolean brokenResource = false;
        try {
            callable.jedis = jedis;
            callable.prefix = prefix;
            return callable.call();
        } catch (JedisConnectionException cause) {
            brokenResource = true;
            throw cause;
        } catch (Exception cause) {
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(
                    "Caught error in redis invoke, wrapping it", cause);
        } finally {
            if (brokenResource) {
                pool.returnBrokenResource(jedis);
            } else {
                pool.returnResource(jedis);
            }
        }

    }

}
