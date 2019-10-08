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
package org.nuxeo.audit.storage.stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.audit.TestNXAuditEventsService.MyInit.YOUPS_PATH;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RestoreAuditLogsTest {

    protected static final int FIRE_EVENT_COUNT = 10;

    protected static final int DEFAULT_BATCH_SIZE = 30;

    protected static final int DEFAULT_KEEP_ALIVE = 10000;

    private static final int ASYNC_STREAM_WAIT_IN_SECONDS = 5;

    protected static final String EVENT_SECURITY_UPDATED = "documentSecurityUpdated";

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String AUDIT_LOG_CONFIG_PROP = "nuxeo.stream.audit.log.config";

    public static final String DEFAULT_LOG_CONFIG = "audit";

    protected void pause() throws InterruptedException {
        TimeUnit.SECONDS.sleep(ASYNC_STREAM_WAIT_IN_SECONDS);
    }

    protected void cleanAudit(AuditBackend auditBackend) {
        // clean logs for audit backend
        ((DefaultAuditBackend) auditBackend).getOrCreatePersistenceProvider().run(true, entityManager -> {
            entityManager.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
            entityManager.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
            entityManager.createNativeQuery("delete from nxp_logs").executeUpdate();
        });
    }

	protected Event createAndFireEvent(CoreSession session, EventService eventService, DocumentModel source,
            String name) throws InterruptedException {
        EventContext ctx = new DocumentEventContext(session, session.getPrincipal(), source);
        Event event = ctx.newEvent(name);
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);

        return event;
    }

	protected void nextTransaction(EventService eventService) throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

	protected void generateEventsOn(CoreSession session, EventService eventService, DocumentModel source)
            throws InterruptedException {
        for (int i = 0; i < FIRE_EVENT_COUNT; i++) {
            createAndFireEvent(session, eventService, source, EVENT_SECURITY_UPDATED);
        }

        session.save();
        nextTransaction(eventService);
    }

	protected LogEntry getLogEntryFromJson(String jsonEntry) {
        try {
            return OBJECT_MAPPER.readValue(jsonEntry, LogEntryImpl.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

	protected HashSet<LogEntry> getLogEntries(AuditStorage storage, String docPath, int batchSize,
            int keepAlive) {
        HashSet<LogEntry> logEntrySet = new HashSet<LogEntry>();

        QueryBuilder queryBuilder = new AuditQueryBuilder();
        ScrollResult<String> scrollResult = storage.scroll(queryBuilder, batchSize, keepAlive);
        assertNotNull(scrollResult.getScrollId());

        while (scrollResult.hasResults()) {
            List<String> results = scrollResult.getResults();
            for (String result : results) {
                LogEntry logEntry = getLogEntryFromJson(result);
                if (YOUPS_PATH.equals(logEntry.getDocPath())) {
                    logEntrySet.add(logEntry);
                }
            }

            scrollResult = storage.scroll(scrollResult.getScrollId());
        }

        return logEntrySet;
    }

    protected int getLogEntryCount(AuditStorage storage, int batchSize, int stayAlive) {
        QueryBuilder queryBuilder = new AuditQueryBuilder();
        ScrollResult<String> scrollResult = storage.scroll(queryBuilder, batchSize, stayAlive);
        assertNotNull(scrollResult.getScrollId());

        int count = 0;
        while (scrollResult.hasResults()) {
            count += scrollResult.getResults().size();
            scrollResult = storage.scroll(scrollResult.getScrollId());
        }

        return count;
    }

	protected void createAndPersistLogEntriesToBackend(CoreSession session, AuditLogger auditLogger, int count)
			throws InterruptedException {
		DocumentModel source = session.getDocument(new PathRef(YOUPS_PATH));
		String principal = session.getPrincipal().getActingUser();
		List<LogEntry> logEntries = new ArrayList<LogEntry>();

		for (int i = 0; i < count; i++) {
			LogEntry entry = auditLogger.newLogEntry();
			entry.setEventId(Constants.W_AUDIT_EVENT);
			entry.setEventDate(new Date());
			entry.setCategory("Automation");
			entry.setDocUUID(source.getId());
			entry.setDocPath(source.getPathAsString());
			entry.setComment("");
			entry.setPrincipalName(principal);
			entry.setDocType(source.getType());
			entry.setRepositoryId(source.getRepositoryName());
			entry.setDocLifeCycle(source.getCurrentLifeCycleState());

			logEntries.add(entry);
		}

		auditLogger.addLogEntries(logEntries);
		session.save();

		TransactionHelper.commitOrRollbackTransaction();
		TransactionHelper.startTransaction();
		assertTrue(auditLogger.await(10, TimeUnit.SECONDS));
	}
}
