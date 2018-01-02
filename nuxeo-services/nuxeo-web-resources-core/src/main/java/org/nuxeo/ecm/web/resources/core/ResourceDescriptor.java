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

    /**
     * Target for this resource.
     * <p>
     * Currently only useful for JSF resources reallocation in the page.
     *
     * @since 7.10
     */
    @XNode("@target")
    public String target;

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
    public boolean isShrinkable() {
        return shrinkable;
    }

    public void setURI(String uri) {
        this.uri = uri;
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
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @since 7.4
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @since 7.4
     */
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * @since 7.4
     */
    public void setProcessors(List<String> processors) {
        this.processors = processors;
    }

    /**
     * @since 7.4
     */
    public void setShrinkable(boolean shrinkable) {
        this.shrinkable = shrinkable;
    }

    /**
     * @since 7.4
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @since 7.10
     */
    public String getTarget() {
        return target;
    }

    /**
     * @since 7.10
     */
    public void setTarget(String target) {
        this.target = target;
    }

}
