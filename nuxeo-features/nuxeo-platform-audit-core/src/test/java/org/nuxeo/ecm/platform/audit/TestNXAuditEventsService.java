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

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Test the event conf service.
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestNXAuditEventsService extends RepositoryOSGITestCase {

    private NXAuditEvents serviceUnderTest;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployBundle("org.nuxeo.ecm.platform.audit");

        NXAuditEventsService.persistenceProvider.setHibernateConfiguration(new TestHibernateConfiguration());

        serviceUnderTest = Framework.getService(NXAuditEvents.class);

        assertNotNull(serviceUnderTest);

        openRepository();

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
    
    public void testLogMessage() throws DocumentException, ClientException {
        DocumentModel source = doCreateDocument();
        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(source.getId());
        assertTrue(entries.size() == 1);
        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/youps", entry.getDocPath());
        assertEquals("File", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("Administrator", entry.getPrincipalName());
    }

    public void testsyncLogCreation() throws AuditException, ClientException {
        doCreateDocument();
        DocumentModel rootDocument = coreSession.getRootDocument();
        long count = serviceUnderTest.syncLogCreationEntries(
                getRepository().getName(), rootDocument.getPathAsString(), true);
        assertEquals(count, 2);
        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(rootDocument.getId());
        assertEquals(entries.size(), 1);
        LogEntry entry = entries.get(0);
        assertEquals("eventDocumentCategory", entry.getCategory());
        assertEquals(null, entry.getComment());
        assertEquals("project", entry.getDocLifeCycle());
        assertEquals("/", entry.getDocPath());
        assertEquals("Root", entry.getDocType());
        assertEquals("documentCreated", entry.getEventId());
        assertEquals("system", entry.getPrincipalName());
    }
}
