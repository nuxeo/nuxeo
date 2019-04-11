/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

    @Override
    public String getSchemaName() {
        return schema;
    }

    @Override
    public String getFieldName() {
        return field;
    }

    @Override
    public String getPropertyName() {
        if (schema == null || schema.length() == 0) {
            return field;
        } else {
            return schema + ":" + field;
        }
    }

    @Override
    public FieldDefinition clone() {
        return new FieldDefinitionImpl(schema, field);
    }

    /**
     * @since 7.1
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("FieldDefinitionImpl");
        sb.append(" {");
        sb.append(" schema=");
        sb.append(schema);
        sb.append(", field=");
        sb.append(field);
        sb.append('}');

        return sb.toString();
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FieldDefinitionImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        FieldDefinitionImpl fd = (FieldDefinitionImpl) obj;
        return new EqualsBuilder().append(schema, fd.schema).append(field, fd.field).isEquals();
    }

}
