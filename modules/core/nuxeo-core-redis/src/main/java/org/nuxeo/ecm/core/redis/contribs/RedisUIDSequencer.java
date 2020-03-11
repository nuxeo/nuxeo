/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis.contribs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.uidgen.AbstractUIDSequencer;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.exceptions.JedisException;

/**
 * Redis-based UID generator.
 *
 * @since 7.4
 */
public class RedisUIDSequencer extends AbstractUIDSequencer {

    protected static final Log log = LogFactory.getLog(RedisUIDSequencer.class);

    protected String namespace;

    @Override
    public void init() {
        RedisAdmin redisAdmin = Framework.getService(RedisAdmin.class);
        namespace = redisAdmin.namespace("counters");
    }

    @Override
    public void dispose() {
    }

    @Override
    public void initSequence(String key, long id) {
        RedisExecutor executor = Framework.getService(RedisExecutor.class);
        try {
            executor.execute(jedis -> jedis.set(namespace + key, String.valueOf(id)));
        } catch (JedisException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public long getNextLong(String key) {
        RedisExecutor executor = Framework.getService(RedisExecutor.class);
        try {
            return executor.execute(jedis -> jedis.incr(namespace + key));
        } catch (JedisException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public List<Long> getNextBlock(String key, int blockSize) {
        List<Long> ret = new ArrayList<>(blockSize);
        if (blockSize == 1) {
            ret.add(getNextLong(key));
            return ret;
        }
        RedisExecutor executor = Framework.getService(RedisExecutor.class);
        long last;
        try {
            last = executor.execute(jedis -> jedis.incrBy(namespace + key, blockSize));
        } catch (JedisException e) {
            throw new NuxeoException(e);
        }
        for (int i = blockSize - 1; i >= 0; i--) {
            ret.add(last - i);
        }
        return ret;
    }
}
