package org.nuxeo.ecm.platform.groups.audit.service.acl.job;

import static org.nuxeo.ecm.core.work.api.Work.State.SUSPENDED;

import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * A work able to start transactions with a custom timeout, and able to return
 * this timeout.
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class Work extends AbstractWork implements ITimeoutable {
    public static final String PROPERTY_ACL_AUDIT_TIMEOUT = "nuxeo.audit.acl.timeout";

    public static final int DEFAULT_TIMEOUT = 1200; // 20 min

    public static final int UNDEFINED_TIMEOUT = -1;

    protected Runnable runnable;

    protected String name;

    protected int timeout;

    /**
     * Uses timeout defined from properties. If undefined,
     * {@link DEFAULT_TIMEOUT} will be used.
     */
    public Work(String name) {
        this(null, name, getAclAuditTimeoutFromProperties());
    }

    public Work(String name, int timeout) {
        this(null, name, timeout);
    }

    /**
     * Uses timeout defined from properties. If undefined,
     * {@link DEFAULT_TIMEOUT} will be used.
     */
    public Work(Runnable runnable, String name) {
        this(runnable, name, getAclAuditTimeoutFromProperties());
    }

    /** If timeout is {@link UNDEFINED_TIMEOUT}, uses {@link DEFAULT_TIMEOUT}. */
    public Work(Runnable runnable, String name, int timeout) {
        super();
        this.runnable = runnable;
        this.name = name;
        this.timeout = timeout;

        if (this.timeout == UNDEFINED_TIMEOUT) {
            this.timeout = DEFAULT_TIMEOUT;
        }
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public void work() throws Exception {
        setProgress(Progress.PROGRESS_0_PC);
        runnable.run();
        setProgress(Progress.PROGRESS_100_PC);
    }

    /** Override to fix transaction timeout. */
    @Override
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

    @Override
    public int getTimeout() {
        return timeout;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public static int getAclAuditTimeoutFromProperties() {
        String v = Framework.getProperty(PROPERTY_ACL_AUDIT_TIMEOUT,
                UNDEFINED_TIMEOUT + "");
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return UNDEFINED_TIMEOUT;
        }
    }
}
