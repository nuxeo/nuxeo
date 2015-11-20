package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.storage.sql.Mapper;

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
}
