/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @author akalogeropoulos
 */
@XObject("plugin")
public class FileImporterDescriptor implements Descriptor, Serializable {

    public static final List<String> DEFAULT_FILTER = new ArrayList<>();

    private static final long serialVersionUID = 1L;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@name")
    protected String name;

    /**
     * @deprecated since 11.1.
     */
    @Deprecated(since = "11.1")
    @XNode("@class")
    protected String className;

    /**
     * @since 11.1
     */
    @XNode("@class")
    protected Class<? extends FileImporter> klass;

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

    /**
     * @deprecated since 11.1. Use {@link #klass}.
     */
    @Deprecated(since = "11.1")
    public String getClassName() {
        return className;
    }

    /**
     * @deprecated since 11.1. Use {@link #klass}.
     */
    @Deprecated(since = "11.1")
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

    @Override
    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
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

    /**
     * @since 11.1
     */
    public FileImporter newInstance() {
        try {
            FileImporter fileImporter = klass.getDeclaredConstructor().newInstance();
            fileImporter.setName(getName());
            fileImporter.setEnabled(isEnabled());
            fileImporter.setDocType(getDocType());
            fileImporter.setFilters(getFilters());
            fileImporter.setOrder(getOrder());
            return fileImporter;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        FileImporterDescriptor other = (FileImporterDescriptor) o;
        if (!other.merge) {
            return other;
        }

        FileImporterDescriptor merged = new FileImporterDescriptor();
        merged.name = other.name;
        merged.enabled = defaultIfNull(other.enabled, enabled);
        merged.klass = defaultIfNull(other.klass, klass);
        merged.className = defaultIfBlank(other.className, className);
        merged.docType = defaultIfBlank(other.docType, docType);
        merged.filters = new ArrayList<>();
        merged.filters.addAll(filters);
        merged.filters.addAll(other.filters);
        merged.order = defaultIfNull(other.order, order);
        return merged;
    }
}
