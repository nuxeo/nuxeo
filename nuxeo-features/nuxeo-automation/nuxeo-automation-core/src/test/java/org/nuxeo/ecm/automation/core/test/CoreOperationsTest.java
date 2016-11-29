/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.core.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.impl.adapters.StringToProperties;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentInput;
import org.nuxeo.ecm.automation.core.operations.RunScript;
import org.nuxeo.ecm.automation.core.operations.RunScriptFile;
import org.nuxeo.ecm.automation.core.operations.SetVar;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.CheckInDocument;
import org.nuxeo.ecm.automation.core.operations.document.CopyDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateVersion;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChildren;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentParent;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.MoveDocument;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentLifeCycle;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentProperty;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.execution.RunDocumentChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunInNewTransaction;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnList;
import org.nuxeo.ecm.automation.core.operations.stack.PopDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PushDocument;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.MvelExpression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
// For version label info
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class CoreOperationsTest {

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        dst = session.createDocumentModel("/", "dst", "Workspace");
        dst.setPropertyValue("dc:title", "Destination");
        dst = session.createDocument(dst);
        session.save();
        dst = session.getDocument(dst.getRef());
    }

    // ------ Tests comes here --------

    @Test
    public void testScriptOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(RunScript.ID).set("script", "Context[\"script_title\"] = This.title;");

        service.run(ctx, chain);
        assertEquals(src.getTitle(), ctx.get("script_title"));
    }

    @Test
    public void testRunScriptOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(RunScript.ID).set("script", "This.setPropertyValue(\"dc:title\",\"modified from mvel\");");

        service.run(ctx, chain);
        String title = src.getProperty("dc:title").getValue(String.class);
        assertThat(title, is("modified from mvel"));
    }

    @Test
    public void testRunScriptWithCondition() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(RunScript.ID).set("script",
                "if (This.id != null &amp;&amp; This.id != '') {This.setPropertyValue(\"dc:title\",\"modified from mvel\");}");
        service.run(ctx, chain);
        String title = src.getProperty("dc:title").getValue(String.class);
        assertThat(title, is("modified from mvel"));
    }

    /*
     * This test is not enabled for now since the operation is disabled until fully implemented Enable this test when
     * the operation will be enabled.
     */
    @Ignore
    @Test
    public void testScriptFileOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(RunScriptFile.ID).set("script", CoreOperationsTest.class.getResource("/test-script.mvel"));

        String oldTitle = src.getTitle();
        service.run(ctx, chain);
        assertEquals(oldTitle, ctx.get("script_title"));
        assertEquals("modified title", src.getPropertyValue("dc:title"));
    }

    /**
     * Create | Copy | Set Property.
     * <p>
     * This is also testing {@link StringToProperties} adapter
     *
     * @throws Exception
     */
    @Test
    public void testChain1() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties", "dc:title=MyDoc");
        chain.add(CopyDocument.ID).set("target", dst).set("name", "note_copy");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "mydesc");

        DocumentModel out = (DocumentModel) service.run(ctx, chain);
        DocumentModel doc = session.getDocument(new PathRef("/dst/note_copy"));
        assertEquals(out.getId(), doc.getId());
        assertEquals("mydesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc", out.getPropertyValue("dc:title"));

        doc = session.getDocument(new PathRef("/src/note"));
        assertEquals("MyDoc", doc.getPropertyValue("dc:title"));
    }

    /**
     * Same as before but tests relative paths Create | Copy | Set Property.
     * <p>
     * This is also testing {@link StringToProperties} adapter
     *
     * @throws Exception
     */
    @Test
    public void testChain1WithRelativePath() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        DocumentModel out = (DocumentModel) service.run(ctx, "core_chain1");
        DocumentModel doc = session.getDocument(new PathRef("/dst/note_copy"));
        assertEquals(out.getId(), doc.getId());
        assertEquals("mydesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc", out.getPropertyValue("dc:title"));

        doc = session.getDocument(new PathRef("/src/note"));
        assertEquals("MyDoc", doc.getPropertyValue("dc:title"));
    }

    /**
     * Same as before Create | Copy | Set Property But also test properties specified using a mvel Expression
     *
     * @throws Exception
     */
    @Test
    public void testMvelExpressionProperties() throws Exception {
        src.setPropertyValue("dc:description", "dc:title=MyDoc");
        session.saveDocument(src);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties",
                new MvelExpression("This.getPropertyValue(\"dc:description\")"));
        chain.add(CopyDocument.ID).set("target", dst).set("name", "note_copy");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "mydesc");

        DocumentModel out = (DocumentModel) service.run(ctx, chain);
        DocumentModel doc = session.getDocument(new PathRef("/dst/note_copy"));
        assertEquals(out.getId(), doc.getId());
        assertEquals("mydesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc", out.getPropertyValue("dc:title"));

        doc = session.getDocument(new PathRef("/src/note"));
        assertEquals("MyDoc", doc.getPropertyValue("dc:title"));
    }

    /**
     * Same as before but use DocumentWrapper to access properties
     *
     * @throws Exception
     */
    @Test
    public void testMvelExpressionProperties2() throws Exception {
        src.setPropertyValue("dc:description", "dc:title=MyDoc");
        session.saveDocument(src);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties",
                new MvelExpression("Document[\"dc:description\"]"));
        chain.add(CopyDocument.ID).set("target", dst).set("name", "note_copy");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "mydesc");

        DocumentModel out = (DocumentModel) service.run(ctx, chain);
        DocumentModel doc = session.getDocument(new PathRef("/dst/note_copy"));
        assertEquals(out.getId(), doc.getId());
        assertEquals("mydesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc", out.getPropertyValue("dc:title"));

        doc = session.getDocument(new PathRef("/src/note"));
        assertEquals("MyDoc", doc.getPropertyValue("dc:title"));
    }

    /**
     * Create | Move | Set Property.
     */
    @Test
    public void testChain2() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties",
                new Properties("dc:title=MyDoc"));
        chain.add(MoveDocument.ID).set("target", dst).set("name", "note_copy");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "mydesc");

        DocumentModel out = (DocumentModel) service.run(ctx, chain);
        DocumentModel doc = session.getDocument(new PathRef("/dst/note_copy"));
        assertEquals(out.getId(), doc.getId());
        assertEquals("mydesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc", out.getPropertyValue("dc:title"));
        try {
            doc = session.getDocument(new PathRef("/src/note"));
            fail("Document /src/note is not supposed to exists");
        } catch (Exception e) {
            // test ok
        }
    }

    /**
     * Create | GetParent | Update Parent | Save | Pop | Lock.
     */
    @Test
    public void testChain3() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("properties", new Properties("dc:title=MyDoc")).set(
                "name", "note");
        chain.add(PushDocument.ID);
        chain.add(GetDocumentParent.ID);
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "parentdoc");
        chain.add(SaveDocument.ID);
        chain.add(PopDocument.ID);
        chain.add(UpdateDocument.ID).set("properties", new Properties("dc:title=MyDoc2\ndc:description=mydesc"));
        chain.add(LockDocument.ID);
        chain.add(SaveDocument.ID);

        assertNull(src.getPropertyValue("dc:description"));
        DocumentModel out = (DocumentModel) service.run(ctx, chain);
        assertEquals("mydesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc2", out.getPropertyValue("dc:title"));
        assertTrue(out.isLocked());
        assertEquals("parentdoc", session.getDocument(src.getRef()).getPropertyValue("dc:description"));
    }

    /**
     * Context Fetch | Create | GetParent | Create. Context Fetch | GetChildren | Delete.
     */
    @Test
    public void testChain4() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note1").set("properties",
                new Properties("dc:title=MyDoc1"));
        chain.add(GetDocumentParent.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note2").set("properties",
                new Properties("dc:title=MyDoc2"));
        service.run(ctx, chain);
        assertEquals(2, session.getChildren(src.getRef()).size());

        ctx = new OperationContext(session);
        ctx.setInput(src);
        chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(GetDocumentChildren.ID);
        chain.add(DeleteDocument.ID);
        service.run(ctx, chain);

        assertEquals(0, session.getChildren(src.getRef()).size());
    }

    /**
     * Context Fetch | Create | GetParent | Create. Context Fetch | GetChildren | Delete.
     */
    @Test
    public void testBlobChain() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        Blob blob = Blobs.createBlob("blob content");
        blob.setFilename("attachment");

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file2");
        chain.add(SetVar.ID).set("name", "file2").set("value", Scripting.newExpression("This"));
        chain.add(GetDocumentParent.ID);
        chain.add(CreateDocument.ID).set("type", "File").set("name", "file1");
        chain.add(SetDocumentBlob.ID).set("xpath", "file:content").set("file", blob);
        chain.add(GetDocumentBlob.ID).set("xpath", "file:content");
        chain.add(AttachBlob.ID).set("xpath", "file:content").set("document", Scripting.newExpression("file2"));

        service.run(ctx, chain);
        session.save();

        blob = (Blob) session.getDocument(new PathRef("/src/file1")).getPropertyValue("file:content");
        assertEquals("blob content", blob.getString());

        blob = (Blob) session.getDocument(new PathRef("/src/file2")).getPropertyValue("file:content");
        assertEquals("blob content", blob.getString());
    }

    /**
     * Test a chain running a sub-chain.
     */
    @Test
    public void testSubChain() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(RunDocumentChain.ID).set("id", "doc_subchain");
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertEquals("My Doc", doc.getTitle());
        assertEquals("My Doc desc", doc.getPropertyValue("dc:description"));
    }

    /**
     * Alternate version - use xml properties instead of java properties when defining a properties value
     *
     * @throws Exception
     */
    @Test
    public void testSubChainAlt() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(RunDocumentChain.ID).set("id", "doc_subchain_alt");
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertEquals("My Doc", doc.getTitle());
        assertEquals("My Doc desc", doc.getPropertyValue("dc:description"));
    }

    /**
     * Test restore doc.
     */
    @Test
    public void testRestore() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(SetVar.ID).set("name", "mydoc").set("value", new PathRef("/dst"));
        chain.add(RestoreDocumentInput.ID).set("name", "mydoc");
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertEquals(dst, doc);
    }

    /**
     * Test date expressions.
     */
    @Test
    public void testDate() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:title").set("value",
                Scripting.newTemplate("Now is @{CurrentDate.months(-2)}"));
        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertTrue(doc.getTitle().startsWith("Now is TIMESTAMP"));
    }

    /**
     * Create a document and copy it by giving the destination path as a template expression.
     */
    @Test
    public void testStringToDocAdapters() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(SetVar.ID).set("name", "st").set("value", "st");
        chain.add(SetVar.ID).set("name", "pathVar").set("value", Scripting.newTemplate("/d@{st}"));
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties", "dc:title=MyDoc");
        chain.add(CopyDocument.ID).set("target", Scripting.newExpression("pathVar")).set("name", "note_copy");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", "mydesc");

        DocumentModel out = (DocumentModel) service.run(ctx, chain);

        DocumentModel doc = session.getDocument(new PathRef("/dst/note_copy"));
        assertEquals(out.getId(), doc.getId());
        assertEquals("mydesc", out.getPropertyValue("dc:description"));
        assertEquals("MyDoc", out.getPropertyValue("dc:title"));

        doc = session.getDocument(new PathRef("/src/note"));
        assertEquals("MyDoc", doc.getPropertyValue("dc:title"));
    }

    @Test
    public void testCreateVersion() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        Expression expr = Scripting.newExpression("Document.versionLabel");
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties", "dc:title=MyDoc");
        chain.add(SetVar.ID).set("name", "versionLabel_1").set("value", expr);
        chain.add(CreateVersion.ID).set("increment", "Major");
        chain.add(SetVar.ID).set("name", "versionLabel_2").set("value", expr);
        chain.add(UpdateDocument.ID).set("properties", "dc:title=MyDoc3");
        chain.add(CreateVersion.ID).set("increment", "Minor");
        chain.add(SetVar.ID).set("name", "versionLabel_3").set("value", expr);
        // update document to test if version change (auto-checkout)
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:title").set("value", "MyDoc4");

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals("0.0", ctx.get("versionLabel_1"));
        assertEquals("1.0", ctx.get("versionLabel_2"));
        assertEquals("1.1", ctx.get("versionLabel_3"));
        assertEquals("1.1+", doc.getVersionLabel());
        assertEquals("MyDoc4", doc.getTitle());
    }

    @Test
    public void testCreateVersion2() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        Expression expr = Scripting.newExpression("Document.versionLabel");
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties", "dc:title=MyDoc");
        chain.add(SetVar.ID).set("name", "versionLabel_1").set("value", expr);
        // update document to test if version change (it should not change)
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:title").set("value", "MyDoc2");

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals("0.0", ctx.get("versionLabel_1"));
        assertEquals("0.0", doc.getVersionLabel());
        assertEquals("MyDoc2", doc.getTitle());
    }

    @Test
    public void testCreateVersion3() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        Expression expr = Scripting.newExpression("Document.versionLabel");
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set("properties", "dc:title=MyDoc");
        chain.add(SetVar.ID).set("name", "versionLabel_1").set("value", expr);
        chain.add(SetDocumentLifeCycle.ID).set("value", "approve");
        chain.add(CheckInDocument.ID).set("version", "major").set("comment", "yo").set("versionVarName", "ver");
        chain.add(SetVar.ID).set("name", "versionLabel_2").set("value", expr);
        // update document to test if version change (it should not change)
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:title").set("value", "MyDoc2");
        chain.add(SetVar.ID).set("name", "versionLabel_3").set("value", expr);
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:title").set("value", "MyDoc3");

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        assertEquals("0.0", ctx.get("versionLabel_1"));
        assertEquals("1.0", ctx.get("versionLabel_2"));
        assertEquals("1.0+", ctx.get("versionLabel_3"));
        assertEquals("1.0+", doc.getVersionLabel());
        assertEquals("MyDoc3", doc.getTitle());
        DocumentRef ver = (DocumentRef) ctx.get("ver");
        assertNotNull(ver);
    }

    @Test
    public void testRunOperatioOnList() throws Exception {
        try {
            service.putOperation(RunOnListItem.class);
            OperationContext ctx = new OperationContext(session);
            String input = "dummyInput";
            ctx.setInput(input);
            ArrayList<String> users = new ArrayList<String>();
            users.add("foo");
            users.add("bar");
            ctx.put("users", users);
            OperationChain chain = new OperationChain("testChain");
            chain.add(RunOperationOnList.ID).set("list", "users").set("id", "runOnList").set("isolate", "false");
            service.run(ctx, chain);
            String result = (String) ctx.get("result");
            assertEquals("foo, bar", result);
        } finally {
            service.removeOperation(RunOnListItem.class);
        }
    }

    @Test
    public void testRunOperationOnArray() throws Exception {
        try {
            service.putOperation(RunOnListItem.class);
            OperationContext ctx = new OperationContext(session);
            String input = "dummyInput";
            ctx.setInput(input);
            String[] groups = new String[2];
            groups[0] = "tic";
            groups[1] = "tac";
            ctx.put("groups", groups);
            OperationChain chain = new OperationChain("testChain");
            chain.add(RunOperationOnList.ID).set("list", "groups").set("id", "runOnList").set("isolate", "false");
            service.run(ctx, chain);
            String result = (String) ctx.get("result");
            assertEquals("tic, tac", result);
        } finally {
            service.removeOperation(RunOnListItem.class);
        }
    }

    @Test
    public void testRunInNewTxOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);

        // test that the global transaction is not marked for rollback
        try {
            OperationChain chain = new OperationChain("testChain");
            chain.add(RunInNewTransaction.ID).set("id", "testExitChain").set("isolate", "false").set(
                    "rollbackGlobalOnError", "false");
            service.run(ctx, chain);
        } finally {
            assertFalse(TransactionHelper.isTransactionMarkedRollback());
        }

        // test that the global transaction is marked for rollback
        try {
            OperationChain chain = new OperationChain("testChain");
            chain.add(RunInNewTransaction.ID).set("id", "testExitChain").set("isolate", "false")
                    .set("rollbackGlobalOnError", "true");
            service.run(ctx, chain);
        } catch (Exception e) {
            assertTrue(TransactionHelper.isTransactionMarkedRollback());
        }
        // needed for session cleanup
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    // non regression test for NXP-13748
    public void testSetPropertyNullValue() throws Exception {
        src.setPropertyValue("dc:description", "foo");
        src.setPropertyValue("dc:format", "bar");
        src.setPropertyValue("dc:language", "baz");
        session.saveDocument(src);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        // check values before
        assertEquals("Source", src.getPropertyValue("dc:title"));
        assertEquals("foo", src.getPropertyValue("dc:description"));
        assertEquals("bar", src.getPropertyValue("dc:format"));
        assertEquals("baz", src.getPropertyValue("dc:language"));

        // run the chain
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set("value", null);
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:format").set("value", "expr:empty");
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:language").set("value", "expr:null");
        DocumentModel out = (DocumentModel) service.run(ctx, chain);

        // check values after
        assertEquals("Source", out.getPropertyValue("dc:title"));
        assertEquals(null, out.getPropertyValue("dc:description"));
        assertEquals("", out.getPropertyValue("dc:format"));
        assertEquals(null, out.getPropertyValue("dc:language"));
    }

}
