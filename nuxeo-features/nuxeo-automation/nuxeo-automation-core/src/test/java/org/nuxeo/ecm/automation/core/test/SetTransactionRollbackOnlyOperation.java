/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Just sets the transaction as rollback-only.
 */
@Operation(id = SetTransactionRollbackOnlyOperation.ID)
public class SetTransactionRollbackOnlyOperation {

    public static final String ID = "Test.SetTransactionRollbackOnlyOperation";

    @OperationMethod
    public void run() {
        // commit and start new transaction to disconnect all sessions
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // mark new transaction rollback-only
        TransactionHelper.setTransactionRollbackOnly();
    }

}
