/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.launcher.commons.text.TextTemplate;

public class ConfigurationGeneratorTest extends AbstractConfigurationTest {
    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(getResourceFile("templates/jboss"), new File(nuxeoHome, "templates"));
        System.setProperty("jboss.home.dir", nuxeoHome.getPath());
    }

    @Test
    public void testEvalDynamicProperties() {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        assertEquals("Bad loop back URL", "http://127.0.0.1:8080/nuxeo",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_LOOPBACK_URL));
        try {
            testAddress("10.213.2.105", "http://10.213.2.105:8080/nuxeo");
        } catch (ConfigurationException e) {
            log.error(e);
        }
        log.debug("Force IPv6");
        System.setProperty("java.net.preferIPv4Stack", "false");
        System.setProperty("java.net.preferIPv6Addresses", "true");
        try {
            testAddress("::", "http://[0:0:0:0:0:0:0:1]:8080/nuxeo");
        } catch (ConfigurationException e) {
            log.error(e);
        }
        try {
            testAddress("2a01:240:fe8e::226:bbff:fe09:55cd", "http://[2a01:240:fe8e:0:226:bbff:fe09:55cd]:8080/nuxeo");
        } catch (ConfigurationException e) {
            log.error(e);
        }
    }

    private void testAddress(String bindAddress, String expectedLoopback) throws ConfigurationException {
        configGenerator.setProperty(ConfigurationGenerator.PARAM_BIND_ADDRESS, bindAddress);
        log.debug("Test with " + configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_BIND_ADDRESS));
        configGenerator.init(true);
        assertEquals("Bad loop back URL", expectedLoopback,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_LOOPBACK_URL));
    }

    @Test
    public void testSetProperty() throws ConfigurationException {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        String oldValue = configGenerator.setProperty("test.prop.key", "test.prop.value");
        assertEquals("Wrong old value", null, oldValue);
        assertEquals("Property not set", "test.prop.value",
                configGenerator.getUserConfig().getProperty("test.prop.key"));
        oldValue = configGenerator.setProperty("test.prop.key", null);
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty("test.prop.key"));
        oldValue = configGenerator.setProperty("test.prop.key", "");
        assertEquals("Wrong old value", null, oldValue);
        assertEquals("Property must not be set", null, configGenerator.getUserConfig().getProperty("test.prop.key"));
        configGenerator.setProperty("test.prop.key", "test.prop.value");
        oldValue = configGenerator.setProperty("test.prop.key", "");
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty("test.prop.key"));
    }

    @Test
    public void testEscapeVariable() throws ConfigurationException {
        TextTemplate templates = new TextTemplate();
        templates.setVariable("pfouh", "bar");
        assertEquals("bar${pfouh}", templates.processText("${pfouh}$${pfouh}"));
    }

    @Test
    public void testAddRmTemplate() throws ConfigurationException {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        assertEquals("Error calculating db template", "default",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME));
        configGenerator.addTemplate("newTemplate");
        assertEquals("Error calculating db template", "postgresql",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME));
        assertEquals("newTemplate not added", originalTemplates + ",newTemplate",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        configGenerator.rmTemplate("newTemplate");
        assertEquals("Error calculating db template", "default",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME));
        assertEquals("newTemplate not removed", originalTemplates,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
    }

    @Test
    public void testSetWizardDone() throws ConfigurationException {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        Map<String, String> changedParameters = new HashMap<String, String>();
        changedParameters.put(ConfigurationGenerator.PARAM_WIZARD_DONE, "true");
        configGenerator.saveFilteredConfiguration(changedParameters);
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        assertEquals("true", configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_WIZARD_DONE));
    }

    @Test
    public void testFreemarkerTemplate() throws ConfigurationException, IOException {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        configGenerator.addTemplate("fmtest");
        configGenerator.setProperty("test.freemarker.part1", "tr");
        configGenerator.setProperty("test.freemarker.part2", "ue");
        configGenerator.setProperty("test.freemarker.key", "${test.freemarker.part1}${test.freemarker.part2}");
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        configGenerator.run();
        File outfile = new File(nuxeoHome, "testfm");
        assertTrue(outfile.exists());
        String fileContents = FileUtils.readFileToString(outfile).trim();
        assertEquals(fileContents, "Success");
    }

    @Test
    public void testChangeDatabase() throws Exception {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        configGenerator.changeDBTemplate("postgresql");
        assertEquals("Failed to change database default to postgresql",
                originalTemplates.replaceFirst("default", "postgresql"), configGenerator.getUserTemplates());
    }

    @Test
    public void testChangeDatabaseFromCustom() throws Exception {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        configGenerator.changeTemplates("testinclude2");
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        configGenerator.changeDBTemplate("postgresql");
        assertEquals("Failed to change database default to postgresql", originalTemplates + ",postgresql",
                configGenerator.getUserTemplates());
        Map<String, String> customParameters = new HashMap<String, String>();
        customParameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME, "postgresql");
        configGenerator.saveFilteredConfiguration(customParameters);
        // Check stored value
        assertTrue(configGenerator.init(true));
        assertEquals("Failed to change database default to postgresql", originalTemplates + ",postgresql",
                configGenerator.getUserTemplates());
        assertEquals("Failed to change database default to postgresql", originalTemplates + ",postgresql",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("Failed to change database default to postgresql", "postgresql",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME));
    }
}
