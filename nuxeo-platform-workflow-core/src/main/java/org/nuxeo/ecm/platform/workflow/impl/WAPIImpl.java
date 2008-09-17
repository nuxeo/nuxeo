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
 * $Id: WAPIImpl.java 22990 2007-07-25 17:58:00Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.NXWorkflow;
import org.nuxeo.ecm.platform.workflow.api.WorkflowEngine;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.ResultSlice;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMFilter;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstanceIterator;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemIterator;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.service.WorkflowService;

/**
 * Local Workflow API implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WAPIImpl implements WAPI {

    private static final Log log = LogFactory.getLog(WAPI.class);

    private static final long serialVersionUID = -618092777574561974L;

    /**
     * Returns the core workflow service singleton.
     *
     * @return a WorkflowService instance
     */
    protected WorkflowService getWorkflowService() {
        return NXWorkflow.getWorkflowService();
    }

    /**
     * Returns the default workflow engine registered.
     *
     * @return a WorkflowEngine instance
     */
    protected WorkflowEngine getDefaultEngine() {
        WorkflowService workflowService = getWorkflowService();
        String defaultEngineName = workflowService.getDefaultEngineName();
        WorkflowEngine workflowEngine = workflowService.getWorkflowEngineByName(defaultEngineName);
        if (workflowEngine == null) {
            log.error("No default workflow engine registered.....");
        }
        return workflowEngine;
    }

    public WMProcessInstance terminateProcessInstance(String processInstanceId)
            throws WMWorkflowException {

        WMProcessInstance pi;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            pi = workflowEngine.terminateProcess(processInstanceId);
        } else {
            pi = null;
        }

        return pi;
    }

    public Collection<WMProcessDefinition> listProcessDefinitions()
            throws WMWorkflowException {

        Collection<WMProcessDefinition> pdefs = new ArrayList<WMProcessDefinition>();

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            pdefs = workflowEngine.getProcessDefinitions();
        }

        return pdefs;
    }

    public WMProcessDefinition getProcessDefinitionById(String id)
            throws WMWorkflowException {

        WMProcessDefinition pdef;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            pdef = workflowEngine.getProcessDefinitionById(id);
        } else {
            pdef = null;
        }

        return pdef;
    }

    public WMActivityInstance startProcess(String processDefinitionId,
            Map<String, Serializable> processVariables,
            Map<String, Serializable> startWorkItemParameters)
            throws WMWorkflowException {

        WMActivityInstance activityInstance;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            activityInstance = workflowEngine.startProcess(processDefinitionId,
                    processVariables, startWorkItemParameters);
        } else {
            activityInstance = null;
        }

        return activityInstance;
    }

    public WMProcessInstance getProcessInstanceById(String pid, String state)
            throws WMWorkflowException {

        WMProcessInstance pi = null;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            pi = workflowEngine.getProcessInstanceById(pid);
            // Filter out on status
            if (pi != null
                    && state != null
                    && state.equals(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE)) {
                if (!pi.getState().equals(state)) {
                    pi = null;
                }
            }
        }

        return pi;
    }

    public Collection<WMActivityInstance> getActivityInstancesFor(String pid)
            throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            return workflowEngine.getActivityInstancesFor(pid);
        }
        return null;
    }

    public WMActivityInstance followTransition(
            WMActivityInstance activityInstance, String transitionName,
            Map<String, Serializable> attrs) throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            activityInstance = workflowEngine.followTransition(
                    activityInstance, transitionName, attrs);
        } else {
            activityInstance = null;
        }

        return activityInstance;
    }

    public void assignWorkItem(String workItemId, WMParticipant participant)
            throws WMWorkflowException {

        WorkflowEngine workflowEngine = getDefaultEngine();

        if (workflowEngine != null) {
            WMWorkItemInstance workItem = workflowEngine.getWorkItemById(workItemId);
            workflowEngine.assignWorkItem(workItem, participant);
        }

    }

    public void unAssignWorkItem(String workItemId, WMParticipant participant)
            throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            WMWorkItemInstance workItem = workflowEngine.getWorkItemById(workItemId);
            workflowEngine.unAssignWorkItem(workItem, participant);
        }
    }

    public Principal getParticipant() {
        return null;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            WMParticipant participant, String state) throws WMWorkflowException {

        Collection<WMWorkItemInstance> workItems;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItems = workflowEngine.getWorkItemsFor(participant, state);
        } else {
            workItems = new ArrayList<WMWorkItemInstance>();
        }

        return workItems;
    }
    public Collection<WMWorkItemInstance> getWorkItemsFor(
            List<WMParticipant> participant, String state) throws WMWorkflowException {

        Collection<WMWorkItemInstance> workItems;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItems = workflowEngine.getWorkItemsFor(participant, state);
        } else {
            workItems = new ArrayList<WMWorkItemInstance>();
        }

        return workItems;
    }

    public ResultSlice<WMWorkItemInstance> getWorkItemsFor(
            List<WMParticipant> participant, String state, int firstResult,
            int maxResult) throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            return workflowEngine.getWorkItemsFor(participant, state, firstResult, maxResult);
        } else {
            return new ResultSlice<WMWorkItemInstance>(
                    Collections.<WMWorkItemInstance> emptyList(), firstResult,
                    maxResult, 0);
        }
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(String pid,
            String state, WMParticipant participant) throws WMWorkflowException {

        Collection<WMWorkItemInstance> workItems;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItems = workflowEngine.getWorkItemsFor(pid, state, participant);
        } else {
            workItems = new ArrayList<WMWorkItemInstance>();
        }

        return workItems;
    }

    public WMProcessDefinition getProcessDefinitionByName(String name)
            throws WMWorkflowException {
        WMProcessDefinition pdef;

        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            pdef = workflowEngine.getProcessDefinitionByName(name);
        } else {
            pdef = null;
        }

        return pdef;
    }

    public WMWorkItemInstance getWorkItemById(String workItemId)
            throws WMWorkflowException {
        WMWorkItemInstance workItem = null;
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItem = workflowEngine.getWorkItemById(workItemId);
        }
        return workItem;
    }

    public WMWorkItemInstance endWorkItem(String workItemId, String transition)
            throws WMWorkflowException {
        WMWorkItemInstance workItem = null;
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItem = getWorkItemById(workItemId);
            if (workItem != null) {
                workItem = workflowEngine.endWorkItem(workItem, transition);
            }
        }
        return workItem;
    }

    public WMWorkItemInstance startWorkItem(String workItemId)
            throws WMWorkflowException {
        WMWorkItemInstance workItem = null;
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItem = getWorkItemById(workItemId);
            if (workItem != null) {
                workItem = workflowEngine.startWorkItem(workItem);
            }
        }
        return workItem;
    }

    public WMWorkItemInstance suspendWorkItem(String workItemId)
            throws WMWorkflowException {
        WMWorkItemInstance workItem = null;
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItem = getWorkItemById(workItemId);
            if (workItem != null) {
                workItem = workflowEngine.suspendWorkItem(workItem);
            }
        }
        return workItem;
    }

    public Map<String, Serializable> listProcessInstanceAttributes(String pid) {
        Map<String, Serializable> attributes = new HashMap<String, Serializable>();
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            attributes = workflowEngine.listProcessInstanceAttributes(pid);
        }
        return attributes;
    }

    public WMWorkItemInstance createWorkItem(String pid, String workItemDefId,
            Map<String, Serializable> variables) throws WMWorkflowException {
        WMWorkItemInstance workItem = null;
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            Collection<WMActivityInstance> activities = workflowEngine.getActivityInstancesFor(pid);
            for (WMActivityInstance activity : activities) {
                Set<WMWorkItemDefinition> workItemDefinitions = workflowEngine.getWorkItemDefinitionsFor(activity);
                for (WMWorkItemDefinition workItemDefinition : workItemDefinitions) {
                    if (workItemDefinition.getId().equals(workItemDefId)) {
                        workItem = workflowEngine.createWorkItem(activity,
                                workItemDefinition, variables);
                    }
                }
            }
        }
        return workItem;
    }

    public Collection<WMWorkItemInstance> listWorkItems(String pid,
            String workItemState) {
        Collection<WMWorkItemInstance> workItems = new ArrayList<WMWorkItemInstance>();
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workItems = workflowEngine.listWorkItems(pid, workItemState);
        }
        return workItems;
    }

    public Set<WMWorkItemDefinition> getWorkItemDefinitionsFor(String pid)
            throws WMWorkflowException {
        Set<WMWorkItemDefinition> workItemDefinitions = new HashSet<WMWorkItemDefinition>();
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            Collection<WMActivityInstance> activities = workflowEngine.getActivityInstancesFor(pid);
            for (WMActivityInstance activity : activities) {
                Set<WMWorkItemDefinition> workItemDefinition = workflowEngine.getWorkItemDefinitionsFor(activity);
                workItemDefinitions.addAll(workItemDefinition);
            }
        }
        return workItemDefinitions;
    }

    public void removeWorkItem(String workItemId) throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            WMWorkItemInstance workItem = workflowEngine.getWorkItemById(workItemId);
            workflowEngine.removeWorkItem(workItem);
        }
    }

    public void rejectWorkItem(String workItemId) throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            WMWorkItemInstance workItem = workflowEngine.getWorkItemById(workItemId);
            workflowEngine.rejectWorkItem(workItem);
        }
    }

    public void updateProcessInstanceAttributes(String pid,
            Map<String, Serializable> variables) throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workflowEngine.updateProcessInstanceAttributes(pid, variables);
        }
    }

    public WMWorkItemInstance createWorkItemFromName(String pid,
            String workItemDefName, Map<String, Serializable> variables)
            throws WMWorkflowException {
        WMWorkItemInstance workItem = null;
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            Collection<WMActivityInstance> activities = workflowEngine.getActivityInstancesFor(pid);
            for (WMActivityInstance activity : activities) {
                Set<WMWorkItemDefinition> workItemDefinitions = workflowEngine.getWorkItemDefinitionsFor(activity);
                for (WMWorkItemDefinition workItemDefinition : workItemDefinitions) {
                    if (workItemDefinition.getName().equals(workItemDefName)) {
                        workItem = workflowEngine.createWorkItem(activity,
                                workItemDefinition, variables);
                    }
                }
            }
        }
        return workItem;
    }

    public void updateWorkItemAttributes(String id,
            Map<String, Serializable> variables) throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            workflowEngine.updateWorkItem(variables, getWorkItemById(id));
        }
    }

    public WMProcessInstanceIterator listProcessInstances(WMFilter filter)
            throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            return workflowEngine.listProcessInstances(filter);
        }
        return null;
    }

    public WMWorkItemIterator listWorkItems(WMFilter filter)
            throws WMWorkflowException {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            return workflowEngine.listWorkItems(filter);
        }
        return null;
    }

    public Collection<WMProcessInstance> getProcessInstanceForCreators(
            List<String> groupNames) {
        WorkflowEngine workflowEngine = getDefaultEngine();
        if (workflowEngine != null) {
            return workflowEngine.listProcessInstanceForCreators(groupNames);
        }
        return null;
    }

}
