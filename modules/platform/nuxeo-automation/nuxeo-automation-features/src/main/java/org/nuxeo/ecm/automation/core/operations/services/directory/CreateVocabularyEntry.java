/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.automation.core.operations.services.directory;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * @since 8.3 Adds a new entry to a vocabulary.
 *        <p>
 *        Notice: This is for a nuxeo Vocabulary, which is a specific kind of Directory. This code expects the
 *        following:
 *        <ul>
 *        <li>The vocabulary schema <i>must</i> have <code>id</code>, <code>label</code>, <code>obsolete</code> and
 *        <code>ordering</code> fields</li>
 *        <li>If it is hierarchical, it must also have the <code>parent</code> field</li>
 *        </ul>
 */
@Operation(id = CreateVocabularyEntry.ID, category = Constants.CAT_SERVICES, label = "Vocabulary: Add Entry", description = "Add a new entry in the <i>vocabularyName</i> vocabulary only if <i>id</i> is not found (an existing entry is"
        + "not updated). If <i>label</i> is empty, it is set to the id. WARNING: Current user must have enough rights "
        + "to write in a vocabulary.")
public class CreateVocabularyEntry {

    public static final String ID = "Directory.CreateVocabularyEntry";

    @Context
    protected DirectoryService directoryService;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "vocabularyName")
    protected String name;

    @Param(name = "id")
    protected String id;

    @Param(name = "label", required = false)
    protected String label;

    @Param(name = "parent", required = false)
    protected String parent = "";

    @Param(name = "obsolete", required = false)
    protected long obsolete;

    @Param(name = "ordering", required = false)
    protected long ordering;

    @OperationMethod
    public void run() {

        if (StringUtils.isBlank(id)) {
            return;
        }

        try (Session directorySession = directoryService.open(name)) {
            if (directorySession.hasEntry(id)) {
                return;
            }
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", id);
            entry.put("label", defaultIfEmpty(label, id));
            String dirSchema = directoryService.getDirectorySchema(name);
            Schema schema = schemaManager.getSchema(dirSchema);
            if (schema.hasField("parent")) {
                entry.put("parent", parent);
            }
            entry.put("obsolete", obsolete);
            entry.put("ordering", ordering);
            directorySession.createEntry(entry);
        }

    }

}
