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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;

/**
 * Helper for managing value expressions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ValueExpressionHelper {

    private ValueExpressionHelper() {
    }

    public static boolean isFormattedAsELExpression(String expression) {
        if (expression.contains(".") || expression.contains("[")) {
            return true;
        }
        return false;
    }

    /**
     * Returns the value expression string representation without the
     * surrounding brackets, for instance: "value.property" instead of
     * #{value.property}.
     */
    public static String createBareExpressionString(String valueName,
            FieldDefinition field) {
        if (field == null || "".equals(field.getPropertyName())) {
            return valueName;
        }
        List<String> expressionElements = new ArrayList<String>();
        expressionElements.add(valueName);

        String dmResolverValue;

        String fieldName = field.getFieldName();
        if (isFormattedAsELExpression(fieldName)) {
            // already formatted as an EL expression => ignore schema name, do
            // not resolve field and do not modify expression format
            expressionElements.add(fieldName);
            dmResolverValue = StringUtils.join(expressionElements, ".");
        } else {
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
                expressionElements.add(String.format("['%s']", schemaName));
            }

            // handle xpath expressions
            String[] splittedFieldName = fieldName.split("/");
            for (String item : splittedFieldName) {
                try {
                    expressionElements.add(String.format("[%s]",
                            Integer.valueOf(Integer.parseInt(item))));
                } catch (NumberFormatException e) {
                    expressionElements.add(String.format("['%s']", item));
                }
            }
            dmResolverValue = StringUtils.join(expressionElements, "");
        }
        return dmResolverValue;
    }

    public static String createExpressionString(String valueName,
            FieldDefinition field) {
        String bareExpression = createBareExpressionString(valueName, field);
        return String.format("#{%s}", bareExpression);
    }

}
