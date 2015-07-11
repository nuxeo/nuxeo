/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
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
            c.resources = new ArrayList<String>(resources);
        }
        return c;
    }

    @Override
    public ResourceBundle merge(ResourceBundle other) {
        if (other instanceof ResourceBundleDescriptor) {
            boolean append = ((ResourceBundleDescriptor) other).isAppend();
            List<String> res = other.getResources();
            List<String> merged = new ArrayList<String>();
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
