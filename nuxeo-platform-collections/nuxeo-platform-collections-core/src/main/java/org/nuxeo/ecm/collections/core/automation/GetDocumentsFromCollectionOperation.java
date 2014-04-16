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
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Class for the operation to get the list of documents from a Collection.
 *
 * @since 5.9.4
 */
@Operation(id = GetDocumentsFromCollectionOperation.ID, category = Constants.CAT_DOCUMENT, label = "Get documents from collection", description = "Get the list "
        + "of documents visible by the currentUser in a collection. This is returning a list of documents.")
public class GetDocumentsFromCollectionOperation {

    public static final String ID = "Collection.GetDocumentsFromCollection";

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @OperationMethod
    public PaginableDocumentModelListImpl run(DocumentModel collection)
            throws Exception {
        Map<String, Object> vars = ctx.getVars();
        vars.put("searchTerm", collection.getId());
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
