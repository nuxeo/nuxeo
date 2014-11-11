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
 * $Id: TestWorkflowService.java 4669 2006-10-23 17:31:08Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import org.nuxeo.ecm.platform.workflow.api.WorkflowEngine;

/**
 * Test the workflow service extension point.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestJbpmEngineRegistration extends AbstractJbpmRuntimeTestCase {

    public void testJbpmWokflowEngineRegistration() {
        WorkflowEngine engine = workflowService
                .getWorkflowEngineByName(ENGINE_NAME);
        assertNotNull(engine);
    }

    public void testDefaultEngine() {
        assertEquals(ENGINE_NAME, workflowService.getDefaultEngineName());
    }

    public void testWorkflowDefinitionsRegistration() {
        WorkflowEngine engine = workflowService.getWorkflowEngineByName("jbpm");
        assertEquals(2, engine.getProcessDefinitions().size());
    }

}
