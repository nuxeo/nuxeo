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

package org.nuxeo.ecm.platform.localconfiguration.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;
import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_PARAMETERS_PROPERTY;
import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_PARAMETER_KEY;
import static org.nuxeo.ecm.platform.localconfiguration.simple.SimpleConfiguration.SIMPLE_CONFIGURATION_PARAMETER_VALUE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests for {@link SimpleConfiguration}
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = LocalConfigurationRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.localconfiguration.simple" })
public class TestSimpleConfiguration extends AbstractSimpleConfigurationTest {

    @Test
    public void shouldNotRetrieveSimpleConfiguration() throws ClientException {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                workspace);

        assertNull(simpleConfiguration);
    }

    @Test
    public void shouldRetrieveSimpleConfigurationOnWorkspace()
            throws ClientException {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(workspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                workspace);

        assertNotNull(simpleConfiguration);
        assertEquals(workspace.getRef(), simpleConfiguration.getDocumentRef());
    }

    @Test
    public void shouldRetrieveSimpleConfigurationFromChildDocument()
            throws ClientException {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(workspace);

        DocumentModel folder = session.getDocument(FOLDER_REF);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET, folder);

        assertNotNull(simpleConfiguration);
        assertEquals(workspace.getRef(), simpleConfiguration.getDocumentRef());
    }

    @Test
    public void shouldRetrieveParametersFromSimpleConfiguration()
            throws ClientException {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        initializeSimpleConfiguration(workspace, parameters);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                workspace);

        assertNotNull(simpleConfiguration);
        assertEquals(workspace.getRef(), simpleConfiguration.getDocumentRef());

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value2", simpleConfiguration.get("key2"));
        assertNull(simpleConfiguration.get("notExistingKey"));

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(2, adapter.parameters.size());
    }

    @Test
    public void shouldMergeSimpleConfigurations() throws ClientException {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parentParameters = new HashMap<String, String>();
        parentParameters.put("key1", "value1");
        parentParameters.put("key2", "value2");
        initializeSimpleConfiguration(parentWorkspace, parentParameters);

        DocumentModel childWorkspace = session.getDocument(CHILD_WORKSPACE_REF);
        Map<String, String> childParameters = new HashMap<String, String>();
        childParameters.put("key3", "value3");
        childParameters.put("key4", "value4");
        initializeSimpleConfiguration(childWorkspace, childParameters);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                childWorkspace);

        assertNotNull(simpleConfiguration);
        assertEquals(parentWorkspace.getRef(),
                simpleConfiguration.getDocumentRef());

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value2", simpleConfiguration.get("key2"));
        assertEquals("value3", simpleConfiguration.get("key3"));
        assertEquals("value4", simpleConfiguration.get("key4"));
        assertNull(simpleConfiguration.get("notExistingKey"));

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(4, adapter.parameters.size());
    }

    @Test
    public void childSimpleConfigurationShouldOverriderParentSimpleConfiguration()
            throws ClientException {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parentParameters = new HashMap<String, String>();
        parentParameters.put("key1", "parentValue1");
        parentParameters.put("key2", "parentValue2");
        initializeSimpleConfiguration(parentWorkspace, parentParameters);

        DocumentModel childWorkspace = session.getDocument(CHILD_WORKSPACE_REF);
        Map<String, String> childParameters = new HashMap<String, String>();
        childParameters.put("key2", "childValue2");
        childParameters.put("key4", "childValue4");
        initializeSimpleConfiguration(childWorkspace, childParameters);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                childWorkspace);

        assertNotNull(simpleConfiguration);
        assertEquals(parentWorkspace.getRef(),
                simpleConfiguration.getDocumentRef());

        assertEquals("parentValue1", simpleConfiguration.get("key1"));
        assertEquals("childValue2", simpleConfiguration.get("key2"));
        assertEquals("childValue4", simpleConfiguration.get("key4"));
        assertNull(simpleConfiguration.get("key3"));
        assertNull(simpleConfiguration.get("notExistingKey"));

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(3, adapter.parameters.size());
    }

    @Test
    public void shouldBeAbleToPutNewParameter() throws ClientException {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                parentWorkspace);

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(0, adapter.parameters.size());

        simpleConfiguration.put("key1", "value1");
        assertEquals(1, adapter.parameters.size());

        simpleConfiguration.put("key2", "value2");
        assertEquals(2, adapter.parameters.size());

        assertEquals("value2", simpleConfiguration.get("key2"));
        assertEquals("value1", simpleConfiguration.get("key1"));
    }

    @Test
    public void shouldBeAbleToPutNewParameters() throws ClientException {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                parentWorkspace);

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(0, adapter.parameters.size());

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        parameters.put("key3", "value3");
        parameters.put("key4", "value4");

        simpleConfiguration.putAll(parameters);
        assertEquals(4, adapter.parameters.size());

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value2", simpleConfiguration.get("key2"));
        assertEquals("value3", simpleConfiguration.get("key3"));
        assertEquals("value4", simpleConfiguration.get("key4"));
    }

    @Test
    public void newParametersShouldBeSavedOnTheDocument()
            throws ClientException {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                parentWorkspace);

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(0, adapter.parameters.size());

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");

        simpleConfiguration.putAll(parameters);
        assertEquals(2, adapter.parameters.size());

        simpleConfiguration.save(session);

        parentWorkspace = session.getDocument(parentWorkspace.getRef());
        List<Map<String, String>> parametersFromDocument = (List<Map<String, String>>) parentWorkspace.getPropertyValue(SIMPLE_CONFIGURATION_PARAMETERS_PROPERTY);
        Map<String, String> savedParameters = convertParametersListToMap(parametersFromDocument);

        assertNotNull(savedParameters);
        assertEquals(2, savedParameters.size());
        assertEquals("value1", savedParameters.get("key1"));
        assertEquals("value2", savedParameters.get("key2"));
    }

    protected Map<String, String> convertParametersListToMap(
            List<Map<String, String>> parametersList) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (parametersList != null) {
            for (Map<String, String> parameter : parametersList) {
                parameters.put(
                        parameter.get(SIMPLE_CONFIGURATION_PARAMETER_KEY),
                        parameter.get(SIMPLE_CONFIGURATION_PARAMETER_VALUE));
            }
        }
        return parameters;
    }

    @Test(expected = DocumentSecurityException.class)
    public void nonAuthorizedUserShouldNotBeAbleToSaveConfiguration()
            throws ClientException {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        addReadForEveryone(CHILD_WORKSPACE_REF);

        CoreSession newSession = openSessionAs("user1");
        DocumentModel childWorkspace = newSession.getDocument(CHILD_WORKSPACE_REF);
        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET,
                childWorkspace);

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(0, adapter.parameters.size());

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");

        simpleConfiguration.putAll(parameters);
        assertEquals(2, adapter.parameters.size());

        try {
            simpleConfiguration.save(newSession);
        } finally {
            CoreInstance.getInstance().close(newSession);
        }
    }

}
