/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class ExtensionPointInfoImpl extends BaseNuxeoArtifact implements ExtensionPointInfo {

    protected ComponentInfoImpl component;
    protected String name;
    protected String[] types;
    protected Collection<ExtensionInfo> extensions = new ArrayList<ExtensionInfo>();

    protected String documentation;

    protected List<Class> spi = new ArrayList<Class>();

    public ExtensionPointInfoImpl(ComponentInfoImpl component, String name) {
        this.name = name;
        this.component = component;
    }

    public ComponentInfoImpl getComponent() {
        return component;
    }

    public String getName() {
        return name;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

    public String[] getTypes() {
        return types;
    }

    public Collection<ExtensionInfo> getExtensions() {
        return extensions;
    }

    public void addExtension(ExtensionInfoImpl xt) {
        this.extensions.add(xt);
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void addSpi(List<Class> spi) {
        this.spi.addAll(spi);
    }

    @Override
    public String getId() {
        return getComponent().getId() + "--" +  getName();
    }

    public String getVersion() {
        return component.getVersion();
    }

    public String getArtifactType() {
        return ExtensionPointInfo.TYPE_NAME;
    }

    public String getLabel() {
        return getName() + " (" + getComponent().getId() + ")";
    }
}
