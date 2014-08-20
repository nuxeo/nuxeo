package org.nuxeo.ecm.core.redis;

import java.util.concurrent.Callable;

import redis.clients.jedis.Jedis;

public abstract class RedisCallable<T> implements Callable<T> {

    protected Jedis jedis;

    protected String prefix;

}
