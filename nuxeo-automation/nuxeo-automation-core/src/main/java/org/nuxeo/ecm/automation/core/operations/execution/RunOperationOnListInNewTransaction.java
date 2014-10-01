/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @deprecated since 5.9.6. Use instead {@link RunOperationOnList} with ID
 * 'Context.RunOperationOnList'.
 *
 * Run an embedded operation chain inside separated transactions using the
 * current input. The output is undefined (Void)
 * @since 5.7.2
 */
@Operation(id = RunOperationOnListInNewTransaction.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run For Each in new TX", description = "Run an operation/chain in a new Transaction for each element from the list defined by the 'list' paramter. The 'list' parameter is pointing to context variable that represent the list which will be iterated. The 'itemName' parameter represent the name of the context varible which will point to the current element in the list at each iteration. You can use the 'isolate' parameter to specify whether or not the evalution context is the same as the parent context or a copy of it. If the isolate is 'true' then a copy of the current contetx is used and so that modifications in this context will not affect the parent context. Any input is accepted. The input is returned back as output when operation terminate.", deprecatedSince = "5.9.6")
public class RunOperationOnListInNewTransaction {

    protected static Log log = LogFactory.getLog(RunOperationOnListInNewTransaction.class);

    public static final String ID = "Context.RunOperationOnListInNewTx";

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
    @SuppressWarnings("unchecked")
    public void run() throws Exception {
        Map<String, Object> vars = isolate ? new HashMap<String, Object>(
                ctx.getVars()) : ctx.getVars();

        Collection<?> list = null;
        if (ctx.get(listName) instanceof Object[]) {
            list = Arrays.asList((Object[]) ctx.get(listName));
        } else if (ctx.get(listName) instanceof Collection<?>) {
            list = (Collection<?>) ctx.get(listName);
        } else {
            throw new UnsupportedOperationException(
                    ctx.get(listName).getClass() + " is not a Collection");
        }

        // commit the current transaction
        TransactionHelper.commitOrRollbackTransaction();

        // execute on list in separate transactions
        for (Object value : list) {
            TransactionHelper.startTransaction();
            try {
                OperationContext subctx = new OperationContext(session, vars);
                subctx.setInput(ctx.getInput());
                subctx.put(itemName, value);
                service.run(subctx, chainId, null);
            } catch (Exception e) { // no InterruptedException
                log.error("Cannot proceed on " + value, e);
                TransactionHelper.setTransactionRollbackOnly();
            } finally {
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
                    if (value != null && value instanceof DocumentModel) {
                        ctx.getVars().put(
                                varName,
                                session.getDocument(((DocumentModel) value).getRef()));
                    } else {
                        ctx.getVars().put(varName, value);
                    }
                }
            }
        }
    }

}
