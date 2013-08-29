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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mvel2.CompileException;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.scripting.DateWrapper;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.api.TasksInfoWrapper;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Graph Node implementation as an adapter over a DocumentModel.
 *
 * @since 5.6
 */
public class GraphNodeImpl extends DocumentRouteElementImpl implements
        GraphNode {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(GraphNodeImpl.class);

    private static final String EXPR_PREFIX = "expr:";

    private static final String TEMPLATE_START = "@{";

    protected final GraphRouteImpl graph;

    protected State localState;

    /** To be used through getter. */
    protected List<Transition> inputTransitions;

    /** To be used through getter. */
    protected List<Transition> outputTransitions;

    /** To be used through getter. */
    protected List<Button> taskButtons;

    protected List<EscalationRule> escalationRules;

    protected List<TaskInfo> tasksInfo;

    public GraphNodeImpl(DocumentModel doc, GraphRouteImpl graph) {
        super(doc, new GraphRunner());
        this.graph = graph;
        inputTransitions = new ArrayList<Transition>(2);
    }

    /**
     * @since 5.7.2
     */
    public GraphNodeImpl(DocumentModel doc) {
        super(doc, new GraphRunner());
        this.graph = (GraphRouteImpl) getDocumentRoute(doc.getCoreSession());
        inputTransitions = new ArrayList<Transition>(2);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(getId()).toString();
    }

    protected boolean getBoolean(String propertyName) {
        return Boolean.TRUE.equals(getProperty(propertyName));
    }

    protected void incrementProp(String prop) {
        try {
            Long count = (Long) getProperty(prop);
            if (count == null) {
                count = Long.valueOf(0);
            }
            document.setPropertyValue(prop, Long.valueOf(count.longValue() + 1));
            saveDocument();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected CoreSession getSession() {
        return document.getCoreSession();
    }

    protected void saveDocument() throws ClientException {
        getSession().saveDocument(document);
    }

    @Override
    public String getId() {
        return (String) getProperty(PROP_NODE_ID);
    }

    @Override
    public State getState() {
        try {
            if (localState != null) {
                return localState;
            }
            String s = document.getCurrentLifeCycleState();
            return State.fromString(s);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void setState(State state) {
        try {
            if (state == null) {
                throw new NullPointerException("null state");
            }
            String lc = state.getLifeCycleState();
            if (lc == null) {
                localState = state;
                return;
            } else {
                localState = null;
                String oldLc = document.getCurrentLifeCycleState();
                if (lc.equals(oldLc)) {
                    return;
                }
                document.followTransition(state.getTransition());
                saveDocument();
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public boolean isStart() {
        return getBoolean(PROP_START);
    }

    @Override
    public boolean isStop() {
        return getBoolean(PROP_STOP);
    }

    @Override
    public void setCanceled() {
        log.debug("Canceling " + this);
        incrementProp(PROP_CANCELED);
    }

    @Override
    public long getCanceledCount() {
        Long c = (Long) getProperty(PROP_CANCELED);
        return c == null ? 0 : c.longValue();
    }

    @Override
    public boolean isMerge() {
        String merge = (String) getProperty(PROP_MERGE);
        return StringUtils.isNotEmpty(merge);
    }

    @Override
    public String getInputChain() {
        return (String) getProperty(PROP_INPUT_CHAIN);
    }

    @Override
    public String getOutputChain() {
        return (String) getProperty(PROP_OUTPUT_CHAIN);
    }

    @Override
    public boolean hasTask() {
        return getBoolean(PROP_HAS_TASK);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getTaskAssignees() {
        return (List<String>) getProperty(PROP_TASK_ASSIGNEES);
    }

    public String getTaskAssigneesVar() {
        return (String) getProperty(PROP_TASK_ASSIGNEES_VAR);
    }

    @Override
    public Date getTaskDueDate() {
        Calendar cal = (Calendar) getProperty(PROP_TASK_DUE_DATE);
        return cal == null ? null : cal.getTime();
    }

    @Override
    public String getTaskDirective() {
        return (String) getProperty(PROP_TASK_DIRECTIVE);
    }

    @Override
    public String getTaskAssigneesPermission() {
        return (String) getProperty(PROP_TASK_ASSIGNEES_PERMISSION);
    }

    @Override
    public String getTaskLayout() {
        return (String) getProperty(PROP_TASK_LAYOUT);
    }

    @Override
    public String getTaskNotificationTemplate() {
        return (String) getProperty(PROP_TASK_NOTIFICATION_TEMPLATE);
    }

    @Override
    public String getTaskDueDateExpr() {
        return (String) getProperty(PROP_TASK_DUE_DATE_EXPR);
    }

    @Override
    public void starting() {
        try {
            // Allow input transitions reevaluation (needed for loop case)
            for (Transition t : inputTransitions) {
                t.setResult(false);
                getSession().saveDocument(t.source.getDocument());
            }
            // Increment node counter
            incrementProp(PROP_COUNT);
            document.setPropertyValue(PROP_NODE_START_DATE,
                    Calendar.getInstance());
            saveDocument();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void ending() {
        try {
            document.setPropertyValue(PROP_NODE_END_DATE,
                    Calendar.getInstance());
            saveDocument();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public Map<String, Serializable> getVariables() {
        return GraphVariablesUtil.getVariables(document, PROP_VARIABLES_FACET);
    }

    public void setVariables(Map<String, Serializable> map) {
        GraphVariablesUtil.setVariables(document, PROP_VARIABLES_FACET, map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAllVariables(Map<String, Object> map) {
        if (map == null) {
            return;
        }

        // get variables from node and graph
        Map<String, Serializable> graphVariables = graph.getVariables();
        Map<String, Serializable> nodeVariables = getVariables();

        // set variables back into node and graph
        boolean changedNodeVariables = false;
        boolean changedGraphVariables = false;
        if (map.get(Constants.VAR_WORKFLOW_NODE) != null) {
            for (Entry<String, Serializable> es : ((Map<String, Serializable>) map.get(Constants.VAR_WORKFLOW_NODE)).entrySet()) {
                String key = es.getKey();
                Serializable value = es.getValue();
                if (nodeVariables.containsKey(key)) {
                    Serializable oldValue = nodeVariables.get(key);
                    if (!equality(value, oldValue)) {
                        changedNodeVariables = true;
                        nodeVariables.put(key, value);
                    }
                }
            }
        }
        if (map.get(Constants.VAR_WORKFLOW) != null) {
            for (Entry<String, Serializable> es : ((Map<String, Serializable>) map.get(Constants.VAR_WORKFLOW)).entrySet()) {
                String key = es.getKey();
                Serializable value = es.getValue();
                if (graphVariables.containsKey(key)) {
                    Serializable oldValue = graphVariables.get(key);
                    if (!equality(value, oldValue)) {
                        changedGraphVariables = true;
                        graphVariables.put(key, value);
                    }
                }
            }
        }
        if (changedNodeVariables) {
            setVariables(nodeVariables);
        }
        if (changedGraphVariables) {
            graph.setVariables(graphVariables);
        }
    }

    public static boolean equality(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1 instanceof List && o2.getClass().isArray()) {
            return Arrays.equals(((List<?>) o1).toArray(), (Object[]) o2);
        } else if (o1.getClass().isArray() && o2 instanceof List) {
            return Arrays.equals((Object[]) o1, ((List<?>) o2).toArray());
        } else if (o1.getClass().isArray() && o2.getClass().isArray()) {
            // Nuxeo doesn't use arrays of primitive types
            return Arrays.equals((Object[]) o1, (Object[]) o2);
        } else {
            return o1.equals(o2);
        }
    }

    protected OperationContext getExecutionContext(CoreSession session) {
        OperationContext context = new OperationContext(session);
        context.putAll(getWorkflowContextualInfo(session, true));
        context.setCommit(false); // no session save at end
        DocumentModelList documents = graph.getAttachedDocuments(session);
        // associated docs
        context.setInput(documents);
        return context;
    }

    @Override
    public Map<String, Serializable> getWorkflowContextualInfo(
            CoreSession session, boolean detached) {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        // workflow context
        context.put("WorkflowVariables", (Serializable) graph.getVariables());
        context.put("workflowInitiator", getWorkflowInitiator());
        context.put("workflowStartTime", getWorkflowStartTime());
        context.put("workflowParent", getWorkflowParentRouteId());
        context.put("workflowParentNode", getWorkflowParentNodeId());
        context.put("workflowInstanceId", graph.getDocument().getId());
        context.put("taskDueTime", (Calendar) getProperty(PROP_TASK_DUE_DATE));

        DocumentModelList documents = graph.getAttachedDocuments(session);
        if (detached) {
            for (DocumentModel documentModel : documents) {
                try {
                    documentModel.detach(true);
                } catch (ClientException e) {
                    log.error(e);
                    throw new ClientRuntimeException(e);
                }
            }
        }
        context.put("workflowDocuments", documents);
        context.put("documents", documents);
        // node context
        String button = (String) getProperty(PROP_NODE_BUTTON);
        Map<String, Serializable> nodeVariables = getVariables();
        nodeVariables.put("button", button);
        nodeVariables.put("numberOfProcessedTasks",
                getProcessedTasksInfo().size());
        nodeVariables.put("numberOfTasks", getTasksInfo().size());
        nodeVariables.put("tasks", (Serializable) new TasksInfoWrapper(
                getTasksInfo()));
        context.put("NodeVariables", (Serializable) nodeVariables);
        context.put("nodeId", getId());
        String state = getState().name().toLowerCase();
        context.put("nodeState", state);
        context.put("state", state);
        context.put("nodeStartTime", getNodeStartTime());
        context.put("nodeEndTime", getNodeEndTime());
        context.put("nodeLastActor", getNodeLastActor());

        // task context
        context.put("comment", "");
        return context;
    }

    protected String getWorkflowInitiator() {
        try {
            return (String) graph.getDocument().getPropertyValue(
                    DocumentRoutingConstants.INITIATOR);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected Calendar getWorkflowStartTime() {
        try {
            return (Calendar) graph.getDocument().getPropertyValue("dc:created");
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected String getWorkflowParentRouteId() {
        try {
            return (String) graph.getDocument().getPropertyValue(
                    GraphRoute.PROP_PARENT_ROUTE);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected String getWorkflowParentNodeId() {
        try {
            return (String) graph.getDocument().getPropertyValue(
                    GraphRoute.PROP_PARENT_NODE);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected Calendar getNodeStartTime() {
        try {
            return (Calendar) getDocument().getPropertyValue(
                    PROP_NODE_START_DATE);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected Calendar getNodeEndTime() {
        try {
            return (Calendar) getDocument().getPropertyValue(PROP_NODE_END_DATE);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected String getNodeLastActor() {
        try {
            return (String) getDocument().getPropertyValue(PROP_NODE_LAST_ACTOR);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void executeChain(String chainId) throws DocumentRouteException {
        executeChain(chainId, null);
    }

    @Override
    public void executeTransitionChain(Transition transition)
            throws DocumentRouteException {
        executeChain(transition.chain, transition.id);
    }

    public void executeChain(String chainId, String transitionId)
            throws DocumentRouteException {
        // TODO events
        if (StringUtils.isEmpty(chainId)) {
            return;
        }

        // get base context
        OperationContext context = getExecutionContext(getSession());
        if (transitionId != null) {
            context.put("transition", transitionId);
        }

        AutomationService automationService = Framework.getLocalService(AutomationService.class);
        try {
            automationService.run(context, chainId);
            // stupid run() method throws generic Exception
        } catch (InterruptedException e) {
            // restore interrupted state
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentRouteException("Error running chain: " + chainId,
                    e);
        }

        setAllVariables(context);
    }

    @Override
    public void initAddInputTransition(Transition transition) {
        inputTransitions.add(transition);
    }

    protected List<Transition> computeOutputTransitions() {
        try {
            ListProperty props = (ListProperty) document.getProperty(PROP_TRANSITIONS);
            List<Transition> trans = new ArrayList<Transition>(props.size());
            for (Property p : props) {
                trans.add(new Transition(this, p));
            }
            return trans;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public List<Transition> getOutputTransitions() {
        if (outputTransitions == null) {
            outputTransitions = computeOutputTransitions();
        }
        return outputTransitions;
    }

    @Override
    public List<Transition> evaluateTransitions() throws DocumentRouteException {
        try {
            List<Transition> trueTrans = new ArrayList<Transition>();
            OperationContext context = getExecutionContext(getSession());
            for (Transition t : getOutputTransitions()) {
                context.put("transition", t.id);
                Expression expr = Scripting.newExpression(t.condition);
                Object res = null;
                try {
                    res = expr.eval(context);
                    // stupid eval() method throws generic Exception
                } catch (InterruptedException e) {
                    // restore interrupted state
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DocumentRouteException(
                            "Error evaluating condition: " + t.condition, e);
                }
                if (!(res instanceof Boolean)) {
                    throw new DocumentRouteException(
                            "Condition for transition " + t + " of node '"
                                    + getId() + "' of graph '"
                                    + graph.getName()
                                    + "' does not evaluate to a boolean: "
                                    + t.condition);
                }
                boolean bool = Boolean.TRUE.equals(res);
                t.setResult(bool);
                if (bool) {
                    trueTrans.add(t);
                    if (executeOnlyFirstTransition()) {
                        // if node is exclusive, no need to evaluate others
                        break;
                    }
                }
            }
            saveDocument();
            return trueTrans;
        } catch (DocumentRouteException e) {
            throw e;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public List<String> evaluateTaskAssignees() throws DocumentRouteException {
        List<String> taskAssignees = new ArrayList<String>();
        String taskAssigneesVar = getTaskAssigneesVar();
        if (StringUtils.isEmpty(taskAssigneesVar)) {
            return taskAssignees;
        }
        OperationContext context = getExecutionContext(getSession());
        Expression expr = Scripting.newExpression(taskAssigneesVar);
        Object res = null;
        try {
            res = expr.eval(context);
        } catch (InterruptedException e) {
            // restore interrupted state
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentRouteException(
                    "Error evaluating task assignees: " + taskAssigneesVar, e);
        }
        if (res instanceof List<?>) {
            res = ((List<?>) res).toArray();
        }
        if (res instanceof Object[]) {
            // try to convert to String[]
            Object[] list = (Object[]) res;
            String[] tmp = new String[list.length];
            try {
                System.arraycopy(list, 0, tmp, 0, list.length);
                res = tmp;
            } catch (ArrayStoreException e) {
                // one of the elements is not a String
            }
        }
        if (!(res instanceof String || res instanceof String[])) {
            throw new DocumentRouteException(
                    "Can not evaluate task assignees from " + taskAssigneesVar);
        }
        if (res instanceof String) {
            taskAssignees.add((String) res);
        } else {
            taskAssignees.addAll(Arrays.asList((String[]) res));
        }
        return taskAssignees;
    }

    @Override
    public boolean canMerge() {
        try {
            int n = 0;
            List<Transition> inputTransitions = getInputTransitions();

            for (Transition t : inputTransitions) {
                if (t.result) {
                    n++;
                }
            }
            String merge = (String) getProperty(PROP_MERGE);
            if (MERGE_ONE.equals(merge)) {
                return n > 0;
            } else if (MERGE_ALL.equals(merge)) {
                return n == inputTransitions.size();
            } else {
                throw new ClientRuntimeException("Illegal merge mode '" + merge
                        + "' for node " + this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Transition> getInputTransitions() {
        return inputTransitions;
    }

    @Override
    public void cancelTasks() {
        CoreSession session = getSession();
        TaskService taskService = Framework.getLocalService(TaskService.class);
        String routeDocId = getDocumentRoute(session).getDocument().getId();
        DocumentRoutingService routingService = Framework.getLocalService(DocumentRoutingService.class);
        try {
            List<Task> tasks = taskService.getAllTaskInstances(routeDocId,
                    getId(), session);
            for (Task task : tasks) {
                routingService.cancelTask(session, routeDocId, task.getId());
            }
            session.save();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public List<Button> getTaskButtons() {
        if (taskButtons == null) {
            taskButtons = computeTaskButtons();
        }
        return taskButtons;
    }

    protected List<Button> computeTaskButtons() {
        try {
            ListProperty props = (ListProperty) document.getProperty(PROP_TASK_BUTTONS);
            List<Button> btns = new ArrayList<Button>(props.size());
            for (Property p : props) {
                btns.add(new Button(this, p));
            }
            Collections.sort(btns);
            return btns;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void setButton(String status) {
        try {
            document.setPropertyValue(PROP_NODE_BUTTON, status);
            saveDocument();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void setLastActor(String actor) {
        try {
            document.setPropertyValue(PROP_NODE_LAST_ACTOR, actor);
            saveDocument();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void addTaskAssignees(List<String> taskAssignees) {
        List<String> allTasksAssignees = getTaskAssignees();
        allTasksAssignees.addAll(taskAssignees);
        try {
            document.setPropertyValue(PROP_TASK_ASSIGNEES,
                    (Serializable) allTasksAssignees);
            saveDocument();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public String getTaskDocType() {
        String taskDocType = (String) getProperty(PROP_TASK_DOC_TYPE);
        if (StringUtils.isEmpty(taskDocType)) {
            taskDocType = TaskConstants.TASK_TYPE_NAME;
        }
        return taskDocType;
    }

    protected Date evaluateDueDate() throws DocumentRouteException {
        String taskDueDateExpr = getTaskDueDateExpr();
        if (StringUtils.isEmpty(taskDueDateExpr)) {
            return new Date();
        }
        OperationContext context = getExecutionContext(getSession());
        Expression expr = Scripting.newExpression(taskDueDateExpr);
        Object res = null;
        try {
            res = expr.eval(context);
        } catch (InterruptedException e) {
            // restore interrupted state
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentRouteException("Error evaluating task due date: "
                    + taskDueDateExpr, e);
        }
        if (res instanceof DateWrapper) {
            return ((DateWrapper) res).getDate();
        } else if (res instanceof Date) {
            return (Date) res;
        } else if (res instanceof Calendar) {
            return ((Calendar) res).getTime();
        } else if (res instanceof String) {
            return DateParser.parseW3CDateTime((String) res);
        } else
            throw new DocumentRouteException(
                    "The following expression can not be evaluated to a date: "
                            + taskDueDateExpr);

    }

    @Override
    public Date computeTaskDueDate() throws DocumentRouteException {
        Date dueDate = evaluateDueDate();
        try {
            document.setPropertyValue(PROP_TASK_DUE_DATE, dueDate);
            CoreSession session = document.getCoreSession();
            session.saveDocument(document);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
        return dueDate;
    }

    @Override
    public boolean executeOnlyFirstTransition() {
        return getBoolean(PROP_EXECUTE_ONLY_FIRST_TRANSITION);
    }

    @Override
    public boolean hasSubRoute() throws DocumentRouteException {
        return getSubRouteModelId() != null;
    }

    @Override
    public String getSubRouteModelId() throws DocumentRouteException {
        String subRouteModelExpr = (String) getProperty(PROP_SUB_ROUTE_MODEL_EXPR);
        if (StringUtils.isBlank(subRouteModelExpr)) {
            return null;
        }
        OperationContext context = getExecutionContext(getSession());
        String res = valueOrExpression(String.class, subRouteModelExpr,
                context, "Sub-workflow id expression");
        return StringUtils.defaultIfBlank((String) res, null);
    }

    protected String getSubRouteInstanceId() {
        return (String) getProperty(GraphNode.PROP_SUB_ROUTE_INSTANCE_ID);
    }

    @Override
    public DocumentRoute startSubRoute() throws DocumentRouteException {
        String subRouteModelId = getSubRouteModelId();
        // create the instance without starting it
        DocumentRoutingService service = Framework.getLocalService(DocumentRoutingService.class);
        List<String> docs = graph.getAttachedDocuments();
        String subRouteInstanceId = service.createNewInstance(subRouteModelId,
                docs, getSession(), false);
        try {
            // set info about parent in subroute
            DocumentModel subRouteInstance = getSession().getDocument(
                    new IdRef(subRouteInstanceId));
            subRouteInstance.setPropertyValue(GraphRoute.PROP_PARENT_ROUTE,
                    getDocument().getParentRef().toString());
            subRouteInstance.setPropertyValue(GraphRoute.PROP_PARENT_NODE,
                    getDocument().getName());
            subRouteInstance = getSession().saveDocument(subRouteInstance);
            // set info about subroute in parent
            document.setPropertyValue(PROP_SUB_ROUTE_INSTANCE_ID,
                    subRouteInstanceId);
            saveDocument();
            // start the sub-route
            Map<String, Serializable> map = getSubRouteInitialVariables();
            service.startInstance(subRouteInstanceId, docs, map, getSession());
            // return the sub-route
            // subRouteInstance.refresh();
            DocumentRoute subRoute = subRouteInstance.getAdapter(DocumentRoute.class);
            return subRoute;
        } catch (ClientException e) {
            throw new DocumentRouteException(e);
        }
    }

    protected Map<String, Serializable> getSubRouteInitialVariables()
            throws ClientException {
        ListProperty props = (ListProperty) document.getProperty(PROP_SUB_ROUTE_VARS);
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        OperationContext context = null;
        for (Property p : props) {
            MapProperty prop = (MapProperty) p;
            String key = (String) prop.get(PROP_KEYVALUE_KEY).getValue();
            String v = (String) prop.get(PROP_KEYVALUE_VALUE).getValue();
            if (context == null) {
                context = getExecutionContext(getSession());
            }
            Serializable value = valueOrExpression(Serializable.class, v,
                    context, "Sub-workflow variable expression");
            map.put(key, value);
        }
        return map;
    }

    /*
     * Code similar to the one in OperationChainContribution.
     */
    protected <T> T valueOrExpression(Class<T> klass, String v,
            OperationContext context, String kind)
            throws DocumentRouteException {
        if (!v.startsWith(EXPR_PREFIX)) {
            return (T) v;
        }
        v = v.substring(EXPR_PREFIX.length()).trim();
        Expression expr;
        if (v.contains(TEMPLATE_START)) {
            expr = Scripting.newTemplate(v);
        } else {
            expr = Scripting.newExpression(v);
        }
        Object res = null;
        try {
            res = expr.eval(context);
            // stupid eval() method throws generic Exception
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted state
            throw new RuntimeException(e);
        } catch (CompileException e) {
            throw new DocumentRouteException("Error evaluating expression: "
                    + v, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentRouteException("Error evaluating expression: "
                    + v, e);
        }
        if (!(klass.isAssignableFrom(res.getClass()))) {
            throw new DocumentRouteException(kind + " of node '" + getId()
                    + "' of graph '" + graph.getName()
                    + "' does not evaluate to " + klass.getSimpleName()
                    + " but " + res.getClass().getName() + ": " + v);
        }
        return (T) res;
    }

    @Override
    public void cancelSubRoute() throws DocumentRouteException {
        String subRouteInstanceId = getSubRouteInstanceId();
        if (!StringUtils.isEmpty(subRouteInstanceId)) {
            try {
                DocumentModel subRouteDoc = getSession().getDocument(
                        new IdRef(subRouteInstanceId));
                DocumentRoute subRoute = subRouteDoc.getAdapter(DocumentRoute.class);
                subRoute.cancel(getSession());
            } catch (ClientException e) {
                throw new DocumentRouteException(e);
            }
        }
    }

    protected List<EscalationRule> computeEscalationRules() {
        try {
            ListProperty props = (ListProperty) document.getProperty(PROP_ESCALATION_RULES);
            List<EscalationRule> rules = new ArrayList<EscalationRule>(
                    props.size());
            for (Property p : props) {
                rules.add(new EscalationRule(this, p));
            }
            Collections.sort(rules);
            return rules;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public List<EscalationRule> getEscalationRules() {
        if (escalationRules == null) {
            escalationRules = computeEscalationRules();
        }
        return escalationRules;
    }

    @Override
    public List<EscalationRule> evaluateEscalationRules() {
        try {
            List<EscalationRule> rulesToExecute = new ArrayList<EscalationRule>();
            OperationContext context = getExecutionContext(getSession());
            for (EscalationRule rule : getEscalationRules()) {
                Expression expr = Scripting.newExpression(rule.condition);
                Object res = null;
                try {
                    res = expr.eval(context);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new DocumentRouteException(
                            "Error evaluating condition: " + rule.condition, e);
                }
                if (!(res instanceof Boolean)) {
                    throw new DocumentRouteException("Condition for rule "
                            + rule + " of node '" + getId() + "' of graph '"
                            + graph.getName()
                            + "' does not evaluate to a boolean: "
                            + rule.condition);
                }
                boolean bool = Boolean.TRUE.equals(res);
                if ((!rule.isExecuted() || rule.isMultipleExecution()) && bool) {
                    rulesToExecute.add(rule);
                }
            }
            return rulesToExecute;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public boolean hasMultipleTasks() {
        return getBoolean(PROP_HAS_MULTIPLE_TASKS);
    }

    protected List<TaskInfo> computeTasksInfo() {
        try {
            ListProperty props = (ListProperty) document.getProperty(PROP_TASKS_INFO);
            List<TaskInfo> tasks = new ArrayList<TaskInfo>(props.size());
            for (Property p : props) {
                tasks.add(new TaskInfo(this, p));
            }
            return tasks;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public List<TaskInfo> getTasksInfo() {
        if (tasksInfo == null) {
            tasksInfo = computeTasksInfo();
        }
        return tasksInfo;
    }

    @Override
    public void addTaskInfo(String taskId) throws ClientException {
        getTasksInfo().add(new TaskInfo(this, taskId));
        saveDocument();
    }

    @Override
    public void updateTaskInfo(String taskId, String status, String actor,
            String comment) throws ClientException {
        boolean updated = false;
        List<TaskInfo> tasksInfo = getTasksInfo();
        for (TaskInfo taskInfo : tasksInfo) {
            if (taskId.equals(taskInfo.getTaskDocId())) {
                taskInfo.setComment(comment);
                taskInfo.setStatus(status);
                taskInfo.setActor(actor);
                updated = true;
            }
        }
        // handle backward compatibility
        if (!updated) {
            // task created before 5.7.3;
            TaskInfo ti = new TaskInfo(this, taskId);
            ti.setActor(actor);
            ti.setStatus(status);
            ti.setComment(comment);
            getTasksInfo().add(ti);
        }
        saveDocument();
    }

    @Override
    public List<TaskInfo> getEndedTasksInfo() {
        List<TaskInfo> tasksInfo = getTasksInfo();
        List<TaskInfo> endedTasks = new ArrayList<TaskInfo>();
        for (TaskInfo taskInfo : tasksInfo) {
            if (taskInfo.getStatus() != null) {
                endedTasks.add(taskInfo);
            }
        }
        return endedTasks;
    }

    @Override
    public boolean hasOpenTasks() {
        return getTasksInfo().size() != getEndedTasksInfo().size();
    }

    @Override
    public List<TaskInfo> getProcessedTasksInfo() {
        List<TaskInfo> tasksInfo = getTasksInfo();
        List<TaskInfo> processedTasks = new ArrayList<TaskInfo>();
        for (TaskInfo taskInfo : tasksInfo) {
            if (taskInfo.getStatus() != null
                    && !"".equals(taskInfo.getStatus())) {
                processedTasks.add(taskInfo);
            }
        }
        return processedTasks;
    }
}