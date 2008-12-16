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
import java.util.Properties;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.HibernateConfiguration;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.PersistenceProvider;
import org.nuxeo.ecm.platform.events.DocumentMessageFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.runtime.api.Framework;

/**
 * Test the event conf service.
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestNXAuditEventsService extends RepositoryOSGITestCase {

    private NXAuditEventsService serviceUnderTest;

    public static class TestHibernateConfiguration implements
            HibernateConfiguration {

        public Properties getProperties() {
            Properties properties = new Properties();
            properties.put("hibernate.connection.url",
                    "jdbc:derby://localhost:1527/sample;create=true");
            properties.put("hibernate.connection.driver_class",
                    "org.apache.derby.jdbc.ClientDriver");
            properties.put("hibernate.connection.auto_commit", "true");
            properties.put("hibernate.connection.pool_size", "1");
            properties.put("hibernate.dialect",
                    "org.hibernate.dialect.DerbyDialect");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.show_sql", "true");
            properties.put("hibernate.format_sql", "true");

            return properties;
        }

    }

    protected DocumentModel rootDocument;
    protected DocumentModel source;
    
    protected DocumentMessage message;

    protected CoreEvent event;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        PersistenceProvider.hibernateConfigurationClass = TestHibernateConfiguration.class;

        deployBundle("org.nuxeo.ecm.platform.usermanager");
       
        deployContrib("org.nuxeo.ecm.platform.audit.tests",
                "nxaudit-test-definitions.xml");

        serviceUnderTest = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        assertNotNull(serviceUnderTest);

        openRepository();
        CoreSession session = getCoreSession();
        rootDocument = getCoreSession().getRootDocument();
        
        DocumentModel model = session.createDocumentModel(
                rootDocument.getPathAsString(), "toto", "File");
        source = session.createDocument(model);
        session.save();
        event = new CoreEventImpl("documentCreated", source, null, session.getPrincipal(),
                null, null);
        message = DocumentMessageFactory.createDocumentMessage(source,
                event);
    }


    public void testLogMessage() throws AuditException, DocumentException {
        serviceUnderTest.logMessage(getCoreSession(), message);
        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(source.getId());
        assertTrue(entries.size() == 1);
    }
    
    public void testsyncLogCreation() throws AuditException, ClientException {
        serviceUnderTest.syncLogCreationEntries(getRepository().getName(),rootDocument.getPathAsString(), true);
        List<LogEntry> entries = serviceUnderTest.getLogEntriesFor(rootDocument.getId());
        assertTrue(entries.size() >= 1);
    }
}
