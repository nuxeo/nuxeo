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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.scripting.DateWrapper;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.api.validation.DocumentValidationReport;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.api.TasksInfoWrapper;
import org.nuxeo.ecm.platform.routing.core.api.scripting.RoutingScriptingExpression;
import org.nuxeo.ecm.platform.routing.core.api.scripting.RoutingScriptingFunctions;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Graph Node implementation as an adapter over a DocumentModel.
 *
 * @since 5.6
 */
public class GraphNodeImpl extends DocumentRouteElementImpl implements GraphNode {

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
        inputTransitions = new ArrayList<>(2);
    }

    /**
     * @since 5.7.2
     */
    public GraphNodeImpl(DocumentModel doc) {
        super(doc, new GraphRunner());
        graph = (GraphRouteImpl) getDocumentRoute(doc.getCoreSession());
        inputTransitions = new ArrayList<>(2);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(getId()).toString();
    }

    protected boolean getBoolean(String propertyName) {
        return Boolean.TRUE.equals(getProperty(propertyName));
    }

    protected void incrementProp(String prop) {
        Long count = (Long) getProperty(prop);
        if (count == null) {
            count = Long.valueOf(0);
        }
        document.setPropertyValue(prop, Long.valueOf(count.longValue() + 1));
        saveDocument();
    }

    protected CoreSession getSession() {
        return document.getCoreSession();
    }

    protected void saveDocument() {
        getSession().saveDocument(document);
    }

    @Override
    public String getId() {
        return (String) getProperty(PROP_NODE_ID);
    }

    @Override
    public State getState() {
        if (localState != null) {
            return localState;
        }
        String s = document.getCurrentLifeCycleState();
        return State.fromString(s);
    }

    @Override
    public void setState(State state) {
        if (state == null) {
            throw new NullPointerException("null state");
        }
        String lc = state.getLifeCycleState();
        if (lc == null) {
            localState = state;
        } else {
            localState = null;
            String oldLc = document.getCurrentLifeCycleState();
            if (lc.equals(oldLc)) {
                return;
            }
            document.followTransition(state.getTransition());
            saveDocument();
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
        // Allow input transitions reevaluation (needed for loop case)
        for (Transition t : inputTransitions) {
            t.setResult(false);
            getSession().saveDocument(t.source.getDocument());
        }
        // Increment node counter
        incrementProp(PROP_COUNT);
        document.setPropertyValue(PROP_NODE_START_DATE, Calendar.getInstance());
        // reset taskInfo property
        tasksInfo = null;
        document.setPropertyValue(PROP_TASKS_INFO, new ArrayList<TaskInfo>());
        saveDocument();
    }

    @Override
    public void ending() {
        document.setPropertyValue(PROP_NODE_END_DATE, Calendar.getInstance());
        saveDocument();
    }

    @Override
    public Map<String, Serializable> getVariables() {
        return GraphVariablesUtil.getVariables(document, PROP_VARIABLES_FACET);
    }

    @Override
    public Map<String, Serializable> getJsonVariables() {
        return GraphVariablesUtil.getVariables(document, PROP_VARIABLES_FACET, true);
    }

    @Override
    public void setVariables(Map<String, Serializable> map) {
        GraphVariablesUtil.setVariables(document, PROP_VARIABLES_FACET, map);
    }

    @Override
    public void setJSONVariables(Map<String, String> map) {
        GraphVariablesUtil.setJSONVariables(document, PROP_VARIABLES_FACET, map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAllVariables(Map<String, Object> map) {
        setAllVariables(map, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAllVariables(Map<String, Object> map, final boolean allowGlobalVariablesAssignement) {
        if (map == null) {
            return;
        }
        Boolean mapToJSON = Boolean.FALSE;
        if (map.containsKey(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON)
                && (Boolean) map.get(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON)) {
            mapToJSON = Boolean.TRUE;
        }

        // get variables from node and graph
        Map<String, Serializable> graphVariables = mapToJSON ? graph.getJsonVariables() : graph.getVariables();
        Map<String, Serializable> nodeVariables = mapToJSON ? getJsonVariables() : getVariables();
        Map<String, Serializable> changedGraphVariables = new HashMap<>();
        Map<String, Serializable> changedNodeVariables = new HashMap<>();

        // set variables back into node and graph
        if (map.get(Constants.VAR_WORKFLOW_NODE) != null) {
            for (Entry<String, Serializable> es : ((Map<String, Serializable>) map.get(
                    Constants.VAR_WORKFLOW_NODE)).entrySet()) {
                String key = es.getKey();
                Serializable value = es.getValue();
                if (nodeVariables.containsKey(key)) {
                    Serializable oldValue = nodeVariables.get(key);
                    if (!equality(value, oldValue)) {
                        changedNodeVariables.put(key, value);
                    }
                }
            }
        }
        final String transientSchemaName = DocumentRoutingConstants.GLOBAL_VAR_SCHEMA_PREFIX + getId();
        final SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (map.get(Constants.VAR_WORKFLOW) != null) {
            final Schema transientSchema = schemaManager.getSchema(transientSchemaName);
            for (Entry<String, Serializable> es : ((Map<String, Serializable>) map.get(
                    Constants.VAR_WORKFLOW)).entrySet()) {
                String key = es.getKey();
                Serializable value = es.getValue();
                if (graphVariables.containsKey(key)) {
                    Serializable oldValue = graphVariables.get(key);
                    if (!equality(value, oldValue)) {
                        if (!allowGlobalVariablesAssignement && transientSchema != null
                                && !transientSchema.hasField(key)) {
                            throw new DocumentRouteException(String.format(
                                    "You don't have the permission to set the workflow variable %s", key));
                        }
                        changedGraphVariables.put(key, value);
                    }
                }
            }
        }

        if (!allowGlobalVariablesAssignement) {
            // Validation
            final DocumentModel transientDocumentModel = new DocumentModelImpl(getDocument().getType());
            transientDocumentModel.copyContent(document);
            final String transientFacetName = "facet-" + transientSchemaName;
            CompositeType transientFacet = schemaManager.getFacet(transientFacetName);
            if (transientFacet != null) {
                changedGraphVariables.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, mapToJSON);
                transientDocumentModel.addFacet("facet-" + transientSchemaName);
                GraphVariablesUtil.setVariables(transientDocumentModel, "facet-" + transientSchemaName,
                        changedGraphVariables, false);
            }
            changedNodeVariables.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, mapToJSON);
            GraphVariablesUtil.setVariables(transientDocumentModel, PROP_VARIABLES_FACET, changedNodeVariables, false);
            DocumentValidationService documentValidationService = Framework.getService(DocumentValidationService.class);
            DocumentValidationReport report = documentValidationService.validate(transientDocumentModel);
            if (report.hasError()) {
                throw new DocumentValidationException(report);
            }
        }

        if (!changedNodeVariables.isEmpty()) {
            changedNodeVariables.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, mapToJSON);
            setVariables(changedNodeVariables);
        }
        if (!changedGraphVariables.isEmpty()) {
            changedGraphVariables.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, mapToJSON);
            graph.setVariables(changedGraphVariables);
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
    public Map<String, Serializable> getWorkflowContextualInfo(CoreSession session, boolean detached) {
        Map<String, Serializable> context = new HashMap<>();
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
                documentModel.detach(true);
            }
        }
        context.put("workflowDocuments", documents);
        context.put("documents", documents);
        // node context
        String button = (String) getProperty(PROP_NODE_BUTTON);
        Map<String, Serializable> nodeVariables = getVariables();
        nodeVariables.put("button", button);
        nodeVariables.put("numberOfProcessedTasks", getProcessedTasksInfo().size());
        nodeVariables.put("numberOfTasks", getTasksInfo().size());
        nodeVariables.put("tasks", new TasksInfoWrapper(getTasksInfo()));
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
        return (String) graph.getDocument().getPropertyValue(DocumentRoutingConstants.INITIATOR);
    }

    protected Calendar getWorkflowStartTime() {
        return (Calendar) graph.getDocument().getPropertyValue("dc:created");
    }

    protected String getWorkflowParentRouteId() {
        return (String) graph.getDocument().getPropertyValue(GraphRoute.PROP_PARENT_ROUTE);
    }

    protected String getWorkflowParentNodeId() {
        return (String) graph.getDocument().getPropertyValue(GraphRoute.PROP_PARENT_NODE);
    }

    protected Calendar getNodeStartTime() {
        return (Calendar) getDocument().getPropertyValue(PROP_NODE_START_DATE);
    }

    protected Calendar getNodeEndTime() {
        return (Calendar) getDocument().getPropertyValue(PROP_NODE_END_DATE);
    }

    protected String getNodeLastActor() {
        return (String) getDocument().getPropertyValue(PROP_NODE_LAST_ACTOR);
    }

    @Override
    public void executeChain(String chainId) throws DocumentRouteException {
        executeChain(chainId, null);
    }

    @Override
    public void executeTransitionChain(Transition transition) throws DocumentRouteException {
        executeChain(transition.chain, transition.id);
    }

    public void executeChain(String chainId, String transitionId) throws DocumentRouteException {
        // TODO events
        if (StringUtils.isEmpty(chainId)) {
            return;
        }

        // get base context
        try (OperationContext context = getExecutionContext(getSession())) {
            if (transitionId != null) {
                context.put("transition", transitionId);
            }

            AutomationService automationService = Framework.getService(AutomationService.class);
            automationService.run(context, chainId);

            setAllVariables(context);
        } catch (OperationException e) {
            throw new DocumentRouteException("Error running chain: " + chainId, e);
        }
    }

    @Override
    public void initAddInputTransition(Transition transition) {
        inputTransitions.add(transition);
    }

    protected List<Transition> computeOutputTransitions() {
        ListProperty props = (ListProperty) document.getProperty(PROP_TRANSITIONS);
        List<Transition> trans = new ArrayList<>(props.size());
        for (Property p : props) {
            trans.add(new Transition(this, p));
        }
        return trans;
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
        List<Transition> trueTrans = new ArrayList<>();
        for (Transition t : getOutputTransitions()) {
            try (OperationContext context = getExecutionContext(getSession())) {
                context.put("transition", t.id);
                Expression expr = new RoutingScriptingExpression(t.condition, new RoutingScriptingFunctions(context));
                Object res = expr.eval(context);
                // stupid eval() method throws generic Exception
                if (!(res instanceof Boolean)) {
                    throw new DocumentRouteException("Condition for transition " + t + " of node '" + getId()
                            + "' of graph '" + graph.getName() + "' does not evaluate to a boolean: " + t.condition);
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
                saveDocument();
            } catch (DocumentRouteException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new DocumentRouteException("Error evaluating condition: " + t.condition, e);
            }
        }
        return trueTrans;
    }

    @Override
    public List<String> evaluateTaskAssignees() throws DocumentRouteException {
        List<String> taskAssignees = new ArrayList<>();
        String taskAssigneesVar = getTaskAssigneesVar();
        if (StringUtils.isEmpty(taskAssigneesVar)) {
            return taskAssignees;
        }
        try (OperationContext context = getExecutionContext(getSession())) {
            Expression expr = Scripting.newExpression(taskAssigneesVar);
            Object res = expr.eval(context);

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
                throw new DocumentRouteException("Can not evaluate task assignees from " + taskAssigneesVar);
            }
            if (res instanceof String) {
                taskAssignees.add((String) res);
            } else {
                taskAssignees.addAll(Arrays.asList((String[]) res));
            }
        } catch (DocumentRouteException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DocumentRouteException("Error evaluating task assignees: " + taskAssigneesVar, e);
        }
        return taskAssignees;
    }

    @Override
    public boolean canMerge() {
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
            throw new NuxeoException("Illegal merge mode '" + merge + "' for node " + this);
        }
    }

    @Override
    public List<Transition> getInputTransitions() {
        return inputTransitions;
    }

    @Override
    public void cancelTasks() {
        CoreSession session = getSession();
        List<TaskInfo> tasks = getTasksInfo();
        for (TaskInfo task : tasks) {
            if (!task.isEnded()) {
                cancelTask(session, task.getTaskDocId());
            }
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
        ListProperty props = (ListProperty) document.getProperty(PROP_TASK_BUTTONS);
        List<Button> btns = new ArrayList<>(props.size());
        for (Property p : props) {
            btns.add(new Button(this, p));
        }
        Collections.sort(btns);
        return btns;
    }

    @Override
    public void setButton(String status) {
        document.setPropertyValue(PROP_NODE_BUTTON, status);
        saveDocument();
    }

    @Override
    public void setLastActor(String actor) {
        document.setPropertyValue(PROP_NODE_LAST_ACTOR, actor);
        saveDocument();
    }

    protected void addTaskAssignees(List<String> taskAssignees) {
        List<String> allTasksAssignees = getTaskAssignees();
        allTasksAssignees.addAll(taskAssignees);
        document.setPropertyValue(PROP_TASK_ASSIGNEES, (Serializable) allTasksAssignees);
        saveDocument();
    }

    @Override
    public String getTaskDocType() {
        String taskDocType = (String) getProperty(PROP_TASK_DOC_TYPE);
        if (StringUtils.isEmpty(taskDocType) || TaskConstants.TASK_TYPE_NAME.equals(taskDocType)) {
            taskDocType = DocumentRoutingConstants.ROUTING_TASK_DOC_TYPE;
        }
        return taskDocType;
    }

    protected Date evaluateDueDate() throws DocumentRouteException {
        String taskDueDateExpr = getTaskDueDateExpr();
        if (StringUtils.isEmpty(taskDueDateExpr)) {
            return new Date();
        }
        try (OperationContext context = getExecutionContext(getSession())) {
            Expression expr = Scripting.newExpression(taskDueDateExpr);
            Object res = expr.eval(context);
            if (res instanceof DateWrapper) {
                return ((DateWrapper) res).getDate();
            } else if (res instanceof Date) {
                return (Date) res;
            } else if (res instanceof Calendar) {
                return ((Calendar) res).getTime();
            } else if (res instanceof String) {
                return DateParser.parseW3CDateTime((String) res);
            } else {
                throw new DocumentRouteException(
                        "The following expression can not be evaluated to a date: " + taskDueDateExpr);
            }
        } catch (DocumentRouteException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DocumentRouteException("Error evaluating task due date: " + taskDueDateExpr, e);
        }
    }

    @Override
    public Date computeTaskDueDate() throws DocumentRouteException {
        Date dueDate = evaluateDueDate();
        document.setPropertyValue(PROP_TASK_DUE_DATE, dueDate);
        CoreSession session = document.getCoreSession();
        session.saveDocument(document);
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
        try (OperationContext context = getExecutionContext(getSession())) {
            String res = valueOrExpression(String.class, subRouteModelExpr, context, "Sub-workflow id expression");
            return StringUtils.defaultIfBlank(res, null);
        }
    }

    protected String getSubRouteInstanceId() {
        return (String) getProperty(GraphNode.PROP_SUB_ROUTE_INSTANCE_ID);
    }

    @Override
    public DocumentRoute startSubRoute() throws DocumentRouteException {
        String subRouteModelId = getSubRouteModelId();
        // create the instance without starting it
        DocumentRoutingService service = Framework.getService(DocumentRoutingService.class);
        List<String> docs = graph.getAttachedDocuments();
        String subRouteInstanceId = service.createNewInstance(subRouteModelId, docs, getSession(), false);
        // set info about parent in subroute
        DocumentModel subRouteInstance = getSession().getDocument(new IdRef(subRouteInstanceId));
        subRouteInstance.setPropertyValue(GraphRoute.PROP_PARENT_ROUTE, getDocument().getParentRef().toString());
        subRouteInstance.setPropertyValue(GraphRoute.PROP_PARENT_NODE, getDocument().getName());
        subRouteInstance = getSession().saveDocument(subRouteInstance);
        // set info about subroute in parent
        document.setPropertyValue(PROP_SUB_ROUTE_INSTANCE_ID, subRouteInstanceId);
        saveDocument();
        // start the sub-route
        Map<String, Serializable> map = getSubRouteInitialVariables();
        service.startInstance(subRouteInstanceId, docs, map, getSession());
        // return the sub-route
        // subRouteInstance.refresh();
        DocumentRoute subRoute = subRouteInstance.getAdapter(DocumentRoute.class);
        return subRoute;
    }

    protected Map<String, Serializable> getSubRouteInitialVariables() {
        ListProperty props = (ListProperty) document.getProperty(PROP_SUB_ROUTE_VARS);
        Map<String, Serializable> map = new HashMap<>();
        try (OperationContext context = getExecutionContext(getSession())) {
            for (Property p : props) {
                MapProperty prop = (MapProperty) p;
                String key = (String) prop.get(PROP_KEYVALUE_KEY).getValue();
                String v = (String) prop.get(PROP_KEYVALUE_VALUE).getValue();
                Serializable value = valueOrExpression(Serializable.class, v, context,
                        "Sub-workflow variable expression");
                map.put(key, value);
            }
        }
        return map;
    }

    /*
     * Code similar to the one in OperationChainContribution.
     */
    protected <T> T valueOrExpression(Class<T> klass, String v, OperationContext context, String kind)
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
        } catch (RuntimeException e) {
            throw new DocumentRouteException("Error evaluating expression: " + v, e);
        }
        if (!(klass.isAssignableFrom(res.getClass()))) {
            throw new DocumentRouteException(
                    kind + " of node '" + getId() + "' of graph '" + graph.getName() + "' does not evaluate to "
                            + klass.getSimpleName() + " but " + res.getClass().getName() + ": " + v);
        }
        return (T) res;
    }

    @Override
    public void cancelSubRoute() throws DocumentRouteException {
        String subRouteInstanceId = getSubRouteInstanceId();
        if (!StringUtils.isEmpty(subRouteInstanceId)) {
            DocumentModel subRouteDoc = getSession().getDocument(new IdRef(subRouteInstanceId));
            DocumentRoute subRoute = subRouteDoc.getAdapter(DocumentRoute.class);
            subRoute.cancel(getSession());
        }
    }

    protected List<EscalationRule> computeEscalationRules() {
        ListProperty props = (ListProperty) document.getProperty(PROP_ESCALATION_RULES);
        List<EscalationRule> rules = new ArrayList<>(props.size());
        for (Property p : props) {
            rules.add(new EscalationRule(this, p));
        }
        Collections.sort(rules);
        return rules;
    }

    @Override
    public List<EscalationRule> getEscalationRules() {
        if (escalationRules == null) {
            escalationRules = computeEscalationRules();
        }
        return escalationRules;
    }

    @Override
    public List<EscalationRule> evaluateEscalationRules() {
        List<EscalationRule> rulesToExecute = new ArrayList<>();
        // add specific helpers for escalation
        for (EscalationRule rule : getEscalationRules()) {
            try (OperationContext context = getExecutionContext(getSession())) {
                Expression expr = new RoutingScriptingExpression(rule.condition,
                        new RoutingScriptingFunctions(context, rule));
                Object res = expr.eval(context);
                if (!(res instanceof Boolean)) {
                    throw new DocumentRouteException("Condition for rule " + rule + " of node '" + getId()
                            + "' of graph '" + graph.getName() + "' does not evaluate to a boolean: " + rule.condition);
                }
                boolean bool = Boolean.TRUE.equals(res);
                if ((!rule.isExecuted() || rule.isMultipleExecution()) && bool) {
                    rulesToExecute.add(rule);
                }
            } catch (DocumentRouteException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new DocumentRouteException("Error evaluating condition: " + rule.condition, e);
            }
        }
        saveDocument();
        return rulesToExecute;
    }

    @Override
    public boolean hasMultipleTasks() {
        return getBoolean(PROP_HAS_MULTIPLE_TASKS);
    }

    protected List<TaskInfo> computeTasksInfo() {
        ListProperty props = (ListProperty) document.getProperty(PROP_TASKS_INFO);
        List<TaskInfo> tasks = new ArrayList<>(props.size());
        for (Property p : props) {
            tasks.add(new TaskInfo(this, p));
        }
        return tasks;
    }

    @Override
    public List<TaskInfo> getTasksInfo() {
        if (tasksInfo == null) {
            tasksInfo = computeTasksInfo();
        }
        return tasksInfo;
    }

    @Override
    public void addTaskInfo(String taskId) {
        getTasksInfo().add(new TaskInfo(this, taskId));
        saveDocument();
    }

    @Override
    public void removeTaskInfo(String taskId) {
        ListProperty props = (ListProperty) document.getProperty(PROP_TASKS_INFO);
        Property propertytoBeRemoved = null;
        for (Property p : props) {
            if (taskId.equals(p.get(PROP_TASK_INFO_TASK_DOC_ID).getValue())) {
                propertytoBeRemoved = p;
                break;
            }
        }
        if (propertytoBeRemoved != null) {
            props.remove(propertytoBeRemoved);
            saveDocument();
            tasksInfo = null;
        }
    }

    @Override
    public void updateTaskInfo(String taskId, boolean ended, String status, String actor, String comment) {
        boolean updated = false;
        List<TaskInfo> tasksInfo = getTasksInfo();
        for (TaskInfo taskInfo : tasksInfo) {
            if (taskId.equals(taskInfo.getTaskDocId())) {
                taskInfo.setComment(comment);
                taskInfo.setStatus(status);
                taskInfo.setActor(actor);
                taskInfo.setEnded(true);
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
            ti.setEnded(true);
            getTasksInfo().add(ti);
        }
        saveDocument();
    }

    @Override
    public List<TaskInfo> getEndedTasksInfo() {
        List<TaskInfo> tasksInfo = getTasksInfo();
        List<TaskInfo> endedTasks = new ArrayList<>();
        for (TaskInfo taskInfo : tasksInfo) {
            if (taskInfo.isEnded()) {
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
        List<TaskInfo> processedTasks = new ArrayList<>();
        for (TaskInfo taskInfo : tasksInfo) {
            if (taskInfo.isEnded() && taskInfo.getStatus() != null) {
                processedTasks.add(taskInfo);
            }
        }
        return processedTasks;
    }

    @Override
    public boolean allowTaskReassignment() {
        return getBoolean(PROP_ALLOW_TASK_REASSIGNMENT);

    }

    protected void cancelTask(CoreSession session, final String taskId) throws DocumentRouteException {
        DocumentRef taskRef = new IdRef(taskId);
        if (!session.exists(taskRef)) {
            log.info(String.format("Task with id %s does not exist anymore", taskId));
            DocumentModelList docs = graph.getAttachedDocumentModels();
            Framework.getService(DocumentRoutingService.class).removePermissionsForTaskActors(session, docs, taskId);
            NuxeoPrincipal principal = session.getPrincipal();
            String actor = principal.getActingUser();
            updateTaskInfo(taskId, true, null, actor, null);
            return;
        }
        DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
        Task task = taskDoc.getAdapter(Task.class);
        if (task == null) {
            throw new DocumentRouteException("Invalid taskId: " + taskId);
        }
        DocumentModelList docs = graph.getAttachedDocumentModels();
        Framework.getService(DocumentRoutingService.class).removePermissionsForTaskActors(session, docs, task);
        if (task.isOpened()) {
            task.cancel(session);
        }
        session.saveDocument(task.getDocument());
        // task is considered processed with the status "null" when is
        // canceled
        // actor
        NuxeoPrincipal principal = session.getPrincipal();
        String actor = principal.getActingUser();
        updateTaskInfo(taskId, true, null, actor, null);
    }

    @Override
    public void setVariable(String name, String value) {
        Map<String, Serializable> nodeVariables = getVariables();
        if (nodeVariables.containsKey(name)) {
            nodeVariables.put(name, value);
            setVariables(nodeVariables);
        }
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean hasTaskButton(String name) {
        for (Button button : getTaskButtons()) {
            if (button.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

}
