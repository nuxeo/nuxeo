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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.automation.core.operations.execution;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @deprecated since 6.0. Use instead {@link RunOperationOnList} with ID 'Context.RunOperationOnList'. Run an embedded
 *             operation chain inside separated transactions using the current input. The output is undefined (Void)
 * @since 5.7.2
 */
@Deprecated
@Operation(id = RunOperationOnListInNewTransaction.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run For Each in new TX", description = "Run an operation/chain in a new Transaction for each element from the list defined by the 'list' parameter. The 'list' parameter is pointing to a context variable that represents the list which will be iterated. The 'itemName' parameter represents the name of the context variable which will point to the current element in the list at each iteration. You can use the 'isolate' parameter to specify whether or not the evalution context is the same as the parent context or a copy of it. If the 'isolate' parameter is 'true' then a copy of the current context is used and so that modifications in this context will not affect the parent context. Any input is accepted. The input is returned back as output when operation terminates.", deprecatedSince = "6.0", aliases = { "Context.RunOperationOnListInNewTx" })
public class RunOperationOnListInNewTransaction {

    protected static Log log = LogFactory.getLog(RunOperationOnListInNewTransaction.class);

    public static final String ID = "RunOperationOnListInNewTx";

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

    @Param(name = "itemName", required = false, values = "item")
    protected String itemName = "item";

    @Param(name = "isolate", required = false, values = "true")
    protected boolean isolate = true;

    @OperationMethod
    public void run() throws OperationException {
        Map<String, Object> vars = isolate ? new HashMap<String, Object>(ctx.getVars()) : ctx.getVars();

        Collection<?> list = null;
        if (ctx.get(listName) instanceof Object[]) {
            list = Arrays.asList((Object[]) ctx.get(listName));
        } else if (ctx.get(listName) instanceof Collection<?>) {
            list = (Collection<?>) ctx.get(listName);
        } else {
            throw new UnsupportedOperationException(ctx.get(listName).getClass() + " is not a Collection");
        }

        // commit the current transaction
        TransactionHelper.commitOrRollbackTransaction();

        // execute on list in separate transactions
        for (Object value : list) {
            TransactionHelper.startTransaction();
            boolean completedAbruptly = true;
            try {
                OperationContext subctx = new OperationContext(session, vars);
                subctx.setInput(ctx.getInput());
                subctx.put(itemName, value);
                service.run(subctx, chainId);
                completedAbruptly = false;
            } finally {
                if (completedAbruptly) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                TransactionHelper.commitOrRollbackTransaction();
            }
        }

        TransactionHelper.startTransaction();

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
