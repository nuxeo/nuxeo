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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SetInputAsVar.ID, category = Constants.CAT_EXECUTION, label = "Set Context Variable From Input", description = "Set a context variable that points to the current input object. You must give a name for the variable. This operation works on any input type and return back the input as the output.")
public class SetInputAsVar {

    public static final String ID = "Context.SetInputAsVar";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @OperationMethod
    public void run() {
        ctx.put(name, ctx.getInput());
    }

}
