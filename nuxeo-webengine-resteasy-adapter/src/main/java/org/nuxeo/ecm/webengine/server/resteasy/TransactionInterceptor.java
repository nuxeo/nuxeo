/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.server.resteasy;

import static javax.transaction.Status.STATUS_ACTIVE;

import java.lang.reflect.Method;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.UserTransaction;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.interception.ResourceMethodContext;
import org.jboss.resteasy.core.interception.ResourceMethodInterceptor;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * The TransactionAttribute marks a method as needing bytecode enhancement to
 * support transactions. The two most common transaction are REQUIRED and
 * SUPPORTS. A REQUIRED method expects to modify the data and wants to ensure
 * the update is consistent. A SUPPORTS method will only read data, so it can
 * avoid the overhead of a transaction.
 * <p>
 * A transaction is the database equivalent of a synchronized lock. Transactions
 * are somewhat more complicated locks because they need to work with multiple
 * machines and possibly multiple databases, but they're still just
 * sophisticated locks. The typical transaction patterns are similar to familiar
 * lock patterns.
 * <ul>
 * <li>A REQUIRED attribute tells Resin that the method must be protected by a
 * transaction. In this case, the swap needs protection from simultaneous
 * threads trying to swap at the same time.
 * <li>A SUPPORTS attribute would tell Resin that the method doesn't need a
 * transaction, but the method should join any transaction that already exists.
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TransactionInterceptor implements ResourceMethodInterceptor {

    private static final Log log = LogFactory.getLog(TransactionInterceptor.class);

    public boolean txDisabled = false;

    public boolean accepted(ResourceMethod method) {
        return null != method.getMethod().getAnnotation(TransactionAttribute.class);
    }

    /**
     * When a tx is required, the transaction is created if not already exists.
     * When tx is supported, a transaction is joined
     * (in case of an error the transaction is marked as rollbacked).
     * When a tx requires a new transaction, a new tx is started.
     */
    public Response invoke(ResourceMethodContext ctx) throws Failure,
            ApplicationException, WebApplicationException {
        Object target = ctx.getTarget();
        if (!txDisabled && target instanceof Resource) {
            Method m = ctx.getMethod().getMethod();
            TransactionAttribute txanno = m.getAnnotation(TransactionAttribute.class);
            if (txanno != null) {
                UserTransaction utx = getUserTransaction();
                if (utx != null) {
                    TransactionAttributeType type = txanno.value();
                    if (type == TransactionAttributeType.REQUIRED) {
                        return invoke(ctx, utx, safeBeginTransaction(utx));
                    } else if (type == TransactionAttributeType.REQUIRES_NEW) {
                        safeBeginNewTransaction(utx);
                        return invoke(ctx, utx, false);
                    } else if (type == TransactionAttributeType.SUPPORTS) {
                        return invoke(ctx, utx, true);
                    }
                }
            }
        }
        return ctx.proceed();
    }

    protected Response invoke(ResourceMethodContext ctx, UserTransaction utx, boolean isManagingTx) throws Failure,
    ApplicationException, WebApplicationException {
        try {
            Response resp = ctx.proceed();
            if (isManagingTx) {
                utx.commit();
            }
            return resp;
        } catch (WebApplicationException e) {
            safeRollback(utx, isManagingTx);
            throw e;
        } catch (ApplicationException e) {
            safeRollback(utx, isManagingTx);
            throw e;
        } catch (Failure e) {
            safeRollback(utx, isManagingTx);
            throw e;
        } catch (Throwable t) {
            safeRollback(utx, isManagingTx);
            throw WebException.wrap(t);
        }
    }

    protected void safeRollback(UserTransaction utx, boolean isManagingTx) throws WebException {
        try {
            if (isManagingTx) {
                utx.rollback();
            } else {
                utx.setRollbackOnly();
            }
        } catch (Exception e) {
            throw WebException.wrap("Failed to rollback tx", e);
        }
    }

    /**
     * Returns true if a new transaction is started or false if an existing transaction is used.
     */
    protected boolean safeBeginTransaction(UserTransaction utx) throws WebException {
        try {
            if (utx.getStatus() == STATUS_ACTIVE) {
                return false;
            }
            utx.begin();
            return true;
        } catch (Exception e) {
            throw WebException.wrap("Failed to begin tx", e);
        }
    }

    protected void safeBeginNewTransaction(UserTransaction utx) throws WebException {
        try {
            utx.begin();
        } catch (Exception e) {
            throw WebException.wrap("Failed to begin tx", e);
        }
    }

    protected final UserTransaction getUserTransaction() {
        try {
            return TransactionHelper.lookupUserTransaction();
        } catch (Throwable e) {
            log.error("Failed to get user transaction, disabling Tx support", e);
            txDisabled = true;
            return null;
        }
    }

}
