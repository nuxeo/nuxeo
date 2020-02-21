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

import org.gephi.project.io.GephiReader;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class GephiGraphGeneratorImpl extends BasicGraphGeneratorImpl {

    public GephiGraphGeneratorImpl(String graphId, DistributionSnapshot distribution) {
        super(graphId, distribution);
    }

    public static Graph getGraph(String graphId, DistributionSnapshot distribution) {
        GephiGraphGeneratorImpl gen = new GephiGraphGeneratorImpl(graphId, distribution);
        return gen.getGraph();
    }

    @Override
    public Graph getGraph() {
        Graph graph = super.getGraph();
        // setup positioning of nodes (?)
        

        return graph;
    }

    @Override
    protected Node createNode(String id, String label, int weight, String path, String type, String category,
            String color) {
        return new PositionedNodeImpl(id, label, weight, path, type, category, color);
    }

}
