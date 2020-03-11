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

import java.util.stream.Collectors;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @since 10.1
 */
@Operation(id = OrderDocument.ID, category = Constants.CAT_DOCUMENT, label = "Order Document", description = "Given a parent document, order the source child before the destination child.", since = "10.1")
public class OrderDocument {

    public static final String ID = "Document.Order";

    public static final String NOT_SAME_FOLDER_ERROR_MSG = "The document can only be ordered within the same folder.";

    @Param(name = "before", required = false)
    protected DocumentModel before;

    @Context
    protected CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        try {
            session.orderBefore(doc.getParentRef(), doc.getName(), before != null ? before.getName() : null);
            session.save();
            return session.getDocument(doc.getRef());
        } catch (DocumentNotFoundException e) {
            throw new NuxeoException(NOT_SAME_FOLDER_ERROR_MSG);
        }
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        return docs.stream().map(doc -> run(doc)).collect(Collectors.toCollection(DocumentModelListImpl::new));
    }

}
