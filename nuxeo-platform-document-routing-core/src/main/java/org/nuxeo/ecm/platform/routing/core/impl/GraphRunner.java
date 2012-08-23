/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.State;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Runs the proper nodes depending on the graph state.
 *
 * @since 5.6
 */
public class GraphRunner extends AbstractRunner implements ElementRunner {

    private static final Log log = LogFactory.getLog(GraphRunner.class);

    /**
     * Maximum number of steps we do before deciding that this graph is looping.
     */
    public static final int MAX_LOOPS = 100;

    @Override
    public void run(CoreSession session, DocumentRouteElement element,
            Map<String, Serializable> map) {
        try {
            GraphRoute graph = (GraphRoute) element;
            element.setRunning(session);
            if (map != null) {
                graph.setVariables(map);
            }
            boolean done = runGraph(session, graph, graph.getStartNode());
            if (done) {
                element.setDone(session);
            }
            session.save();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        run(session, element, null);
    }

    @Override
    public void resume(CoreSession session, DocumentRouteElement element,
            String nodeId, String taskId, Map<String, Object> varData,
            String status) {
        try {
            GraphRoute graph = (GraphRoute) element;
            Task task = null;
            if (taskId == null) {
                if (nodeId == null) {
                    throw new DocumentRouteException(
                            "nodeId and taskId both missing");
                }
            } else {
                DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
                task = taskDoc.getAdapter(Task.class);
                if (task == null) {
                    throw new DocumentRouteException("Invalid taskId: " + taskId);
                }
                if (nodeId == null) {
                    nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
                    if (StringUtils.isEmpty(nodeId)) {
                        throw new DocumentRouteException(
                                "No nodeId found on task: " + taskId);
                    }
                }
            }
            GraphNode node = graph.getNode(nodeId);
            if (node == null) {
                throw new DocumentRouteException("Invalid nodeId: " + nodeId);
            }
            if (node.getState() != State.SUSPENDED) {
                throw new DocumentRouteException(
                        "Cannot resume on non-suspended node: " + node);
            }

            node.setAllVariables(varData);
            if (StringUtils.isNotEmpty(status)) {
                node.setButton(status);
            }
            if (task != null) {
                finishTask(session, graph, node, task);
            }
            boolean done = runGraph(session, graph, node);
            if (done) {
                element.setDone(session);
            }
            session.save();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    /**
     * Runs the graph starting with the given node.
     *
     * @param graph the graph
     * @param initialNode the initial node to run
     * @return {@code true} if the graph execution is done, {@code false} if
     *         there are still suspended nodes
     */
    protected boolean runGraph(CoreSession session, GraphRoute graph,
            GraphNode initialNode) throws DocumentRouteException {
        LinkedList<GraphNode> pendingNodes = new LinkedList<GraphNode>();
        pendingNodes.add(initialNode);
        boolean done = false;
        int count = 0;
        while (!pendingNodes.isEmpty()) {
            GraphNode node = pendingNodes.pop();
            count++;
            if (count > MAX_LOOPS) {
                throw new DocumentRouteException("Execution is looping, node: "
                        + node);
            }
            State jump = null;
            switch (node.getState()) {
            case READY:
                log.debug("Doing node " + node);
                if (node.isMerge()) {
                    jump = State.WAITING;
                } else {
                    jump = State.RUNNING_INPUT;
                }
                break;
            case WAITING:
                if (node.canMerge()) {
                    recursiveCancelInput(graph, node, pendingNodes);
                    jump = State.RUNNING_INPUT;
                }
                // else leave state to WAITING
                break;
            case RUNNING_INPUT:
                node.starting();
                node.executeChain(node.getInputChain());
                if (node.hasTask()) {
                    createTask(session, graph, node); // may create several
                    node.setState(State.SUSPENDED);
                    // next node
                } else {
                    jump = State.RUNNING_OUTPUT;
                }
                break;
            case SUSPENDED:
                if (node != initialNode) {
                    throw new DocumentRouteException(
                            "Executing unexpected SUSPENDED state");
                }
                // actor
                NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
                String actor = principal.getOriginatingUser();
                if (actor == null) {
                    actor = principal.getName();
                }
                node.setLastActor(actor);
                // resuming, variables have been set by resumeGraph
                jump = State.RUNNING_OUTPUT;
                break;
            case RUNNING_OUTPUT:
                node.executeChain(node.getOutputChain());
                List<Transition> trueTrans = node.evaluateTransitions();
                node.ending();
                node.setState(State.READY);
                if (node.isStop()) {
                    if (!pendingNodes.isEmpty()) {
                        throw new DocumentRouteException(
                                String.format(
                                        "Route %s stopped with still pending nodes: %s",
                                        graph, pendingNodes));
                    }
                    done = true;
                } else {
                    if (trueTrans.isEmpty()) {
                        throw new DocumentRouteException(
                                "No transition evaluated to true from node "
                                        + node);
                    }
                    for (Transition t : trueTrans) {
                        node.executeTransitionChain(t);
                        GraphNode target = graph.getNode(t.target);
                        if (!pendingNodes.contains(target)) {
                            pendingNodes.add(target);
                        }
                    }
                }
                break;
            }
            if (jump != null) {
                node.setState(jump);
                // loop again on this node
                count--;
                pendingNodes.addFirst(node);
            }
        }
        return done;
    }

    protected void recursiveCancelInput(GraphRoute graph,
            GraphNode originalNode, LinkedList<GraphNode> pendingNodes) {
        LinkedList<GraphNode> todo = new LinkedList<GraphNode>();
        todo.add(originalNode);
        Set<String> done = new HashSet<String>();
        while (!todo.isEmpty()) {
            GraphNode node = todo.pop();
            done.add(node.getId());
            for (Transition t : node.getInputTransitions()) {
                if (t.loop) {
                    // don't recurse through loop transitions
                    continue;
                }
                GraphNode source = t.source;
                if (done.contains(source.getId())) {
                    // looping somewhere TODO check it's not happening
                    continue;
                }
                source.setCanceled();
                State state = source.getState();
                source.setState(State.READY);
                pendingNodes.remove(node);
                if (state == State.SUSPENDED) {
                    // we're suspended on a task, cancel it and stop recursion
                    source.cancelTasks();
                } else {
                    // else recurse
                    todo.add(source);
                }
            }
        }
    }

    protected void createTask(CoreSession session, GraphRoute graph,
            GraphNode node) throws DocumentRouteException {
        DocumentRouteElement routeInstance = (DocumentRouteElement) graph;
        Map<String, String> taskVariables = new HashMap<String, String>();
        taskVariables.put(
                DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY,
                routeInstance.getDocument().getId());
        taskVariables.put(DocumentRoutingConstants.TASK_NODE_ID_KEY,
                node.getId());
        taskVariables.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY,
                node.getDocument().getId());
        String taskNotiftemplate = node.getTaskNotificationTemplate();
        if (!StringUtils.isEmpty(taskNotiftemplate)) {
            taskVariables.put(
                    DocumentRoutingConstants.TASK_ASSIGNED_NOTIFICATION_TEMPLATE,
                    taskNotiftemplate);
        } else {
            // disable notification service
            taskVariables.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE,
                    "true");
        }
        // evaluate task assignees from taskVar if any
        HashSet<String> actors = new LinkedHashSet<String>();
        actors.addAll(node.evaluateTaskAssignees());
        actors.addAll(node.getTaskAssignees());
        // evaluate taskDueDate from the taskDueDateExpr;
        Date dueDate = node.computeTaskDueDate();
        DocumentModelList docs = graph.getAttachedDocumentModels();
        try {
            TaskService taskService = Framework.getLocalService(TaskService.class);
            DocumentRoutingService routing = Framework.getLocalService(DocumentRoutingService.class);
            // TODO documents other than the first are not attached to the task
            // (task API allows only one document)
            DocumentModel doc = docs.size() > 0 ? docs.get(0) : null;
            // we may get several tasks if there's one per actor
            List<Task> tasks = taskService.createTask(session,
                    (NuxeoPrincipal) session.getPrincipal(), doc,
                    node.getTaskDocType(), node.getDocument().getTitle(),
                    node.getId(), routeInstance.getDocument().getId(),
                    new ArrayList<String>(actors), false,
                    node.getTaskDirective(), null, dueDate, taskVariables,
                    null, node.getWorkflowContextualInfo());

            routing.makeRoutingTasks(session, tasks);
            String taskAssigneesPermission = node.getTaskAssigneesPermission();
            if (StringUtils.isEmpty(taskAssigneesPermission)) {
                return;
            }
            for (Task task : tasks) {
                routing.grantPermissionToTaskAssignees(session,
                        taskAssigneesPermission, docs, task);
            }
        } catch (ClientException e) {
            throw new DocumentRouteException("Can not create task", e);
        }
    }

    protected void finishTask(CoreSession session, GraphRoute graph,
            GraphNode node, Task task) throws DocumentRouteException {
        DocumentRoutingService routing = Framework.getLocalService(DocumentRoutingService.class);
        DocumentModelList docs = graph.getAttachedDocumentModels();
        try {
            routing.removePermissionFromTaskAssignees(session, docs, task);
        } catch (ClientException e) {
            throw new DocumentRouteException("Cannot finish task", e);
        }
        // TODO delete task?
    }

}
