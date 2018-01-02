/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;

/**
 * @since 7.3
 */
@XObject("bundle")
public class ResourceBundleDescriptor implements ResourceBundle {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    public String name;

    @XNode("resources@append")
    boolean append;

    @XNodeList(value = "resources/resource", type = ArrayList.class, componentType = String.class)
    List<String> resources;

    public String getName() {
        return name;
    }

    public List<String> getResources() {
        return resources;
    }

    public boolean isAppend() {
        return append;
    }

    /**
     * @since 7.4
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @since 7.4
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * @since 7.4
     */
    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public ResourceBundleDescriptor clone() {
        ResourceBundleDescriptor c = new ResourceBundleDescriptor();
        c.name = name;
        c.append = append;
        if (resources != null) {
            c.resources = new ArrayList<>(resources);
        }
        return c;
    }

    @Override
    public ResourceBundle merge(ResourceBundle other) {
        if (other instanceof ResourceBundleDescriptor) {
            boolean append = ((ResourceBundleDescriptor) other).isAppend();
            List<String> res = other.getResources();
            List<String> merged = new ArrayList<>();
            if (append && resources != null) {
                merged.addAll(resources);
            }
            if (res != null) {
                merged.addAll(res);
            }
            resources = merged;
        }
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceBundleDescriptor)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        ResourceBundleDescriptor b = (ResourceBundleDescriptor) obj;
        return new EqualsBuilder().append(name, b.name).append(append, b.append).append(resources, b.resources).isEquals();
    }

}
