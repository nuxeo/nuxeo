/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnList;
import org.nuxeo.ecm.automation.core.trace.Call;
import org.nuxeo.ecm.automation.core.trace.Trace;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.automation.server.test.operations.ContextTraceOperation;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.test" })
@LocalDeploy("org.nuxeo.ecm.automation.test:test-bindings.xml")
public class CanTraceChainsTest {

    @Inject
    AutomationService service;

    @Inject
    OperationContext context;

    @Inject
    CoreSession session;

    @Inject
    TracerFactory factory;

    DocumentModel src;

    @Before
    public void setup() throws OperationException {
        service.putOperation(DummyOperation.class);
        // Setup a document
        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        // Enable trace mode
        if (!factory.getRecordingState()) {
            factory.toggleRecording();
        }
    }

    @After
    public void teardown() {
        service.removeOperation(DummyOperation.class);
    }

    @Test
    public void testSimpleChainTrace() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        chain.add(DummyOperation.ID).set(DummyOperation.ID, DummyOperation.ID);
        chain.add(DummyOperation.ID);
        chain.add(DummyOperation.ID);

        context.setInput(DummyOperation.ID);
        context.put(DummyOperation.ID, DummyOperation.ID);

        service.run(context, chain);

        Trace trace = factory.getTrace("testChain");
        assertNull(trace.getError());
        assertEquals(DummyOperation.ID, trace.getOutput());
        List<Call> calls = trace.getCalls();
        assertEquals(3, calls.size());
        Call firstCall = calls.get(0);
        assertEquals(DummyOperation.ID, firstCall.getType().getId());
        assertEquals(DummyOperation.ID, firstCall.getVariables().get(DummyOperation.ID));
        assertEquals(DummyOperation.ID, firstCall.getParameters().get(DummyOperation.ID));

        // Deactivate trace mode -> light weight trace
        factory.toggleRecording();
        calls = trace.getCalls();
        assertEquals(3, calls.size());
    }

    @Test
    public void testSubchainsTrace() throws Exception {
        final String chainid = "traceSubchains";
        OperationChain chain = new OperationChain("parentChain");
        OperationParameters runOnListParams = new OperationParameters(RunOperationOnList.ID);
        runOnListParams.set("list", "list");
        runOnListParams.set("id", chainid);
        chain.add(runOnListParams);
        context.setInput(src);
        context.put("list", Arrays.asList(new String[] { "one", "two" }));
        service.run(context, chain);
        Trace trace = factory.getTrace("parentChain");
        List<Call> calls = trace.getCalls();
        assertEquals(1, calls.size());
        List<Trace> nested = calls.get(0).getNested();
        assertEquals(2, nested.size());
        assertEquals(nested.get(0).getCalls().get(0).getVariables().get("item"), "one");
        assertEquals(nested.get(1).getCalls().get(0).getVariables().get("item"), "two");
    }

    @Test
    public void testOperationTrace() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", src);
        service.run(context, FetchDocument.ID, parameters);
        Trace trace = factory.getTrace(FetchDocument.ID);
        List<Call> calls = trace.getCalls();
        assertEquals(1, calls.size());
    }

    @Test
    public void testTraceMvelExpression() throws Exception {
        context.setInput(src);
        service.run(context, "testChainTrace");
        Trace trace = factory.getTrace("testChainTrace");
        assertEquals("chain.doc",
                ((Call.ExpressionParameter) trace.getCalls().get(2).getParameters().get("name")).getParameterValue());
        assertEquals("name",
                ((Call.ExpressionParameter) trace.getCalls().get(2).getParameters().get("name")).getParameterId());
    }

    @Test
    public void canKeepSubContextValuesWithTraces() throws Exception {
        OperationContext ctx = new OperationContext(session);
        List<String> users = new ArrayList<>();
        users.add("foo");
        users.add("bar");
        users.add("baz");
        users.add("bum");
        ctx.put("users", users);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("list", "users");
        parameters.put("id", "mvelSubChain");
        parameters.put("isolate", "false");

        service.run(ctx,RunOperationOnList.ID, parameters);
        assertEquals("foo", ctx.get("foo"));
        assertEquals("bar", ctx.get("bar"));
        assertEquals("baz", ctx.get("baz"));
        assertEquals("bum", ctx.get("bum"));
    }
}
