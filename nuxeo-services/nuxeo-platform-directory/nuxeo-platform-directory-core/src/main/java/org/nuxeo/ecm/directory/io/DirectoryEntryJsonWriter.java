/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.directory.io;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import javax.inject.Inject;

import org.codehaus.jackson.JsonGenerator;
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
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link DirectoryEntry} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link DirectoryEntry}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(DirectoryEntry, JsonWriter)}.
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
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DirectoryEntryJsonWriter extends ExtensibleEntityJsonWriter<DirectoryEntry> {

    public static final String ENTITY_TYPE = "directoryEntry";

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
        DocumentModel document = entry.getDocumentModel();
        String schemaName = directoryService.getDirectorySchema(directoryName);
        String passwordField = directoryService.getDirectoryPasswordField(directoryName);
        jg.writeStringField("directoryName", directoryName);
        Schema schema = schemaManager.getSchema(schemaName);
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        jg.writeObjectFieldStart("properties");
        for (Field field : schema.getFields()) {
            QName fieldName = field.getName();
            String key = fieldName.getLocalName();
            jg.writeFieldName(key);
            if (key.equals(passwordField)) {
                jg.writeString("");
            } else {
                Property property = document.getProperty(fieldName.getPrefixedName());
                propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE,
                        new OutputStreamWithJsonWriter(jg));
            }
        }
        jg.writeEndObject();
    }

}
