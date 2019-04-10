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

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
public class ServiceInfoImpl extends BaseNuxeoArtifact implements ServiceInfo {

    protected final String serviceClassName;

    protected final ComponentInfo component;

    protected final boolean overriden;

    public ServiceInfoImpl(String serviceClassName, boolean overriden, ComponentInfo component) {
        this.serviceClassName = serviceClassName;
        this.overriden = overriden;
        this.component = component;
    }

    @Override
    public String getId() {
        return serviceClassName;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getVersion() {
        return component.getVersion();
    }

    @Override
    public String getComponentId() {
        return component.getId();
    }

    @Override
    public String getHierarchyPath() {
        return component.getHierarchyPath() + "/" + VirtualNodesConsts.Services_VNODE_NAME + "/" + getId();
    }

    @Override
    public boolean isOverriden() {
        return overriden;
    }
}
