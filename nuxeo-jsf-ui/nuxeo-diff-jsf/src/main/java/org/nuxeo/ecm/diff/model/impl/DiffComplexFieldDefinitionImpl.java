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
import org.nuxeo.ecm.diff.model.DiffComplexFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;

/**
 * Default implementation of a {@link DiffComplexFieldDefinition}
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public class DiffComplexFieldDefinitionImpl implements DiffComplexFieldDefinition {

    private static final long serialVersionUID = 5289865501066754428L;

    protected String schema;

    protected String name;

    protected List<DiffFieldItemDefinition> includedItems;

    protected List<DiffFieldItemDefinition> excludedItems;

    public DiffComplexFieldDefinitionImpl(String schema, String name) {
        this(schema, name, new ArrayList<DiffFieldItemDefinition>(), new ArrayList<DiffFieldItemDefinition>());
    }

    public DiffComplexFieldDefinitionImpl(String schema, String name, List<DiffFieldItemDefinition> includedItems,
            List<DiffFieldItemDefinition> excludedItems) {
        this.schema = schema;
        this.name = name;
        this.includedItems = includedItems;
        this.excludedItems = excludedItems;
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
    public List<DiffFieldItemDefinition> getIncludedItems() {
        return includedItems;
    }

    @Override
    public List<DiffFieldItemDefinition> getExcludedItems() {
        return excludedItems;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof DiffComplexFieldDefinition)) {
            return false;
        }

        String otherSchema = ((DiffComplexFieldDefinition) other).getSchema();
        String otherName = ((DiffComplexFieldDefinition) other).getName();
        if (schema == null && otherSchema == null && name == null && otherName == null) {
            return true;
        }
        if (schema == null || otherSchema == null || name == null || otherName == null
                || (schema != null && !schema.equals(otherSchema)) || (name != null && !name.equals(otherName))) {
            return false;
        }

        List<DiffFieldItemDefinition> otherIncludedItems = ((DiffComplexFieldDefinition) other).getIncludedItems();
        List<DiffFieldItemDefinition> otherExcludedItems = ((DiffComplexFieldDefinition) other).getExcludedItems();
        if (CollectionUtils.isEmpty(includedItems) && CollectionUtils.isEmpty(otherIncludedItems)
                && CollectionUtils.isEmpty(excludedItems) && CollectionUtils.isEmpty(otherExcludedItems)) {
            return true;
        }
        if (CollectionUtils.isEmpty(includedItems) && !CollectionUtils.isEmpty(otherIncludedItems)
                || !CollectionUtils.isEmpty(includedItems) && CollectionUtils.isEmpty(otherIncludedItems)
                || !includedItems.equals(otherIncludedItems)) {
            return false;
        }
        if (CollectionUtils.isEmpty(excludedItems) && !CollectionUtils.isEmpty(otherExcludedItems)
                || !CollectionUtils.isEmpty(excludedItems) && CollectionUtils.isEmpty(otherExcludedItems)
                || !excludedItems.equals(otherExcludedItems)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(schema);
        sb.append(":");
        sb.append(name);
        sb.append(!CollectionUtils.isEmpty(includedItems) ? " / " + includedItems : "");
        sb.append(!CollectionUtils.isEmpty(excludedItems) ? " / " + excludedItems : "");
        return sb.toString();
    }
}
