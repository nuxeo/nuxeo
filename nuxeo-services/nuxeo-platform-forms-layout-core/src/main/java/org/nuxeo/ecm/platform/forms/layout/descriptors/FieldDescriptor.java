/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
