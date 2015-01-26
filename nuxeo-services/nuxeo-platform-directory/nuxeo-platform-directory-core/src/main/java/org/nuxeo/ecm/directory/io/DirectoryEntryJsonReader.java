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

import org.apache.commons.lang3.reflect.TypeUtils;
import org.codehaus.jackson.JsonNode;
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
 *
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
        Session session = null;
        JsonNode propsNode = jn.get("properties");
        if (propsNode != null && !propsNode.isNull() && propsNode.isObject()) {
            String id = getStringField(propsNode, directory.getIdField());
            try {
                session = directory.getSession();
                DocumentModel entry = session.getEntry(id);
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
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
        return null;
    }
}
