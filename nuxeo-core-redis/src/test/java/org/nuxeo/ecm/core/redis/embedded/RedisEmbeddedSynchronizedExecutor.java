package org.nuxeo.ecm.core.redis.embedded;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

public class RedisEmbeddedSynchronizedExecutor implements RedisExecutor {

    protected final RedisExecutor delegate;

    protected final Log log = LogFactory.getLog(RedisEmbeddedSynchronizedExecutor.class);

    public RedisEmbeddedSynchronizedExecutor(RedisExecutor executor) {
        delegate = executor;
    }

    @Override
    public <T> T execute(RedisCallable<T> call) throws IOException,
            JedisException {
        synchronized (this) {
            return delegate.execute(call);
        }
    }

    @Override
    public Pool<Jedis> getPool() {
        return delegate.getPool();
    }

}
