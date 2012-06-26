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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.runtime.api.Framework;

/**
 * Graph Node implementation as an adapter over a DocumentModel.
 *
 * @since 5.6
 */
public class GraphNodeImpl extends DocumentRouteElementImpl implements
        GraphNode {

    private static final Log log = LogFactory.getLog(GraphNodeImpl.class);

    private static final long serialVersionUID = 1L;

    protected final GraphRouteImpl graph;

    protected State localState;

    /** To be used through getter. */
    protected List<Transition> inputTransitions;

    /** To be used through getter. */
    protected List<Transition> outputTransitions;

    /** To be used through getter. */
    protected List<Button> taskButtons;

    public GraphNodeImpl(DocumentModel doc, GraphRouteImpl graph) {
        super(doc, new GraphRunner());
        this.graph = graph;
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
            getSession().save(); // XXX debug
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
    public void incrementCount() {
        incrementProp(PROP_COUNT);
    }

    @Override
    public Map<String, Serializable> getVariables() {
        return GraphVariablesUtil.getVariables(document, PROP_VARIABLES_FACET);
    }

    public void setVariables(Map<String, Serializable> map) {
        GraphVariablesUtil.setVariables(document, PROP_VARIABLES_FACET, map);
    }

    @Override
    public void setAllVariables(Map<String, Object> map) {

        // get variables from node and graph
        Map<String, Serializable> graphVariables = graph.getVariables();
        Map<String, Serializable> nodeVariables = getVariables();

        // set variables back into node and graph
        boolean changedNodeVariables = false;
        boolean changedGraphVariables = false;
        for (Entry<String, Object> es : map.entrySet()) {
            String key = es.getKey();
            Serializable value = (Serializable) es.getValue();
            if (nodeVariables.containsKey(key)) {
                Serializable oldValue = nodeVariables.get(key);
                if (!ObjectUtils.equals(value, oldValue)) {
                    changedNodeVariables = true;
                    nodeVariables.put(key, value);
                }
            } else if (graphVariables.containsKey(key)) {
                Serializable oldValue = graphVariables.get(key);
                if (!ObjectUtils.equals(value, oldValue)) {
                    changedGraphVariables = true;
                    graphVariables.put(key, value);
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

    protected OperationContext getContext() {
        OperationContext context = new OperationContext(getSession());
        context.setCommit(false); // no session save at end
        // context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY,
        // element);
        context.putAll(graph.getVariables());
        context.putAll(getVariables());
        // workflow context
        // context.put("workflowId", graph.get);
        context.put("initiator", "");
        context.put("documents", "");
        context.put("workflowStartTime", "");
        // node context
        context.put("nodeId", getId());
        context.put("state", getState().name().toLowerCase());
        context.put("nodeStartTime", ""); // TODO
        // task context
        context.put("comment", ""); // TODO filled by form
        context.put("button", getProperty(PROP_NODE_BUTTON));
        // associated docs
        context.setInput(graph.getAttachedDocumentModels());
        return context;
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
        OperationContext context = getContext();
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
            Collections.sort(trans);
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
            OperationContext context = getContext();
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
    public void evaluateTaskAssignees() throws DocumentRouteException {
        List<String> taskAssignees = new ArrayList<String>();
        OperationContext context = getContext();
        String taskAssigneesVar = getTaskAssigneesVar();
        if (StringUtils.isEmpty(taskAssigneesVar)) {
            return;
        }
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
        if (!((res instanceof String) || res instanceof String[])) {
            throw new DocumentRouteException(
                    "Can not evaluate task assignees from " + taskAssigneesVar);
        }
        if (res instanceof String) {
            taskAssignees.add((String) res);
        } else {
            taskAssignees.addAll(Arrays.asList((String[]) res));
        }
        addTaskAssignees(taskAssignees);
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
            throw new ClientRuntimeException("Illegal merge mode '" + merge
                    + "' for node " + this);
        }
    }

    @Override
    public List<Transition> getInputTransitions() {
        return inputTransitions;
    }

    @Override
    public void cancelTask() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
            CoreSession session = document.getCoreSession();
            session.saveDocument(document);
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
            CoreSession session = document.getCoreSession();
            session.saveDocument(document);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }
}