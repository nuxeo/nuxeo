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
 *    Mariana Cedica
 *    Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.operations.execution;

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
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @deprecated since 6.0. Use instead {@link RunDocumentChain} with ID 'Context.RunDocumentOperation'. Operation to run
 *             an operation chain in a separate transaction. The existing transaction is committed before running the
 *             new transaction.
 * @since 5.6
 */
@Deprecated
@Operation(id = RunInNewTransaction.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Run Document Chain in new Tx", description = "Run an operation chain in a separate tx. The 'parameters' injected are accessible in the subcontext ChainParameters. For instance, @{ChainParameters['parameterKey']}.", deprecatedSince = "6.0")
public class RunInNewTransaction {

    public static final String ID = "Context.RunDocumentOperationInNewTx";

    private static final Log log = LogFactory.getLog(RunInNewTransaction.class);

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService service;

    @Context
    protected CoreSession session;

    @Param(name = "id")
    protected String chainId;

    @Param(name = "isolate", required = false, values = "false")
    protected boolean isolate = false;

    @Param(name = "rollbackGlobalOnError", required = false, values = "false")
    protected boolean rollbackGlobalOnError = false;

    @Param(name = "parameters", description = "Accessible in the subcontext ChainParameters. For instance, @{ChainParameters['parameterKey']}.", required = false)
    protected Properties chainParameters = new Properties();

    @Param(name = "timeout", required = false)
    protected Integer timeout = Integer.valueOf(60);

    @OperationMethod
    public void run() throws OperationException {
        // if the current transaction was already marked for rollback, do nothing
        if (TransactionHelper.isTransactionMarkedRollback()) {
            return;
        }
        try {
            TransactionHelper.runInNewTransaction(() -> {
                try (OperationContext subctx = ctx.getSubContext(isolate)) {
                    try {
                        service.run(subctx, chainId, chainParameters);
                    } catch (OperationException e) {
                        if (rollbackGlobalOnError) {
                            throw new NuxeoException(e);
                        } else {
                            // just log, no rethrow
                            log.error("Error while executing operation " + chainId, e);
                        }
                    }
                } catch (OperationException e) {
                    throw new NuxeoException(e);
                }
            });
        } catch (NuxeoException e) {
            if (e.getCause() instanceof OperationException) {
                throw (OperationException) e.getCause();
            }
            throw e;
        }
    }

}
