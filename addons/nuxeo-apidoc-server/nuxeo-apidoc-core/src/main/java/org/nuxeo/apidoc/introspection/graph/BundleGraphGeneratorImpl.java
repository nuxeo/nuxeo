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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.GRAPH_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_CATEGORY;
import org.nuxeo.apidoc.api.graph.NODE_TYPE;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class BundleGraphGeneratorImpl extends BasicGraphGeneratorImpl {

    public BundleGraphGeneratorImpl() {
        super();
    }

    @Override
    public Graph getGraph(DistributionSnapshot distribution) {
        GraphImpl graph = (GraphImpl) createGraph();

        List<String> children = new ArrayList<>();

        for (String bid : distribution.getBundleIds()) {
            BundleInfo bundle = distribution.getBundle(bid);
            // add node for bundle
            NODE_CATEGORY cat = NODE_CATEGORY.getCategory(bundle);
            Node bundleNode = createBundleNode(bundle, cat);
            graph.addNode(bundleNode);
            for (String requirement : bundle.getRequirements()) {
                addEdge(graph, null, createEdge(bundleNode, createNode(prefixId(BundleInfo.TYPE_NAME, requirement)),
                        EDGE_TYPE.REQUIRES.name()));
            }
            if (bundle.getRequirements().isEmpty()) {
                children.add(bid);
            }
        }

        // add common root for all roots in the bundle graph, as it is supposed to be a directed tree without cycles
        List<Node> roots = new ArrayList<>();
        for (Node node : graph.getNodes()) {
            if (!children.contains(node.getOriginalId())) {
                roots.add(node);
            }
        }
        if (roots.size() > 1) {
            String pbid = prefixId(BundleInfo.TYPE_NAME, BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE);
            Node bundleNode = createNode(pbid, BundleInfo.RUNTIME_ROOT_PSEUDO_BUNDLE, 0, "", NODE_TYPE.BUNDLE.name(),
                    NODE_CATEGORY.RUNTIME.name(), NODE_CATEGORY.RUNTIME.getColor());
            graph.addNode(bundleNode);
            for (Node root : roots) {
                addEdge(graph, null, createEdge(root, bundleNode, EDGE_TYPE.REQUIRES.name()));
            }
        }

        refine(graph, null);

        Graph finalGraph = GephiLayout.getLayout(graph);

        return finalGraph;

    }

    protected Graph createGraph() {
        Graph graph = new GraphImpl(getGraphName());
        graph.setType(GRAPH_TYPE.BASIC_LAYOUT.name());
        syncGraphAttributes(graph);
        return graph;
    }

    @Override
    protected PositionedNodeImpl createNode(String id, String label, int weight, String path, String type,
            String category, String color) {
        return new PositionedNodeImpl(id, label, weight, path, type, category, color);
    }
}
