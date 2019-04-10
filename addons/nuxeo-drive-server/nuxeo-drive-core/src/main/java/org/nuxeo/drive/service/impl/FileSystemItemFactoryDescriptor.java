/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * XMap descriptor for factories contributed to the {@code fileSystemItemFactory} extension point of the
 * {@link FileSystemItemAdapterService}.
 * 
 * @author Antoine Taillefer
 */
@XObject("fileSystemItemFactory")
public class FileSystemItemFactoryDescriptor implements Serializable, Comparable<FileSystemItemFactoryDescriptor> {

    private static final long serialVersionUID = -7840980495329452651L;

    @XNode("@name")
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
    protected Map<String, String> parameters = new HashMap<String, String>();

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public Class<? extends FileSystemItemFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends FileSystemItemFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public FileSystemItemFactory getFactory() throws InstantiationException, IllegalAccessException, ClientException {
        FileSystemItemFactory factory = factoryClass.newInstance();
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
