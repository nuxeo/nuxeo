/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services.directory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Delete entries for a given {@link org.nuxeo.ecm.directory.Directory}.
 * <p>
 * Entries ids to delete are sent through a JSON array.
 * <p>
 * Instead of being deleted, the entries can be marked as obsolete using the {@code markObsolete} parameter.
 * <p>
 * Returns deleted, or marked as obsolete, entries id as a JSON array.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Operation(id = DeleteDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Deletes directory entries", description = "Deletes directory entries. Entries ids to delete are sent through a JSON array. Returns deleted entries id as a JSON array.", addToStudio = false)
public class DeleteDirectoryEntries extends AbstractDirectoryOperation {

    public static final String ID = "Directory.DeleteEntries";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "entries", required = true)
    protected String jsonEntries;

    @Param(name = "markObsolete", required = false)
    protected boolean markObsolete = false;

    @OperationMethod
    public Blob run() throws IOException {
        validateCanManageDirectories(ctx);

        ObjectMapper mapper = new ObjectMapper();

        List<String> entries = mapper.readValue(jsonEntries, new TypeReference<List<String>>() {
        });
        List<String> ids = new ArrayList<String>();
        try (Session session = directoryService.open(directoryName)) {
            for (String entryId : entries) {
                if (markObsolete) {
                    markObsoleteOrDelete(session, entryId);
                } else {
                    session.deleteEntry(entryId);
                }
                ids.add(entryId);
            }
        }

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, ids);
        return Blobs.createJSONBlob(writer.toString());
    }

    protected void markObsoleteOrDelete(Session session, String id) {
        Directory directory = directoryService.getDirectory(directoryName);
        String schemaName = directory.getSchema();
        Schema schema = schemaManager.getSchema(schemaName);
        if (schema.hasField("obsolete")) {
            DocumentModel entry = session.getEntry(id);
            if (entry != null) {
                entry.setProperty(schemaName, "obsolete", 1);
                session.updateEntry(entry);
            }
        } else {
            session.deleteEntry(id);
        }
    }

}
