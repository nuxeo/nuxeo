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
 * $Id: TestLogEntryFactory.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.facade;

import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryFactory;
import org.nuxeo.ecm.platform.audit.ejb.LogEntryFactoryImpl;
import org.nuxeo.ecm.platform.audit.ejb.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestLogEntryFactory extends NXRuntimeTestCase {

    NXAuditEventsService aes;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.audit.ejb.tests",
                "nxaudit-service-definitions.xml");
        deployContrib("org.nuxeo.ecm.platform.audit.ejb.tests",
                "nxaudit-service-contrib.xml");

        aes = (NXAuditEventsService) runtime.getComponent(NXAuditEventsService.NAME);
    }

    public void testRegistration() {
        assertNotNull(aes.getLogEntryFactoryKlass());
        assertEquals(LogEntryFactoryImpl.class, aes.getLogEntryFactoryKlass());
    }

    public void testFactory() throws Exception {
        LogEntryFactory factory = aes.getLogEntryFactory();
        assertNotNull(factory);

        Class<LogEntry> klass = factory.getLogEntryClass();
        assertNotNull(klass);
        assertEquals(LogEntryImpl.class, klass);

        DocumentMessage doc = new DocumentMessageImpl();
        LogEntry entry = factory.computeLogEntryFrom(doc);
        assertNotNull(entry);
        assertTrue(entry instanceof LogEntryImpl);
    }

}
