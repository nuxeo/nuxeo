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
package org.nuxeo.ecm.diff.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;

/**
 * Default implementation of a {@link DiffFieldDefinition}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DiffFieldDefinitionImpl implements DiffFieldDefinition {

    private static final long serialVersionUID = 6192730067253949180L;

    protected String category;

    protected String schema;

    protected String name;

    protected boolean displayContentDiffLinks;

    protected List<DiffFieldItemDefinition> items;

    public DiffFieldDefinitionImpl(String category, String schema, String name) {
        this(category, schema, name, false);
    }

    public DiffFieldDefinitionImpl(String category, String schema, String name, boolean displayContentDiffLinks) {
        this(category, schema, name, displayContentDiffLinks, new ArrayList<DiffFieldItemDefinition>());
    }

    public DiffFieldDefinitionImpl(String category, String schema, String name, List<DiffFieldItemDefinition> items) {
        this(category, schema, name, false, items);
    }

    public DiffFieldDefinitionImpl(String category, String schema, String name, boolean displayContentDiffLinks,
            List<DiffFieldItemDefinition> items) {
        this.category = category;
        this.schema = schema;
        this.name = name;
        this.displayContentDiffLinks = displayContentDiffLinks;
        this.items = items;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDisplayContentDiffLinks() {
        return displayContentDiffLinks;
    }

    @Override
    public List<DiffFieldItemDefinition> getItems() {
        return items;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof DiffFieldDefinition)) {
            return false;
        }

        String otherCategory = ((DiffFieldDefinition) other).getCategory();
        String otherSchema = ((DiffFieldDefinition) other).getSchema();
        String otherName = ((DiffFieldDefinition) other).getName();
        boolean otherDisplayContentDiffLinks = ((DiffFieldDefinition) other).isDisplayContentDiffLinks();
        if (category == null && otherCategory == null && schema == null && otherSchema == null && name == null
                && otherName == null) {
            return true;
        }
        if (schema == null || otherSchema == null || name == null || otherName == null
                || (category == null && otherCategory != null) || (category != null && otherCategory == null)
                || (category != null && !category.equals(otherCategory))
                || (schema != null && !schema.equals(otherSchema)) || (name != null && !name.equals(otherName))
                || displayContentDiffLinks != otherDisplayContentDiffLinks) {
            return false;
        }

        List<DiffFieldItemDefinition> otherItems = ((DiffFieldDefinition) other).getItems();
        if (CollectionUtils.isEmpty(items) && CollectionUtils.isEmpty(otherItems)) {
            return true;
        }
        if (CollectionUtils.isEmpty(items) && !CollectionUtils.isEmpty(otherItems) || !CollectionUtils.isEmpty(items)
                && CollectionUtils.isEmpty(otherItems) || !items.equals(otherItems)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("category=");
        sb.append(category);
        sb.append(" / ");
        sb.append(schema);
        sb.append(":");
        sb.append(name);
        sb.append(" / ");
        sb.append("displayContentDiffLinks=");
        sb.append(displayContentDiffLinks);
        sb.append(!CollectionUtils.isEmpty(items) ? " / " + items : "");
        return sb.toString();
    }
}
