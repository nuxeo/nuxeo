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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.diff.model.DiffComplexFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;
import org.nuxeo.ecm.diff.model.impl.DiffComplexFieldDefinitionImpl;

/**
 * Diff complex field descriptor.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
@XObject("diffComplexField")
public class DiffComplexFieldDescriptor {

    @XNode("@schema")
    public String schema;

    @XNode("@name")
    public String name;

    @XNodeList(value = "includedItems/item", type = ArrayList.class, componentType = DiffFieldItemDescriptor.class)
    public List<DiffFieldItemDescriptor> includedItems;

    @XNodeList(value = "excludedItems/item", type = ArrayList.class, componentType = DiffFieldItemDescriptor.class)
    public List<DiffFieldItemDescriptor> excludedItems;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DiffFieldItemDescriptor> getIncludedItems() {
        return includedItems;
    }

    public void setIncludedItems(List<DiffFieldItemDescriptor> includedItems) {
        this.includedItems = includedItems;
    }

    public List<DiffFieldItemDescriptor> getExcludedItems() {
        return excludedItems;
    }

    public void setExcludedItems(List<DiffFieldItemDescriptor> excludedItems) {
        this.excludedItems = excludedItems;
    }

    public DiffComplexFieldDefinition getDiffComplexFieldDefinition() {
        return new DiffComplexFieldDefinitionImpl(getSchema(), getName(),
                getDiffFieldItemDefinitions(getIncludedItems()),
                getDiffFieldItemDefinitions(getExcludedItems()));
    }

    protected List<DiffFieldItemDefinition> getDiffFieldItemDefinitions(
            List<DiffFieldItemDescriptor> itemDescriptors) {
        List<DiffFieldItemDefinition> diffFieldItemDefinitions = new ArrayList<DiffFieldItemDefinition>();
        for (DiffFieldItemDescriptor itemDescriptor : itemDescriptors) {
            diffFieldItemDefinitions.add(itemDescriptor.getDiffFieldItemDefinition());
        }
        return diffFieldItemDefinitions;
    }

}
