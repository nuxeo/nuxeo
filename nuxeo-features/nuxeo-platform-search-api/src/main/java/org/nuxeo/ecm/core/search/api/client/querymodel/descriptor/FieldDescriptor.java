/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.querymodel.descriptor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

@XObject(value = "field")
public class FieldDescriptor {

    final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");

    @XNode("@name")
    protected String name;

    @XNode("@schema")
    protected String schema;

    private String fieldType;

    public FieldDescriptor() {
    }

    public FieldDescriptor(String schema, String name) {
        this.name = name;
        this.schema = schema;
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    public String getPlainStringValue(DocumentModel model) {
        Object rawValue = model.getProperty(schema, name);
        if (rawValue == null) {
            return null;
        }
        String value = (String) rawValue;
        if (value.equals("")) {
            return null;
        }
        return value;
    }

    public Integer getIntValue(DocumentModel model) {
        Object rawValue = model.getProperty(schema, name);
        if (rawValue == null || "".equals(rawValue)) {
            return null;
        } else if (rawValue instanceof Integer) {
            return (Integer) rawValue;
        } else if (rawValue instanceof String) {
            return Integer.parseInt((String) rawValue);
        } else {
            return Integer.parseInt(rawValue.toString());
        }
    }

    public String getFieldType() throws ClientException {
        try {
            SchemaManager typeManager = Framework.getService(SchemaManager.class);
            Schema schemaObj = typeManager.getSchema(schema);
            if (schemaObj == null) {
                throw new ClientException("failed to obtain schema: " + schema);
            }
            Field field = schemaObj.getField(name);
            if (field == null) {
                throw new ClientException("failed to obtain field: " + schema + ":" + name);
            }
            return field.getType().getName();
        } catch (Exception e) {
            throw new ClientException("failed to get field type for " + schema + ":" + name, e);
        }
    }

    public String getStringValue(DocumentModel model) throws ClientException {
        Object rawValue = model.getProperty(schema, name);
        if (rawValue == null) {
            return null;
        }
        String value;
        if (rawValue instanceof GregorianCalendar) {
            GregorianCalendar gc = (GregorianCalendar) rawValue;
            value = "DATE '" + sf.format(gc.getTime()) + "'";
        } else if (rawValue instanceof Date) {
            Date date = (Date) rawValue;
            value = "DATE '" + sf.format(date) + "'";
        } else if (rawValue instanceof Integer || rawValue instanceof Long) {
            value = rawValue.toString(); // no quotes
        } else if (rawValue instanceof Boolean) {
            value = (Boolean) rawValue ? "1" : "0";
        } else {
            value = rawValue.toString().trim();
            if (value.equals("")) {
                return null;
            }
            if (fieldType == null) {
                fieldType = getFieldType();
            }
            if ("long".equals(fieldType) || "integer".equals(fieldType)) {
                return value;
            } else {
                // TODO switch back to SQLQueryParser for org.nuxeo.core 1.4
                return QueryModelDescriptor.prepareStringLiteral(value);
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public List<String> getListValue(DocumentModel model) {
        Object rawValue = model.getProperty(schema, name);
        if (rawValue == null) {
            return null;
        }
        List<String> values = new ArrayList<String>();
        if (rawValue instanceof ArrayList) {
            rawValue = ((ArrayList<Object>) rawValue).toArray();
        }
        for (Object element : (Object[]) rawValue) {
            // XXX: SQL escape values against SQL injection here! or refactor to
            // use a PreparedStatement-like API at the Core level
            if (element != null) {
                String value = element.toString().trim();
                if (!value.equals("")) {
                    values.add("'" + value + "'");
                }
            }
        }
        return values;
    }

    public Boolean getBooleanValue(DocumentModel model) {
        Object rawValue = model.getProperty(schema, name);
        if (rawValue == null) {
            return null;
        } else {
            return (Boolean) rawValue;
        }
    }

}
