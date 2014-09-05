package org.nuxeo.ecm.core.redis.embedded;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import redis.clients.jedis.Jedis;

class RedisEmbeddedPooledObject extends DefaultPooledObject<Jedis> {

    protected volatile Thread owner;

    protected Throwable root;

    protected RedisEmbeddedPooledObject(Jedis object) {
        super(object);
    }

    @Override
    public boolean allocate() {
        synchronized (this) {
            if (owner != null) {
                return false;
            }
            LogFactory.getLog(ReddisEmbeddedFactory.class).info(
                    "Allocating " + this);
            if (super.allocate()) {
                owner = Thread.currentThread();
                root = new Throwable("ici");
                LogFactory.getLog(ReddisEmbeddedFactory.class).info(
                        "Allocated " + this + " was at", root);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean deallocate() {
        LogFactory.getLog(ReddisEmbeddedFactory.class).info(
                "Deallocating  " + this);
        if (owner != Thread.currentThread()) {
            LogFactory.getLog(ReddisEmbeddedFactory.class).error(
                    this + " in " + owner.getName() + " was at", root);
            return false;
        }
        if (super.deallocate()) {
            LogFactory.getLog(ReddisEmbeddedFactory.class).info(
                    "Deallocated  " + this);
            owner = null;
            return true;
        }
        LogFactory.getLog(ReddisEmbeddedFactory.class).error("was at", root);
        return false;
    }


    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + ", State " + getState();
    }
}