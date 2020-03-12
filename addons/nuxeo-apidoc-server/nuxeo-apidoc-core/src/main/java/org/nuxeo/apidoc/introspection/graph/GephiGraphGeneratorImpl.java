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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphFactory;
import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.project.api.ProjectController;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.EditableGraph;
import org.nuxeo.apidoc.api.graph.GRAPH_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.introspection.graph.export.PlotlyGraphExporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.openide.util.Lookup;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class GephiGraphGeneratorImpl extends AbstractGraphGeneratorImpl {

    protected static String TIMEOUT_PROP = "forceLayout.timeout";

    public GephiGraphGeneratorImpl() {
        super();
    }

    @Override
    protected PositionedNodeImpl createNode(String id, String label, int weight, String path, String type,
            String category) {
        return new PositionedNodeImpl(id, label, weight, path, type, category);
    }

    @Override
    public List<Graph> getGraphs(DistributionSnapshot distribution) {
        EditableGraph defaultGraph = super.getDefaultGraph(distribution);

        EditableGraph completeGraph = applyForceLayout(distribution, defaultGraph.copy(null), NODE_TYPE.BUNDLE);
        completeGraph.setName(getName());
        completeGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        completeGraph.setTitle("Complete Force Graph");
        completeGraph.setDescription("Complete graph, with dependencies, with a force layout");
        ContentGraphImpl g1 = new PlotlyGraphExporter().export(completeGraph);

        EditableGraph bundleGraph = defaultGraph.copy(new NodeTypeFilter(NODE_TYPE.BUNDLE.name()));
        bundleGraph = applyForceLayout(distribution, bundleGraph, NODE_TYPE.BUNDLE);
        bundleGraph.setName(getName() + "bundles");
        bundleGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        bundleGraph.setTitle("Bundles Force Graph");
        bundleGraph.setDescription("Selected bundles, with dependencies, with a force layout");
        ContentGraphImpl g2 = new PlotlyGraphExporter().export(bundleGraph);

        EditableGraph xpGraph = defaultGraph.copy(new NodeTypeFilter(NODE_TYPE.COMPONENT.name(),
                NODE_TYPE.EXTENSION_POINT.name(), NODE_TYPE.CONTRIBUTION.name()));
        bundleGraph = applyForceLayout(distribution, xpGraph, NODE_TYPE.COMPONENT);
        bundleGraph.setName(getName() + "xp");
        bundleGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        bundleGraph.setTitle("XP Force Graph");
        bundleGraph.setDescription("Selected XP, with dependencies, with a force layout");
        ContentGraphImpl g3 = new PlotlyGraphExporter().export(xpGraph);

        // EditableGraph completeOOGraph = applyBundleOpenOrdLayout(distribution, defaultGraph.copy(null));
        // completeOOGraph.setName(getName() + "oo");
        // completeOOGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        // completeOOGraph.setTitle("Complete OpenOrd Graph");
        // completeOOGraph.setDescription("Complete graph, with dependencies, with a OpenOrd layout");
        // ContentGraphImpl g5 = new PlotlyGraphExporter().export(completeOOGraph);
        //
        // EditableGraph bundleOOGraph = completeOOGraph.copy(new NodeTypeFilter(NODE_TYPE.BUNDLE.name()));
        // bundleOOGraph.setName(getName() + "oobundles");
        // bundleOOGraph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        // bundleOOGraph.setTitle("Bundles OpenOrd Graph");
        // bundleOOGraph.setDescription("Selected bundles, with dependencies, with a OpenOrd layout");
        // ContentGraphImpl g6 = new PlotlyGraphExporter().export(bundleOOGraph);

        return Arrays.asList(g1, g2, g3);
    }

    protected AutoLayout initLayout(EditableGraph graph) {
        // Layout for 1 minute by default
        String timeout = graph.getProperty(TIMEOUT_PROP, "60");
        return new AutoLayout(Integer.valueOf(timeout), TimeUnit.SECONDS);
    }

    protected EditableGraph applyForceLayout(DistributionSnapshot distribution, EditableGraph graph,
            NODE_TYPE weightRef) {
        AutoLayout autoLayout = initLayout(graph);
        YifanHuLayout yhLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        ForceAtlasLayout forceLayout = new ForceAtlasLayout(null);
        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty(
                "forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f); // True after 10% of layout time
        AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty(
                "forceAtlas.repulsionStrength.name", 500., 0f); // 500 for the complete period
        autoLayout.addLayout(yhLayout, 0.5f);
        autoLayout.addLayout(forceLayout, 0.5f,
                new AutoLayout.DynamicProperty[] { adjustBySizeProperty, repulsionProperty });
        return applyGephiLayout(distribution, graph, weightRef, autoLayout);
    }

    protected EditableGraph applyBundleOpenOrdLayout(DistributionSnapshot distribution, EditableGraph graph,
            NODE_TYPE weightRef) {
        AutoLayout autoLayout = initLayout(graph);
        YifanHuLayout yhLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        // XXX: timeout does not apply to it :(
        OpenOrdLayout ooLayout = new OpenOrdLayout(null);
        ooLayout.resetPropertiesValues();
        AutoLayout.DynamicProperty numIterationsProperty = AutoLayout.createDynamicProperty(
                "OpenOrd.properties.numiterations.name", 10, 0f);
        autoLayout.addLayout(yhLayout, 0.5f);
        autoLayout.addLayout(ooLayout, 0.5f, new AutoLayout.DynamicProperty[] { numIterationsProperty });
        return applyGephiLayout(distribution, graph, weightRef, autoLayout);
    }

    protected EditableGraph applyGephiLayout(DistributionSnapshot distribution, EditableGraph graph,
            NODE_TYPE weightRef, AutoLayout layout) {
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
        // components)
        for (Node node : graph.getNodes()) {
            Integer weight = 1;
            NODE_TYPE ntype = NODE_TYPE.getType(node.getType());
            if (weightRef.equals(ntype)) {
                weight = node.getWeight() + 1;
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
        positionZ(graph);

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
                type.getIndex(), type.isDirected());
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
                // TODO: z is not positioned for now
                pnode.setZ(gnode.z());
            }
        }
    }

    protected void positionZ(EditableGraph graph) {
        for (Node node : graph.getNodes()) {
            if (node instanceof PositionedNodeImpl) {
                PositionedNodeImpl pnode = (PositionedNodeImpl) node;
                pnode.setZ(NODE_TYPE.getType(node.getType()).getZIndex());
            }
        }
    }

    protected void repositionOtherNodes(DistributionSnapshot distribution, EditableGraph graph, boolean fullLayout) {
        // reprocess all nodes to compute X Y Z
        // for (String bid : distribution.getBundleIds()) {
        // BundleInfo bundle = distribution.getBundle(bid);
        // Node bundleNode = graph.getNode(prefixId(BundleInfo.TYPE_NAME, bid));
        // for (ComponentInfo component : bundle.getComponents()) {
        // Node compNode = graph.getNode(prefixId(ComponentInfo.TYPE_NAME, component.getId()));
        // if (!fullLayout) {
        // copyXY(bundleNode, compNode);
        // }
        // push(bundleNode, compNode);
        // for (ServiceInfo service : component.getServices()) {
        // if (service.isOverriden()) {
        // continue;
        // }
        // Node serviceNode = graph.getNode(prefixId(ServiceInfo.TYPE_NAME, service.getId()));
        // if (!fullLayout) {
        // copyXY(bundleNode, serviceNode);
        // }
        // push(compNode, serviceNode);
        // }
        // for (ExtensionPointInfo xp : component.getExtensionPoints()) {
        // Node xpNode = graph.getNode(prefixId(ExtensionPointInfo.TYPE_NAME, xp.getId()));
        // if (!fullLayout) {
        // copyXY(bundleNode, xpNode);
        // }
        // push(compNode, xpNode);
        // }
        // Map<String, Integer> comps = new HashMap<String, Integer>();
        // for (ExtensionInfo contribution : component.getExtensions()) {
        // // handle multiple contributions to the same extension point
        // String cid = contribution.getId();
        // if (comps.containsKey(cid)) {
        // Integer num = comps.get(cid);
        // comps.put(cid, num + 1);
        // cid += "-" + String.valueOf(num + 1);
        // } else {
        // comps.put(cid, Integer.valueOf(0));
        // }
        //
        // Node contNode = graph.getNode(prefixId(ExtensionInfo.TYPE_NAME, cid));
        // if (!fullLayout) {
        // copyXY(compNode, contNode);
        // }
        // push(compNode, contNode);
        // }
        // }
        // }
    }

    protected void copyXY(Node source, Node target) {
        if (source instanceof PositionedNodeImpl && target instanceof PositionedNodeImpl) {
            ((PositionedNodeImpl) target).setX(((PositionedNodeImpl) source).getX());
            ((PositionedNodeImpl) target).setY(((PositionedNodeImpl) source).getY());
        }
    }

    protected void push(Node source, Node target) {
        if (source instanceof PositionedNodeImpl && target instanceof PositionedNodeImpl) {
            float z = ((PositionedNodeImpl) source).getZ();
            ((PositionedNodeImpl) target).setZ(z + 1);
        }
    }

}
