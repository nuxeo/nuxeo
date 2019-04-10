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
        return new DiffFieldDefinitionImpl(getCategory(), getSchema(),
                getName());
    }
}
