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

            UserTransaction ut = TransactionsHelper.getUserTransaction();
            if (ut==null) {
                return false;
            }
            ut.begin();
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
            UserTransaction ut = TransactionsHelper.getUserTransaction();
            if (ut==null) {
                return;
            }
            ut.setRollbackOnly();
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
            UserTransaction ut = TransactionsHelper.getUserTransaction();
            if (ut==null) {
                return;
            }
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
