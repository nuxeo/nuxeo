/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services.directory;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * Delete entries for a given {@link org.nuxeo.ecm.directory.Directory}.
 * <p>
 * Entries ids to delete are sent through a JSON array.
 * <p>
 * Instead of being deleted, the entries can be marked as obsolete using the
 * {@code markObsolete} parameter.
 * <p>
 * Returns deleted, or marked as obsolete, entries id as a JSON array.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Operation(id = DeleteDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Deletes directory entries", description = "Deletes directory entries. Entries ids to delete are sent through a JSON array. Returns deleted entries id as a JSON array.")
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
    public Blob run() throws Exception {
        validateCanManageDirectories(ctx);

        ObjectMapper mapper = new ObjectMapper();

        List<String> entries = mapper.readValue(jsonEntries,
                new TypeReference<List<String>>() {
                });
        List<String> ids = new ArrayList<String>();
        Session session = null;
        try {
            session = directoryService.open(directoryName);
            for (String entryId : entries) {
                if (markObsolete) {
                    markObsoleteOrDelete(session, entryId);
                } else {
                    session.deleteEntry(entryId);
                }
                ids.add(entryId);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, ids);
        return new InputStreamBlob(new ByteArrayInputStream(
                writer.toString().getBytes("UTF-8")), "application/json");
    }

    protected void markObsoleteOrDelete(Session session, String id)
            throws ClientException {
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
