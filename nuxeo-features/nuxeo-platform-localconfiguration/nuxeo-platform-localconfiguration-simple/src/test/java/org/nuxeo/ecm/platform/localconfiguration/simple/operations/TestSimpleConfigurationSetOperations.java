/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.localconfiguration.simple.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.localconfiguration.simple.AbstractSimpleConfigurationTest;
import org.nuxeo.ecm.platform.localconfiguration.simple.LocalConfigurationRepositoryInit;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, init = LocalConfigurationRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.platform.localconfiguration.simple" })
public class TestSimpleConfigurationSetOperations extends
        AbstractSimpleConfigurationTest {

    @Inject
    AutomationService service;

    @Test
    public void shouldSetContextVariableFromSimpleConfiguration()
            throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        initializeSimpleConfiguration(workspace, parameters);

        OperationContext ctx = new OperationContext(session);
        assertNotNull(ctx);

        OperationChain chain = new OperationChain(
                "testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(SetSimpleConfigurationParameterAsVar.ID).set("name",
                "simpleConfigurationParameter").set("parameterName", "key2");

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

        OperationChain chain = new OperationChain(
                "testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(SetSimpleConfigurationParameterAsVar.ID).set("name",
                "simpleConfigurationParameter").set("parameterName", "key1").set(
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

        OperationChain chain = new OperationChain(
                "testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(SetSimpleConfigurationParameterAsVar.ID).set("name",
                "simpleConfigurationParameter").set("parameterName", "key2").set(
                "defaultValue", "default");

        service.run(ctx, chain);

        String contextVariable = (String) ctx.get("simpleConfigurationParameter");
        assertNotNull(contextVariable);
        assertEquals("default", contextVariable);
    }

}
