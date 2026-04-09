/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;
import static org.nuxeo.ecm.platform.audit.impl.LogEntrySequenceGenerator.USE_NUXEO_SEQUENCER_PROPERTY;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RandomBug;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, CoreFeature.class })
public class TestAuditBackendWithDocument {

    @Inject
    protected AuditBackend backend;

    @Inject
    protected CoreSession session;

    @Inject
    protected AuditFeature auditFeature;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void shouldLogInAudit() {
        // generate events
        DocumentModel doc = session.createDocumentModel("/", "a-file", "File");
        doc.setPropertyValue("dc:title", "A File");
        doc = session.createDocument(doc);

        transactionalFeature.nextTransaction();

        doc.setPropertyValue("dc:title", "A modified File");
        doc = session.saveDocument(doc);

        transactionalFeature.nextTransaction();

        // test audit trail
        List<LogEntry> trail = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, doc.getId()))
                                       .and(Predicates.eq(LOG_REPOSITORY_ID, session.getRepositoryName()))
                                       .defaultOrder());

        assertNotNull(trail);
        assertEquals(2, trail.size());

        LogEntry entry = trail.get(0);
        // the hibernate sequencer is not reset so the assertion will fail if the test is not the first one to run
        if (!auditFeature.isBackendSql() || Framework.isBooleanPropertyTrue(USE_NUXEO_SEQUENCER_PROPERTY)) {
            assertEquals(2L, entry.getId());
        }
        assertEquals("documentModified", entry.getEventId());
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("A modified File", entry.getExtendedValue("title"));

        entry = trail.get(1);
        if (!auditFeature.isBackendSql() || Framework.isBooleanPropertyTrue(USE_NUXEO_SEQUENCER_PROPERTY)) {
            assertEquals(1L, entry.getId());
        }
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("A File", entry.getExtendedValue("title"));

        LogEntry entryById = backend.getLogEntryByID(entry.getId());
        assertEquals(entry.getId(), entryById.getId());

        entryById = backend.getLogEntryByID(123L);
        assertNull(entryById);

        assertEquals(1L, backend.getEventsCount("documentModified").longValue());
    }

    @Test
    @RandomBug.Repeat(issue = "NXP-28711: randomly failing in dev mode")
    public void canLogMultipleLifecycleTransitionsInSameTx() {
        // generate events
        DocumentModel doc = session.createDocumentModel("/", "a-file", "File");
        doc = session.createDocument(doc);
        String initialLifeCycle = doc.getCurrentLifeCycleState();
        doc.followTransition("approve");
        String approvedLifeCycle = doc.getCurrentLifeCycleState();
        doc.followTransition("backToProject");
        String projectLifeCycle = doc.getCurrentLifeCycleState();
        transactionalFeature.nextTransaction();

        // test audit trail
        List<LogEntry> trail = backend.queryLogs(
                new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, doc.getId()))
                                       .and(Predicates.eq(LOG_REPOSITORY_ID, session.getRepositoryName()))
                                       .defaultOrder());

        assertNotNull(trail);
        assertEquals(3, trail.size());

        boolean seenDocCreated = false;
        boolean seenDocApproved = false;
        boolean seenDocBackToProject = false;

        for (LogEntry entry : trail) {
            String lifeCycle = entry.getDocLifeCycle();
            String id = entry.getEventId();
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)) {
                if (initialLifeCycle.equals(lifeCycle)) {
                    seenDocCreated = true;
                }
            } else if (LifeCycleConstants.TRANSITION_EVENT.equals(id)) {
                if (projectLifeCycle.equals(lifeCycle)) {
                    seenDocBackToProject = true;
                } else if (approvedLifeCycle.equals(lifeCycle)) {
                    seenDocApproved = true;
                }
            }
        }

        assertTrue(seenDocBackToProject);
        assertTrue(seenDocApproved);
        assertTrue(seenDocCreated);
    }

    @Test
    @RandomBug.Repeat(issue = "NXP-28711: randomly failing in dev mode")
    public void testLogDate() throws InterruptedException {
        // generate doc creation events
        DocumentModel doc = session.createDocumentModel("/", "a-file", "File");
        doc = session.createDocument(doc);

        // simulate a long running process in the same transaction: make the
        // delay big enough to make logDate not the same as eventDate even on
        // databases that have a 1s time resolution.
        Thread.sleep(1000);

        // commit the transaction and let the audit service log the events in the log
        transactionalFeature.nextTransaction();

        // test audit trail
        List<LogEntry> trail = backend.getLogEntriesFor(doc.getId(), doc.getRepositoryName());

        assertNotNull(trail);
        assertEquals(1, trail.size());

        Date eventDate = null;
        Date logDate = null;
        for (LogEntry entry : trail) {
            String id = entry.getEventId();
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)) {
                eventDate = entry.getEventDate();
                logDate = entry.getLogDate();
            }
        }
        assertNotNull(eventDate);
        assertNotNull(logDate);
        assertTrue(logDate.after(eventDate));
    }
}
