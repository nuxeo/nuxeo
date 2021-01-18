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
 *     akalogeropoulos
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Descriptor for file import.
 */
@XObject("plugin")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class FileImporterDescriptor {

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    boolean enabled = true;

    @XNode("@name")
    @XRegistryId
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
    protected List<String> filters;

    @XNode("@filter")
    protected String filter;

    @XNode("@order")
    private Integer order;

    public String getName() {
        return name;
    }

    /**
     * @deprecated since 11.1. Use {@link #klass}.
     */
    @Deprecated(since = "11.1")
    public String getClassName() {
        return className;
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

    public List<String> getFilters() {
        return filters;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getOrder() {
        return order;
    }

    /**
     * @since 11.1
     */
    public FileImporter newInstance() {
        try {
            FileImporter fileImporter = klass.getDeclaredConstructor().newInstance();
            fileImporter.setName(name);
            fileImporter.setEnabled(enabled);
            fileImporter.setDocType(docType);
            fileImporter.setFilters(filters);
            fileImporter.setOrder(order);
            return fileImporter;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    public String getId() {
        return name;
    }

    protected <T> T defaultValue(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

}
