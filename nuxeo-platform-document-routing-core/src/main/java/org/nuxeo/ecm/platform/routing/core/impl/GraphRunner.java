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

import java.util.LinkedList;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;

/**
 * Runs the proper nodes depending on the graph state.
 *
 * @since 5.6
 */
public class GraphRunner extends AbstractRunner implements ElementRunner {

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        try {
            startGraph(session, element);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void startGraph(CoreSession session, DocumentRouteElement element)
            throws DocumentRouteException {
        GraphRoute graph = new GraphRouteImpl(element);
        // TODO graph.setRunning();
        // TODO event
        runGraph(graph, graph.getStartNode());
    }

    public void resumeGraph(GraphRoute graph, GraphNode node, Object data)
            throws DocumentRouteException {
        // TODO data
        runGraph(graph, node);
    }

    protected void runGraph(GraphRoute graph, GraphNode initialNode)
            throws DocumentRouteException {
        LinkedList<GraphNode> nodes = new LinkedList<GraphNode>();
        nodes.add(initialNode);
        while (!nodes.isEmpty()) {
            GraphNode node = nodes.pop();

        }

    }

}
