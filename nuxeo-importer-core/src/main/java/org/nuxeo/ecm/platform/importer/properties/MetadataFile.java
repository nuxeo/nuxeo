/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.importer.properties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;

/**
 * Handle properties file creation from a {@code DocumentModel}.
 * <p>
 * Only support the types that {@code MetadataCollector} knows.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class MetadataFile {

    private static final Log log = LogFactory.getLog(MetadataFile.class);

    protected DocumentModel doc;

    protected Properties metadataProperties = new Properties();

    /**
     * Create a {@code MetadataFile} from a {@code DocumentModel}'s schemas.
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromDocument(DocumentModel doc) {
        MetadataFile mdFile = new MetadataFile(doc);
        mdFile.load();
        return mdFile;
    }

    /**
     * Create a {@code MetadataFile} from the listed schemas (with all properties) and the listed properties of a
     * {@code DocumentModel}
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromSchemasAndProperties(DocumentModel doc, List<String> allPropertiesSchemas,
            List<String> properties) {
        MetadataFile mdFile = new MetadataFile(doc);
        mdFile.load(allPropertiesSchemas, properties);
        return mdFile;
    }

    /**
     * Create a {@code MetadataFile} from the listed schemas (with all properties) of a {@code DocumentModel}
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromSchemas(DocumentModel doc, List<String> allPropertiesSchemas) {
        return createFromSchemasAndProperties(doc, allPropertiesSchemas, Collections.<String> emptyList());
    }

    /**
     * Create a {@code MetadataFile} from the listed properties of a {@code DocumentModel}
     *
     * @return a new MetadataFile object
     */
    public static MetadataFile createFromProperties(DocumentModel doc, List<String> properties) {
        return createFromSchemasAndProperties(doc, Collections.<String> emptyList(), properties);
    }

    protected MetadataFile(DocumentModel doc) {
        this.doc = doc;
    }

    protected void load(List<String> allPropertiesSchemas, List<String> properties) {
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
                String message = String.format("Property '%s' not found on document type: %s. Skipping it.", property,
                        doc.getType());
                log.debug(message);
            }
        }
    }

    protected void addAllProperties(String schema) {
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
                        list = new ArrayList<>(list);
                        list.add("");
                    }
                    metadataProperties.put(propertyKey, String.join(MetadataCollector.LIST_SEPARATOR, list));
                }
            } catch (ClassCastException e) {
                // do nothing
            }
        } else if (value instanceof String[]) {
            List<String> list = Arrays.asList((String[]) value);
            if (!list.isEmpty()) {
                if (list.size() == 1) {
                    list = new ArrayList<>(list);
                    list.add("");
                }
                metadataProperties.put(propertyKey, String.join(MetadataCollector.ARRAY_SEPARATOR, list));
            }
        } else if (value instanceof Calendar) {
            metadataProperties.put(propertyKey, new DateType().encode(value));
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

    protected void load() {
        for (String schema : doc.getSchemas()) {
            addAllProperties(schema);
        }
    }

    /**
     * Write the properties file to the given {@code file}
     */
    public void writeTo(File file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            metadataProperties.store(fos, null);
        } catch (IOException e) {
            throw new NuxeoException("Unable to write the metadata properties to " + file.getAbsolutePath(), e);
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
