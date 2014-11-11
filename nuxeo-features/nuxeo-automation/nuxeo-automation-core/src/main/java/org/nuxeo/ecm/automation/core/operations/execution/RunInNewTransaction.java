/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mariana Cedica
 */
package org.nuxeo.ecm.automation.core.operations.execution;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * Operation to run an operation chain in a separate transaction. The
 * rollbackGlobalOnError allows to configure the rollback policy -> if true then
 * the new transaction will be rollbacked and the main transaction will be set
 * for rollback too
 *
 * @since 5.6
 */
@Operation(id = RunInNewTransaction.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run Document Chain in new Tx", description = "Run an operation chain in a separate tx.")
public class RunInNewTransaction {

    public static final String ID = "Context.RunDocumentOperationInNewTx";

    private static final Log log = LogFactory.getLog(RunInNewTransaction.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name = "isolate", required = false, values = "false")
    protected boolean isolate = false;

    @Param(name = "rollbackGlobalOnError", required = false, values = "false")
    protected boolean rollbackGlobalOnError = false;

    @OperationMethod
    public void run() throws Exception {
        // if the transaction was already marked for rollback, do nothing
        if (TransactionHelper.isTransactionMarkedRollback()) {
            return;
        }

        Map<String, Object> vars = isolate ? new HashMap<String, Object>(
                ctx.getVars()) : ctx.getVars();
        OperationContext subctx = new OperationContext(ctx.getCoreSession(),
                vars);
        subctx.setInput(ctx.getInput());
        final TransactionManager transactionManager = TransactionHelper.lookupTransactionManager();
        final Transaction globalTx = transactionManager.suspend();
        try {
            TransactionHelper.startTransaction();
            try {
                service.run(subctx, chainId);
            } catch (Exception e) {
                handleRollbackOnlyOnGlobal(globalTx, e);
            } finally {
                try {
                    TransactionHelper.commitOrRollbackTransaction();
                } catch (TransactionRuntimeException e) {
                    handleRollbackOnlyOnGlobal(globalTx, e);
                }
            }
        } finally {
            transactionManager.resume(globalTx);
        }
    }

    private void handleRollbackOnlyOnGlobal(Transaction mainTx, Throwable e)
            throws SystemException, OperationException {
        TransactionHelper.setTransactionRollbackOnly();
        if (rollbackGlobalOnError == true) {
            mainTx.setRollbackOnly();
            throw new OperationException(
                    "Catching error on new transaction, rollbacking global tx",
                    e);
        } else {
            log.error("Caught error on new transaction, continuing global tx",
                    e);
        }
    }
}
