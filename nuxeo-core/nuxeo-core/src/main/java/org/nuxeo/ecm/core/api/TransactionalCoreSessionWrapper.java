/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
    private final ThreadLocal<Boolean> threadBound = new ThreadLocal<Boolean>();

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

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Boolean b = threadBound.get();
        if (b == null) {
            // first call in thread
            try {
                Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
                if (tx != null
                        && tx.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                    tx.registerSynchronization(this);
                    session.afterBegin();
                    threadBound.set(Boolean.TRUE);
                }
            } catch (NamingException e) {
                // no transaction manager, ignore
            } catch (Exception e) {
                log.error("Error on transaction synchronizer registration", e);
            }
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

    public void beforeCompletion() {
        session.beforeCompletion();
    }

    public void afterCompletion(int status) {
        threadBound.remove();
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
