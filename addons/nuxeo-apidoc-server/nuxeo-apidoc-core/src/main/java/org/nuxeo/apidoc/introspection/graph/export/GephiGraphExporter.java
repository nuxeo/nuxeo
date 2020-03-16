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
package org.nuxeo.apidoc.introspection.graph.export;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphFactory;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.plugin.ExporterGEXF;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.project.api.ProjectController;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.EditableGraph;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.introspection.graph.ContentGraphImpl;
import org.nuxeo.apidoc.introspection.graph.PositionedNodeImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.openide.util.Lookup;

/**
 * Exporter for Gephi graph format.
 *
 * @since 11.1
 */
public class GephiGraphExporter extends AbstractGraphExporter implements GraphExporter {

    protected static String TIMEOUT_PROP = "layout.timeout";

    protected StringWriter backupWriter;

    public GephiGraphExporter(EditableGraph graph) {
        super(graph);
    }

    @Override
    public void setGraph(EditableGraph graph) {
        super.setGraph(graph);
        backupWriter = null;
    }

    @Override
    public ContentGraphImpl export() {
        if (backupWriter == null) {
            throw new IllegalArgumentException("No graph produced to export");
        }
        ContentGraphImpl cgraph = initContentGraph(graph);
        cgraph.setContent(backupWriter.toString());
        cgraph.setContentName("graph.gexf");
        cgraph.setContentType("application/xml");

        return cgraph;
    }

    protected int getTimeoutSeconds(EditableGraph graph) {
        // Layout for 1 minute by default
        String timeout = graph.getProperty(TIMEOUT_PROP, "60");
        int res = Integer.valueOf(timeout);
        return res;
    }

    protected AutoLayout initLayout(EditableGraph graph) {
        return new AutoLayout(getTimeoutSeconds(graph), TimeUnit.SECONDS);
    }

