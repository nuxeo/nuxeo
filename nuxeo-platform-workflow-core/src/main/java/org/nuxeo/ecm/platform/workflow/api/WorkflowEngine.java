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
 * $Id: WorkflowEngine.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMFilter;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinitionState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstanceIterator;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemIterator;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;

/**
 * Workflow engine common interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowEngine {

    /**
     * Gets the workflow engine name.
     *
     * @return the workflow engine name
     */
    String getName();

    /**
     * Sets the workflow engine name.
     *
     * @param name the name of the workflow engine.
     */
    void setName(String name);

    /**
     * Deploys a process definition.
     *
     * @param definitionURL a WMProcessDefinition
     * @return the process definition id
     * @throws WMWorkflowException
     * @deprecated Use {@link #deployDefinition(InputStream,String)} instead
     */
    @Deprecated
    WMProcessDefinitionState deployDefinition(URL definitionURL, String mimetype)
            throws WMWorkflowException;

    /**
     * Deploys a process definition.
     *
     * @param stream a WMProcessDefinition
     * @return the process definition id
     * @throws WMWorkflowException
     */
    WMProcessDefinitionState deployDefinition(InputStream stream, String mimetype)
            throws WMWorkflowException;

    /**
     * Undeploys a process definition given its identifier.
     *
     * @param pdefId the identifier of the process definition to undeploy
     * @throws WMWorkflowException
     */
    void undeployDefinition(String pdefId) throws WMWorkflowException;

    /**
     * Returns all process definitions deployed.
     *
     * @return a collection of WMProcessDefinition
     */
    Collection<WMProcessDefinition> getProcessDefinitions();

    /**
     * Returns a process definition given its identifier.
     *
     * @param pdefId the process definition identifier
     * @return the process definition matching the identifier
     */
    WMProcessDefinition getProcessDefinitionById(String pdefId);

    /**
     * Returns a process definition given its name.
     *
     * @param name the name of the process definition
     * @return the process definition matching the name
     */
    WMProcessDefinition getProcessDefinitionByName(String name);

    /**
     * Is the definition matching a given identifier deployed?
     *
     * @param pdefId the process definition identifier
     * @return a boolean
     */
    boolean isDefinitionDeployed(String pdefId);

    /**
     * Starts a process given its identifier, process attributes and start work
     * item attributes.
     *
     * @param wdefId the process definition identifier
     * @param attrs the process attributes to setup on the process.
     * @param startWorkItemAttrs the start work item attributes to setup if a
     *            start activity exists
     * @return the start activity
     * @throws WMWorkflowException TODO
     */
    WMActivityInstance startProcess(String wdefId,
            Map<String, Serializable> attrs,
            Map<String, Serializable> startWorkItemAttrs)
            throws WMWorkflowException;

    /**
     * Terminates a process given its identifier.
     *
     * @param pid the process definition identifier
     * @return a snapshot of the process instance after the it has been
     *         terminated.
     * @throws WMWorkflowException TODO
     */
    WMProcessInstance terminateProcess(String pid) throws WMWorkflowException;

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
     * Returns all the process instances corresponding to a given process
     * definition.
     *
     * @param pdefId the identifier of a process definition
     * @return a collection of process instances.
     */
    Collection<WMProcessInstance> getProcessInstancesFor(String pdefId);

    /**
     * Returns a process instance given its identifier.
     *
     * @param pid the process instance identifier
     * @throws WMWorkflowException
     * @return a WMProcessInstance matching the identifier
     */
    WMProcessInstance getProcessInstanceById(String pid)
            throws WMWorkflowException;

    /**
     * Returns the active process instances corresponding to a given process
     * definition.
     *
     * @param workflowDefinitionId a process definition id
     * @return a collection of active process instances.
     */
    Collection<WMProcessInstance> getActiveProcessInstancesFor(
            String workflowDefinitionId);

    /**
     * Returns the attributes for a given activity.
     *
     * @param activityInstance the activity instance
     * @return the activity instance attributes in a Map instance
     */
    Map<String, Serializable> listActivityInstanceAttributes(
            WMActivityInstance activityInstance);

    /**
     * Returns the attributes for a given process instance.
     *
     * @param pid the process instance identifier
     * @return the process instance attributes in a Map instance
     */
    Map<String, Serializable> listProcessInstanceAttributes(String pid);

    /**
     * Updates a process instance attributes.
     *
     * @param pid the process instance identifier
     * @param attrs map from string to serializable.
     */
    void updateProcessInstanceAttributes(String pid,
            Map<String, Serializable> attrs) throws WMWorkflowException;

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
     * Returns a work item given is identifier.
     *
     * @param workItemId the work item identifier
     * @return a work item instance
     */
    WMWorkItemInstance getWorkItemById(String workItemId);

    /**
     * Assigns a work item to a given workflow participant.
     *
     * @param workItem the work item instance
     * @param participant the workflow participant
     */
    void assignWorkItem(WMWorkItemInstance workItem, WMParticipant participant);

    /**
     * Unassigns a work item to a given workflow participant.
     *
     * @param workItem the work item instance
     * @param participant the workflow participant
     */
    void unAssignWorkItem(WMWorkItemInstance workItem, WMParticipant participant);

    /**
     * Returns the list of work item given a process instance id and a work item
     * state.
     *
     * @see org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState
     *
     * @param pid the process instance id
     * @param state the work item state
     * @return a collection of work item instances
     */
    Collection<WMWorkItemInstance> listWorkItems(String pid, String state);

    /**
     * Returns the work items for a given workflow participant in a given state.
     *
     * @see org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState
     *
     * @param participant the workflow participant
     * @param state the work item state
     * @return a collection of work items
     */
    Collection<WMWorkItemInstance> getWorkItemsFor(WMParticipant participant,
            String state);

    /**
     * Returns the work items for a given process instance in a given state.
     *
     * @see org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState
     *
     * @param pid the process instance id
     * @param state the work item state
     * @param participant the workflow participant
     * @return a collection of work item instances
     */
    Collection<WMWorkItemInstance> getWorkItemsFor(String pid, String state,
            WMParticipant participant);

    /**
     * Returns the work items for a given activity in a given state.
     *
     * @see org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState
     *
     * @param activityInstance the activity instance
     * @param state the work item state
     * @param participant the workflow participant
     * @return a collection of work item for the activity given as as parameter
     */
    Collection<WMWorkItemInstance> getWorkItemsFor(
            WMActivityInstance activityInstance, String state,
            WMParticipant participant);

    /**
     * Starts a workItem given.
     *
     * @param workItem the work item instance
     * @return the work item instance
     */
    WMWorkItemInstance startWorkItem(WMWorkItemInstance workItem);

    /**
     * Ends a given work item and specify the specific transition to follow
     * after completude.
     * <p>
     * If no transition are specified then follow the default one.
     *
     * @param workItem the work item instance
     * @param transitionName the name of the transition to follow
     * @return a WMWorkItemInstance instance
     * @throws WMWorkflowException TODO
     */
    WMWorkItemInstance endWorkItem(WMWorkItemInstance workItem,
            String transitionName) throws WMWorkflowException;

    /**
     * Suspends work item.
     *
     * @param workItem the work item instance
     * @return the suspended work item instance
     */
    WMWorkItemInstance suspendWorkItem(WMWorkItemInstance workItem);

    /**
     * Updates a work item.
     *
     * @param props the map from prop name to prop value
     * @param workItem the work item instance
     * @return the updated work item instance
     * @throws WMWorkflowException TODO
     */
    WMWorkItemInstance updateWorkItem(Map<String, Serializable> props,
            WMWorkItemInstance workItem) throws WMWorkflowException;

    /**
     * Removes a work item instance.
     *
     * @param workItem the work item instance
     */
    void removeWorkItem(WMWorkItemInstance workItem);

    /**
     * Rejects a work item.
     *
     * @param workItem the work item instance
     */
    void rejectWorkItem(WMWorkItemInstance workItem);

    /**
     * Returns the list of work item definitions for an activity instance.
     *
     * @param activityInstance the activity instance
     * @return a collection of activity instance definitions.
     */
    Set<WMWorkItemDefinition> getWorkItemDefinitionsFor(
            WMActivityInstance activityInstance);

    /**
     * Creates a work item for a given participant given a work item definition.
     *
     * @param activityInstance the activity instance
     * @param workItemDefinition the work item definition
     * @param attrs the work item attributes
     * @return a work item instance
     * @throws WMWorkflowException TODO
     */
    WMWorkItemInstance createWorkItem(WMActivityInstance activityInstance,
            WMWorkItemDefinition workItemDefinition,
            Map<String, Serializable> attrs) throws WMWorkflowException;

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
}
