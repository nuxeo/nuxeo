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

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class JDBCMapperConnector implements InvocationHandler {

    protected final Mapper mapper;

    protected JDBCMapperConnector(Mapper mapper) {
        this.mapper = mapper;
    }

    protected Object doInvoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(mapper, args);
        } catch (InvocationTargetException cause) {
            throw cause.getTargetException();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
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
        if ("createDatabase".equals(name)) {
            return suspendAndInvoke(method, args);
        }
        return connectAndInvoke(method, args);
    }

    protected Object suspendAndInvoke(Method method, Object[] args) throws Throwable {
        Transaction tx = TransactionHelper.suspendTransaction();
        try {
            return connectAndInvoke(method, args);
        } finally {
            TransactionHelper.resumeTransaction(tx);
        }
    }

    protected Object connectAndInvoke(Method method, Object[] args) throws Throwable {
        mapper.connect();
        try {
            return doInvoke(method, args);
        } finally {
            if (mapper.isConnected()) {
                mapper.disconnect();
            }
        }
    }

    public static Mapper newConnector(Mapper mapper) {
        return (Mapper) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { Mapper.class }, new JDBCMapperConnector(mapper));
    }

    public static Mapper unwrap(Mapper mapper) {
        if (!Proxy.isProxyClass(mapper.getClass())) {
            return mapper;
        }
        return ((JDBCMapperConnector)Proxy.getInvocationHandler(mapper)).mapper;
    }
}
