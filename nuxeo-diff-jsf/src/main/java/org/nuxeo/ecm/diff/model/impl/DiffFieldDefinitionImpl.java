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
 *     ataillefer
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

    protected String schema;

    protected String name;

    protected boolean displayContentDiffLinks;

    protected List<DiffFieldItemDefinition> items;

    public DiffFieldDefinitionImpl(String schema, String name) {
        this(schema, name, false);
    }

    public DiffFieldDefinitionImpl(String schema, String name,
            boolean displayContentDiffLinks) {
        this(schema, name, displayContentDiffLinks,
                new ArrayList<DiffFieldItemDefinition>());
    }

    public DiffFieldDefinitionImpl(String schema, String name,
            List<DiffFieldItemDefinition> items) {
        this(schema, name, false, items);
    }

    public DiffFieldDefinitionImpl(String schema, String name,
            boolean displayContentDiffLinks, List<DiffFieldItemDefinition> items) {
        this.schema = schema;
        this.name = name;
        this.displayContentDiffLinks = displayContentDiffLinks;
        this.items = items;
    }

    public String getSchema() {
        return schema;
    }

    public String getName() {
        return name;
    }

    public boolean isDisplayContentDiffLinks() {
        return displayContentDiffLinks;
    }

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

        String otherSchema = ((DiffFieldDefinition) other).getSchema();
        String otherName = ((DiffFieldDefinition) other).getName();
        boolean otherDisplayContentDiffLinks = ((DiffFieldDefinition) other).isDisplayContentDiffLinks();
        if (schema == null && otherSchema == null && name == null
                && otherName == null) {
            return true;
        }
        if (schema == null || otherSchema == null || name == null
                || otherName == null || !schema.equals(otherSchema)
                || !name.equals(otherName)
                || displayContentDiffLinks != otherDisplayContentDiffLinks) {
            return false;
        }

        List<DiffFieldItemDefinition> otherItems = ((DiffFieldDefinition) other).getItems();
        if (CollectionUtils.isEmpty(items)
                && CollectionUtils.isEmpty(otherItems)) {
            return true;
        }
        if (CollectionUtils.isEmpty(items)
                && !CollectionUtils.isEmpty(otherItems)
                || !CollectionUtils.isEmpty(items)
                && CollectionUtils.isEmpty(otherItems)
                || !items.equals(otherItems)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return schema + ":" + name + " (displayContentDiffLinks: "
                + displayContentDiffLinks + ") "
                + (!CollectionUtils.isEmpty(items) ? items : "");
    }
}
