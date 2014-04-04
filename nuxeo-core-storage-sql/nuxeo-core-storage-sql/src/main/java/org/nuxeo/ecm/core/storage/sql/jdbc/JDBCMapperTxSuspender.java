package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.transaction.Transaction;

import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class JDBCMapperTxSuspender implements InvocationHandler {

    protected final Mapper mapper;

    protected JDBCMapperTxSuspender(Mapper mapper) {
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
        Transaction tx = TransactionHelper.suspendTransaction();
        try {
            return doInvoke(method, args);
        } finally {
            if (tx != null) {
                TransactionHelper.resumeTransaction(tx);
            }
        }
    }

    public static Mapper newConnector(Mapper mapper) {
        return (Mapper)Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(), new Class<?>[] {
                        Mapper.class },
                new JDBCMapperTxSuspender(mapper));
    }
}