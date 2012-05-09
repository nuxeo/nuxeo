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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;

/**
 * @since 5.6
 */
public class GraphRouteImpl implements GraphRoute {

    protected final DocumentModel doc;

    protected final List<GraphNode> nodes;

    public GraphRouteImpl(DocumentRouteElement element)
            throws DocumentRouteException {
        this.doc = element.getDocument();
        this.nodes = computeNodes();
    }

    protected List<GraphNode> computeNodes() throws DocumentRouteException {
        try {
            CoreSession session = doc.getCoreSession();
            DocumentModelList children = session.getChildren(doc.getRef());
            List<GraphNode> nodes = new ArrayList<GraphNode>(
                    children.size());
            for (DocumentModel doc : children) {
                // TODO use adapters
                if (doc.getType().equals("RouteNode")) {
                    nodes.add(new GraphNodeImpl(doc));
                }
            }
            return nodes;
        } catch (ClientException e) {
            throw new DocumentRouteException(e);
        }
    }

    @Override
    public String getName() {
        try {
            return doc.getTitle();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public GraphNode getStartNode() throws DocumentRouteException {
        for (GraphNode node : nodes) {
            if (node.isStart()) {
                return node;
            }
        }
        throw new DocumentRouteException("No start node for graph: "
                + getName());
    }

    public Collection<GraphNode> getNodes() {
        return nodes;
    }
}
