/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.DocumentationHelper;

public class ExtensionPointInfoImpl extends BaseNuxeoArtifact implements
        ExtensionPointInfo {

    protected final ComponentInfoImpl component;

    protected final String name;

    protected final Collection<ExtensionInfo> extensions = new ArrayList<ExtensionInfo>();

    protected final List<Class<?>> spi = new ArrayList<Class<?>>();

    protected String[] descriptors;

    protected String documentation;

    public ExtensionPointInfoImpl(ComponentInfoImpl component, String name) {
        this.name = name;
        this.component = component;
    }

    @Override
    public ComponentInfoImpl getComponent() {
        return component;
    }

    @Override
    public String getComponentId() {
        return component.getId();
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
    public Collection<ExtensionInfo> getExtensions() {
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
        return component.getId() + "--" + name;
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
        return name + " (" + component.getId() + ")";
    }

    @Override
    public String getHierarchyPath() {
        return component.getHierarchyPath() + "/"
                + VirtualNodesConsts.ExtensionPoints_VNODE_NAME + "/" + getId();
    }

}
