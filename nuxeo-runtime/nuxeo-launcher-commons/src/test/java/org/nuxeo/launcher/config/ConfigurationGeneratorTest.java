/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        log.debug("Test with " + configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_BIND_ADDRESS));
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        System.clearProperty("jboss.home.dir");
        System.clearProperty("java.net.preferIPv4Stack");
        System.clearProperty("java.net.preferIPv6Addresses");
    }

    @Test
    public void testEvalDynamicProperties() {
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
        final String testProperty = "test.prop.key";
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        String oldValue = configGenerator.setProperty(testProperty, "test.prop.value");
        assertEquals("Wrong old value", null, oldValue);
        assertEquals("Property not set", "test.prop.value", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", null, oldValue);
        assertEquals("Property must not be set", null, configGenerator.getUserConfig().getProperty(testProperty));
        configGenerator.setProperty(testProperty, "test.prop.value");
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty(testProperty));
    }

    /**
     * According to {@link ConfigurationGenerator#saveConfiguration(Map, boolean, boolean)}:<br>
     * <q>
     * {@link ConfigurationGenerator#PARAM_WIZARD_DONE}, {@link ConfigurationGenerator#PARAM_TEMPLATES_NAME} and
     * {@link ConfigurationGenerator#PARAM_FORCE_GENERATION} cannot be unset</q>
     *
     * <pre>
     * nuxeo.templates=default,common,testinclude
     * nuxeo.wizard.done=false
     * nuxeo.force.generation=true
     * </pre>
     *
     * @throws ConfigurationException
     */
    @Test
    public void testSetSpecialProperties() throws ConfigurationException {
        String oldValue = configGenerator.setProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, null);
        assertEquals("Wrong old value", "default,common,testinclude", oldValue);
        assertEquals(ConfigurationGenerator.PARAM_TEMPLATES_NAME + " should be reset", "default",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        configGenerator.changeTemplates(oldValue);
        configGenerator.setProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        assertEquals(ConfigurationGenerator.PARAM_TEMPLATES_NAME + " should be reset", "default",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        configGenerator.setProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, oldValue);
        assertEquals(ConfigurationGenerator.PARAM_TEMPLATES_NAME + " should be modifiable", oldValue,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));

        oldValue = configGenerator.setProperty(ConfigurationGenerator.PARAM_FORCE_GENERATION, null);
        assertEquals("Wrong old value", "true", oldValue);
        assertEquals("Property should not be unset", oldValue,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_FORCE_GENERATION));
        configGenerator.setProperty(ConfigurationGenerator.PARAM_FORCE_GENERATION, "");
        assertEquals("Property should not be unset", oldValue,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_FORCE_GENERATION));
        configGenerator.setProperty(ConfigurationGenerator.PARAM_FORCE_GENERATION, "false");
        assertEquals(ConfigurationGenerator.PARAM_FORCE_GENERATION + " should not be modifiable like this", oldValue,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_FORCE_GENERATION));

        oldValue = configGenerator.setProperty(ConfigurationGenerator.PARAM_WIZARD_DONE, null);
        assertEquals("Property should not be unset", oldValue,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_WIZARD_DONE));
        configGenerator.setProperty(ConfigurationGenerator.PARAM_WIZARD_DONE, "" + !Boolean.parseBoolean(oldValue));
        assertNotEquals(ConfigurationGenerator.PARAM_WIZARD_DONE + " should be modifiable", oldValue,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_WIZARD_DONE));
        configGenerator.setProperty(ConfigurationGenerator.PARAM_WIZARD_DONE, "" + oldValue);
        assertEquals(ConfigurationGenerator.PARAM_WIZARD_DONE + " should be modifiable", oldValue,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_WIZARD_DONE));
    }

    /**
     * Test on property "sampled" in nuxeo.conf: already present and commented
     *
     * <pre>
     * #test.sampled.prop=someValue
     * </pre>
     */
    @Test
    public void testSetSampledCommentedProperty() throws ConfigurationException {
        final String testProperty = "test.sampled.prop";
        assertTrue(configGenerator.init());
        String oldValue = configGenerator.setProperty(testProperty, "anotherValue");
        assertEquals("Wrong old value", null, oldValue);
        assertEquals("Property not set", "anotherValue", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "anotherValue", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", null, oldValue);
        assertEquals("Property must not be set", null, configGenerator.getUserConfig().getProperty(testProperty));
        configGenerator.setProperty(testProperty, "someValue");
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty(testProperty));
    }

    /**
     * Test on property "sampled" in nuxeo.conf: already present and not commented
     *
     * <pre>
     * test.sampled.prop2 = someValue
     * </pre>
     */
    @Test
    public void testSetSampledActiveProperty() throws ConfigurationException {
        final String testProperty = "test.sampled.prop2";
        String oldValue = configGenerator.setProperty(testProperty, "anotherValue");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertEquals("Property not set", "anotherValue", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "anotherValue", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", null, oldValue);
        assertEquals("Property must not be set", null, configGenerator.getUserConfig().getProperty(testProperty));
        configGenerator.setProperty(testProperty, "someValue");
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertEquals("Property not unset", null, configGenerator.getUserConfig().getProperty(testProperty));
    }

    @Test
    public void testAddRmTemplate() throws ConfigurationException {
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
        Map<String, String> changedParameters = new HashMap<>();
        changedParameters.put(ConfigurationGenerator.PARAM_WIZARD_DONE, "true");
        configGenerator.saveFilteredConfiguration(changedParameters);
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        assertEquals("true", configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_WIZARD_DONE));
    }

    @Test
    public void testFreemarkerTemplate() throws ConfigurationException, IOException {
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
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        configGenerator.changeDBTemplate("postgresql");
        assertEquals("Failed to change database default to postgresql",
                originalTemplates.replaceFirst("default", "postgresql"), configGenerator.getUserTemplates());
    }

    @Test
    public void testChangeDatabaseFromCustom() throws Exception {
        configGenerator.changeTemplates("testinclude2");
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        configGenerator.changeDBTemplate("postgresql");
        assertEquals("Failed to change database default to postgresql", originalTemplates.concat(",postgresql"),
                configGenerator.getUserTemplates());
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME, "postgresql");
        configGenerator.saveFilteredConfiguration(customParameters);
        // Check stored value
        assertTrue(configGenerator.init(true));
        assertEquals("Failed to change database default to postgresql", originalTemplates.concat(",postgresql"),
                configGenerator.getUserTemplates());
        assertEquals("Failed to change database default to postgresql", originalTemplates.concat(",postgresql"),
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("Failed to change database default to postgresql", "postgresql",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME));
    }

    @Test
    public void testChangeNoSqlDatabase() throws Exception {
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("mongodb");
        assertEquals("Failed to set NoSQL database to mongodb", originalTemplates.concat(",mongodb"),
                configGenerator.getUserTemplates());
    }

    @Test
    public void testChangeNoSqlDatabaseFromCustom() throws Exception {
        configGenerator.changeTemplates("testinclude2");
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("mongodb");
        assertEquals("Failed to set NoSQL database to mongodb", originalTemplates.concat(",mongodb"),
                configGenerator.getUserTemplates());
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBNOSQL_NAME, "mongodb");
        configGenerator.saveFilteredConfiguration(customParameters);
        // Check stored value
        assertTrue(configGenerator.init(true));
        assertEquals("Failed to set NoSQL database to mongodb", originalTemplates.concat(",mongodb"),
                configGenerator.getUserTemplates());
        assertEquals("Failed to set NoSQL database to mongodb", originalTemplates.concat(",mongodb"),
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("Failed to set NoSQL database to mongodb", "mongodb",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNOSQL_NAME));
    }

    @Test
    public void testChangeMarkLogicDatabase() throws Exception {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("marklogic");
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
    }

    @Test
    public void testChangeMarkLogicDatabaseFromCustom() throws Exception {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        configGenerator.changeTemplates("testinclude2");
        String originalTemplates = configGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("marklogic");
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBNOSQL_NAME, "marklogic");
        configGenerator.saveFilteredConfiguration(customParameters);
        // Check stored value
        assertTrue(configGenerator.init(true));
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("Failed to set NoSQL database to marklogic", "marklogic",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBNOSQL_NAME));
    }

}
