/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.localconf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = LocalConfRepositoryInit.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.localconf")
public class TestPutOperations extends AbstractSimpleConfigurationTest {

    @Inject
    AutomationService service;

    @Test
    public void shouldPutNewParameterOnExistingConfiguration() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        initializeSimpleConfiguration(workspace, parameters);

        OperationContext ctx = new OperationContext(session);
        assertNotNull(ctx);

        OperationChain chain = new OperationChain("testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(PutSimpleConfParam.ID).set("key", "key2").set("value", "value2");
        service.run(ctx, chain);

        workspace = session.getDocument(workspace.getRef());

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, workspace);

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value2", simpleConfiguration.get("key2"));
    }

    @Test
    public void shouldReplaceExistingParameters() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> existingParameters = new HashMap<String, String>();
        existingParameters.put("key1", "value1");
        existingParameters.put("key2", "value2");
        existingParameters.put("key3", "value3");
        initializeSimpleConfiguration(workspace, existingParameters);

        OperationContext ctx = new OperationContext(session);
        assertNotNull(ctx);

        Map<String, String> newParameters = new HashMap<String, String>();
        newParameters.put("key2", "newValue2");
        newParameters.put("key3", "newValue3");
        OperationChain chain = new OperationChain("testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(PutSimpleConfParams.ID).set("parameters", new Properties(newParameters));
        service.run(ctx, chain);

        workspace = session.getDocument(workspace.getRef());

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, workspace);

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("newValue2", simpleConfiguration.get("key2"));
        assertEquals("newValue3", simpleConfiguration.get("key3"));
    }

    @Test
    public void shouldAddFacetAndPutNewParameters() throws Exception {
        OperationContext ctx = new OperationContext(session);
        assertNotNull(ctx);

        // PutSimpleConfigurationParameters
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key3", "value3");
        OperationChain chain = new OperationChain("testPutSimpleConfigurationParametersChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(PutSimpleConfParams.ID).set("parameters", new Properties(parameters));
        service.run(ctx, chain);

        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, workspace);

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value3", simpleConfiguration.get("key3"));

        // PutSimpleConfigurationParameter
        chain = new OperationChain("testPutSimpleConfigurationParametersChain");
        chain.add(FetchDocument.ID).set("value", CHILD_WORKSPACE_REF);
        chain.add(PutSimpleConfParam.ID).set("key", "key1").set("value", "value1");
        service.run(ctx, chain);

        workspace = session.getDocument(CHILD_WORKSPACE_REF);
        simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, workspace);
        assertEquals("value1", simpleConfiguration.get("key1"));
    }

    @Test(expected = DocumentSecurityException.class)
    public void nonAuthorizedUserShouldNotBeAbleToPutNewParameter() throws Exception {
        addReadForEveryone(CHILD_WORKSPACE_REF);

        try (CloseableCoreSession newSession = openSessionAs("user1")) {
            OperationContext ctx = new OperationContext(newSession);
            assertNotNull(ctx);

            // PutSimpleConfigurationParameter
            OperationChain chain = new OperationChain("testPutSimpleConfigurationParametersChain");
            chain.add(FetchDocument.ID).set("value", CHILD_WORKSPACE_REF);
            chain.add(PutSimpleConfParam.ID).set("key", "key1").set("value", "value1");
            service.run(ctx, chain);
        }
    }

}
