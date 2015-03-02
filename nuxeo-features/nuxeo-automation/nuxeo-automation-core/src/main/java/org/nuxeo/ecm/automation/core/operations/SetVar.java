/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Generic fetch document operation that can be used on any context that has a document as the input. This operation
 * takes the context input and it returns it as a document. If the input is not a document, an exception is thrown.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SetVar.ID, category = Constants.CAT_EXECUTION, label = "Set Context Variable", description = "Set a context variable given a name and the value. To compute the value at runtime from the current context you should use an EL expression as the value. This operation works on any input type and return back the input as the output.")
public class SetVar {

    public static final String ID = "Context.SetVar";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @Param(name = "value", required = false)
    protected Object value;

    @OperationMethod
    public void run() {
        ctx.put(name, value);
    }

}
