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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.DocumentationHelper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtensionPointInfoImpl extends BaseNuxeoArtifact implements ExtensionPointInfo {

    protected final ComponentInfo component;

    protected final String componentId;

    protected final String name;

    protected final List<ExtensionInfo> extensions = new ArrayList<>();

    protected final List<Class<?>> spi = new ArrayList<>();

    protected String[] descriptors;

    protected String documentation;

    public ExtensionPointInfoImpl(ComponentInfoImpl component, String name) {
        this.component = component;
        this.componentId = component.getId();
        this.name = name;
    }

    @JsonCreator
    private ExtensionPointInfoImpl(@JsonProperty("componentId") String componentId, @JsonProperty("name") String name,
            @JsonProperty("descriptors") String[] descriptors, @JsonProperty("documentation") String documentation) {
        this.component = null; // will be handled by json back reference
        this.componentId = componentId; // kept here to ensure id resolution during json deserialization
        this.name = name;
        this.descriptors = descriptors;
        this.documentation = documentation;
    }

    @Override
    public ComponentInfo getComponent() {
        return component;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setDescriptors(String[] descriptors) {
        this.descriptors = descriptors;
    }

    @Override
    public String[] getDescriptors() {
        return descriptors;
    }

    @Override
    public List<ExtensionInfo> getExtensions() {
        return extensions;
    }

    public void addExtension(ExtensionInfoImpl xt) {
        extensions.add(xt);
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String getDocumentationHtml() {
        return DocumentationHelper.getHtml(getDocumentation());
    }

    public void addSpi(List<Class<?>> spi) {
        this.spi.addAll(spi);
    }

    @Override
    public String getId() {
        return getComponentId() + "--" + getName();
    }

    @Override
    public String getVersion() {
        return component.getVersion();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getLabel() {
        return getName() + " (" + getComponentId() + ")";
    }

    @Override
    public String getHierarchyPath() {
        return component.getHierarchyPath() + "/" + VirtualNodesConsts.ExtensionPoints_VNODE_NAME + "/" + getId();
    }

}
