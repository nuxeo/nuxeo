/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnListInNewTransaction;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@TransactionalConfig(autoStart = false)
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init=DefaultRepositoryInit.class)
public class OperationsAndTxTests {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Test
    public void testRunOperationOnArrayWithTx() throws Exception {

        TransactionHelper.startTransaction();
        OperationContext ctx = null;
        try {
            service.putOperation(RunOnListItemWithTx.class);
            try {
                ctx = new OperationContext(session);
                String input = "dummyInput";
                ctx.setInput(input);
                String[] groups = new String[3];
                groups[0] = "tic";
                groups[1] = "tac";
                groups[2] = "toc";
                ctx.put("groups", groups);
                OperationChain chain = new OperationChain("testChain");
                chain.add(RunOperationOnListInNewTransaction.ID)
                        .set("list", "groups").set("id", "runOnListItemWithTx")
                        .set("isolate", "false");
                service.run(ctx, chain);
                List<String> result = (List<String>) ctx.get("result");
                List<String> txids = (List<String>) ctx.get("txids");
                List<String> sids = (List<String>) ctx.get("sids");
                List<String> sqlsids = (List<String>) ctx.get("sqlsids");

                assertTrue(result.contains("tic"));
                assertTrue(result.contains("tac"));
                assertTrue(result.contains("toc"));
                assertFalse(txids.get(0).equals(txids.get(1)));
                assertTrue(sids.get(0).equals(sids.get(1)));
                assertTrue(sids.get(2).equals(sids.get(1)));
                assertFalse(sqlsids.get(0).equals(sqlsids.get(1)) && sqlsids.get(2).equals(sqlsids.get(1)));

            } finally {
                service.removeOperation(RunOnListItemWithTx.class);
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }
}
