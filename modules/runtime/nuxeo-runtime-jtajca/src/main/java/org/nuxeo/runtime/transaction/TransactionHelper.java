/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.runtime.transaction;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * Utilities to work with transactions.
 */
public class TransactionHelper {

    private static final Logger log = LogManager.getLogger(TransactionHelper.class);

    private static final Field GERONIMO_TRANSACTION_TIMEOUT_FIELD = FieldUtils.getField(
            org.apache.geronimo.transaction.manager.TransactionImpl.class, "timeout", true);

    /**
     * Thread pool used to execute code in a separate transactional context.
     *
     * @since 11.1
     */
    // like Executors.newCachedThreadPool() but using a small keepAliveTime to avoid blocking shutdown
    protected static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    private TransactionHelper() {
        // utility class
    }

    /**
     * Looks up the User Transaction in JNDI.
     *
     * @return the User Transaction
     * @throws NamingException if not found
     */
    public static UserTransaction lookupUserTransaction() throws NamingException {
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            throw new NamingException("tx manager not installed");
        }
        return ut;
    }

    /**
     * Gets the transaction status.
     *
     * @return the transaction {@linkplain Status status}, or -1 if there is no transaction manager
     * @since 8.4
     * @see Status
     */
    public static int getTransactionStatus() {
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            return -1;
        }
        try {
            return ut.getStatus();
        } catch (SystemException e) {
            throw new TransactionRuntimeException("Cannot get transaction status", e);
        }
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
    public static TransactionManager lookupTransactionManager() throws NamingException {
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
    public static TransactionSynchronizationRegistry lookupSynchronizationRegistry() throws NamingException {
        TransactionSynchronizationRegistry synch = NuxeoContainer.getTransactionSynchronizationRegistry();
        if (synch == null) {
            throw new NamingException("tx manager not installed");
        }
        return synch;
    }

    /**
     * Checks if there is no transaction
     *
     * @since 6.0
     */
    public static boolean isNoTransaction() {
        int status = getTransactionStatus();
        return status == Status.STATUS_NO_TRANSACTION || status == -1;
    }

    /**
     * Checks if the current User Transaction is active.
     */
    public static boolean isTransactionActive() {
        int status = getTransactionStatus();
        return status == Status.STATUS_ACTIVE;
    }

    /**
     * Checks if the current User Transaction is marked rollback only.
     */
    public static boolean isTransactionMarkedRollback() {
        int status = getTransactionStatus();
        return status == Status.STATUS_MARKED_ROLLBACK;
    }

    /**
     * Checks if the current User Transaction is active or marked rollback only.
     */
    public static boolean isTransactionActiveOrMarkedRollback() {
        int status = getTransactionStatus();
        return status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
    }

    /**
     * Checks if the current User Transaction is active or preparing.
     *
     * @since 8.4
     */
    public static boolean isTransactionActiveOrPreparing() {
        int status = getTransactionStatus();
        return status == Status.STATUS_ACTIVE || status == Status.STATUS_PREPARING;
    }

    /**
     * Checks if the current User Transaction has already timed out, i.e., whether a commit would immediately abort with
     * a timeout exception.
     *
     * @return {@code true} if there is a current transaction that has timed out, {@code false} otherwise
     * @since 7.1
     */
    public static boolean isTransactionTimedOut() {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) {
            return false;
        }
        try {
            Transaction tx = tm.getTransaction();
            if (tx == null || tx.getStatus() != Status.STATUS_ACTIVE) {
                return false;
            }
            if (tx instanceof org.apache.geronimo.transaction.manager.TransactionImpl) {
                // Geronimo Transaction Manager
                Long timeout = (Long) GERONIMO_TRANSACTION_TIMEOUT_FIELD.get(tx);
                return System.currentTimeMillis() > timeout.longValue();
            } else {
                // unknown transaction manager
                return false;
            }
        } catch (SystemException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the transaction TTL.
     *
     * @return the time to live in second, {@code 0} if the transaction has already timed out, {@code -1} when outside
     *         of a transaction.
     * @since 11.5
     */
    public static int getTransactionTimeToLive() {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) {
            return -1;
        }
        try {
            Transaction tx = tm.getTransaction();
            if (!(tx instanceof org.apache.geronimo.transaction.manager.TransactionImpl)) {
                // Only geronimo manager is handled
                return -1;
            }
            int status = tx.getStatus();
            if (status != Status.STATUS_ACTIVE && status != Status.STATUS_MARKED_ROLLBACK) {
                return -1;
            }
            long ttl = ((Long) GERONIMO_TRANSACTION_TIMEOUT_FIELD.get(tx) - System.currentTimeMillis()) / 1000;
            return ttl > 0 ? Math.toIntExact(ttl) : 0;
        } catch (SystemException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the current User Transaction has already timed out, i.e., whether a commit would immediately abort with
     * a timeout exception.
     * <p>
     * Throws if the transaction has timed out.
     *
     * @throws TransactionRuntimeException if the transaction has timed out
     * @since 8.4
     */
    public static void checkTransactionTimeout() throws TransactionRuntimeException {
        if (isTransactionTimedOut()) {
            throw new TransactionRuntimeException("Transaction has timed out");
        }
    }

    /**
     * Starts a new User Transaction.
     *
     * @return {@code true} if the transaction was successfully started, {@code false} otherwise
     */
    public static boolean startTransaction() {
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            return false;
        }
        try {
            log.debug("Starting transaction");
            ut.begin();
            return true;
        } catch (NotSupportedException | SystemException e) {
            log.error("Unable to start transaction", e);
        }
        return false;
    }

    /**
     * Suspend the current transaction if active and start a new transaction
     *
     * @return the suspended transaction or null
     * @since 5.6
     * @deprecated since 11.1, as not all backends (transaction resource managers) allow suspending the transaction or
     *             transaction interleaving, instead use {@link #runInNewTransaction} or {@link #runWithoutTransaction}
     *             explicitly
     */
    @Deprecated(since = "11.1")
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
        } catch (NotSupportedException | SystemException e) {
            throw new TransactionRuntimeException("Cannot suspend tx", e);
        }
    }

    /**
     * Suspends the current transaction and returns
     *
     * @return the suspended transaction or null
     * @deprecated since 11.1, as not all backends (transaction resource managers) allow suspending the transaction or
     *             transaction interleaving, instead use {@link #runInNewTransaction} or {@link #runWithoutTransaction}
     *             explicitly
     */
    @Deprecated(since = "11.1")
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
        } catch (SystemException e) {
            throw new TransactionRuntimeException("Cannot suspend tx", e);
        }
    }

    /**
     * Commit the current transaction if active and resume the principal transaction
     *
     * @deprecated since 11.1, as not all backends (transaction resource managers) allow suspending the transaction or
     *             transaction interleaving, instead use {@link #runInNewTransaction} or {@link #runWithoutTransaction}
     *             explicitly
     */
    @Deprecated(since = "11.1")
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
        } catch (SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException
                | InvalidTransactionException | IllegalStateException | SecurityException e) {
            throw new TransactionRuntimeException("Cannot resume tx", e);
        }
    }

    /**
     * Starts a new User Transaction with the specified timeout.
     *
     * @param timeout the timeout in seconds, &lt;= 0 for the default
     * @return {@code true} if the transaction was successfully started, {@code false} otherwise
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
            log.error("Unable to set transaction timeout: {}", timeout, e);
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
     * Commits or rolls back the User Transaction depending on the transaction status.
     */
    public static void commitOrRollbackTransaction() {
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            return;
        }
        noteSuppressedExceptions();
        RuntimeException thrown = null;
        boolean isRollbackDuringCommit = false;
        try {
            int status = ut.getStatus();
            if (status == Status.STATUS_ACTIVE) {
                log.debug("Committing transaction");
                try {
                    ut.commit();
                } catch (HeuristicRollbackException | HeuristicMixedException e) {
                    throw new TransactionRuntimeException(e.getMessage(), e);
                } catch (RollbackException e) {
                    // from org.apache.geronimo.transaction.manager.TransactionImpl.commit
                    Throwable cause = e.getCause();
                    String msg;
                    if (cause != null && "Transaction has timed out".equals(cause.getMessage())) {
                        msg = "Unable to commit: Transaction timeout";
                    } else {
                        // this happens if there's a ConcurrentUpdateException at transaction end inside VCS
                        isRollbackDuringCommit = true;
                        msg = e.getMessage();
                    }
                    log.debug("Unable to commit", e);
                    throw new TransactionRuntimeException(msg, e);
                }
            } else if (status == Status.STATUS_MARKED_ROLLBACK) {
                log.debug("Cannot commit transaction because it is marked rollback only");
                ut.rollback();
            } else {
                log.debug("Cannot commit transaction with unknown status: {}", status);
            }
        } catch (SystemException e) {
            thrown = new TransactionRuntimeException(e);
            throw thrown;
        } catch (RuntimeException e) {
            thrown = e;
            throw thrown;
        } finally {
            List<Exception> suppressed = getSuppressedExceptions();
            if (!suppressed.isEmpty()) {
                // add suppressed to thrown exception, or throw a new one
                RuntimeException e;
                if (thrown == null) {
                    e = new TransactionRuntimeException("Exception during commit");
                } else {
                    if (isRollbackDuringCommit && suppressed.get(0) instanceof RuntimeException) {
                        // use the suppressed one directly and throw it instead
                        thrown = null; // force rethrow below
                        e = (RuntimeException) suppressed.remove(0);
                    } else {
                        e = thrown;
                    }
                }
                suppressed.forEach(e::addSuppressed);
                if (thrown == null) {
                    throw e;
                }
            }
        }
    }

    private static final ThreadLocal<List<Exception>> suppressedExceptions = new ThreadLocal<>();

    /**
     * After this, some exceptions during transaction commit may be suppressed and remembered.
     *
     * @since 5.9.4
     */
    protected static void noteSuppressedExceptions() {
        suppressedExceptions.set(new ArrayList<>());
    }

    /**
     * Remembers the exception if it happens during the processing of a commit, so that it can be surfaced as a
     * suppressed exception at the end of the commit.
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
    protected static List<Exception> getSuppressedExceptions() {
        List<Exception> exceptions = suppressedExceptions.get();
        suppressedExceptions.remove();
        return exceptions == null ? List.of() : exceptions;
    }

    /**
     * Sets the current User Transaction as rollback only.
     *
     * @return {@code true} if the transaction was successfully marked rollback only, {@code false} otherwise
     */
    public static boolean setTransactionRollbackOnly() {
        log.debug("Setting transaction as rollback only");
        if (log.isTraceEnabled()) {
            log.trace("Rollback stack trace", new Throwable("Rollback stack trace"));
        }
        UserTransaction ut = NuxeoContainer.getUserTransaction();
        if (ut == null) {
            return false;
        }
        try {
            ut.setRollbackOnly();
            return true;
        } catch (IllegalStateException | SystemException cause) {
            log.error("Could not mark transaction as rollback only", cause);
        }
        return false;
    }

    /**
     * Sets the current User Transaction as rollback only if it has timed out.
     *
     * @return {@code true} if the transaction was successfully marked rollback only, {@code false} otherwise
     * @since 7.1
     */
    public static boolean setTransactionRollbackOnlyIfTimedOut() {
        if (isTransactionTimedOut()) {
            return setTransactionRollbackOnly();
        }
        return false;
    }

    public static void registerSynchronization(Synchronization handler) {
        if (!isTransactionActiveOrPreparing()) {
            throw new TransactionRuntimeException("Cannot register Synchronization if transaction is not active");
        }
        try {
            NuxeoContainer.getTransactionManager().getTransaction().registerSynchronization(handler);
        } catch (IllegalStateException | RollbackException | SystemException cause) {
            throw new RuntimeException("Cannot register synch handler in current tx", cause);
        }
    }

    /**
     * Enlists a XA resource in the current transaction.
     *
     * @param xaRes the XA resource
     * @since 11.1
     */
    public static void enlistResource(XAResource xaRes) {
        if (!isTransactionActiveOrMarkedRollback()) {
            throw new TransactionRuntimeException("Cannot enlist XA resource if transaction is not active");
        }
        try {
            NuxeoContainer.getTransactionManager().getTransaction().enlistResource(xaRes);
        } catch (IllegalStateException | RollbackException | SystemException cause) {
            throw new RuntimeException("Cannot enlist XA resource in current tx", cause);
        }
    }

    /**
     * Runs the given {@link Runnable} without a transactional context.
     *
     * @param runnable the {@link Runnable}
     * @since 9.1
     */
    public static void runWithoutTransaction(Runnable runnable) {
        runWithoutTransaction(() -> { runnable.run(); return null; });
    }

    /**
     * Calls the given {@link Supplier} without a transactional context.
     *
     * @param supplier the {@link Supplier}
     * @return the supplier's result
     * @since 9.1
     */
    public static <R> R runWithoutTransaction(Supplier<R> supplier) {
        return runWithoutTransactionInternal(() -> runAndCleanupTransactionContext(supplier));
    }

    /**
     * Runs the given {@link Runnable} in a new transactional context.
     *
     * @param runnable the {@link Runnable}
     * @since 9.1
     */
    public static void runInNewTransaction(Runnable runnable) {
        runInNewTransaction(() -> { runnable.run(); return null;});
    }

    /**
     * Calls the given {@link Supplier} in a new transactional context.
     *
     * @param supplier the {@link Supplier}
     * @return the supplier's result
     * @since 9.1
     */
    public static <R> R runInNewTransaction(Supplier<R> supplier) {
        return runWithoutTransaction(() -> runInTransaction(supplier));
    }

    /**
     * Runs the given {@link Runnable} in a transactional context. Will not start a new transaction if one already
     * exists.
     *
     * @param runnable the {@link Runnable}
     * @since 8.4
     */
    public static void runInTransaction(Runnable runnable) {
        runInTransaction(0, runnable);
    }

    /**
     * Runs the given {@link Runnable} in a transactional context. Will not start a new transaction if one already
     * exists.
     *
     * @param timeout the timeout in seconds, &lt;= 0 for the default
     * @param runnable the {@link Runnable}
     * @since 11.5
     */
    public static void runInTransaction(int timeout, Runnable runnable) {
        runInTransaction(timeout, () -> {runnable.run(); return null;});
    }

    /**
     * Calls the given {@link Supplier} in a transactional context. Will not start a new transaction if one already
     * exists.
     *
     * @param supplier the {@link Supplier}
     * @return the supplier's result
     * @since 8.4
     */
    public static <R> R runInTransaction(Supplier<R> supplier) {
        return runInTransaction(0, supplier);
    }

    /**
     * Calls the given {@link Supplier} in a transactional context. Will not start a new transaction if one already
     * exists.
     *
     * @param timeout the timeout in seconds, &lt;= 0 for the default
     * @param supplier the {@link Supplier}
     * @return the supplier's result
     * @since 11.5
     */
    public static <R> R runInTransaction(int timeout, Supplier<R> supplier) {
        boolean startTransaction = !isTransactionActiveOrMarkedRollback();
        if (startTransaction) {
            if (!startTransaction(timeout)) {
                throw new TransactionRuntimeException("Cannot start transaction");
            }
        }
        boolean completedAbruptly = true;
        try {
            R result = supplier.get();
            completedAbruptly = false;
            return result;
        } finally {
            try {
                if (completedAbruptly) {
                    setTransactionRollbackOnly();
                }
            } finally {
                if (startTransaction) {
                    commitOrRollbackTransaction();
                }
            }
        }
    }

    /**
     * Calls the given {@link Supplier} in a context without a transaction. The supplier must do its own transactional
     * cleanup to restore the thread to a pristine state.
     *
     * @param supplier the {@link Supplier}
     * @return the supplier's result
     * @since 11.1
     */
    protected static <R> R runWithoutTransactionInternal(Supplier<R> supplier) {
        // if there is already no transaction, run in this thread
        if (isNoTransaction()) {
            return supplier.get();
        }
        // otherwise use a separate thread to get a separate transactional context
        try {
            return EXECUTOR.submit(supplier::get).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted status
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    /**
     * Calls the given {@link Supplier} and cleans up the transaction context afterwards.
     *
     * @param supplier the {@link Supplier}
     * @return the supplier's result
     * @since 11.1
     */
    protected static <R> R runAndCleanupTransactionContext(Supplier<R> supplier) {
        try {
            return supplier.get();
        } finally {
            if (!isNoTransaction()) {
                // restore the no-transaction context of this thread
                try {
                    commitOrRollbackTransaction();
                } catch (TransactionRuntimeException e) {
                    log.error("Failed to commit/rollback", e);
                }
            }
        }
    }

}
