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
package org.nuxeo.ecm.diff.web;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.runtime.api.Framework;

/**
 * TODO: refactor as a service + use xpath for fetching doc property values.
 * Helper to get complex property names and values, and list property values.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public final class ComplexPropertyHelper {

    public static Field getField(String schemaName, String fieldName)
            throws ClientException {

        Schema schema = getSchemaManager().getSchema(schemaName);
        if (schema == null) {
            throw new ClientException(String.format(
                    "Schema [%s] does not exist.", schemaName));
        }

        Field field = schema.getField(fieldName);
        if (field == null) {
            throw new ClientException(String.format(
                    "Field [%s] does not exist in schema [%s].", fieldName,
                    schemaName));
        }
        return field;
    }

    public static Field getComplexFieldItem(Field field, String complexItemName)
            throws ClientException {

        Type fieldType = field.getType();
        if (!fieldType.isComplexType()) {
            throw new ClientException(String.format(
                    "Field '%s' is not a complex type.", field));
        }

        return ((ComplexType) fieldType).getField(complexItemName);

    }

    public static List<Field> getComplexFieldItems(Field field)
            throws ClientException {

        Type fieldType = field.getType();
        if (!fieldType.isComplexType()) {
            throw new ClientException(String.format(
                    "Field [%s] is not a complex type.",
                    field.getName().getLocalName()));
        }

        return new ArrayList<Field>(((ComplexType) fieldType).getFields());
    }

    public static Field getListFieldItem(Field field) throws ClientException {

        Type fieldType = field.getType();
        if (!fieldType.isListType()) {
            throw new ClientException(String.format(
                    "Field [%s] is not a list type.",
                    field.getName().getLocalName()));
        }

        Field listFieldItem = ((ListType) fieldType).getField();
        if (listFieldItem == null) {
            throw new ClientException(
                    String.format(
                            "Field [%s] is a list type but has no field defining the elements stored by this list.",
                            field.getName().getLocalName()));
        }

        return listFieldItem;
    }

    public static String getFieldType(Field field) throws ClientException {

        String fieldTypeName;
        Type fieldType = field.getType();

        // Complex type
        if (fieldType.isComplexType()) {
            // Content
            if (TypeConstants.isContentType(fieldType)) {
                fieldTypeName = PropertyType.CONTENT;
            }
            // Complex
            else {
                fieldTypeName = PropertyType.COMPLEX;
            }
        }
        // List type
        else if (fieldType.isListType()) {
            Field listField = ((ListType) fieldType).getField();
            // Complex list
            if (listField.getType().isComplexType()) {
                fieldTypeName = PropertyType.COMPLEX_LIST;
            }
            // Scalar list
            else {
                fieldTypeName = PropertyType.SCALAR_LIST;
            }
        }
        // Scalar type (string, boolean, date, integer, long, double) or content
        // type.
        else {
            fieldTypeName = fieldType.getName();
        }

        return fieldTypeName;
    }

    private static final SchemaManager getSchemaManager()
            throws ClientException {

        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
        if (schemaManager == null) {
            throw new ClientException("SchemaManager service is null.");
        }
        return schemaManager;
    }
}
