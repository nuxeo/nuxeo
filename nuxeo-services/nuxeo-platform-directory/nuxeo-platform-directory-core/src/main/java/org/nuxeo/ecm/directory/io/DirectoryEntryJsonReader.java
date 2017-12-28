/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertiesJsonReader.DEFAULT_SCHEMA_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter.ENTITY_TYPE;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertiesJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Convert Json as {@link DirectoryEntry}.
 * <p>
 * Format is (any additional json property is ignored):
 *
 * <pre>
 * {
 *   "entity-type": "directoryEntry",
 *   "directoryName": "DIRECTORY_NAME", <- use it to update an existing document
 *   "properties": {
 *     <- entry properties depending on the directory schema (password fields are hidden)
 *     <- format is managed by {@link DocumentPropertiesJsonReader}
 *   }
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DirectoryEntryJsonReader extends EntityJsonReader<DirectoryEntry> {

    @Inject
    private DirectoryService directoryService;

    public DirectoryEntryJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    protected DirectoryEntry readEntity(JsonNode jn) throws IOException {
        String directoryName = getStringField(jn, "directoryName");
        Directory directory = directoryService.getDirectory(directoryName);
        String schema = directory.getSchema();

        try (Session session = directory.getSession()) {
            DocumentModel entry = null;
            String id = getStringField(jn, "id");
            if (StringUtils.isNotBlank(id)) {
                entry = session.getEntry(id);
            }

            JsonNode propsNode = jn.get("properties");
            if (propsNode != null && !propsNode.isNull() && propsNode.isObject()) {
                if (entry == null) {
                    // backward compatibility; try to fetch the entry from the id inside the properties
                    id = getStringField(propsNode, directory.getIdField());
                    entry = session.getEntry(id);
                }
                if (entry == null) {
                    entry = BaseSession.createEntryModel(null, schema, id, new HashMap<String, Object>());
                }
                ParameterizedType genericType = TypeUtils.parameterize(List.class, Property.class);
                try (Closeable resource = ctx.wrap().with(DEFAULT_SCHEMA_NAME, schema).open()) {
                    List<Property> properties = readEntity(List.class, genericType, propsNode);
                    for (Property property : properties) {
                        entry.setPropertyValue(property.getName(), property.getValue());
                    }
                }
                return new DirectoryEntry(directory.getName(), entry);
            }
        }
        return null;
    }
}
