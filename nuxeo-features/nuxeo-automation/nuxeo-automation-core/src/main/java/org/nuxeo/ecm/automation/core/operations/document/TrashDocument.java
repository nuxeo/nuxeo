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

import java.util.Collections;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.trash.TrashService;

/**
 * @since 10.1
 */
@Operation(id = TrashDocument.ID, category = Constants.CAT_DOCUMENT, label = "Trash", description = "Moves documents to the trash.")
public class TrashDocument {

    public static final String ID = "Document.Trash";

    @Context
    protected CoreSession session;

    @Context
    protected TrashService trashService;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        trashService.trashDocuments(Collections.singletonList(doc));
        // return doc with updated deleted markers
        return session.getDocument(doc.getRef());
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        trashService.trashDocuments(docs);
        // return docs with updated deleted markers
        DocumentModelList result = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            if (session.exists(doc.getRef())) {
                result.add(session.getDocument(doc.getRef()));
            }
        }
        return result;
    }

}
