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

package org.nuxeo.ecm.core.redis;

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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Cache implementation for on top of Redis
 * 
 * @since 5.9.6
 */
public class RedisCacheImpl extends AbstractCache {

    protected static final String UTF_8 = "UTF-8";

    private static final Log log = LogFactory.getLog(RedisCacheImpl.class);

    private RedisService redisService = null;


    protected String prefix;

    /**
     * Prefix for keys, added after the globally configured prefix of the
     * {@link RedisService}.
     */
    public static final String PREFIX = "cache:";

    public RedisCacheImpl() {
        redisService = getRedisService();
    }

    protected RedisService getRedisService() {
        if (redisService == null) {
            redisService = Framework.getLocalService(RedisService.class);
            prefix = redisService.getPrefix() + PREFIX + name + ":";
        }
        return redisService;
    }

    protected Jedis getJedis() {
        RedisService redisService = getRedisService();
        if (redisService == null) {
            return null;
        }
        JedisPool jedisPool = redisService.getJedisPool();
        if (jedisPool == null) {
            return null;
        }
        return jedisPool.getResource();
    }

    protected void closeJedis(Jedis jedis) {
        if(getRedisService().getJedisPool() != null)
        {
            getRedisService().getJedisPool().returnResource(jedis);
        }
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
    public Serializable get(String key) {
        Jedis jedis = getJedis();
        try {
            return deserializeValue(jedis.get(bytes(prefix + key)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally
        {
            closeJedis(jedis);
        }
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
    public void invalidate(String key) {
        Jedis jedis = getJedis();
        try {
            jedis.del(key);
        }
        finally
        {
            closeJedis(jedis);
        }
    }

    @Override
    public void invalidateAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(String key, Serializable value) {
        Jedis jedis = getJedis();
        try {
            byte[] bkey = bytes(prefix + key);
            jedis.set(bkey, serializeValue(value));
            jedis.expire(bkey, ttl);
        } catch (IOException e) {
            log.error(
                    String.format("Could not set value in the cache %s", name),
                    e);
            throw new RuntimeException(e);
        } finally {
            closeJedis(jedis);
        }
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        // TODO Auto-generated method stub
        // return false;
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleEvent(Event event) {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

}
