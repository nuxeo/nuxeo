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
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_FACET;
import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_PARAMETERS_PROPERTY;
import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_PARAMETER_KEY;
import static org.nuxeo.ecm.localconf.SimpleConfiguration.SIMPLE_CONFIGURATION_PARAMETER_VALUE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.test.CoreFeature;
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
@RepositoryConfig(init = LocalConfRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.localconf")
public class TestSimpleConfiguration extends AbstractSimpleConfigurationTest {

    @Test
    public void shouldNotRetrieveSimpleConfiguration() {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, workspace);

        assertNull(simpleConfiguration);
    }

    @Test
    public void shouldRetrieveSimpleConfigurationOnWorkspace() {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(workspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, workspace);

        assertNotNull(simpleConfiguration);
        assertEquals(workspace.getRef(), simpleConfiguration.getDocumentRef());
    }

    @Test
    public void shouldRetrieveSimpleConfigurationFromChildDocument() {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(workspace);

        DocumentModel folder = session.getDocument(FOLDER_REF);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, folder);

        assertNotNull(simpleConfiguration);
        assertEquals(workspace.getRef(), simpleConfiguration.getDocumentRef());
    }

    @Test
    public void shouldRetrieveParametersFromSimpleConfiguration() {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        initializeSimpleConfiguration(workspace, parameters);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, workspace);

        assertNotNull(simpleConfiguration);
        assertEquals(workspace.getRef(), simpleConfiguration.getDocumentRef());

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value2", simpleConfiguration.get("key2"));
        assertNull(simpleConfiguration.get("notExistingKey"));

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(2, adapter.parameters.size());
    }

    @Test
    public void shouldMergeSimpleConfigurations() {
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

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, childWorkspace);

        assertNotNull(simpleConfiguration);
        assertEquals(parentWorkspace.getRef(), simpleConfiguration.getDocumentRef());

        assertEquals("value1", simpleConfiguration.get("key1"));
        assertEquals("value2", simpleConfiguration.get("key2"));
        assertEquals("value3", simpleConfiguration.get("key3"));
        assertEquals("value4", simpleConfiguration.get("key4"));
        assertNull(simpleConfiguration.get("notExistingKey"));

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(4, adapter.parameters.size());
    }

    @Test
    public void childSimpleConfigurationShouldOverriderParentSimpleConfiguration() {
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

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, childWorkspace);

        assertNotNull(simpleConfiguration);
        assertEquals(parentWorkspace.getRef(), simpleConfiguration.getDocumentRef());

        assertEquals("parentValue1", simpleConfiguration.get("key1"));
        assertEquals("childValue2", simpleConfiguration.get("key2"));
        assertEquals("childValue4", simpleConfiguration.get("key4"));
        assertNull(simpleConfiguration.get("key3"));
        assertNull(simpleConfiguration.get("notExistingKey"));

        SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
        assertEquals(3, adapter.parameters.size());
    }

    @Test
    public void shouldBeAbleToPutNewParameter() {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, parentWorkspace);

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
    public void shouldBeAbleToPutNewParameters() {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, parentWorkspace);

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
    public void newParametersShouldBeSavedOnTheDocument() {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(SimpleConfiguration.class,
                SIMPLE_CONFIGURATION_FACET, parentWorkspace);

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

    protected Map<String, String> convertParametersListToMap(List<Map<String, String>> parametersList) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (parametersList != null) {
            for (Map<String, String> parameter : parametersList) {
                parameters.put(parameter.get(SIMPLE_CONFIGURATION_PARAMETER_KEY),
                        parameter.get(SIMPLE_CONFIGURATION_PARAMETER_VALUE));
            }
        }
        return parameters;
    }

    @Test(expected = DocumentSecurityException.class)
    public void nonAuthorizedUserShouldNotBeAbleToSaveConfiguration() {
        DocumentModel parentWorkspace = session.getDocument(PARENT_WORKSPACE_REF);
        initializeSimpleConfiguration(parentWorkspace);

        addReadForEveryone(CHILD_WORKSPACE_REF);

        try (CloseableCoreSession newSession = openSessionAs("user1")) {
            DocumentModel childWorkspace = newSession.getDocument(CHILD_WORKSPACE_REF);
            SimpleConfiguration simpleConfiguration = localConfigurationService.getConfiguration(
                    SimpleConfiguration.class, SIMPLE_CONFIGURATION_FACET, childWorkspace);

            SimpleConfigurationAdapter adapter = (SimpleConfigurationAdapter) simpleConfiguration;
            assertEquals(0, adapter.parameters.size());

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key1", "value1");
            parameters.put("key2", "value2");

            simpleConfiguration.putAll(parameters);
            assertEquals(2, adapter.parameters.size());
            simpleConfiguration.save(newSession);
        }
    }

}
