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
 * $Id: FakeWorkflowEngine.java 28443 2008-01-02 18:16:28Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.ResultSlice;
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
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMProcessDefinitionStateImpl;
import org.nuxeo.ecm.platform.workflow.impl.AbstractWorkflowEngine;

/**
 * Fake workflow engine for tests.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class FakeWorkflowEngine extends AbstractWorkflowEngine {

    private final Map<String, WMProcessDefinition> definitions = new HashMap<String, WMProcessDefinition>();

    public Collection<WMProcessDefinition> getProcessDefinitions() {
        Collection<WMProcessDefinition> defs = new ArrayList<WMProcessDefinition>();
        for (WMProcessDefinition def : definitions.values()) {
            defs.add(def);
        }
        return defs;
    }

    /**
     * @deprecated Use {@link #deployDefinition(InputStream,String)} instead
     */
    @Deprecated
    public WMProcessDefinitionState deployDefinition(URL definitionURL,
            String mimetype) throws WMWorkflowException {
        return deployDefinition(definitionURL, mimetype);
    }

    public WMProcessDefinitionState deployDefinition(InputStream stream,
            String mimetype) throws WMWorkflowException {
        // :FIXME: compute this randomly if we want to test with more than one
        // definition.
        String id = "fake";
        WMProcessDefinition wdef = new FakeWorkflowDefinition(id,
                stream, mimetype);
        definitions.put(id, wdef);
        return new WMProcessDefinitionStateImpl(wdef, new String[0]);
    }

    public boolean isDefinitionDeployed(String pdefId) {
        return definitions.containsKey(pdefId);
    }

    public void undeployDefinition(String pdefId) throws WMWorkflowException {
        definitions.remove(pdefId);
    }

    public WMProcessDefinition getProcessDefinitionById(String pdefId) {
        return definitions.get(pdefId);
    }

    public void assignWorkItem(WMWorkItemInstance workItem,
            WMParticipant participant) {
    }

    public WMProcessInstance terminateProcess(String pid)
            throws WMWorkflowException {
        return null;
    }

    public WMActivityInstance followTransition(
            WMActivityInstance activityInstance, String transitionName,
            Map<String, Serializable> attrs) throws WMWorkflowException {
        return null;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            WMActivityInstance activityInstance, String state,
            WMParticipant participant) {
        return null;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            WMParticipant participant, String state) {
        return null;
    }

    public Collection<WMProcessInstance> getProcessInstancesFor(String pdefId) {
        return null;
    }

    public Collection<WMActivityInstance> getActivityInstancesFor(String pid)
            throws WMWorkflowException {
        return null;
    }

    public WMActivityInstance startProcess(String wdefId,
            Map<String, Serializable> attrs,
            Map<String, Serializable> startWorkItemAttrs)
            throws WMWorkflowException {
        return null;
    }

    public void unAssignWorkItem(WMWorkItemInstance workItem,
            WMParticipant participant) {
    }

    public Collection<WMProcessInstance> getActiveProcessInstancesFor(
            String workflowDefinitionId) {
        return null;
    }

    public WMProcessInstance getProcessInstanceById(String pid)
            throws WMWorkflowException {
        return null;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(String pid,
            String state, WMParticipant participant) {
        return null;
    }

    public WMWorkItemInstance endWorkItem(WMWorkItemInstance workItem,
            String transitionName) throws WMWorkflowException {
        return null;
    }

    public Map<String, Serializable> listActivityInstanceAttributes(
            WMActivityInstance activityInstance) {
        return null;
    }

    public WMWorkItemInstance startWorkItem(WMWorkItemInstance workItem) {
        return null;
    }

    public WMWorkItemInstance suspendWorkItem(WMWorkItemInstance workItem) {
        return null;
    }

    public WMWorkItemInstance updateWorkItem(Map<String, Serializable> props,
            WMWorkItemInstance workItem) throws WMWorkflowException {
        return null;
    }

    public WMWorkItemInstance getWorkItemById(String workItemId) {
        return null;
    }

    public Collection<WMWorkItemInstance> getPooledTasksFor(
            WMParticipant principal, String taskState) {
        return null;
    }

    public Collection<WMWorkItemInstance> getPooledTasksFor(String workflowId,
            String taskState, WMParticipant principal) {
        return null;
    }

    public Collection<WMWorkItemInstance> getPooledTasksFor(
            WMActivityInstance path, String taskState, WMParticipant principal) {
        return null;
    }

    public WMWorkItemInstance getTaskById(String taskInstanceId) {
        return null;
    }

    public WMProcessDefinition getProcessDefinitionByName(String name) {
        return null;
    }

    public Map<String, Serializable> listProcessInstanceAttributes(String pid) {
        return null;
    }

    public WMWorkItemInstance createWorkItem(
            WMActivityInstance activityInstance,
            WMWorkItemDefinition workItemDefinition,
            Map<String, Serializable> attrs) throws WMWorkflowException {
        return null;
    }

    public Set<WMWorkItemDefinition> getWorkItemDefinitionsFor(
            WMActivityInstance activityInstance) {
        return null;
    }

    public Collection<WMWorkItemInstance> listWorkItems(String pid, String state) {
        return new ArrayList<WMWorkItemInstance>();
    }

    public void removeWorkItem(WMWorkItemInstance workItem) {
    }

    public void rejectWorkItem(WMWorkItemInstance workItem) {
    }

    public void updateProcessInstanceAttributes(String pid,
            Map<String, Serializable> attrs) throws WMWorkflowException {
    }

    public WMProcessInstanceIterator listProcessInstances(WMFilter filter)
            throws WMWorkflowException {
        return null;
    }

    public WMWorkItemIterator listWorkItems(WMFilter filter) {
        return null;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            List<WMParticipant> participants, String state) {
        return null;
    }

    public ResultSlice<WMWorkItemInstance> getWorkItemsFor(
            List<WMParticipant> participants, String state, int firstResult,
            int maxResult) {
        return null;
    }

    public Collection<WMProcessInstance> listProcessInstanceForCreators(
            List<String> creators) {
        return null;
    }

}
