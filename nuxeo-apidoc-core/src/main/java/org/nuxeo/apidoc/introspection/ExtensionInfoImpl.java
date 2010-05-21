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

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.runtime.model.ComponentName;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class ExtensionInfoImpl extends BaseNuxeoArtifact implements ExtensionInfo {

    protected String id;
    protected ComponentInfoImpl component;
    protected String extensionPoint;

    protected String documentation;

    protected String xml;

    protected ComponentName targetComponentName;

    protected Object[] contribution;

    public ExtensionInfoImpl(ComponentInfoImpl component, String xpoint) {
        this.id = component.getId() + "--" + xpoint;
        this.component = component;
        this.extensionPoint = xpoint;
    }

    public String getExtensionPoint() {
        return targetComponentName.getName() + "--" + extensionPoint;
    }

    public String getComponentName() {
        return component.getId();
    }

    public ComponentInfoImpl getComponent() {
        return component;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public String getDocumentation() {
        return documentation;
    }

    public ComponentName getTargetComponentName() {
        return targetComponentName;
    }

    public void setTargetComponentName(ComponentName targetComponentName) {
        this.targetComponentName = targetComponentName;
    }

    public Object[] getContribution() {
        return contribution;
    }

    public void setContribution(Object[] contribution) {
        this.contribution = contribution;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getVersion() {
        return component.getVersion();
    }

    public String getArtifactType() {
        return ExtensionInfo.TYPE_NAME;
    }

    public String getHierarchyPath() {
        return component.getHierarchyPath() + "/" + getId();
    }

}
