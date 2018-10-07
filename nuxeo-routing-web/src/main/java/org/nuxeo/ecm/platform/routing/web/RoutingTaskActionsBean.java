/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Button;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.action.ActionContextProvider;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Task validators
 *
 * @since 5.6
 */
@Scope(CONVERSATION)
@Name("routingTaskActions")
public class RoutingTaskActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RoutingTaskActionsBean.class);

    public static final String SUBJECT_PATTERN = "([a-zA-Z_0-9]*(:)[a-zA-Z_0-9]*)";

    /**
     * Runtime property name, that makes it possible to cache actions available on a given task, depending on its type.
     * <p>
     * This caching is global to all tasks in the platform, and will not work correctly if some tasks are filtering some
     * actions depending on local variables, for instance.
     *
     * @since 5.7
     */
    public static final String CACHE_ACTIONS_PER_TASK_TYPE_PROP_NAME = "org.nuxeo.routing.cacheActionsPerTaskType";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    @In(create = true, required = false)
    protected ContentViewActions contentViewActions;

    @RequestParameter("button")
    protected String button;

    protected ActionManager actionService;

    protected Map<String, TaskInfo> tasksInfoCache = new HashMap<String, TaskInfo>();

    protected Task currentTask;

    protected List<String> formVariablesToKeep;

    public void validateTaskDueDate(FacesContext context, UIComponent component, Object value) {
        final String DATE_FORMAT = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        String messageString = null;
        if (value != null) {
            try {
                Date dueDate = dateFormat.parse(dateFormat.format((Date) value));
                Date today = dateFormat.parse(dateFormat.format(new Date()));
                if (dueDate.before(today)) {
                    messageString = "label.workflow.error.outdated_duedate";
                }
            } catch (ParseException e) {
                messageString = "label.workflow.error.date_parsing";
            }
        }

        if (messageString != null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(context,
                    "label.workflow.error.outdated_duedate"), null);
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
        }
    }

    public void validateSubject(FacesContext context, UIComponent component, Object value) {
        if (!((value instanceof String) && ((String) value).matches(SUBJECT_PATTERN))) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(context,
                    "label.document.routing.invalid.subject"), null);
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

    public String getTaskLayout(Task task) {
        return getTaskInfo(task, true).layout;
    }

    public List<Action> getTaskButtons(Task task) {
        List<Button> buttons = getTaskInfo(task, true).buttons;
        List<Action> actions = new ArrayList<Action>();

        DocumentModel workflowInstance = documentManager.getDocument(new IdRef(task.getProcessId()));
        GraphRoute workflow = workflowInstance.getAdapter(GraphRoute.class);
        if (workflow == null) {
            // task was not created by a workflow process , no actions to
            // display;
            return actions;
        }
        GraphNode node = workflow.getNode(task.getType());
        for (Button button : buttons) {
            Action action = new Action(button.getName(), Action.EMPTY_CATEGORIES);
            action.setLabel(button.getLabel());
            boolean displayAction = true;
            if (StringUtils.isNotEmpty(button.getFilter())) {
                ActionContext actionContext = actionContextProvider.createActionContext();
                if (node != null) {
                    Map<String, Object> workflowContextualInfo = new HashMap<String, Object>();
                    workflowContextualInfo.putAll(node.getWorkflowContextualInfo(documentManager, true));
                    actionContext.putAllLocalVariables(workflowContextualInfo);
                }
                displayAction = getActionService().checkFilter(button.filter, actionContext);
            }
            if (displayAction) {
                actions.add(action);
            }
        }
        return actions;
    }

    public String endTask(Task task) {
        // collect form data
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Serializable> formVariables = getFormVariables(task);
        if (getFormVariables(task) != null) {
            data.put("WorkflowVariables", getFormVariables(task));
            data.put("NodeVariables", getFormVariables(task));
            // if there is a comment on the submitted form, pass it to be
            // logged by audit
            if (formVariables.containsKey(GraphNode.NODE_VARIABLE_COMMENT)) {
                data.put(GraphNode.NODE_VARIABLE_COMMENT, formVariables.get(GraphNode.NODE_VARIABLE_COMMENT));
            }
        }
        // add the button name that was clicked
        try {
            DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
            routing.endTask(documentManager, task, data, button);
            facesMessages.add(StatusMessage.Severity.INFO, messages.get("workflow.feedback.info.taskEnded"));
        } catch (DocumentRouteException e) {
            log.error(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get("workflow.feedback.error.taskEnded"));
        }
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);
        clear(task.getId());
        if (navigationContext.getCurrentDocument() != null
                && documentManager.hasPermission(navigationContext.getCurrentDocument().getRef(),
                        SecurityConstants.READ)) {
            return null;
        }
        // if the user only had temporary permissions on the current doc given
        // by the workflow
        navigationContext.setCurrentDocument(null);
        return navigationContext.goHome();
    }

    private void clear(String taskId) {
        button = null;
        if (tasksInfoCache.containsKey(taskId)) {
            tasksInfoCache.remove(taskId);
        }
    }

    public Map<String, Serializable> getFormVariables(Task task) {
        return getTaskInfo(task, true).formVariables;
    }

    public class TaskInfo {
        protected HashMap<String, Serializable> formVariables;

        protected String layout;

        protected boolean canBeReassigned;

        protected List<Button> buttons;

        protected List<String> actors;

        protected String comment;

        protected String taskId;

        protected String name;

        protected TaskInfo(String taskId, HashMap<String, Serializable> formVariables, String layout,
                List<Button> buttons, boolean canBeReassigned, String name) {
            this.formVariables = formVariables;
            this.layout = layout;
            this.buttons = buttons;
            this.canBeReassigned = canBeReassigned;
            this.taskId = taskId;
            this.name = name;
        }

        public List<String> getActors() {
            return actors;
        }

        public void setActors(List<String> actors) {
            this.actors = actors;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public boolean isCanBeReassigned() {
            return canBeReassigned;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getName() {
            return name;
        }
    }

    // we have to be unrestricted to get this info
    // because the current user may not be the one that started the
    // workflow
    public TaskInfo getTaskInfo(final Task task, final boolean getFormVariables) {
        if (tasksInfoCache.containsKey(task.getId())) {
            return tasksInfoCache.get(task.getId());
        }
        final String routeDocId = task.getVariable(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        final String nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
        if (routeDocId == null) {
            throw new NuxeoException("Can not get the source graph for this task");
        }
        if (nodeId == null) {
            throw new NuxeoException("Can not get the source node for this task");
        }
        final TaskInfo[] res = new TaskInfo[1];
        new UnrestrictedSessionRunner(documentManager) {
            @Override
            public void run() {
                DocumentModel doc = session.getDocument(new IdRef(routeDocId));
                GraphRoute route = doc.getAdapter(GraphRoute.class);
                GraphNode node = route.getNode(nodeId);
                HashMap<String, Serializable> map = new HashMap<String, Serializable>();
                if (getFormVariables) {
                    map.putAll(node.getVariables());
                    map.putAll(route.getVariables());
                }
                res[0] = new TaskInfo(task.getId(), map, node.getTaskLayout(), node.getTaskButtons(),
                        node.allowTaskReassignment(), task.getName());
            }
        }.runUnrestricted();
        // don't add tasks in cache when are fetched without the form variables
        // for
        // bulk processing
        if (getFormVariables) {
            tasksInfoCache.put(task.getId(), res[0]);
        }
        return res[0];
    }

    /**
     * @since 5.6
     */
    public boolean isRoutingTask(Task task) {
        return task.getDocument().hasFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME);
    }

    /**
     * @since 5.6
     */
    public List<Action> getTaskActions(Task task) {
        return new ArrayList<Action>(getTaskActionsMap(task).values());
    }

    // temp method because Studio also refers to empty layouts
    protected boolean isLayoutEmpty(String layoutName) {
        if (layoutName == null || layoutName.isEmpty()) {
            return true;
        }
        // explore the layout and find out if it contains only empty widgets
        WebLayoutManager lm = Framework.getService(WebLayoutManager.class);
        LayoutDefinition layout = lm.getLayoutDefinition(layoutName);
        if (layout == null || layout.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Helper to generate a unique action id for all task types
     *
     * @since 5.7
     */
    protected String getTaskActionId(Task task, String buttonId) {
        return String.format("%s_%s", task.getType(), buttonId);
    }

    /**
     * @since 5.6
     */
    public Map<String, Action> getTaskActionsMap(Task task) {
        Map<String, Action> actions = new LinkedHashMap<String, Action>();
        TaskInfo taskInfo = getTaskInfo(task, true);
        String layout = taskInfo.layout;
        List<Button> buttons = taskInfo.buttons;

        boolean addLayout = !isLayoutEmpty(layout);
        Map<String, Serializable> props = null;
        if (addLayout) {
            props = new HashMap<String, Serializable>();
            props.put("layout", layout);
            props.put("formVariables", taskInfo.formVariables);
        }

        if (buttons != null && !buttons.isEmpty()) {
            for (Button button : buttons) {
                String buttonId = button.getName();
                String id = getTaskActionId(task, buttonId);
                Action action = new Action(id, Action.EMPTY_CATEGORIES);
                action.setLabel(button.getLabel());
                Map<String, Serializable> actionProps = new HashMap<String, Serializable>();
                actionProps.put("buttonId", buttonId);
                if (addLayout) {
                    actionProps.putAll(props);
                    action.setProperties(actionProps);
                    action.setType("fancybox");
                } else {
                    action.setProperties(actionProps);
                    action.setType("link");
                }
                boolean displayAction = true;
                if (StringUtils.isNotEmpty(button.getFilter())) {
                    displayAction = getActionService().checkFilter(button.filter,
                            actionContextProvider.createActionContext());
                }
                if (displayAction) {
                    actions.put(id, action);
                }
            }
        }

        // If there is a form attached to these tasks, add a generic
        // process action to open the fancy box.
        // The form of the first task will be displayed, but all the tasks
        // concerned by this action share the same form, as they share the
        // same type.
        if (addLayout && !actions.isEmpty()) {
            String id = getTaskActionId(task, "process_task");
            Action processAction = new Action(id, Action.EMPTY_CATEGORIES);

            formVariablesToKeep = new ArrayList<>();
            WebLayoutManager layoutService = Framework.getService(WebLayoutManager.class);
            LayoutDefinition taskLayout = layoutService.getLayoutDefinition(taskInfo.layout);
            if (taskLayout != null) {
                for (LayoutRowDefinition row : taskLayout.getRows()) {
                    for (WidgetReference widgetRef : row.getWidgetReferences()) {
                        WidgetDefinition widgetDefinition = taskLayout.getWidgetDefinition(widgetRef.getName());
                        if (widgetDefinition == null) {
                            continue;
                        }

                        String mode = widgetDefinition.getMode(BuiltinModes.EDIT);
                        ActionContext el = new ELActionContext();
                        el.setCurrentPrincipal(documentManager.getPrincipal());
                        el.setCurrentDocument(navigationContext.getCurrentDocument());
                        mode = el.evalExpression(mode, String.class);
                        if (mode == null || mode.equals(BuiltinModes.EDIT)) {
                            Arrays.stream(widgetDefinition.getFieldDefinitions()).forEach((field) -> {
                                // workflow form fields are always like "['$variable']"
                                // remove both [' and '] to keep only the variable name
                                String fieldName = field.getFieldName().replaceAll("^\\['|']$", "");
                                formVariablesToKeep.add(fieldName);
                            });
                        }
                    }
                }
            }

            processAction.setProperties(props);
            processAction.setType("process_task");
            actions.put(id, processAction);
        }

        return actions;
    }

    /**
     * Returns actions for task document buttons defined in the workflow graph
     *
     * @since 5.6
     */
    @SuppressWarnings("boxing")
    public List<Action> getTaskActions(String selectionListName) {
        Map<String, Action> actions = new LinkedHashMap<String, Action>();
        Map<String, Map<String, Action>> actionsPerTaskType = new LinkedHashMap<String, Map<String, Action>>();
        Map<String, Integer> actionsCounter = new HashMap<String, Integer>();
        List<DocumentModel> docs = documentsListsManager.getWorkingList(selectionListName);
        boolean cachePerType = Boolean.TRUE.equals(Boolean.valueOf(Framework.getProperty(CACHE_ACTIONS_PER_TASK_TYPE_PROP_NAME)));
        int taskDocsNum = 0;
        if (docs != null && !docs.isEmpty()) {
            for (DocumentModel doc : docs) {
                if (doc.hasFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME)) {
                    Task task = new TaskImpl(doc);
                    String taskType = task.getType();
                    Map<String, Action> taskActions = Collections.emptyMap();
                    // if caching per type, fill the per type map, else update
                    // actions directly
                    if (cachePerType) {
                        if (actionsPerTaskType.containsKey(taskType)) {
                            taskActions = actionsPerTaskType.get(taskType);
                        } else {
                            taskActions = getTaskActionsMap(task);
                            actionsPerTaskType.put(taskType, taskActions);
                        }
                    } else {
                        taskActions = getTaskActionsMap(task);
                        actions.putAll(taskActions);
                    }
                    for (String actionId : taskActions.keySet()) {
                        Integer count = actionsCounter.get(actionId);
                        if (count == null) {
                            actionsCounter.put(actionId, 1);
                        } else {
                            actionsCounter.put(actionId, count + 1);
                        }
                    }
                    taskDocsNum++;
                }
            }
        }
        if (cachePerType) {
            // initialize actions for cache map
            for (Map<String, Action> actionsPerType : actionsPerTaskType.values()) {
                actions.putAll(actionsPerType);
            }
        }
        List<Action> res = new ArrayList<Action>(actions.values());
        for (Action action : res) {
            if (!actionsCounter.get(action.getId()).equals(taskDocsNum)) {
                action.setAvailable(false);
            }
        }
        return res;
    }

    /**
     * Ends a task given a selection list name and an action
     *
     * @since 5.6
     */
    @SuppressWarnings("unchecked")
    public String endTasks(String selectionListName, Action taskAction) {
        // collect form data
        Map<String, Object> data = new HashMap<String, Object>();
        String buttonId = (String) taskAction.getProperties().get("buttonId");
        Map<String, Serializable> formVariables = (Map<String, Serializable>) taskAction.getProperties().get(
                "formVariables");

        if (formVariables != null && !formVariables.isEmpty()) {
            // if there is a comment on the submitted form, pass it to be
            // logged by audit
            if (formVariables.containsKey(GraphNode.NODE_VARIABLE_COMMENT)) {
                data.put(GraphNode.NODE_VARIABLE_COMMENT, formVariables.get(GraphNode.NODE_VARIABLE_COMMENT));
            }
        }

        // get task documents
        boolean hasErrors = false;
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        List<DocumentModel> docs = documentsListsManager.getWorkingList(selectionListName);
        if (docs != null && !docs.isEmpty()) {
            for (DocumentModel doc : docs) {
                // For each task, compute its own node and workflow variables
                Task task = new TaskImpl(doc);
                Map<String, Serializable> variables = getFormVariables(task);
                for (String fieldName : formVariablesToKeep) {
                    variables.put(fieldName, formVariables.get(fieldName));
                }
                data.put("WorkflowVariables", variables);
                data.put("NodeVariables", variables);
                if (doc.hasFacet(DocumentRoutingConstants.ROUTING_TASK_FACET_NAME)) {
                    // add the button name that was clicked
                    try {
                        routing.endTask(documentManager, new TaskImpl(doc), data, buttonId);
                    } catch (DocumentRouteException e) {
                        log.error(e, e);
                        hasErrors = true;
                    }
                }
            }
            formVariablesToKeep = null;
        }
        if (hasErrors) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get("workflow.feedback.error.tasksEnded"));
        } else {
            facesMessages.add(StatusMessage.Severity.INFO, messages.get("workflow.feedback.info.tasksEnded"));
        }
        // reset selection list
        documentsListsManager.resetWorkingList(selectionListName);
        // raise document change event to trigger refresh of content views
        // listing task documents.
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);
        return null;
    }

    private ActionManager getActionService() {
        if (actionService == null) {
            actionService = Framework.getService(ActionManager.class);
        }
        return actionService;
    }

    /***
     * @since 5.7
     */
    @Observer(value = { TaskEventNames.WORKFLOW_TASK_COMPLETED, TaskEventNames.WORKFLOW_TASK_REASSIGNED,
            TaskEventNames.WORKFLOW_TASK_DELEGATED })
    @BypassInterceptors
    public void OnTaskCompleted() {
        if (contentViewActions != null) {
            contentViewActions.refreshOnSeamEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);
            contentViewActions.resetPageProviderOnSeamEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);
        }
        tasksInfoCache.clear();
        currentTask = null;
    }

    /**
     * @since 5.7.3
     */
    public String reassignTask(TaskInfo taskInfo) {
        try {
            Framework.getService(DocumentRoutingService.class).reassignTask(documentManager, taskInfo.getTaskId(),
                    taskInfo.getActors(), taskInfo.getComment());
            Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_REASSIGNED);
        } catch (DocumentRouteException e) {
            log.error(e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get("workflow.feedback.error.taskEnded"));
        }
        return null;
    }

    /**
     * @since 5.7.3
     */
    public String getWorkflowTitle(String instanceId) {
        String workflowTitle = "";

        try {
            DocumentModel routeInstance = documentManager.getDocument(new IdRef(instanceId));
            workflowTitle = routeInstance.getTitle();
        } catch (DocumentNotFoundException e) {
            log.error("Can not fetch route instance with id " + instanceId, e);
        }
        return workflowTitle;
    }

    /**
     * @since 5.8
     */
    public String delegateTask(TaskInfo taskInfo) {
        try {
            Framework.getService(DocumentRoutingService.class).delegateTask(documentManager, taskInfo.getTaskId(),
                    taskInfo.getActors(), taskInfo.getComment());
            Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_DELEGATED);
        } catch (DocumentRouteException e) {
            log.error(e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get("workflow.feedback.error.taskEnded"));
        }
        return null;
    }

    /**
     * @since 5.8
     */
    public String navigateToTask(DocumentModel taskDoc) {
        setCurrentTask(taskDoc.getAdapter(Task.class));
        return null;
    }

    /**
     * @since 5.8
     */
    public String navigateToTasksView() {
        setCurrentTask(null);
        return null;
    }

    /**
     * @since 5.8
     */
    public Task getCurrentTask() {
        return currentTask;
    }

    /**
     * @since 5.8
     */
    public void setCurrentTask(Task currentTask) {
        this.currentTask = currentTask;
    }

    /**
     * Added to avoid an error when opening a task created @before 5.8 see NXP-14047
     *
     * @since 5.9.3, 5.8.0-HF10
     * @return
     */
    @SuppressWarnings("deprecation")
    public List<String> getCurrentTaskTargetDocumentsIds() {
        Set<String> uniqueTargetDocIds = new HashSet<String>();
        List<String> docIds = new ArrayList<String>();
        if (currentTask == null) {
            return docIds;
        }
        uniqueTargetDocIds.addAll(currentTask.getTargetDocumentsIds());
        docIds.addAll(uniqueTargetDocIds);
        return docIds.isEmpty() ? null : docIds;
    }

    /**
     * @since 5.8 - Define if action reassign task can be displayed.
     */
    public boolean canBeReassign() {
        if (currentTask == null) {
            return false;
        }
        DocumentModel workflowInstance = documentManager.getDocument(new IdRef(currentTask.getProcessId()));
        GraphRoute workflow = workflowInstance.getAdapter(GraphRoute.class);
        if (workflow == null) {
            return false;
        }
        GraphNode node = workflow.getNode(currentTask.getType());
        return node.allowTaskReassignment() && !currentTask.getDelegatedActors().contains(documentManager.getPrincipal().getName());
    }
}
