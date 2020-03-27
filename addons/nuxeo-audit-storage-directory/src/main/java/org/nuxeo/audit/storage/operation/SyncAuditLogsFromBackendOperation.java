/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     - Ku Chang <kchang@nuxeo.com>
 */
package org.nuxeo.audit.storage.operation;

import java.util.List;

import org.nuxeo.audit.storage.impl.DirectoryAuditStorage;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;


@Operation(id = SyncAuditLogsFromBackendOperation.ID, category = Constants.W_AUDIT_EVENT, label = "Audit Event: Migrate Audit Backend Events", description = "Migrate audit backend logs into audit storage")
public class SyncAuditLogsFromBackendOperation extends RestoreOperation {

    public static final String ID = "AuditBackend.Sync";

    @Context
    protected AuditBackend auditBackend;

    @Context
    protected NXAuditEventsService auditService;

    @Param(name = "after", required = false)
    protected Long after = Long.MIN_VALUE;

    @Param(name = "batchSize", required = false)
    protected Integer batchSize = DEAFULT_BATCH_SIZE;

    @Param(name = "keepAlive", required = false)
    protected Integer keepAlive = DEFAULT_KEEP_ALIVE_SECONDS;

    @Override
    @SuppressWarnings("unchecked")
    protected void populateResultList(List<? extends Object> resultList, String logString, LogEntry logEntry) {
        ((List<String>) resultList).add(logString);
    }

    /*
     * for restoring audit logs from backend, write logs to directory audit
     * storage
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void writeEntries(Object storageDestination, List<? extends Object> entries) {
        ((AuditStorage) storageDestination).append((List<String>) entries);
    }

    /*
     * restore source is audit backend, destination is directory audit storage
     */
    @OperationMethod
    public Blob run(Long before) {
        AuditStorage dest = auditService.getAuditStorage(DirectoryAuditStorage.NAME);

        return generateOutput(restore((DefaultAuditBackend) auditBackend, dest, batchSize, keepAlive, before, after));
    }
}
