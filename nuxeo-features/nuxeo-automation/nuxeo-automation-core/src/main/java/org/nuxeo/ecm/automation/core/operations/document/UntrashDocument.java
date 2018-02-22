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
import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.trash.TrashService;

/**
 * @since 10.1
 */
@Operation(id = UntrashDocument.ID, category = Constants.CAT_DOCUMENT, label = "Untrash", description = "Undeletes documents (and ancestors if needed to make them visible)..")
public class UntrashDocument {

    public static final String ID = "Document.Untrash";

    @Context
    protected CoreSession session;

    @Context
    protected TrashService trashService;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        List<DocumentModel> docs = Collections.singletonList(doc);
        if (trashService.canPurgeOrUndelete(docs, session.getPrincipal())) {
            trashService.undeleteDocuments(docs);
            return session.getDocument(doc.getRef());
        } else {
            throw new NuxeoException("Cannot untrash these documents");
        }
    }

}
