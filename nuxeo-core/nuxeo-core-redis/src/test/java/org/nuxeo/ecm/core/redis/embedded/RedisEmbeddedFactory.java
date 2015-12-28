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

import static org.joor.Reflect.on;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import redis.clients.jedis.Jedis;

import com.lordofthejars.nosqlunit.redis.embedded.NoArgsJedis;

public class RedisEmbeddedFactory implements PooledObjectFactory<Jedis> {

    protected final RedisEmbeddedConnection connection = new RedisEmbeddedConnection(this);

    protected RedisEmbeddedGuessConnectionError error = new RedisEmbeddedGuessConnectionError.NoError();

    public Jedis createProxy() {
        return Jedis.class.cast(Enhancer.create(NoArgsJedis.class, new TryFailoverMethod()));
    }

    public class TryFailoverMethod implements MethodInterceptor {

        @Override
        public Object intercept(Object object, Method method, Object[] arguments, MethodProxy proxy) throws Throwable {
            if (!method.getDeclaringClass().equals(Object.class)) {
                error.guessError();
            }
            return on(connection).call(method.getName(), arguments).get();
        }

    }

    protected final RedisEmbeddedLuaEngine lua = new RedisEmbeddedLuaEngine(connection);

    @Override
    public PooledObject<Jedis> makeObject() throws Exception {
        Jedis jedis = createProxy();
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
    public void activateObject(PooledObject<Jedis> p) throws Exception {;
    }

    @Override
    public void passivateObject(PooledObject<Jedis> p) throws Exception {;
    }

}
