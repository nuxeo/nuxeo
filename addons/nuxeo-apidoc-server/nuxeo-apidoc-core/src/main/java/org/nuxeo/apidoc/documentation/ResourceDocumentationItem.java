/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.documentation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.AbstractDocumentationItem;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;

import com.cforcoding.jmd.MarkDownParserAndSanitizer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceDocumentationItem extends AbstractDocumentationItem implements DocumentationItem {

    protected final String content;

    protected final String filename;

    protected final String target;

    protected final String targetType;

    protected final String type;

    protected final List<String> applicableVersion;

    @JsonCreator
    public ResourceDocumentationItem(@JsonProperty("filename") String filename, @JsonProperty("content") String content,
            @JsonProperty("type") String type, @JsonProperty("target") String target, @JsonProperty("targetType") String targetType,
            @JsonProperty("applicableVersion") List<String> applicableVersion, @JsonProperty("typeLabel") String typeLabel) {
        super(typeLabel);
        this.content = content;
        this.filename = filename;
        this.type = type;
        this.target = target;
        this.targetType = targetType;
        this.applicableVersion = applicableVersion;
    }


    public ResourceDocumentationItem(String filename, String content, String type, NuxeoArtifact target) {
        this(filename, content, type, target.getId(), target.getArtifactType(), Arrays.asList(target.getVersion()), typeLabelOf(type));
    }

    public ResourceDocumentationItem(ResourceDocumentationItem other, NuxeoArtifact target) {
        this(other.filename, other.content, other.type, target);
    }

    @Override
    @JsonIgnore
    public String getTitle() {
        return getCleanName() + " " + target;
    }

    protected String getCleanName() {
        if (filename == null || filename.toLowerCase().startsWith("readme")) {
            return "ReadMe";
        }
        int idx = filename.indexOf(".");
        if (idx > 0) {
            return filename.substring(0, idx);
        }
        return filename;
    }

    @Override
    @JsonIgnore
    public String getContent() {
        MarkDownParserAndSanitizer parser = new MarkDownParserAndSanitizer();
        String xHtml = parser.transform(content);
        return xHtml;
    }

    @Override
    @JsonIgnore
    public String getType() {
        return type;
    }

    @Override
    @JsonIgnore
    public String getRenderingType() {
        return "html";
    }

    @Override
    public List<String> getApplicableVersion() {
        return applicableVersion;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String getTargetType() {
        return targetType;
    }

    @Override
    @JsonIgnore
    public boolean isApproved() {
        return true;
    }

    @Override
    @JsonIgnore
    public String getId() {
        return getTargetType() + "--" + filename;
    }

    @Override
    @JsonIgnore
    public String getUUID() {
        return null;
    }

    @Override
    @JsonIgnore
    public Map<String, String> getAttachments() {
        return new HashMap<>();
    }

    @Override
    @JsonIgnore
    public boolean isPlaceHolder() {
        return true;
    }

    @Override
    @JsonIgnore
    public String getEditId() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isReadOnly() {
        return true;
    }
}
