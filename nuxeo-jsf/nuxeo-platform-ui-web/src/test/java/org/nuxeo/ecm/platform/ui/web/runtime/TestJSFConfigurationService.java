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
package org.nuxeo.ecm.platform.ui.web.runtime;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 7.4
 */
public class TestJSFConfigurationService extends NXRuntimeTestCase {

    JSFConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.ui");
        configurationService = Framework.getService(JSFConfigurationService.class);
        assertNotNull(configurationService);
    }

    @Test
    public void testExtensionPoint() throws Exception {
        // Assert properties don't exist
        assertNull(configurationService.getProperty("nuxeo.test.jsf.dummyBooleanProperty"));
        assertNull(configurationService.getProperty("nuxeo.test.jsf.anotherDummyBooleanProperty"));
        assertNull(configurationService.getProperty("nuxeo.test.jsf.dummyStringProperty"));
        // Deploy contribution with properties
        deployContrib("org.nuxeo.ecm.platform.ui.test", "OSGI-INF/jsfconfiguration-test-contrib.xml");
        assertNotNull(configurationService.getProperty("nuxeo.test.jsf.dummyBooleanProperty"));
        assertNotNull(configurationService.getProperty("nuxeo.test.jsf.anotherDummyBooleanProperty"));
        assertNotNull(configurationService.getProperty("nuxeo.test.jsf.dummyStringProperty"));
        assertNotNull(configurationService.getProperty("nuxeo.test.jsf.dummyStaticProperty"));
        assertNull(configurationService.getProperty("nuxeo.test.jsf.invalidDummyProperty"));
    }

    @Test
    public void testProperties() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.ui.test", "OSGI-INF/jsfconfiguration-test-contrib.xml");
        assertTrue(configurationService.isBooleanPropertyTrue("nuxeo.test.jsf.dummyBooleanProperty"));
        assertFalse(configurationService.isBooleanPropertyTrue("nuxeo.test.jsf.anotherDummyBooleanProperty"));
        assertEquals("dummyValue", configurationService.getProperty("nuxeo.test.jsf.dummyStringProperty"));
        assertEquals("staticValue", configurationService.getProperty("nuxeo.test.jsf.dummyStaticProperty"));
    }

    @Test
    public void testOverride() throws Exception {
        // Override default value
        Framework.getProperties().setProperty("nuxeo.test.jsf.dummyStringProperty", "anotherDummyValue");
        // Deploy contribution with properties
        deployContrib("org.nuxeo.ecm.platform.ui.test", "OSGI-INF/jsfconfiguration-test-contrib.xml");
        // Assert property has overridden value
        assertEquals("anotherDummyValue", configurationService.getProperty("nuxeo.test.jsf.dummyStringProperty"));
        // Assert property don't exist
        assertNull(configurationService.getProperty("nuxeo.test.jsf.overrideContribDummyProperty"));
        // Deploy another contrib with a new property and override existing properties
        deployContrib("org.nuxeo.ecm.platform.ui.test", "OSGI-INF/jsfconfiguration-override-contrib.xml");
        // Assert new property was added
        assertEquals("overrideContrib", configurationService.getProperty("nuxeo.test.jsf.overrideContribDummyProperty"));
        // Assert old properties have overridden values
        assertEquals("dummyStringValueOverridden", configurationService.getProperty("nuxeo.test.jsf.dummyStringProperty"));
        assertTrue(configurationService.isBooleanPropertyFalse("nuxeo.test.jsf.dummyBooleanProperty"));

        // Undeploy contrib
        undeployContrib("org.nuxeo.ecm.platform.ui.test", "OSGI-INF/jsfconfiguration-override-contrib.xml");
        // Assert property was removed
        assertNull(configurationService.getProperty("nuxeo.test.jsf.overrideContribDummyProperty"));
        // Assert overridden values were restored
        assertEquals("anotherDummyValue", configurationService.getProperty("nuxeo.test.jsf.dummyStringProperty"));
        assertTrue(configurationService.isBooleanPropertyTrue("nuxeo.test.jsf.dummyBooleanProperty"));
    }
}
