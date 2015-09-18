/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Andre Justo
 */
package org.nuxeo.runtime.services.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @since 7.4
 */
public class TestConfigurationService extends NXRuntimeTestCase {

    ConfigurationService cs;

    @Before
    public void setUp() throws Exception {
        super.setUp();
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
        deployContrib("org.nuxeo.runtime.test.tests", "configuration-test-contrib.xml");
        assertNotNull(cs.getProperty("nuxeo.test.dummyBooleanProperty"));
        assertNotNull(cs.getProperty("nuxeo.test.anotherDummyBooleanProperty"));
        assertNotNull(cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertNull(cs.getProperty("nuxeo.test.invalidDummyProperty"));
    }

    @Test
    public void testProperties() throws Exception {
        deployContrib("org.nuxeo.runtime.test.tests", "configuration-test-contrib.xml");
        assertTrue(cs.isBooleanPropertyTrue("nuxeo.test.dummyBooleanProperty"));
        assertFalse(cs.isBooleanPropertyTrue("nuxeo.test.anotherDummyBooleanProperty"));
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
    }

    @Test
    public void testOverride() throws Exception {
        // Deploy contribution with properties
        deployContrib("org.nuxeo.runtime.test.tests", "configuration-test-contrib.xml");
        // Assert property has overridden value
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
        // Assert property don't exist
        assertNull(cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Deploy another contrib with a new property and override existing properties
        deployContrib("org.nuxeo.runtime.test.tests", "configuration-override-contrib.xml");
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
        undeployContrib("org.nuxeo.runtime.test.tests", "configuration-override-contrib.xml");
        // Assert property was removed
        assertNull(cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Assert overridden values were restored
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertTrue(cs.isBooleanPropertyTrue("nuxeo.test.dummyBooleanProperty"));
    }

    @Test
    public void testCompatWarn() throws Exception {
        Framework.getProperties().setProperty("nuxeo.test.dummyStringProperty", "anotherDummyValue");
        assertEquals(0, Framework.getRuntime().getWarnings().size());
        // Deploy contribution with properties
        deployContrib("org.nuxeo.runtime.test.tests", "configuration-test-contrib.xml");
        assertEquals(1, Framework.getRuntime().getWarnings().size());
        assertEquals(
                "Property 'nuxeo.test.dummyStringProperty' should now be contributed to "
                        + "extension point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'",
                Framework.getRuntime().getWarnings().get(0));
        Framework.getProperties().remove("nuxeo.test.dummyStringProperty");
    }

}
