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
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;

/**
 * Default implementation of a {@link DiffBlockDefinition}.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DiffBlockDefinitionImpl implements DiffBlockDefinition {

    private static final long serialVersionUID = 511776842683091931L;

    protected static final String DIFF_DISPLAY_BLOCK_LABEL_PREFIX = "label.diffBlock.";

    protected String name;

    protected String label;

    protected List<DiffFieldDefinition> fields;

    public DiffBlockDefinitionImpl(String name, String label,
            List<DiffFieldDefinition> fields) {
        this.name = name;
        if (StringUtils.isEmpty(label)) {
            this.label = DIFF_DISPLAY_BLOCK_LABEL_PREFIX + name;
        } else {
            this.label = label;
        }
        if (fields == null) {
            this.fields = new ArrayList<DiffFieldDefinition>();
        } else {
            this.fields = fields;
        }
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public List<DiffFieldDefinition> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof DiffBlockDefinition)) {
            return false;
        }

        String otherName = ((DiffBlockDefinition) other).getName();
        if (name == null && otherName == null) {
            return true;
        }
        if (name == null && otherName != null || name != null
                && otherName == null || !name.equals(otherName)) {
            return false;
        }

        List<DiffFieldDefinition> otherFields = ((DiffBlockDefinition) other).getFields();
        if (CollectionUtils.isEmpty(fields)
                && CollectionUtils.isEmpty(otherFields)) {
            return true;
        }
        if (CollectionUtils.isEmpty(fields)
                && !CollectionUtils.isEmpty(otherFields)
                || !CollectionUtils.isEmpty(fields)
                && CollectionUtils.isEmpty(otherFields)
                || !fields.equals(otherFields)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return name + fields;
    }
}
