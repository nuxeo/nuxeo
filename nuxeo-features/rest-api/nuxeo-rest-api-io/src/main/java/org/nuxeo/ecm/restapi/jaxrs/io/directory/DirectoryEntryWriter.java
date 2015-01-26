/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io.directory;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class DirectoryEntryWriter extends EntityWriter<DirectoryEntry> {

    /**
     *
     */
    public static final String ENTITY_TYPE = "directoryEntry";

    @Override
    protected void writeEntityBody(JsonGenerator jg, DirectoryEntry directoryEntry) throws IOException, ClientException {
        String directoryName = directoryEntry.getDirectoryName();

        jg.writeStringField("directoryName", directoryName);
        jg.writeObjectFieldStart("properties");
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Directory directory = ds.getDirectory(directoryName);

        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        Schema schema = sm.getSchema(directory.getSchema());

        DocumentModel entry = directoryEntry.getDocumentModel();

        for (Field field : schema.getFields()) {
            QName fieldName = field.getName();
            String key = fieldName.getLocalName();

            jg.writeFieldName(key);
            if (key.equals(directory.getPasswordField())) {
                jg.writeString("");
            } else {
                JsonDocumentWriter.writePropertyValue(jg, entry.getProperty(fieldName.getPrefixedName()), "");
            }

        }
        jg.writeEndObject();

    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

}
