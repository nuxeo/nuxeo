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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;

/**
 * Returns a json representation of the graph route
 *
 * @since 5.6
 *
 */
@Operation(id = GetGraphOperation.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Get graph", description = "get graph nodes.")
public class GetGraphOperation {
    public final static String ID = "Document.Routing.GetGraph";

    @Context
    protected OperationContext context;

    @Param(name = "routeDocId", required = true)
    protected String routeDocId;

    @Context
    protected CoreSession session;

    @OperationMethod
    public Blob run() throws Exception {
        GetRouteAsJsonUnrestricted unrestrictedRunner = new GetRouteAsJsonUnrestricted(
                session, routeDocId);
        String json = unrestrictedRunner.getJSON();
        return new InputStreamBlob(new ByteArrayInputStream(
                json.getBytes("UTF-8")), "application/json");

    }

    public static String toJSON(GraphRoute route) {
        try {
            Map<String, Object> graph = new HashMap<String, Object>();
            List<NodeView> nodeViews = new ArrayList<NodeView>();
            Map<String, TransitionView> tranViews = new HashMap<String, TransitionView>();

            for (GraphNode node : route.getNodes()) {
                nodeViews.add(new NodeView(node));
                List<Transition> transitions = node.getOutputTransitions();
                for (Transition transition : transitions) {
                    GraphNode targetNode = route.getNode(transition.getTarget());
                    tranViews.put(transition.getId(),
                            new TransitionView(node.getId(),
                                    targetNode.getId(), transition.getLabel()));
                }
            }
            graph.put("nodes", nodeViews);
            graph.put("transitions", tranViews.values());

            ObjectMapper mapper = new ObjectMapper();
            StringWriter writer = new StringWriter();
            mapper.writeValue(writer, graph);
            return writer.toString();
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }
    
    class GetRouteAsJsonUnrestricted extends UnrestrictedSessionRunner {

        String docId;
        String json;

        protected GetRouteAsJsonUnrestricted(CoreSession session, String docId) {
            super(session);
            this.docId = docId;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel doc = session.getDocument(new IdRef(docId));
            GraphRoute route = doc.getAdapter(GraphRoute.class);
            json = toJSON(route);
        }

        public String getJSON() throws ClientException {
            runUnrestricted();
            return json;
        }
    }
}

class NodeView {

    public NodeView(GraphNode node) throws ClientException {
        this.x = Integer.parseInt((String) node.getDocument().getPropertyValue(
                GraphNode.PROP_NODE_X_COORDINATE));
        this.y = Integer.parseInt((String) node.getDocument().getPropertyValue(
                GraphNode.PROP_NODE_Y_COORDINATE));
        this.isEndNode = node.isStart();
        this.isEndNode = node.isStop();
        this.id = node.getId();
        this.title = (String) node.getDocument().getPropertyValue(
                GraphNode.PROP_TITLE);
        this.state = node.getState().getLifeCycleState();
    }

    public int x;

    public int y;

    public boolean isStartNode;

    public boolean isEndNode;

    public String id;

    public String title;

    public String state;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isStartNode() {
        return isStartNode;
    }

    public boolean isEndNode() {
        return isEndNode;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getState() {
        return state;
    }
}

class TransitionView {

    public TransitionView(String nodeSourceId, String nodeTargetId, String label) {
        this.nodeSourceId = nodeSourceId;
        this.nodeTargetId = nodeTargetId;
        this.label = label;
    }

    public String nodeSourceId;

    public String nodeTargetId;

    public String label;

    public String getNodeSourceId() {
        return nodeSourceId;
    }

    public String getNodeTargetId() {
        return nodeTargetId;
    }

    public String getLabel() {
        return label;
    }
}
