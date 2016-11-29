/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.execution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Run an embedded operation chain using the current input. The output is undefined (Void)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @since 5.5
 */
@Operation(id = RunOperationOnList.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run For Each", description = "Run an operation for each element from the list defined by the 'list' parameter. The 'list' parameter is pointing to a context variable that represents the list which will be iterated. The 'item' parameter represents the name of the context variable which will point to the current element in the list at each iteration. You can use the 'isolate' parameter to specify whether or not the evalution context is the same as the parent context or a copy of it. If the 'isolate' parameter is 'true' then a copy of the current context is used and so that modifications in this context will not affect the parent context. Any input is accepted. The input is returned back as output when operation terminates. The 'parameters' injected are accessible in the subcontext ChainParameters. For instance, @{ChainParameters['parameterKey']}.", aliases = { "Context.RunOperationOnList" })
public class RunOperationOnList {

    public static final String ID = "RunOperationOnList";

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Context
    protected CoreSession session;

    @Param(name = "id")
    protected String chainId;

    @Param(name = "list")
    protected String listName;

    @Param(name = "item", required = false, values = "item")
    protected String itemName = "item";

    @Param(name = "isolate", required = false, values = "true")
    protected boolean isolate = true;

    @Param(name = "parameters", description = "Accessible in the subcontext " + "ChainParameters. For instance, "
            + "@{ChainParameters['parameterKey']}.", required = false)
    protected Properties chainParameters = new Properties();

    /**
     * @since 6.0 Define if the chain in parameter should be executed in new transaction.
     */
    @Param(name = "newTx", required = false, values = "false", description = "Define if the chain in parameter should be "
            + "executed in new transaction.")
    protected boolean newTx = false;

    /**
     * @since 6.0 Define transaction timeout (default to 60 sec).
     */
    @Param(name = "timeout", required = false, description = "Define " + "transaction timeout (default to 60 sec).")
    protected Integer timeout = 60;

    /**
     * @since 6.0 Define if transaction should rollback or not (default to true).
     */
    @Param(name = "rollbackGlobalOnError", required = false, values = "true", description = "Define if transaction should rollback or not "
            + "(default to true)")
    protected boolean rollbackGlobalOnError = true;

    @OperationMethod
    public void run() throws OperationException {
        // Handle isolation option
        Map<String, Object> vars = isolate ? new HashMap<>(ctx.getVars()) : ctx.getVars();
        OperationContext subctx = ctx.getSubContext(isolate, ctx.getInput());

        // Running chain/operation for each list elements
        Collection<?> list = null;
        if (ctx.get(listName) instanceof Object[]) {
            list = Arrays.asList((Object[]) ctx.get(listName));
        } else if (ctx.get(listName) instanceof Collection<?>) {
            list = (Collection<?>) ctx.get(listName);
        } else {
            throw new UnsupportedOperationException(ctx.get(listName).getClass() + " is not a Collection");
        }
        for (Object value : list) {
            subctx.put(itemName, value);
            // Running chain/operation
            if (newTx) {
                service.runInNewTx(subctx, chainId, chainParameters, timeout, rollbackGlobalOnError);
            } else {
                service.run(subctx, chainId, chainParameters);
            }
        }

        // reconnect documents in the context
        if (!isolate) {
            for (String varName : vars.keySet()) {
                if (!ctx.getVars().containsKey(varName)) {
                    ctx.put(varName, vars.get(varName));
                } else {
                    Object value = vars.get(varName);
                    if (session != null && value != null && value instanceof DocumentModel) {
                        ctx.getVars().put(varName, session.getDocument(((DocumentModel) value).getRef()));
                    } else {
                        ctx.getVars().put(varName, value);
                    }
                }
            }
        }
    }
}
