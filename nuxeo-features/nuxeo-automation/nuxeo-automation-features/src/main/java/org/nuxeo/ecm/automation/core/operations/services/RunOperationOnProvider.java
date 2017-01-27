/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.automation.core.operations.services;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Run an embedded operation chain for each page of the pageProvider given as input. The output is undefined (Void)
 *
 * @since 5.6
 */
@Operation(id = RunOperationOnProvider.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run For Each Page", description = "Run an operation for each page of the provider defined by the provider name, the operation input is the curent page ", aliases = { "Context.RunOperationOnProvider" })
public class RunOperationOnProvider {
    public static final String ID = "RunOperationOnProvider";

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name = "isolate", required = false, values = { "true" })
    protected boolean isolate = true;

    @OperationMethod
    public void run(PaginableDocumentModelListImpl paginableList) throws InvalidChainException, OperationException, Exception {
        PageProvider<DocumentModel> pageProvider = paginableList.getProvider();
        try (OperationContext subctx = ctx.getSubContext(isolate)) {
            long initialRC = pageProvider.getResultsCount();
            long initialNoPages = pageProvider.getNumberOfPages();

            PaginableDocumentModelListImpl input = new PaginableDocumentModelListImpl(pageProvider);
            while (pageProvider.getCurrentPageIndex() < initialNoPages) {
                subctx.setInput(input);
                service.run(subctx, chainId);
                if (!pageProvider.isNextPageAvailable()) {
                    break;
                }
                // check if the chain run is "consuming" docs returned by the
                // pageProvider or not
                pageProvider.refresh();
                input = new PaginableDocumentModelListImpl(pageProvider);

                if (pageProvider.getResultsCount() == initialRC) {
                    pageProvider.nextPage();
                    input = new PaginableDocumentModelListImpl(pageProvider);
                }
            }
        }

    }
}
