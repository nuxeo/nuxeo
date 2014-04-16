/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.collections.core.automation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Class for the operation to remove a list of documents in a Collection.
 *
 * @since 5.9.4
 */
@Operation(id = RemoveFromCollectionOperation.ID, category = Constants.CAT_DOCUMENT, label = "Remove from collection", description = "Remove a list of documents from a collection. "
        + "No value is returned.")
public class RemoveFromCollectionOperation {
    public static final String ID = "Collection.RemoveFromCollection";

    @Context
    protected CoreSession session;

    @Context
    protected CollectionManager collectionManager;

    @Param(name = "collection")
    protected DocumentModel collection;

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws ClientException {
        for (DocumentModel doc : docs) {
            collectionManager.removeFromCollection(collection, doc, session);
        }

        return docs;
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws ClientException {
        collectionManager.removeFromCollection(collection, doc, session);

        return doc;
    }
}
