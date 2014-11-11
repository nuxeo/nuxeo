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
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.collections.api.CollectionConstants;

/**
 * Class for the operation to get the collections matching the searching terms.
 *
 * @since 5.9.4
 */
@Operation(id = GetCollectionsOperation.ID, category = Constants.CAT_DOCUMENT, label = "Get collections", description = "Get the list of all the collections visible by the currentUser. "
        + "This is returning a list of collections.")
public class GetCollectionsOperation {

    public static final String ID = "Collection.GetCollections";

    @Param(name = "searchTerm")
    protected String searchTerm;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @OperationMethod
    public PaginableDocumentModelListImpl run() throws Exception {

        Map<String, Object> vars = ctx.getVars();
        StringList sl = new StringList();
        sl.add(searchTerm + (searchTerm.endsWith("%") ? "" : "%"));
        sl.add(DocumentPageProviderOperation.CURRENT_USERID_PATTERN);
        vars.put("queryParams", sl);
        vars.put("providerName", CollectionConstants.COLLECTION_PAGE_PROVIDER);

        OperationContext subctx = new OperationContext(ctx.getCoreSession(),
                vars);

        OperationChain chain = new OperationChain("operation");
        OperationParameters oparams = new OperationParameters(DocumentPageProviderOperation.ID, vars);
        chain.add(oparams);
        return (PaginableDocumentModelListImpl) service.run(subctx, chain);
    }
}
