/*
 * (C) Copyright 2006-2022 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.execution.RunDocumentChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunFileChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnList;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnListInNewTransaction;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-operations.xml")
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class OperationsAndTxTests {

    @Inject
    protected AutomationService service;

    @Inject
    protected CoreSession session;

    protected OperationContext ctx;

    @Before
    public void before() throws Exception {
        service.putOperation(OperationFailure.class);
        service.putOperation(RunOnListItemWithTx.class);
        ctx = new OperationContext(session);
    }

    @After
    public void after() {
        ctx.close();
        service.removeOperation(RunOnListItemWithTx.class);
        service.removeOperation(OperationFailure.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnArrayWithTx() throws Exception {
        String input = "dummyInput";
        ctx.setInput(input);
        String[] groups = new String[3];
        groups[0] = "tic";
        groups[1] = "tac";
        groups[2] = "toc";
        ctx.put("groups", groups);

        // Test with deprecated RunOperationOnListInNewTransaction.ID
        OperationChain chain = new OperationChain("testChain");
        chain.add(RunOperationOnListInNewTransaction.ID)
             .set("list", "groups")
             .set("id", RunOnListItemWithTx.ID)
             .set("isolate", "false");
        service.run(ctx, chain);
        List<String> result = (List<String>) ctx.remove("result");
        List<String> txids = (List<String>) ctx.remove("txids");
        List<String> sids = (List<String>) ctx.remove("sids");

        assertTrue(result.contains("tic"));
        assertTrue(result.contains("tac"));
        assertTrue(result.contains("toc"));
        assertNotEquals(txids.get(0), txids.get(1));
        assertEquals(sids.get(0), sids.get(1));
        assertEquals(sids.get(0), sids.get(2));


        // Same test with RunOperationOnList.ID
        chain = new OperationChain("testChain");
        chain.add(RunOperationOnList.ID)
             .set("list", "groups")
             .set("id", RunOnListItemWithTx.ID)
             .set("isolate", "false")
             .set("newTx", "true");
        service.run(ctx, chain);
        result = (List<String>) ctx.remove("result");
        txids = (List<String>) ctx.remove("txids");
        sids = (List<String>) ctx.remove("sids");

        assertTrue(result.contains("tic"));
        assertTrue(result.contains("tac"));
        assertTrue(result.contains("toc"));
        assertNotEquals(txids.get(0), txids.get(1));
        assertEquals(sids.get(0), sids.get(1));
        assertEquals(sids.get(0), sids.get(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnDocumentWithTx() throws Exception {
        DocumentModel document = session.createDocumentModel("/", "src", "Folder");
        document.setPropertyValue("dc:title", "Source");
        document = session.createDocument(document);
        session.save();
        // storing in context which session and transaction id is used in main process.
        Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
        getOrCreateList(ctx, "txids").add(tx.toString());
        getOrCreateList(ctx, "sids").add(session.getSessionId());
        ctx.setInput(document);
        OperationChain chain = new OperationChain("testChain");
        chain.add(RunDocumentChain.ID).set("id", RunOnListItemWithTx.ID).set("isolate", "false").set("newTx", "true");
        DocumentModel result = (DocumentModel) service.run(ctx, chain);

        // Checking if new transaction id has been registered if same session has been used.
        List<String> txids = (List<String>) ctx.get("txids");
        List<String> sids = (List<String>) ctx.get("sids");

        assertNotNull(result);
        assertNotEquals(txids.get(0), txids.get(1));
        assertEquals(sids.get(0), sids.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnBlobWithTx() throws Exception {
        // storing in context which session and transaction id is used in main process.
        Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
        getOrCreateList(ctx, "txids").add(tx.toString());
        getOrCreateList(ctx, "sids").add(session.getSessionId());
        Blob blob = Blobs.createBlob("blob");
        ctx.setInput(blob);
        OperationChain chain = new OperationChain("testChain");
        chain.add(RunFileChain.ID).set("id", RunOnListItemWithTx.ID).set("isolate", "false").set("newTx", "true");
        Blob result = (Blob) service.run(ctx, chain);

        // Checking if new transaction id has been registered if same session has been used.
        List<String> txids = (List<String>) ctx.get("txids");
        List<String> sids = (List<String>) ctx.get("sids");

        assertNotNull(result);
        assertNotEquals(txids.get(0), txids.get(1));
        assertEquals(sids.get(0), sids.get(1));
    }

    // NXP-30897
    @Test
    public void testTransactionCanBeHandledByCaller() {
        ctx.handleTransaction(false);
        ctx.setInput(SimpleDocumentModel.empty());
        try {
            service.run(ctx, OperationFailure.ID);
            fail("Operation execution should throw exception.");
        } catch(OperationException e) {
            //expected
            assertTrue(e.getCause() instanceof NullPointerException);
            assertFalse(TransactionHelper.isTransactionMarkedRollback());
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> getOrCreateList(OperationContext ctx, String name) {
        return (List<String>) ctx.computeIfAbsent(name, k -> new ArrayList<>());
    }
}