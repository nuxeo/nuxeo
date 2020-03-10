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
package org.nuxeo.apidoc.api.graph;

import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.introspection.graph.NodeFilter;

/**
 * @since 11.1
 */
public interface EditableGraph extends Graph {

    void setName(String name);

    void setTitle(String title);

    void setDescription(String description);

    void setType(String type);

    void addEdge(Edge edge);

    void addNode(Node node);

    Node getNode(String nodeId);

    List<Node> getNodes();

    List<Edge> getEdges();

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    String getProperty(String name, String defaultValue);

    EditableGraph copy(NodeFilter nodeFilter);

}
