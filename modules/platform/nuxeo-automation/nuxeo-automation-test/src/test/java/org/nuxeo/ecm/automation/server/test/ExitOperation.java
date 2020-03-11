/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.test;

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
    public void run() throws Exception {
        if (error && rollback) {
            throw new Exception("termination error");
        }
        throw new ExitException(ctx.getInput(), rollback);
    }

}
