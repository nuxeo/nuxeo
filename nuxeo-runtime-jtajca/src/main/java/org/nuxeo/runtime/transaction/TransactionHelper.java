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

package org.nuxeo.runtime.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * Utilities to work with transactions.
 */
public class TransactionHelper {

    private static final Log log = LogFactory.getLog(TransactionHelper.class);

    private TransactionHelper() {
        // utility class
    }

    /**
     * Looks up the User Transaction in JNDI.
     *
     * @return the User Transaction
     * @throws NamingException if not found
     */
    public static UserTransaction lookupUserTransaction()
            throws NamingException {
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            throw new NamingException("tx manager not installed");
        }
        return ut;
     }

    /**
     * Returns the UserTransaction JNDI binding name.
     * <p>
     * Assumes {@link #lookupUserTransaction} has been called once before.
     */
    public static String getUserTransactionJNDIName() {
        return NuxeoContainer.nameOf("UserTransaction");
    }

    /**
     * Looks up the TransactionManager in JNDI.
     *
     * @return the TransactionManager
     * @throws NamingException if not found
     */
    public static TransactionManager lookupTransactionManager()
            throws NamingException {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) {
            throw new NamingException("tx manager not installed");
        }
        return tm;
    }


    /**
     * Looks up the TransactionSynchronizationRegistry in JNDI.
     *
     * @return the TransactionSynchronizationRegistry
     * @throws NamingException if not found
     */
    public static TransactionSynchronizationRegistry lookupSynchronizationRegistry()
            throws NamingException {
        TransactionSynchronizationRegistry synch = NuxeoContainer.getTransactionSynchronizationRegistry();
        if (synch == null) {
            throw new NamingException("tx manager not installed");
        }
        return synch;
    }

    /**
     * Checks if there is no transaction
     *
     * @5.9.6
     */
    public static boolean isNoTransaction() {
        try {
            return lookupUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION;
        } catch (NamingException | SystemException cause) {
            return true;
        }
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
     * @return {@code true} if the transaction was successfully started,
     *         {@code false} otherwise
     */
    public static boolean startTransaction() {
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            return false;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Starting transaction");
            }
            ut.begin();
            return true;
        } catch (Exception e) {
            log.error("Unable to start transaction", e);
        }
        return false;
    }

    /**
     * Suspend the current transaction if active and start a new transaction
     *
     * @return the suspended transaction or null
     * @throws TransactionRuntimeException
     * @since 5.6
     */
    public static Transaction requireNewTransaction() {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) {
            return null;
        }
        try {
            Transaction tx = tm.getTransaction();
            if (tx != null) {
                tx = tm.suspend();
            }
            tm.begin();
            return tx;
        } catch (Exception e) {
            throw new TransactionRuntimeException("Cannot suspend tx", e);
        }
    }

    public static Transaction suspendTransaction() {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) {
            return null;
        }
        try {
            Transaction tx = tm.getTransaction();
            if (tx != null) {
                tx = tm.suspend();
            }
            return tx;
        } catch (Exception e) {
            throw new TransactionRuntimeException("Cannot suspend tx", e);
        }
    }

    /**
     * Commit the current transaction if active and resume the principal
     * transaction
     *
     * @param tx
     */
    public static void resumeTransaction(Transaction tx) {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) {
            return;
        }
        try {
            if (tm.getStatus() == Status.STATUS_ACTIVE) {
                tm.commit();
            }
            if (tx != null) {
                tm.resume(tx);
            }
        } catch (Exception e) {
            throw new TransactionRuntimeException("Cannot resume tx", e);
        }
    }

    /**
     * Starts a new User Transaction with the specified timeout.
     *
     * @param timeout the timeout in seconds, <= 0 for the default
     * @return {@code true} if the transaction was successfully started,
     *         {@code false} otherwise
     *
     * @since 5.6
     */
    public static boolean startTransaction(int timeout) {
        if (timeout < 0) {
            timeout = 0;
        }
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) {
            return false;
        }


        try {
            tm.setTransactionTimeout(timeout);
        } catch (SystemException e) {
            log.error("Unable to set transaction timeout: " + timeout, e);
            return false;
        }
        try {
            return startTransaction();
        } finally {
            try {
                tm.setTransactionTimeout(0);
            } catch (SystemException e) {
                log.error("Unable to reset transaction timeout", e);
            }
        }
    }

    /**
     * Commits or rolls back the User Transaction depending on the transaction
     * status.
     */
    public static void commitOrRollbackTransaction() {
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            return;
        }
        try {
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
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot commit transaction with unknown status: "
                            + status);
                }
            }
        } catch (Exception e) {
            String msg = "Unable to commit/rollback  " + ut;
            if (e instanceof RollbackException
                    && "Unable to commit: transaction marked for rollback".equals(e.getMessage())) {
                // don't log as error, this happens if there's a
                // ConcurrentModificationException at transaction end inside VCS
                log.debug(msg, e);
            } else {
                log.error(msg, e);
            }
            throw new TransactionRuntimeException(msg, e);
        }
    }

    private static ThreadLocal<List<Exception>> suppressedExceptions = new ThreadLocal<List<Exception>>();

    /**
     * After this, some exceptions during transaction commit may be suppressed
     * and remembered.
     *
     * @since 5.9.4
     */
    public static void noteSuppressedExceptions() {
        suppressedExceptions.set(new ArrayList<Exception>(1));
    }

    /**
     * If activated by {@linked #noteSuppressedExceptions}, remembers the exception.
     *
     * @since 5.9.4
     */
    public static void noteSuppressedException(Exception e) {
        List<Exception> exceptions = suppressedExceptions.get();
        if (exceptions != null) {
            exceptions.add(e);
        }
    }

    /**
     * Gets the suppressed exceptions, and stops remembering.
     *
     * @since 5.9.4
     */
    public static List<Exception> getSuppressedExceptions() {
        List<Exception> exceptions = suppressedExceptions.get();
        suppressedExceptions.remove();
        return exceptions == null ? Collections.<Exception> emptyList()
                : exceptions;
    }

    /**
     * Sets the current User Transaction as rollback only.
     *
     * @return {@code true} if the transaction was successfully marked rollback
     *         only, {@code false} otherwise
     */
    public static boolean setTransactionRollbackOnly() {
        log.info("Setting transaction as rollback only", new Throwable("rollback stack trace"));
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            return false;
        }
        try {
            ut.setRollbackOnly();
            return true;
        } catch (Exception cause) {
            log.error("Could not mark transaction as rollback only", cause);
        }
        return false;
    }

    public static void registerSynchronization(Synchronization handler) {
        if (!isTransactionActiveOrMarkedRollback()) {
            return;
        }
        try {
            NuxeoContainer.getTransactionManager().getTransaction()
                .registerSynchronization(handler);
        } catch (IllegalStateException | RollbackException | SystemException cause) {
            throw new RuntimeException(
                    "Cannot register synch handler in current tx", cause);
        }
    }


}
