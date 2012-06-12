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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;

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
            // TODO compute loop transitions on the graph
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

    protected void computeTransitions() throws DocumentRouteException {
        for (GraphNode node : nodes) {
            List<Transition> tt = node.getOutputTransitions();
            for (Transition t : tt) {
                GraphNode target = getNode(t.target);
                target.initAddInputTransition(t);
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
        GraphVariablesUtil.setVariables(document, PROP_VARIABLES_FACET, map);
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

}
