/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.diff.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
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
 * Helper to get complex property names and values, and list property values.
 * <p>
 * TODO: refactor as a service + use xpath for fetching doc property values.
 */
public final class ComplexPropertyHelper {

    private ComplexPropertyHelper() {
        // helper class
    }

    public static Field getField(String schemaName, String fieldName) {

        Schema schema = getSchemaManager().getSchema(schemaName);
        if (schema == null) {
            throw new NuxeoException(String.format("Schema [%s] does not exist.", schemaName));
        }

        Field field = schema.getField(fieldName);
        if (field == null) {
            throw new NuxeoException(String.format("Field [%s] does not exist in schema [%s].", fieldName, schemaName));
        }
        return field;
    }

    public static Field getComplexFieldItem(Field field, String complexItemName) {

        Type fieldType = field.getType();
        if (!fieldType.isComplexType()) {
            throw new NuxeoException(String.format("Field '%s' is not a complex type.", field));
        }

        return ((ComplexType) fieldType).getField(complexItemName);

    }

    public static List<Field> getComplexFieldItems(Field field) {

        Type fieldType = field.getType();
        if (!fieldType.isComplexType()) {
            throw new NuxeoException(
                    String.format("Field [%s] is not a complex type.", field.getName().getLocalName()));
        }

        return new ArrayList<>(((ComplexType) fieldType).getFields());
    }

    public static Field getListFieldItem(Field field) {

        Type fieldType = field.getType();
        if (!fieldType.isListType()) {
            throw new NuxeoException(String.format("Field [%s] is not a list type.", field.getName().getLocalName()));
        }

        Field listFieldItem = ((ListType) fieldType).getField();
        if (listFieldItem == null) {
            throw new NuxeoException(String.format(
                    "Field [%s] is a list type but has no field defining the elements stored by this list.",
                    field.getName().getLocalName()));
        }

        return listFieldItem;
    }

    public static String getFieldType(Field field) {

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
        // Scalar type (string, boolean, date, integer, long, double)
        else {
            fieldTypeName = fieldType.getName();
        }

        return fieldTypeName;
    }

    private static final SchemaManager getSchemaManager() {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (schemaManager == null) {
            throw new NuxeoException("SchemaManager service is null.");
        }
        return schemaManager;
    }
}
