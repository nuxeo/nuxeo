/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis.embedded;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;


public class RedisEmbeddedTraceExecutor implements RedisExecutor {

    protected final RedisExecutor delegate;

    protected final Log log = LogFactory.getLog(RedisEmbeddedTraceExecutor.class);

    public RedisEmbeddedTraceExecutor(RedisExecutor executor) {
        delegate = executor;
    }

    @Override
    public <T> T execute(RedisCallable<T> call) throws IOException,
            JedisException {
        log.trace("Executing " + call, new Throwable("redis call stack trace"));
        return delegate.execute(call);
    }

    @Override
    public Pool<Jedis> getPool() {
        return delegate.getPool();
    }

}