    public EditableGraph applyForceLayout(DistributionSnapshot distribution, NODE_TYPE weightRef, boolean is3D) {
        AutoLayout autoLayout = initLayout(graph);
        YifanHuLayout yhLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        AutoLayout.DynamicProperty optimalDistanceProperty = AutoLayout.createDynamicProperty(
                "YifanHu.optimalDistance.name", 500.0, 0f);
        ForceAtlasLayout forceLayout = new ForceAtlasLayout(null);
        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty(
                "forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f); // True after 10% of layout time
        AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty(
                "forceAtlas.repulsionStrength.name", 500., 0f); // 500 for the complete period
        autoLayout.addLayout(yhLayout, 0.5f, new AutoLayout.DynamicProperty[] { optimalDistanceProperty });
        autoLayout.addLayout(forceLayout, 0.5f,
                new AutoLayout.DynamicProperty[] { adjustBySizeProperty, repulsionProperty });
        return applyGephiLayout(distribution, graph, weightRef, autoLayout, is3D);
    }

    public EditableGraph applyBundleOpenOrdLayout(DistributionSnapshot distribution, NODE_TYPE weightRef) {
        AutoLayout autoLayout = initLayout(graph);
        YifanHuLayout yhLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        // XXX: timeout does not apply to it :( --> pattern to consider:
        // Thread thread = new Thread(() -> {
        // layout.execute();
        // });
        // thread.start();
        // thread.join(10_000); // wait 10s for thread completion
        // if (!layout.isCompleted()) {
        // layout.cancel();
        // // wait 1s for cancel to take effect
        // thread.join(1_000);
        // thread.interrupt(); // hopefully thread will stop
        // }
        OpenOrdLayout ooLayout = new OpenOrdLayout(null);
        ooLayout.resetPropertiesValues();
        AutoLayout.DynamicProperty numIterationsProperty = AutoLayout.createDynamicProperty(
                "OpenOrd.properties.numiterations.name", 10, 0f);
        autoLayout.addLayout(yhLayout, 0.5f);
        autoLayout.addLayout(ooLayout, 0.5f, new AutoLayout.DynamicProperty[] { numIterationsProperty });
        return applyGephiLayout(distribution, graph, weightRef, autoLayout, true);
    }

    public EditableGraph applyGephiLayout(DistributionSnapshot distribution, EditableGraph graph, NODE_TYPE weightRef,
            AutoLayout layout, boolean is3D) {
        // create gephi project and associated workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();

        // generate gephi graph from original one
        GraphModel ggraphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        GraphFactory factory = ggraphModel.factory();
        org.gephi.graph.api.Graph ggraph = ggraphModel.getDirectedGraph();
        Map<String, org.gephi.graph.api.Node> gnodes = new HashMap<>();

        // apply layouting to bundles taking their weight into account, all other edges of type CONTAINS are taken into
        // account but using the same weight (1) to produce circular positioning around containing bundle.
        // z positioning will be done later according to node type and other requirements (REFERENCES + REQUIRES between
        // components).
        // forget above explanation if relying on deployment order instead
        for (Node node : graph.getNodes()) {
            Integer weight = 1;
            NODE_TYPE ntype = NODE_TYPE.getType(node.getType());
            if (weightRef != null && weightRef.equals(ntype)) {
                weight = node.getWeight();
            }
            org.gephi.graph.api.Node gnode = createGephiNode(factory, node, weight);
            ggraph.addNode(gnode);
            gnodes.put(node.getId(), gnode);
        }
        for (Edge edge : graph.getEdges()) {
            EDGE_TYPE etype = EDGE_TYPE.getType(edge.getValue());
            if (EDGE_TYPE.CONTAINS.equals(etype) || EDGE_TYPE.REQUIRES.equals(etype)) {
                ggraph.addEdge(createGephiEdge(factory, edge, gnodes));
            }
        }

        layout.setGraphModel(ggraph.getModel());
        layout.execute();

        copyPositions(ggraph, graph);
        if (is3D) {
            positionZByType(graph);
        }

        // save export
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        ExporterGEXF exporter = (ExporterGEXF) ec.getExporter("gexf");
        exporter.setWorkspace(pc.getCurrentWorkspace());
        backupWriter = new StringWriter();
        ec.exportWriter(backupWriter, exporter);

        // cleanup
        pc.removeProject(pc.getCurrentProject());

        return graph;
    }

    protected org.gephi.graph.api.Node createGephiNode(GraphFactory factory, Node node, Integer newWeight) {
        org.gephi.graph.api.Node gnode = factory.newNode(node.getId());
        gnode.setLabel(node.getLabel());
        if (newWeight == null) {
            gnode.setSize(node.getWeight());
        } else {
            gnode.setSize(newWeight);
        }
        return gnode;
    }

    protected org.gephi.graph.api.Edge createGephiEdge(GraphFactory factory, Edge edge,
            Map<String, org.gephi.graph.api.Node> gnodes) {
        EDGE_TYPE type = EDGE_TYPE.getType(edge.getValue());
        org.gephi.graph.api.Edge gedge = factory.newEdge(gnodes.get(edge.getSource()), gnodes.get(edge.getTarget()),
                type.getIndex(), edge.getWeight(), type.isDirected());
        gedge.setLabel(edge.getValue());
        return gedge;
    }

    protected void copyPositions(org.gephi.graph.api.Graph ggraph, EditableGraph graph) {
        for (org.gephi.graph.api.Node gnode : ggraph.getNodes()) {
            Node node = graph.getNode(String.valueOf(gnode.getId()));
            if (node instanceof PositionedNodeImpl) {
                PositionedNodeImpl pnode = (PositionedNodeImpl) node;
                pnode.setX(gnode.x());
                pnode.setY(gnode.y());
                pnode.setZ(gnode.z());
            }
        }
    }

    protected void copyXY(Node source, Node target) {
        if (source instanceof PositionedNodeImpl && target instanceof PositionedNodeImpl) {
            ((PositionedNodeImpl) target).setX(((PositionedNodeImpl) source).getX());
            ((PositionedNodeImpl) target).setY(((PositionedNodeImpl) source).getY());
        }
    }

    protected void positionZByType(EditableGraph graph) {
        for (Node node : graph.getNodes()) {
            if (node instanceof PositionedNodeImpl) {
                ((PositionedNodeImpl) node).setZ(NODE_TYPE.getType(node.getType()).getZIndex());
            }
        }
    }

}
