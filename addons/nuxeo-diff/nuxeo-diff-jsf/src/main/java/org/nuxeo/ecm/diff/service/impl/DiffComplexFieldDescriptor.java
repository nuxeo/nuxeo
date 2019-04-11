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
                getDiffFieldItemDefinitions(getIncludedItems()), getDiffFieldItemDefinitions(getExcludedItems()));
    }

    protected List<DiffFieldItemDefinition> getDiffFieldItemDefinitions(List<DiffFieldItemDescriptor> itemDescriptors) {
        List<DiffFieldItemDefinition> diffFieldItemDefinitions = new ArrayList<>();
        for (DiffFieldItemDescriptor itemDescriptor : itemDescriptors) {
            diffFieldItemDefinitions.add(itemDescriptor.getDiffFieldItemDefinition());
        }
        return diffFieldItemDefinitions;
    }

}
