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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.nuxeo.audit.storage.impl.DirectoryAuditStorage;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @author <a href="mailto:kchang@nuxeo.com">Ku Chang</a>
 *
 *         Retrieve audit logs from DB backup then populate them to audit
 *         backend (ES in production)
 *
 */
@Operation(id = SyncAuditLogsFromStorageOperation.ID, category = Constants.W_AUDIT_EVENT, label = "Audit Event: Migrate Audit Storage Events", description = "Migrate directory audit storage logs into audit backend")
public class SyncAuditLogsFromStorageOperation extends RestoreOperation {

    public static final String ID = "AuditStorage.Sync";

    public static final String RESTORE_AUDIT_STORAGE_LOG_KEY = "restore_audit_storage_log_key";
    public static final String RESTORE_AUDIT_STORAGE_LOG_VALUE = "restore_audit_storage_log_value";

    @Context
    protected NXAuditEventsService auditService;

    @Param(name = "after", required = false)
    protected Long after = Long.MIN_VALUE;

    @Param(name = "batchSize", required = false)
    protected Integer batchSize = DEAFULT_BATCH_SIZE;

    @Param(name = "keepAlive", required = false)
    protected Integer keepAlive = DEFAULT_KEEP_ALIVE_SECONDS;

    public static LogEntry getLogEntryFromJson(String jsonEntry) {
        try {
            return OBJECT_MAPPER.readValue(jsonEntry, LogEntryImpl.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static LogAppender<Record> getLogAppender() {
        StreamService streamService = Framework.getService(StreamService.class);
        return streamService.getLogManager(getLogConfig()).getAppender(StreamAuditEventListener.STREAM_NAME);
    }

    protected static String getLogConfig() {
        return Framework.getProperty(StreamAuditEventListener.AUDIT_LOG_CONFIG_PROP,
                StreamAuditEventListener.DEFAULT_LOG_CONFIG);
    }

    protected static long getTimestampForEntry(LogEntry entry) {
        if (entry.getEventDate() != null) {
            return entry.getEventDate().getTime();
        }
        return System.currentTimeMillis();
    }

    protected static String asJson(LogEntry entry) {
        if (entry == null) {
            return null;
        }
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        try {
            return MarshallerHelper.objectToJson(entry, ctx);
        } catch (IOException e) {
            return null;
        }
    }

    protected static void writeEntry(LogAppender<Record> appender, LogEntry entry) {
        String json = asJson(entry);
        if (json == null) {
            return;
        }
        long timestamp = getTimestampForEntry(entry);
        appender.append(0, new Record(String.valueOf(entry.getId()), json.getBytes(UTF_8),
                Watermark.ofTimestamp(timestamp).getValue()));
    }

    /*
     * for restoring audit logs from auidt storage, append messages to stream for
     * better performance/scalability.
     */
    /*
     * add data to restore messages' extended info field, so
     * StreamAuditStorageWriter can filter out restore messages to avoid duplication
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void writeEntries(Object storageDestination, List<? extends Object> entries) {
        LogAppender<Record> appender = (LogAppender<Record>) storageDestination;
        List<LogEntry> logEntries = (List<LogEntry>) entries;

        logEntries.forEach(entry -> {
            entry.getExtendedInfos().put(RESTORE_AUDIT_STORAGE_LOG_KEY,
                    ExtendedInfoImpl.createExtendedInfo(RESTORE_AUDIT_STORAGE_LOG_VALUE));
            writeEntry(appender, entry);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void populateResultList(List<? extends Object> resultList, String logString, LogEntry logEntry) {
        ((List<LogEntry>) resultList).add(logEntry);
    }

    /*
     * for restoring audit logs from auidt storage, source is directory audit
     * storage, and target is LogAppender (Stream)
     */
    @OperationMethod
    public Blob run(Long before) {
        AuditStorage auditStorage = auditService.getAuditStorage(DirectoryAuditStorage.NAME);
        LogAppender<Record> appender = getLogAppender();

        return generateOutput(restore(auditStorage, appender, batchSize, keepAlive, before, after));
    }
}
