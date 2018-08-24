/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.automation.core.operations.collections;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Class for the operation getting the elements present in the Favorites collection.
 *
 * @since 6.0
 */
@Operation(id = GetDocumentsFromFavoritesOperation.ID, category = Constants.CAT_DOCUMENT, label = "Get documents from favorites", description = "Get the list "
        + "of documents visible from the currentUser's favorites. This is returning a list of documents.", aliases = { "Collection.GetElementsInFavorite" })
public class GetDocumentsFromFavoritesOperation {

    public static final String ID = "Favorite.GetDocuments";

    @Context
    protected CoreSession session;

    @Context
    protected FavoritesManager favoritesManager;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @OperationMethod
    public DocumentModelList run(DocumentModel context) throws OperationException {

        DocumentModel favorites = favoritesManager.getFavorites(context, session);

        Map<String, Object> vars = new HashMap<>();
        vars.put("searchTerm", favorites.getId());
        vars.put("providerName", CollectionConstants.COLLECTION_CONTENT_PAGE_PROVIDER);

        OperationChain chain = new OperationChain("operation");
        OperationParameters oparams = new OperationParameters(DocumentPageProviderOperation.ID, vars);
        chain.add(oparams);
        return (PaginableDocumentModelListImpl) service.run(ctx, chain);
    }
}
