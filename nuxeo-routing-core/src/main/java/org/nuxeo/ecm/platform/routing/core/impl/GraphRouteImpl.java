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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.6
 */
public class GraphRouteImpl extends DocumentRouteImpl implements GraphRoute {

    private static final long serialVersionUID = 1L;

    /** To be used through getter. */
    protected List<GraphNode> nodes;

    /** To be used through getter. */
    protected Map<String, GraphNode> nodesById;

    public GraphRouteImpl(DocumentModel doc) {
        super(doc, new GraphRunner());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(getName()).toString();
    }

    @Override
    public Collection<GraphNode> getNodes() {
        if (nodes == null) {
            compute();
        }
        return nodes;
    }

    protected void compute() {
        try {
            computeNodes();
            computeTransitions();
            computeLoopTransitions();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void computeNodes() throws ClientException {
        CoreSession session = document.getCoreSession();
        DocumentModelList children = session.getChildren(document.getRef());
        nodes = new ArrayList<GraphNode>(children.size());
        nodesById = new HashMap<String, GraphNode>();
        for (DocumentModel doc : children) {
            // TODO use adapters
            if (doc.getType().equals("RouteNode")) {
                GraphNode node = new GraphNodeImpl(doc, this);
                String id = node.getId();
                if (nodesById.put(id, node) != null) {
                    throw new DocumentRouteException(
                            "Duplicate nodes with id: " + id);
                }
                nodes.add(node);
            }
        }
    }

    /**
     * Deduce input transitions from output transitions.
     */
    protected void computeTransitions() throws DocumentRouteException {
        for (GraphNode node : nodes) {
            List<Transition> tt = node.getOutputTransitions();
            for (Transition t : tt) {
                GraphNode target = getNode(t.target);
                target.initAddInputTransition(t);
            }
        }
    }

    /**
     * Finds which transitions are re-looping (feedback arc set).
     */
    protected void computeLoopTransitions() {
        /*
         * Depth-first search. In the todo stack, each element records a list
         * of the siblings left to visit at that depth. After visiting the last
         * sibling, we go back to the parent and at this point mark it as
         * visited in post-traversal order.
         */
        List<String> postOrder = new LinkedList<String>();
        Deque<Deque<String>> stack = new LinkedList<Deque<String>>();
        Deque<String> first = new LinkedList<String>();
        first.add(getStartNode().getId());
        stack.push(first);
        Set<String> done = new HashSet<String>();
        for (;;) {
            // find next sibling
            String nodeId = stack.peek().peek();
            if (nodeId == null) {
                // last sibling done
                // go back up one level and mark post-traversal order
                stack.pop(); // pop empty children
                if (stack.isEmpty()) {
                    // we are done
                    break;
                }
                nodeId = stack.peek().pop(); // pop parent
                postOrder.add(nodeId); // mark post-traversal order
            } else if (done.add(nodeId)) {
                // traverse the next sibling
                Deque<String> children = new LinkedList<String>();
                for (Transition t : getNode(nodeId).getOutputTransitions()) {
                    children.add(t.target);
                }
                // add children to stack and recurse
                stack.push(children);
            } else {
                // already traversed
                stack.peek().pop(); // skip it
            }
        }

        // reverse the post-order to find the topological ordering
        Collections.reverse(postOrder);
        Map<String, Integer> ordering = new HashMap<String, Integer>();
        int i = 1;
        for (String nodeId : postOrder) {
            ordering.put(nodeId, Integer.valueOf(i++));
        }

        // walk the graph and all transitions again
        // and mark as looping the transitions pointing to a node
        // with a smaller order that the source
        done.clear();
        Deque<String> todo = new LinkedList<String>();
        todo.add(getStartNode().getId());
        while (!todo.isEmpty()) {
            String nodeId = todo.pop();
            if (done.add(nodeId)) {
                int source = ordering.get(nodeId).intValue();
                for (Transition t : getNode(nodeId).getOutputTransitions()) {
                    todo.push(t.target);
                    // compare orders to detected feeback arcs
                    int target = ordering.get(t.target).intValue();
                    if (target <= source) {
                        t.loop = true;
                    }
                }
            }
        }
    }

    @Override
    public GraphNode getStartNode() throws DocumentRouteException {
        for (GraphNode node : getNodes()) {
            if (node.isStart()) {
                return node;
            }
        }
        throw new DocumentRouteException("No start node for graph: "
                + getName());
    }

    @Override
    public GraphNode getNode(String id) {
        getNodes(); // compute
        GraphNode node = nodesById.get(id);
        if (node != null) {
            return node;
        }
        throw new IllegalArgumentException("No node with id: " + id
                + " in graph: " + this);
    }

    @Override
    public Map<String, Serializable> getVariables() {
        return GraphVariablesUtil.getVariables(document, PROP_VARIABLES_FACET);
    }

    @Override
    public void setVariables(Map<String, Serializable> map) {
        if (map.containsKey(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON)
                && (Boolean) map.get(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON)) {
            Map<String, String> vars = new HashMap<String, String>();
            map.remove(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON);
            for (String key : map.keySet()) {
                if (map.get(key) != null && !(map.get(key) instanceof String)) {
                    throw new ClientRuntimeException(
                            "The parameter 'map' should contain only Strings as it contains the marker '_MAP_VAR_FORMAT_JSON' ");
                }
                vars.put(key, (String) map.get(key));
            }
            GraphVariablesUtil.setJSONVariables(document, PROP_VARIABLES_FACET,
                    vars);
        } else {
            GraphVariablesUtil.setVariables(document, PROP_VARIABLES_FACET, map);
        }
    }

    @Override
    public void setJSONVariables(Map<String, String> map) {
        GraphVariablesUtil.setJSONVariables(document, PROP_VARIABLES_FACET, map);
    }

    @Override
    public DocumentModelList getAttachedDocumentModels() {
        try {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) document.getPropertyValue(DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME);
            ArrayList<DocumentRef> docRefs = new ArrayList<DocumentRef>();
            for (String id : ids) {
                docRefs.add(new IdRef(id));
            }
            return document.getCoreSession().getDocuments(
                    docRefs.toArray(new DocumentRef[0]));
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public String getAvailabilityFilter() {
        try {
            return (String) document.getPropertyValue(PROP_AVAILABILITY_FILTER);
        } catch (ClientException e) {
            return null;
        }
    }

    @Override
    public boolean hasParentRoute() {
        try {
            String parentRouteInstanceId = (String) document.getPropertyValue(PROP_PARENT_ROUTE);
            return !StringUtils.isEmpty(parentRouteInstanceId);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void resumeParentRoute(CoreSession session) {
        DocumentRoutingService routing = Framework.getLocalService(DocumentRoutingService.class);
        try {
            String parentRouteInstanceId = (String) document.getPropertyValue(PROP_PARENT_ROUTE);
            String parentRouteNodeId = (String) document.getPropertyValue(PROP_PARENT_NODE);
            routing.resumeInstance(parentRouteInstanceId, parentRouteNodeId,
                    null, null, session);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }
}
