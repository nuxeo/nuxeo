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
        + "This is returning a list of collections.", aliases = { "Collection.GetCollections" })
public class GetCollectionsOperation {

    public static final String ID = "User.GetCollections";

    @Param(name = "searchTerm")
    protected String searchTerm;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @OperationMethod
    public PaginableDocumentModelListImpl run() throws OperationException {
        StringList sl = new StringList();
        sl.add(searchTerm + (searchTerm.endsWith("%") ? "" : "%"));
        sl.add(DocumentPageProviderOperation.CURRENT_USERID_PATTERN);
        OperationChain chain = new OperationChain("operation");
        Map<String, Object> vars = new HashMap<>();
        vars.put("queryParams", sl);
        vars.put("providerName", CollectionConstants.COLLECTION_PAGE_PROVIDER);
        OperationParameters oparams = new OperationParameters(DocumentPageProviderOperation.ID, vars);
        chain.add(oparams);
        return (PaginableDocumentModelListImpl) service.run(ctx, chain);
    }
}
