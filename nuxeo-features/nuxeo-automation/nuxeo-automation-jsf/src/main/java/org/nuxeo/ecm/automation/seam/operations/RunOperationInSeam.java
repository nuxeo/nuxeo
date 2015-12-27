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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
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
@Operation(id = RunOperationInSeam.ID, category = Constants.CAT_UI, label = "Run operation in Seam Context", description = "Initialize a Seam context (including Conversation if needed) and runs an Operation", aliases = { "Seam.RunOperation" })
public class RunOperationInSeam {

    public static final String ID = "WebUI.RunOperationInSeam";

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

        Map<String, Object> vars = isolate ? new HashMap<String, Object>(ctx.getVars()) : ctx.getVars();

        OperationContext subctx = new OperationContext(ctx.getCoreSession(), vars);
        subctx.setInput(ctx.getInput());
        if (!OperationHelper.isSeamContextAvailable()) {
            SeamOperationFilter.handleBeforeRun(ctx, conversationId);
            try {
                return runChain(subctx, vars);
            } finally {
                SeamOperationFilter.handleAfterRun(ctx, conversationId);
            }
        } else {
            return runChain(subctx, vars);
        }
    }

    protected Object runChain(OperationContext subctx, Map<String, Object> vars) throws OperationException {
        if (chainId.startsWith("Chain.")) {
            return service.run(subctx, chainId.substring(6));
        } else {
            OperationChain chain = new OperationChain("operation");
            OperationParameters oparams = new OperationParameters(chainId, vars);
            chain.add(oparams);
            return service.run(subctx, chain);
        }
    }
}
