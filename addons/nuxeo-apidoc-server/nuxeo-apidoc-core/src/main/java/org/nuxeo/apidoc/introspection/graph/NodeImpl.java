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

import org.nuxeo.apidoc.api.graph.Node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 11.1
 */
@JsonIgnoreType
public class NodeImpl implements Node {

    protected final String id;

    protected final String label;

    protected final int weight;

    protected final String path;

    protected final String type;

    protected final String category;

    protected final String color;

    @JsonCreator
    public NodeImpl(@JsonProperty("id") String id, @JsonProperty("label") String label,
            @JsonProperty("weight") int weight, @JsonProperty("path") String path, @JsonProperty("type") String type,
            @JsonProperty("category") String category, @JsonProperty("color") String color) {
        super();
        this.id = id;
        this.label = label;
        this.weight = weight;
        this.path = path;
        this.type = type;
        this.category = category;
        this.color = color;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

}
