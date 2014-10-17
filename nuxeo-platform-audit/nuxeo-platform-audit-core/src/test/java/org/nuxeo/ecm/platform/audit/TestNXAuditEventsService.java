/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestEventConfService.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.TestNXAuditEventsService.MyInit;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.management.AuditEventMetricFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
@RepositoryConfig(init = MyInit.class, cleanup = Granularity.CLASS)
@LocalDeploy("org.nuxeo.ecm.platform.audit:test-audit-contrib.xml")
public class TestNXAuditEventsService {

    protected static MyInit repo;

    public static class MyInit extends DefaultRepositoryInit {
        {
            repo = this;
        }

        protected DocumentModel source;

        @Override
        public void populate(CoreSession session) throws ClientException {
            super.populate(session);
            DocumentModel rootDocument = session.getRootDocument();
            DocumentModel model = session.createDocumentModel(
                    rootDocument.getPathAsString(), "youps", "File");
            model.setProperty("dublincore", "title", "huum");
            session.createDocument(model);
            source = session.getDocument(new PathRef("/youps"));
        }
    }

    @Inject
    Logs serviceUnderTest;

    protected MBeanServer mbeanServer;

    @Inject
    CoreSession session;

    @Before
    public void setUp() throws Exception {
        mbeanServer = Framework.getLocalService(ServerLocator.class)
            .lookupServer();
    }

    @Test
    public void testAuditContribution() throws Exception {
        NXAuditEventsService auditService = (NXAuditEventsService) Framework
            .getRuntime().getComponent(NXAuditEventsService.NAME);
        assertNotNull(auditService);
        Set<AdapterDescriptor> registeredAdapters = auditService
            .getDocumentAdapters();
        assertEquals(1, registeredAdapters.size());

        AdapterDescriptor ad = registeredAdapters.iterator().next();
        assertEquals("myadapter", ad.getName());

    }

    @Test
    public void testLogDocumentMessageWithoutCategory() throws ClientException {
        EventContext ctx = new DocumentEventContext(session,
                session.getPrincipal(), repo.source);
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        Framework.getLocalService(EventService.class).fireEvent(event);
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(repo.source
            .getId());
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
    public void testLogDocumentMessageWithCategory() throws ClientException {
        EventContext ctx = new DocumentEventContext(session,
                session.getPrincipal(), repo.source);
        ctx.setProperty("category", "myCategory");
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        Framework.getLocalService(EventService.class).fireEvent(event);
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(repo.source
            .getId());
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
    public void testLogMiscMessage() throws ClientException {

        DefaultAuditBackend backend = (DefaultAuditBackend) serviceUnderTest;

        List<String> eventIds = backend.getLoggedEventIds();
        int n = eventIds.size();

        EventContext ctx = new EventContextImpl(); // not:DocumentEventContext
        Event event = ctx.newEvent("documentModified"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        Framework.getLocalService(EventService.class).fireEvent(event);
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        eventIds = backend.getLoggedEventIds();
        assertEquals(n + 1, eventIds.size());
    }

    @Test
    public void testsyncLogCreation() throws Exception {
        DocumentModel rootDocument = session.getRootDocument();
        long count = serviceUnderTest.syncLogCreationEntries(
                session.getRepositoryName(), rootDocument.getPathAsString(),
                true);
        assertEquals(14, count);

        String query = String.format(
                "log.docUUID = '%s' and log.eventId = 'documentCreated'",
                rootDocument.getId());

        List<LogEntry> entries = serviceUnderTest.nativeQueryLogs(query, 1, 1);
        assertEquals(1, entries.size());

        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertNull(entry.getComment());
        assertEquals("/", entry.getDocPath());
        assertEquals("Root", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals(SecurityConstants.SYSTEM_USERNAME,
                entry.getPrincipalName());
        assertEquals("test", entry.getRepositoryId());
    }

    protected Set<ObjectName> doQuery(String name) {
        String qualifiedName = ObjectNameFactory.getQualifiedName(name);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        return mbeanServer.queryNames(objectName, null);
    }

    public void TODOtestCount() throws Exception {
        DocumentModel rootDocument = session.getRootDocument();
        DocumentModel model = session.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        session.createDocument(model);
        session.save();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        ObjectName objectName = AuditEventMetricFactory
            .getObjectName("documentCreated");
        Long count = (Long) mbeanServer.getAttribute(objectName, "count");
        assertEquals(new Long(1L), count);
    }

}
