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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.graph.Edge;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.api.graph.Node;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @since 11.1
 */
public class GraphImpl extends BaseNuxeoArtifact implements Graph {

    protected String id;

    protected String type;

    protected final Map<String, String> properties = new HashMap<>();

    protected final List<Node> nodes = new ArrayList<>();

    protected final List<Edge> edges = new ArrayList<>();

    protected final Map<String, Node> nodeMap = new HashMap<>();

    @JsonCreator
    public GraphImpl(@JsonProperty("id") String id, @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, String> properties) {
        super();
        this.id = id;
        this.type = type;
        setProperties(properties);
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
    @JsonIgnore
    public String getName() {
        return getId();
    }

    @Override
    public void setName(String name) {
        this.id = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
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

    protected String getJsonContent() {
        final ObjectMapper mapper = new ObjectMapper().registerModule(
                new SimpleModule().addAbstractTypeMapping(Node.class, NodeImpl.class)
                                  .addAbstractTypeMapping(Edge.class, EdgeImpl.class));
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("id", getId());
        values.put("type", getType());
        values.put("nodes", nodes);
        values.put("edges", edges);
        try {
            return mapper.writerFor(LinkedHashMap.class)
                         .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                         .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                         .withDefaultPrettyPrinter()
                         .writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @JsonIgnore
    public Blob getBlob() {
        Blob blob = Blobs.createBlob(getJsonContent());
        blob.setFilename("graph.json");
        blob.setMimeType("application/json");
        blob.setEncoding("UTF-8");
        return blob;
    }

    public void addNode(Node node) {
        this.nodes.add(node);
        this.nodeMap.put(node.getOriginalId(), node);
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

}
