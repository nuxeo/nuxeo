/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.trash.TrashService;

/**
 * @since 10.1
 */
@Operation(id = EmptyTrash.ID, category = Constants.CAT_DOCUMENT, label = "Empty Trash", description = "Emtpy a folder's trash by permanently deleting documents marked as trashed.")
public class EmptyTrash {

    public static final String ID = "Document.EmptyTrash";

    @Context
    protected CoreSession session;

    @Context
    protected TrashService trashService;

    @Param(name = "parent", description = "A Folderish document whose trash will be emptied")
    DocumentModel parent;

    @OperationMethod()
    public void run() {
        if (parent == null || !parent.hasFacet(FacetNames.FOLDERISH)) {
            throw new UnsupportedOperationException("Empty trash can only be performed on a Folderish document");
        }
        trashService.purgeDocuments(session, trashService.getPurgeableDocumentRefs(parent));
    }

}
