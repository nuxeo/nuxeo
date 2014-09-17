package org.nuxeo.ecm.core.redis.embedded;

import redis.clients.jedis.exceptions.JedisConnectionException;

public interface RedisEmbeddedGuessError {

    void guess() throws JedisConnectionException;

}
