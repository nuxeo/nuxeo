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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.documentation.XMLContributionParser;
import org.nuxeo.runtime.model.ComponentName;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtensionInfoImpl extends BaseNuxeoArtifact implements ExtensionInfo {

    protected static final Log log = LogFactory.getLog(ExtensionInfoImpl.class);

    protected final String id;

    protected final ComponentInfo component;

    protected final String extensionPoint;

    protected String documentation;

    protected String xml;

    protected ComponentName targetComponentName;

    protected Object[] contribution;

    public ExtensionInfoImpl(ComponentInfo component, String extensionPoint, int index) {
        String id = component.getId() + "--" + extensionPoint;
        if (index > 0) {
            id += index;
        }
        this.id = id;
        this.component = component;
        this.extensionPoint = extensionPoint;
    }

    @JsonCreator
    private ExtensionInfoImpl(@JsonProperty("id") String id, @JsonProperty("extensionPoint") String extensionPoint,
            @JsonProperty("documentation") String documentation, @JsonProperty("xml") String xml,
            @JsonProperty("targetComponentName") ComponentName targetComponentName) {
        this.id = id;
        this.component = null; // will be handled by json back reference
        if (extensionPoint != null) {
            extensionPoint = extensionPoint.substring(extensionPoint.lastIndexOf("--") + 2);
        }
        this.extensionPoint = extensionPoint;
        this.documentation = documentation;
        this.xml = xml;
        this.targetComponentName = targetComponentName;
    }

    @Override
    public String getExtensionPoint() {
        return targetComponentName.getName() + "--" + extensionPoint;
    }

    @Override
    public String getId() {
        return id;
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

    @Override
    public ComponentName getTargetComponentName() {
        return targetComponentName;
    }

    public void setTargetComponentName(ComponentName targetComponentName) {
        this.targetComponentName = targetComponentName;
    }

    @JsonIgnore
    public Object[] getContribution() {
        return contribution;
    }

    public void setContribution(Object[] contribution) {
        this.contribution = contribution;
    }

    @Override
    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    @Override
    public String getVersion() {
        return component.getVersion();
    }

    @Override
    public String getArtifactType() {
        return ExtensionInfo.TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return component.getHierarchyPath() + "/" + VirtualNodesConsts.Contributions_VNODE_NAME + "/" + getId();
    }

    @Override
    public List<ContributionItem> getContributionItems() {
        try {
            return XMLContributionParser.extractContributionItems(getXml());
        } catch (DocumentException e) {
            log.error(e, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ComponentInfo getComponent() {
        return component;
    }
}
