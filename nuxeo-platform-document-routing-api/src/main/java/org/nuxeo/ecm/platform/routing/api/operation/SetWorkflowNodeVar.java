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

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Generic fetch document operation that can be used on any context that has a
 * document as the input. This operation takes the context input and it returns
 * it as a document. If the input is not a document, an exception is thrown.
 *
 */
@Operation(id = SetWorkflowNodeVar.ID, category = Constants.CAT_WORKFLOW, requires = Constants.WORKFLOW_CONTEXT, label = "Set Workflow Node Variable", description = "Set a workflow node variable given a name and the value in the context of a running workflow. To compute the value at runtime from the current context you should use an EL expression as the value. This operation works on any input type and return back the input as the output.")
public class SetWorkflowNodeVar {

    public static final String ID = "Context.SetWorkflowNodeVar";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @Param(name = "value")
    protected Object value;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public void run() {
        if (ctx.get(Constants.VAR_WORKFLOW_NODE) != null) {
            ((Map<String, Serializable>) ctx.get(Constants.VAR_WORKFLOW_NODE)).put(
                    name, (Serializable) value);
        }
    }
}
