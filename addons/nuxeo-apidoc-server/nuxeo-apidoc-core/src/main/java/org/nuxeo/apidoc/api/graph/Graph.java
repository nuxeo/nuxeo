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

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @since 11.1
 */
public interface Graph extends NuxeoArtifact {

    /** Prefix for {@link #getId}. */
    String ARTIFACT_PREFIX = "graph:";

    String ARTIFACT_TYPE = "graph";

    String TYPE_NAME = "NXGraph";

    String PROP_GRAPH_TYPE = "nxgraph:type";

    String getName();

    void setName(String name);

    String getTitle();

    void setTitle(String title);

    String getDescription();

    void setDescription(String description);

    Map<String, String> getProperties();

    String getProperty(String name, String defaultValue);

    void setProperties(Map<String, String> properties);

    String getType();

    void setType(String type);

    Blob getBlob();

    void addEdge(Edge edge);

    Node getNode(String nodeId);

    List<Node> getNodes();

    List<Edge> getEdges();

}
