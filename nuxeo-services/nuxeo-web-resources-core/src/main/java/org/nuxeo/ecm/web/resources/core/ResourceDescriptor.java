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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.web.resources.api.Resource;

/**
 * @since 7.3
 */
@XObject("resource")
public class ResourceDescriptor implements Resource {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    public String name;

    @XNode("@type")
    public String type;

    @XNode("path")
    public String path;

    @XNodeList(value = "require", type = ArrayList.class, componentType = String.class)
    public List<String> dependencies;

    @XNodeList(value = "processors/processor", type = ArrayList.class, componentType = String.class)
    public List<String> processors;

    @XNode("shrinkable")
    public boolean shrinkable = true;

    @XNode("uri")
    protected String uri;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        if (StringUtils.isBlank(type)) {
            // try to infer it from name for easier declaration
            return FileUtils.getFileExtension(name);
        }
        return type;
    }

    @Override
    public List<String> getDependencies() {
        return dependencies;
    }

    @Override
    public List<String> getProcessors() {
        return processors;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public void setURI(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean isShrinkable() {
        return shrinkable;
    }

}