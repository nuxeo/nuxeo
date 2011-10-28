/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.event.tx;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static javax.transaction.Status.STATUS_ROLLEDBACK;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Helper class to encapsulate Transaction management.
 *
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class EventBundleTransactionHandler {

    private static final Log log = LogFactory.getLog(EventBundleTransactionHandler.class);

    protected UserTransaction tx;

    protected boolean disabled;

    public void beginNewTransaction() {
        beginNewTransaction(null);
    }
    public void beginNewTransaction(Integer transactionTimeout) {
        if (disabled) {
            return;
        }
        if (tx != null) {
            throw new UnsupportedOperationException(
                    "There is already an uncommited transaction running");
        }
        tx = createUT(transactionTimeout);
        if (tx == null) {
            log.debug("No TransactionManager");
            disabled = true;
            return;
        }
        try {
            if (tx.getStatus() == STATUS_COMMITTED) {
                log.error("Transaction is already commited, try to begin anyway");
            }
            tx.begin();
        } catch (Exception e) {
            log.error("Unable to start transaction", e);
        }
    }

    protected UserTransaction createUT(Integer transactionTimeout) {
        return createUT(transactionTimeout, false);
    }
    protected UserTransaction createUT(Integer transactionTimeout, boolean retry) {
        try {
            new InitialContext();
        } catch (Exception e) {
            disabled = true;
            return null;
        }

        UserTransaction ut;
        try {
            ut = TransactionHelper.lookupUserTransaction();
        } catch (NamingException e) {
            disabled = true;
            return null;
        }

        try {
            int txStatus = ut.getStatus();
            if (txStatus != Status.STATUS_NO_TRANSACTION && !retry) {
                // if previous tx in this thread aborted in TimeOut
                // Ajuna may have failed to dissociate tx from thread context
                // => force rollback to avoid reusing a dead tx
                log.warn("Transaction was not properly cleanup up from thread context, rolling back before getting a new tx");
                try {
                    ut.rollback();
                }
                catch (Throwable t) {
                    log.warn("error during tx rollback", t);
                }
                return createUT(transactionTimeout, true);
            }
        } catch (Exception se) {
            log.warn("Error while getting TX status", se);
        }

        if (transactionTimeout!=null) {
            try {
                ut.setTransactionTimeout(transactionTimeout);
            } catch (SystemException e) {
                log.error("Error while setting transaction timeout to " + transactionTimeout, e);
            }
        }
        return ut;
    }

    protected boolean isUTTransactionActive() {
        try {
            return tx.getStatus() == STATUS_ACTIVE;
        } catch (SystemException e) {
            log.error("Error while getting tx status", e);
            return false;
        }
    }

    private boolean isUTTransactionMarkedRollback() {
        try {
            int status = tx.getStatus();
            return status == STATUS_MARKED_ROLLBACK
                    || status == STATUS_ROLLEDBACK;
        } catch (SystemException e) {
            log.error("Error while getting tx status", e);
            return false;
        }
    }

    public void rollbackTransaction() {
        if (disabled || tx == null) {
            return;
        }
        try {
            if (!isUTTransactionMarkedRollback()) {
                tx.setRollbackOnly();
            }
            commitOrRollbackTransaction();
        } catch (Exception e) {
            log.error("Error while marking tx for rollback", e);
        } finally {
            tx = null;
        }
    }

    public void commitOrRollbackTransaction() {
        if (disabled || tx == null) {
            return;
        }
        try {
            if (isUTTransactionActive()) {
                try {
                    tx.commit();
                } catch (Exception e) {
                    log.error("Error during commit", e);
                }
            } else {
                try {
                    log.debug("Rolling back transaction");
                    tx.rollback();
                } catch (Exception e) {
                    // if the transaction was already rolledback due to
                    // transaction timeout, we must still call tx.rollback but
                    // this causes a spurious error message, so log at debug
                    // level
                    log.debug("Error during rollback", e);
                }
            }
        } finally {
            tx = null;
        }
    }
    
    /**
     * @since 5.5
     */
    public void setTransactionRollbackOnly() {
        if (disabled || tx == null) {
            return;
        }
        try {
            tx.setRollbackOnly();
        } catch (Exception e) {
           log.error("Exception caught while setting the transaction as rollback only", e);
        }
    }

}
