/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class JDBCMapperConnector implements InvocationHandler {

    protected final Mapper mapper;

    protected final boolean noSharing;

    protected final Function<Supplier<Object>, Object> defaultRunner;

    protected JDBCMapperConnector(Mapper mapper, boolean noSharing) {
        this.mapper = mapper;
        this.noSharing = noSharing;
        defaultRunner = noSharing ? TransactionHelper::runInNewTransaction : TransactionHelper::runInTransaction;
    }

    protected Object doInvoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(mapper, args);
        } catch (InvocationTargetException cause) {
            return cause.getTargetException();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (mapper.isConnected()) {
            if (Arrays.asList("start", "end", "prepare", "commit", "rollback").contains(name)) {
                throw new SystemException("wrong tx management invoke on managed connection");
            }
            return doInvoke(method, args);
        }
        // should not operate with tx mamagement (managed connection)
        if ("start".equals(name)) {
            return XAResource.XA_OK;
        }
        if ("end".equals(name)) {
            return null;
        }
        if ("prepare".equals(name)) {
            return XAResource.XA_OK;
        }
        if ("commit".equals(name)) {
            return null;
        }
        if ("rollback".equals(name)) {
            return null;
        }
        if ("clearCache".equals(name)) {
            return doInvoke(method, args);
        }
        if ("receiveInvalidations".equals(name)) {
            return doInvoke(method, args);
        }
        if ("sendInvalidations".equals(name)) {
            return doInvoke(method, args);
        }
        return doConnectAndInvoke(method, args);
    }

    protected Object doConnectAndInvoke(Method method, Object[] args) throws Throwable {
        String name = method.getName();
        Object result = runnerOf(name).apply(() -> {
            mapper.connect(noSharingOf(name));
            try {
                try {
                    return doInvoke(method, args);
                } catch (Throwable cause) {
                    return cause;
                }
            } finally {
                if (mapper.isConnected()) {
                    mapper.disconnect();
                }
            }
        });
        if (result instanceof Throwable) {
            if (result instanceof Exception) {
                ExceptionUtils.checkInterrupt((Exception) result);
            }
            throw (Throwable) result;
        }
        return result;
    }

    protected Function<Supplier<Object>, Object> runnerOf(String name) {
        if ("createDatabase".equals(name)) {
            return TransactionHelper::runWithoutTransaction;
        }
        return defaultRunner;
    }

    protected boolean noSharingOf(String name) {
        if ("createDatabase".equals(name)) {
            return true;
        }
        return noSharing;
    }

    public static Mapper newConnector(Mapper mapper, boolean noSharing) {
        return (Mapper) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { Mapper.class }, new JDBCMapperConnector(mapper, noSharing));
    }

    public static Mapper unwrap(Mapper mapper) {
        if (!Proxy.isProxyClass(mapper.getClass())) {
            return mapper;
        }
        return ((JDBCMapperConnector) Proxy.getInvocationHandler(mapper)).mapper;
    }
}
