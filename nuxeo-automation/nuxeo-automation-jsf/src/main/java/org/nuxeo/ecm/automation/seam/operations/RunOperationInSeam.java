/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Execute an operation within the Seam context
 *(doing automatically the needed init and cleanup)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
@Operation(id = RunOperationInSeam.ID, category = Constants.CAT_UI, label = "Run operation in Seam Context", description = "Initialize a Seam context (including Conversation if needed) and runs an Operation")
public class RunOperationInSeam {

    public static final String ID = "Seam.RunOperation";

    @Context
    protected OperationContext ctx;

    @Param(name="conversationId", required=false)
    protected String conversationId;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name="isolate", required = false, values = "false")
    protected boolean isolate = false;

    @OperationMethod
    public Object run() throws Exception {

        Map<String, Object> vars = isolate ? new HashMap<String, Object>(ctx.getVars()) : ctx.getVars();

        OperationContext subctx = new OperationContext(ctx.getCoreSession(), vars);
        subctx.setInput(ctx.getInput());
        SeamOperationFilter.handleBeforeRun(ctx, conversationId);
        try {
            if (chainId.startsWith("Chain.")) {
                return service.run(subctx, chainId.substring(6));
            } else {
                OperationChain chain = new OperationChain("operation");
                OperationParameters oparams = new OperationParameters(chainId,vars);
                chain.add(oparams);
                return service.run(subctx, chain);
            }
        } finally {
            SeamOperationFilter.handleAfterRun(ctx, conversationId);
        }
    }
}
