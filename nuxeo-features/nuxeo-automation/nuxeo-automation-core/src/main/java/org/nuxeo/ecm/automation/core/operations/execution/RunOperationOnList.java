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
package org.nuxeo.ecm.automation.core.operations.execution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;

/**
 * Run an embedded operation chain using the current input. The output is
 * undefined (Void)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @since 5.5
 */
@Operation(id = RunOperationOnList.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run For Each", description = "Run an operation for each element from the list defined by the 'list' parameter. The 'list' parameter is pointing to a context variable that represents the list which will be iterated. The 'item' parameter represents the name of the context variable which will point to the current element in the list at each iteration. You can use the 'isolate' parameter to specify whether or not the evalution context is the same as the parent context or a copy of it. If the 'isolate' parameter is 'true' then a copy of the current context is used and so that modifications in this context will not affect the parent context. Any input is accepted. The input is returned back as output when operation terminates. The 'parameters' injected are accessible in the subcontext ChainParameters. For instance, @{ChainParameters['parameterKey']}.")
public class RunOperationOnList {

    public static final String ID = "Context.RunOperationOnList";

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name = "list")
    protected String listName;

    @Param(name = "item", required = false, values = "item")
    protected String itemName = "item";

    @Param(name = "isolate", required = false, values = "true")
    protected boolean isolate = true;

    @Param(name = "parameters", description = "Accessible in the subcontext ChainParameters. For instance, @{ChainParameters['parameterKey']}.", required = false)
    protected Properties chainParameters;

    @OperationMethod
    public void run() throws Exception {
        OperationContext subctx = ctx.getSubContext(isolate, ctx.getInput());

        Collection<?> list = null;
        if (ctx.get(listName) instanceof Object[]) {
            list = Arrays.asList((Object[]) ctx.get(listName));
        } else if (ctx.get(listName) instanceof Collection<?>) {
            list = (Collection<?>) ctx.get(listName);
        } else {
            throw new UnsupportedOperationException(
                    ctx.get(listName).getClass() + " is not a Collection");
        }
        for (Object value : list) {
            subctx.put(itemName, value);
            service.run(subctx, chainId, (Map) chainParameters);
        }
    }

}
