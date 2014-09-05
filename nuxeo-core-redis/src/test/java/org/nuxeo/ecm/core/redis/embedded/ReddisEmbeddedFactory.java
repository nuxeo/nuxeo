package org.nuxeo.ecm.core.redis.embedded;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;

import redis.clients.jedis.Jedis;

import com.lordofthejars.nosqlunit.proxy.RedirectProxy;
import com.lordofthejars.nosqlunit.redis.embedded.NoArgsJedis;

public class ReddisEmbeddedFactory implements PooledObjectFactory<Jedis> {

    protected final Jedis jedis = RedirectProxy.createProxy(NoArgsJedis.class,
            new RedisEmbeddedConnection(this));

    protected final RedisEmbeddedLuaEngine lua =
            new RedisEmbeddedLuaEngine(this);

    @Override
    public PooledObject<Jedis> makeObject() throws Exception {
        PooledObject<Jedis> pooled = new RedisEmbeddedPooledObject(jedis);
        LogFactory.getLog(ReddisEmbeddedFactory.class).trace("created " + pooled);
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
