/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.execution;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Run an embedded operation chain using the current input. The output is
 * undefined (Void)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RunOperation.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run Operation", description = "Run an operation chain in the current context")
public class RunOperation {

    public static final String ID = "Context.RunOperation";

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @OperationMethod
    public void run() throws Exception {
        OperationContext subctx = new OperationContext(ctx.getCoreSession());
        subctx.setInput(ctx.getInput());
        service.run(subctx, chainId);
    }

}
