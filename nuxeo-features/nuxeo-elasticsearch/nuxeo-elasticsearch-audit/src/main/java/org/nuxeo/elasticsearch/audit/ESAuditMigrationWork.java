/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
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

        final AuditBackend sourceBackend = new DefaultAuditBackend();
        sourceBackend.activate(auditService);

        @SuppressWarnings("unchecked")
        List<Long> res = (List<Long>) sourceBackend.nativeQuery("select count(*) from LogEntry", 1, 20);
        final long nbEntriesToMigrate = res.get(0);

        final AuditLogger destBackend = auditService.getBackend();

        TransactionHelper.commitOrRollbackTransaction();
        try {
            long t0 = System.currentTimeMillis();
            long nbEntriesMigrated = 0;
            int pageIdx = 1;

            while (nbEntriesMigrated < nbEntriesToMigrate) {
                @SuppressWarnings("unchecked")
                List<LogEntry> entries = (List<LogEntry>) sourceBackend.nativeQuery(
                        "from LogEntry log order by log.id asc", pageIdx, batchSize);

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
            TransactionHelper.startTransaction();
            sourceBackend.deactivate();
        }
    }

}
