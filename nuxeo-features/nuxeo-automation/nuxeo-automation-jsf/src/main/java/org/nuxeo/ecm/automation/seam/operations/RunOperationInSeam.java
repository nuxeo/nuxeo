/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.automation.seam.operations;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.jsf.OperationHelper;

/**
 * Execute an operation within the Seam context (doing automatically the needed init and cleanup)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = RunOperationInSeam.ID, category = Constants.CAT_UI, label = "Run operation in Seam Context", description = "Initialize a Seam context (including Conversation if needed) and runs an Operation", aliases = {"WebUI.RunOperationInSeam"})
public class RunOperationInSeam {

    public static final String ID = "Seam.RunOperation";

    @Context
    protected OperationContext ctx;

    @Param(name = "conversationId", required = false)
    protected String conversationId;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name = "isolate", required = false, values = "false")
    protected boolean isolate = false;

    @OperationMethod
    public Object run() throws OperationException {
        try (OperationContext subctx = ctx.getSubContext(isolate)) {
            if (!OperationHelper.isSeamContextAvailable()) {
                SeamOperationFilter.handleBeforeRun(ctx, conversationId);
                try {
                    return runChain(subctx);
                } finally {
                    SeamOperationFilter.handleAfterRun(ctx, conversationId);
                }
            } else {
                return runChain(subctx);
            }
        }
    }

    protected Object runChain(OperationContext subctx) throws OperationException {
        return service.run(subctx, chainId.startsWith("Chain.") ? chainId.substring(6) : chainId);
    }
}
