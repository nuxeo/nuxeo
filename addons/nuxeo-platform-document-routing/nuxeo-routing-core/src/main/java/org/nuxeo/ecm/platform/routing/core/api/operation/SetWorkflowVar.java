/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.routing.core.api.operation;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;

/**
 * Set a workflow variable. The workflow variable must exists on the workflow. If no workflowId is specified the
 * variable is set on the current workflow.
 *
 * @since 5.6
 */
@Operation(id = SetWorkflowVar.ID, category = Constants.CAT_WORKFLOW, label = "Set Workflow Variable", requires = Constants.WORKFLOW_CONTEXT, description = "Set a workflow variable. The workflow variable must exists on "
        + "the workflow. If no workflowId is specified the variable is set on the current workflow."
        + "To compute the value at runtime from the current context you should use a MVEL expression as the value. This operation works on any input type and return back the input as the output.")
public class SetWorkflowVar {

    public static final String ID = "Context.SetWorkflowVar";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "name")
    protected String name;

    @Param(name = "value", required = false)
    protected Object value;

    @Param(name = "workflowInstanceId", required = false)
    protected String workflowInstanceId;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public void run() {
        // first fill context
        if (ctx.get(Constants.VAR_WORKFLOW) != null) {
            ((Map<String, Serializable>) ctx.get(Constants.VAR_WORKFLOW)).put(name, (Serializable) value);
        }
        // get workflow instance id from context if not in automation parameters
        if (workflowInstanceId == null) {
            workflowInstanceId = (String) ctx.get("workflowInstanceId");
        }
        if (workflowInstanceId == null) {
            return;
        }
        // finally save graph variables
        DocumentModel workflowInstance = session.getDocument(new IdRef(workflowInstanceId));
        GraphRoute graph = workflowInstance.getAdapter(GraphRoute.class);
        Map<String, Serializable> vars = graph.getVariables();
        vars.put(name, (Serializable) value);
        graph.setVariables(vars);
    }
}
