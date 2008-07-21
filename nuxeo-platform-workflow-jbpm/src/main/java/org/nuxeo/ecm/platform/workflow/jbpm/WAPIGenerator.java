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
 * $Id: WAPIGenerator.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import java.util.List;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.security.permission.CreateProcessInstancePermission;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMTransitionDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMActivityDefinitionImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMActivityInstanceImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMProcessDefinitionImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMProcessInstanceImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMTransitionImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMWorkItemDefinitionImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMWorkItemInstanceImpl;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.jbpm.tasks.ExtendedTaskInstance;
import org.nuxeo.ecm.platform.workflow.jbpm.util.IDConverter;

/**
 * WAPI instances generator.
 *
 * @see org.nuxeo.ecm.platform.workflow.api.client.wfmc
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WAPIGenerator {

    // Utility class.
    private WAPIGenerator() {}

    public static WMParticipant createWorkflowPrincipal(String actorId) {
        return new WMParticipantImpl(actorId);
    }

    public static WMProcessDefinition createProcessDefinition(
            ProcessDefinition definition) {

        WMProcessDefinition pdef;

        if (definition != null) {
            pdef = new WMProcessDefinitionImpl(
                    IDConverter.getNXWorkflowIdentifier(definition.getId()),
                    definition.getVersion(), definition.getName());
        } else {
            pdef = null;
        }

        return pdef;
    }

    public static WMProcessInstance createProcessInstance(
            ProcessInstance instance, String creator) {

        WMProcessInstance pi;

        if (instance != null) {
            String status;
            if (instance.hasEnded()) {
                status = WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE;
            } else {
                status = WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE;
            }

            pi = new WMProcessInstanceImpl(
                    IDConverter.getNXWorkflowIdentifier(instance.getId()),
                    createProcessDefinition(instance.getProcessDefinition()),
                    status,
                    instance.getStart(),
                    instance.getEnd(),
                    createWorkflowPrincipal(creator));
        } else {
            pi = null;
        }

        return pi;

    }

    /**
     * @param instance
     * @return
     */
    @Deprecated
    public static WMProcessInstance createProcessInstance(
            ProcessInstance instance) {

        WMProcessInstance pi;

        if (instance != null) {
            String status;
            if (instance.hasEnded()) {
                status = WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE;
            } else {
                status = WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE;
            }

            pi = new WMProcessInstanceImpl(
                    IDConverter.getNXWorkflowIdentifier(instance.getId()),
                    createProcessDefinition(instance.getProcessDefinition()),
                    status,
                    instance.getStart(),
                    instance.getEnd(),
                    createWorkflowPrincipal((String) instance.getContextInstance().getVariable(
                            WorkflowConstants.WORKFLOW_CREATOR)));
        } else {
            pi = null;
        }

        return pi;
    }
    public static WMTransitionDefinition createTransitionDefinition(
            Transition transition) {

        WMTransitionDefinition tx;

        if (transition != null) {
            tx = new WMTransitionImpl(
                    IDConverter.getNXWorkflowIdentifier(transition.getId()),
                    transition.getName(), transition.getName(),
                    transition.getFrom().getDefaultLeavingTransition().equals(
                            transition),
                    createActivityDefinition(transition.getFrom()),
                    createActivityDefinition(transition.getTo()));
        } else {
            tx = null;
        }

        return tx;
    }

    public static WMActivityDefinition createActivityDefinition(Node node) {

        WMActivityDefinition activityDefinition;

        if (node != null) {
            String nodeType = node.getClass().getSimpleName();
            boolean taskAwareNode = nodeType.equals(WorkflowConstants.TASK_NODE_TYPE_ID);

            // :FIXME: not always reliable
            taskAwareNode = true;

            // Generate WMTransitionDefinition list
            List nodeTransitions = node.getLeavingTransitions();
            WMTransitionDefinition[] transitions = new WMTransitionDefinition[nodeTransitions.size()];
            int i = 0;
            for (Object object : nodeTransitions) {
                Transition transition = (Transition) object;
                if (transition.getFrom().getId() == node.getId()) {
                    // Case to avoid recursion.
                    // :FIXME: fix from and to activities in this case.
                    transitions[i] = new WMTransitionImpl(
                            IDConverter.getNXWorkflowIdentifier(transition.getId()),
                            transition.getName(),
                            transition.getName(),
                            transition.getFrom().getDefaultLeavingTransition().equals(
                                    transition), null, null);

                } else {
                    transitions[i] = createTransitionDefinition(transition);
                }
                i++;
            }

            activityDefinition = new WMActivityDefinitionImpl(node.getName(),
                    node.getFullyQualifiedName(), transitions, taskAwareNode,
                    nodeType);
        } else {
            activityDefinition = null;
        }
        return activityDefinition;
    }

    public static WMActivityInstance createActivityInstance(Token token) {

        WMActivityInstance ai;

        if (token != null) {

            ai = new WMActivityInstanceImpl(
                    IDConverter.getNXWorkflowIdentifier(token.getId()),
                    token.getFullName(),
                    createActivityDefinition(token.getNode()),
                    createProcessInstance(token.getProcessInstance()));
        } else {
            ai = null;
        }

        return ai;
    }

    public static WMWorkItemDefinition createWorkItemDefinition(Task task) {

        WMWorkItemDefinition widef;

        if (task != null) {
            widef = new WMWorkItemDefinitionImpl(
                    IDConverter.getNXWorkflowIdentifier(task.getId()),
                    // :XXX: inject type
                    createActivityDefinition(task.getTaskNode()), null,
                    task.getName());
        } else {
            widef = null;
        }

        return widef;
    }

    public static WMWorkItemInstance createWorkItemInstance(
            TaskInstance taskInstance, ProcessInstance processInstance, String creator) {

        WMWorkItemInstance wii;

        if (taskInstance != null) {
            ExtendedTaskInstance eTaskInstance = (ExtendedTaskInstance) taskInstance;
            wii = new WMWorkItemInstanceImpl(
                    IDConverter.getNXWorkflowIdentifier(eTaskInstance.getId()),
                    eTaskInstance.getName(),
                    eTaskInstance.getDescription(),
                    createWorkItemDefinition(eTaskInstance.getTask()),
                    createWorkflowPrincipal(eTaskInstance.getActorId()),
                    createProcessInstance(processInstance, creator),
                    eTaskInstance.getStart(), eTaskInstance.getEnd(),
                    eTaskInstance.getDueDate(), eTaskInstance.getDirective(),
                    eTaskInstance.isCancelled(), eTaskInstance.getComment(),
                    eTaskInstance.getOrder(), eTaskInstance.isRejected(),
                    eTaskInstance.getCreate());
        } else {
            wii = null;
        }

        return wii;
    }

    /**
     * Use {@link #createWorkItemInstance(TaskInstance, ProcessInstance, String)} instead.
     * @param taskInstance
     * @return
     */
    @Deprecated
    public static WMWorkItemInstance createWorkItemInstance(
            TaskInstance taskInstance) {
        return createWorkItemInstance(taskInstance, taskInstance.getTaskMgmtInstance().getProcessInstance(), (String) taskInstance.getTaskMgmtInstance().getProcessInstance().getContextInstance().getVariable(
                WorkflowConstants.WORKFLOW_CREATOR));
    }

}
