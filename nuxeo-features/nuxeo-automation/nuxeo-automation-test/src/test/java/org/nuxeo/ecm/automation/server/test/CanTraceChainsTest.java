/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

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
    public void setup() throws OperationException, ClientException {
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
        assertEquals(DummyOperation.ID,
                firstCall.getVariables().get(DummyOperation.ID));
        assertEquals(DummyOperation.ID,
                firstCall.getParmeters().get(DummyOperation.ID));

        // Deactivate trace mode -> light weight trace
        factory.toggleRecording();
        calls = trace.getCalls();
        assertEquals(3, calls.size());
    }

    @Test
    public void testSubchainsTrace() throws Exception {
        final String chainid = "traceSubchains";
        OperationChain chain = new OperationChain("parentChain");
        OperationParameters runOnListParams = new OperationParameters(
                RunOperationOnList.ID);
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
        assertEquals(
                nested.get(0).getCalls().get(0).getVariables().get("item"),
                "one");
        assertEquals(
                nested.get(1).getCalls().get(0).getVariables().get("item"),
                "two");
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
}
