/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *      Andre Justo
 */
package org.nuxeo.runtime.services.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @since 7.4
 */
public class TestConfigurationService extends NXRuntimeTestCase {

    ConfigurationService cs;

    @Override
    public void postSetUp() throws Exception {
        cs = Framework.getService(ConfigurationService.class);
        assertNotNull(cs);
    }

    @Test
    public void testExtensionPoint() throws Exception {
        // Assert properties don't exist
        assertNull(cs.getProperty("nuxeo.test.dummyBooleanProperty"));
        assertNull(cs.getProperty("nuxeo.test.anotherDummyBooleanProperty"));
        assertNull(cs.getProperty("nuxeo.test.dummyStringProperty"));
        // Deploy contribution with properties
        pushInlineDeployments("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");
        postSetUp();
        assertNotNull(cs.getProperty("nuxeo.test.dummyBooleanProperty"));
        assertNotNull(cs.getProperty("nuxeo.test.anotherDummyBooleanProperty"));
        assertNotNull(cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertNull(cs.getProperty("nuxeo.test.invalidDummyProperty"));
    }

    @Test
    public void testProperties() throws Exception {
        pushInlineDeployments("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");
        postSetUp();
        assertTrue(cs.isBooleanPropertyTrue("nuxeo.test.dummyBooleanProperty"));
        assertFalse(cs.isBooleanPropertyTrue("nuxeo.test.anotherDummyBooleanProperty"));
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
    }

    @Test
    public void testOverride() throws Exception {
        // Deploy contribution with properties
        pushInlineDeployments("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");
        postSetUp();
        // Assert property has overridden value
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
        // Assert property don't exist
        assertNull(cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Deploy another contrib with a new property and override existing properties
        pushInlineDeployments("org.nuxeo.runtime.test.tests:configuration-override-contrib.xml");
        postSetUp();
        // Assert new property was added
        assertEquals("overrideContrib", cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Assert framework property does not takes precedence
        assertEquals("dummyStringValueOverridden", cs.getProperty("nuxeo.test.dummyStringProperty"));
        Framework.getProperties().setProperty("nuxeo.test.dummyStringProperty", "anotherDummyValue");
        assertEquals("dummyStringValueOverridden", cs.getProperty("nuxeo.test.dummyStringProperty"));
        Framework.getProperties().remove("nuxeo.test.dummyStringProperty");
        // Assert old properties have overridden values
        assertEquals("dummyStringValueOverridden", cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertTrue(cs.isBooleanPropertyFalse("nuxeo.test.dummyBooleanProperty"));

        // Undeploy contrib
        popInlineDeployments();
        postSetUp();
        // Assert property was removed
        assertNull(cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Assert overridden values were restored
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertTrue(cs.isBooleanPropertyTrue("nuxeo.test.dummyBooleanProperty"));
    }

    @Test
    public void testCompatWarn() throws Exception {
        Framework.getProperties().setProperty("nuxeo.test.dummyStringProperty", "anotherDummyValue");
        assertEquals(0, Framework.getRuntime().getMessageHandler().getWarnings().size());
        // Deploy contribution with properties
        pushInlineDeployments("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");
        postSetUp();
        assertEquals(1, Framework.getRuntime().getMessageHandler().getWarnings().size());
        assertEquals(
                "Property 'nuxeo.test.dummyStringProperty' should now be contributed to "
                        + "extension point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'",
                Framework.getRuntime().getMessageHandler().getWarnings().get(0));
        Framework.getProperties().remove("nuxeo.test.dummyStringProperty");
    }

}
