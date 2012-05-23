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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
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
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.runtime.api.Framework;

/**
 * Graph Node implementation as an adapter over a DocumentModel.
 *
 * @since 5.6
 */
public class GraphNodeImpl extends DocumentRouteElementImpl implements
        GraphNode {

    private static final long serialVersionUID = 1L;

    public static final String PROP_NODE_ID = "rnode:nodeId";

    public static final String PROP_START = "rnode:start";

    public static final String PROP_STOP = "rnode:stop";

    public static final String PROP_MERGE = "rnode:merge";

    public static final String PROP_COUNT = "rnode:count";

    public static final String PROP_INPUT_CHAIN = "rnode:inputChain";

    public static final String PROP_OUTPUT_CHAIN = "rnode:outputChain";

    public static final String PROP_HAS_TASK = "rnode:hasTask";

    public static final String PROP_VARIABLES = "rnode:variables";

    public static final String PROP_VAR_NAME = "name";

    public static final String PROP_VAR_VALUE = "value";

    public static final String PROP_TRANSITIONS = "rnode:transitions";

    public static final String PROP_TRANS_NAME = "name";

    public static final String PROP_TRANS_TARGET = "targetId";

    public static final String PROP_TRANS_CONDITION = "condition";

    public static final String PROP_TRANS_RESULT = "result";

    protected final GraphRouteImpl graph;

    protected State localState;

    public GraphNodeImpl(DocumentModel doc, GraphRouteImpl graph) {
        super(doc, new GraphRunner());
        this.graph = graph;
    }

    protected boolean getBoolean(String propertyName) {
        return Boolean.TRUE.equals(getProperty(propertyName));
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

    @Override
    public void incrementCount() {
        try {
            Long count = (Long) getProperty(PROP_COUNT);
            if (count == null) {
                count = Long.valueOf(0);
            }
            document.setPropertyValue(PROP_COUNT,
                    Long.valueOf(count.longValue() + 1));
            saveDocument();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    /**
     * Gets the node variables.
     *
     * @return the map of variables
     */
    protected Map<String, Serializable> getVariables() {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Serializable>> vars = (List<Map<String, Serializable>>) document.getPropertyValue(PROP_VARIABLES);
            Map<String, Serializable> map = new LinkedHashMap<String, Serializable>();
            for (Map<String, Serializable> var : vars) {
                String name = (String) var.get(PROP_VAR_NAME);
                Serializable value = var.get(PROP_VAR_VALUE);
                map.put(name, value);
            }
            return map;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    /**
     * Sets the node variables.
     *
     * @param map the map of variables
     */
    protected void setVariables(Map<String, Serializable> map) {
        try {
            List<Map<String, Serializable>> vars = new LinkedList<Map<String, Serializable>>();
            for (Entry<String, Serializable> es : map.entrySet()) {
                Map<String, Serializable> m = new HashMap<String, Serializable>();
                m.put(PROP_VAR_NAME, es.getKey());
                m.put(PROP_VAR_VALUE, es.getValue());
                vars.add(m);
            }
            document.setPropertyValue(PROP_VARIABLES, (Serializable) vars);
            saveDocument();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected OperationContext getContext(
            Map<String, Serializable> graphVariables,
            Map<String, Serializable> nodeVariables) {
        OperationContext context = new OperationContext(getSession());
        // context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY,
        // element);
        context.putAll(graphVariables);
        context.putAll(nodeVariables);
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
        context.put("assignees", ""); // TODO
        context.put("comment", ""); // TODO filled by form
        context.put("status", ""); // TODO filled by form
        // associated docs
        context.setInput(graph.getAttachedDocumentModels());
        return context;
    }

    @Override
    public void executeChain(String chainId) throws DocumentRouteException {
        // TODO events
        if (StringUtils.isEmpty(chainId)) {
            return;
        }

        // get variables from node and graph
        Map<String, Serializable> graphVariables = graph.getVariables();
        Map<String, Serializable> nodeVariables = getVariables();
        OperationContext context = getContext(graphVariables, nodeVariables);

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

        // set variables back into node and graph
        boolean changedNodeVariables = false;
        boolean changedGraphVariables = false;
        for (Entry<String, Object> es : context.entrySet()) {
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

    @Override
    public Set<String> evaluateTransitionConditions()
            throws DocumentRouteException {
        try {
            Set<String> targetIds = new HashSet<String>();
            OperationContext context = getContext(graph.getVariables(),
                    getVariables());
            ListProperty transProps = (ListProperty) document.getProperty(PROP_TRANSITIONS);
            for (Property p : transProps) {
                MapProperty prop = (MapProperty) p;
                String transitionId = (String) prop.get(PROP_TRANS_NAME).getValue();
                String condition = (String) prop.get(PROP_TRANS_CONDITION).getValue();

                context.put("transition", transitionId);
                Expression expr = Scripting.newExpression(condition);
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
                            "Error evaluating condition: " + condition, e);
                }
                boolean bool = Boolean.TRUE.equals(res);
                prop.get(PROP_TRANS_RESULT).setValue(Boolean.valueOf(bool));
                if (bool) {
                    String targetId = (String) prop.get(PROP_TRANS_TARGET).getValue();
                    targetIds.add(targetId);
                }
            }
            saveDocument();
            return targetIds;
        } catch (DocumentRouteException e) {
            throw e;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

    }

}
