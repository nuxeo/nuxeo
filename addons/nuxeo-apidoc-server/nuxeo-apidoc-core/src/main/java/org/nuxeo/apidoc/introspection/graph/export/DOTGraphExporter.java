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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.ComponentNameProvider;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.ExportException;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.EditableGraph;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.introspection.graph.ContentGraphImpl;

/**
 * Exporter for DOT graph format, relying on jgrapht library.
 *
 * @since 11.1
 */
public class DOTGraphExporter extends AbstractGraphExporter implements GraphExporter {

    @Override
    public ContentGraphImpl export(EditableGraph graph) {
        ContentGraphImpl cgraph = initGraph(graph);

        SimpleDirectedGraph<IdNode, Edge> g = new SimpleDirectedGraph<>(Edge.class);

        int itemIndex = 1;
        Map<String, IdNode> idMap = new HashMap<>();
        for (Node node : graph.getNodes()) {
            IdNode idNode = new IdNode(itemIndex, node);
            g.addVertex(idNode);
            idMap.put(node.getId(), idNode);
            itemIndex++;
        }

        for (Edge edge : graph.getEdges()) {
            Node source = graph.getNode(edge.getSource());
            Node target = graph.getNode(edge.getTarget());
            g.addEdge(idMap.get(source.getId()), idMap.get(target.getId()), edge);
        }

        try {
            ComponentNameProvider<IdNode> vertexIDProvider = new ComponentNameProvider<>() {
                @Override
                public String getName(IdNode idNode) {
                    return String.valueOf(idNode.getId());
                }
            };
            ComponentNameProvider<IdNode> vertexLabelProvider = new ComponentNameProvider<>() {
                @Override
                public String getName(IdNode idNode) {
                    return idNode.getNode().getLabel();
                }
            };
            ComponentNameProvider<Edge> edgeLabelProvider = new ComponentNameProvider<>() {
                @Override
                public String getName(Edge edge) {
                    return edge.getValue();
                }
            };
            ComponentAttributeProvider<IdNode> vertexAttributeProvider = new ComponentAttributeProvider<>() {
                public Map<String, String> getComponentAttributes(IdNode idNode) {
                    Map<String, String> map = new LinkedHashMap<String, String>();
                    Node node = idNode.getNode();
                    map.put("weight", String.valueOf(node.getWeight()));
                    map.put("path", String.valueOf(node.getPath()));
                    map.put("category", String.valueOf(node.getCategory()));
                    map.put("type", String.valueOf(node.getType()));
                    return map;
                }
            };
            DOTExporter<IdNode, Edge> exporter = new DOTExporter<>(vertexIDProvider, vertexLabelProvider,
                    edgeLabelProvider, vertexAttributeProvider, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            exporter.exportGraph(g, out);
            cgraph.setContent(out.toString());
            cgraph.setContentName("graph.dot");
            cgraph.setContentType("application/xml");
        } catch (ExportException e) {
            throw new RuntimeException(e);
        }

        return cgraph;
    }

    class IdNode {

        int id;

        Node node;

        public IdNode(int id, Node node) {
            super();
            this.id = id;
            this.node = node;
        }

        public int getId() {
            return id;
        }

        public Node getNode() {
            return node;
        }

        @Override
        public String toString() {
            return "IdNode(" + id + ", " + node.getId() + ")";
        }

    }

}