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
@RedisFeature.Config(guessError = RedisEmbeddedGuessConnectionError.OnEveryCall.class)
public class TestRedisFailover {

    @Inject
    protected RedisExecutor executor;

    @Test(expected = JedisConnectionException.class)
    public void cannotRetry() throws JedisException, IOException {
        executor.execute(new RedisCallable<Void>() {
            @Override
            public Void call(Jedis jedis) {
                jedis.get("pfouh");
                return null;
            }
        });
    }
}
