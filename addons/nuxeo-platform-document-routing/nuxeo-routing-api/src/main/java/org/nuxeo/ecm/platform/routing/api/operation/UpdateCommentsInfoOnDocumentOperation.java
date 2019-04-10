/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api.operation;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/***
 * Updates the number of comments stored on the {@link DocumentRouteStep}. This is used to avoid unnecessary jena calls
 * when displaying the number of comments on each step. This updates the number of comments on the documents from the
 * relations. To invoke it: run from nuxeo-shell : run updateCommentsOnDoc
 *
 * @author mcedica
 */
@Operation(id = UpdateCommentsInfoOnDocumentOperation.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Update comments number on the document", description = "Update comments number on the document", addToStudio = false)
public class UpdateCommentsInfoOnDocumentOperation {

    public final static String ID = "Document.Routing.UpdateCommentsInfoOnDocument";

    @Context
    protected CoreSession session;

    @OperationMethod
    public void updateCommentsInfo() {
        DocumentModelList allDocsToUpdate = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:mixinType = '%s'",
                DocumentRoutingConstants.COMMENTS_INFO_HOLDER_FACET));
        if (allDocsToUpdate == null || allDocsToUpdate.size() == 0) {
            return;
        }
        for (DocumentModel documentModel : allDocsToUpdate) {
            CommentableDocument commentableDoc = documentModel.getAdapter(CommentableDocument.class);
            documentModel.setPropertyValue(DocumentRoutingConstants.COMMENTS_NO_PROPERTY_NAME,
                    Integer.valueOf(commentableDoc.getComments().size()));
            session.saveDocument(documentModel);
        }

    }
}
