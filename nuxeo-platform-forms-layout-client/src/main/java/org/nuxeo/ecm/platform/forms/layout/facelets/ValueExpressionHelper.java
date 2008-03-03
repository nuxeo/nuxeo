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
 * $Id: ValueExpressionHelper.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for managing value expressions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class ValueExpressionHelper {

    private ValueExpressionHelper() {
    }

    public static String createExpressionString(String valueName,
            FieldDefinition field) {
        String schemaName = field.getSchemaName();
        String fieldName = field.getFieldName();
        if (schemaName == null) {
            // resolve schema name for prefix
            String propertyName = field.getFieldName();
            String[] s = propertyName.split(":");
            if (s.length == 2) {
                String prefix = s[0];
                Schema schema = null;
                try {
                    SchemaManager tm = Framework.getService(SchemaManager.class);
                    schema = tm.getSchemaFromPrefix(prefix);
                } catch (Exception e) {
                }
                if (schema == null) {
                    // fall back on prefix as it may be the schema name
                    schemaName = prefix;
                } else {
                    schemaName = schema.getName();
                }
                fieldName = s[1];
            }
        }
        String[] splittedFieldName = fieldName.split("/");
        StringBuffer newFieldName = new StringBuffer();
        boolean first = true;
        for (String item : splittedFieldName) {
            try {
                newFieldName.append(String.format("[%s]",
                        Integer.parseInt(item)));
            } catch (NumberFormatException e) {
                if (!first) {
                    newFieldName.append(String.format(".%s", item));
                } else {
                    newFieldName.append(item);
                }
            }
            first = false;
        }
        String dmResolverValue;
        if (schemaName == null) {
            dmResolverValue = String.format("#{%s.%s}", valueName, newFieldName);
        } else {
            String fieldValue = String.format("%s.%s", schemaName, newFieldName);
            dmResolverValue = String.format("#{%s.%s}", valueName, fieldValue);
        }
        return dmResolverValue;
    }

}
