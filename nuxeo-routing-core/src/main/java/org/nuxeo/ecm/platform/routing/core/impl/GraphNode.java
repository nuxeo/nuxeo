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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;

/**
 * A node for a route graph. Represents operation chains, associated task and form, output transitions and their
 * conditions, etc.
 *
 * @since 5.6
 */
public interface GraphNode {

    String MERGE_ONE = "one";

    String MERGE_ALL = "all";

    String PROP_NODE_ID = "rnode:nodeId";

    String PROP_TITLE = "dc:title";

    String PROP_START = "rnode:start";

    String PROP_STOP = "rnode:stop";

    String PROP_MERGE = "rnode:merge";

    String PROP_COUNT = "rnode:count";

    String PROP_CANCELED = "rnode:canceled";

    String PROP_INPUT_CHAIN = "rnode:inputChain";

    String PROP_OUTPUT_CHAIN = "rnode:outputChain";

    String PROP_HAS_TASK = "rnode:hasTask";

    String PROP_VARIABLES_FACET = "rnode:variablesFacet";

    String PROP_TRANSITIONS = "rnode:transitions";

    String PROP_TRANS_NAME = "name";

    String PROP_TRANS_TARGET = "targetId";

    String PROP_TRANS_CONDITION = "condition";

    String PROP_TRANS_RESULT = "result";

    String PROP_TRANS_CHAIN = "chain";

    String PROP_TRANS_LABEL = "label";

    /**
     * @since 7.1 a transition can hold a custom path
     */
    String PROP_TRANS_PATH = "path";

    String PROP_TASK_ASSIGNEES = "rnode:taskAssignees";

    String PROP_TASK_ASSIGNEES_VAR = "rnode:taskAssigneesExpr";

    String PROP_TASK_ASSIGNEES_PERMISSION = "rnode:taskAssigneesPermission";

    String PROP_TASK_DUE_DATE = "rnode:taskDueDate";

    String PROP_TASK_DIRECTIVE = "rnode:taskDirective";

    String PROP_TASK_LAYOUT = "rnode:taskLayout";

    String PROP_TASK_BUTTONS = "rnode:taskButtons";

    String PROP_BTN_NAME = "name";

    String PROP_BTN_LABEL = "label";

    String PROP_BTN_FILTER = "filter";

    String PROP_NODE_X_COORDINATE = "rnode:taskX";

    String PROP_NODE_Y_COORDINATE = "rnode:taskY";

    /**
     * @since 5.7.3 a node can create multiple tasks, in this case, this stores the status of the last task ended
     */
    String PROP_NODE_BUTTON = "rnode:button";

    String PROP_NODE_START_DATE = "rnode:startDate";

    String PROP_NODE_END_DATE = "rnode:endDate";

    String PROP_NODE_LAST_ACTOR = "rnode:lastActor";

    String PROP_TASK_DOC_TYPE = "rnode:taskDocType";

    String PROP_TASK_NOTIFICATION_TEMPLATE = "rnode:taskNotificationTemplate";

    String PROP_TASK_DUE_DATE_EXPR = "rnode:taskDueDateExpr";

    /** @since 5.7.2 */
    String PROP_EXECUTE_ONLY_FIRST_TRANSITION = "rnode:executeOnlyFirstTransition";

    /**
     * The sub-route model id (expression) to run, if present.
     *
     * @since 5.7.2
     */
    String PROP_SUB_ROUTE_MODEL_EXPR = "rnode:subRouteModelExpr";

    /**
     * The sub-route instance id being run while this node is suspended.
     *
     * @since 5.7.2
     */
    String PROP_SUB_ROUTE_INSTANCE_ID = "rnode:subRouteInstanceId";

    /**
     * The sub-route variables to set (key/value list).
     *
     * @since 5.7.2
     */
    String PROP_SUB_ROUTE_VARS = "rnode:subRouteVariables";

    /** @since 5.7.2 */
    String PROP_KEYVALUE_KEY = "key";

    /** @since 5.7.2 */
    String PROP_KEYVALUE_VALUE = "value";

    // @since 5.7.2
    String PROP_ESCALATION_RULES = "rnode:escalationRules";

    // @since 5.7.2
    String PROP_ESCALATION_RULE_ID = "name";

    // @since 5.7.2
    String PROP_ESCALATION_RULE_LABEL = "label";

    // @since 5.7.2
    String PROP_ESCALATION_RULE_MULTIPLE_EXECUTION = "multipleExecution";

    // @since 5.7.2
    String PROP_ESCALATION_RULE_CONDITION = "condition";

    // @since 5.7.2
    String PROP_ESCALATION_RULE_CHAIN = "chain";

