/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.util.Date;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

/**
 * @author matic
 * 
 */
public interface TransactionStatistics {

    enum Status {

        ACTIVE(javax.transaction.Status.STATUS_ACTIVE), COMMITTED(
                javax.transaction.Status.STATUS_COMMITTED), COMMITTING(
                javax.transaction.Status.STATUS_COMMITTING), MARKED_ROLLLEDBACK(
                javax.transaction.Status.STATUS_MARKED_ROLLBACK), NO_TRANSACTION(
                javax.transaction.Status.STATUS_NO_TRANSACTION), PREPARED(
                javax.transaction.Status.STATUS_PREPARED), PREPARING(
                javax.transaction.Status.STATUS_PREPARING), ROLLEDBACK(
                javax.transaction.Status.STATUS_ROLLEDBACK), ROLLING_BACK(
                javax.transaction.Status.STATUS_ROLLING_BACK), UNKNOWN(
                javax.transaction.Status.STATUS_UNKNOWN);

        public final int code;

        Status(int code) {
            this.code = code;
        }

        public static Status fromCode(int code) {
            for (Status e : Status.values()) {
                if (e.code == code) {
                    return e;
                }
            }
            return UNKNOWN;
        }

        public static Status fromTx(Transaction tx) {
            try {
                return fromCode(tx.getStatus());
            } catch (SystemException e) {
                return UNKNOWN;
            }
        }
    }

    String getId();

    String getThreadName();

    Status getStatus();

    Date getStartDate();

    String getStartCapturedContextMessage();

    Date getEndDate();

    String getEndCapturedContextMessage();

    long getDuration();

    boolean isEnded();

}