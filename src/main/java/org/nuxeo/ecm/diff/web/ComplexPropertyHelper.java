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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * TODO: refactor as a service + use xpath for fetching doc property values.
 * Helper to get complex property names and values, and list property values.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public final class ComplexPropertyHelper {

    private static final String SYSTEM_ELEMENT = "system";

    private static final String TYPE_ELEMENT = "type";

    private static final String PATH_ELEMENT = "path";

    private static final String LIFECYCLE_STATE_ELEMENT = "lifecycle-state";

    /**
     * Gets the simple property value.
     * 
     * @param doc the doc
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the simple property value
     * @throws ClientException the client exception
     */
    public static Serializable getSimplePropertyValue(DocumentModel doc,
            String schemaName, String fieldName) throws ClientException {

        Serializable propertyValue = null;

        if (SYSTEM_ELEMENT.equals(schemaName)) {
            if (TYPE_ELEMENT.equals(fieldName)) {
                propertyValue = doc.getType();
            } else if (PATH_ELEMENT.equals(fieldName)) {
                propertyValue = doc.getPathAsString();
            } else if (LIFECYCLE_STATE_ELEMENT.equals(fieldName)) {
                propertyValue = doc.getCurrentLifeCycleState();
            }
        } else {
            propertyValue = (Serializable) doc.getProperty(schemaName,
                    fieldName);
        }

        return propertyValue;

    }

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

        Object complexProp = getComplexProperty(doc, schemaName, fieldName);

        if (complexProp == null) {
            throw new ClientException(String.format(
                    "Property [%s:%s] is null on doc '%s'.", schemaName,
                    fieldName, doc.getTitle()));
        }

        return getComplexOrContentItemValue(complexProp, complexItemName);
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
            return ((List<Serializable>) prop).get(itemIndex);
        } else {
            return ((Serializable[]) prop)[itemIndex];
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
    public static Serializable getComplexListItemValue(DocumentModel doc,
            String schemaName, String fieldName, int itemIndex,
            String complexItemName) throws ClientException {

        Serializable listItem = getListItemValue(doc, schemaName, fieldName,
                itemIndex);

        if (listItem == null) {
            return null;
        }

        if (!(listItem instanceof Map<?, ?> || listItem instanceof SQLBlob)) {
            throw new ClientException(
                    String.format(
                            "Property %s:%s[%d] on doc '%s' is not a complex type nor a SQLBlob.",
                            schemaName, fieldName, itemIndex, doc.getTitle()));
        }

        return getComplexOrContentItemValue(listItem, complexItemName);
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
     * Checks if is content property.
     * 
     * @param prop the prop
     * @return true, if is content property
     */
    public static boolean isContentProperty(Object prop) {
        return prop instanceof SQLBlob;
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

    /**
     * Gets the complex property.
     * 
     * @param doc the doc
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the complex property
     * @throws ClientException the client exception
     */
    private static Object getComplexProperty(DocumentModel doc,
            String schemaName, String fieldName) throws ClientException {

        Object prop = doc.getProperty(schemaName, fieldName);
        if (prop == null) {
            return null;
        }

        if (!(prop instanceof Map<?, ?> || prop instanceof SQLBlob)) {
            throw new ClientException(
                    String.format(
                            "Field [%s:%s] on doc '%s' is not a complex type nor a SQLBlob.",
                            schemaName, fieldName, doc.getTitle()));
        }

        return prop;

    }

    /**
     * Gets the complex or content item value.
     * 
     * @param prop the prop
     * @param itemName the item name
     * @return the complex or content item value
     * @throws ClientException the client exception
     */
    @SuppressWarnings("unchecked")
    private static Serializable getComplexOrContentItemValue(Object prop,
            String itemName) throws ClientException {

        Serializable value;

        if (prop instanceof Map<?, ?>) {
            value = ((Map<String, Serializable>) prop).get(itemName);
        } else { // complexType instanceof SQLBlob
            SQLBlob blobProp = ((SQLBlob) prop);
            if ("name".equals(itemName)) {
                value = blobProp.getFilename();
            } else if ("length".equals(itemName)) {
                value = blobProp.getLength();
            } else if ("data".equals(itemName)) {
                value = blobProp.getBinary();
            } else if ("encoding".equals(itemName)) {
                value = blobProp.getEncoding();
            } else if ("digest".equals(itemName)) {
                value = blobProp.getDigest();
            } else if ("mime-type".equals(itemName)) {
                value = blobProp.getMimeType();
            } else {
                throw new ClientException(
                        String.format(
                                "Property [%s] is of type [content] => it has no sub item named [%s].",
                                prop, itemName));
            }
        }

        if (value == null) {
            return StringUtils.EMPTY;
        }

        return value;
    }
}
