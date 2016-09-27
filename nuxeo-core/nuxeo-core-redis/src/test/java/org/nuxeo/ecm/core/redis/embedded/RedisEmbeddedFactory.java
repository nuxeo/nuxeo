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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

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
        return logConnection(connection);
    }

    RedisEmbeddedConnection logConnection(RedisEmbeddedConnection connection) {
        Log log = LogFactory.getLog(RedisEmbeddedConnection.class);
        if (!log.isTraceEnabled()) {
            return connection;
        }

        class Logger implements MethodInterceptor {

            @Override
            public Object intercept(Object object, Method method, Object[] arguments, MethodProxy proxy)
                    throws Throwable {
                class Stringifier {

                    StringBuilder sb = new StringBuilder();

                    Stringifier append(Object[] args) {
                        StringBuilder sb = new StringBuilder();
                        Iterator<Object> it = Arrays.stream(args).iterator();
                        while (it.hasNext()) {
                            append(it.next());
                            if (it.hasNext()) {
                                sb.append(',');
                            }
                        }
                        return this;
                    }

                    Stringifier append(byte[] bytes) {
                        int mark = sb.length();
                        try {
                            sb.append(new String(bytes, "UTF-8").replaceAll("\\p{C}", "x"));
                        } catch (UnsupportedEncodingException cause) {
                            sb.setLength(mark);
                            sb.append(Arrays.toString(bytes));
                        }
                        return this;
                    }

                    Stringifier append(Object object) {
                        if (object == null) {
                            sb.append("null");
                            return this;
                        }
                        if (object instanceof byte[]) {
                            return append((byte[]) object);
                        }
                        Class<? extends Object> typeof = object.getClass();
                        if (object instanceof Collection<?>) {
                            return append((Collection<?>) object);
                        }
                        if (typeof.isArray() && !typeof.getComponentType().isPrimitive()) {
                            return append((Object[])object);
                        }
                        sb.append(object.toString());
                        return this;
                    }

                    Stringifier append(Collection<?> collection) {
                        Iterator<?> it = collection.iterator();
                        while (it.hasNext()) {
                            append(it.next());
                            if (it.hasNext()) {
                                sb.append(',');
                            }
                        }
                        return this;
                    }

                    @Override
                    public String toString() {
                        return sb.toString();
                    }
                }
                if (method.getDeclaringClass() == Object.class) {
                    return on(connection).call(method.getName(), arguments).get();
                }
                try {
                    log.trace(String.format("%s(%s) ->", method.getName(), new Stringifier().append(arguments)));
                    Object result = on(connection).call(method.getName(), arguments).get();
                    log.trace(String.format("%s(%s) <- %s", method.getName(), new Stringifier().append(arguments),
                            new Stringifier().append(result)), new Throwable("stack trace"));
                    return result;
                } catch (Throwable error) {
                    log.trace(String.format("%s(%s) <- error", method.getName(), new Stringifier().append(arguments)), error);
                    if (error instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    throw error;
                }
            }

            RedisEmbeddedConnection createProxy() {
                return RedisEmbeddedConnection.class.cast(Enhancer.create(RedisEmbeddedConnection.class, this));
            }

        }

        return new Logger().createProxy();
    }

    RedisEmbeddedGuessConnectionError error = new RedisEmbeddedGuessConnectionError.NoError();

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
