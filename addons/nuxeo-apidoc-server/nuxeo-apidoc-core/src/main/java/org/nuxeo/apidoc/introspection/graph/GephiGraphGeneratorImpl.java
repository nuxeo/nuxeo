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

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.EditableGraph;
import org.nuxeo.apidoc.api.graph.GRAPH_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.introspection.graph.export.PlotlyGraphExporter;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class GephiGraphGeneratorImpl extends AbstractGraphGeneratorImpl {

    public GephiGraphGeneratorImpl() {
        super();
    }

    @Override
    public List<Graph> getGraphs(DistributionSnapshot distribution) {
        EditableGraph graph = getDefaultGraph(distribution);
        graph.setName(getName());
        graph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        graph.setTitle("Complete Graph");
        graph.setDescription("Complete graph, with dependencies, with a layout");

        ContentGraphImpl g1 = new PlotlyGraphExporter().export(graph);

        EditableGraph copy = graph.copy(new NodeTypeFilter(NODE_TYPE.BUNDLE.name()));
        copy.setName("bundles");
        copy.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        copy.setTitle("Bundles Graph");
        copy.setDescription("Selected bundles, with dependencies, with a layout");
        ContentGraphImpl g2 = new PlotlyGraphExporter().export(copy);

        return Arrays.asList(g1, g2);
    }

    @Override
    public EditableGraph getDefaultGraph(DistributionSnapshot distribution) {
        EditableGraph ograph = super.getDefaultGraph(distribution);

        // process Gephi layouting
        EditableGraph graph = GephiLayout.getLayout(ograph);

        // reprocess all nodes to compute X Y Z
        for (String bid : distribution.getBundleIds()) {
            BundleInfo bundle = distribution.getBundle(bid);
            Node bundleNode = graph.getNode(prefixId(BundleInfo.TYPE_NAME, bid));
            for (ComponentInfo component : bundle.getComponents()) {
                Node compNode = graph.getNode(prefixId(ComponentInfo.TYPE_NAME, component.getId()));
                copyXY(bundleNode, compNode);
                push((PositionedNodeImpl) bundleNode, (PositionedNodeImpl) compNode);
                for (ServiceInfo service : component.getServices()) {
                    if (service.isOverriden()) {
                        continue;
                    }
                    Node serviceNode = graph.getNode(prefixId(ServiceInfo.TYPE_NAME, service.getId()));
                    copyXY(bundleNode, serviceNode);
                    push((PositionedNodeImpl) compNode, (PositionedNodeImpl) serviceNode);
                }
                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    Node xpNode = graph.getNode(prefixId(ExtensionPointInfo.TYPE_NAME, xp.getId()));
                    copyXY(bundleNode, xpNode);
                    push((PositionedNodeImpl) compNode, (PositionedNodeImpl) xpNode);
                }
                Map<String, Integer> comps = new HashMap<String, Integer>();
                for (ExtensionInfo contribution : component.getExtensions()) {
                    // handle multiple contributions to the same extension point
                    String cid = contribution.getId();
                    if (comps.containsKey(cid)) {
                        Integer num = comps.get(cid);
                        comps.put(cid, num + 1);
                        cid += "-" + String.valueOf(num + 1);
                    } else {
                        comps.put(cid, Integer.valueOf(0));
                    }

                    Node contNode = graph.getNode(prefixId(ExtensionInfo.TYPE_NAME, cid));
                    copyXY(compNode, contNode);
                    push((PositionedNodeImpl) compNode, (PositionedNodeImpl) contNode);
                }
            }
        }

        return graph;
    }

    @Override
    protected PositionedNodeImpl createNode(String id, String label, int weight, String path, String type,
            String category) {
        return new PositionedNodeImpl(id, label, weight, path, type, category);
    }

    protected void copyXY(Node source, Node target) {
        if (source instanceof PositionedNodeImpl && target instanceof PositionedNodeImpl) {
            ((PositionedNodeImpl) target).setX(((PositionedNodeImpl) source).getX());
            ((PositionedNodeImpl) target).setY(((PositionedNodeImpl) source).getY());
        }
    }

    protected void add3DEdge(EditableGraph graph, Map<String, Integer> hits, Edge edge, Node source, Node target) {
        super.addEdge(graph, hits, edge);
        push((PositionedNodeImpl) target, (PositionedNodeImpl) source);
    }

    protected void push(PositionedNodeImpl node, PositionedNodeImpl source) {
        if (source == null) {
            float z = node.getZ();
            node.setZ(z + 1);
        } else {
            float z = source.getZ();
            node.setZ(z + 1);
        }
    }

}
