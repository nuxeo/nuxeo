/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 * @deprecated since 7.10 The Nuxeo JSON marshalling was migrated to nuxeo-core-io. This class is replaced by
 *             org.nuxeo.ecm.directory.io.DirectoryEntrysonWriter which is registered by default and available to
 *             marshal {@link DirectoryEntry} from the Nuxeo Rest API thanks to the JAX-RS marshaller
 *             {@link JsonCoreIODelegate}.
 */
@Deprecated
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class DirectoryEntryWriter extends EntityWriter<DirectoryEntry> {

    /**
     *
     */
    public static final String ENTITY_TYPE = "directoryEntry";

    @Override
    protected void writeEntityBody(JsonGenerator jg, DirectoryEntry directoryEntry) throws IOException {
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
