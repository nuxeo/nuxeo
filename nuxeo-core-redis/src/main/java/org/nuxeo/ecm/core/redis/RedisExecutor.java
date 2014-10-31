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
        public <T> T execute(RedisCallable<T> call) throws IOException,
                JedisException {
            throw new UnsupportedOperationException("No redis executor available");
        }

        @Override
        public Pool<Jedis> getPool() {
          throw new UnsupportedOperationException("No pool available");
        }

    };

    <T> T execute(RedisCallable<T> call) throws IOException, JedisException;

    Pool<Jedis> getPool();
}
