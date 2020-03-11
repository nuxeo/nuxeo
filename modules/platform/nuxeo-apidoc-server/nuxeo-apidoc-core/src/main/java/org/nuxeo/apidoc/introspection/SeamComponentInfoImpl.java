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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.SeamComponentInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
public class SeamComponentInfoImpl extends BaseNuxeoArtifact implements SeamComponentInfo {

    protected String name;

    protected String scope;

    protected String precedence;

    protected String className;

    protected final List<String> interfaceNames = new ArrayList<>();

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
