/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.introspection;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for an {@link OperationInfo}, used for the runtime implementation.
 */
public class OperationInfoImpl extends BaseNuxeoArtifact implements OperationInfo {

    protected final String name;

    protected final String version;

    protected final String[] aliases;

    protected final String operationClass;

    protected final String contributingComponent;

    protected final String description;

    protected final String[] signature;

    protected final String category;

    protected final String url;

    protected final String label;

    protected final String requires;

    protected final String since;

    protected final List<Param> params;

    public OperationInfoImpl(@JsonProperty("name") String name, @JsonProperty("version") String version,
            @JsonProperty("aliases") String[] aliases, @JsonProperty("description") String description,
            @JsonProperty("operationClass") String operationClass,
            @JsonProperty("contributingComponent") String contributingComponent,
            @JsonProperty("signature") String[] signature, @JsonProperty("category") String category,
            @JsonProperty("url") String url, @JsonProperty("label") String label,
            @JsonProperty("requires") String requires, @JsonProperty("since") String since,
            @JsonProperty("params") List<Param> params) {

        this.name = name;
        this.version = version;
        this.aliases = aliases;
        this.description = description;
        this.operationClass = operationClass;
        if (contributingComponent == null || contributingComponent.isEmpty()) {
            this.contributingComponent = OperationInfo.BUILT_IN;
        } else {
            String[] parts = contributingComponent.split(":");
            if (parts.length > 1) {
                this.contributingComponent = parts[1];
            } else {
                this.contributingComponent = contributingComponent;
            }
        }
        this.signature = signature;
        this.category = category;
        this.url = url;
        this.label = label;
        this.requires = requires;
        this.since = since;
        this.params = params;
    }

    public OperationInfoImpl(OperationDocumentation op, String version, String operationClass,
            String contributingComponent) {
        this(op.getId(), version, op.getAliases(), op.getDescription(), operationClass, contributingComponent,
                op.getSignature(), op.getCategory(), op.getUrl(), op.getLabel(), op.getRequires(), op.getSince(),
                Arrays.asList(op.getParams()));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return ARTIFACT_PREFIX + getName();
    }

    @Override
    public String[] getAliases() {
        return aliases;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String[] getSignature() {
        return signature;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getRequires() {
        return requires;
    }

    @Override
    public String getSince() {
        return since;
    }

    @Override
    public List<Param> getParams() {
        return params;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return "/" + getId();
    }

    @Override
    public int compareTo(OperationInfo o) {
        String s1 = getLabel() == null ? getId() : getLabel();
        String s2 = o.getLabel() == null ? o.getId() : o.getLabel();
        return s1.compareTo(s2);
    }

    @Override
    public String getOperationClass() {
        return operationClass;
    }

    @Override
    public String getContributingComponent() {
        return contributingComponent;
    }

}
