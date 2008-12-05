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
 * $Id: DocumentModelUtils.java 28563 2008-01-08 08:56:29Z sfermigier $
 */

package org.nuxeo.ecm.core.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility methods to deal with property names retrieval.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class DocumentModelUtils {

    private static final Log log = LogFactory.getLog(DocumentModelUtils.class);

    // Utility class.
    private DocumentModelUtils() {
    }

    /**
     * Encodes a property name to use it in a url.
     *
     * @param propertyName like dc:title, file:content.3.filename (?)
     */
    public static String encodePropertyName(String propertyName) {
        if (propertyName == null) {
            return null;
        }
        String[] items = propertyName.split(":");
        return join(items, "/");
    }

    /**
     * Decodes a property path given in a url.
     *
     * @param propertyPath like dc:title file/content/3/filename (?)
     */
    public static String decodePropertyName(String propertyPath) {
        if (propertyPath == null) {
            return null;
        }
        String[] items = propertyPath.split("/");
        return join(items, ".");
    }

    /**
     * The given propertyName should have 'schema_name:property_name' format.
     *
     * @param doc
     * @param propertyName
     * @return <code>null</code> if any error occurs.
     */
    public static Object getPropertyValue(DocumentModel doc, String propertyName) {
        try {
            String schemaName = getSchemaName(propertyName);
            String fieldName = getFieldName(propertyName);
            return doc.getProperty(schemaName, fieldName);
        } catch (Exception e) {
            log.warn("Error trying to get property " + propertyName + ". "
                    + e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug(e);
            }
            return null;
        }
    }

    /**
     * Obtains a property value given its path.
     *
     * @param doc the document
     * @param propertyPath the property path
     * @return the property value or <code>null</code> if an error occured.
     */
    public static Object getComplexPropertyValue(DocumentModel doc,
            String propertyPath) {
        try {
            return doc.getPropertyValue(propertyPath);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtains the schema name from the full propertyName.
     *
     * @param propertyName
     * @return
     * @throws IllegalArgumentException if the propertyName does not have a
     *             schema:field pattern
     */
    public static String getSchemaName(String propertyName) {
        String[] s = propertyName.split(":");
        if (s.length != 2) {
            throw new IllegalArgumentException("offending value: "
                    + propertyName);
        }
        String prefix = s[0];
        Schema schema = null;
        try {
            SchemaManager tm = Framework.getService(SchemaManager.class);
            schema = tm.getSchemaFromPrefix(prefix);
        } catch (Exception e) {
        }
        if (schema == null) {
            // fall back on prefix as it may be the schema name
            return prefix;
        } else {
            return schema.getName();
        }
    }

    /**
     * Obtains the field name from the full propertyName.
     *
     * @param propertyName
     * @return
     * @throws IllegalArgumentException if the propertyName does not have a
     *             schema:field pattern
     */
    public static String getFieldName(String propertyName) {
        int index = propertyName.indexOf(":");
        if (index == -1) {
            throw new IllegalArgumentException("offending value: "
                    + propertyName);
        }
        return propertyName.substring(index + 1);
    }

    private static String join(String[] items, String sep) {
        StringBuilder buf = new StringBuilder();
        int max = items.length - 1;
        for (int i = 0; i < items.length; i++) {
            buf.append(items[i]);
            if (i < max) {
                buf.append(sep);
            }
        }
        return buf.toString();
    }

    /**
     * Gets all properties defined in declared schemas of a DocumentModel.
     *
     * @param docModel
     * @return map with property names as keys
     */
    public static Map<String, Object> getProperties(DocumentModel docModel) {
        final String[] schemas = docModel.getDeclaredSchemas();
        if (schemas == null) {
            throw new IllegalStateException(
                    "schemas are not declared for docModel " + docModel);
        }
        final Map<String, Object> allProps = new HashMap<String, Object>();
        for (String schemaName : schemas) {
            Map<String, Object> props;
            try {
                props = docModel.getProperties(schemaName);
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            allProps.putAll(props);
        }
        return allProps;
    }

}
