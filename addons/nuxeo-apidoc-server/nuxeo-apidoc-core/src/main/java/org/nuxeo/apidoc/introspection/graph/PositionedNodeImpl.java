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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Node implentation with positioning information.
 *
 * @since 11.1
 */
@JsonIgnoreType
public class PositionedNodeImpl extends NodeImpl {

    protected float x = 0;

    protected float y = 0;

    protected float z = 0;

    public PositionedNodeImpl(@JsonProperty("originalId") String originalId, @JsonProperty("label") String label,
            @JsonProperty("weight") int weight, @JsonProperty("path") String path, @JsonProperty("type") String type,
            @JsonProperty("category") String category, @JsonProperty("color") String color) {
        super(originalId, label, weight, path, type, category, color);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

}
