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

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Graph implementation holding exported content instead of generating it.
 *
 * @since 11.1
 */
public class ContentGraphImpl implements Graph {

    protected String name;

    protected String type;

    protected String title;

    protected String description;

    protected String content;

    protected String contentType;

    protected String contentName;

    @JsonCreator
    public ContentGraphImpl(@JsonProperty("name") String name) {
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
    @JsonIgnore
    public String getId() {
        return ARTIFACT_PREFIX + getName();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentName() {
        return contentName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    @Override
    @JsonIgnore
    public Blob getBlob() {
        Blob blob = Blobs.createBlob(getContent());
        blob.setFilename(getContentName());
        blob.setMimeType(getContentType());
        blob.setEncoding("UTF-8");
        return blob;
    }

    @Override
    public AssociatedDocuments getAssociatedDocuments(CoreSession session) {
        throw new UnsupportedOperationException();
    }

}
