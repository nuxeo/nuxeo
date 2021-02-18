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
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = ExitOperation.ID, category = Constants.CAT_EXECUTION, label = "Exit", description = "Exit the chain execution. You can control how chain termination is done - either by sillently exiting and returning the last input as the chain output, either by throwing an exception. Also you can control if the current transaction will rollback or not. This is a void operation and will return back the input object.")
public class ExitOperation {

    public static final String ID = "Test.Exit";

    public static final int ERR_CODE = 444;

    @Context
    protected OperationContext ctx;

    @Param(name = "error", required = false)
    protected boolean error = false;

    @Param(name = "rollback", required = false)
    protected boolean rollback = false;

    @Param(name = "throwOnCleanup", required = false)
    protected boolean throwOnCleanup = false;

    @OperationMethod
    public void run() throws OperationException {
        if (throwOnCleanup) {
            RequestContext.getActiveContext().addRequestCleanupHandler(req -> {
                throw new NuxeoException("exc in cleanup", ERR_CODE);
            });
            return;
        }
        if (error && rollback) {
            throw new NuxeoException("termination error", ERR_CODE);
        }
        throw new ExitException("test exit", rollback);
    }

}
