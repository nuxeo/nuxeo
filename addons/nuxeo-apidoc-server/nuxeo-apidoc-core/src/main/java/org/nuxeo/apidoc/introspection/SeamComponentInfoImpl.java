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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.SeamComponentInfo;

public class SeamComponentInfoImpl extends BaseNuxeoArtifact implements
        SeamComponentInfo {

    protected String name;

    protected String scope;

    protected String precedence;

    protected String className;

    protected final List<String> interfaceNames = new ArrayList<String>();

    protected String version;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public String getPrecedence() {
        return precedence;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void addInterfaceName(String name) {
        if (!interfaceNames.contains(name)) {
            interfaceNames.add(name);
        }
    }

    @Override
    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    @Override
    public String getArtifactType() {
        return SeamComponentInfo.TYPE_NAME;
    }

    @Override
    public String getId() {
        return "seam:" + getName();
    }

    @Override
    public String getHierarchyPath() {
        return "/";
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int compareTo(SeamComponentInfo o) {
        int c = getName().compareToIgnoreCase(o.getName());
        if (c != 0) {
            return c;
        }
        return getClassName().compareTo(o.getClassName());
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setPrecedence(String precedence) {
        this.precedence = precedence;
    }

    public void setClassName(String className) {
        this.className = className;
    }

}
