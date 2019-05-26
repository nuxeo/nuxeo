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
 * $Id: ValueExpressionHelper.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import org.nuxeo.ecm.platform.el.DocumentModelResolver;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Helper for managing value expressions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ValueExpressionHelper {

    private ValueExpressionHelper() {
    }

    /**
     * Returns true if given expression contains some special characters, in which case no transformation of the widget
     * field definition will be done to make it compliant with {@link DocumentModelResolver} lookups when handling
     * document fields. Special characters are:
     * <ul>
     * <li>".": this makes it possible to resolve subelements, for instance "myfield.mysubfield".</li>
     * <li>"[": this makes it possible to include map or array sub elements, for instance
     * "contextData['request/comment']" to fill a document model context map.</li>
     * </ul>
     *
     * @throws NullPointerException if expression is null
     */
    public static boolean isFormattedAsELExpression(String expression) {
        if (expression.contains(".") || expression.contains("[")) {
            return true;
        }
        return false;
    }

    /**
     * Returns the value expression string representation without the surrounding brackets, for instance:
     * "value.property" instead of #{value.property}.
     */
    public static String createBareExpressionString(String valueName, FieldDefinition field) {
        if (field == null || "".equals(field.getPropertyName())) {
            return valueName;
        }

        String fieldName = field.getFieldName();
        if (ComponentTagUtils.isStrictValueReference(fieldName)) {
            // already an EL expression => ignore schema name, do not resolve
            // field, ignore previous expression elements
            return ComponentTagUtils.getBareValueName(fieldName);
        } else if (isFormattedAsELExpression(fieldName)) {
            // already formatted as an EL expression => ignore schema name, do
            // not resolve field and do not modify expression format
            StringBuilder builder = new StringBuilder();
            builder.append(valueName);
            if (!fieldName.startsWith(".") && !fieldName.startsWith("[")) {
                builder.append(".");
            }
            builder.append(fieldName);
            return builder.toString();
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(valueName);

            // try to resolve schema name/prefix
            String schemaName = field.getSchemaName();
            if (schemaName == null) {
                String propertyName = field.getFieldName();
                String[] s = propertyName.split(":");
                if (s.length == 2) {
                    schemaName = s[0];
                    fieldName = s[1];
                }
            }

            if (schemaName != null) {
                builder.append("['").append(schemaName).append("']");
            }

            // handle xpath expressions
            String[] splittedFieldName = fieldName.split("/");
            for (String item : splittedFieldName) {
                builder.append("[");
                try {
                    builder.append(Integer.parseInt(item));
                } catch (NumberFormatException e) {
                    builder.append("'").append(item).append("'");
                }
                builder.append("]");
            }

            return builder.toString();
        }
    }

    public static String createExpressionString(String valueName, FieldDefinition field) {
        String bareExpression = createBareExpressionString(valueName, field);
        return "#{" + bareExpression + "}";
    }

}
