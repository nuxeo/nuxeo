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

import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
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
 * Class for the operation getting the elements present in the Favorites
 * collection.
 *
 * @since 6.0
 */
@Operation(id = GetDocumentsFromFavoritesOperation.ID, category = Constants.CAT_DOCUMENT, label = "Get documents from favorites", description = "Get the list "
        + "of documents visible from the currentUser's favorites. This is returning a list of documents.")
public class GetDocumentsFromFavoritesOperation {

    public static final String ID = "Collection.GetElementsInFavorite";

    @Context
    protected CoreSession session;

    @Context
    protected FavoritesManager favoritesManager;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @OperationMethod
    public DocumentModelList run(DocumentModel context)
            throws Exception {

        DocumentModel favorites = favoritesManager.getFavorites(
                context, session);

        Map<String, Object> vars = ctx.getVars();
        vars.put("searchTerm", favorites.getId());
        vars.put("providerName",
                CollectionConstants.COLLECTION_CONTENT_PAGE_PROVIDER);

        OperationContext subctx = new OperationContext(ctx.getCoreSession(),
                vars);

        OperationChain chain = new OperationChain("operation");
        OperationParameters oparams = new OperationParameters(
                DocumentPageProviderOperation.ID, vars);
        chain.add(oparams);
        return (PaginableDocumentModelListImpl) service.run(subctx, chain);
    }
}
