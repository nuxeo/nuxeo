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
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * Updates entries for a given {@link org.nuxeo.ecm.directory.Directory}.
 * <p>
 * Entries to update are sent as a JSON array.
 * <p>
 * Returns the updated entries ids as a JSON array of JSON objects containing
 * all fields
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Operation(id = UpdateDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Updates directory entries", description = "Updates directory entries. Entries to update are sent as a JSON array. Returns the updated entries ids as a JSON array of JSON objects containing all fields")
public class UpdateDirectoryEntries extends AbstractDirectoryOperation {

    public static final String ID = "Directory.UpdateEntries";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "entries", required = true)
    protected String jsonEntries;

    @OperationMethod
    public Blob run() throws Exception {
        validateCanManageDirectories(ctx);

        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> entries = mapper.readValue(jsonEntries,
                new TypeReference<List<Map<String, Object>>>() {
                });
        List<String> ids = new ArrayList<String>();

        Directory directory = directoryService.getDirectory(directoryName);
        String idField = directory.getIdField();

        Session session = null;
        try {

            session = directoryService.open(directoryName);
            for (Map<String, Object> entry : entries) {
                if (entry.containsKey(idField)) {
                    DocumentModel doc = session.getEntry((String) entry.get(idField));
                    if (doc != null) {
                        doc.getDataModel(directory.getSchema()).setMap(entry);
                        session.updateEntry(doc);
                        ids.add(doc.getId());
                    }
                }
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
}
