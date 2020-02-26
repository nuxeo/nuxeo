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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gephi.io.exporter.api.ExportController;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_CATEGORY;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.openide.util.Lookup;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class GephiGraphGeneratorImpl extends BundleGraphGeneratorImpl {

    public GephiGraphGeneratorImpl() {
        super();
    }

    @Override
    public Graph getGraph(DistributionSnapshot distribution) {
        GraphImpl graph = (GraphImpl) super.getGraph(distribution);
        graph = (GraphImpl) GephiLayout.getLayout(graph);

        // build the rest of the tree on top of that, adjusting positions

        Map<String, Integer> hits = new HashMap<>();

        for (String bid : distribution.getBundleIds()) {
            BundleInfo bundle = distribution.getBundle(bid);
            Node bundleNode = graph.getNode(prefixId(BundleInfo.TYPE_NAME, bid));
            NODE_CATEGORY cat = NODE_CATEGORY.getCategory(bundleNode.getCategory());
            // compute sub components
            List<ComponentInfo> components = bundle.getComponents();
            for (ComponentInfo component : components) {
                Node compNode = createComponentNode(component, cat);
                copyXY(bundleNode, compNode);
                graph.addNode(compNode);
                add3DEdge(graph, hits, createEdge(bundleNode, compNode, EDGE_TYPE.CONTAINS.name()), bundleNode,
                        compNode);
                for (ServiceInfo service : component.getServices()) {
                    if (service.isOverriden()) {
                        continue;
                    }
                    Node serviceNode = createServiceNode(service, cat);
                    copyXY(bundleNode, serviceNode);
                    graph.addNode(serviceNode);
                    add3DEdge(graph, hits, createEdge(compNode, serviceNode, EDGE_TYPE.CONTAINS.name()), compNode,
                            serviceNode);
                }

                for (ExtensionPointInfo xp : component.getExtensionPoints()) {
                    Node xpNode = createXPNode(xp, cat);
                    copyXY(bundleNode, xpNode);
                    graph.addNode(xpNode);
                    add3DEdge(graph, hits, createEdge(compNode, xpNode, EDGE_TYPE.CONTAINS.name()), compNode, xpNode);
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

                    Node contNode = createContributionNode(contribution, cat);
                    copyXY(compNode, contNode);
                    graph.addNode(contNode);
                    // add link to corresponding component
                    add3DEdge(graph, hits, createEdge(compNode, contNode, EDGE_TYPE.CONTAINS.name()), compNode,
                            contNode);

                    // also add link to target extension point, "guessing" the extension point id, not counting for
                    // hits
                    String targetId = prefixId(ComponentInfo.TYPE_NAME,
                            contribution.getTargetComponentName() + "--" + contribution.getExtensionPoint());
                    addEdge(graph, null, createEdge(createNode(targetId), contNode, EDGE_TYPE.REFERENCES.name()));

                    // compute requirements
                    for (String requirement : component.getRequirements()) {
                        // XX: setup deps!
                        addEdge(graph, hits, createEdge(compNode,
                                createNode(prefixId(ComponentInfo.TYPE_NAME, requirement)), EDGE_TYPE.REQUIRES.name()));
                    }
                }
            }
        }

        refine(graph, hits);

        // Export
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File("autolayout3.pdf"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return graph;
    }

    protected void copyXY(Node source, Node target) {
        if (source instanceof PositionedNodeImpl && target instanceof PositionedNodeImpl) {
            ((PositionedNodeImpl) target).setX(((PositionedNodeImpl) source).getX());
            ((PositionedNodeImpl) target).setY(((PositionedNodeImpl) source).getY());
        }
    }

    protected void add3DEdge(Graph graph, Map<String, Integer> hits, Edge edge, Node source, Node target) {
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
