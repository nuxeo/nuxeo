package org.nuxeo.ecm.platform.web.common.tx;

import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleTxManager {


	private static final Log log = LogFactory.getLog(SimpleTxManager.class);

	  /**
     * Starts a new {@link UserTransaction}.
     *
     * @return true if the transaction was successfully translated, false
     *         otherwise
     */
    public static boolean startUserTransaction() {
        try {
            TransactionsHelper.getUserTransaction().begin();
        } catch (Exception e) {
            log.error("Unable to start transaction", e);
            return false;
        }
        return true;
    }

    /**
     * Marks the {@link UserTransaction} for rollBack.
     */
    public static void markTransactionForRollBack() {
        try {
            TransactionsHelper.getUserTransaction().setRollbackOnly();
            if (log.isDebugEnabled()) {
                log.debug("setting transaction to RollBackOnly");
            }
        } catch (Exception e) {
            log.error("Unable to rollback transaction", e);
        }
    }

    /**
     * Commits or rollbacks the {@link UserTransaction} depending on the
     * Transaction status.
     */
    public static void commitOrRollBackUserTransaction() {
        try {
            if (TransactionsHelper.isTransactionActiveOrMarkedRollback()) {
                if (TransactionsHelper.isTransactionMarkedRollback()) {
                    if (log.isDebugEnabled()) {
                        log.debug("can not commit transaction since it is marked RollBack only");
                    }
                    TransactionsHelper.getUserTransaction().rollback();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("commiting transaction");
                    }
                    TransactionsHelper.getUserTransaction().commit();
                }
            }
        } catch (Exception e) {
                log.error("Unable to commit/rollback transaction", e);
        }
    }

}
