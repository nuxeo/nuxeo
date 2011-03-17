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
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.ExitException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = ExitOperation.ID, category = Constants.CAT_EXECUTION, label = "Exit", description = "Exit the chain execution. You can control how chain termination is done - either by sillently exiting and returning the last input as the chain output, either by throwing an exception. Also you can control if the current transaction will rollback or not. This is a void operation and will return back the input object.")
public class ExitOperation {

    public static final String ID = "Test.Exit";

    @Context
    protected OperationContext ctx;

    @Param(name = "error", required = false)
    protected boolean error = false;

    @Param(name = "rollback", required = false)
    protected boolean rollback = false;

    @OperationMethod
    public void run() throws java.lang.Exception {
        if (error && rollback) {
            throw new java.lang.Exception("termination error");
        }
        throw new ExitException(ctx.getInput(), rollback);
    }

}
