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
import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.localconfiguration.simple.AbstractSimpleConfigurationTest;
import org.nuxeo.ecm.platform.localconfiguration.simple.LocalConfigurationRepositoryInit;
import org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration;
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
public class TestSimpleConfigurationPutOperations extends
        AbstractSimpleConfigurationTest {

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

        OperationChain chain = new OperationChain(
                "testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(PutSimpleConfigurationParameter.ID).set("key", "key2").set(
                "value", "value2");
        service.run(ctx, chain);

        workspace = session.getDocument(workspace.getRef());

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                workspace);

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
        OperationChain chain = new OperationChain(
                "testSimpleConfigurationChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(PutSimpleConfigurationParameters.ID).set("parameters",
                new Properties(newParameters));
        service.run(ctx, chain);

        workspace = session.getDocument(workspace.getRef());

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                workspace);

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
        OperationChain chain = new OperationChain(
                "testPutSimpleConfigurationParametersChain");
        chain.add(FetchDocument.ID).set("value", PARENT_WORKSPACE_REF);
        chain.add(PutSimpleConfigurationParameters.ID).set("parameters",
                new Properties(parameters));
        service.run(ctx, chain);

        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                workspace);

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value3", simpleConfiguration.get("key3"));

        // PutSimpleConfigurationParameter
        chain = new OperationChain("testPutSimpleConfigurationParametersChain");
        chain.add(FetchDocument.ID).set("value", CHILD_WORKSPACE_REF);
        chain.add(PutSimpleConfigurationParameter.ID).set("key", "key1").set(
                "value", "value1");
        service.run(ctx, chain);

        workspace = session.getDocument(CHILD_WORKSPACE_REF);
        simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                workspace);
        assertEquals("value1", simpleConfiguration.get("key1"));
    }

    @Test(expected = OperationException.class)
    public void nonAuthorizedUserShouldNotBeAbleToPutNewParameter()
            throws Exception {
        addReadForEveryone(CHILD_WORKSPACE_REF);

        CoreSession newSession = openSessionAs("user1");
        OperationContext ctx = new OperationContext(newSession);
        assertNotNull(ctx);

        // PutSimpleConfigurationParameter
        OperationChain chain = new OperationChain(
                "testPutSimpleConfigurationParametersChain");
        chain.add(FetchDocument.ID).set("value", CHILD_WORKSPACE_REF);
        chain.add(PutSimpleConfigurationParameter.ID).set("key", "key1").set(
                "value", "value1");
        try {
            service.run(ctx, chain);
        } finally {
            CoreInstance.getInstance().close(newSession);
        }
    }

}
