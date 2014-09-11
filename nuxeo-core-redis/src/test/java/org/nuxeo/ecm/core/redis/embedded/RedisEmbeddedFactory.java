package org.nuxeo.ecm.core.redis.embedded;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import redis.clients.jedis.Jedis;

import com.lordofthejars.nosqlunit.proxy.RedirectProxy;
import com.lordofthejars.nosqlunit.redis.embedded.NoArgsJedis;

public class RedisEmbeddedFactory implements PooledObjectFactory<Jedis> {

    protected final RedisEmbeddedConnection connection = new RedisEmbeddedConnection(this);

    protected final RedisEmbeddedLuaEngine lua =
            new RedisEmbeddedLuaEngine(connection);

    @Override
    public PooledObject<Jedis> makeObject() throws Exception {
        Jedis jedis = RedirectProxy.createProxy(NoArgsJedis.class,
                connection);
        PooledObject<Jedis> pooled = new DefaultPooledObject<>(jedis);
        LogFactory.getLog(RedisEmbeddedFactory.class).trace("created " + pooled);
        return pooled;
    }

    @Override
    public void destroyObject(PooledObject<Jedis> p) throws Exception {
        return;
    }

    @Override
    public boolean validateObject(PooledObject<Jedis> p) {
        return true;
    }

    @Override
    public void activateObject(PooledObject<Jedis> p) throws Exception {
        ;
    }

    @Override
    public void passivateObject(PooledObject<Jedis> p) throws Exception {
        ;
    }

}
