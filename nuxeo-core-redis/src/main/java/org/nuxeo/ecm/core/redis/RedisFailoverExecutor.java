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

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class RedisFailoverExecutor implements RedisExecutor {

    protected final long timeout;

    protected final RedisExecutor executor;

    public RedisFailoverExecutor(long timeout, RedisExecutor base) {
        this.timeout = timeout;
        executor = base;
    }

    @Override
    public <T> T execute(RedisCallable<T> callable) throws IOException,
            JedisException {
        long end = System.currentTimeMillis() + timeout;
        do {
            try {
                return executor.execute(callable);
            } catch (JedisConnectionException cause) {
                if (end >= System.currentTimeMillis()) {
                    throw cause;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (true);
    }

}
