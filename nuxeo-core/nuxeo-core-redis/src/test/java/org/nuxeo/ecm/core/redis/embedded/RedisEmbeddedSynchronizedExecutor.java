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
package org.nuxeo.ecm.core.redis.embedded;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.redis.RedisAbstractExecutor;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

public class RedisEmbeddedSynchronizedExecutor extends RedisAbstractExecutor {

    protected final RedisExecutor delegate;

    protected final Log log = LogFactory.getLog(RedisEmbeddedSynchronizedExecutor.class);

    public RedisEmbeddedSynchronizedExecutor(RedisExecutor executor) {
        delegate = executor;
    }

    @Override
    public <T> T execute(RedisCallable<T> call) throws JedisException {
        synchronized (this) {
            return delegate.execute(call);
        }
    }

    @Override
    public Pool<Jedis> getPool() {
        return delegate.getPool();
    }

}
