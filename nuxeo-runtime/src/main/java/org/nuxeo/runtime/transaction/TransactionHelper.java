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

package org.nuxeo.runtime.transaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utilities to work with transactions.
 */
public class TransactionHelper {

    private static final Log log = LogFactory.getLog(TransactionHelper.class);

    private TransactionHelper() {
        // utility class
    }

    /**
     * Various binding names for the UserTransaction. They depend on the
     * application server used and how the configuration is done.
     */
    public static final String[] UT_NAMES = { "java:comp/UserTransaction", // standard
            "java:comp/env/UserTransaction", // manual binding outside appserver
            "UserTransaction" // jboss
    };

    /**
     * Various binding names for the TransactionManager. They depend on the
     * application server used and how the configuration is done.
     */
    public static final String[] TM_NAMES = { "java:comp/TransactionManager", // common
            "java:comp/env/TransactionManager", // manual binding
            "java:TransactionManager" //
    };

    /**
     * Looks up the User Transaction in JNDI.
     *
     * @return the User Transaction
     * @throws NamingException if not found
     */
    public static UserTransaction lookupUserTransaction()
            throws NamingException {
        InitialContext context = new InitialContext();
        int i = 0;
        for (String name : UT_NAMES) {
            try {
                UserTransaction userTransaction = (UserTransaction) context.lookup(name);
                if (userTransaction != null) {
                    if (i != 0) {
                        // put successful name first for next time
                        UT_NAMES[i] = UT_NAMES[0];
                        UT_NAMES[0] = name;
                    }
                    return userTransaction;
                }
            } catch (NamingException e) {
                // try next one
            }
            i++;
        }
        throw new NamingException("UserTransaction not found in JNDI");
    }

    /**
     * Returns the UserTransaction JNDI binding name.
     * <p>
     * Assumes {@link #lookupUserTransaction} has been called once before.
     */
    public static String getUserTransactionJNDIName() {
        return UT_NAMES[0];
    }

    /**
     * Looks up the TransactionManager in JNDI.
     *
     * @return the TransactionManager
     * @throws NamingException if not found
     */
    public static TransactionManager lookupTransactionManager()
            throws NamingException {
        InitialContext context = new InitialContext();
        int i = 0;
        for (String name : TM_NAMES) {
            try {
                TransactionManager transactionManager = (TransactionManager) context.lookup(name);
                if (transactionManager != null) {
                    if (i != 0) {
                        // put successful name first for next time
                        TM_NAMES[i] = TM_NAMES[0];
                        TM_NAMES[0] = name;
                    }
                    return transactionManager;
                }
            } catch (NamingException e) {
                // try next one
            }
            i++;
        }
        throw new NamingException("TransactionManager not found in JNDI");
    }

    /**
     * Checks if the current User Transaction is active.
     */
    public static boolean isTransactionActive() {
        try {
            return lookupUserTransaction().getStatus() == Status.STATUS_ACTIVE;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the current User Transaction is marked rollback only.
     */
    public static boolean isTransactionMarkedRollback() {
        try {
            return lookupUserTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the current User Transaction is active or marked rollback only.
     */
    public static boolean isTransactionActiveOrMarkedRollback() {
        try {
            int status = lookupUserTransaction().getStatus();
            return status == Status.STATUS_ACTIVE
                    || status == Status.STATUS_MARKED_ROLLBACK;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Starts a new User Transaction.
     *
     * @return {@code true} if the transaction was successfully started, {@code
     *         false} otherwise
     */
    public static boolean startTransaction() {
        try {
            lookupUserTransaction().begin();
            return true;
        } catch (NamingException e) {
            // no transaction
        } catch (Exception e) {
            log.error("Unable to start transaction", e);
        }
        return false;
    }

    /**
     * Commits or rolls back the User Transaction depending on the transaction
     * status.
     */
    public static void commitOrRollbackTransaction() {
        try {
            UserTransaction ut = lookupUserTransaction();
            int status = ut.getStatus();
            if (status == Status.STATUS_ACTIVE) {
                if (log.isDebugEnabled()) {
                    log.debug("Commiting transaction");
                }
                ut.commit();
            } else if (status == Status.STATUS_MARKED_ROLLBACK) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot commit transaction because it is marked rollback only");
                }
                ut.rollback();
            }
        } catch (NamingException e) {
            // no transaction
        } catch (Exception e) {
            log.error("Unable to commit/rollback transaction", e);
        }
    }

    /**
     * Sets the current User Transaction as rollback only.
     *
     * @return {@code true} if the transaction was successfully marked rollback
     *         only, {@code false} otherwise
     */
    public static boolean setTransactionRollbackOnly() {
        try {
            lookupUserTransaction().setRollbackOnly();
            return true;
        } catch (NamingException e) {
            // no transaction
        } catch (Exception e) {
            log.error("Could not mark transaction as rollback only", e);
        }
        return false;
    }

}
