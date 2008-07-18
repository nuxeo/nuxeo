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
 * $Id: JbpmWorkflowEngine.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.bytes.ByteArray;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.variableinstance.ByteArrayInstance;
import org.jbpm.context.exe.variableinstance.StringInstance;
import org.jbpm.db.GraphSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;
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
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMProcessDefinitionStateImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMProcessInstanceIteratorImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMWorkItemIteratorImpl;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.impl.AbstractWorkflowEngine;
import org.nuxeo.ecm.platform.workflow.jbpm.tasks.ExtendedTaskInstance;
import org.nuxeo.ecm.platform.workflow.jbpm.util.IDConverter;
import org.nuxeo.ecm.platform.workflow.jbpm.util.NXJpdlXmlReader;

/**
 * JBPM workflow engine implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class JbpmWorkflowEngine extends AbstractWorkflowEngine {

    private static final Log log = LogFactory.getLog(JbpmWorkflowEngine.class);

    /**
     * @deprecated Use {@link #deployDefinition(InputStream,String)} instead
     */
    @Deprecated
    public WMProcessDefinitionState deployDefinition(URL definitionURL,
            String mimetype) throws WMWorkflowException {
        try {
            return deployDefinition(definitionURL.openStream(), mimetype);
        } catch (IOException e) {
            throw new WMWorkflowException(e);
        }
    }

    public WMProcessDefinitionState deployDefinition(InputStream stream,
            String mimetype) throws WMWorkflowException {

        if (stream == null) {
            throw new WMWorkflowException("Not definition provided !");
        }

        ProcessDefinition processDefinition = ProcessDefinition.parseXmlInputStream(stream);
        WMProcessDefinitionState workflowDeployment;

        String nxwDefId = IDConverter.getNXWorkflowIdentifier(processDefinition.getId());
        String[] status = getJpdlStatus(stream);
        if (!isDefinitionDeployed(nxwDefId)) {

            JbpmWorkflowExecutionContext ctx = getExecutionContext();

            try {
                ctx.getContext().deployProcessDefinition(processDefinition);
            } catch (JbpmException jbpme) {
                // jbpme.printStackTrace();
                throw new WMWorkflowException(
                        "An error occurred while trying to deploy the process definition !",
                        jbpme);
            }

            WMProcessDefinition pdef = WAPIGenerator.createProcessDefinition(processDefinition);

            ctx.closeContext();

            // :XXX: The status of the jpdl file should be checked before trying
            // to
            // deploy
            workflowDeployment = new WMProcessDefinitionStateImpl(pdef, status);
        } else {
            log.info("Definition already deployed !");
            workflowDeployment = new WMProcessDefinitionStateImpl(
                    getProcessDefinitionById(nxwDefId), status);
        }

        return workflowDeployment;
    }

    public void undeployDefinition(String pdefId) throws WMWorkflowException {
        WMProcessDefinition pdef = getProcessDefinitionById(pdefId);
        if (pdef == null) {
            throw new WMWorkflowException("Definition " + pdefId
                    + "is not deployed !");
        }

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();
        try {
            graphSession.deleteProcessDefinition(IDConverter.getJbpmIdentifier(pdef.getId()));
        } catch (JbpmException jbpme) {
            throw new WMWorkflowException(
                    "Error while attempting to undeploy workflow definition with id="
                            + pdefId, jbpme);
        }
        ctx.closeContext();
    }

    public WMProcessDefinition getProcessDefinitionById(String pdefId) {

        WMProcessDefinition def = null;

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        ProcessDefinition pdef = getProcessDefinition(pdefId, graphSession);

        if (pdef != null) {
            def = WAPIGenerator.createProcessDefinition(pdef);
        }

        ctx.closeContext();
        return def;
    }

    public boolean isDefinitionDeployed(String pdefId) {
        return getProcessDefinitionById(pdefId) != null;
    }

    public Collection<WMProcessDefinition> getProcessDefinitions() {
        Collection<WMProcessDefinition> pdefs = new ArrayList<WMProcessDefinition>();

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();
        for (Object graph : graphSession.findLatestProcessDefinitions()) {
            ProcessDefinition pdef = (ProcessDefinition) graph;
            pdefs.add(WAPIGenerator.createProcessDefinition(pdef));
        }

        ctx.closeContext();
        return pdefs;
    }

    protected void setProcessParameters(JbpmWorkflowExecutionContext ctx,
            ProcessInstance pi, Map<String, Serializable> attrs) {
        if (attrs != null) {
            ContextInstance ctxInstance = pi.getContextInstance();
            for (String key : attrs.keySet()) {
                Object value = attrs.get(key);
                if (key.equals(WorkflowConstants.WORKFLOW_CREATOR)) {
                    try {
                        ctx.getContext().setActorId((String) value);
                    } catch (ClassCastException cce) {
                        log.error("Impossible to get the keys for actorId...");
                    }
                }
                // Set variables on the root token
                ctxInstance.setVariable(key, attrs.get(key));
            }
        }
    }

    public WMActivityInstance startProcess(String wdefId,
            Map<String, Serializable> attrs,
            Map<String, Serializable> startWorkItemAttrs)
            throws WMWorkflowException {

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        // Try to fetch the corresponding process definition
        // Raise a WMWorkflowException if not found
        ProcessDefinition pd = getProcessDefinition(wdefId, graphSession);

        if (pd == null) {
            throw new WMWorkflowException(
                    "Cannot find workflow definition with name=" + wdefId);
        }

        // Create process instance
        ProcessInstance pi = new ProcessInstance(pd);

        // Set process variables if given as a parameter
        setProcessParameters(ctx, pi, attrs);

        // Check if there is a start task and set parameters to it if given
        Token rootToken = pi.getRootToken();
        Task startTask = pi.getTaskMgmtInstance().getTaskMgmtDefinition().getStartTask();
        if (startTask != null) {

            TaskInstance startTaskInstance = pi.getTaskMgmtInstance().createStartTaskInstance();

            if (attrs != null
                    && attrs.containsKey(WorkflowConstants.WORKFLOW_CREATOR)) {

                // Done by an assignment handler dedicated to the start task
                /*
                 * String actorId = (String) processVariables
                 * .get(WorkflowConstants.WORKFLOW_CREATOR); // Assign the start
                 * task to the author startTaskInstance.setActorId(actorId);
                 * assert startTaskInstance.getActorId() == actorId;
                 */

            }

            // For the workflow path instantiation at the end.
            // :XXX: set tasl properties. Need to introspect the task
            // definition
            rootToken = startTaskInstance.getToken();

        }

        // Start workflow
        pi.signal();

        // Save process instance.
        ctx.getContext().save(pi);

        WMActivityInstance activityInstance = WAPIGenerator.createActivityInstance(rootToken);

        ctx.closeContext();

        return activityInstance;
    }

    public WMProcessInstance terminateProcess(String pid)
            throws WMWorkflowException {

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        ProcessInstance processInstance = getProcessInstance(pid, graphSession);
        if (processInstance == null) {
            throw new WMWorkflowException(
                    "Cannot find workflow instance with id=" + pid);
        }

        // Delete the process instance
        // :XXX: Should I keep the instance stalled within the db ?
        // :XXX: Should we keep the tasks ? , the timers ? , the tasks ?
        graphSession.deleteProcessInstance(processInstance, true, true, true);

        WMProcessInstance pi = WAPIGenerator.createProcessInstance(processInstance);

        // We need to setup the status by hand here since the process instance
        // has been deleted and thus we can't resync.
        pi.setState(WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE);

        ctx.closeContext();
        return pi;
    }

    public Collection<WMProcessInstance> getProcessInstancesFor(String pdefId) {
        Collection<WMProcessInstance> pis = new ArrayList<WMProcessInstance>();

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession session = ctx.getGraphSession();

        List processInstances = session.findProcessInstances(IDConverter.getJbpmIdentifier(pdefId));

        for (Object ob : processInstances) {
            ProcessInstance pi = (ProcessInstance) ob;
            pis.add(WAPIGenerator.createProcessInstance(pi));
        }

        ctx.closeContext();
        return pis;
    }

    public Collection<WMProcessInstance> getActiveProcessInstancesFor(
            String workflowDefinitionId) {
        Collection<WMProcessInstance> pis = getProcessInstancesFor(workflowDefinitionId);
        for (WMProcessInstance instance : pis) {
            if (instance.getState().equals(
                    WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE)) {
                pis.remove(instance);
            }
        }
        return pis;
    }

    public WMProcessInstance getProcessInstanceById(String pid)
            throws WMWorkflowException {
        WMProcessInstance pi = null;

        if (pid == null) {
            return pi;
        }

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        try {
            Long jbpmWorkflowInstanceId = IDConverter.getJbpmIdentifier(pid);
            ProcessInstance processInstance = ctx.getGraphSession().getProcessInstance(
                    jbpmWorkflowInstanceId);
            if (processInstance != null) {
                pi = WAPIGenerator.createProcessInstance(processInstance);
            } else {
                log.error("Cannot find process instance with id=" + pid);
            }
        } catch (JbpmException jbpme) {
            throw new WMWorkflowException(jbpme);
        } catch (Exception e) {
            // Possible JDBC exceptions...
            throw new WMWorkflowException(e);
        }
        ctx.closeContext();
        return pi;
    }

    public Collection<WMActivityInstance> getActivityInstancesFor(String pid)
            throws WMWorkflowException {

        Collection<WMActivityInstance> activities = new ArrayList<WMActivityInstance>();

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession session = ctx.getGraphSession();

        ProcessInstance pi;
        try {
            pi = session.getProcessInstance(IDConverter.getJbpmIdentifier(pid));
        } catch (JbpmException jbpme) {
            throw new WMWorkflowException("Cannot find process instance!",
                    jbpme);
        }

        if (pi != null) {
            List objects = pi.findAllTokens();
            for (Object object : objects) {
                Token token = (Token) object;
                activities.add(WAPIGenerator.createActivityInstance(token));
            }
        }

        ctx.closeContext();
        return activities;
    }

    /**
     * warning: if 2 variables from 2 different token have the same key one will
     * be overridden.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Serializable> listProcessInstanceAttributes(String pid) {

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        if(pid == null) {
            return props;
        }
        JbpmWorkflowExecutionContext ctx = getExecutionContext();

        JbpmContext jctx = ctx.getContext();
        Session session = jctx.getSession();
        List<VariableInstance> vis = null;
        try {
            Query query = session.getNamedQuery("Nuxeo.findAllVariablesForPid");
            query.setString("pid", pid);
            vis = query.list();
        } catch (Exception e) {
            log.error(e);
        }
        if(vis == null || vis.isEmpty()) {
            return props;
        }
        for (VariableInstance vi : vis) {
            if (!(vi instanceof ByteArrayInstance)) {// don't load, it would
                // mean 1 requete to the
                // db by isntance.
                props.put(vi.getName(), (Serializable) vi.getValue());
            }
        }
        List<ByteArrayInstance> bais = null;
        try {
            Query query = session.getNamedQuery("Nuxeo.findAllByteArrayForPid");
            query.setString("pid", pid);
            bais = query.list();
        } catch (Exception e) {
            log.error(e);
        }
        for (ByteArrayInstance bai : bais) {
            props.put(bai.getName(), (Serializable) bai.getValue());
        }
        ctx.closeContext();
        return props;
    }

    public Map<String, Serializable> listActivityInstanceAttributes(
            WMActivityInstance activityInstance) {

        Map<String, Serializable> props = new HashMap<String, Serializable>();

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        ProcessInstance pi = null;
        try {
            pi = graphSession.getProcessInstance(IDConverter.getJbpmIdentifier(activityInstance.getProcessInstance().getId()));
        } catch (JbpmException jbpme) {
            log.error("Cannot find workflow instance...", jbpme);
        }

        if (pi != null) {
            ContextInstance ctxInstance = pi.getContextInstance();
            Token token = pi.findToken(activityInstance.getRelativePath());
            Map objectMaps = ctxInstance.getVariables(token);
            for (Object object : objectMaps.keySet()) {
                String key = (String) object;
                Serializable value = (Serializable) objectMaps.get(key);
                props.put(key, value);
            }

        }

        ctx.closeContext();
        return props;
    }

    public WMWorkItemInstance endWorkItem(WMWorkItemInstance workItem,
            String transitionName) throws WMWorkflowException {

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();

        // Get task instance
        ExtendedTaskInstance taskInstance = (ExtendedTaskInstance) taskSession.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));

        if (taskInstance.hasEnded()) {
            return WAPIGenerator.createWorkItemInstance(taskInstance);
        }

        if (taskInstance.isRejected()) {
            taskInstance.setRejected(false);
        }

        // End the task and follow a transition if found
        if (transitionName == null) {
            try {
                taskInstance.end();
            } catch (IllegalStateException jbpme) {
                throw new WMWorkflowException(jbpme);
            }
        } else {
            Node node = taskInstance.getToken().getNode();
            if (node.getLeavingTransition(transitionName) == null) {
                throw new WMWorkflowException("Cannot find transition="
                        + transitionName);
            } else {
                try {
                    taskInstance.end(transitionName);
                } catch (IllegalStateException jbpme) {
                    throw new WMWorkflowException(jbpme);
                }
            }
        }

        // Save the process instance
        ProcessInstance pi = taskInstance.getToken().getProcessInstance();

        ctx.getContext().save(pi);

        WMWorkItemInstance newWorkItem = WAPIGenerator.createWorkItemInstance(taskInstance);

        ctx.closeContext();
        return newWorkItem;
    }

    public WMActivityInstance followTransition(
            WMActivityInstance activityInstance, String transitionName,
            Map<String, Serializable> attrs) throws WMWorkflowException {

        // Get execution context
        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        // Get process instance
        ProcessInstance pi;
        try {
            pi = graphSession.getProcessInstance(IDConverter.getJbpmIdentifier(activityInstance.getProcessInstance().getId()));
        } catch (JbpmException jbpme) {
            throw new WMWorkflowException("Cannot find process instance...!",
                    jbpme);
        }

        if (pi == null) {
            ctx.closeContext();
            return null;
        }

        // Set process variables if given as a parameter
        setProcessParameters(ctx, pi, attrs);

        // Get the corresponding token.
        Token token = pi.findToken(activityInstance.getRelativePath());
        if (token == null) {
            throw new WMWorkflowException("Token not found !");
        }

        // Check for leaving transitions
        if (transitionName == null) {
            token.signal();
        } else {
            Node node = token.getNode();
            if (node.hasNoLeavingTransitions()) {
                throw new WMWorkflowException(
                        "No leaving transitions found for !");
            }
            token.signal(transitionName);
        }

        // Save the process instance state.
        pi = token.getProcessInstance();
        ctx.getContext().save(pi);

        WMActivityInstance newActivityInstance = WAPIGenerator.createActivityInstance(token);
        ctx.closeContext();
        return newActivityInstance;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            WMActivityInstance activityInstance, String state,
            WMParticipant participant) {

        Collection<WMWorkItemInstance> workItems = new ArrayList<WMWorkItemInstance>();

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        // Get process instance
        ProcessInstance pi;
        try {
            pi = graphSession.getProcessInstance(IDConverter.getJbpmIdentifier(activityInstance.getProcessInstance().getId()));
        } catch (JbpmException jbpme) {
            log.error("Cannot find process instance...", jbpme);
            ctx.closeContext();
            return null;
        }

        // Get the corresponding token.
        Token token = pi.findToken(activityInstance.getRelativePath());
        if (token == null) {
            log.error("Cannot find token....");
            ctx.closeContext();
            return null;
        }

        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();
        List objects = taskSession.findTaskInstancesByToken(token.getId());
        for (Object object : objects) {
            TaskInstance taskInstance = (TaskInstance) object;
            if (taskInstance != null) {
                String actorId = taskInstance.getActorId();
                if (actorId != null && actorId.equals(participant.getName())) {
                    if (isStateCandidate(taskInstance, state)) {
                        workItems.add(WAPIGenerator.createWorkItemInstance(taskInstance));
                    }
                }
            }
        }

        ctx.closeContext();
        return workItems;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            WMParticipant participant, String state) {

        Collection<WMWorkItemInstance> workItems = new ArrayList<WMWorkItemInstance>();

        JbpmWorkflowExecutionContext ctx = getExecutionContext();

        JbpmContext jctx = ctx.getContext();
        Session session = jctx.getSession();
        List<Object[]> objects = null;
        try {
            Query query = session.getNamedQuery("TaskMgmtSession.findAllTaskInstancesProcessInstanceByActorId");
            query.setString("actorId", participant.getName());
            objects = query.list();
        } catch (Exception e) {
            log.error(e);
        }
        if (objects != null) {
            for (Object[] object : objects) {
                TaskInstance taskInstance = (TaskInstance) object[0];
                ProcessInstance processInstance = (ProcessInstance) object[1];
                StringInstance creator = (StringInstance) object[2];
                if (isStateCandidate(taskInstance, state)) {
                    workItems.add(WAPIGenerator.createWorkItemInstance(
                            taskInstance, processInstance,
                            (String) creator.getValue()));
                }
            }
        }

        ctx.closeContext();
        return workItems;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            List<WMParticipant> participants, String state) {

        Collection<WMWorkItemInstance> workItems = new ArrayList<WMWorkItemInstance>();
        if (participants == null || participants.isEmpty()) {
            return workItems;
        }
        JbpmWorkflowExecutionContext ctx = getExecutionContext();

        JbpmContext jctx = ctx.getContext();
        Session session = jctx.getSession();
        List<Object[]> objects = null;
        StringBuilder actorIds = new StringBuilder();
        for (WMParticipant participant : participants) {
            actorIds.append("'" + participant.getName() + "',");
        }
        actorIds.deleteCharAt(actorIds.length() - 1);
        String query = "select ti, ti.taskMgmtInstance.processInstance, si "
                + "from org.jbpm.taskmgmt.exe.TaskInstance as ti, "
                + "org.jbpm.context.exe.variableinstance.StringInstance si "
                + "where ti.actorId in ("
                + actorIds
                + ") "
                + "and ti.taskMgmtInstance.processInstance = si.processInstance "
                + "and ti.end is null " + "and si.name = 'author'";
        try {
            objects = session.createQuery(query).list();
        } catch (Exception e) {
            log.error(e);
        }
        if (objects != null) {
            for (Object[] object : objects) {
                TaskInstance taskInstance = (TaskInstance) object[0];
                ProcessInstance processInstance = (ProcessInstance) object[1];
                StringInstance creator = (StringInstance) object[2];
                if (isStateCandidate(taskInstance, state)) {
                    workItems.add(WAPIGenerator.createWorkItemInstance(
                            taskInstance, processInstance,
                            (String) creator.getValue()));
                }
            }
        }

        ctx.closeContext();
        return workItems;
    }

    public Collection<WMWorkItemInstance> getWorkItemsFor(
            String workflowInstanceId, String state, WMParticipant participant) {

        Collection<WMWorkItemInstance> workItems = new ArrayList<WMWorkItemInstance>();
        JbpmWorkflowExecutionContext ctx = getExecutionContext();

        ProcessInstance pi = null;
        long jbpmId = IDConverter.getJbpmIdentifier(workflowInstanceId);
        try {
            pi = ctx.getContext().getProcessInstance(jbpmId);
        } catch (JbpmException jbpme) {
            log.error("Cannot find process instance=" + workflowInstanceId);
        }

        if (pi != null) {
            TaskMgmtSession taskSession = ctx.getTaskMgmtSession();
            List tokenObjects = taskSession.findTaskInstances(participant.getName());
            for (Object object : tokenObjects) {
                TaskInstance taskInstance = (TaskInstance) object;
                if (taskInstance != null) {
                    if (isStateCandidate(taskInstance, state)) {
                        // :TODO: we should have another way to do this than
                        // post filtering
                        if (taskInstance.getToken().getProcessInstance().getId() == jbpmId) {
                            workItems.add(WAPIGenerator.createWorkItemInstance(taskInstance));
                        }
                    }

                }
            }
        }

        ctx.closeContext();
        return workItems;
    }

    public Collection<WMWorkItemInstance> listWorkItems(String pid, String state) {

        final Collection<WMWorkItemInstance> workItems = new ArrayList<WMWorkItemInstance>();

        JbpmWorkflowExecutionContext ctx = getExecutionContext();

        ProcessInstance pi = null;
        try {
            pi = ctx.getContext().getProcessInstance(
                    IDConverter.getJbpmIdentifier(pid));
        } catch (JbpmException jbpme) {
            log.error("Cannot find process instance=" + pid);
        }

        if (pi != null) {
            // Find all tokens
            List objects = pi.findAllTokens();
            Collection<Token> tokens = new ArrayList<Token>();
            for (Object object : objects) {
                Token token = (Token) object;
                if (WMWorkItemState.getActiveStates().contains(state)) {
                    if (!token.hasEnded()) {
                        tokens.add(token);
                    }
                } else {
                    tokens.add(token);
                }
            }

            List<Long> found = new ArrayList<Long>();
            for (Token token : tokens) {

                ExecutionContext eCtx = new ExecutionContext(token);
                TaskMgmtInstance tmi = eCtx.getTaskMgmtInstance();

                Collection taskObjects = tmi.getTaskInstances();
                for (Object object : taskObjects) {
                    TaskInstance taskInstance = (TaskInstance) object;
                    if (taskInstance != null
                            && !found.contains(taskInstance.getId())) {
                        // Task match request
                        if (isStateCandidate(taskInstance, state)) {
                            workItems.add(WAPIGenerator.createWorkItemInstance(taskInstance));
                            found.add(taskInstance.getId());

                        }
                    }
                }
            }
        }

        ctx.closeContext();
        return workItems;
    }

    public WMWorkItemInstance getWorkItemById(String workItemId) {
        WMWorkItemInstance workItem = null;

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();

        try {
            TaskInstance jbpmTaskInstance = taskSession.getTaskInstance(IDConverter.getJbpmIdentifier(workItemId));
            if (jbpmTaskInstance != null) {
                workItem = WAPIGenerator.createWorkItemInstance(jbpmTaskInstance);
            } else {
                log.error("Task with identifier=" + workItemId
                        + " is not found ...");
            }
        } catch (JbpmException jbpme) {
            log.error("An error occured trying to find task with identifier="
                    + workItemId);
        }

        ctx.closeContext();
        return workItem;
    }

    public WMWorkItemInstance startWorkItem(WMWorkItemInstance workItem) {

        // :XXX: factor out.

        WMWorkItemInstance newWorkItem = null;

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();

        try {
            TaskInstance jbpmTaskInstance = taskSession.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));
            if (jbpmTaskInstance != null) {
                jbpmTaskInstance.start();
                if (jbpmTaskInstance.getStart() == null) {
                    ProcessInstance pi = jbpmTaskInstance.getToken().getProcessInstance();
                    ctx.getContext().save(pi);
                }
                newWorkItem = WAPIGenerator.createWorkItemInstance(jbpmTaskInstance);

            } else {
                log.error("Task with identifier=" + workItem.getId()
                        + " is not found ...");
            }
        } catch (JbpmException jbpme) {
            log.error("An error occured trying to find task with identifier="
                    + workItem.getId());
        }
        ctx.closeContext();
        return newWorkItem;
    }

    public WMWorkItemInstance suspendWorkItem(WMWorkItemInstance workItem) {

        // :XXX: factor out.

        WMWorkItemInstance newWorkItem = null;

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();

        try {
            TaskInstance jbpmTaskInstance = taskSession.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));
            if (jbpmTaskInstance != null) {
                jbpmTaskInstance.suspend();
                if (jbpmTaskInstance.getStart() != null
                        && !jbpmTaskInstance.hasEnded()) {
                    ProcessInstance pi = jbpmTaskInstance.getToken().getProcessInstance();
                    ctx.getContext().save(pi);
                }
                newWorkItem = WAPIGenerator.createWorkItemInstance(jbpmTaskInstance);

            } else {
                log.error("Task with identifier=" + workItem.getId()
                        + " is not found ...");
            }
        } catch (JbpmException jbpme) {
            log.error("An error occured trying to find task with identifier="
                    + workItem.getId());
        }
        ctx.closeContext();
        return newWorkItem;
    }

    public WMWorkItemInstance updateWorkItem(Map<String, Serializable> props,
            WMWorkItemInstance workItem) throws WMWorkflowException {

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();

        ExtendedTaskInstance eti = null;
        try {
            eti = (ExtendedTaskInstance) taskSession.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));
            if (props != null) {

                if (props.keySet().contains(
                        WorkflowConstants.WORKFLOW_TASK_PROP_DIRECTIVE)) {
                    eti.setDirective((String) props.get(WorkflowConstants.WORKFLOW_TASK_PROP_DIRECTIVE));
                }

                if (props.keySet().contains(
                        WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE)) {
                    eti.setDueDate((Date) props.get(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE));
                }

                if (props.keySet().contains(
                        WorkflowConstants.WORKFLOW_TASK_PROP_COMMENT)) {
                    eti.setComment((String) props.get(WorkflowConstants.WORKFLOW_TASK_PROP_COMMENT));
                }

                if (props.keySet().contains(
                        WorkflowConstants.WORKFLOW_TASK_PROP_ORDER)) {
                    eti.setOrder((Integer) props.get(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER));
                }
                if (props.keySet().contains(
                        WorkflowConstants.WORKFLOW_TASK_ASSIGNEE)) {
                    eti.setActorId((String) props.get(WorkflowConstants.WORKFLOW_TASK_ASSIGNEE));
                }
                if (props.keySet().contains(
                        WorkflowConstants.WORKFLOW_TASK_PROP_REJECTED)) {
                    eti.setRejected((Boolean) props.get(WorkflowConstants.WORKFLOW_TASK_PROP_REJECTED));
                }
            }
            if (eti != null) {
                workItem = WAPIGenerator.createWorkItemInstance(eti);
            } else {
                log.error("Task with identifier=" + workItem.getId()
                        + " is not found ...");
            }
        } catch (JbpmException jbpme) {
            log.error("An error occured trying to find task with identifier="
                    + workItem.getId());
        }

        // Save back the task
        ProcessInstance pi = eti.getToken().getProcessInstance();
        ctx.getContext().save(pi);

        ctx.closeContext();

        return WAPIGenerator.createWorkItemInstance(eti);
    }

    @SuppressWarnings("unchecked")
    public void assignWorkItem(WMWorkItemInstance workItem,
            WMParticipant participant) {

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();

        TaskInstance taskInstance = null;
        try {
            taskInstance = taskSession.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));
        } catch (JbpmException jbpme) {
            log.error("Cannot find task instance with id=" + workItem.getId());
        }

        ExecutionContext eCtx = new ExecutionContext(taskInstance.getToken());
        eCtx.setVariable(WorkflowConstants.WORKFLOW_TASK_ASSIGNEE,
                participant.getName());
        taskInstance.assign(eCtx);
        // taskInstance.setActorId(participant.getName());

        // Save changes
        ProcessInstance pi = taskInstance.getToken().getProcessInstance();
        ctx.getContext().save(pi);

        ctx.closeContext();

    }

    @SuppressWarnings("unchecked")
    public void unAssignWorkItem(WMWorkItemInstance workItem,
            WMParticipant participant) {

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession taskSession = ctx.getTaskMgmtSession();

        TaskInstance taskInstance = null;
        try {
            taskInstance = taskSession.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));
        } catch (JbpmException jbpme) {
            log.error("Cannot find task instance with id=" + workItem.getId());
        }

        taskInstance.setActorId(null);

        // Save changes
        ProcessInstance pi = taskInstance.getToken().getProcessInstance();
        ctx.getContext().save(pi);

        ctx.closeContext();

    }

    /**
     * Returns the status of the jdpl check.
     *
     * @param inputStream the jdpl
     * @return the status if the jpdl check
     */
    private String[] getJpdlStatus(InputStream inputStream) {
        NXJpdlXmlReader reader = new NXJpdlXmlReader(inputStream);
        // :XXX: return (String[]) reader.getStatus().toArray();
        String[] status = new String[reader.getStatus().size()];
        int i = 0;
        for (Object message : reader.getStatus()) {
            status[i] = message.toString();
            i++;
        }
        return status;
    }

    /**
     * Given a workflow definition id, returns its corresponding process
     * definition.
     *
     * @param workflowDefinitionId a NXWorkflow definition id
     * @param session a jbpm graph session
     * @return a jBPM ProcessDefinition instance
     */
    private ProcessDefinition getProcessDefinition(String workflowDefinitionId,
            GraphSession session) {
        return session.getProcessDefinition(IDConverter.getJbpmIdentifier(workflowDefinitionId));
    }

    /**
     * Given a workflow instance id, return its corresponding jBPM process
     * instance.
     *
     * @param workflowInstanceID a NXWorkflow workflow instansce ID
     * @param session a jBPM graph sesssion
     * @return a jBPM ProcessInstance instance
     */
    private ProcessInstance getProcessInstance(String workflowInstanceID,
            GraphSession session) {
        return session.getProcessInstance(IDConverter.getJbpmIdentifier(workflowInstanceID));
    }

    /**
     * TODO.
     *
     * @return
     */
    private JbpmWorkflowExecutionContext getExecutionContext() {
        return new JbpmWorkflowExecutionContext();
    }

    public WMProcessDefinition getProcessDefinitionByName(String name) {
        WMProcessDefinition wdef = null;

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        ProcessDefinition pdef = graphSession.findLatestProcessDefinition(name);

        if (pdef != null) {
            wdef = WAPIGenerator.createProcessDefinition(pdef);
        }

        ctx.closeContext();
        return wdef;
    }

    public WMWorkItemInstance createWorkItem(
            WMActivityInstance activityInstance,
            WMWorkItemDefinition workItemDefinition,
            Map<String, Serializable> attrs) throws WMWorkflowException {

        WMWorkItemInstance workItem;

        // Task aware activityDefinition ?
        if (!activityInstance.getActivityDefinition().isTaskAwareActivity()) {
            throw new WMWorkflowException("Path does not support task !");
        }

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        ProcessInstance pi;
        try {
            pi = graphSession.getProcessInstance(IDConverter.getJbpmIdentifier(activityInstance.getProcessInstance().getId()));
        } catch (JbpmException jbpme) {
            log.error("Cannot find workflow instance.", jbpme);
            throw new WMWorkflowException(jbpme);
        }

        if (pi == null) {
            throw new WMWorkflowException(
                    "Process instance does not exist anymore");
        }

        Token token = pi.findToken(activityInstance.getRelativePath());
        if (token == null) {
            throw new WMWorkflowException("Path does not exist anymore");
        }

        ExecutionContext eCtx = new ExecutionContext(token);
        TaskMgmtInstance tmi = eCtx.getTaskMgmtInstance();
        TaskNode taskNode = (TaskNode) ctx.getContext().getSession().load(
                TaskNode.class, token.getNode().getId());

        Task task = taskNode.getTask(workItemDefinition.getName());
        if (task == null) {
            log.error("Task definition does not exist : "
                    + workItemDefinition.getName());
            throw new WMWorkflowException("Task definition does not exist");
        }

        ExtendedTaskInstance eti;
        try {
            eti = (ExtendedTaskInstance) tmi.createTaskInstance(task, token);
        } catch (JbpmException je) {
            throw new WMWorkflowException(je);
        }

        if (eti == null) {
            throw new WMWorkflowException(
                    "An error occured trying to create a task...");
        }

        // Set properties.
        if (attrs != null) {

            if (attrs.keySet().contains(
                    WorkflowConstants.WORKFLOW_TASK_PROP_DIRECTIVE)) {
                eti.setDirective((String) attrs.get(WorkflowConstants.WORKFLOW_TASK_PROP_DIRECTIVE));
            }

            if (attrs.keySet().contains(
                    WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE)) {
                eti.setDueDate((Date) attrs.get(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE));
            }

            if (attrs.keySet().contains(
                    WorkflowConstants.WORKFLOW_TASK_PROP_COMMENT)) {
                eti.setComment((String) attrs.get(WorkflowConstants.WORKFLOW_TASK_PROP_COMMENT));
            }

            if (attrs.keySet().contains(
                    WorkflowConstants.WORKFLOW_TASK_PROP_ORDER)) {
                eti.setOrder((Integer) attrs.get(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER));
            }
            if (attrs.keySet().contains(
                    WorkflowConstants.WORKFLOW_TASK_ASSIGNEE)) {
                eti.setActorId((String) attrs.get(WorkflowConstants.WORKFLOW_TASK_ASSIGNEE));
            }

        }

        ctx.getContext().save(pi);

        workItem = WAPIGenerator.createWorkItemInstance(eti);

        ctx.closeContext();
        return workItem;
    }

    public Set<WMWorkItemDefinition> getWorkItemDefinitionsFor(
            WMActivityInstance activityInstance) {
        Set<WMWorkItemDefinition> workItemDefinitions = new HashSet<WMWorkItemDefinition>();

        // Task aware activityDefinition ?
        if (!activityInstance.getActivityDefinition().isTaskAwareActivity()) {
            return workItemDefinitions;
        }

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        GraphSession graphSession = ctx.getGraphSession();

        ProcessInstance pi = null;
        try {
            pi = graphSession.getProcessInstance(IDConverter.getJbpmIdentifier(activityInstance.getProcessInstance().getId()));
        } catch (JbpmException jbpme) {
            log.debug("Cannot find workflow instance.", jbpme);
        }

        if (pi == null) {
            ctx.closeContext();
            return workItemDefinitions;
        }

        Token token = pi.findToken(activityInstance.getRelativePath());
        if (token != null) {
            // http://www.mail-archive.com/jboss-user@lists.sourceforge.net/msg107722.html
            TaskNode taskNode = (TaskNode) ctx.getContext().getSession().load(
                    TaskNode.class, token.getNode().getId());
            for (Object taskOb : taskNode.getTasks()) {
                Task task = (Task) taskOb;
                WMWorkItemDefinition workItemDefinition = WAPIGenerator.createWorkItemDefinition(task);
                workItemDefinitions.add(workItemDefinition);
            }
        } else {
            log.debug("Path does not exist anymore.");
        }

        ctx.closeContext();
        return workItemDefinitions;
    }

    public void removeWorkItem(WMWorkItemInstance workItem) {

        if (workItem == null) {
            log.debug("Work item is null. ");
            return;
        }

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession tms = ctx.getTaskMgmtSession();

        ExtendedTaskInstance jbpmTask = (ExtendedTaskInstance) tms.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));

        if (jbpmTask == null) {
            log.debug("Cannot find jbpm task.");
            ctx.closeContext();
            return;
        }

        ProcessInstance pi = jbpmTask.getToken().getProcessInstance();

        if (jbpmTask.isBlocking()) {
            jbpmTask.setBlocking(false);
        }

        if (jbpmTask.isSignalling()) {
            jbpmTask.setSignalling(false);
        }

        if (!jbpmTask.isCancelled() && !jbpmTask.hasEnded()) {
            jbpmTask.cancel();
        }

        ctx.getContext().save(pi);
        ctx.closeContext();

    }

    protected boolean isStateCandidate(TaskInstance taskInstance,
            String taskState) {
        boolean candidate = false;
        ExtendedTaskInstance eTaskInstance = (ExtendedTaskInstance) taskInstance;

        if (taskState == null
                || taskState.equals(WMWorkItemState.WORKFLOW_TASK_STATE_ALL)) {
            candidate = true;
            return candidate;
        }

        if (eTaskInstance.getStart() == null) {
            // Task is not started
            if (taskState.equals(WMWorkItemState.WORKFLOW_TASK_STATE_CREATED)
                    && !eTaskInstance.hasEnded() && !eTaskInstance.isRejected()
                    && !eTaskInstance.isCancelled()) {
                candidate = true;
            }
        } else {
            // Task has been started
            if (eTaskInstance.hasEnded()) {
                if (!eTaskInstance.isCancelled()
                        && taskState.equals(WMWorkItemState.WORKFLOW_TASK_STATE_CLOSED)) {
                    // Task closed request
                    candidate = true;
                }
            } else {
                if (taskState.equals(WMWorkItemState.WORKFLOW_TASK_STATE_STARTED)
                        && !eTaskInstance.isCancelled()
                        && !eTaskInstance.isRejected()
                        && !eTaskInstance.isSuspended()) {
                    // Task is started request
                    candidate = true;
                } else if (taskState.equals(WMWorkItemState.WORKFLOW_TASK_STATE_SUSPENDED)) {
                    if (eTaskInstance.isSuspended()) {
                        candidate = true;
                    }
                } else if (taskState.equals(WMWorkItemState.WORKFLOW_TASK_STATE_REJECTED)) {
                    if (eTaskInstance.isRejected()) {
                        candidate = true;
                    }
                }
            }
        }
        return candidate;
    }

    public void rejectWorkItem(WMWorkItemInstance workItem) {

        if (workItem == null) {
            return;
        }

        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        TaskMgmtSession tms = ctx.getTaskMgmtSession();

        ExtendedTaskInstance jbpmTask = (ExtendedTaskInstance) tms.getTaskInstance(IDConverter.getJbpmIdentifier(workItem.getId()));

        if (jbpmTask == null) {
            return;
        }

        ProcessInstance pi = jbpmTask.getToken().getProcessInstance();
        jbpmTask.setRejected(true);

        ctx.getContext().save(pi);
        ctx.closeContext();

    }

    public void updateProcessInstanceAttributes(String pid,
            Map<String, Serializable> attrs) throws WMWorkflowException {
        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        try {
            Long jbpmWorkflowInstanceId = IDConverter.getJbpmIdentifier(pid);
            ProcessInstance processInstance = ctx.getGraphSession().getProcessInstance(
                    jbpmWorkflowInstanceId);
            if (processInstance != null && attrs != null) {
                for (String k : attrs.keySet()) {
                    ContextInstance ci = processInstance.getContextInstance();
                    ci.setVariable(k, attrs.get(k));
                }
                ctx.getContext().save(processInstance);
            } else {
                log.error("Cannot find process instance with id=" + pid);
            }
        } catch (JbpmException jbpme) {
            throw new WMWorkflowException(jbpme);
        } catch (Exception e) {
            // Possible JDBC exceptions...
            throw new WMWorkflowException(e);
        }
        ctx.closeContext();
    }

    public WMProcessInstanceIterator listProcessInstances(WMFilter filter)
            throws WMWorkflowException {

        // XXX implement filters

        JbpmWorkflowExecutionContext ctx = getExecutionContext();

        // XXX no direct API in Jbpm for this ?
        List<WMProcessInstance> wprocs = new ArrayList<WMProcessInstance>();
        try {

            List defs = ctx.getGraphSession().findAllProcessDefinitions();

            for (Object pd : defs) {
                List procs = ctx.getGraphSession().findProcessInstances(
                        ((ProcessDefinition) pd).getId());
                for (Object proc : procs) {
                    wprocs.add(WAPIGenerator.createProcessInstance((ProcessInstance) proc));
                }
            }

            if (filter != null) {
                wprocs = filterProcessInstances(wprocs, filter);
            }

        } catch (Exception e) {
            throw new WMWorkflowException(e);
        } finally {
            ctx.closeContext();
        }

        return new WMProcessInstanceIteratorImpl(wprocs);
    }

    public WMWorkItemIterator listWorkItems(WMFilter filter)
            throws WMWorkflowException {
        JbpmWorkflowExecutionContext ctx = getExecutionContext();

        // XXX implement filter

        Collection<WMWorkItemInstance> wiis = new ArrayList<WMWorkItemInstance>();
        try {
            WMProcessInstanceIterator it = listProcessInstances(filter);
            while (it.hasNext()) {
                WMProcessInstance proc = it.next();
                // XXX filter on item states
                Collection<WMWorkItemInstance> items = listWorkItems(
                        proc.getId(), null);
                if (filter == null) {
                    wiis.addAll(items);
                } else {
                    wiis.addAll(filterWorkItems(
                            (List<WMWorkItemInstance>) items, filter));
                }
            }
        } finally {
            ctx.closeContext();
        }
        return new WMWorkItemIteratorImpl((List<WMWorkItemInstance>) wiis);
    }

    protected List<WMWorkItemInstance> filterWorkItems(
            List<WMWorkItemInstance> items, WMFilter filter) {

        // FIXME only partial implemtation. Needs to rethink this.

        final List<WMWorkItemInstance> filtered = new ArrayList<WMWorkItemInstance>();

        for (WMWorkItemInstance item : items) {
            if (filter.getAttributeName().equals(
                    WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE)) {
                if (filter.getFilterValue() instanceof Date) {
                    Date date = (Date) filter.getFilterValue();
                    Date dueDate = item.getDueDate();
                    if (dueDate != null) {
                        int cmp = date.compareTo(item.getDueDate());
                        int fcmp = filter.getComparison();
                        if ((WMFilter.EQ == fcmp && cmp == 0)
                                || (WMFilter.GE == fcmp && cmp <= 0)
                                || (WMFilter.GT == fcmp && cmp < 0)
                                || (WMFilter.LE == fcmp && cmp >= 0)
                                || (WMFilter.LT == fcmp && cmp > 0)
                                || (WMFilter.NE == fcmp && cmp != 0)) {
                            filtered.add(item);
                        }
                    }
                } else {
                    log.warn("Date is expected by WMFilter on prop="
                            + WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE);
                }
            } else {
                // TODO other attributes
                log.debug("Didn't match any filter...");
                filtered.add(item);
            }
        }

        return filtered;
    }

    protected List<WMProcessInstance> filterProcessInstances(
            List<WMProcessInstance> procs, WMFilter filter) {

        // FIXME only partial implemtation. Needs to rethink this.
        final List<WMProcessInstance> filtered = new ArrayList<WMProcessInstance>();

        for (WMProcessInstance pi : procs) {
            if (filter.getAttributeName().equals(
                    WorkflowConstants.WORKFLOW_CREATOR)) {
                String fcreator = (String) filter.getFilterValue();
                int fcomp = filter.getComparison();
                String authorName = pi.getAuthorName();
                if ((WMFilter.EQ == fcomp && fcreator.equals(authorName))
                        || (WMFilter.NE == fcomp && !fcreator.equals(authorName))) {
                    filtered.add(pi);
                } else {
                    log.debug("Discard pi with name=" + pi.getId());
                }
            } else {
                // XXX deal with others.
                log.debug("Didn't match any filter...");
                filtered.add(pi);
            }
        }

        return filtered;

    }

    @SuppressWarnings("unchecked")
    public Collection<WMProcessInstance> listProcessInstanceForCreators(
            List<String> groupNames) {
        List<WMProcessInstance> processes = new ArrayList<WMProcessInstance>();
        if (groupNames == null || groupNames.isEmpty()) {
            return processes;
        }
        JbpmWorkflowExecutionContext ctx = getExecutionContext();
        Session session = ctx.getContext().getSession();
        StringBuilder values = new StringBuilder();
        for (String group : groupNames) {
            values.append("'" + group + "',");
        }
        values.deleteCharAt(values.length() - 1);
        String query = "select si.processInstance, si.value "
                + "from org.jbpm.context.exe.variableinstance.StringInstance si "
                + "where si.name = '" + WorkflowConstants.WORKFLOW_CREATOR
                + "' " + "and si.value in (" + values + ") "
                + "and si.processInstance.end is null ";
        List<Object[]> list = session.createQuery(query).list();
        for (Object[] objects : list) {
            processes.add(WAPIGenerator.createProcessInstance(
                    (ProcessInstance) objects[0], (String) objects[1]));
        }
        ctx.closeContext();
        return processes;
    }
}
