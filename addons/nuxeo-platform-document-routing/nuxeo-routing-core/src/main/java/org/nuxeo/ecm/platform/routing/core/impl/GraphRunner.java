/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.audit.RoutingAuditHelper;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.State;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Runs the proper nodes depending on the graph state.
 *
 * @since 5.6
 */
public class GraphRunner extends AbstractRunner implements ElementRunner, Serializable {

    private static final Log log = LogFactory.getLog(GraphRunner.class);

    /**
     * Maximum number of steps we do before deciding that this graph is looping.
     */
    public static final int MAX_LOOPS = 100;

    @Override
    public void run(CoreSession session, DocumentRouteElement element, Map<String, Serializable> map) {
        GraphRoute graph = (GraphRoute) element;
        element.setRunning(session);
        if (map != null) {
            graph.setVariables(map);
        }
        runGraph(session, element, graph.getStartNode());
    }

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        run(session, element, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void resume(CoreSession session, DocumentRouteElement element, String nodeId, String taskId,
            Map<String, Object> varData, String status) {
        GraphRoute graph = (GraphRoute) element;
        Task task = null;
        if (taskId == null) {
            if (nodeId == null) {
                throw new DocumentRouteException("nodeId and taskId both missing");
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
                    throw new DocumentRouteException("No nodeId found on task: " + taskId);
                }
            }
        }
        GraphNode node = graph.getNode(nodeId);
        if (node == null) {
            throw new DocumentRouteException("Invalid nodeId: " + nodeId);
        }
        boolean forceResume = (varData != null && varData.get(DocumentRoutingConstants.WORKFLOW_FORCE_RESUME) != null
                && (Boolean) varData.get(DocumentRoutingConstants.WORKFLOW_FORCE_RESUME));

        if (forceResume && node.getState() != State.SUSPENDED && node.getState() != State.WAITING) {
            throw new DocumentRouteException("Cannot force resume on non-suspended or non-waiting node: " + node);
        }
        if (!forceResume && node.getState() != State.SUSPENDED) {
            throw new DocumentRouteException("Cannot resume on non-suspended node: " + node);
        }
        node.setAllVariables(varData, false);
        if (StringUtils.isNotEmpty(status)) {
            node.setButton(status);
        }
        if (task != null) {
            finishTask(session, graph, node, task, false, status);
            // don't delete (yet)
            if (task != null) {
                Map<String, Serializable> eventProperties = new HashMap<>();
                eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentRoutingConstants.ROUTING_CATEGORY);
                eventProperties.put("taskName", task.getName());
                eventProperties.put("modelName", graph.getModelName());
                eventProperties.put("action", status);
                eventProperties.put("data", (Serializable) varData);
                eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, graph.getInitiator());
                eventProperties.put(RoutingAuditHelper.TASK_ACTOR, session.getPrincipal().getActingUser());
                eventProperties.put("nodeVariables", (Serializable) node.getVariables());
                eventProperties.put("workflowVariables", (Serializable) graph.getVariables());

                // Compute duration of the task itself
                long duration = RoutingAuditHelper.computeDurationSinceTaskStarted(task.getId());
                if (duration >= 0) {
                    eventProperties.put(RoutingAuditHelper.TIME_SINCE_TASK_STARTED, duration);
                }

                // Then compute duration since workflow started
                long timeSinceWfStarted = RoutingAuditHelper.computeDurationSinceWfStarted(task.getProcessId());
                if (timeSinceWfStarted >= 0) {
                    eventProperties.put(RoutingAuditHelper.TIME_SINCE_WF_STARTED, timeSinceWfStarted);
                }

                DocumentEventContext envContext = new DocumentEventContext(session, session.getPrincipal(), task.getDocument());
                envContext.setProperties(eventProperties);
                EventProducer eventProducer = Framework.getService(EventProducer.class);
                eventProducer.fireEvent(envContext.newEvent(DocumentRoutingConstants.Events.afterWorkflowTaskEnded.name()));
            }
        } else {
            // cancel any remaing tasks on this node
            node.cancelTasks();
        }
        if (node.hasOpenTasks()) {
            log.info("Node " + node.getId() + "has open tasks, the workflow can not be resumed for now.");
            // do nothing, the workflow is resumed only when all the tasks
            // created from
            // this node are processed
            // as this is a multi-task node, reset comment if it was
            // previously set
            if (varData != null && varData.get(Constants.VAR_WORKFLOW_NODE) != null
                    && ((Map<String, Serializable>) varData.get(Constants.VAR_WORKFLOW_NODE)).containsKey(
                            GraphNode.NODE_VARIABLE_COMMENT)) {
                node.setVariable(GraphNode.NODE_VARIABLE_COMMENT, "");
            }
            return;
        }
        runGraph(session, element, node);
    }

    @Override
    public void cancel(CoreSession session, DocumentRouteElement element) {
        GraphRoute graph = element instanceof GraphRoute ? (GraphRoute) element : null;

        Map<String, Serializable> eventProperties = new HashMap<>();
        if (graph != null) {
            eventProperties.put("modelId", graph.getModelId());
            eventProperties.put("modelName", graph.getModelName());
            eventProperties.put(RoutingAuditHelper.WORKFLOW_VARIABLES, (Serializable) graph.getVariables());
            eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, graph.getInitiator());
            // Get the list of pending node
            List<String> pendingNodeNames = new ArrayList<>();
            for (GraphNode suspendedNode : graph.getSuspendedNodes()) {
                pendingNodeNames.add(suspendedNode.getId());
            }
            eventProperties.put("pendingNodes", (Serializable) pendingNodeNames);
        }
        EventFirer.fireEvent(session, element, eventProperties, DocumentRoutingConstants.Events.beforeWorkflowCanceled.name());

        super.cancel(session, element);
        if (graph == null) {
            return;
        }
        // also cancel tasks
        // also cancel sub-workflows
        for (GraphNode node : graph.getNodes()) {
            node.cancelTasks();
            node.cancelSubRoute();
        }
    }

    /**
     * Runs the graph starting with the given node.
     *
     * @param initialNode the initial node to run
     */
    protected void runGraph(CoreSession session, DocumentRouteElement element, GraphNode initialNode)
            throws DocumentRouteException {
        GraphRoute graph = (GraphRoute) element;
        List<GraphNode> pendingSubRoutes = new LinkedList<>();
        LinkedList<GraphNode> pendingNodes = new LinkedList<>();
        pendingNodes.add(initialNode);
        boolean done = false;
        int count = 0;
        while (!pendingNodes.isEmpty()) {
            GraphNode node = pendingNodes.pop();
            count++;
            if (count > MAX_LOOPS) {
                throw new DocumentRouteException("Execution is looping, node: " + node);
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
                if (node.hasTask() || node.hasMultipleTasks()) {
                    createTask(session, graph, node); // may create several
                    node.setState(State.SUSPENDED);
                }
                if (node.hasSubRoute()) {
                    if (!pendingSubRoutes.contains(node)) {
                        pendingSubRoutes.add(node);
                    }
                    node.setState(State.SUSPENDED);
                }
                if (node.getState() != State.SUSPENDED) {
                    jump = State.RUNNING_OUTPUT;
                }
                // else this node is suspended,
                // remove it from queue of nodes to process
                break;
            case SUSPENDED:
                if (node != initialNode) {
                    throw new DocumentRouteException("Executing unexpected SUSPENDED state");
                }
                // actor
                NuxeoPrincipal principal = session.getPrincipal();
                String actor = principal.getActingUser();
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
                        throw new DocumentRouteException(String.format("Route %s stopped with still pending nodes: %s",
                                graph, pendingNodes));
                    }
                    done = true;
                } else {
                    if (trueTrans.isEmpty()) {
                        throw new DocumentRouteException("No transition evaluated to true from node " + node);
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
        if (done) {
            element.setDone(session);
            /*
             * Resume the parent route if this is a sub-route.
             */
            if (graph.hasParentRoute()) {
                graph.resumeParentRoute(session);
            }
        }
        /*
         * Now run the sub-routes. If they are done, they'll call back into the routing service to resume the parent
         * node (above code).
         */
        for (GraphNode node : pendingSubRoutes) {
            DocumentRoute subRoute = node.startSubRoute();
        }
        session.save();
    }

    protected void recursiveCancelInput(GraphRoute graph, GraphNode originalNode, LinkedList<GraphNode> pendingNodes) {
        LinkedList<GraphNode> todo = new LinkedList<>();
        todo.add(originalNode);
        Set<String> done = new HashSet<>();
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

    protected void createTask(CoreSession session, GraphRoute graph, GraphNode node) throws DocumentRouteException {
        DocumentRouteElement routeInstance = graph;
        Map<String, String> taskVariables = new HashMap<>();
        taskVariables.put(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY,
                routeInstance.getDocument().getId());
        taskVariables.put(DocumentRoutingConstants.TASK_NODE_ID_KEY, node.getId());
        taskVariables.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, node.getDocument().getId());
        String taskNotiftemplate = node.getTaskNotificationTemplate();
        if (!StringUtils.isEmpty(taskNotiftemplate)) {
            taskVariables.put(DocumentRoutingConstants.TASK_ASSIGNED_NOTIFICATION_TEMPLATE, taskNotiftemplate);
        } else {
            // disable notification service
            taskVariables.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE, "true");
        }
        // evaluate task assignees from taskVar if any
        HashSet<String> actors = new LinkedHashSet<>();
        actors.addAll(node.evaluateTaskAssignees());
        actors.addAll(node.getTaskAssignees());
        // evaluate taskDueDate from the taskDueDateExpr;
        Date dueDate = node.computeTaskDueDate();
        DocumentModelList docs = graph.getAttachedDocumentModels();
        TaskService taskService = Framework.getService(TaskService.class);
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        // TODO documents other than the first are not attached to the task
        // (task API allows only one document)
        // we may get several tasks if there's one per actor when the node
        // has the property
        // hasMultipleTasks set to true
        List<Task> tasks = taskService.createTask(session, session.getPrincipal(), docs,
                                                  node.getTaskDocType(), node.getDocument().getTitle(), node.getId(), routeInstance.getDocument().getId(),
                                                  new ArrayList<>(actors), node.hasMultipleTasks(), node.getTaskDirective(), null, dueDate,
                                                  taskVariables, null, node.getWorkflowContextualInfo(session, true));

        // Audit task assignment
        for (Task task : tasks) {
            Map<String, Serializable> eventProperties = new HashMap<>();
            eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentRoutingConstants.ROUTING_CATEGORY);
            eventProperties.put("taskName", node.getDocument().getTitle());
            eventProperties.put("actors", actors);
            eventProperties.put("modelId", graph.getModelId());
            eventProperties.put("modelName", graph.getModelName());
            eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, graph.getInitiator());
            eventProperties.put(RoutingAuditHelper.TASK_ACTOR, session.getPrincipal().getOriginatingUser());
            eventProperties.put("nodeVariables", (Serializable) node.getVariables());
            if (routeInstance instanceof GraphRoute) {
                eventProperties.put("workflowVariables", (Serializable) ((GraphRoute) routeInstance).getVariables());
            }

            // compute duration since workflow started
            long timeSinceWfStarted = RoutingAuditHelper.computeDurationSinceWfStarted(task.getProcessId());
            if (timeSinceWfStarted >= 0) {
                eventProperties.put(RoutingAuditHelper.TIME_SINCE_WF_STARTED, timeSinceWfStarted);
            }

            DocumentEventContext envContext = new DocumentEventContext(session, session.getPrincipal(), task.getDocument());
            envContext.setProperties(eventProperties);
            EventProducer eventProducer = Framework.getService(EventProducer.class);
            eventProducer.fireEvent(envContext.newEvent(DocumentRoutingConstants.Events.afterWorkflowTaskCreated.name()));
        }

        // routing.makeRoutingTasks(session, tasks);
        for (Task task : tasks) {
            node.addTaskInfo(task.getId());
        }
        String taskAssigneesPermission = node.getTaskAssigneesPermission();
        if (StringUtils.isEmpty(taskAssigneesPermission)) {
            return;
        }
        for (Task task : tasks) {
            routing.grantPermissionToTaskAssignees(session, taskAssigneesPermission, docs, task);
        }
    }

    protected void finishTask(CoreSession session, GraphRoute graph, GraphNode node, Task task, boolean delete)
            throws DocumentRouteException {
        finishTask(session, graph, node, task, delete, null);
    }

    protected void finishTask(CoreSession session, GraphRoute graph, GraphNode node, Task task, boolean delete,
            String status) throws DocumentRouteException {
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        DocumentModelList docs = graph.getAttachedDocumentModels();
        routing.removePermissionsForTaskActors(session, docs, task);
        // delete task
        if (delete) {
            session.removeDocument(new IdRef(task.getId()));
        }
        // get the last comment on the task, if there are several:
        // task might have been previously reassigned or delegated
        List<TaskComment> comments = task.getComments();
        String comment = comments.size() > 0 ? comments.get(comments.size() - 1).getText() : "";
        // actor
        NuxeoPrincipal principal = session.getPrincipal();
        String actor = principal.getActingUser();
        node.updateTaskInfo(task.getId(), true, status, actor, comment);
    }

}
