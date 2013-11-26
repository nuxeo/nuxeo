/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.api.operation;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;

/**
 * Completes a task. If this is the last task the workflow will continue.
 *
 * @since 5.7.2
 */
@Operation(id = CompleteTaskOperation.ID, category = Constants.CAT_WORKFLOW, label = "Complete task", requires = Constants.WORKFLOW_CONTEXT, description = "Completes the input task. If this is the last task the workflow will continue. "
        + "Returns back the task document. \"Status\" is the id of the button the user would have clicked to submit the task form (if the outgoing transitions of the workflow node that created the task have conditions depending on it).")
public class CompleteTaskOperation {
    public static final String ID = "Workflow.CompleteTaskOperation";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "status", required = false)
    protected String status;

    @Param(name = "comment", required = false)
    protected String comment;

    @Context
    protected DocumentRoutingService documentRoutingService;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel completeTask(DocumentModel task)
            throws ClientException {
        Map<String, Object> data = new HashMap<String, Object>();
        if (comment != null) {
            data.put("comment", comment);
        }
        documentRoutingService.endTask(session, task.getAdapter(Task.class),
                new HashMap<String, Object>(), status);
        return task;
    }
}
