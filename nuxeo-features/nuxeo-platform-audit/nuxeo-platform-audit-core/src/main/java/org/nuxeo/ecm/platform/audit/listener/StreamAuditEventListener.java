/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.platform.audit.listener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * An events collector that write log entries as json record into a stream.
 *
 * @since 9.3
 */
public class StreamAuditEventListener implements EventListener, Synchronization {
    private static final Log log = LogFactory.getLog(StreamAuditEventListener.class);

    protected static final ThreadLocal<Boolean> isEnlisted = ThreadLocal.withInitial(() -> Boolean.FALSE);

    protected static final ThreadLocal<List<LogEntry>> entries = ThreadLocal.withInitial(ArrayList::new);

    public static final String STREAM_AUDIT_ENABLED_PROP = "nuxeo.stream.audit.enabled";

    public static final String AUDIT_LOG_CONFIG_PROP = "nuxeo.stream.audit.log.config";

    public static final String DEFAULT_LOG_CONFIG = "audit";

    public static final String STREAM_NAME = "audit";

    @Override
    public void handleEvent(Event event) {
        AuditLogger logger = Framework.getService(AuditLogger.class);
        if (logger == null) {
            return;
        }
        if (!isEnlisted.get()) {
            isEnlisted.set(registerSynchronization(this));
            entries.get().clear();
            if (log.isDebugEnabled()) {
                log.debug("AuditEventListener collecting entries for the tx");
            }
        }
        if (logger.getAuditableEventNames().contains(event.getName())) {
            entries.get().add(logger.buildEntryFromEvent(event));
        }
        if (!isEnlisted.get()) {
            // there is no transaction so don't wait for a commit
            afterCompletion(Status.STATUS_COMMITTED);
        }

    }

    @Override
    public void beforeCompletion() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("AuditEventListener going to write %d entries.", entries.get().size()));
        }
    }

    @Override
    public void afterCompletion(int status) {
        try {
            if (entries.get().isEmpty()
                    || (Status.STATUS_MARKED_ROLLBACK == status || Status.STATUS_ROLLEDBACK == status)) {
                // This means that in case of rollback there is no event logged
                return;
            }
            writeEntries();
            if (log.isDebugEnabled()) {
                log.debug(String.format("AuditEventListener writes %d entries.", entries.get().size()));
            }
        } finally {
            isEnlisted.set(false);
            entries.get().clear();
        }
    }

    protected void writeEntries() {
        if (entries.get().isEmpty()) {
            return;
        }
        LogAppender<Record> appender = getLogManager().getAppender(STREAM_NAME);
        entries.get().forEach(entry -> writeEntry(appender, entry));
    }

    protected void writeEntry(LogAppender<Record> appender, LogEntry entry) {
        String json = asJson(entry);
        if (json == null) {
            return;
        }
        appender.append(0, Record.of(String.valueOf(entry.getId()), json.getBytes(StandardCharsets.UTF_8)));
    }

    protected String asJson(LogEntry entry) {
        if (entry == null) {
            return null;
        }
        RenderingContext ctx = RenderingContext.CtxBuilder.get();
        try {
            return MarshallerHelper.objectToJson(entry, ctx);
        } catch (IOException e) {
            log.warn("Unable to translate entry into json, eventId:" + entry.getEventId() + ": " + e.getMessage(), e);
            return null;
        }
    }

    protected boolean registerSynchronization(Synchronization sync) {
        try {
            TransactionManager tm = TransactionHelper.lookupTransactionManager();
            if (tm != null) {
                if (tm.getTransaction() != null) {
                    tm.getTransaction().registerSynchronization(sync);
                    return true;
                }
                return false;
            } else {
                log.error("Unable to register synchronization : no TransactionManager");
                return false;
            }
        } catch (NamingException | IllegalStateException | SystemException | RollbackException e) {
            log.error("Unable to register synchronization", e);
            return false;
        }
    }

    protected LogManager getLogManager() {
        StreamService service = Framework.getService(StreamService.class);
        return service.getLogManager(getLogConfig());
    }

    protected String getLogConfig() {
        return Framework.getProperty(AUDIT_LOG_CONFIG_PROP, DEFAULT_LOG_CONFIG);
    }
}
