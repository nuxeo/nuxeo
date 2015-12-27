/*******************************************************************************
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
 ******************************************************************************/
package org.nuxeo.ecm.core.redis.embedded;

import java.util.List;

import javax.script.ScriptException;

import redis.clients.jedis.exceptions.JedisException;

import com.lordofthejars.nosqlunit.redis.embedded.EmbeddedJedis;

public class RedisEmbeddedConnection extends EmbeddedJedis {

    protected final RedisEmbeddedLuaEngine lua;

    RedisEmbeddedConnection(RedisEmbeddedFactory reddisEmbeddedFactory) {
        lua = new RedisEmbeddedLuaEngine(this);
    }

    public String rpoplpush(final String srckey, final String dstkey) {
        String value = this.rpop(srckey);
        super.rpush(dstkey);
        return value;
    }

    public byte[] rpoplpush(final byte[] srckey, final byte[] dstkey) {
        byte[] value = this.rpop(srckey);
        super.rpush(dstkey);
        return value;
    }

    @Override
    public List<String> lrange(final String key, final long start, final long end) {
        return super.lrange(key, start, end);
    }

    @Override
    public List<byte[]> lrange(final byte[] key, final long start, final long end) {
        return super.lrange(key, (int) start, (int) end);
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        return super.lrem(key, (int) count, value);
    }

    @Override
    public String scriptLoad(String content) {
        try {
            return lua.load(content);
        } catch (ScriptException e) {
            throw new JedisException("Cannot load script " + content);
        }
    }

    @Override
    public Object evalsha(String sha, List<String> keys, List<String> args) {
        try {
            return lua.evalsha(sha, keys, args);
        } catch (ScriptException e) {
            throw new JedisException("Cannot evaluate script " + sha);
        }
    }
}
