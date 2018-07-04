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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.BulkService;

/**
 * Wait for Bulk computation. This operation is meant to be used for tests. Its usage in production is not recommended.
 *
 * @since 10.2
 */
@Operation(id = BulkWaitForAction.ID, category = Constants.CAT_SERVICES, label = "Wait for Bulk computation",
        since = "10.2",
        description = "Wait until Bulk computation is done. This operation is meant to be used for tests. Its usage in production is not recommended.")
public class BulkWaitForAction {

    public static final String ID = "Bulk.WaitForAction";

    @Context
    protected BulkService bulkService;

    @Context
    protected CoreSession repo;

    @Param(name = "bulkId")
    protected String bulkId;

    @Param(name = "timeoutSecond", required = false)
    protected long timeout = 60L;

    @OperationMethod
    public Boolean run() {
        try {
            if (!bulkService.await(bulkId, timeout, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
        } catch (InterruptedException | TimeoutException e) {
            if (ExceptionUtils.hasInterruptedCause(e)) {
                // reset interrupted status
                Thread.currentThread().interrupt();
            }
            return FALSE;
        }
        return TRUE;
    }

}
