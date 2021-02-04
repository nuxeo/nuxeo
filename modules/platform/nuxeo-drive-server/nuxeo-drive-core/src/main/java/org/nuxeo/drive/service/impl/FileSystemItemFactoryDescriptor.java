/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * XMap descriptor for factories contributed to the {@code fileSystemItemFactory} extension point of the
 * {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@XObject("fileSystemItemFactory")
@XRegistry
public class FileSystemItemFactoryDescriptor implements Comparable<FileSystemItemFactoryDescriptor> {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@order")
    protected int order = 0;

    @XNode("@docType")
    protected String docType;

    @XNode("@facet")
    protected String facet;

    @XNode("@class")
    protected Class<? extends FileSystemItemFactory> factoryClass;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters;

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public String getDocType() {
        return docType;
    }

    public String getFacet() {
        return facet;
    }

    public Class<? extends FileSystemItemFactory> getFactoryClass() {
        return factoryClass;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public FileSystemItemFactory getFactory() {
        FileSystemItemFactory factory;
        try {
            factory = factoryClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
        factory.setName(name);
        factory.handleParameters(parameters);
        return factory;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("(");
        sb.append(getOrder());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int compareTo(FileSystemItemFactoryDescriptor other) {
        if (other == null) {
            return 1;
        }
        int orderDiff = getOrder() - other.getOrder();
        if (orderDiff == 0) {
            // Make sure we have a deterministic sort, use name
            orderDiff = getName().compareTo(other.getName());
        }
        return orderDiff;
    }

}
