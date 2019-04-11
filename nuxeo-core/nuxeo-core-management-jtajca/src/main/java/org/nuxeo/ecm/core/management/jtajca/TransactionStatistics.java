/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.util.Date;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

/**
 * @author matic
 */
public interface TransactionStatistics {

    enum Status {

        ACTIVE(javax.transaction.Status.STATUS_ACTIVE), COMMITTED(javax.transaction.Status.STATUS_COMMITTED), COMMITTING(
                javax.transaction.Status.STATUS_COMMITTING), MARKED_ROLLLEDBACK(
                javax.transaction.Status.STATUS_MARKED_ROLLBACK), NO_TRANSACTION(
                javax.transaction.Status.STATUS_NO_TRANSACTION), PREPARED(javax.transaction.Status.STATUS_PREPARED), PREPARING(
                javax.transaction.Status.STATUS_PREPARING), ROLLEDBACK(javax.transaction.Status.STATUS_ROLLEDBACK), ROLLING_BACK(
                javax.transaction.Status.STATUS_ROLLING_BACK), UNKNOWN(javax.transaction.Status.STATUS_UNKNOWN);

        int code;

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
