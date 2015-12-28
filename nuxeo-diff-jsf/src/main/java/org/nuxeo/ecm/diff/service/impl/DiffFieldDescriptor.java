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
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;

/**
 * Diff field descriptor.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
@XObject("field")
public class DiffFieldDescriptor {

    @XNode("@category")
    public String category;

    @XNode("@schema")
    public String schema;

    @XNode("@name")
    public String name;

    @XNode("@displayContentDiffLinks")
    public boolean displayContentDiffLinks;

    @XNodeList(value = "items/item", type = ArrayList.class, componentType = DiffFieldItemDescriptor.class)
    public List<DiffFieldItemDescriptor> items;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

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

    public boolean isDisplayContentDiffLinks() {
        return displayContentDiffLinks;
    }

    public void setDisplayContentDiffLinks(boolean displayContentDiffLinks) {
        this.displayContentDiffLinks = displayContentDiffLinks;
    }

    public List<DiffFieldItemDescriptor> getItems() {
        return items;
    }

    public void setItems(List<DiffFieldItemDescriptor> items) {
        this.items = items;
    }

    public DiffFieldDefinition getDiffFieldDefinition() {
        return new DiffFieldDefinitionImpl(getCategory(), getSchema(), getName());
    }
}
