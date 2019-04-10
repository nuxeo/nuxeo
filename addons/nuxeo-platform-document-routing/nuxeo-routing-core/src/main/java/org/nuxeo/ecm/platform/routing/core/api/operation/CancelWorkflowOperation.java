/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.api.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;

/**
 * Cancels the workflow with the given id, where the id is the id of the document representing the workflow instance.
 *
 * @since 5.7.2
 */
@Operation(id = CancelWorkflowOperation.ID, category = Constants.CAT_WORKFLOW, label = "Cancel workflow", requires = Constants.WORKFLOW_CONTEXT, description = "Cancel the workflow with the given id, "
        + "where the required id is the id of the document representing the workflow instance.", aliases = { "Context.CancelWorkflow" })
public class CancelWorkflowOperation {

    public static final String ID = "WorkflowInstance.Cancel";

    private static final Log log = LogFactory.getLog(CancelWorkflowOperation.class);

    @Context
    protected CoreSession session;

    @Param(name = "id")
    protected String id;

    @Context
    protected DocumentRoutingEngineService documentRoutingEngineService;

    @OperationMethod
    public void cancelWorkflow() {
        DocumentModel workflowInstance;
        try {
            workflowInstance = session.getDocument(new IdRef(id));
        } catch (DocumentNotFoundException e) {
            e.addInfo("Cannot get workflow instance with id: " + id);
            throw e;
        }
        documentRoutingEngineService.cancel(workflowInstance.getAdapter(DocumentRoute.class), session);
    }
}
