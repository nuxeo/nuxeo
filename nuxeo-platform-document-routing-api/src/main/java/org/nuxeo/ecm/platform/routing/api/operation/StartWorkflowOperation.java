/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.api.operation;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;

/**
 * Starts the workflow with the given id on the input documents. Returns back
 * the input documents.
 *
 * @since 5.6
 *
 */
@Operation(id = StartWorkflowOperation.ID, category = Constants.CAT_WORKFLOW, label = "Start workflow", requires = Constants.WORKFLOW_CONTEXT, description = "Starts the workflow with the given id on the input documents. Returns back the input documents ."
        + " The workflow instance is available under the \"WorkflowInstance\" context variable")
public class StartWorkflowOperation {

    public static final String ID = "Context.StartWorkflow";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "id", required = true)
    protected String id;

    @Context
    protected DocumentRoutingService documentRoutingService;

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws ClientException {
        List<String> ids = new ArrayList<String>();
        for (DocumentModel doc : docs) {
            ids.add(doc.getId());
        }
        startNewInstance(ids);
        return docs;
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws ClientException {
        List<String> ids = new ArrayList<String>();
        ids.add(doc.getId());
        startNewInstance(ids);
        return doc;
    }

    private void startNewInstance(List<String> ids) throws ClientException {
        DocumentRoute model = documentRoutingService.getRouteModelWithId(
                session, id);
        if (model == null) {
            throw new ClientException(
                    "Could not find any workflow with the id " + id);

        }
        DocumentRoute workflow = documentRoutingService.createNewInstance(
                model, ids, session, true);
        ctx.put("WorkflowInstance", workflow);
    }
}