    // @since 5.7.2
    String PROP_ESCALATION_RULE_EXECUTED = "executed";

    // @since 5.9.3
    String PROP_LAST_EXECUTION_TIME = "executionDate";

    // @since 5.7.3
    String PROP_HAS_MULTIPLE_TASKS = "rnode:hasMultipleTasks";

    // @since 5.7.3
    String PROP_TASKS_INFO = "rnode:tasksInfo";

    // @since 5.7.3
    String PROP_TASK_INFO_ACTOR = "actor";

    // @since 5.7.3
    String PROP_TASK_INFO_COMMENT = "comment";

    // @since 5.7.3
    String PROP_TASK_INFO_STATUS = "status";

    // @since 5.7.3
    String PROP_TASK_INFO_ENDED = "ended";

    // @since 5.7.3
    String PROP_TASK_INFO_TASK_DOC_ID = "taskDocId";

    // @since 5.7.3
    String PROP_ALLOW_TASK_REASSIGNMENT = "rnode:allowTaskReassignment";

    // @since 5.6
    // if present as the node variable, acts as a built-in variable:
    // is passed to the task comment and logged in by audit;
    // internally rested when set on node having multiple tasks.
    String NODE_VARIABLE_COMMENT = "comment";

    /**
     * The internal state of a node.
     */
    enum State {
        /** Node is ready. */
        READY("ready", "toReady"),
        /** Merge node is waiting for more incoming transitions. */
        WAITING("waiting", "toWaiting"),
        /** While executing input phase. Not persisted. */
        RUNNING_INPUT,
        /** Task node is waiting for task to be done. */
        SUSPENDED("suspended", "toSuspended"),
        /** While executing output phase. Not persisted. */
        RUNNING_OUTPUT;

        private final String lifeCycleState;

        private final String transition;

        private State() {
            lifeCycleState = null;
            transition = null;
        }

        private State(String lifeCycleState, String transition) {
            this.lifeCycleState = lifeCycleState;
            this.transition = transition;
        }

        /**
         * Corresponding lifecycle state.
         */
        public String getLifeCycleState() {
            return lifeCycleState;
        }

        /**
         * Transition leading to this state.
         */
        public String getTransition() {
            return transition;
        }

        public static State fromString(String s) {
            try {
                return State.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(s);
            }
        }
    }

    /**
     * @since 7.1
     */
    class Point {

        public double x;

