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

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enables client applications to connect to and interact with the Nuxeo 5
 * workflow engine.
 * <p>
 * The API includes both process and work item related API.
 * <p>
 * Process definitions deployment are not part of this API. See NXWorkflow
 * extension points.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WAPI extends Serializable {

    /**
     * Returns the current participant interacting with the facade.
     *
     * @return a Principal instance
     */
    Principal getParticipant();

    /**
     * Returns all deployed process definitions.
     *
     * @return an array of WMProcessDefinition instances.
     * @throws WMWorkflowException
     */
    Collection<WMProcessDefinition> listProcessDefinitions()
            throws WMWorkflowException;

    /**
     * Returns a process definition given its id.
     *
     * @param id the process definition id
     * @return a WMProcessDefinition instance
     * @throws WMWorkflowException
     */
    WMProcessDefinition getProcessDefinitionById(String id)
            throws WMWorkflowException;

    /**
     * Returns a process definition given its name.
     *
     * @param name the name of the process definition
     * @return a WMProcessDefinition instance
     */
    WMProcessDefinition getProcessDefinitionByName(String name)
            throws WMWorkflowException;

    /**
     * Starts a process instance given the name of the process definition.
     *
     * @param processDefinitionId the process definition identifier
     * @param processVariables process variables
     * @param startWorkItemParameters useless: parameters for the start work
     *            item are taken from the processVariables map.
     * @return a WMActvityInstance instance
     * @throws WMWorkflowException
     */
    WMActivityInstance startProcess(String processDefinitionId,
            Map<String, Serializable> processVariables,
            Map<String, Serializable> startWorkItemParameters)
            throws WMWorkflowException;

    /**
     * Terminates a process instance given its id.
     *
     * @param processInstanceId the process instance identifier
     * @return a WMProcessInstance
     * @throws WMWorkflowException
     */
    WMProcessInstance terminateProcessInstance(String processInstanceId)
            throws WMWorkflowException;

    /**
     * Gets activities for given process instance.
     *
     * @param pid the process instance identifier
     * @return a collection of activity instances
     * @throws WMWorkflowException TODO
     */
    Collection<WMActivityInstance> getActivityInstancesFor(String pid)
            throws WMWorkflowException;

    /**
     * Follows a transition given the activity and the transition.
     *
     * @param activityInstance the activity instance that will receive a signal
     *            for transition
     * @param transitionName the transition to follow
     * @param attrs the process attributes to setup on the process.
     * @return the destination activity instance.
     * @throws WMWorkflowException TODO
     */
    WMActivityInstance followTransition(WMActivityInstance activityInstance,
            String transitionName, Map<String, Serializable> attrs)
            throws WMWorkflowException;

    /**
     * Returns a process instance given its identifier.
     *
     * @param processInstanceId the process instance identifier
     * @param state TODO
     * @throws WMWorkflowException
     * @return a WMProcessInstance matching the identifier
     */
    WMProcessInstance getProcessInstanceById(String processInstanceId,
            String state) throws WMWorkflowException;

    /**
     * Assigns a work item to a given participant.
     *
     * @param workItemId the work item identifier
     * @param participant the workflow participant
     * @throws WMWorkflowException
     */
    void assignWorkItem(String workItemId, WMParticipant participant)
            throws WMWorkflowException;

    /**
     * Unassigns a work item.
     *
     * @param workItemId the work item
     * @param participant the workflow participant
     * @throws WMWorkflowException
     */
    void unAssignWorkItem(String workItemId, WMParticipant participant)
            throws WMWorkflowException;

    /**
     * Returns all work items in a given state for a given workflow participant.
     * <p>
     * If the state is null then we will return all work items matching.
     *
     * @param participant the workflow participant
     * @param state the work item state
     * @return a collection of work item
     * @throws WMWorkflowException
     */
    Collection<WMWorkItemInstance> getWorkItemsFor(WMParticipant participant,
            String state) throws WMWorkflowException;

    Collection<WMWorkItemInstance> getWorkItemsFor(List<WMParticipant> participant,
            String state) throws WMWorkflowException;
    ResultSlice<WMWorkItemInstance> getWorkItemsFor(List<WMParticipant> participant,
            String state, int firstResult, int maxResult) throws WMWorkflowException;

    /**
     * Returns for a given workflow participant and for a given process instance
     * all work items in a given state.
     * <p>
     * If the work item state is null then we will return all work items.
     *
     * @param pid the process instance instance id
     * @param state the work item state
     * @param participant the workflow participant
     * @return a collection of work item for the path given as as parameter
     * @throws WMWorkflowException
     */
    Collection<WMWorkItemInstance> getWorkItemsFor(String pid, String state,
            WMParticipant participant) throws WMWorkflowException;

    /**
     * Returns a work item given its id.
     *
     * @param workItemId the work item identifier
     * @return a WMWorkItemInstance
     * @throws WMWorkflowException
     */
    WMWorkItemInstance getWorkItemById(String workItemId)
            throws WMWorkflowException;

    /**
     * Starts a work item given its identifier.
     *
     * @param workItemId the work item identifier
     * @return a work item instance
     * @throws WMWorkflowException
     */
    WMWorkItemInstance startWorkItem(String workItemId)
            throws WMWorkflowException;

    /**
     * Ends a work item.
     *
     * @param workItemId the workItem identifier id
     * @param transition outgoing transition to follow if the work item was a
     *            blocking a work item
     * @return a WMWorkItemInstance
     * @throws WMWorkflowException
     */
    WMWorkItemInstance endWorkItem(String workItemId, String transition)
            throws WMWorkflowException;

    /**
     * Suspends a work item given its identifier.
     *
     * @param workItemId the work item id
     * @return a WMWorkItemInstance
     * @throws WMWorkflowException
     */
    WMWorkItemInstance suspendWorkItem(String workItemId)
            throws WMWorkflowException;

    /**
     * Returns the process instance variables.
     *
     * @param pid the workflow instance identifier
     * @return a map from string to serializable
     */
    Map<String, Serializable> listProcessInstanceAttributes(String pid);

    /**
     * Updates the process variables.
     *
     * @param pid the process instance identifier
     * @param variables map from string to serializable.
     */
    void updateProcessInstanceAttributes(String pid,
            Map<String, Serializable> variables) throws WMWorkflowException;

    /**
     * Update the work item attributes given the work item identifier.
     *
     * @param id : the workitem identifier
     * @param variables : map from prop id to prop value
     * @throws WMWorkflowException
     */
    void updateWorkItemAttributes(String id, Map<String, Serializable> variables)
            throws WMWorkflowException;

    /**
     * Returns the list of work items given a pid and a work item state.
     *
     * @param pid the process instance identifier
     * @param workItemState the work item state
     * @return a collection of work item
     */
    Collection<WMWorkItemInstance> listWorkItems(String pid,
            String workItemState);

    /**
     * Creates a new work item given its work item definition id.
     *
     * @param pid the workflow instance id
     * @param workItemDefId the work item definition id
     * @param variables the work item attributes
     * @return a work item instance
     * @throws WMWorkflowException
     */
    WMWorkItemInstance createWorkItem(String pid, String workItemDefId,
            Map<String, Serializable> variables) throws WMWorkflowException;

    /**
     * Creates a new work item given its work item definition name.
     *
     * @param pid the workflow instance id
     * @param workItemDefName the work item definition name
     * @param variables the work item attributes
     * @return a work item instance
     * @throws WMWorkflowException
     */
    WMWorkItemInstance createWorkItemFromName(String pid,
            String workItemDefName, Map<String, Serializable> variables)
            throws WMWorkflowException;

    /**
     * Returns the list of work item definitions for a given process instance.
     *
     * @param pid the process instance identifier
     * @return a collection of work item definitions.
     */
    Set<WMWorkItemDefinition> getWorkItemDefinitionsFor(String pid)
            throws WMWorkflowException;

    /**
     * Removes a work item given its id.
     *
     * @param workItemId the work item identifier
     */
    void removeWorkItem(String workItemId) throws WMWorkflowException;

    /**
     * Rejects a work item given its id.
     *
     * @param workItemId the work item identifier
     * @throws WMWorkflowException
     */
    void rejectWorkItem(String workItemId) throws WMWorkflowException;

    /**
     * Returns a process instances iterator
     *
     * @param filter : a filter instance
     * @return a process instances iterator
     * @throws WMWorkflowException
     */
    WMProcessInstanceIterator listProcessInstances(WMFilter filter)
            throws WMWorkflowException;

    /**
     * Returns a work item instance iterator.
     *
     * @param filter : a filter instance
     * @return a work item instance iterator.
     * @throws WMWorkflowException
     */
    WMWorkItemIterator listWorkItems(WMFilter filter)
            throws WMWorkflowException;

    /**
     * Return a list of process instance having one of the group has creator.
     *
     * @param groupNames a list of group.
     * @return
     */
    Collection<WMProcessInstance> getProcessInstanceForCreators(List<String> groupNames);

}
