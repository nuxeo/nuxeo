/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.introspection.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphFactory;
import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.project.api.ProjectController;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.Node;
import org.openide.util.Lookup;

/**
 * Gephi utils to position nodes in the graph.
 * 
 * @since 11.1
 */
public class GephiLayout {

    protected static String TIMEOUT_PROP = "forceLayout.timeout";

    public static Graph getLayout(Graph graph) {

        // create gephi project and associated workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();

        // generate gephi graph from original one
        GraphModel ggraphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        GraphFactory factory = ggraphModel.factory();
        org.gephi.graph.api.Graph ggraph = ggraphModel.getDirectedGraph();
        Map<String, org.gephi.graph.api.Node> gnodes = new HashMap<>();
        for (Node node : graph.getNodes()) {
            org.gephi.graph.api.Node gnode = createGephiNode(factory, node);
            ggraph.addNode(gnode);
            gnodes.put(node.getOriginalId(), gnode);
        }
        for (Edge edge : graph.getEdges()) {
            ggraph.addEdge(createGephiEdge(factory, edge, gnodes));
        }

        // Layout for 1 minute by default
        String timeout = graph.getProperty(TIMEOUT_PROP, "60");
        AutoLayout autoLayout = new AutoLayout(Integer.valueOf(timeout), TimeUnit.SECONDS);
        autoLayout.setGraphModel(ggraphModel);
        YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        ForceAtlasLayout secondLayout = new ForceAtlasLayout(null);
        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty(
                "forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f); // True after 10% of layout time
        AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty(
                "forceAtlas.repulsionStrength.name", 500., 0f); // 500 for the complete period
        autoLayout.addLayout(firstLayout, 0.5f);
        autoLayout.addLayout(secondLayout, 0.5f,
                new AutoLayout.DynamicProperty[] { adjustBySizeProperty, repulsionProperty });
        autoLayout.execute();

        for (org.gephi.graph.api.Node gnode : ggraph.getNodes()) {
            Node node = graph.getNode(String.valueOf(gnode.getId()));
            if (node instanceof PositionedNodeImpl) {
                PositionedNodeImpl pnode = (PositionedNodeImpl) node;
                pnode.setX(gnode.x());
                pnode.setY(gnode.y());
                // TODO: z is not positioned for now
                pnode.setZ(gnode.z());
            }
        }

        return graph;
    }

    protected static org.gephi.graph.api.Node createGephiNode(GraphFactory factory, Node node) {
        org.gephi.graph.api.Node gnode = factory.newNode(node.getOriginalId());
        gnode.setLabel(node.getLabel());
        gnode.setSize(node.getWeight());
        return gnode;
    }

    protected static org.gephi.graph.api.Edge createGephiEdge(GraphFactory factory, Edge edge,
            Map<String, org.gephi.graph.api.Node> gnodes) {
        EDGE_TYPE type = EDGE_TYPE.getType(edge.getValue());
        org.gephi.graph.api.Edge gedge = factory.newEdge(gnodes.get(edge.getOriginalSourceId()),
                gnodes.get(edge.getOriginalTargetId()), type.getIndex(), type.isDirected());
        gedge.setLabel(edge.getValue());
        return gedge;
    }
}
