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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.EditableGraph;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.apidoc.introspection.graph.export.GraphExporter;
import org.nuxeo.ecm.core.api.Blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Graph implementation supporting edition logics, but does not handle the content generation.
 *
 * @see ContentGraphImpl and @see {@link GraphExporter} for content export logics.
 * @since 11.1
 */
public class GraphImpl extends BaseNuxeoArtifact implements EditableGraph {

    protected String name;

    protected String type;

    protected String title;

    protected String description;

    protected final Map<String, String> properties = new HashMap<>();

    protected final List<Node> nodes = new ArrayList<>();

    protected final List<Edge> edges = new ArrayList<>();

    protected final Map<String, Node> nodeMap = new HashMap<>();

    @JsonCreator
    public GraphImpl(@JsonProperty("name") String name) {
        super();
        this.name = name;
    }

    @Override
    @JsonIgnore
    public String getVersion() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getArtifactType() {
        return ARTIFACT_TYPE;
    }

    @Override
    @JsonIgnore
    public String getHierarchyPath() {
        return null;
    }

    @Override
    public String getId() {
        return ARTIFACT_PREFIX + getName();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String getProperty(String name, String defaultValue) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        }
        return defaultValue;
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    public void addNode(Node node) {
        this.nodes.add(node);
        this.nodeMap.put(node.getId(), node);
    }

    public void addEdge(Edge edge) {
        this.edges.add(edge);
    }

    public Node getNode(String id) {
        return nodeMap.get(id);
    }

    @Override
    @JsonIgnore
    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    @JsonIgnore
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public Blob getBlob() {
        throw new UnsupportedOperationException();
    }

    public EditableGraph copy(NodeFilter nodeFilter) {
        GraphImpl g = new GraphImpl(getName());
        g.setTitle(getTitle());
        g.setDescription(getDescription());
        g.setType(getType());
        g.setProperties(getProperties());
        for (Node node : getNodes()) {
            if (nodeFilter == null || nodeFilter.accept(node)) {
                g.addNode(node.copy());
            }
        }
        for (Edge edge : getEdges()) {
            if (nodeFilter == null) {
                g.addEdge(edge.copy());
            } else {
                Node source = getNode(edge.getSource());
                Node target = getNode(edge.getTarget());
                if (nodeFilter.accept(source) && nodeFilter.accept(target)) {
                    g.addEdge(edge.copy());
                }
            }
        }
        return g;
    }

}