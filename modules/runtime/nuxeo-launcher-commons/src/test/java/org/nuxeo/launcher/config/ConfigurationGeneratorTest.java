/*
 * (C) Copyright 2011-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Frantz Fischer <ffischer@nuxeo.com>
 */
package org.nuxeo.launcher.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationGeneratorTest extends AbstractConfigurationTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(getResourceFile("templates/jboss"), new File(nuxeoHome, "templates"));
        setSystemProperty("jboss.home.dir", nuxeoHome.getPath());
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        log.debug("Test with {}",
                () -> configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_BIND_ADDRESS));
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
        // env.clear();
    }

    @Test
    public void getJavaOptsStringWithoutConfig() {
        assertThat(configGenerator.getJavaOptsString()).isEmpty();
    }

    @Test
    public void getJavaOptsWithoutConfig() {
        List<String> javaOpts = configGenerator.getJavaOpts(Function.identity());
        assertThat(javaOpts).containsExactly("");
    }

    @Test
    public void getJavaOptsWithConfig() {
        setSystemProperty(ConfigurationGenerator.JAVA_OPTS_PROP, "-Xms1g -Xmx2g");
        List<String> javaOpts = configGenerator.getJavaOpts(Function.identity());
        assertThat(javaOpts).containsExactly("-Xms1g", "-Xmx2g");
    }

    @Test
    public void getJavaOptsWithMultivaluedProperty() {
        setSystemProperty(ConfigurationGenerator.JAVA_OPTS_PROP, "-Da=\"a1 a2\" -Db=\"b1 b2\"");
        List<String> javaOpts = configGenerator.getJavaOpts(Function.identity());
        assertThat(javaOpts).containsExactly("-Da=\"a1 a2\"", "-Db=\"b1 b2\"");
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
        setSystemProperty("java.net.preferIPv4Stack", "false");
        setSystemProperty("java.net.preferIPv6Addresses", "true");
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
        log.debug("Test with {}",
                () -> configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_BIND_ADDRESS));
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
        assertNull("Wrong old value", oldValue);
        assertEquals("Property not set", "test.prop.value", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertNull("Property not unset", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, "");
        assertNull("Wrong old value", oldValue);
        assertNull("Property must not be set", configGenerator.getUserConfig().getProperty(testProperty));
        configGenerator.setProperty(testProperty, "test.prop.value");
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertNull("Property not unset", configGenerator.getUserConfig().getProperty(testProperty));
    }

    /**
     * According to {@link ConfigurationGenerator#saveConfiguration(Map, boolean, boolean)}: <br>
     * <q>{@link ConfigurationGenerator#PARAM_TEMPLATES_NAME} and {@link ConfigurationGenerator#PARAM_FORCE_GENERATION}
     * cannot be unset</q>
     *
     * <pre>
     * nuxeo.templates=default,common,testinclude
     * nuxeo.force.generation=true
     * </pre>
     */
    @Test
    public void testSetSpecialProperties() throws ConfigurationException {
        String oldValue = configGenerator.setProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, null);
        assertEquals("Wrong old value", "default,common,testinclude,testenv,backing", oldValue);
        assertEquals(ConfigurationGenerator.PARAM_TEMPLATES_NAME + " should be reset", "default",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        configGenerator.setProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, oldValue);
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
        assertNull("Wrong old value", oldValue);
        assertEquals("Property not set", "anotherValue", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "anotherValue", oldValue);
        assertNull("Property not unset", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, "");
        assertNull("Wrong old value", oldValue);
        assertNull("Property must not be set", configGenerator.getUserConfig().getProperty(testProperty));
        configGenerator.setProperty(testProperty, "someValue");
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertNull("Property not unset", configGenerator.getUserConfig().getProperty(testProperty));
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
        assertNull("Property not unset", configGenerator.getUserConfig().getProperty(testProperty));
        oldValue = configGenerator.setProperty(testProperty, "");
        assertNull("Wrong old value", oldValue);
        assertNull("Property must not be set", configGenerator.getUserConfig().getProperty(testProperty));
        configGenerator.setProperty(testProperty, "someValue");
        oldValue = configGenerator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertNull("Property not unset", configGenerator.getUserConfig().getProperty(testProperty));
    }

    @Test
    public void testAddRmTemplate() throws ConfigurationException {
        String originalTemplates = configGenerator.getUserConfig()
                                                  .getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        assertEquals("Error calculating db template", "default",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBTYPE));
        configGenerator.addTemplate("newTemplate");
        assertEquals("Error calculating db template", "postgresql",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBTYPE));
        assertEquals("newTemplate not added", originalTemplates + ",newTemplate",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        configGenerator.rmTemplate("newTemplate");
        assertEquals("Error calculating db template", "default",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBTYPE));
        assertEquals("newTemplate not removed", originalTemplates,
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
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
        String fileContents = FileUtils.readFileToString(outfile, UTF_8).trim();
        assertEquals(fileContents, "Success");
    }

    /**
     * NXP-22031 - test the configuration reloading after wizard setup when using Nuxeo GUI launcher.
     */
    @Test
    public void testReloadConfigurationWhenConfigurationFileWasEditedByAnotherGenerator() throws Exception {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        // Update template - write it to nuxeo.conf
        configGenerator.saveConfiguration(
                Collections.singletonMap(ConfigurationGenerator.PARAM_TEMPLATES_NAME, "default,mongodb"));

        // Test configuration generator context before reloading it
        // getUserTemplates lazy load templates in the configuration generator context and put it back to userConfig
        // That's explain the two assertions below
        assertEquals("default,common,testinclude,testenv,backing",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("default,common,testinclude,testenv,backing",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));

        // Reload it
        // At this point we test that we flush correctly the configuration generator context
        assertTrue(configGenerator.init(true));

        // Check values
        // userConfig was filled with values from nuxeo.conf and getUserTemplates re-load templates from userConfig
        assertEquals("default,mongodb",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("default,mongodb",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
    }

    @Test
    public void testIncludeProfile() {
        String profileToTest = "testprofile";
        assertFalse("Profile should not be included", isTemplateIncluded(profileToTest));
        assertNotEquals("true", configGenerator.getUserConfig().getProperty("nuxeo.profile.added.by.test"));

        // env.put(NUXEO_PROFILES, profileToTest);

        configGenerator.init(true);

        assertTrue("Profile should be included", isTemplateIncluded(profileToTest));
        assertEquals("true", configGenerator.getUserConfig().getProperty("nuxeo.profile.added.by.test"));
    }

    protected boolean isTemplateIncluded(String template) {
        return configGenerator.getIncludedTemplates().stream().map(File::getName).anyMatch(isEqual(template));
    }
}