        public double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    class Transition implements Comparable<Transition>, Serializable {

        public GraphNode source;

        public MapProperty prop;

        public String id;

        public String condition;

        public String chain;

        public String target;

        public String label;

        public boolean result;

        /**
         * @since 7.1
         */
        public List<Point> path;

        /** Computed by graph. */
        public boolean loop;

        protected Transition(GraphNode source, Property p) {
            this.source = source;
            prop = (MapProperty) p;
            id = (String) prop.get(PROP_TRANS_NAME).getValue();
            condition = (String) prop.get(PROP_TRANS_CONDITION).getValue();
            chain = (String) prop.get(PROP_TRANS_CHAIN).getValue();
            target = (String) prop.get(PROP_TRANS_TARGET).getValue();
            label = (String) prop.get(PROP_TRANS_LABEL).getValue();
            Property resultProp = prop.get(PROP_TRANS_RESULT);
            if (resultProp != null) {
                result = BooleanUtils.isTrue(resultProp.getValue(Boolean.class));
            }
        }

        protected void setResult(boolean bool) {
            result = bool;
            prop.get(PROP_TRANS_RESULT).setValue(Boolean.valueOf(bool));
        }

        @Override
        public int compareTo(Transition other) {
            return id.compareTo(other.id);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("id", id).append("condition", condition).append("result", result).toString();
        }

        public String getTarget() {
            return target;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        /**
         * @since 7.1
         */
        public List<Point> getPath() {
            if (path == null) {
                path = computePath();
            }
            return path;
        }

        protected List<Point> computePath() {
            ListProperty props = (ListProperty) prop.get(PROP_TRANS_PATH);
            List<Point> points = new ArrayList<>(props.size());
            for (Property p : props) {
                points.add(new Point(
                    (Double) p.get("x").getValue(),
                    (Double) p.get("y").getValue()));
            }
            return points;
        }
    }

    public class Button implements Comparable<Button> {

        public GraphNode source;

        public String name;

        public String label;

        public String filter;

        public MapProperty prop;

        public Button(GraphNode source, Property p) {
            this.source = source;
            this.prop = (MapProperty) p;
            name = (String) prop.get(PROP_BTN_NAME).getValue();
            label = (String) prop.get(PROP_BTN_LABEL).getValue();
            filter = (String) prop.get(PROP_BTN_FILTER).getValue();
        }

        @Override
        public int compareTo(Button other) {
            return name.compareTo(other.name);
        }

        public String getLabel() {
            return label;
        }

        public String getName() {
            return name;
        }

        public String getFilter() {
            return filter;
        }
    }

    /**
     * @since 5.7.2
     */
    class EscalationRule implements Comparable<EscalationRule> {

        protected String id;

        protected String label;

        protected boolean multipleExecution;

        protected String condition;

        protected boolean executed;

        protected String chain;

        protected MapProperty prop;

        protected GraphNode node;

        /**
         * @since 5.9.3
         */
        protected Calendar lastExcutionTime;

        public EscalationRule(GraphNode node, Property p) {
            this.prop = (MapProperty) p;
            this.node = node;
            this.id = (String) p.get(PROP_ESCALATION_RULE_ID).getValue();
            this.label = (String) p.get(PROP_ESCALATION_RULE_LABEL).getValue();
            Property multipleEvaluationProp = prop.get(PROP_ESCALATION_RULE_MULTIPLE_EXECUTION);
            if (multipleEvaluationProp != null) {
                multipleExecution = BooleanUtils.isTrue(multipleEvaluationProp.getValue(Boolean.class));
            }
            this.condition = (String) p.get(PROP_ESCALATION_RULE_CONDITION).getValue();
            Property evaluatedProp = prop.get(PROP_ESCALATION_RULE_EXECUTED);
            if (evaluatedProp != null) {
                executed = BooleanUtils.isTrue(evaluatedProp.getValue(Boolean.class));
            }
            this.chain = (String) p.get(PROP_ESCALATION_RULE_CHAIN).getValue();
            this.lastExcutionTime = (Calendar) p.get(PROP_LAST_EXECUTION_TIME).getValue();
        }

        @Override
        public int compareTo(EscalationRule o) {
            return id.compareTo(o.id);
        }

        public String getLabel() {
            return label;
        }

        public String getChain() {
            return chain;
        }

        public GraphNode getNode() {
            return node;
        }

        public void setExecuted(boolean executed) {
            this.executed = executed;
            prop.get(PROP_ESCALATION_RULE_EXECUTED).setValue(Boolean.valueOf(executed));
            if (executed) {
                setExecutionTime(Calendar.getInstance());
            }
        }

        protected void setExecutionTime(Calendar time) {
            prop.get(PROP_LAST_EXECUTION_TIME).setValue(time);
            this.lastExcutionTime = time;
        }

        public boolean isExecuted() {
            return executed;
        }

        public String getId() {
            return id;
        }

        public boolean isMultipleExecution() {
            return multipleExecution;
        }

        /**
         * @since 5.9.3 Returns 'null' if the node was not executed, or the executed date was not computed ( for rules
         *        created before 5.9.3)
         */
        public Calendar getLastExecutionTime() {
            if (executed && lastExcutionTime != null) {
                return lastExcutionTime;
            }
            return null;
        }
    }

    /**
     * @since 5.7.3
     */
    class TaskInfo implements Comparable<TaskInfo>, Serializable {

        protected String taskDocId;

        protected String actor;

        protected String comment;

        protected String status;

        protected boolean ended;

        protected MapProperty prop;

        protected GraphNode node;

        public TaskInfo(GraphNode node, Property p) {
            this.prop = (MapProperty) p;
            this.node = node;
            this.taskDocId = (String) p.get(PROP_TASK_INFO_TASK_DOC_ID).getValue();
            this.status = (String) p.get(PROP_TASK_INFO_STATUS).getValue();
            this.actor = (String) p.get(PROP_TASK_INFO_ACTOR).getValue();
            this.comment = (String) p.get(PROP_TASK_INFO_COMMENT).getValue();
            Property ended = prop.get(PROP_TASK_INFO_ENDED);
            if (ended != null) {
                this.ended = BooleanUtils.isTrue(ended.getValue(Boolean.class));
            }
        }

        public TaskInfo(GraphNode node, String taskDocId) {
            this.node = node;
            this.prop = (MapProperty) ((ListProperty) node.getDocument().getProperty(PROP_TASKS_INFO)).addEmpty();
            this.prop.get(PROP_TASK_INFO_TASK_DOC_ID).setValue(taskDocId);
            this.taskDocId = taskDocId;
        }

        @Override
        public int compareTo(TaskInfo o) {
            return taskDocId.compareTo(o.taskDocId);
        }

        public String getTaskDocId() {
            return taskDocId;
        }

        public String getActor() {
            return actor;
        }

        public String getComment() {
            return comment;
        }

        public String getStatus() {
            return status;
        }

        public GraphNode getNode() {
            return node;
        }

        public boolean isEnded() {
            return ended;
        }

        public void setComment(String comment) {
            this.comment = comment;
            prop.get(PROP_TASK_INFO_COMMENT).setValue(comment);
        }

        public void setStatus(String status) {
            this.status = status;
            prop.get(PROP_TASK_INFO_STATUS).setValue(status);

        }

        public void setActor(String actor) {
            this.actor = actor;
            prop.get(PROP_TASK_INFO_ACTOR).setValue(actor);
        }

        public void setEnded(boolean ended) {
            this.ended = ended;
            prop.get(PROP_TASK_INFO_ENDED).setValue(Boolean.valueOf(ended));
        }
    }

    /**
     * Get the node id.
     *
     * @return the node id
     */
    String getId();

    /**
     * Get the node state.
     *
     * @return the node state
     */
    State getState();

    /**
     * Set the node state.
     *
     * @param state the node state
     */
    void setState(State state);

    /**
     * Checks if this is the start node.
     */
    boolean isStart();

    /**
     * Checks if this is a stop node.
     */
    boolean isStop();

    /**
     * Checks if this is a merge node.
     */
    boolean isMerge();

    /**
     * Checks if the merge is ready to execute (enough input transitions are present).
     */
    boolean canMerge();

    /**
     * Notes that this node was canceled (increments canceled counter).
     */
    void setCanceled();

    /**
     * Gets the canceled count for this node.
     */
    long getCanceledCount();

    /**
     * Cancels the tasks not ended on this node.
     */
    void cancelTasks();

    /**
     * Get input chain.
     *
     * @return the input chain
     */
    String getInputChain();

    /**
     * Get output chain.
     *
     * @return the output chain
     */
    String getOutputChain();

    /**
     * Checks it this node has an associated user task.
     */
    boolean hasTask();

    /**
     * Gets the task assignees
     *
     * @return the task assignees
     */
    List<String> getTaskAssignees();

    /**
     * Gets the due date
     */
    Date getTaskDueDate();

    /**
     * Gets the task directive
     */
    String getTaskDirective();

    /**
     * Gets the permission to the granted to the actors on this task on the document following the workflow
     */
    String getTaskAssigneesPermission();

    /**
     * Gets the task layout
     */
    String getTaskLayout();

    /**
     * @return the taskDocType. If none is specified, the default task type is returned.
     */
    String getTaskDocType();

    String getTaskNotificationTemplate();

    /**
     * Does bookkeeping at node start.
     */
    void starting();

    /**
     * Does bookkeeping at node end.
     */
    void ending();

    /**
     * Executes an Automation chain in the context of this node.
     *
     * @param chainId the chain
     */
    void executeChain(String chainId) throws DocumentRouteException;

    /** Internal during graph init. */
    void initAddInputTransition(Transition transition);

    /**
     * Gets the input transitions.
     */
    List<Transition> getInputTransitions();

    /**
     * Gets the output transitions.
     */
    List<Transition> getOutputTransitions();

    String getTaskDueDateExpr();

    /**
     * Executes an Automation chain in the context of this node for a given transition
     *
     * @param transition the transition
     */
    void executeTransitionChain(Transition transition) throws DocumentRouteException;

    /**
     * Evaluates transition conditions and returns the transitions that were true.
     * <p>
     * Transitions are evaluated in the order set on the node when the workflow was designed. Since @5.7.2 if the node
     * has the property "executeOnlyFirstTransition" set to true, only the first transition evaluated to true is
     * returned
     *
     * @return the true transitions
     */
    List<Transition> evaluateTransitions() throws DocumentRouteException;

    /**
     * Sets the graph and node variables.
     *
     * @param map the map of variables
     */
    void setAllVariables(Map<String, Object> map);

    /**
     * Sets the graph and node variables.
     *
     * @param map the map of variables
     * @param allowGlobalVariablesAssignement if set to false, throw a DocumentRouteException when trying to set global
     *            variables when not supposed to
     * @since 7.2
     */
    void setAllVariables(Map<String, Object> map, final boolean allowGlobalVariablesAssignement);

    /**
     * Gets the task buttons
     */
    List<Button> getTaskButtons();

    /**
     * Has the node the given action.
     *
     * @since 7.2
     */
    boolean hasTaskButton(final String name);

    /**
     * Gets the document representing this node
     */
    DocumentModel getDocument();

    /**
     * Gets a map containing the variables currently defined on this node
     */
    Map<String, Serializable> getVariables();

    /**
     * Gets a map containing the Json formatted variables currently defined on this node
     * @since 7.2
     */
    Map<String, Serializable> getJsonVariables();

    /**
     * Sets the property button on the node, keeping the id of the last action executed by the user on the associated
     * task if any
     */
    void setButton(String status);

    /**
     * Sets the last actor on a node (user who completed the task).
     *
     * @param actor the user id
     */
    void setLastActor(String actor);

    /**
     * Evaluates the task assignees from the taskAssigneesVar
     */
    List<String> evaluateTaskAssignees() throws DocumentRouteException;

    /**
     * Evaluates the task due date from the taskDueDateExpr and sets it as the dueDate
     */
    Date computeTaskDueDate() throws DocumentRouteException;

    /**
     * Gets a map containing the workflow and node variables and workflow documents.
     *
     * @param detached The documents added into this map can be detached or not
     */
    Map<String, Serializable> getWorkflowContextualInfo(CoreSession session, boolean detached);

    /**
     * When workflow engine runs an exclusive node, it evaluates the transition one by one and stops a soon as one of
     * the transition is evaluated to true
     *
     * @since 5.7.2
     */
    boolean executeOnlyFirstTransition();

    /**
     * Checks if this node has a sub-route model defined.
     *
     * @return {@code true} if there is a sub-route
     * @since 5.7.2
     */
    boolean hasSubRoute() throws DocumentRouteException;

    /**
     * Gets the sub-route model id.
     * <p>
     * If this is present, then this node will be suspended while the sub-route is run. When the sub-route ends, this
     * node will resume.
     *
     * @return the sub-route id, or {@code null} if none is defined
     * @since 5.7.2
     */
    String getSubRouteModelId() throws DocumentRouteException;

    /**
     * Starts the sub-route on this node.
     *
     * @return the sub-route
     * @since 5.7.2
     */
    DocumentRoute startSubRoute() throws DocumentRouteException;

    /**
     * Cancels the sub-route if there is one.
     *
     * @since 5.7.2
     */
    void cancelSubRoute() throws DocumentRouteException;

    /**
     * Evaluates the rules for the escalation rules and returns the ones to be executed. The rules already executed and
     * not having the property multipleExecution = true are also ignored
     *
     * @since 5.7.2
     */
    List<EscalationRule> evaluateEscalationRules();

    /**
     * Gets the list of all escalation rules for the node
     *
     * @since 5.7.2
     */
    List<EscalationRule> getEscalationRules();

    /**
     * Checks if this node has created multiple tasks, one for each assignees.
     *
     * @since 5.7.3
     */
    boolean hasMultipleTasks();

    /**
     * Gets all the tasks info for the tasks created from this node
     *
     * @since 5.7.3
     */
    List<TaskInfo> getTasksInfo();

    /**
     * Persist the info when a new task is created from this node
     *
     * @since 5.7.3
     */
    void addTaskInfo(String taskId);

    /**
     * Persist these info from the task on the node. Status is the id of the button clicked to end the task by the
     * actor.
     *
     * @since 5.7.3
     */
    void updateTaskInfo(String taskId, boolean ended, String status, String actor, String comment);

    /**
     * Gets all the ended tasks originating from this node. This also counts the canceled tasks.
     *
     * @since 5.7.3
     */
    List<TaskInfo> getEndedTasksInfo();

    /**
     * Gets all the ended tasks originating from this node that were processed with a status. Doesn't count the canceled
     * tasks.
     *
     * @since 5.7.3
     */
    List<TaskInfo> getProcessedTasksInfo();

    /**
     * Returns false if all tasks created from this node were ended.
     *
     * @since 5.7.3
     */
    boolean hasOpenTasks();

    /**
     * Returns true if tasks created from this node can be reassigned.
     *
     * @since 5.7.3
     */
    boolean allowTaskReassignment();

    /**
     * Sets the variable on this node if it exists as a Node Variable.
     *
     * @since 5.8
     */
    void setVariable(String name, String value);

    /**
     * Sets the node variables.
     *
     * @since 5.9.3, 5.8.0-HF11
     * @param map the map of variables
     */
    void setVariables(Map<String, Serializable> map);

    /**
     * Sets the variables of the workflow based on their JSON representation (especially for scalar lists). Eg.
     * Map<String, String> map = new HashMap<String, String>();
     * map.put("contributors","[\"John Doe\", \"John Smith\"]"); map.put("title","Test Title");
     *
     * @param map the map of variables
     * @since 5.9.3, 5.8.0-HF11
     */
    void setJSONVariables(Map<String, String> map);

    /**
     * @since 7.4
     */
    void removeTaskInfo(String taskId);

}
