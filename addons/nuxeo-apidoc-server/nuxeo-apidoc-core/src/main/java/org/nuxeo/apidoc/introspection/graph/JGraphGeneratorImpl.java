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

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.ComponentNameProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.ExportException;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.GRAPH_TYPE;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;

/**
 * Basic implementation relying on introspection of distribution using jgrapht library.
 *
 * @since 11.1
 */
public class JGraphGeneratorImpl extends BasicGraphGeneratorImpl {

    public JGraphGeneratorImpl() {
        super();
    }

    @Override
    public Graph getGraph(DistributionSnapshot distribution) {
        ContentGraphImpl graph = (ContentGraphImpl) super.getGraph(distribution);

        // transform to get corresponding graph
        SimpleDirectedGraph<Node, Edge> g = new SimpleDirectedGraph<Node, Edge>(Edge.class);

        for (Node node : graph.getNodes()) {
            g.addVertex(node);
        }

        for (Edge edge : graph.getEdges()) {
            Node source = graph.getNode(edge.getOriginalSourceId());
            Node target = graph.getNode(edge.getOriginalTargetId());
            g.addEdge(source, target, edge);
        }

        try {
            ComponentNameProvider<Node> vertexIDProvider = new ComponentNameProvider<>() {
                @Override
                public String getName(Node component) {
                    return String.valueOf(component.getId());
                }
            };
            ComponentNameProvider<Node> vertexLabelProvider = new ComponentNameProvider<Node>() {
                @Override
                public String getName(Node component) {
                    return component.getLabel();
                }
            };
            ComponentNameProvider<Edge> edgeLabelProvider = new ComponentNameProvider<Edge>() {
                @Override
                public String getName(Edge component) {
                    return component.getValue();
                }
            };
            ComponentAttributeProvider<Node> vertexAttributeProvider = new ComponentAttributeProvider<Node>() {
                public Map<String, String> getComponentAttributes(Node node) {
                    Map<String, String> map = new LinkedHashMap<String, String>();
                    map.put("weight", String.valueOf(node.getWeight()));
                    map.put("path", String.valueOf(node.getPath()));
                    map.put("color", String.valueOf(node.getColor()));
                    map.put("category", String.valueOf(node.getCategory()));
                    map.put("type", String.valueOf(node.getType()));
                    return map;
                }
            };
            DOTExporter<Node, Edge> exporter = new DOTExporter<>(vertexIDProvider, vertexLabelProvider,
                    edgeLabelProvider, vertexAttributeProvider, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            exporter.exportGraph(g, out);
            graph.setContent(out.toString());
            graph.setContentName("graph.dot");
            graph.setContentType("application/xml");
        } catch (ExportException e) {
            throw new RuntimeException(e);
        }

        return graph;
    }

    @Override
    protected Graph createGraph() {
        return new ContentGraphImpl(graphName, GRAPH_TYPE.BASIC.name(), getProperties());
    }

}
