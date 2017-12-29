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
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Creates entries for a given {@link org.nuxeo.ecm.directory.Directory}.
 * <p>
 * Entries to create are sent as a JSON array.
 * <p>
 * Returns the created entries ids as a JSON array.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Operation(id = CreateDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Creates directory entries", description = "Creates directory entries. Entries are sent as a JSON array. Returning the created entries ids as a JSON array.", addToStudio = false)
public class CreateDirectoryEntries extends AbstractDirectoryOperation {

    public static final String ID = "Directory.CreateEntries";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "entries", required = true)
    protected String jsonEntries;

    @OperationMethod
    public Blob run() throws IOException {
        validateCanManageDirectories(ctx);

        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> entries = mapper.readValue(jsonEntries,
                new TypeReference<List<Map<String, Object>>>() {
                });
        List<String> ids = new ArrayList<String>();
        try (Session session = directoryService.open(directoryName)) {
            for (Map<String, Object> entry : entries) {
                DocumentModel doc = session.createEntry(entry);
                ids.add(doc.getId());
            }
        }

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, ids);
        return Blobs.createJSONBlob(writer.toString());
    }

}
