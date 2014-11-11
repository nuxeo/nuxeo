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

    // FIXME follow xpath syntax
    public static String createExpressionString(String valueName,
            FieldDefinition field) {
        if (field == null || "".equals(field.getPropertyName())) {
            return String.format("#{%s}", valueName);
        }
        List<String> expressionElements = new ArrayList<String>();
        expressionElements.add(valueName);

        String schemaName = field.getSchemaName();
        String fieldName = field.getFieldName();
        if (schemaName == null) {
            // try to resolve schema name
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

        String dmResolverValue;
        if (fieldName.contains(".")) {
            expressionElements.add(fieldName);
            // already formatted as an EL expression => do not use brackets
            dmResolverValue = String.format("#{%s}", StringUtils.join(
                    expressionElements, "."));
        } else {
            String[] splittedFieldName = fieldName.split("/");
            for (String item : splittedFieldName) {
                try {
                    expressionElements.add(String.format("[%s]",
                            Integer.valueOf(Integer.parseInt(item))));
                } catch (NumberFormatException e) {
                    expressionElements.add(String.format("['%s']", item));
                }
            }
            dmResolverValue = String.format("#{%s}", StringUtils.join(
                    expressionElements, ""));
        }
        return dmResolverValue;
    }

}
