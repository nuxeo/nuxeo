/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis.contribs;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.storage.kv.KeyValueStoreDescriptor;
import org.nuxeo.ecm.core.storage.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Redis implementation of a Key/Value Store Provider.
 * <p>
 * The following configuration properties are available:
 * <ul>
 * <li>namespace: the Redis namespace to use for keys (in addition to the global Redis namespace configured in the Redis
 * service).
 * </ul>
 *
 * @since 9.1
 */
public class RedisKeyValueStore implements KeyValueStoreProvider {

    private static final Log log = LogFactory.getLog(RedisKeyValueStore.class);

    public static final String NAMESPACE_PROP = "namespace";

    protected static final Long ONE = Long.valueOf(1);

    protected String name;

    protected String namespace;

    protected byte[] compareAndSetSHA;

    protected byte[] compareAndDelSHA;

    protected byte[] compareNullAndSetSHA;

    protected static byte[] getBytes(String key) {
        return key.getBytes(UTF_8);
    }

    @Override
    public void initialize(KeyValueStoreDescriptor descriptor) {
        log.debug("Initializing");
        name = descriptor.name;
        Map<String, String> properties = descriptor.getProperties();
        String name = properties.get(NAMESPACE_PROP);
        RedisAdmin redisAdmin = Framework.getService(RedisAdmin.class);
        namespace = redisAdmin.namespace(name == null ? new String[0] : new String[] { name });
        try {
            compareAndSetSHA = getBytes(redisAdmin.load("org.nuxeo.ecm.core.redis", "compare-and-set"));
            compareAndDelSHA = getBytes(redisAdmin.load("org.nuxeo.ecm.core.redis", "compare-and-del"));
            compareNullAndSetSHA = getBytes(redisAdmin.load("org.nuxeo.ecm.core.redis", "compare-null-and-set"));
        } catch (IOException e) {
            throw new NuxeoException("Cannot load Redis script", e);
        }
    }

    @Override
    public void close() {
        log.debug("Closed");
    }

    @Override
    public void clear() {
        RedisAdmin redisAdmin = Framework.getService(RedisAdmin.class);
        redisAdmin.clear(namespace + "*");
    }

    @Override
    public void put(String key, byte[] value, long ttl) {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        redisExecutor.execute(jedis -> {
            byte[] keyb = getBytes(namespace + key);
            if (value == null) {
                jedis.del(keyb);
            } else if (ttl == 0) {
                jedis.set(keyb, value);
            } else {
                jedis.setex(keyb, (int) ttl, value);
            }
            return null;
        });
    }

    @Override
    public byte[] get(String key) {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        return redisExecutor.execute(jedis -> jedis.get(getBytes(namespace + key)));
    }

    @Override
    public boolean setTTL(String key, long ttl) {
        RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
        Long result = redisExecutor.execute(jedis -> {
            byte[] keyb = getBytes(namespace + key);
            if (ttl == 0) {
                return jedis.persist(keyb);
            } else {
                return jedis.expire(keyb, (int) ttl);
            }
        });
        return ONE.equals(result);
    }

    @Override
    public boolean compareAndSet(String key, byte[] expected, byte[] value) {
        if (expected == null && value == null) {
            return get(key) == null;
        } else {
            byte[] sha;
            List<byte[]> keys = Collections.singletonList(getBytes(namespace + key));
            List<byte[]> args;
            if (expected == null) {
                sha = compareNullAndSetSHA;
                args = Collections.singletonList(value);
            } else if (value == null) {
                sha = compareAndDelSHA;
                args = Collections.singletonList(expected);
            } else {
                sha = compareAndSetSHA;
                args = Arrays.asList(expected, value);
            }
            RedisExecutor redisExecutor = Framework.getService(RedisExecutor.class);
            Object result = redisExecutor.evalsha(sha, keys, args);
            return ONE.equals(result);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }

}
