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
package org.nuxeo.ecm.platform.diff.helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.runtime.api.Framework;

/**
 * TODO: refactor as a service + use xpath for fetching doc property values.
 * Helper to get complex property names and values, and list property values.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public final class ComplexPropertyHelper {

    /**
     * Gets the complex item names.
     * 
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the complex item names
     * @throws Exception the exception
     */
    public static List<String> getComplexItemNames(String schemaName,
            String fieldName) throws Exception {

        List<String> complexItemNames = new ArrayList<String>();

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(schemaName);
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

        Type fieldType = field.getType();
        if (!fieldType.isComplexType()) {
            throw new ClientException(String.format(
                    "Field [%s:%s] is not a complex type.", schemaName,
                    fieldName));
        }

        ComplexType fieldComplexType = (ComplexType) fieldType;
        Collection<Field> complexItems = fieldComplexType.getFields();

        Iterator<Field> complexItemsIt = complexItems.iterator();
        while (complexItemsIt.hasNext()) {
            Field complexItem = complexItemsIt.next();
            complexItemNames.add(complexItem.getName().getLocalName());
        }

        return complexItemNames;
    }

    /**
     * Gets the complex item values.
     * 
     * @param doc the doc
     * @param schemaName the schema
     * @param fieldName the field
     * @param complexItemName the complex item name
     * @return the complex item values
     * @throws ClientException the client exception
     */
    public static Serializable getComplexItemValue(DocumentModel doc,
            String schemaName, String fieldName, String complexItemName)
            throws ClientException {

        Map<String, Serializable> complexProp = getComplexProperty(doc,
                schemaName, fieldName);

        if (complexProp == null) {
            throw new ClientException(String.format(
                    "Property [%s:%s] is null on doc '%s'.", schemaName,
                    fieldName, doc.getTitle()));
        }

        return complexProp.get(complexItemName);
    }

    /**
     * Gets the list item indexes.
     * 
     * @param listPropertyDiff the listproperty diff
     * @return the list item indexes
     * @throws ClientException the client exception
     */
    public static List<Integer> getListItemIndexes(
            ListPropertyDiff listPropertyDiff) throws ClientException {

        return new ArrayList<Integer>(listPropertyDiff.getDiffMap().keySet());
    }

    /**
     * Gets the list item value.
     * 
     * @param doc the doc
     * @param schemaName the schema
     * @param fieldName the field
     * @param itemIndex the item index
     * @return the list item value
     * @throws ClientException the client exception
     */
    @SuppressWarnings("unchecked")
    public static Serializable getListItemValue(DocumentModel doc,
            String schemaName, String fieldName, int itemIndex)
            throws ClientException {

        Object prop = doc.getProperty(schemaName, fieldName);

        if (prop == null) {
            throw new ClientException(String.format(
                    "Property [%s:%s] is null on doc '%s'.", schemaName,
                    fieldName, doc.getTitle()));
        }

        if (!(prop instanceof List<?> || prop instanceof Object[])) {
            throw new ClientException(
                    "Cannot call this method with a non list type property");
        }

        if ((prop instanceof List<?> && itemIndex >= ((List<?>) prop).size())
                || (prop instanceof Object[] && itemIndex >= ((Object[]) prop).length)) {
            return null;
        }

        if (prop instanceof List<?>) {
            return ((List<String>) prop).get(itemIndex);
        } else {
            return ((String[]) prop)[itemIndex];
        }
    }

    /**
     * Gets the complex list item names.
     * 
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the complex item names
     * @throws Exception the exception
     */
    public static List<String> getComplexListItemNames(String schemaName,
            String fieldName) throws Exception {

        List<String> complexItemNames = new ArrayList<String>();

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(schemaName);
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

        Type fieldType = field.getType();
        if (!fieldType.isListType()) {
            throw new ClientException(String.format(
                    "Field [%s:%s] is not a list type.", schemaName, fieldName));
        }

        ListType fieldListType = (ListType) fieldType;
        Type listItemType = fieldListType.getFieldType();

        if (!listItemType.isComplexType()) {
            throw new ClientException(String.format(
                    "Field [%s:%s] is not a complex list type.", schemaName,
                    fieldName));
        }

        ComplexType listItemComplexType = (ComplexType) listItemType;
        Collection<Field> complexItems = listItemComplexType.getFields();

        Iterator<Field> complexItemsIt = complexItems.iterator();
        while (complexItemsIt.hasNext()) {
            Field complexItem = complexItemsIt.next();
            complexItemNames.add(complexItem.getName().getLocalName());
        }

        return complexItemNames;
    }

    /**
     * Gets the complex list item values.
     * 
     * @param doc the doc
     * @param schemaName the schema
     * @param fieldName the field
     * @param itemIndex the item index
     * @param complexItemName the complex item name
     * @return the complex list item values
     * @throws ClientException the client exception
     */
    @SuppressWarnings("unchecked")
    public static Serializable getComplexListItemValue(DocumentModel doc,
            String schemaName, String fieldName, int itemIndex,
            String complexItemName) throws ClientException {

        Serializable listItem = getListItemValue(doc, schemaName, fieldName,
                itemIndex);

        if (listItem == null) {
            return null;
        }

        if (!(listItem instanceof Map<?, ?>)) {
            throw new ClientException(String.format(
                    "Property %s:%s[%d] on doc '%s' is not a complex type.",
                    schemaName, fieldName, itemIndex, doc.getTitle()));
        }

        return ((Map<String, Serializable>) listItem).get(complexItemName);

    }

    /**
     * Gets the complex property.
     * 
     * @param doc the doc
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the complex property
     * @throws ClientException the client exception
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Serializable> getComplexProperty(
            DocumentModel doc, String schemaName, String fieldName)
            throws ClientException {

        Object prop = doc.getProperty(schemaName, fieldName);
        if (prop == null) {
            return null;
        }

        if (!(prop instanceof Map<?, ?>)) {
            throw new ClientException(String.format(
                    "Field [%s:%s] is not a complex type.", schemaName,
                    fieldName));
        }

        return ((Map<String, Serializable>) prop);

    }

    /**
     * Checks if is simple property.
     * 
     * @param prop the prop
     * @return true, if is simple property
     */
    public static boolean isSimpleProperty(Object prop) {
        return !isComplexProperty(prop) && !isListProperty(prop);
    }

    /**
     * Checks if is complex property.
     * 
     * @param prop the prop
     * @return true, if is complex property
     */
    public static boolean isComplexProperty(Object prop) {
        return prop instanceof Map<?, ?>;
    }

    /**
     * Checks if is list property.
     * 
     * @param prop the prop
     * @return true, if is list property
     */
    public static boolean isListProperty(Object prop) {
        return prop instanceof List<?> || prop instanceof Object[];
    }
}
