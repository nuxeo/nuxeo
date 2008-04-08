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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.utils.SerializableHelper;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMFilter;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinitionState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstanceIterator;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMTransitionDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMFilterImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyConstants;

/**
 * Test the jBPM engine itself.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestJbpmEngine extends AbstractJbmTestCase {

    private static final String RPATH_PD1 = "samples/processdefinition.xml";

    private static final String RPATH_PD2 = "samples/processdefinition2.xml";

    private static final String RPATH_PD3 = "samples/tasksprocess/processdefinition.xml";

    private JbpmWorkflowEngine engine;

    private Map<String, WMProcessDefinitionState> deployments;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployments = new HashMap<String, WMProcessDefinitionState>();
        engine = new JbpmWorkflowEngine();
    }

    @Override
    public void tearDown() throws Exception {
        engine = null;
        deployments = null;
        super.tearDown();
    }

    private void deployOne(String rpath) throws WMWorkflowException {
        URL definitionURL = Thread.currentThread().getContextClassLoader().getResource(
                rpath);
        WMProcessDefinitionState depl = engine.deployDefinition(definitionURL,
                "text/xml");
        deployments.put(rpath, depl);
    }

    private void undeployOne(String rpath) throws WMWorkflowException {
        String wdefId = deployments.get(rpath).getDefinition().getId();
        engine.undeployDefinition(wdefId);
        deployments.remove(rpath);
    }

    private WMProcessDefinition getDefinitionFor(String rpath) {
        WMProcessDefinitionState depl = deployments.get(rpath);
        return depl.getDefinition();
    }

    public void testDeployDefinition() throws WMWorkflowException {

        newTransaction();
        deployOne(RPATH_PD1);
        assertNotNull(getDefinitionFor(RPATH_PD1));

        newTransaction();
        undeployOne(RPATH_PD1);

    }

    public void testUndeploy() throws WMWorkflowException {
        newTransaction();

        deployOne(RPATH_PD1);
        String identifier = getDefinitionFor(RPATH_PD1).getId();
        assertTrue(engine.isDefinitionDeployed(identifier));

        newTransaction();

        undeployOne(RPATH_PD1);
        assertFalse(engine.isDefinitionDeployed(identifier));
    }

    public void testDefinitions() throws WMWorkflowException {
        newTransaction();

        deployOne(RPATH_PD1);

        assertEquals(1, engine.getProcessDefinitions().size());

        newTransaction();
        undeployOne(RPATH_PD1);
    }

    public void testDefinitionById() throws WMWorkflowException {
        newTransaction();
        deployOne(RPATH_PD1);
        String identifier = getDefinitionFor(RPATH_PD1).getId();
        assertNotNull(engine.getProcessDefinitionById(identifier));

        newTransaction();
        undeployOne(RPATH_PD1);
    }

    public void testDeploySameDefinition() throws WMWorkflowException {
        newTransaction();
        deployOne(RPATH_PD1);
        assertEquals(1, engine.getProcessDefinitions().size());

        newTransaction();
        deployOne(RPATH_PD1);
        assertEquals(1, engine.getProcessDefinitions().size());
    }

    public void testDeploySeveral() throws WMWorkflowException {
        newTransaction();
        deployOne(RPATH_PD1);
        assertEquals(1, engine.getProcessDefinitions().size());

        newTransaction();
        deployOne(RPATH_PD2);
        assertEquals(2, engine.getProcessDefinitions().size());
    }

    public void testWorkflowInstances() throws WMWorkflowException {
        // 1. Deploy the definition
        newTransaction();
        deployOne(RPATH_PD1);
        String wdefID1 = getDefinitionFor(RPATH_PD1).getId();

        // Start a new workflow instance and check the results.
        newTransaction();
        WMActivityInstance wpath = engine.startProcess(wdefID1, null, null);

        assertNotNull(wpath);
        assertNotNull(wpath.getActivityDefinition());
        assertNotNull(wpath.getActivityDefinition());
        assertNotNull(wpath.getProcessInstance());

        // Ask the engine for all corresponding workflow instances
        newTransaction();
        Collection<WMProcessInstance> winstances = engine.getProcessInstancesFor(wdefID1);
        assertEquals(1, winstances.size());

        // Test the status of the workflow instances.
        for (WMProcessInstance wi : winstances) {
            assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE,
                    wi.getState());
        }

        // Ask the engine for all active workflow instances
        newTransaction();
        Collection<WMProcessInstance> wainstances = engine.getActiveProcessInstancesFor(wdefID1);
        assertEquals(1, wainstances.size());

        // Test the status of the workflow instances.
        for (WMProcessInstance wi : wainstances) {
            assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE,
                    wi.getState());
        }

        // Start another instance
        newTransaction();
        WMActivityInstance wpath2 = engine.startProcess(wdefID1, null, null);
        assertNotNull(wpath2);

        // Ask the engine for all corresponding workflow instances
        newTransaction();
        winstances = engine.getProcessInstancesFor(wdefID1);
        assertEquals(2, winstances.size());

        // Test the status of the workflow instances.
        for (WMProcessInstance wi : winstances) {
            assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE,
                    wi.getState());
        }

        // Ask the engine for all active workflow instances
        newTransaction();
        wainstances = engine.getActiveProcessInstancesFor(wdefID1);
        assertEquals(2, wainstances.size());

        // Test the status of the workflow instances.
        for (WMProcessInstance wi : wainstances) {
            assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE,
                    wi.getState());
        }

        // Now delete the first process instance.
        newTransaction();
        WMProcessInstance endedInstance = engine.terminateProcess(wpath.getProcessInstance().getId());
        assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE,
                endedInstance.getState());

        // Ask the engine for all corresponding workflow instances
        newTransaction();
        winstances = engine.getProcessInstancesFor(wdefID1);
        assertEquals(1, winstances.size());

        // Ask the engine for all corresponding active workflow instances
        newTransaction();
        winstances = engine.getActiveProcessInstancesFor(wdefID1);
        assertEquals(1, winstances.size());

        // Now delete the second process instance.
        newTransaction();
        endedInstance = engine.terminateProcess(wpath2.getProcessInstance().getId());
        assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE,
                endedInstance.getState());

        // Ask the engine for all corresponding workflow instances
        newTransaction();
        winstances = engine.getProcessInstancesFor(wdefID1);
        assertEquals(0, winstances.size());

        // Ask the engine for all corresponding active workflow instances
        newTransaction();
        winstances = engine.getActiveProcessInstancesFor(wdefID1);
        assertEquals(0, winstances.size());

        // undeploy
        newTransaction();
        undeployOne(RPATH_PD1);
    }

    public void testTransitions() throws WMWorkflowException {
        newTransaction();
        deployOne(RPATH_PD1);
        String wdefID1 = getDefinitionFor(RPATH_PD1).getId();

        // Start a new workflow instance and check the results.
        newTransaction();
        WMActivityInstance path = engine.startProcess(wdefID1, null, null);

        WMActivityDefinition node = path.getActivityDefinition();
        WMTransitionDefinition[] transitions = node.getAvailableTransitions();
        assertEquals(1, transitions.length);
        assertEquals("to_end", transitions[0].getName());

        path = engine.followTransition(path, "to_end", null);
        node = path.getActivityDefinition();
        transitions = node.getAvailableTransitions();
        assertEquals(0, transitions.length);

        engine.terminateProcess(path.getProcessInstance().getId());
    }

    public void testWorkflowPaths() throws WMWorkflowException {
        deployOne(RPATH_PD1);
        String wdefID1 = getDefinitionFor(RPATH_PD1).getId();

        // Start a new workflow instance and check the results.
        newTransaction();
        WMActivityInstance path = engine.startProcess(wdefID1, null, null);

        Collection<WMActivityInstance> paths = engine.getActivityInstancesFor(path.getProcessInstance().getId());
        assertEquals(1, paths.size());

        engine.terminateProcess(path.getProcessInstance().getId());
        undeployOne(RPATH_PD1);
    }

    public void testProcessVariables() throws WMWorkflowException {
        deployOne(RPATH_PD1);
        String wdefID1 = getDefinitionFor(RPATH_PD1).getId();

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put("author", "janguenot");

        WMActivityInstance path = engine.startProcess(wdefID1, props, null);

        Map<String, Serializable> fetchedProps = engine.listActivityInstanceAttributes(path);
        assertEquals(1, fetchedProps.size());
        assertEquals(props.get("author"), fetchedProps.get("author"));

        undeployOne(RPATH_PD1);
    }

    public void testActions() throws WMWorkflowException {
        deployOne(RPATH_PD2);
        String wdefID2 = getDefinitionFor(RPATH_PD2).getId();

        WMActivityInstance path = engine.startProcess(wdefID2, null, null);
        engine.followTransition(path, "to_end", null);

        assertTrue(FakeJbpmWorkflowActionHandler.isExecuted);

        undeployOne(RPATH_PD2);
    }

    // Ensure if no definition are deployed we don't get errors.
    public void testNoDefinitions() throws WMWorkflowException {
        Collection<WMProcessDefinition> definitions = engine.getProcessDefinitions();
        assertEquals(0, definitions.size());

        assertNull(engine.getProcessDefinitionById(String.valueOf(0)));
    }

    public void testTasksAPI() throws WMWorkflowException {
        WMParticipant principal = new WMParticipantImpl("anguenot");

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());

        WMActivityInstance path = engine.startProcess(wdefID3, params, params);

        WMActivityDefinition node = path.getActivityDefinition();
        assertEquals("review", node.getName());
        WMTransitionDefinition[] transitions = node.getAvailableTransitions();
        assertEquals(1, transitions.length);
        assertEquals("validate", transitions[0].getName());

        //
        // First the task specified a a start task
        //

        // Test getAssignedTask by actor
        Collection<WMWorkItemInstance> taskInstances = engine.getWorkItemsFor(
                principal, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, taskInstances.size());

        WMWorkItemInstance taskInstance = taskInstances.iterator().next();
        assertEquals(principal.getName(),
                taskInstance.getParticipant().getName());
        assertEquals("review", taskInstance.getName());

        // Test getAssignedTask by actor and workflow path
        taskInstances = engine.getWorkItemsFor(path,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, principal);
        assertEquals(1, taskInstances.size());
        taskInstance = taskInstances.iterator().next();
        assertEquals(principal.getName(),
                taskInstance.getParticipant().getName());
        assertEquals("review", taskInstance.getName());

        // Test getAssignedTask by actor and workflow instance id
        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, principal);
        assertEquals(1, taskInstances.size());
        taskInstance = taskInstances.iterator().next();
        assertEquals(principal.getName(),
                taskInstance.getParticipant().getName());
        assertEquals("review", taskInstance.getName());

        // engine.followTransition(path, "validate");

        // Try to assign
        WMParticipant anotherPrincipal = new WMParticipantImpl("another");
        WMParticipant stillAnotherPrincipal = new WMParticipantImpl(
                "stillAnother");

        engine.assignWorkItem(taskInstance, anotherPrincipal);

        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, anotherPrincipal);
        assertEquals(1, taskInstances.size());

        engine.assignWorkItem(taskInstance, stillAnotherPrincipal);

        // Not an assigned task for anotherPrincipal
        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, stillAnotherPrincipal);
        assertEquals(1, taskInstances.size());

        // Remove 2
        engine.unAssignWorkItem(taskInstance, stillAnotherPrincipal);

        // Remove 1
        engine.unAssignWorkItem(taskInstance, anotherPrincipal);

        undeployOne(RPATH_PD3);
    }

    public void testTaskAssignment() throws WMWorkflowException {
        WMParticipant principal = new WMParticipantImpl("anguenot");

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());
        params.put(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION, "approved");
        params.put(
                WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE,
                "approve");

        WMActivityInstance path = engine.startProcess(wdefID3, params, params);

        WMActivityDefinition node = path.getActivityDefinition();
        assertEquals("review", node.getName());
        WMTransitionDefinition[] transitions = node.getAvailableTransitions();
        assertEquals(1, transitions.length);
        assertEquals("validate", transitions[0].getName());

        //
        // First the task specified a a start task
        //

        // Test getAssignedTask by actor
        Collection<WMWorkItemInstance> taskInstances = engine.getWorkItemsFor(
                principal, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, taskInstances.size());

        WMWorkItemInstance taskInstance = taskInstances.iterator().next();
        assertEquals(principal.getName(),
                taskInstance.getParticipant().getName());

        WMParticipant newPrincipal = new WMParticipantImpl("arnaud");

        engine.assignWorkItem(taskInstance, newPrincipal);

        // Test getAssignedTask by actor
        taskInstances = engine.getWorkItemsFor(principal,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(0, taskInstances.size());

        taskInstances = engine.getWorkItemsFor(newPrincipal,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, taskInstances.size());

        taskInstance = taskInstances.iterator().next();
        assertEquals(newPrincipal.getName(),
                taskInstance.getParticipant().getName());

        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, newPrincipal);
        assertEquals(1, taskInstances.size());

        // Check task properties.
        taskInstance = taskInstances.iterator().next();
        String taskInstanceId = taskInstance.getId();
        // :XXX

        // End task
        engine.endWorkItem(taskInstance, null);

        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, newPrincipal);
        assertEquals(0, taskInstances.size());

        taskInstance = engine.getWorkItemById(taskInstanceId);
        assertNotNull(taskInstance);
        assertTrue(taskInstance.hasEnded());

        Collection<WMActivityInstance> paths = engine.getActivityInstancesFor(path.getProcessInstance().getId());
        assertEquals(1, paths.size());

        // End state
        assertEquals("validated",
                paths.iterator().next().getActivityDefinition().getName());

        // Stop workflow.
        engine.terminateProcess(path.getProcessInstance().getId());
    }

    public void testGetVariablesFromPID() throws WMWorkflowException {

        WMParticipant principal = new WMParticipantImpl("anguenot");

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());
        params.put(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION, "approved");
        params.put(
                WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE,
                "approve");

        WMActivityInstance path = engine.startProcess(wdefID3, params, params);

        String pid = path.getProcessInstance().getId();
        Map<String, Serializable> vars = engine.listProcessInstanceAttributes(pid);

        assertTrue(vars.containsKey(WorkflowConstants.WORKFLOW_CREATOR));
        assertTrue(vars.containsKey(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION));
        assertTrue(vars.containsKey(WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE));

        assertEquals("anguenot", vars.get(WorkflowConstants.WORKFLOW_CREATOR));
        assertEquals("approved",
                vars.get(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION));
        assertEquals(
                "approve",
                vars.get(WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE));

        engine.terminateProcess(pid);
    }

    public void testGetVariablesFromPath() throws WMWorkflowException {
        WMParticipant principal = new WMParticipantImpl("anguenot");

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());
        params.put(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION, "approved");
        params.put(
                WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE,
                "approve");

        WMActivityInstance path = engine.startProcess(wdefID3, params, params);

        String pid = path.getProcessInstance().getId();
        Map<String, Serializable> vars = engine.listActivityInstanceAttributes(path);

        assertTrue(vars.containsKey(WorkflowConstants.WORKFLOW_CREATOR));
        assertTrue(vars.containsKey(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION));
        assertTrue(vars.containsKey(WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE));

        assertEquals("anguenot", vars.get(WorkflowConstants.WORKFLOW_CREATOR));
        assertEquals("approved",
                vars.get(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION));
        assertEquals(
                "approve",
                vars.get(WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE));

        engine.terminateProcess(pid);
    }

    public void testGetWorkflowDefinitionByName() throws Exception {
        newTransaction();

        assertNull(engine.getProcessDefinitionByName("simple1"));

        deployOne(RPATH_PD1);
        assertNotNull(engine.getProcessDefinitionByName("simple1"));

        newTransaction();
        undeployOne(RPATH_PD1);
    }

    public void testWorkflowTaskAPIWithParallelReviewingProcess()
            throws Exception {
        WMParticipant principal = new WMParticipantImpl("anguenot");
        WMParticipant anotherPrincipal = new WMParticipantImpl("julien");

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());
        params.put(WorkflowConstants.LIFE_CYCLE_STATE_DESTINATION, "approved");
        params.put(
                WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE,
                "approve");

        WMActivityInstance path = engine.startProcess(wdefID3, params, params);
        assertEquals("review", path.getActivityDefinition().getName());
        assertEquals(WorkflowConstants.TASK_NODE_TYPE_ID,
                path.getActivityDefinition().getNodeType());

        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();
        assertEquals("review", taskDef.getActivityDefinition().getName());

        Collection<WMWorkItemInstance> taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, principal);
        assertEquals(1, taskInstances.size());

        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, anotherPrincipal);
        assertEquals(0, taskInstances.size());

        WMWorkItemInstance taskInstance = engine.createWorkItem(path, taskDef,
                null);
        engine.assignWorkItem(taskInstance, anotherPrincipal);

        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, anotherPrincipal);
        assertEquals(1, taskInstances.size());
        assertNull(taskInstances.iterator().next().getStartDate());
        engine.startWorkItem(taskInstance);
        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, anotherPrincipal);
        assertNotNull(taskInstances.iterator().next().getStartDate());

        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, principal);
        assertEquals(1, taskInstances.size());

        taskInstances = engine.listWorkItems(path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, taskInstances.size());

        // End anotherPrincipal task
        // Now the workflow has reached its final state since the process is a
        // parallel reviewing one.

        taskInstances = engine.getWorkItemsFor(
                path.getProcessInstance().getId(),
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL, anotherPrincipal);
        WMWorkItemInstance task = taskInstances.iterator().next();
        engine.endWorkItem(task, null);

        Collection<WMActivityInstance> paths = engine.getActivityInstancesFor(path.getProcessInstance().getId());
        assertEquals("review",
                paths.iterator().next().getActivityDefinition().getName());
        engine.terminateProcess(path.getProcessInstance().getId());

        undeployOne(RPATH_PD3);
    }

    public void testGetWorkflowTaskDefinitions() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        assertTrue(path.getActivityDefinition().isTaskAwareActivity());
        String pid = path.getProcessInstance().getId();

        // From a given path.
        Set<WMWorkItemDefinition> tdefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, tdefs.size());

        // Recompute paths of execution and ask back.
        Collection<WMActivityInstance> paths = engine.getActivityInstancesFor(pid);
        assertEquals(1, paths.size());

        Set<WMWorkItemDefinition> agregated = new HashSet<WMWorkItemDefinition>();
        for (WMActivityInstance cpath : paths) {
            tdefs = engine.getWorkItemDefinitionsFor(cpath);
            for (WMWorkItemDefinition each : tdefs) {
                agregated.add(each);
            }
        }
        assertEquals(1, agregated.size());

        WMWorkItemInstance ti = engine.createWorkItem(paths.iterator().next(),
                tdefs.iterator().next(), null);
        assertNotNull(ti);

        engine.terminateProcess(pid);
        undeployOne(RPATH_PD3);
    }

    public void testVariableController() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        assertTrue(path.getActivityDefinition().isTaskAwareActivity());
        String pid = path.getProcessInstance().getId();

        // :TODO: need to be implemented.

        engine.terminateProcess(pid);
        undeployOne(RPATH_PD3);
    }

    public void testWorkflowCreator() throws WMWorkflowException {
        WMParticipant principal = new WMParticipantImpl("anguenot");

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, principal.getName());

        WMActivityInstance path = engine.startProcess(wdefID3, params, params);
        String pid = path.getProcessInstance().getId();

        assertEquals(principal.getName(),
                path.getProcessInstance().getAuthor().getName());
        assertEquals(principal.getName(),
                path.getProcessInstance().getAuthorName());

        engine.terminateProcess(pid);
    }

    public void testendTaskStates() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();

        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();

        Collection<WMWorkItemInstance> tis;

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        WMWorkItemInstance taskInstance = engine.createWorkItem(path, taskDef,
                null);
        assertNotNull(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        engine.startWorkItem(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        engine.endWorkItem(taskInstance, null);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        engine.terminateProcess(path.getProcessInstance().getId());
        undeployOne(RPATH_PD3);
    }

    public void testRemoveTaskStates() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();

        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();

        Collection<WMWorkItemInstance> tis;

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        WMWorkItemInstance taskInstance = engine.createWorkItem(path, taskDef,
                null);
        assertNotNull(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        engine.startWorkItem(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        engine.removeWorkItem(taskInstance);

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        int i = 0;
        for (WMWorkItemInstance wi : tis) {
            if (wi.isCancelled()) {
                i += 1;
            }
        }
        assertEquals(1, i);

        engine.terminateProcess(path.getProcessInstance().getId());
        undeployOne(RPATH_PD3);
    }

    public void testRejectTaskStates() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();

        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();

        Collection<WMWorkItemInstance> tis;

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        WMWorkItemInstance taskInstance = engine.createWorkItem(path, taskDef,
                null);
        assertNotNull(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        engine.startWorkItem(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        engine.rejectWorkItem(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_REJECTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        engine.terminateProcess(path.getProcessInstance().getId());
        undeployOne(RPATH_PD3);
    }

    public void testSuspendedTaskStates() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();

        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();

        Collection<WMWorkItemInstance> tis;

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        WMWorkItemInstance taskInstance = engine.createWorkItem(path, taskDef,
                null);
        assertNotNull(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(1, tis.size());

        engine.startWorkItem(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        engine.suspendWorkItem(taskInstance);

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_REJECTED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED);
        assertEquals(1, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(0, tis.size());

        tis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED);
        assertEquals(0, tis.size());

        engine.terminateProcess(path.getProcessInstance().getId());
        undeployOne(RPATH_PD3);
    }

    public void testExtendedTask() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();

        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_COMMENT, "A comment");
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER, 1);
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_DIRECTIVE, "Validation");
        Date dueDate = new Date();
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE, dueDate);

        WMWorkItemInstance ti = engine.createWorkItem(path, taskDef, props);
        assertEquals("A comment", ti.getComment());
        assertEquals(1, ti.getOrder());
        assertEquals("Validation", ti.getDirective());
        assertEquals(dueDate, ti.getDueDate());

        engine.terminateProcess(pid);
    }

    public void testWorkflowTerminateAfterTaskCompletion()
            throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();

        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();

        // Create additionial tasks
        engine.createWorkItem(path, taskDef, null);
        engine.createWorkItem(path, taskDef, null);

        // First kill the task created at process creation time
        Collection<WMWorkItemInstance> taskInstances = engine.listWorkItems(
                pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(3, taskInstances.size());

        for (WMWorkItemInstance ti : taskInstances) {
            engine.endWorkItem(ti, null);
        }

        WMProcessInstance wi = engine.getProcessInstanceById(pid);
        assertEquals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE,
                wi.getState());

        undeployOne(RPATH_PD3);
    }

    public void testInitialProcessVariablesInProcessDefinition()
            throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();
        Map<String, Serializable> props = engine.listProcessInstanceAttributes(pid);
        String reviewType = (String) props.get(WorkflowConstants.WORKLFOW_REVIEW_TYPE);
        assertEquals(WorkflowConstants.WORKFLOW_REVIEW_TYPE_PARALLEL,
                reviewType);

        engine.terminateProcess(pid);
        undeployOne(RPATH_PD3);
    }

    public void testSetProcessVariables() throws WMWorkflowException {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        String pid = path.getProcessInstance().getId();
        Map<String, Serializable> props = engine.listProcessInstanceAttributes(pid);
        String reviewType = (String) props.get(WorkflowConstants.WORKLFOW_REVIEW_TYPE);
        assertEquals(WorkflowConstants.WORKFLOW_REVIEW_TYPE_PARALLEL,
                reviewType);

        // Update an existing variables
        props.put(WorkflowConstants.WORKLFOW_REVIEW_TYPE,
                WorkflowConstants.WORKFLOW_REVIEW_TYPE_SERIE);

        // Add a new one
        props.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL, 1);

        engine.updateProcessInstanceAttributes(pid, props);
        props = engine.listProcessInstanceAttributes(pid);

        reviewType = (String) props.get(WorkflowConstants.WORKLFOW_REVIEW_TYPE);
        int level = (Integer) props.get(WorkflowConstants.WORKFLOW_REVIEW_LEVEL);
        assertEquals(WorkflowConstants.WORKFLOW_REVIEW_TYPE_SERIE, reviewType);
        assertEquals(1, level);

        // Test one prop defined on the jpdl process definition.

        String[] fetchVar = (String[]) props.get(WorkflowConstants.WORKFLOW_DIRECTIVES);
        assertEquals(2, fetchVar.length);

        engine.terminateProcess(pid);
        undeployOne(RPATH_PD3);
    }

    public void testCancelFetchTasks() throws WMWorkflowException {

        WMParticipant participant = new WMParticipantImpl("anguenot");

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        // Used both for the process variables and start task parameters for
        // now.
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(WorkflowConstants.WORKFLOW_CREATOR, participant.getName());

        WMActivityInstance path = engine.startProcess(wdefID3, params, params);
        String pid = path.getProcessInstance().getId();

        Collection<WMWorkItemInstance> tis;

        tis = engine.listWorkItems(pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        tis = engine.getWorkItemsFor(participant,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

        engine.endWorkItem(tis.iterator().next(), null);

        tis = engine.getWorkItemsFor(participant,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED);
        assertEquals(1, tis.size());

        tis = engine.getWorkItemsFor(participant,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(1, tis.size());

    }

    public void testActivityInstanceSerializable() throws Exception {

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        Map<String, Serializable> processVariables = new HashMap<String, Serializable>();
        processVariables.put(WorkflowConstants.DOCUMENT_REF, null);
        processVariables.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL, 0);
        processVariables.put(WorkflowConstants.WORKFLOW_FORMER_REVIEW_LEVEL, 0);
        processVariables.put(
                WorkflowConstants.DOCUMENT_VERSIONING_POLICY,
                WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_AUTO);
        processVariables.put(
                WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE,
                "obsolete");
        processVariables.put(WorkflowConstants.WORKFLOW_CREATOR,
                "Administrator");
        RepositoryLocation repoLoc = new RepositoryLocation("demo");
        processVariables.put(WorkflowConstants.DOCUMENT_LOCATION_URI, repoLoc);

        WMActivityInstance path = engine.startProcess(wdefID3,
                processVariables, null);
        assertTrue(SerializableHelper.isSerializable(path));

        undeployOne(RPATH_PD3);
    }

    public void testTaskUpdate() throws Exception {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        WMActivityInstance path = engine.startProcess(wdefID3, null, null);
        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(path);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();
        WMWorkItemInstance taskInstance = engine.createWorkItem(path, taskDef,
                null);
        assertEquals(0, taskInstance.getOrder());

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER, 1);

        taskInstance = engine.updateWorkItem(props, taskInstance);
        assertEquals(1, taskInstance.getOrder());
    }

    public void testProcessWithFork() throws Exception {

        final String PD_PATH = "samples/process_fork.xml";

        deployOne(PD_PATH);

        final String wdefId = getDefinitionFor(PD_PATH).getId();

        WMActivityInstance path = engine.startProcess(wdefId, null, null);
        final String pid = path.getProcessInstance().getId();

        Collection<WMWorkItemInstance> wiis = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL);
        assertEquals(2, wiis.size());

        assertEquals(1, engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED).size());

        assertEquals(1, engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED).size());

        // End the started task
        final WMWorkItemInstance wi = engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED).iterator().next();
        engine.endWorkItem(wi, null);

        // 2 started, 1 created and 1 closed.
        assertEquals(4, engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL).size());

        assertEquals(1, engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CREATED).size());

        assertEquals(2, engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_STARTED).size());

        assertEquals(1, engine.listWorkItems(pid,
                WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED).size());

        undeployOne(PD_PATH);
    }

    public void testProcessWithSwinlanes() throws Exception {

        final String PD_PATH = "samples/process_several_swinlanes.xml";

        deployOne(PD_PATH);

        final String wdefId = getDefinitionFor(PD_PATH).getId();

        WMActivityInstance path = engine.startProcess(wdefId, null, null);
        final String pid = path.getProcessInstance().getId();

        undeployOne(PD_PATH);
    }

    public void testListAllProcessInstanes() throws Exception {

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        assertEquals(0, engine.listProcessInstances(null).size());

        for (int i = 0; i < 5; i++) {
            engine.startProcess(wdefID3, null, null);
            assertEquals(i + 1, engine.listProcessInstances(null).size());
        }

        WMProcessInstanceIterator it = engine.listProcessInstances(null);
        while (it.hasNext()) {
            WMProcessInstance proc = it.next();
            // TODO: assert something?
        }

    }

    public void testListAllWorkItems() throws Exception {

        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        assertEquals(0, engine.listProcessInstances(null).size());

        for (int i = 0; i < 5; i++) {
            engine.startProcess(wdefID3, null, null);
        }

        assertEquals(5, engine.listWorkItems(null).size());

    }

    public void testFilterWorkItems() throws Exception {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        assertEquals(0, engine.listProcessInstances(null).size());

        WMActivityInstance activity = engine.startProcess(wdefID3, null, null);
        Set<WMWorkItemDefinition> taskDefs = engine.getWorkItemDefinitionsFor(activity);
        assertEquals(1, taskDefs.size());
        WMWorkItemDefinition taskDef = taskDefs.iterator().next();

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        Date dueDate = new Date();
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE, dueDate);
        WMWorkItemInstance taskInstance = engine.createWorkItem(activity,
                taskDef, props);

        assertEquals(1, engine.listWorkItems(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        WMFilter.LE, new Date())).size());

        assertEquals(1, engine.listWorkItems(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        WMFilter.LT, new Date())).size());

        assertEquals(0, engine.listWorkItems(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        WMFilter.GE, new Date())).size());

        assertEquals(0, engine.listWorkItems(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        WMFilter.GT, new Date())).size());

        assertEquals(0, engine.listWorkItems(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        WMFilter.EQ, new Date())).size());

        assertEquals(0, engine.listWorkItems(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        WMFilter.EQ, dueDate)).size());

        assertEquals(1, engine.listWorkItems(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
                        WMFilter.NE, new Date())).size());

        /*
         * FIXME assertEquals(0, engine.listWorkItems( new
         * WMFilterImpl(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE,
         * WMFilter.NE, dueDate)).size());
         */

    }

    public void testFilterProcessInstances() throws Exception {
        deployOne(RPATH_PD3);
        String wdefID3 = getDefinitionFor(RPATH_PD3).getId();

        assertEquals(0, engine.listProcessInstances(null).size());

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(WorkflowConstants.WORKFLOW_CREATOR, "Anton");
        engine.startProcess(wdefID3, props, null);

        assertEquals(1, engine.listProcessInstances(null).size());

        assertEquals(1, engine.listProcessInstances(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_CREATOR,
                        WMFilter.EQ, "Anton")).size());

        assertEquals(1, engine.listProcessInstances(
                new WMFilterImpl(WorkflowConstants.WORKFLOW_CREATOR,
                        WMFilter.NE, "Joy")).size());


    }

    public void testNoteTransitionsLoop01() throws Exception {

        final String PD_PATH = "samples/transitions_node_loop.xml";

        deployOne(PD_PATH);

        final String wdefId = getDefinitionFor(PD_PATH).getId();

        WMActivityInstance path = engine.startProcess(wdefId, null, null);

        WMParticipant principal = new WMParticipantImpl("awesome");

        Collection<WMWorkItemInstance> taskInstances = engine.listWorkItems(
                path.getId(), null);

        // Check task properties.
        WMWorkItemInstance taskInstance = taskInstances.iterator().next();
        engine.assignWorkItem(taskInstance, principal);

        // End task
        engine.endWorkItem(taskInstance, "submit");

        taskInstances = engine.getWorkItemsFor(principal, null);
        taskInstance = taskInstances.iterator().next();
        engine.endWorkItem(taskInstance, "reject");

        taskInstances = engine.getWorkItemsFor(principal, null);
        taskInstance = taskInstances.iterator().next();
        engine.endWorkItem(taskInstance, "accept");

    }

    public void testNoteTransitionsLoop02() throws Exception {

        final String PD_PATH = "samples/transitions_node_loop.xml";

        deployOne(PD_PATH);

        final String wdefId = getDefinitionFor(PD_PATH).getId();

        WMActivityInstance path = engine.startProcess(wdefId, null, null);

        WMParticipant principal = new WMParticipantImpl("awesome");

        Collection<WMWorkItemInstance> taskInstances = engine.listWorkItems(
                path.getId(), null);

        // Check task properties.
        WMWorkItemInstance taskInstance = taskInstances.iterator().next();
        engine.assignWorkItem(taskInstance, principal);

        // End task
        engine.endWorkItem(taskInstance, "submit");

        taskInstances = engine.getWorkItemsFor(principal, null);
        taskInstance = taskInstances.iterator().next();
        engine.endWorkItem(taskInstance, "accept");

    }

}
