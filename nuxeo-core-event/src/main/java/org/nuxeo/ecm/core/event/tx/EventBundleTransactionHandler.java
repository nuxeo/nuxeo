/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.tx;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * Helper class to encapsulate Transaction management
 * 
 * @author tiry
 * 
 */
public class EventBundleTransactionHandler {

    protected UserTransaction tx;

    protected static final String UTName = "java:comp/UserTransaction";

    protected static final String UTNameAlternate = "UserTransaction";

    private static final Log log = LogFactory.getLog(EventBundleTransactionHandler.class);

    protected static boolean isTxEnabled = true;

    public void beginNewTransaction() {

        if (!isTxEnabled) {
            return;
        }

        if (tx != null) {
            throw new UnsupportedOperationException(
                    "There is already an uncomited transaction running");
        }

        tx = createUT();
        if (tx == null) {
            log.error("No TransactionManager");
            isTxEnabled = false;
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

    protected UserTransaction createUT() {

        InitialContext context = null;
        try {
            context = new InitialContext();
        } catch (Exception e) {
            isTxEnabled = false;
        }

        UserTransaction ut = null;
        if (context != null) {
            try {
                ut = (UserTransaction) context.lookup(UTName);
            } catch (NamingException ne) {
                try {
                    ut = (UserTransaction) context.lookup(UTNameAlternate);
                } catch (NamingException ne2) {
                    isTxEnabled = false;
                }
            }
        }
        return ut;
    }

    protected Transaction createTxFromTM() {
        InitialContext context = null;
        try {
            context = new InitialContext();
        } catch (Exception e) {
            isTxEnabled = false;
        }

        TransactionManager tm = null;
        try {
            tm = (TransactionManager) context.lookup("TransactionManager");
        } catch (NamingException ne) {
            try {
                tm = (TransactionManager) context.lookup("java:/TransactionManager");
            } catch (NamingException ne2) {
                isTxEnabled = false;
            }
        }

        if (tm == null) {
            isTxEnabled = false;
            return null;
        }

        try {
            return tm.getTransaction();
        } catch (SystemException e) {
            isTxEnabled = false;
            return null;
        }
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
            return tx.getStatus() == STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            log.error("Error while getting tx status", e);
            return false;
        }
    }

    public void rollbackTransaction() {
        if (!isTxEnabled) {
            return;
        }
        if (tx != null) {
            try {
                if (!isUTTransactionMarkedRollback()) {
                    tx.setRollbackOnly();
                }
                commitOrRollbackTransaction();
            } catch (Exception e) {
                log.error("Error while marking tx for rollback", e);
            }
            tx = null;
        }
    }

    public void commitOrRollbackTransaction() {
        if (!isTxEnabled) {
            return;
        }
        if (tx != null) {
            if (isUTTransactionActive()) {
                try {
                    tx.commit();
                } catch (Exception e) {
                    log.error("Error during Commit", e);
                }
            } else if (isUTTransactionMarkedRollback()) {
                try {
                    log.debug("Rolling back transaction");
                    tx.rollback();
                } catch (Exception e) {
                    log.error("Error during RollBack", e);
                }
            } else {
                // log.error("TX is in abnormal state)
            }
            tx = null;
        }
    }

}
