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
 *     vpasquier
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.impl.OperationServiceImpl;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.SetVar;
import org.nuxeo.ecm.automation.core.operations.execution.RunDocumentChain;
import org.nuxeo.ecm.automation.core.scripting.MvelTemplate;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests for parameterized chains.
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-parametrization-chain.xml")
public class TestOperationChainParametrization {

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
        Framework.getService(EventService.class).waitForAsyncCompletion();
        session.removeChildren(session.getRootDocument().getRef());
    }

    /**
     * Check if using chain with parameters is working
     */
    @Test
    public void testParametizedChain() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(src);
            OperationChain chain = new OperationChain("notRegisteredChain");
            chain.add(FetchContextDocument.ID);
            chain.add("o1").set("message", "Hello 1!");
            chain.add("o2").set("message", "Hello 2!");
            chain.add("oChainCtx").set("message", "expr:@{ChainParameters['messageChain']}");
            // Setting parameters of the chain
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("messageChain", "Hello i'm a chain!");
            chain.addChainParameters(params);
            // Checking if chain parameter is taken into account
            DocumentModel doc = (DocumentModel) ((OperationServiceImpl) service).run(ctx, chain);
            assertNotNull(doc);
        }
    }

    /**
     * Check if using registered chain with its operations is working
     */
    @Test
    public void testContributedParametizedChain() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(src);
            // Setting parameters of the chain
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("messageChain", "Hello i'm a chain!");
            // Checking if chain parameter is taken into account
            DocumentModel doc = (DocumentModel) service.run(ctx, "contributedchain", params);
            assertNotNull(doc);
        }
    }

    /**
     * Check if using new chain parameters in an execution flow operation is working
     */
    @Test
    public void testExecutionFlowOperation() throws Exception {
        // Run Document Chain
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(src);
            OperationChain chain = new OperationChain("testChain");
            chain.add(FetchContextDocument.ID);
            chain.add(RunDocumentChain.ID).set("id", "contributedchain2").set("parameters",
                    new Properties("exampleKey=exampleValue\nexampleKey2=exampleValue2"));
            DocumentModel doc = (DocumentModel) service.run(ctx, chain);
            assertNotNull(doc);
        }
    }

    // begin non regression tests for NXP-24480
    /**
     * @since 10.2
     */
    @Test
    public void testRunOperationWithSameKeyInContextAndChainParameters() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            service.run(ctx, "testRunOperationWithSameKeyInContextAndChainParameters");
            // Check that the initial context is unchanged.
            assertEquals("contextValue1", ctx.get("contextKey1"));
            assertEquals("contextValue2", ctx.get("contextKey2"));
            // Check that the chain parameters passed to RunOperation are well resolved in the underlying operation that
            // joins them into a context variable.
            assertEquals("bar,chainParameterValue,contextValue1", ctx.get("resolvedChainParameters"));
        }
    }

    /**
     * @since 10.2
     */
    @Test
    public void testResolveOperationParameterFromChainParameters() throws Exception {
        try (OperationContext ctx = new OperationContext(session)) {
            // Check that a chain parameter of type string ("value": "bar") is well resolved and injected as the "value"
            // parameter of the SetVar operation.
            OperationChain chain = new OperationChain("testChain");
            chain.add(SetVar.ID).set("name", "foo");
            chain.addChainParameters(Collections.singletonMap("value", "bar"));
            service.run(ctx, chain);
            assertEquals("bar", ctx.get("foo"));

            // Check that a chain parameter of type expression ("value": "expr:...") is well resolved and injected as
            // the "value" parameter of the SetVar operation.
            chain = new OperationChain("testChain");
            chain.add(SetVar.ID).set("name", "foo");
            ctx.put("contextKey", "bar");
            chain.addChainParameters(
                    Collections.singletonMap("value", new MvelTemplate("this is @{Context['contextKey']}")));
            service.run(ctx, chain);
            assertEquals("this is bar", ctx.get("foo"));
        }
    }
    // end non regression tests for NXP-24480

}
