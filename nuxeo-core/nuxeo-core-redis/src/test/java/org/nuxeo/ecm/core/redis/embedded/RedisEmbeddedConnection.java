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

import java.util.List;
import java.util.Set;

import javax.script.ScriptException;

import com.lordofthejars.nosqlunit.redis.embedded.EmbeddedJedis;

import redis.clients.jedis.exceptions.JedisException;

public class RedisEmbeddedConnection extends EmbeddedJedis {

    protected final RedisEmbeddedLuaEngine lua;

    RedisEmbeddedConnection() {
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
    public String set(String key, String value, String nxxx, String expx, long time) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long pexpire(String key, long milliseconds) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Double incrByFloat(String key, double value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<String> lrange(final String key, final long start, final long end) {
        return super.lrange(key, start, end);
    }

    @Override
    public Set<String> spop(String key, long count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<String> srandmember(String key, int count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<String> blpop(int timeout, String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<String> brpop(int timeout, String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long pfadd(String key, String... elements) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long pfcount(String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long pexpire(byte[] key, long milliseconds) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long pexpireAt(byte[] key, long millisecondsTimestamp) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Double incrByFloat(byte[] key, double value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Double hincrByFloat(byte[] key, byte[] field, double value) {
        throw new UnsupportedOperationException("Not implemented");
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
    public Set<byte[]> spop(byte[] key, long count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long zremrangeByLex(byte[] key, byte[] min, byte[] max) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Long pfadd(byte[] key, byte[]... elements) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long pfcount(byte[] key) {
        throw new UnsupportedOperationException("Not implemented");
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

    public Object evalsha(byte[] sha, List<byte[]> keys, List<byte[]> args) {
        try {
            return lua.evalsha(sha, keys, args);
        } catch (ScriptException e) {
            throw new JedisException("Cannot evaluate script " + sha);
        }
    }

}
