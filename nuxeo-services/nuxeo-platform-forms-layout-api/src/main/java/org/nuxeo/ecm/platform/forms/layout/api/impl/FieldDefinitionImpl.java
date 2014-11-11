/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class FieldDefinitionImpl implements FieldDefinition {

    private static final long serialVersionUID = 1L;

    protected String schema;

    protected String field;

    // needed by GWT serialization
    protected FieldDefinitionImpl() {
    }

    public FieldDefinitionImpl(String schema, String field) {
        this.schema = schema;
        this.field = field;
    }

    public String getSchemaName() {
        return schema;
    }

    public String getFieldName() {
        return field;
    }

    public String getPropertyName() {
        if (schema == null || schema.length() == 0) {
            return field;
        } else {
            return schema + ":" + field;
        }
    }

}
