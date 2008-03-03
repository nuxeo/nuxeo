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
 * $Id: AbstractTestAPI.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.ejb;

import java.util.Collection;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;

/**
 * Common API tests for local and remote tests.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractTestAPI extends AbstractInitializer {

    protected static int NB_DEPLOYED_DEFS;

    protected static String DEFINITION_1_ID;

    public void testGetDefinitions() throws WMWorkflowException {
        Collection<WMProcessDefinition> definitions = wapi.listProcessDefinitions();
        assertEquals(NB_DEPLOYED_DEFS, definitions.size());
    }

    public void testGetDefinitioById() throws WMWorkflowException {
        Collection<WMProcessDefinition> definitions = wapi.listProcessDefinitions();
        for (WMProcessDefinition definition : definitions) {
            WMProcessDefinition rdefinition = wapi.getProcessDefinitionById(definition
                    .getId());
            assertEquals(definition.getId(), rdefinition.getId());
            assertEquals(definition.getName(), rdefinition.getName());
            assertEquals(definition.getVersion(), rdefinition.getVersion());
        }
    }

    public void testStartStopWorkflow() throws WMWorkflowException {
        Collection<WMProcessDefinition> definitions = wapi.listProcessDefinitions();
        for (WMProcessDefinition definition : definitions) {
            WMActivityInstance path = wapi.startProcess(definition.getId(), null,
                    null);
            String workflowInstanceId = path.getProcessInstance().getId();
            assertNotNull(wapi.getProcessInstanceById(workflowInstanceId, null));

            WMProcessInstance instance = wapi.terminateProcessInstance(workflowInstanceId);
            assertNotNull(instance);
            assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE,
                    instance.getState());
        }
    }

}
