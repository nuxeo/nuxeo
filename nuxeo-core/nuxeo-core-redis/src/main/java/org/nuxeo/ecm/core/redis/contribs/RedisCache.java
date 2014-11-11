/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
        namespace = Framework.getService(RedisAdmin.class).namespace("cache",name);
    }

    protected String formatKey(String key) {
        return namespace.concat(key);
    }

    protected Serializable deserializeValue(byte[] workBytes)
            throws IOException {
        if (workBytes == null) {
            return null;
        }
        InputStream bain = new ByteArrayInputStream(workBytes);
        ObjectInputStream in = new ObjectInputStream(bain);
        try {
            return (Serializable) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected static byte[] bytes(String string) {
        try {
            return string.getBytes(UTF_8);
        } catch (IOException e) {
            // cannot happen for UTF-8
            throw new RuntimeException(e);
        }
    }

    @Override
    public Serializable get(final String key) throws IOException {
        return executor.execute(new RedisCallable<Serializable>() {

            @Override
            public Serializable call(Jedis jedis) throws Exception {
                return deserializeValue(jedis.get(bytes(formatKey(key))));
            }
        });

    }

    protected byte[] serializeValue(Serializable value) throws IOException {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baout);
        out.writeObject(value);
        out.flush();
        out.close();
        return baout.toByteArray();
    }

    @Override
    public void invalidate(final String key) throws IOException {
        executor.execute(new RedisCallable<Void>() {

            @Override
            public Void call(Jedis jedis) throws Exception {
                jedis.del(new String[] { formatKey(key)});
                return null;
            }
        });
    }

    @Override
    public void invalidateAll() throws IOException {
        Framework.getService(RedisAdmin.class).clear(formatKey("*"));
    }

    @Override
    public void put(final String key, final Serializable value)
            throws IOException {
        executor.execute(new RedisCallable<Void>() {

            @Override
            public Void call(Jedis jedis) throws Exception {
                byte[] bkey = bytes(formatKey(key));
                jedis.set(bkey, serializeValue(value));
                // Redis set in second ttl but descriptor set as mn
                int ttlKey = ttl * 60;
                jedis.expire(bkey, ttlKey);
                return null;
            }
        });
    }

}
