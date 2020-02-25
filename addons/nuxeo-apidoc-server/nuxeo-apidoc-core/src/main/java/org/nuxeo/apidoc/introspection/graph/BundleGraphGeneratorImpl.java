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

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.graph.EDGE_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.NODE_CATEGORY;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class BundleGraphGeneratorImpl extends BasicGraphGeneratorImpl {

    public BundleGraphGeneratorImpl(String graphId, DistributionSnapshot distribution) {
        super(graphId, distribution);
    }

    public static Graph getGraph(String graphId, DistributionSnapshot distribution) {
        BundleGraphGeneratorImpl gen = new BundleGraphGeneratorImpl(graphId, distribution);
        return gen.getGraph();
    }

    @Override
    public Graph getGraph() {
        GraphImpl graph = new GraphImpl(graphId);

        Map<String, Integer> hits = new HashMap<>();

        for (String bid : distribution.getBundleIds()) {
            BundleInfo bundle = distribution.getBundle(bid);
            // add node for bundle
            NODE_CATEGORY cat = NODE_CATEGORY.getCategory(bundle);
            Node bundleNode = createBundleNode(bundle, cat);
            graph.addNode(bundleNode);
            for (String requirement : bundle.getRequirements()) {
                addEdge(graph, hits, createEdge(bundleNode, createNode(prefixId(BundleInfo.TYPE_NAME, requirement)),
                        EDGE_TYPE.REQUIRES.name()));
            }
        }

        refine(graph, hits);

        return graph;
    }

}
