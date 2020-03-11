/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FieldDescriptor.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;

/**
 * Field definition descriptor.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("field")
public class FieldDescriptor {

    @XNode("@schema")
    String schema;

    @XNode("")
    String field;

    public FieldDescriptor() {
    }

    public FieldDescriptor(String schema, String field) {
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

    public FieldDefinition getFieldDefinition() {
        return new FieldDefinitionImpl(schema, field);
    }

}
