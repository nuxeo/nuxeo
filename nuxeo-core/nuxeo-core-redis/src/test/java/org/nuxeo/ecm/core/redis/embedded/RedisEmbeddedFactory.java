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
import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.lordofthejars.nosqlunit.redis.embedded.NoArgsJedis;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import redis.clients.jedis.Jedis;

public class RedisEmbeddedFactory implements PooledObjectFactory<Jedis> {

    final RedisEmbeddedConnection connection = wrapConnection(new RedisEmbeddedConnection());

    RedisEmbeddedConnection wrapConnection(RedisEmbeddedConnection connection) {
        connection = synchronizedConnection(connection);
        connection = logConnection(connection);
        return connection;
    }

    RedisEmbeddedConnection logConnection(RedisEmbeddedConnection connection) {
        Log log = LogFactory.getLog(RedisEmbeddedConnection.class);
        if (!log.isTraceEnabled()) {
            return connection;
        }

        class Logger implements MethodInterceptor {


            @Override
            public Object intercept(Object object, Method method, Object[] arguments, MethodProxy proxy) throws Throwable {

                try {
                    Object result = on(connection).call(method.getName(), arguments).get();
                    log.trace(String.format("%s(%s) <- %s",
                            method.getName(),
                            Arrays.deepToString(arguments),
                            result));
                    return result;
                } catch (Throwable error) {
                    log.trace(String.format("%s(%s) <- %s",
                            method.getName(),
                            Arrays.deepToString(arguments),
                            error));
                    throw error;
                }
            }

            RedisEmbeddedConnection createProxy() {
                return RedisEmbeddedConnection.class
                        .cast(Enhancer.create(RedisEmbeddedConnection.class, this));
            }

        }

        return new Logger().createProxy();
    }

    RedisEmbeddedConnection synchronizedConnection(RedisEmbeddedConnection connection) {
        class Synchronizer implements MethodInterceptor {

            @Override
            public Object intercept(Object object, Method method, Object[] arguments, MethodProxy proxy) throws Throwable {
                synchronized (connection) {
                    return on(connection).call(method.getName(), arguments).get();
                }
            }

            RedisEmbeddedConnection createProxy() {
                return RedisEmbeddedConnection.class
                        .cast(Enhancer.create(RedisEmbeddedConnection.class, this));
            }
        }
        return new Synchronizer().createProxy();
    }

    RedisEmbeddedGuessConnectionError error = new RedisEmbeddedGuessConnectionError.NoError();

    protected final RedisEmbeddedLuaEngine lua = new RedisEmbeddedLuaEngine(connection);

    @Override
    public PooledObject<Jedis> makeObject() throws Exception {
        class TryFailoverMethod implements MethodInterceptor {

            @Override
            public Object intercept(Object object, Method method, Object[] arguments, MethodProxy proxy)
                    throws Throwable {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    error.guessError();
                }
                return on(connection).call(method.getName(), arguments).get();
            }

            Jedis createProxy() {
                return Jedis.class.cast(Enhancer.create(NoArgsJedis.class, this));
            }
        }
        Jedis jedis = new TryFailoverMethod().createProxy();
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
