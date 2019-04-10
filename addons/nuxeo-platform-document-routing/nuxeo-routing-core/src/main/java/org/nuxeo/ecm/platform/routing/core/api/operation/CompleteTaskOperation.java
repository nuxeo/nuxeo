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
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;

/**
 * Completes a task. If this is the last task the workflow will continue.
 *
 * @since 5.7.2
 */
@Operation(id = CompleteTaskOperation.ID, category = Constants.CAT_WORKFLOW, label = "Complete task", requires = Constants.WORKFLOW_CONTEXT, description = "Completes the input task. If this is the last task the workflow will continue. "
        + "Returns back the task document. \"Status\" is the id of the button the user would have clicked to submit the task form (if the outgoing transitions of the workflow node that created the task have conditions depending on it)."
        + "@since 5.9.3 and 5.8.0-HF11 you can set multiple  node or workflow variables when completing the task (also similar to ending the task via form submision from the UI).The variables are specified as <i>key=value</i> pairs separated by a new line."
        + "To specify multi-line values you can use a \\ character followed by a new line. <p>Example:<pre>description=foo bar</pre>For updating a date, you will need to expose the value as ISO 8601 format, "
        + "for instance : <p>Example:<pre>workflowVarString=A sample value<br>workflowVarDate=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}</pre><p>"
        + "For all values, you have to submit a JSON representation. This is an example for a variable of type StringList:"
        + "<p><pre>nodeVarList = [\"John Doe\", \"John Test\"]</pre></p>")
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

    // @since 5.9.3, 5.8.0-HF11
    @Param(name = "nodeVariables", required = false)
    protected Properties nodeVariables;

    // @since 5.9.3, 5.8.0-HF11
    @Param(name = "workflowVariables", required = false)
    protected Properties workflowVariables;

    @Context
    protected DocumentRoutingService documentRoutingService;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel completeTask(DocumentModel task) throws ClientException {
        Map<String, Object> data = new HashMap<String, Object>();
        if (comment != null) {
            data.put("comment", comment);
        }

        // the service expects an unique map containing both worflow and
        // nodeVariables
        if (nodeVariables != null) {
            data.put(Constants.VAR_WORKFLOW_NODE, nodeVariables);
        }
        if (workflowVariables != null) {
            data.put(Constants.VAR_WORKFLOW, workflowVariables);
        }
        data.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, Boolean.TRUE);
        documentRoutingService.endTask(session, task.getAdapter(Task.class), data, status);
        return task;
    }
}
