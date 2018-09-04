/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestEventConfService.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.security.Principal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.TestNXAuditEventsService.MyInit;
import org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.management.AuditEventMetricFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
@RepositoryConfig(init = MyInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.audit:test-audit-contrib.xml")
public class TestNXAuditEventsService {

    protected static MyInit repo;

    public static class MyInit extends DefaultRepositoryInit {
        {
            repo = this;
        }

        protected DocumentModel source;

        @Override
        public void populate(CoreSession session) {
            super.populate(session);
            DocumentModel rootDocument = session.getRootDocument();
            DocumentModel model = session.createDocumentModel(rootDocument.getPathAsString(), "youps", "File");
            model.setProperty("dublincore", "title", "huum");
            session.createDocument(model);
            source = session.getDocument(new PathRef("/youps"));
        }
    }

    @Inject
    Logs serviceUnderTest;

    protected MBeanServer mbeanServer;

    @Inject
    protected EventService eventService;

    @Inject
    CoreSession session;

    @Inject
    TransactionalFeature txFeature;

    @Before
    public void setUp() throws Exception {
        mbeanServer = Framework.getService(ServerLocator.class).lookupServer();
    }

    public void waitForAsyncCompletion() throws InterruptedException {
        txFeature.nextTransaction(Duration.ofSeconds(20));
    }

    public boolean extendedInfosComputedWithFullDocumentModel() {
        return false;
    }

    @Test
    public void testAuditContribution() throws Exception {
        NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime()
                                                                            .getComponent(NXAuditEventsService.NAME);
        assertNotNull(auditService);
        Set<AdapterDescriptor> registeredAdapters = auditService.getDocumentAdapters();
        assertEquals(1, registeredAdapters.size());

        AdapterDescriptor ad = registeredAdapters.iterator().next();
        assertEquals("myadapter", ad.getName());

    }

    @Test
    public void testLogDocumentMessageWithoutCategory() throws InterruptedException {
        EventContext ctx = new DocumentEventContext(session, session.getPrincipal(), repo.source);
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        waitForAsyncCompletion();

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(repo.source.getId());
        assertEquals(2, entries.size());

        // entries are not ordered => skip creation log
        for (LogEntry entry : entries) {
            if ("documentSecurityUpdated".equals(entry.getEventId())) {
                assertEquals("eventDocumentCategory", entry.getCategory());
                assertNull(entry.getComment());
                assertEquals("project", entry.getDocLifeCycle());
                assertEquals("/youps", entry.getDocPath());
                assertEquals("File", entry.getDocType());
                assertEquals("documentSecurityUpdated", entry.getEventId());
                assertEquals("Administrator", entry.getPrincipalName());
            } else {
                assertEquals("documentCreated", entry.getEventId());
            }
            assertEquals("test", entry.getRepositoryId());
        }
    }

    @Test
    public void testLogDocumentMessageWithCategory() throws InterruptedException {
        EventContext ctx = new DocumentEventContext(session, session.getPrincipal(), repo.source);
        ctx.setProperty("category", "myCategory");
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        waitForAsyncCompletion();

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(repo.source.getId(), repo.source.getRepositoryName());
        assertEquals(2, entries.size());

        // entries are not ordered => skip creation log
        for (LogEntry entry : entries) {
            if ("documentSecurityUpdated".equals(entry.getEventId())) {
                assertEquals("myCategory", entry.getCategory());
                assertNull(entry.getComment());
                assertEquals("project", entry.getDocLifeCycle());
                assertEquals("/youps", entry.getDocPath());
                assertEquals("File", entry.getDocType());
                assertEquals("documentSecurityUpdated", entry.getEventId());
                assertEquals("Administrator", entry.getPrincipalName());
            } else {
                assertEquals("documentCreated", entry.getEventId());
            }
            assertEquals("test", entry.getRepositoryId());
        }
    }

    @Test
    public void testLogMiscMessage() throws InterruptedException {

        DefaultAuditBackend backend = (DefaultAuditBackend) serviceUnderTest;

        List<String> eventIds = backend.getLoggedEventIds();
        int n = eventIds.size();

        EventContext ctx = new EventContextImpl(); // not:DocumentEventContext
        Event event = ctx.newEvent("documentDuplicated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        waitForAsyncCompletion();

        eventIds = backend.getLoggedEventIds();
        assertEquals(n + 1, eventIds.size());
    }

    @Test
    public void testsyncLogCreation() throws Exception {
        DocumentModel rootDocument = session.getRootDocument();
        long count = serviceUnderTest.syncLogCreationEntries(session.getRepositoryName(),
                rootDocument.getPathAsString(), true);
        assertEquals(14, count);

        String query = String.format("log.docUUID = '%s' and log.eventId = 'documentCreated'", rootDocument.getId());

        List<LogEntry> entries = serviceUnderTest.nativeQueryLogs(query, 1, 1);
        assertEquals(1, entries.size());

        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertNull(entry.getComment());
        assertEquals("/", entry.getDocPath());
        assertEquals("Root", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, entry.getPrincipalName());

    }

    @Test
    public void simplePincipalNameIsLoggedAsPrincipalName() throws Exception {
        // Given a simple principal
        Principal principal = new SimplePrincipal("testuser");

        // When i fire an event with it
        EventContext ctx = new UnboundEventContext(principal, new HashMap<String, Serializable>());
        eventService.fireEvent(ctx.newEvent("loginSuccess"));
        waitForAsyncCompletion();

        // Then then event is logged with the principal's name
        assertEquals(1, serviceUnderTest.getEventsCount("loginSuccess").intValue());
        LogEntry logEntry = serviceUnderTest.nativeQueryLogs("log.eventId ='loginSuccess'", 1, 1).get(0);
        assertEquals("testuser", logEntry.getPrincipalName());

    }

    @Test
    public void testExtendedInfos() throws InterruptedException {
        DocumentModel rootDocument = session.getRootDocument();
        DocumentModel model = session.createDocumentModel(rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        model = session.createDocument(model);
        long count = serviceUnderTest.syncLogCreationEntries(session.getRepositoryName(), model.getPathAsString(),
                true);
        assertEquals(1, count);

        String query = String.format("log.docUUID = '%s' and log.eventId = 'documentCreated'", model.getId());

        List<LogEntry> entries = serviceUnderTest.nativeQueryLogs(query, 1, 1);
        assertEquals(1, entries.size());

        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("test", entry.getRepositoryId());
        assertEquals("huum", entry.getExtendedInfos().get("title").getSerializableValue());
        assertEquals("/", entry.getExtendedInfos().get("parentPath").getSerializableValue());

        session.removeDocument(model.getRef());
        session.save();

        waitForAsyncCompletion();

        FilterMapEntry filterByDocRemoved = new FilterMapEntry();
        filterByDocRemoved.setColumnName(BuiltinLogEntryData.LOG_EVENT_ID);
        filterByDocRemoved.setOperator("=");
        filterByDocRemoved.setQueryParameterName(BuiltinLogEntryData.LOG_EVENT_ID);
        filterByDocRemoved.setObject("documentRemoved");

        Map<String, FilterMapEntry> filterMap = new HashMap<String, FilterMapEntry>();
        filterMap.put("eventId", filterByDocRemoved);
        entries = serviceUnderTest.getLogEntriesFor(model.getId(), filterMap, true);
        assertEquals(1, entries.size());
        Map<String, ExtendedInfo> infos = entries.get(0).getExtendedInfos();
        assertEquals("/", infos.get("parentPath").getSerializableValue());
        // For the original audit implementation using a post-commit event listeners,
        // we only have a DeletedDocumentModel so no title available.
        // For the Stream-based audit implementation, extended infos are computed early with
        // the full DocumentModel so we have actual values.
        ExtendedInfo titleInfo = infos.get("title");
        if (extendedInfosComputedWithFullDocumentModel()) {
            assertEquals("huum", titleInfo.getSerializableValue());
        } else {
            assertNull(titleInfo);
        }
    }

    protected Set<ObjectName> doQuery(String name) {
        String qualifiedName = ObjectNameFactory.getQualifiedName(name);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        return mbeanServer.queryNames(objectName, null);
    }

    public void TODOtestCount() throws Exception {
        DocumentModel rootDocument = session.getRootDocument();
        DocumentModel model = session.createDocumentModel(rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        session.createDocument(model);
        session.save();
        waitForAsyncCompletion();
        ObjectName objectName = AuditEventMetricFactory.getObjectName("documentCreated");
        Long count = (Long) mbeanServer.getAttribute(objectName, "count");
        assertEquals(new Long(1L), count);
    }

    @Test
    public void testGetLatestLogId() throws Exception {
        String repositoryId = "test";
        createLogEntry("documentModified");
        long id1 = serviceUnderTest.getLatestLogId(repositoryId, "documentModified");
        assertTrue("id: " + id1, id1 > 0);
        createLogEntry("documentCreated");
        long id2 = serviceUnderTest.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue("id2: " + id2, id2 > 0);
        assertTrue(id2 > id1);
        long id = serviceUnderTest.getLatestLogId(repositoryId, "documentModified");
        assertEquals(id1, id);
        id = serviceUnderTest.getLatestLogId(repositoryId, "unknownEvent");
        assertTrue("id: " + id, id == 0);
    }

    @Test
    public void testGetLogEntriesAfter() throws Exception {
        String repositoryId = "test";
        createLogEntry("something");
        createLogEntry("documentModified");
        long id1 = serviceUnderTest.getLatestLogId(repositoryId, "documentModified");

        createLogEntry("documentCreated");
        long id2 = serviceUnderTest.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue(id2 > id1);

        createLogEntry("documentCreated");
        long id3 = serviceUnderTest.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue(id3 > id2);

        createLogEntry("documentCreated");
        long id4 = serviceUnderTest.getLatestLogId(repositoryId, "documentModified", "documentCreated");
        assertTrue(id4 > id3);

        List<LogEntry> entries = serviceUnderTest.getLogEntriesAfter(id1, 5, repositoryId, "documentCreated", "documentModified");
        assertEquals(4, entries.size());
        assertEquals(id1, entries.get(0).getId());

        entries = serviceUnderTest.getLogEntriesAfter(id2, 5, repositoryId, "documentCreated", "documentModified");
        assertEquals(3, entries.size());
        assertEquals(id2, entries.get(0).getId());
    }

    protected void createLogEntry(String eventId) throws InterruptedException {
        EventContext ctx = new DocumentEventContext(session, session.getPrincipal(), repo.source);
        Event event = ctx.newEvent(eventId);
        event.setInline(false);
        event.setImmediate(true);
        eventService.fireEvent(event);
        waitForAsyncCompletion();
    }

    @Test
    public void testLogRetentionActiveChange() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);

        // set retention active
        session.setRetentionActive(doc.getRef(), true);

        // an event is logged
        waitForAsyncCompletion();
        long id = serviceUnderTest.getLatestLogId(session.getRepositoryName(), "retentionActiveChanged");
        assertNotEquals(0, id);
        LogEntry logEntry = serviceUnderTest.getLogEntryByID(id);
        assertEquals("retentionActiveChanged", logEntry.getEventId());
        assertEquals(doc.getId(), logEntry.getDocUUID());
        assertEquals(Boolean.TRUE, logEntry.getExtendedInfos().get("retentionActive").getSerializableValue());
        assertEquals("true", logEntry.getComment());

        // unset retention active
        session.setRetentionActive(doc.getRef(), false);

        // an event is logged
        waitForAsyncCompletion();
        id = serviceUnderTest.getLatestLogId(session.getRepositoryName(), "retentionActiveChanged");
        assertNotEquals(0, id);
        logEntry = serviceUnderTest.getLogEntryByID(id);
        assertEquals("retentionActiveChanged", logEntry.getEventId());
        assertEquals(doc.getId(), logEntry.getDocUUID());
        assertEquals(Boolean.FALSE, logEntry.getExtendedInfos().get("retentionActive").getSerializableValue());
        assertEquals("false", logEntry.getComment());
    }

}
