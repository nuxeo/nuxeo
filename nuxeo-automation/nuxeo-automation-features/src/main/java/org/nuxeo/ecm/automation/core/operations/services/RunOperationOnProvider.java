/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mariana Cedica
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProvider;

/**
 * Run an embedded operation chain for each page of the pageProvider given as
 * input. The output is undefined (Void)
 * 
 * @since 5.6
 */
@Operation(id = RunOperationOnProvider.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run For Each Page", description = "Run an operation for each page of the provider defined by the provider name, the operation input is the curent page ")
public class RunOperationOnProvider {
    public static final String ID = "Context.RunOperationOnProvider";

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name = "isolate", required = false, values = "true")
    protected boolean isolate = true;

    @OperationMethod
    public void run(PaginableDocumentModelListImpl paginableList)

    throws InvalidChainException, OperationException, Exception {
        PageProvider<DocumentModel> pageProvider = paginableList.getProvider();
        Map<String, Object> vars = isolate ? new HashMap<String, Object>(
                ctx.getVars()) : ctx.getVars();
        OperationContext subctx = new OperationContext(ctx.getCoreSession(),
                vars);
        final long initialRC = pageProvider.getResultsCount();
        final long initialNoPages = pageProvider.getNumberOfPages();

        PaginableDocumentModelListImpl input = new PaginableDocumentModelListImpl(
                pageProvider);
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