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

import org.nuxeo.apidoc.api.graph.Edge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 11.1
 */
@JsonIgnoreType
public class EdgeImpl implements Edge {

    protected int id = 0;

    protected int source = 0;

    protected final String originalSourceId;

    protected int target = 0;

    protected final String originalTargetId;

    protected final String value;

    @JsonCreator
    public EdgeImpl(@JsonProperty("originalSourceId") String originalSourceId,
            @JsonProperty("originalTargetId") String originalTargetId, @JsonProperty("value") String value) {
        super();
        this.originalSourceId = originalSourceId;
        this.originalTargetId = originalTargetId;
        this.value = value;
    }

    @Override
    public String getOriginalSourceId() {
        return originalSourceId;
    }

    @Override
    public String getOriginalTargetId() {
        return originalTargetId;
    }

    @Override
    public String getValue() {
        return value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

}
