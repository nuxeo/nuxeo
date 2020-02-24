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

import org.jgrapht.ext.ExportException;
import org.jgrapht.ext.GmlExporter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class JGraphGeneratorImpl extends BasicGraphGeneratorImpl {

    public JGraphGeneratorImpl(String graphId, DistributionSnapshot distribution) {
        super(graphId, distribution);
    }

    public static Graph getGraph(String graphId, DistributionSnapshot distribution) {
        JGraphGeneratorImpl gen = new JGraphGeneratorImpl(graphId, distribution);
        return gen.getGraph();
    }

    @Override
    public Graph getGraph() {
        Graph graph = super.getGraph();

        // transform to get corresponding graph
        SimpleDirectedGraph<PositionedNodeImpl, DefaultEdge> g = new SimpleDirectedGraph<PositionedNodeImpl, DefaultEdge>(
                DefaultEdge.class);

        for (Node node : graph.getNodes()) {
            g.addVertex((PositionedNodeImpl) node);
        }

        for (Edge edge : graph.getEdges()) {
            Node source = graph.getNode(edge.getSource());
            Node target = graph.getNode(edge.getTarget());
            g.addEdge((PositionedNodeImpl) source, (PositionedNodeImpl) target);
        }

        try {
            GmlExporter<PositionedNodeImpl, DefaultEdge> exporter = new GmlExporter<PositionedNodeImpl, DefaultEdge>();
            exporter.exportGraph(g, new File("test.graphml"));
        } catch (ExportException e) {
            throw new RuntimeException(e);
        }

        return graph;
    }

    @Override
    protected PositionedNodeImpl createNode(String id, String label, int weight, String path, String type,
            String category, String color) {
        return new PositionedNodeImpl(id, label, weight, path, type, category, color);
    }

}
