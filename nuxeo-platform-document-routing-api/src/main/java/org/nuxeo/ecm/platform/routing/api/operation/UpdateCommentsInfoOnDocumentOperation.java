/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api.operation;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/***
 * Updates the number of comments stored on the {@link DocumentRouteStep}. This
 * is used to avoid unnecessary jena calls when displaying the number of
 * comments on each step. This updates the number of comments on the documents
 * from the relations. To invoke it: run from nuxeo-shell : run
 * updateCommentsOnDoc
 *
 * @author mcedica
 */
@Operation(id = UpdateCommentsInfoOnDocumentOperation.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Update comments number on the document", description = "Update comments number on the document", addToStudio = false)
public class UpdateCommentsInfoOnDocumentOperation {

    public final static String ID = "Document.Routing.UpdateCommentsInfoOnDocument";

    @Context
    protected CoreSession session;

    @OperationMethod
    public void updateCommentsInfo() throws ClientException {
        DocumentModelList allDocsToUpdate = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:mixinType = '%s'",
                DocumentRoutingConstants.COMMENTS_INFO_HOLDER_FACET));
        if (allDocsToUpdate == null || allDocsToUpdate.size() == 0) {
            return;
        }
        for (DocumentModel documentModel : allDocsToUpdate) {
            CommentableDocument commentableDoc = documentModel.getAdapter(CommentableDocument.class);
            documentModel.setPropertyValue(
                    DocumentRoutingConstants.COMMENTS_NO_PROPERTY_NAME,
                    Integer.valueOf(commentableDoc.getComments().size()));
            session.saveDocument(documentModel);
        }

    }
}
