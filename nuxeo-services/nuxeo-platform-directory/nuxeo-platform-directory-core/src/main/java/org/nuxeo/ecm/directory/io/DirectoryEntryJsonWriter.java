/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.directory.io;

import static java.util.Locale.ENGLISH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.MAX_DEPTH_PARAM;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertiesJsonReader;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Convert {@link DirectoryEntry} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link DirectoryEntry}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link DirectoryEntryJsonWriter#extend(Object, JsonGenerator)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *   "entity-type": "directoryEntry",
 *   "directoryName": "DIRECTORY_NAME", <- use it to update an existing document
 *   "properties": {
 *     <- entry properties depending on the directory schema (password fields are hidden)
 *     <- format is managed by {@link DocumentPropertiesJsonReader}
 *   }
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DirectoryEntryJsonWriter extends ExtensibleEntityJsonWriter<DirectoryEntry> {

    public static final String ENTITY_TYPE = "directoryEntry";

    /** @since 11.1 */
    public static final String PARENT_FIELD_NAME = "parent";

    private static final String MESSAGES_BUNDLE = "messages";

    private static final Log log = LogFactory.getLog(DirectoryEntryJsonWriter.class);

    @Inject
    private SchemaManager schemaManager;

    @Inject
    private DirectoryService directoryService;

    public DirectoryEntryJsonWriter() {
        super(ENTITY_TYPE, DirectoryEntry.class);
    }

    @Override
    protected void writeEntityBody(DirectoryEntry entry, JsonGenerator jg) throws IOException {
        String directoryName = entry.getDirectoryName();
        Directory directory = directoryService.getDirectory(directoryName);
        String parentDirectoryName = directory.getParentDirectory();
        boolean hasParentDirectory = StringUtils.isNotBlank(parentDirectoryName);
        String schemaName = directory.getSchema();
        String passwordField = directory.getPasswordField();
        DocumentModel document = entry.getDocumentModel();
        jg.writeStringField("directoryName", directoryName);
        jg.writeStringField("id", document.getId());
        Schema schema = schemaManager.getSchema(schemaName);
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        // for each properties, fetch it
        jg.writeObjectFieldStart("properties");
        Set<String> translated = ctx.getTranslated(ENTITY_TYPE);
        Set<String> fetched = ctx.getFetched(ENTITY_TYPE);
        for (Field field : schema.getFields()) {
            QName fieldName = field.getName();
            String key = fieldName.getLocalName();
            jg.writeFieldName(key);
            if (key.equals(passwordField)) {
                jg.writeString("");
            } else {
                Property property = document.getProperty(fieldName.getPrefixedName());
                boolean managed = false;
                Object value = property.getValue();
                if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
                    String valueString = (String) value;
                    String localName = fieldName.getLocalName();
                    if (fetched.contains(localName)) {
                        // try to fetch a referenced entry (parent for example)
                        try (Closeable resource = ctx.wrap().with(MAX_DEPTH_PARAM, "max").open()) {
                            String dName = PARENT_FIELD_NAME.equals(localName) && hasParentDirectory
                                    ? parentDirectoryName
                                    : directoryName;
                            managed = writeFetchedValue(jg, dName, localName, valueString);
                        }
                    } else if (translated.contains(localName)) {
                        // try to fetch a translated property
                        managed = writeTranslatedValue(jg, localName, valueString);
                    }
                }
                if (!managed) {
                    propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE,
                            new OutputStreamWithJsonWriter(jg));
                }
            }
        }
        jg.writeEndObject();
    }

    protected boolean writeFetchedValue(JsonGenerator jg, String directoryName, String fieldName, String value)
            throws IOException {
        try (Session session = directoryService.open(directoryName)) {
            DocumentModel entryModel = session.getEntry(value);
            if (entryModel != null) {
                DirectoryEntry entry = new DirectoryEntry(directoryName, entryModel);
                writeEntity(entry, jg);
                return true;
            }
        }
        return false;
    }

    protected boolean writeTranslatedValue(JsonGenerator jg, String fieldName, String value) throws IOException {
        Locale locale = ctx.getLocale();
        String msg = getMessageString(value, new Object[0], locale);
        if (msg == null && locale != ENGLISH) {
            msg = getMessageString(value, new Object[0], ENGLISH);
        }
        if (msg != null && !msg.equals(value)) {
            jg.writeString(msg);
            return true;
        }
        return false;
    }

    public static String getMessageString(String key, Object[] params, Locale locale) {
        try {
            return I18NUtils.getMessageString(MESSAGES_BUNDLE, key, params, locale);
        } catch (MissingResourceException e) {
            log.trace("No bundle found", e);
            return null;
        }
    }

}
