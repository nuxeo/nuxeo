/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.J2EEContainerDescriptor;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Wrapper around a CoreSession that gives it transactional behavior.
 * <p>
 * Transactional behavior:
 * <ul>
 * <li>notifies the event service on transaction start/stop</li>
 * <li>throws RollbackException</li>
 * </ul>
 */
public class TransactionalCoreSessionWrapper implements InvocationHandler,
        Synchronization {

    private static final Log log = LogFactory.getLog(TransactionalCoreSessionWrapper.class);

    private static final Class<?>[] INTERFACES = new Class[] { CoreSession.class };

    private final CoreSession session;

    /**
     * Per-thread flag with transaction status:
     * <ul>
     * <li>{@code null}: outside transaction</li>
     * <li>{@code TRUE}: in a transaction</li>
     * </ul>
     */
    private final ThreadLocal<Transaction> threadBound = new ThreadLocal<Transaction>();

    protected TransactionalCoreSessionWrapper(CoreSession session) {
        this.session = session;
    }

    public static CoreSession wrap(CoreSession session) {
        try {
            TransactionHelper.lookupTransactionManager();
        } catch (NamingException e) {
            // no transactions, do not wrap
            return session;
        }
        ClassLoader cl = session.getClass().getClassLoader();
        return (CoreSession) Proxy.newProxyInstance(cl, INTERFACES,
                new TransactionalCoreSessionWrapper(session));
    }

    protected void checkTxActiveRequired(Method m) {
        if (threadBound.get() != null) {
            return; // tx is active, no ckeck needed
        }
        if (J2EEContainerDescriptor.getSelected() == null) {
            return; // not in container
        }
        // TODO add annotation on core session api for marking non
        // transactional API
        final String name = m.getName();
        if ("getSessionId".equals(name)) {
            return;
        }
        if ("connect".equals(name)) {
            return;
        }
        if ("disconnect".equals(name)) {
            return;
        }
        if ("close".equals(name)) {
            return;
        }
        if ("destroy".equals(name)) {
            return;
        }

        log.warn("Session invoked in a container without a "
                + "transaction active: turn on debug logs for more "
                + "information about the faulty call.");
        if (log.isDebugEnabled()) {
            log.debug("Session invoked in a container without a transaction",
                    new Throwable());
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Transaction main = threadBound.get();
        if (main == null) {
            // first call in thread
            try {
                main = TransactionHelper.lookupTransactionManager().getTransaction();

                if (main != null) {
                    if (main.getStatus() == Status.STATUS_ACTIVE) {
                        main.registerSynchronization(this);
                        session.afterBegin();
                        threadBound.set(main);
                    }
                }
            } catch (NamingException e) {
                // no transaction manager, ignore
            } catch (Exception e) {
                log.error("Error on transaction synchronizer registration", e);
            }
            checkTxActiveRequired(method);
        }
        try {
            return method.invoke(session, args);
        } catch (Throwable t) {
            if (TransactionHelper.isTransactionActive()
                    && needsRollback(method, t)) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            if (t instanceof InvocationTargetException) {
                Throwable tt = ((InvocationTargetException) t).getTargetException();
                if (tt != null) {
                    throw tt;
                }
            }
            throw t;
        }
    }

    protected boolean needsRollback(Method method, Throwable t) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType() == NoRollbackOnException.class) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void beforeCompletion() {
        session.beforeCompletion();
    }

    @Override
    public void afterCompletion(int status) {
        Transaction current = null;
        try {
            current = TransactionHelper.lookupTransactionManager().getTransaction();
        } catch (Exception e) {
            throw new RuntimeException("no tx", e);
        }
        Transaction main = threadBound.get();
        if (main.equals(current)) {
            threadBound.remove();
        }
        boolean committed;
        if (status == Status.STATUS_COMMITTED) {
            committed = true;
        } else if (status == Status.STATUS_ROLLEDBACK) {
            committed = false;
        } else {
            log.error("Unexpected status after completion: " + status);
            return;
        }
        session.afterCompletion(committed);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '(' + session + ')';
    }

}
