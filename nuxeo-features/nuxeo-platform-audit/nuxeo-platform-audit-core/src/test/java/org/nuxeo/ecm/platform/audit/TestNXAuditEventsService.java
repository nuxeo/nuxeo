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

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.management.AuditEventMetricFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestNXAuditEventsService extends RepositoryOSGITestCase {

    private NXAuditEventsService serviceUnderTest;

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.audit");
        deployBundle("org.nuxeo.ecm.platform.audit.tests");
        deployBundle("org.nuxeo.runtime.management");

        deployTestContrib("org.nuxeo.ecm.platform.audit.tests", "nxaudit-tests.xml");
        deployTestContrib("org.nuxeo.ecm.platform.audit.tests", "test-audit-contrib.xml");
        serviceUnderTest = (NXAuditEventsService) Framework.getLocalService(NXAuditEvents.class);
        assertNotNull(serviceUnderTest);
        openRepository();
        fireFrameworkStarted();
    }

    @Override
    public void tearDown() throws Exception  {
        waitForEventsDispatched();
        super.tearDown();
    }

    protected DocumentModel doCreateDocument() throws ClientException {
        DocumentModel rootDocument = coreSession.getRootDocument();
        DocumentModel model = coreSession.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        DocumentModel source = coreSession.createDocument(model);
        coreSession.save();
        waitForEventsDispatched();
        return source;
    }

    public void testLogMessage() throws ClientException {
        DocumentModel source = doCreateDocument();
        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(source.getId());
        assertEquals(1, entries.size());

        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/youps", entry.getDocPath());
        assertEquals("File", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("Administrator", entry.getPrincipalName());
    }

    public void testLogDocumentMessageWithoutCategory() throws ClientException {
        DocumentModel source = doCreateDocument();
        EventContext ctx = new DocumentEventContext(coreSession,
                coreSession.getPrincipal(), source);
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        Framework.getLocalService(EventService.class).fireEvent(event);
        waitForEventsDispatched();

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(source.getId());
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
        }
    }

    public void testLogDocumentMessageWithCategory() throws ClientException {
        DocumentModel source = doCreateDocument();
        EventContext ctx = new DocumentEventContext(coreSession,
                coreSession.getPrincipal(), source);
        ctx.setProperty("category", "myCategory");
        Event event = ctx.newEvent("documentSecurityUpdated"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        Framework.getLocalService(EventService.class).fireEvent(event);
        waitForEventsDispatched();

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(source.getId());
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
        }
    }

    public void testLogMiscMessage() throws ClientException {
        List<String> eventIds = serviceUnderTest.getLoggedEventIds();
        int n = eventIds.size();

        EventContext ctx = new EventContextImpl(); // not:DocumentEventContext
        Event event = ctx.newEvent("documentModified"); // auditable
        event.setInline(false);
        event.setImmediate(true);
        Framework.getLocalService(EventService.class).fireEvent(event);
        waitForEventsDispatched();

        eventIds = serviceUnderTest.getLoggedEventIds();
        assertEquals(n + 1, eventIds.size());
    }

    public void testsyncLogCreation() throws AuditException, ClientException {
        doCreateDocument();
        DocumentModel rootDocument = coreSession.getRootDocument();
        long count = serviceUnderTest.syncLogCreationEntries(
                coreSession.getRepositoryName(),
                rootDocument.getPathAsString(), true);
        assertEquals(2, count);

        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(rootDocument.getId());
        assertEquals(1, entries.size());

        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertNull(entry.getComment());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/", entry.getDocPath());
        assertEquals("Root", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals(SecurityConstants.SYSTEM_USERNAME, entry.getPrincipalName());
    }

    protected Set<ObjectName> doQuery(String name) {
        String qualifiedName = ObjectNameFactory.getQualifiedName(name);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        return mbeanServer.queryNames(objectName, null);
    }

    public void TODOtestCount() throws Exception {
        CoreSession session = getCoreSession();
        DocumentModel rootDocument = getCoreSession().getRootDocument();
        DocumentModel model = session.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        session.createDocument(model);
        session.save();
        waitForEventsDispatched();
        ObjectName objectName = AuditEventMetricFactory.getObjectName("documentCreated");
        Long count = (Long) mbeanServer.getAttribute(objectName, "count");
        assertEquals(new Long(1L), count);
    }

}
