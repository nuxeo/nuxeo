/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.execution.RunDocumentChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunFileChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnList;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnListInNewTransaction;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class OperationsAndTxTests {

    protected DocumentModel document;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        document = session.createDocumentModel("/", "src", "Folder");
        document.setPropertyValue("dc:title", "Source");
        document = session.createDocument(document);
        session.save();
        document = session.getDocument(document.getRef());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnArrayWithTx() throws Exception {

        OperationContext ctx = null;
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

            // Test with deprecated RunOperationOnListInNewTransaction.ID
            OperationChain chain = new OperationChain("testChain");
            chain.add(RunOperationOnListInNewTransaction.ID).set("list", "groups").set("id", "runOnListItemWithTx")
                    .set("isolate", "false");
            service.run(ctx, chain);
            List<String> result = (List<String>) ctx.remove("result");
            List<String> txids = (List<String>) ctx.remove("txids");
            List<String> sids = (List<String>) ctx.remove("sids");

            assertTrue(result.contains("tic"));
            assertTrue(result.contains("tac"));
            assertTrue(result.contains("toc"));
            assertFalse(txids.get(0).equals(txids.get(1)));
            assertTrue(sids.get(0).equals(sids.get(1)));
            assertTrue(sids.get(0).equals(sids.get(2)));

            // Same test with RunOperationOnList.ID
            chain = new OperationChain("testChain");
            chain.add(RunOperationOnList.ID).set("list", "groups").set("id", "runOnListItemWithTx")
                    .set("isolate", "false").set("newTx", "true");
            service.run(ctx, chain);
            result = (List<String>) ctx.remove("result");
            txids = (List<String>) ctx.remove("txids");
            sids = (List<String>) ctx.remove("sids");

            assertTrue(result.contains("tic"));
            assertTrue(result.contains("tac"));
            assertTrue(result.contains("toc"));
            assertFalse(txids.get(0).equals(txids.get(1)));
            assertTrue(sids.get(0).equals(sids.get(1)));
            assertTrue(sids.get(0).equals(sids.get(2)));

        } finally {
            service.removeOperation(RunOnListItemWithTx.class);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnDocumentWithTx() throws Exception {
        OperationContext ctx = null;
        service.putOperation(RunOnListItemWithTx.class);
        try {
            // storing in context which session and transaction id is
            // used in main process.
            ctx = new OperationContext(session);
            Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
            getOrCreateList(ctx, "sids").add(session.getSessionId());
            getOrCreateList(ctx, "txids").add(tx.toString());
            ctx.setInput(document);
            OperationChain chain = new OperationChain("testChain");
            chain.add(RunDocumentChain.ID).set("id", "runOnListItemWithTx").set("isolate", "false").set("newTx",
                    "true");
            DocumentModel result = (DocumentModel) service.run(ctx, chain);

            // Checking if new transaction id has been registered if same
            // session has been used.
            List<String> txids = (List<String>) ctx.get("txids");
            List<String> sids = (List<String>) ctx.get("sids");

            assertNotNull(result);
            assertFalse(txids.get(0).equals(txids.get(1)));
            assertTrue(sids.get(0).equals(sids.get(1)));
        } finally {
            service.removeOperation(RunOnListItemWithTx.class);
        }

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnBlobWithTx() throws Exception {

        OperationContext ctx = null;
        service.putOperation(RunOnListItemWithTx.class);
        try {
            // storing in context which session and transaction id is
            // used in main process.
            ctx = new OperationContext(session);
            Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
            getOrCreateList(ctx, "sids").add(session.getSessionId());
            getOrCreateList(ctx, "txids").add(tx.toString());
            Blob blob = Blobs.createBlob("blob");
            ctx.setInput(blob);
            OperationChain chain = new OperationChain("testChain");
            chain.add(RunFileChain.ID).set("id", "runOnListItemWithTx").set("isolate", "false").set("newTx", "true");
            Blob result = (Blob) service.run(ctx, chain);

            // Checking if new transaction id has been registered if same
            // session has been used.
            List<String> txids = (List<String>) ctx.get("txids");
            List<String> sids = (List<String>) ctx.get("sids");

            assertNotNull(result);
            assertFalse(txids.get(0).equals(txids.get(1)));
            assertTrue(sids.get(0).equals(sids.get(1)));
        } finally {
            service.removeOperation(RunOnListItemWithTx.class);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> getOrCreateList(OperationContext ctx, String name) {
        List<String> list = (List<String>) ctx.get(name);
        if (list == null) {
            list = new ArrayList<String>();
            ctx.put(name, list);
        }
        return list;
    }
}
