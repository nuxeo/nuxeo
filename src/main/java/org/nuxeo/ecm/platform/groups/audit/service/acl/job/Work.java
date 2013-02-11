package org.nuxeo.ecm.platform.groups.audit.service.acl.job;

import static org.nuxeo.ecm.core.work.api.Work.State.SUSPENDED;

import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.transaction.TransactionHelper;

/** A work able to start transactions with a custom timeout, and able
 * to return this timeout.
 *
 * @author Martin Pernollet
 */
public class Work extends AbstractWork implements ITimeoutWork{
    public Work(String name, int timeout) {
        super();
        this.name = name;
        this.timeout = timeout;
    }

    public Work(Runnable runnable, String name, int timeout) {
        this.runnable = runnable;
        this.name = name;
        this.timeout = timeout;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public void work() throws Exception {
        runnable.run();
    }

    /** Override to fix transaction timeout. */
    protected boolean startTransaction() {
        if (isTransactional() && !isTransactionStarted) {
            isTransactionStarted = TransactionHelper.startTransaction(getTimeout());
        }
        return isTransactionStarted;
    }

    /** Override to fix transaction timeout. */
    @Override
    public void run() {
        if (state == SUSPENDED) {
            return;
        }
        boolean tx = isTransactional();
        if (tx) {
            isTransactionStarted = TransactionHelper.startTransaction(getTimeout());
        }
        boolean ok = false;
        Exception err = null;
        try {
            work();
            ok = true;
        } catch (Exception e) {
            err = e;
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            try {
                cleanUp(ok, err);
            } finally {
                try {
                    if (tx && isTransactionStarted) {
                        if (!ok) {
                            TransactionHelper.setTransactionRollbackOnly();
                        }
                        TransactionHelper.commitOrRollbackTransaction();
                        isTransactionStarted = false;
                    }
                } finally {
                    if (err instanceof InterruptedException) {
                        // restore interrupted status for the thread pool worker
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }



    protected Runnable runnable;
    protected String name;
    protected int timeout;
}
