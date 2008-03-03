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
 * $Id: TestWorkflowService.java 28498 2008-01-05 11:46:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow;

import java.util.Collection;

import org.nuxeo.ecm.platform.workflow.api.WorkflowEngine;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.service.WorkflowService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the workflow service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestWorkflowService extends NXRuntimeTestCase {

    private static final String ENGINE_NAME = "fake";

    private WorkflowService workflowService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("WorkflowService.xml");
        deploy("WorkflowServiceTestExtensions.xml");

        workflowService = NXWorkflow.getWorkflowService();
        assertNotNull(workflowService);
    }

    public void testWokflowEngineRegistration() {
        WorkflowEngine engine = workflowService.getWorkflowEngineByName(ENGINE_NAME);
        assertNotNull(engine);
    }
    public void testDefaultEngine() {
        assertEquals(ENGINE_NAME, workflowService.getDefaultEngineName());
    }

    public void testWorkflowDefinitionRegistration() {
        WorkflowEngine engine = workflowService.getWorkflowEngineByName(ENGINE_NAME);

        assertTrue(engine.isDefinitionDeployed("fake"));

        Collection<WMProcessDefinition> defs = engine.getProcessDefinitions();
        assertEquals(1, defs.size());

        WMProcessDefinition def = engine.getProcessDefinitionById("fake");
        assertEquals("fake", def.getId());
    }

}
