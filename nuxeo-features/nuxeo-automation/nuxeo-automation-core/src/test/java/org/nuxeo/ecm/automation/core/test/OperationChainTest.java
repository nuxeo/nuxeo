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

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.SetVar;
import org.nuxeo.ecm.automation.core.scripting.MvelExpression;
import org.nuxeo.ecm.automation.core.scripting.MvelTemplate;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
public class OperationChainTest {

    protected DocumentModel src;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        src = session.createDocumentModel("/", "src", "Folder");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());
    }

    @After
    public void clearRepo() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        session.removeChildren(session.getRootDocument().getRef());
    }

    protected void assertContextOk(OperationContext ctx, String chain, String message, String title) {
        assertEquals(chain, ctx.get("chain"));
        assertEquals(message, ctx.get("message"));
        assertEquals(title, ctx.get("title"));
    }

    // ------ Tests comes here --------

    /**
     * <pre>
     * Input: doc
     * Test chain: O1 -&gt; O2 -&gt; O1
     * O1:doc:doc | O2:doc:ref* | O1:doc:doc
     * O1:ref:doc | O2:doc:doc  | O1:ref:doc
     * where * means the method has priority over the other with the same input.
     * Expected output: (parameters in context)
     * chain : &quot;O1:doc:doc,O2:doc:ref,O1:ref:doc&quot;
     * message : &quot;Hello 1!,Hello 2!,Hello 3!&quot;
     * title : &quot;Source,Source,Source&quot;
     * </pre>
     * <p>
     * This is testing a chain having multiple choices and one choice having a higher priority.
     */
    @Test
    public void testChain1() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add("o1").set("message", "Hello 1!");
        chain.add("o2").set("message", "Hello 2!");
        chain.add("o1").set("message", "Hello 3!");

        service.run(ctx, chain);

        assertContextOk(ctx, "O1:doc:doc,O2:doc:ref,O1:ref:doc", "Hello 1!,Hello 2!,Hello 3!", "Source,Source,Source");
    }

    /**
     * Same as before but use a managed chain
     *
     * @throws Exception
     */
    @Test
    public void testManagedChain1() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        service.run(ctx, "mychain");

        assertContextOk(ctx, "O1:doc:doc,O2:doc:ref,O1:ref:doc", "Hello 1!,Hello 2!,Hello 3!", "Source,Source,Source");
    }

    /**
     * Test compiled chain
     *
     * @throws Exception
     */
    @Test
    public void testManagedChain2() throws Exception {
        testManagedChain1();
    }

    /**
     * <pre>
     * Input: ref
     * Test chain: O1 -&gt; O2 -&gt; O1
     * O1:doc:doc | O2:doc:ref* | O1:doc:doc
     * O1:ref:doc | O2:doc:doc  | O1:ref:doc
     * where * means the method has priority over the other with the same input.
     * Expected output: (parameters in context)
     * chain : &quot;O1:ref:doc,O2:doc:ref,O1:ref:doc&quot;
     * message : &quot;Hello 1!,Hello 2!,Hello 3!&quot;
     * title : &quot;Source,Source,Source&quot;
     * </pre>
     * <p>
     * This test is using the same chain as in the previous test but changes the input to DocumentRef. This is also
     * testing matching on derived classes (since the IdRef used is a subclass of DocumentRef)
     */
    @Test
    public void testChain2() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src.getRef());

        OperationChain chain = new OperationChain("testChain");
        chain.add("o1").set("message", "Hello 1!");
        chain.add("o2").set("message", "Hello 2!");
        chain.add("o1").set("message", "Hello 3!");

        service.run(ctx, chain);

        assertContextOk(ctx, "O1:ref:doc,O2:doc:ref,O1:ref:doc", "Hello 1!,Hello 2!,Hello 3!", "Source,Source,Source");
    }

    /**
     * <pre>
     * Input: doc
     * Test chain: O1 -&gt; O3 -&gt; O3
     * O1:doc:doc | O3:doc:ref  | O3:doc:ref
     * O1:ref:doc | O3:doc:doc  | O3:doc:doc
     * Expected output: (parameters in context)
     * chain : &quot;O1:doc:doc,O3:doc:doc,O3:doc:doc&quot;
     * message : &quot;Hello 1!,Hello 2!,Hello 3!&quot;
     * title : &quot;Source,Source,Source&quot;
     * </pre>
     * <p>
     * This is testing a chain having multiple choices. You can see that the second operation in chain (O3) provides 2
     * ways of processing a 'doc'. But the 'O3:doc:doc' way will be selected since the other way (e.g. O3:doc:ref)
     * cannot generate a complete chain path.
     */
    @Test
    public void testChain3() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add("o1").set("message", "Hello 1!");
        chain.add("o3").set("message", "Hello 2!");
        chain.add("o3").set("message", "Hello 3!");

        service.run(ctx, chain);

        assertContextOk(ctx, "O1:doc:doc,O3:doc:doc,O3:doc:doc", "Hello 1!,Hello 2!,Hello 3!", "Source,Source,Source");
    }

    /**
     * Same as before but with a ctrl operation between o3 and o3
     *
     * @throws Exception
     */
    @Test
    public void testChain3WithCtrl() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add("o1").set("message", "Hello 1!");
        chain.add("o3").set("message", "Hello 2!");
        chain.add("ctrl").set("message", "Control!");
        chain.add("o3").set("message", "Hello 3!");

        service.run(ctx, chain);

        assertContextOk(ctx, "O1:doc:doc,O3:doc:doc,ctrl:void:void,O3:doc:doc", "Hello 1!,Hello 2!,Control!,Hello 3!",
                "Source,Source,Source,Source");
    }

    /**
     * This is testing the parameter expressions. If you set an operation parameter that point to 'var:principal' it
     * will return
     *
     * @throws Exception
     */
    @Test
    public void testExpressionParams() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        SimplePrincipal principal = new SimplePrincipal("Hello from Context!");
        ctx.put("messageHolder", principal);
        OperationChain chain = new OperationChain("testChain");
        chain.add("o1").set("message", new MvelExpression("Context[\"messageHolder\"].name"));

        service.run(ctx, chain);

        assertContextOk(ctx, "O1:doc:doc", "Hello from Context!", "Source");
    }

    /**
     * Same as previous but test params specified as Mvel templates
     *
     * @throws Exception
     */
    @Test
    public void testTemplateParams() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        SimplePrincipal principal = new SimplePrincipal("Context");
        ctx.put("messageHolder", principal);
        OperationChain chain = new OperationChain("testChain");
        chain.add("o1").set("message", new MvelTemplate("Hello from @{Context[\"messageHolder\"].name}!"));

        service.run(ctx, chain);

        assertContextOk(ctx, "O1:doc:doc", "Hello from Context!", "Source");
    }

    /**
     * This is testing an invalid chain. The last operation in the chain accepts as input only Principal which is never
     * produced by the previous operations in the chain.
     *
     * @throws Exception
     */
    @Test
    public void testInvalidChain() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add("o1").set("message", "Hello 1!");
        chain.add("o2").set("message", "Hello 2!");
        chain.add("unmatched");

        try {
            service.run(ctx, chain);
            fail("Invalid chain not detected!");
        } catch (OperationException e) {
            assertEquals(
                    "Cannot find any valid path in operation chain - no method found for operation 'o1' and for first input type 'org.nuxeo.ecm.core.api.impl.DocumentModelImpl'",
                    e.getMessage());
        }
    }

    /**
     * When using a null context input an exception must be thrown if the first operation doesn't accept void input.
     */
    @Test
    public void testInvalidInput() throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("testChain");
        chain.add("o1").set("message", "Hello 1!");

        try {
            service.run(ctx, chain);
            fail("Null input was handled by a non void operation!");
        } catch (OperationException e) {
            // test passed
        }
    }

    /**
     * <pre>
     * Input: VOID
     * Test chain: V1 -&gt; V1 -&gt; V2
     * V1:void:doc | V1:void:doc | V2:void:doc
     * V1:doc:doc  | V1:doc:doc  | V2:string:doc
     * Expected output: (parameters in context)
     * chain : &quot;V1:void:doc,V1:doc:doc,V2:void:doc&quot;
     * message : &quot;Hello 1!,Hello 2!,Hello 3!&quot;
     * title : &quot;,/,/&quot;
     * </pre>
     *
     * Test void input.
     *
     * @throws Exception
     */
    @Test
    public void testVoidInput() throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("testChain");
        chain.add("v1").set("message", "Hello 1!");
        chain.add("v1").set("message", "Hello 2!");
        chain.add("v2").set("message", "Hello 3!");

        service.run(ctx, chain);

        assertContextOk(ctx, "V1:void:doc,V1:doc:doc,V2:void:doc", "Hello 1!,Hello 2!,Hello 3!", ",/,/");
    }

    /**
     * <pre>
     * Input: VOID
     * Test chain: A1 -&gt; A1
     * A1:void:docref | A1:void:docref
     * A1:doc:doc     | A1:doc:doc
     * Expected output: (parameters in context)
     * chain : &quot;A1:void:docref,A1:doc:doc&quot;
     * message : &quot;Hello 1!,Hello 2!&quot;
     * title : &quot;/,/&quot;
     * </pre>
     *
     * Test docref to doc adapter. Test precedence of adapters over void.
     *
     * @throws Exception
     */
    @Test
    public void testTypeAdapters() throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("testChain");
        chain.add("a1").set("message", "Hello 1!");
        chain.add("a1").set("message", "Hello 2!");

        service.run(ctx, chain);

        assertContextOk(ctx, "A1:void:docref,A1:doc:doc", "Hello 1!,Hello 2!", "/,/");
    }

    /**
     * <pre>
     * Input: doc
     * Test chain: A2
     * A2:void:docref
     * A2:docref:doc
     * Expected output: (parameters in context)
     * chain : &quot;A2:docref:docref&quot;
     * message : &quot;Hello 1!&quot;
     * title : &quot;/&quot;
     * </pre>
     *
     * Test doc to docref adapter.
     *
     * @throws Exception
     */
    @Test
    public void testTypeAdapters2() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(session.getRootDocument());

        OperationChain chain = new OperationChain("testChain");
        chain.add("a2").set("message", "Hello 1!");

        service.run(ctx, chain);

        assertContextOk(ctx, "A2:docref:docref", "Hello 1!", "/");
    }

    /**
     * This is testing optional parameters. Operation2 has an optional 'message' parameter. If this is not specified in
     * the operation parameter map the default value will be used which is 'default message'.
     *
     * @throws Exception
     */
    @Test
    public void testOptionalParam() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add("o2");

        service.run(ctx, chain);

        assertContextOk(ctx, "O2:doc:ref", "default message", "Source");
    }

    /**
     * This is testing required parameters. Operation1 has a required 'message' parameter. If this is not specified in
     * the operation parameter map an exception must be thrown.
     *
     * @throws Exception
     */
    @Test
    public void testRequiredParam() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add("o1");

        try {
            service.run(ctx, chain);
            fail("Required parameter test failure!");
        } catch (OperationException e) {
            // test passed
        }
    }

    /**
     * This is testing adapters when injecting parameters. Operation 4 is taking a DocumentModel parameter. We will
     * inject a DocumentRef to test DocRef to DocModel adapter.
     *
     * @throws Exception
     */
    @Test
    public void testAdaptableParam() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add("o4").set("message", "Hello 1!").set("doc", src.getRef());

        Object out = service.run(ctx, chain);
        assertEquals(src.getId(), ((DocumentModel) out).getId());

        assertContextOk(ctx, "O4:void:doc", "Hello 1!", "Source");
    }

    /**
     * Set a context variable from the title of the input document and use it in the next operation (by returning it)
     * This is also testing boolean injection.
     *
     * @throws Exception
     */
    @Test
    public void testSetVar() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(SetVar.ID).set("name", "myvar").set("value", Scripting.newExpression("Document['dc:title']"));
        chain.add("GetVar").set("name", "myvar").set("flag", true);

        Object out = service.run(ctx, chain);
        assertEquals(src.getTitle(), out);
    }

    @Test
    public void testStringListOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("testSlo");
        chain.add("slo").set("emails", "a,b,c");

        StringList out = (StringList) service.run(ctx, chain);
        assertEquals(3, out.size());
        assertTrue(out.contains("a"));
        assertTrue(out.contains("b"));
        assertTrue(out.contains("c"));
    }

    @Test
    public void testCopyDates() throws Exception {
        DocumentModel doc = session.createDocumentModel(src.getPathAsString(), "testDoc", "Folder");
        doc.setPropertyValue("dc:title", "TestDoc");
        doc = session.createDocument(doc);
        session.save();

        Calendar date = Calendar.getInstance();
        src.setPropertyValue("dc:issued", date);
        src = session.saveDocument(src);

        session.save();

        assertNull(doc.getPropertyValue("dc:issued"));
        assertEquals(date, src.getPropertyValue("dc:issued"));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        // this chain copy the dc:issue from the parent to the child
        DocumentModel out = (DocumentModel) service.run(ctx, "testDateCopy");

        assertEquals(date, out.getPropertyValue("dc:issued"));
    }
}
