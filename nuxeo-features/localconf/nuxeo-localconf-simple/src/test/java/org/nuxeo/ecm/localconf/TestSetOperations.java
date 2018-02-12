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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
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
@RepositoryConfig(init = LocalConfRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.localconf")
public class TestSetOperations extends AbstractSimpleConfigurationTest {

    @Inject
    AutomationService service;

    @Test
    public void shouldSetContextVariableFromSimpleConfiguration() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        initializeSimpleConfiguration(workspace, parameters);

        OperationContext ctx = new OperationContext(session);
        assertNotNull(ctx);

        OperationChain chain = new OperationChain("testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(SetSimpleConfParamVar.ID).set("name", "simpleConfigurationParameter").set("parameterName", "key2");

        service.run(ctx, chain);

        String contextVariable = (String) ctx.get("simpleConfigurationParameter");
        assertNotNull(contextVariable);
        assertEquals("value2", contextVariable);
    }

    @Test
    public void shouldNotUseDefaultValueIfParameterExists() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        initializeSimpleConfiguration(workspace, parameters);

        OperationContext ctx = new OperationContext(session);
        assertNotNull(ctx);

        OperationChain chain = new OperationChain("testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(SetSimpleConfParamVar.ID).set("name", "simpleConfigurationParameter").set("parameterName", "key1").set(
                "defaultValue", "default");

        service.run(ctx, chain);

        String contextVariable = (String) ctx.get("simpleConfigurationParameter");
        assertNotNull(contextVariable);
        assertEquals("value1", contextVariable);
    }

    @Test
    public void shouldUseDefaultValueIfParameterDoesNotExist() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        initializeSimpleConfiguration(workspace, parameters);

        OperationContext ctx = new OperationContext(session);
        assertNotNull(ctx);

        OperationChain chain = new OperationChain("testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(SetSimpleConfParamVar.ID).set("name", "simpleConfigurationParameter").set("parameterName", "key2").set(
                "defaultValue", "default");

        service.run(ctx, chain);

        String contextVariable = (String) ctx.get("simpleConfigurationParameter");
        assertNotNull(contextVariable);
        assertEquals("default", contextVariable);
    }

}
