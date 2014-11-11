/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedGuessConnectionError;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

@RunWith(FeaturesRunner.class)
@Features(RedisFailoverFeature.class)
@RedisFeature.Config(guessError=RedisEmbeddedGuessConnectionError.OnEveryCall.class )
public class TestRedisFailover {

    @Inject protected RedisExecutor executor;

    @Test(expected=JedisConnectionException.class)
    public void cannotRetry() throws JedisException, IOException {
        executor.execute(new RedisCallable<Void>() {

            @Override
            public Void call(Jedis jedis) throws Exception {
                jedis.get("pfouh");
                return null;
            }
        });
    }
}
