package org.nuxeo.ecm.core.redis.embedded;

import java.util.List;

import javax.script.ScriptException;

import redis.clients.jedis.exceptions.JedisException;

import com.lordofthejars.nosqlunit.redis.embedded.EmbeddedJedis;

class RedisEmbeddedConnection extends EmbeddedJedis {

    protected final ReddisEmbeddedFactory factory;

    RedisEmbeddedConnection(ReddisEmbeddedFactory reddisEmbeddedFactory) {
        factory = reddisEmbeddedFactory;
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
    public List<String> lrange(final String key, final long start,
            final long end) {
        return super.lrange(key, start, end);
    }

    @Override
    public List<byte[]> lrange(final byte[] key, final long start,
            final long end) {
        return super.lrange(key, (int)start, (int)end);
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        return super.lrem(key,(int)count,value);
    }

    @Override
    public String scriptLoad(String content) {
        try {
            return factory.lua.load(content);
        } catch (ScriptException e) {
            throw new JedisException("Cannot load script " + content);
        }
    }

    @Override
    public Object evalsha(String sha, List<String> keys, List<String> args) {
        try {
            return factory.lua.evalsha(sha, keys, args);
        } catch (ScriptException e) {
            throw new JedisException("Cannot evaluate script " + sha);
        }
    }
}