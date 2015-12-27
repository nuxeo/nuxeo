/*
 * (C) Copyright 2006 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: PluginExtension.java 3036 2006-09-18 17:32:20Z janguenot $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author akalogeropoulos
 */
@XObject("plugin")
public class FileImporterDescriptor implements Serializable {

    public static final List<String> DEFAULT_FILTER = new ArrayList<String>();

    private static final long serialVersionUID = 1L;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected String className;

    @XNode("@docType")
    protected String docType;

    @XNodeList(value = "filter", type = ArrayList.class, componentType = String.class)
    protected List<String> filters = DEFAULT_FILTER;

    @XNode("@filter")
    protected String filter;

    @XNode("@order")
    private Integer order;

    @XNode("@merge")
    private boolean merge = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns the configured document type to be created when using the importer
     *
     * @since 5.5
     */
    public String getDocType() {
        return docType;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getOrder() {
        return order;
    }

    /**
     * Returns {@code true} if this {@code FileImporterDescriptor} should be merged with an existing one, {@code false}
     * otherwise.
     *
     * @since 5.5
     */
    public boolean isMerge() {
        return merge;
    }
}
