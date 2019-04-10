/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.importer.properties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * Handle properties file creation from a {@code DocumentModel}.
 * <p>
 * Only support the types that {@code MetadataCollector} knows.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class MetadataFile {

    private static final Log log = LogFactory.getLog(MetadataFile.class);

    public static DateFormat DATE_FORMAT = new SimpleDateFormat(
            MetadataCollector.DATE_FORMAT);

    protected DocumentModel doc;

    protected Properties metadataProperties = new Properties();

    /**
     * Create a {@code MetadataFile} from a {@code DocumentModel}'s schemas.
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromDocument(DocumentModel doc)
            throws ClientException {
        MetadataFile mdFile = new MetadataFile(doc);
        mdFile.load();
        return mdFile;
    }

    /**
     * Create a {@code MetadataFile} from the listed schemas (with all
     * properties) and the listed properties of a {@code DocumentModel}
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromSchemasAndProperties(
            DocumentModel doc, List<String> allPropertiesSchemas,
            List<String> properties) throws ClientException {
        MetadataFile mdFile = new MetadataFile(doc);
        mdFile.load(allPropertiesSchemas, properties);
        return mdFile;
    }

    /**
     * Create a {@code MetadataFile} from the listed schemas (with all
     * properties) of a {@code DocumentModel}
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromSchemas(DocumentModel doc,
            List<String> allPropertiesSchemas) throws ClientException {
        return createFromSchemasAndProperties(doc, allPropertiesSchemas,
                Collections.<String> emptyList());
    }

    /**
     * Create a {@code MetadataFile} from the listed properties of a {@code
     * DocumentModel}
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromProperties(DocumentModel doc,
            List<String> properties) throws ClientException {
        return createFromSchemasAndProperties(doc,
                Collections.<String> emptyList(), properties);
    }

    protected MetadataFile(DocumentModel doc) {
        this.doc = doc;
    }

    protected void load(List<String> allPropertiesSchemas,
            List<String> properties) throws ClientException {
        if (!metadataProperties.isEmpty()) {
            return;
        }

        for (String schema : allPropertiesSchemas) {
            addAllProperties(schema);
        }

        for (String property : properties) {
            try {
                addProperty(property, doc.getPropertyValue(property));
            } catch (PropertyException e) {
                String message = String.format(
                        "Property '%s' not found on document type: %s. Skipping it.",
                        property, doc.getType());
                log.debug(message);
            }
        }
    }

    protected void addAllProperties(String schema) throws ClientException {
        DataModel dm = doc.getDataModel(schema);
        if (dm != null) {
            for (Map.Entry<String, Object> entry : dm.getMap().entrySet()) {
                Object value = entry.getValue();
                String propertyKey = entry.getKey();
                addProperty(computePropertyKey(propertyKey, schema), value);
            }
        }
    }

    public void addProperty(String propertyKey, Object value) {
        if (value instanceof String) {
            metadataProperties.put(propertyKey, value);
        } else if (value instanceof List) {
            try {
                List<String> list = (List<String>) value;
                if (!list.isEmpty()) {
                    if (list.size() == 1) {
                        list = new ArrayList<String>(list);
                        list.add("");
                    }
                    metadataProperties.put(propertyKey, StringUtils.join(list,
                            MetadataCollector.LIST_SEPARATOR));
                }
            } catch (ClassCastException e) {
                // do nothing
            }
        } else if (value instanceof String[]) {
            List<String> list = Arrays.asList((String[]) value);
            if (!list.isEmpty()) {
                if (list.size() == 1) {
                    list = new ArrayList<String>(list);
                    list.add("");
                }
                metadataProperties.put(propertyKey, StringUtils.join(list,
                        MetadataCollector.ARRAY_SEPARATOR));
            }
        } else if (value instanceof Calendar) {
            Date date = ((Calendar) value).getTime();
            metadataProperties.put(propertyKey, DATE_FORMAT.format(date));
        } else if (value instanceof Number) {
            metadataProperties.put(propertyKey, value.toString());
        }
    }

    protected String computePropertyKey(String propertyKey, String schema) {
        if (!propertyKey.contains(":")) {
            propertyKey = schema + ":" + propertyKey;
        }
        return propertyKey;
    }

    protected void load() throws ClientException {
        for (String schema : doc.getDeclaredSchemas()) {
            addAllProperties(schema);
        }
    }

    /**
     * Write the properties file to the given {@code file}
     */
    public void writeTo(File file) throws ClientException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            metadataProperties.store(fos, null);
        } catch (IOException e) {
            throw new ClientException(
                    "Unable to write the metadata properties to "
                            + file.getAbsolutePath(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // nothing to do...
                }
            }
        }
    }

}
