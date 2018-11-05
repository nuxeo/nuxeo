/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.elasticsearch.audit;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Work for the SQL to Elasticsearch audit migration.
 *
 * @since 7.10
 */
public class ESAuditMigrationWork extends AbstractWork {

    private static final long serialVersionUID = 3764830939638449534L;

    private static final Log log = LogFactory.getLog(ESAuditMigrationWork.class);

    protected int batchSize;

    public ESAuditMigrationWork(String id, int batchSize) {
        super(id);
        this.batchSize = batchSize;
    }

    @Override
    public String getTitle() {
        return "Audit migration worker";
    }

    @Override
    public void work() {

        NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        AuditBackendDescriptor config = new AuditBackendDescriptor();
        AuditBackend sourceBackend = config.newInstance(auditService);
        sourceBackend.onApplicationStarted();

        try {
        @SuppressWarnings("unchecked")
        List<Long> res = (List<Long>) sourceBackend.nativeQuery("select count(*) from LogEntry", 1, 20);
        long nbEntriesToMigrate = res.get(0);

        AuditLogger destBackend = auditService.getBackend();

        TransactionHelper.commitOrRollbackTransaction();
            long t0 = System.currentTimeMillis();
            long nbEntriesMigrated = 0;
            int pageIdx = 1;

            while (nbEntriesMigrated < nbEntriesToMigrate) {
                boolean txStarted = TransactionHelper.startTransaction();
                List<LogEntry> entries;
                if (txStarted) {
                    // @SuppressWarnings("unchecked")
                    entries = (List<LogEntry>) sourceBackend.nativeQuery(
            	                         "from LogEntry log order by log.id asc", pageIdx, batchSize);
                    TransactionHelper.commitOrRollbackTransaction();
                } else {
                    throw new NuxeoException("Cannot start a transaction");
                }

                if (entries.size() == 0) {
                    log.warn("Migration ending after " + nbEntriesMigrated + " entries");
                    break;
                }
                setProgress(new Progress(nbEntriesMigrated, nbEntriesToMigrate));
                destBackend.addLogEntries(entries);
                pageIdx++;
                nbEntriesMigrated += entries.size();
                log.info("Migrated " + nbEntriesMigrated + " log entries on " + nbEntriesToMigrate);
                double dt = (System.currentTimeMillis() - t0) / 1000.0;
                if (dt != 0) {
                    log.info("Migration speed: " + (nbEntriesMigrated / dt) + " entries/s");
                }
            }
            log.info("Audit migration from SQL to Elasticsearch done: " + nbEntriesMigrated + " entries migrated");

            // Log technical event in audit as a flag to know if the migration has been processed at application
            // startup
            AuditLogger logger = Framework.getService(AuditLogger.class);
            LogEntry entry = logger.newLogEntry();
            entry.setCategory("NuxeoTechnicalEvent");
            entry.setEventId(ESAuditBackend.MIGRATION_DONE_EVENT);
            entry.setPrincipalName(SecurityConstants.SYSTEM_USERNAME);
            entry.setEventDate(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
            destBackend.addLogEntries(Collections.singletonList(entry));
        } finally {
            sourceBackend.onApplicationStopped();
        }
    }

}
