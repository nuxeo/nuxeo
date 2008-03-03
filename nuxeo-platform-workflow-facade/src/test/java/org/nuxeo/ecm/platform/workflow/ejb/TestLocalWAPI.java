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
 * $Id: TestLocalWAPI.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.ejb;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMTransitionDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.impl.WAPIImpl;

/**
 * Tests the local WAPI.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestLocalWAPI extends AbstractJbpmRuntimeTestCase {

    @Override
    protected void _initializeTest() throws Exception {
        wapi = new WAPIImpl();
    }

    @Override
    protected void _unInitializeTest() throws Exception {
        wapi = null;
    }

    @Override
    public void testGetDefinitions() throws WMWorkflowException {
        Collection<WMProcessDefinition> definitions = wapi.listProcessDefinitions();
        assertEquals(3, definitions.size());
    }

    public void testTaskAssignment() throws WMWorkflowException {

        String wdefID3 = "3";

        WMProcessDefinition definition = wapi.getProcessDefinitionById(wdefID3);
        assertNotNull(definition);

        WMParticipant principal = new WMParticipantImpl("anguenot");

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());

        WMActivityInstance path = wapi.startProcess(wdefID3, params, params);

        WMActivityDefinition node = path.getActivityDefinition();
        assertEquals("review", node.getName());
        WMTransitionDefinition[] transitions = node.getAvailableTransitions();
        assertEquals(1, transitions.length);
        assertEquals("validate", transitions[0].getName());

        //
        // First the task specified a a start task
        //

        // Test getAssignedTask by actor
        Collection<WMWorkItemInstance> taskInstances = wapi
                .getWorkItemsFor(principal,
                        WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, taskInstances.size());

        WMWorkItemInstance taskInstance = taskInstances.iterator().next();
        assertEquals(principal.getName(), taskInstance.getParticipant().getName());

        WMParticipant newPrincipal = new WMParticipantImpl("arnaud");

        wapi.assignWorkItem(taskInstance.getId(), newPrincipal);
        newTransaction();

        // Test getAssignedTask by actor
        taskInstances = wapi.getWorkItemsFor(principal,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(0, taskInstances.size());

        taskInstances = wapi.getWorkItemsFor(newPrincipal,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, taskInstances.size());

        taskInstance = taskInstances.iterator().next();
        assertEquals(newPrincipal.getName(), taskInstance.getParticipant()
                .getName());

        taskInstances = wapi.getWorkItemsFor(path.getProcessInstance()
                .getId(), WMWorkItemState.WORKFLOW_TASK_STATE_ALL, newPrincipal);
        assertEquals(1, taskInstances.size());
    }

    public void testTaskCreation() throws WMWorkflowException {
        String wdefID3 = "3";

        WMProcessDefinition definition = wapi.getProcessDefinitionById(wdefID3);
        assertNotNull(definition);

        WMParticipant principal = new WMParticipantImpl("anguenot");

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());

        WMActivityInstance path = wapi.startProcess(wdefID3, params, params);
        String pid = path.getProcessInstance().getId();

        Set<WMWorkItemDefinition> tdefs = wapi
                .getWorkItemDefinitionsFor(pid);
        assertEquals(1, tdefs.size());

        WMWorkItemInstance ti = wapi.createWorkItem(pid, tdefs.iterator()
                .next().getId(), null);
        assertNotNull(ti);
    }

    public void testTaskEnd() throws WMWorkflowException {
        String wdefID3 = "3";

        WMProcessDefinition definition = wapi.getProcessDefinitionById(wdefID3);
        assertNotNull(definition);

        WMParticipant principal = new WMParticipantImpl("anguenot");

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());

        WMActivityInstance path = wapi.startProcess(wdefID3, params, params);
        String pid = path.getProcessInstance().getId();

        // There is already a task created while entering the task activityDefinition.

        Collection<WMWorkItemInstance> tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        Set<WMWorkItemDefinition> tdefs = wapi
                .getWorkItemDefinitionsFor(pid);
        assertEquals(1, tdefs.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        WMWorkItemInstance ti = wapi.createWorkItem(pid, tdefs.iterator()
                .next().getId(), null);
        assertNotNull(ti);

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(2, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        wapi.startWorkItem(ti.getId());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        wapi.endWorkItem(ti.getId(), null);

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());
    }

    public void testTaskRemove() throws WMWorkflowException {
        String wdefID3 = "3";

        WMProcessDefinition definition = wapi.getProcessDefinitionById(wdefID3);
        assertNotNull(definition);

        WMParticipant principal = new WMParticipantImpl("anguenot");

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());

        WMActivityInstance path = wapi.startProcess(wdefID3, params, params);
        String pid = path.getProcessInstance().getId();

        // There is already a task created while entering the task activityDefinition.

        Collection<WMWorkItemInstance> tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        Set<WMWorkItemDefinition> tdefs = wapi
                .getWorkItemDefinitionsFor(pid);
        assertEquals(1, tdefs.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        WMWorkItemInstance ti = wapi.createWorkItem(pid, tdefs.iterator()
                .next().getId(), null);
        assertNotNull(ti);

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(2, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        wapi.startWorkItem(ti.getId());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        wapi.removeWorkItem(ti.getId());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = wapi.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());
    }

}
