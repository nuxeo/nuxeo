/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FakeNuxeoArtifact implements NuxeoArtifact {

    public final String id;

    public String version;

    public final String type;

    public FakeNuxeoArtifact(NuxeoArtifact artifact) {
        id = artifact.getId();
        version = artifact.getVersion();
        type = artifact.getArtifactType();
    }

    @JsonCreator
    private FakeNuxeoArtifact(@JsonProperty("id") String id, @JsonProperty("version") String version,
            @JsonProperty("type") String type) {
        this.id = id;
        this.version = version;
        this.type = type;
    }

    @Override
    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public String getArtifactType() {
        return type;
    }

    @Override
    @JsonIgnore
    public String getHierarchyPath() {
        return null;
    }

}
