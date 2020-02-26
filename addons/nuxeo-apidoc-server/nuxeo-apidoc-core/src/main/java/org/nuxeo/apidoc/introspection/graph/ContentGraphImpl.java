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

import java.util.Map;

import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Graph implementation holding exported content instead of generating it.
 *
 * @since 11.1
 */
public class ContentGraphImpl extends GraphImpl implements Graph {

    protected String content;

    protected String contentType;

    protected String contentName;

    @JsonCreator
    public ContentGraphImpl(@JsonProperty("id") String id, @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, String> properties) {
        super(id, type, properties);
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
    public Blob getBlob() {
        Blob blob = Blobs.createBlob(getContent());
        blob.setFilename(getContentName());
        blob.setMimeType(getContentType());
        blob.setEncoding("UTF-8");
        return blob;
    }

}
