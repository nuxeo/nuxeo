/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * Check out the input document.
 */
@Operation(id = CheckOutDocument.ID, category = Constants.CAT_DOCUMENT, label = "Check Out", description = "Checks out the input document. Returns back the document.")
public class CheckOutDocument {

    public static final String ID = "Document.CheckOut";

    @Context
    protected CoreSession session;

    @OperationMethod
    public DocumentRef run(DocumentRef doc) throws Exception {
        if (!session.isCheckedOut(doc)) {
            session.checkOut(doc);
        }
        return doc;
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        if (!doc.isCheckedOut()) {
            session.checkOut(doc.getRef());
            doc.refresh(DocumentModel.REFRESH_STATE, null);
        }
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws Exception {
        DocumentModelList result = new DocumentModelListImpl(
                (int) docs.totalSize());
        for (DocumentModel doc : docs) {
            result.add(run(doc));
        }
        return result;
    }

    @OperationMethod
    public DocumentModelList run(DocumentRefList docs) throws Exception {
        DocumentModelList result = new DocumentModelListImpl(
                (int) docs.totalSize());
        for (DocumentRef doc : docs) {
            result.add(session.getDocument(run(doc)));
        }
        return result;
    }

}
