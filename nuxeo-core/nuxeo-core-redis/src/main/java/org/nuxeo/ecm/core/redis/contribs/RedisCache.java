/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 */

package org.nuxeo.ecm.core.redis.contribs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.cache.AbstractCache;
import org.nuxeo.ecm.core.cache.CacheDescriptor;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.runtime.api.Framework;

import redis.clients.jedis.Jedis;

/**
 * Cache implementation on top of Redis
 *
 * @since 6.0
 */
public class RedisCache extends AbstractCache {

    protected static final String UTF_8 = "UTF-8";

    protected static final Log log = LogFactory.getLog(RedisCache.class);

    protected final RedisExecutor executor;

    protected final String namespace;

    public RedisCache(CacheDescriptor desc) {
        super(desc);
        executor = Framework.getService(RedisExecutor.class);
        namespace = Framework.getService(RedisAdmin.class).namespace("cache", name);
    }

    protected String formatKey(String key) {
        return namespace.concat(key);
    }

    protected Serializable deserializeValue(byte[] workBytes) throws IOException {
        if (workBytes == null) {
            return null;
        }

        InputStream bain = new ByteArrayInputStream(workBytes);
        ObjectInputStream in = new ObjectInputStream(bain);
        try {
            return (Serializable) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new NuxeoException(e);
        }
    }

    protected static byte[] bytes(String string) {
        try {
            return string.getBytes(UTF_8);
        } catch (IOException e) {
            // cannot happen for UTF-8
            throw new NuxeoException(e);
        }
    }

    @Override
    public Serializable get(final String key) {
        return executor.execute(new RedisCallable<Serializable>() {
            @Override
            public Serializable call(Jedis jedis) {
                try {
                    return deserializeValue(jedis.get(bytes(formatKey(key))));
                } catch (IOException e) {
                    log.error(e);
                    return null;
                }
            }
        });

    }

    @Override
    public Set<String> keySet() {
        return executor.execute(new RedisCallable<Set<String>>() {
            @Override
            public Set<String> call(Jedis jedis) {
                int offset = namespace.length();
                return jedis.keys(formatKey("*"))
                            .stream()
                            .map(key -> key.substring(offset))
                            .collect(Collectors.toSet());
            }
        });
    }

    protected byte[] serializeValue(Serializable value) throws IOException {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(baout)) {
            out.writeObject(value);
            out.flush();
        }
        return baout.toByteArray();
    }

    @Override
    public void invalidate(final String key) {
        executor.execute(new RedisCallable<Void>() {
            @Override
            public Void call(Jedis jedis) {
                jedis.del(new String[] { formatKey(key) });
                return null;
            }
        });
    }

    @Override
    public void invalidateAll() {
        Framework.getService(RedisAdmin.class).clear(formatKey("*"));
    }

    @Override
    public void put(final String key, final Serializable value) {
        executor.execute(new RedisCallable<Void>() {
            @Override
            public Void call(Jedis jedis) {
                try {
                    byte[] bkey = bytes(formatKey(key));
                    jedis.set(bkey, serializeValue(value));
                    // Redis set in second ttl but descriptor set as mn
                    int ttlKey = ttl * 60;
                    jedis.expire(bkey, ttlKey);
                    return null;
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            }
        });
    }

    @Override
    public boolean hasEntry(final String key) {
        return executor.<Boolean>execute(new RedisCallable<Boolean>() {
            @Override
            public Boolean call(Jedis jedis) {
                return jedis.exists(bytes(formatKey(key)));
            }
        }).booleanValue();
    }

    /**
     * Too expensive to evaluate the # keys redis side, should monitor redis itself
     * @return -1L
     */
    @Override
    public long getSize() {
        return -1L;
    }
}
