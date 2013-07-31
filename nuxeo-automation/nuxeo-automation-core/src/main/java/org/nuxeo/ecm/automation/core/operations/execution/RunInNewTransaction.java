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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Operation to run an operation chain in a separate transaction. The
 * rollbackGlobalOnError allows to configure the rollback policy -> if true then
 * the new transaction will be rollbacked and the main transaction will be set
 * for rollback too
 *
 * @since 5.6
 */
@Operation(id = RunInNewTransaction.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run Document Chain in new Tx", description = "Run an operation chain in a separate tx. The 'parameters' injected are accessible in the subcontext ChainParameters. For instance, @{ChainParameters['parameterKey']}.")
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

    @Param(name = "parameters", required = false)
    protected Properties chainParameters;

    @Param(name = "timeout", required = false)
    protected Integer timeout = 60;

    @OperationMethod
    public void run() throws Exception {
        // if the transaction was already marked for rollback, do nothing
        if (TransactionHelper.isTransactionMarkedRollback()) {
            return;
        }

        Map<String, Object> vars = isolate ? new HashMap<String, Object>(
                ctx.getVars()) : ctx.getVars();

        RunnableOperation runOp = new RunnableOperation(
                ctx.getCoreSession().getRepositoryName(), chainId, vars);
        boolean failed = false;
        try {
            runOp.start();
            runOp.join((timeout + 1) * 1000);
            if (runOp.isAlive()) {
                failed = true;
            }
        } finally {
            if ((failed || runOp.isFailed()) && rollbackGlobalOnError) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        }
        // flush invalidations
        ctx.getCoreSession().save();
    }

    protected class RunnableOperation extends Thread {

        protected final Map<String, Object> vars;

        protected final String repo;

        protected final String opName;

        protected boolean failed = false;

        public RunnableOperation(String repo, String opName,
                Map<String, Object> vars) {
            super("Runner-for-" + opName);
            this.vars = vars;
            this.repo = repo;
            this.opName = opName;
        }

        @Override
        public void run() {
            TransactionHelper.startTransaction(timeout);
            try {
                new UnrestrictedSessionRunner(repo) {
                    @Override
                    public void run() throws ClientException {
                        OperationContext subctx = new OperationContext(session,
                                vars);
                        subctx.setInput(ctx.getInput());
                        try {
                            service.run(subctx, opName, (Map) chainParameters);
                        } catch (Exception e) {
                            throw ClientException.wrap(e);
                        }
                    }
                }.runUnrestricted();
            } catch (ClientException e) {
                TransactionHelper.setTransactionRollbackOnly();
                failed = true;
                log.error("Error while executing operation " + opName, e);
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }

        public boolean isFailed() {
            return failed;
        }

    }
}
