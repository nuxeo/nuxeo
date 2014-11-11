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
 * $Id: TestWorkflowService.java 6230 2006-11-14 09:42:31Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow;

import java.util.Set;

import org.nuxeo.ecm.platform.audit.NXAudit;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.PersistenceProvider;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the workflow service.
 * 
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestWorkflowEventRegistration extends NXRuntimeTestCase {

    private NXAuditEvents service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        NXAuditEventsService.persistenceProvider.setHibernateConfiguration(new TestAuditHibernateConfiguration());
        deployContrib("org.nuxeo.ecm.platform.workflow.tests",
                "nxaudit-event-service.xml");
        deployContrib("org.nuxeo.ecm.platform.workflow.tests",
                "nxaudit-events-workflow.xml");

        
        service = NXAudit.getNXAuditEventsService();
        assertNotNull(service);
    }

    public void testEventRegistrations() {
        Set<String> eventNames = service.getAuditableEventNames();
        assertTrue(eventNames.contains("workflowStarted"));
        assertTrue(eventNames.contains("workflowEnded"));
        assertTrue(eventNames.contains("workflowTaskAssigned"));
        assertTrue(eventNames.contains("workflowTaskUnassigned"));
        assertTrue(eventNames.contains("workflowTaskStarted"));
        assertTrue(eventNames.contains("workflowTaskEnded"));
    }

}
