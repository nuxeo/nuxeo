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

import static java.util.function.Predicate.isEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.Environment.NUXEO_HOME;
import static org.nuxeo.launcher.config.ConfigurationConstants.ENV_NUXEO_PROFILES;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_BIND_ADDRESS;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_FORCE_GENERATION;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_LOOPBACK_URL;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_NUXEO_CONF;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_TEMPLATES_NAME;
import static org.nuxeo.launcher.config.ConfigurationConstants.PARAM_TEMPLATE_DBTYPE;
import static org.nuxeo.launcher.config.ConfigurationGenerator.JAVA_OPTS_PROP;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.common.Environment;

public class ConfigurationGeneratorTest {

    @Rule
    public final ConfigurationRule rule = new ConfigurationRule("configuration/generator");

    @Before
    public void setUp() {
        // have to set the home property for Environment class
        System.setProperty(NUXEO_HOME, rule.getNuxeoHome().toString());
    }

    @After
    public void tearDown() {
        System.clearProperty(NUXEO_HOME);
    }

    @Test
    public void testDefault() {
        var generator = generatorBuilder().build();
        assertTrue(generator.init());
        assertEquals("true", generator.getUserConfig().getProperty("nuxeo.test.default.home"));
        // check nuxeo.conf has been set by generator
        assertEquals(rule.getNuxeoConf().toString(), generator.systemProperties.getProperty(PARAM_NUXEO_CONF));
    }

    @Test
    public void getJavaOptsStringWithoutConfig() {
        var generator = generatorBuilder().init(true).build();
        assertThat(generator.getJavaOptsString()).isEmpty();
    }

    @Test
    public void getJavaOptsWithoutConfig() {
        var generator = generatorBuilder().init(true).build();
        List<String> javaOpts = generator.getJavaOpts(Function.identity());
        assertThat(javaOpts).containsExactly("");
    }

    @Test
    public void getJavaOptsWithConfig() {
        var generator = generatorBuilder().init(true).putSystemProperty(JAVA_OPTS_PROP, "-Xms1g -Xmx2g").build();
        List<String> javaOpts = generator.getJavaOpts(Function.identity());
        assertThat(javaOpts).containsExactly("-Xms1g", "-Xmx2g");
    }

    @Test
    public void getJavaOptsWithMultivaluedProperty() {
        var generator = generatorBuilder().init(true)
                                          .putSystemProperty(JAVA_OPTS_PROP, "-Da=\"a1 a2\" -Db=\"b1 b2\"")
                                          .build();
        List<String> javaOpts = generator.getJavaOpts(Function.identity());
        assertThat(javaOpts).containsExactly("-Da=\"a1 a2\"", "-Db=\"b1 b2\"");
    }

    @Test
    public void testEvalDynamicProperties() {
        // IPv4
        var generator = generatorBuilder().init(true).build();
        assertEquals("http://127.0.0.1:8080/nuxeo", generator.getUserConfig().getProperty(PARAM_LOOPBACK_URL));

        generator = generatorBuilder().init(true).putSystemProperty(PARAM_BIND_ADDRESS, "10.213.2.105").build();
        assertEquals("http://10.213.2.105:8080/nuxeo", generator.getUserConfig().getProperty(PARAM_LOOPBACK_URL));

        // IPv6
        generator = generatorBuilder().init(true)
                                      .putSystemProperty(PARAM_BIND_ADDRESS, "::")
                                      .putSystemProperty("java.net.preferIPv4Stack", "false")
                                      .putSystemProperty("java.net.preferIPv6Addresses", "true")
                                      .build();
        assertEquals("http://[0:0:0:0:0:0:0:1]:8080/nuxeo", generator.getUserConfig().getProperty(PARAM_LOOPBACK_URL));

        generator = generatorBuilder().init(true)
                                      .putSystemProperty(PARAM_BIND_ADDRESS, "2a01:240:fe8e::226:bbff:fe09:55cd")
                                      .putSystemProperty("java.net.preferIPv4Stack", "false")
                                      .putSystemProperty("java.net.preferIPv6Addresses", "true")
                                      .build();
        assertEquals("http://[2a01:240:fe8e:0:226:bbff:fe09:55cd]:8080/nuxeo",
                generator.getUserConfig().getProperty(PARAM_LOOPBACK_URL));
    }

    @Test
    public void testSetProperty() throws ConfigurationException {
        var generator = generatorBuilder().init(true).build();

        final String testProperty = "test.prop.key";
        String oldValue = generator.setProperty(testProperty, "test.prop.value");
        assertNull("Wrong old value", oldValue);
        assertEquals("Property not set", "test.prop.value", generator.getUserConfig().getProperty(testProperty));
        oldValue = generator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertNull("Property not unset", generator.getUserConfig().getProperty(testProperty));
        oldValue = generator.setProperty(testProperty, "");
        assertNull("Wrong old value", oldValue);
        assertNull("Property must not be set", generator.getUserConfig().getProperty(testProperty));
        generator.setProperty(testProperty, "test.prop.value");
        oldValue = generator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "test.prop.value", oldValue);
        assertNull("Property not unset", generator.getUserConfig().getProperty(testProperty));
    }

    /**
     * According to {@link ConfigurationGenerator#saveConfiguration(Map, boolean, boolean)}: <br>
     * <q>{@link ConfigurationConstants#PARAM_TEMPLATES_NAME} and {@link ConfigurationConstants#PARAM_FORCE_GENERATION}
     * cannot be unset</q>
     *
     * <pre>
     * nuxeo.templates=default,common
     * nuxeo.force.generation=true
     * </pre>
     */
    @Test
    public void testSetSpecialProperties() throws ConfigurationException {
        var generator = generatorBuilder().init(true).build();

        String oldValue = generator.setProperty(PARAM_TEMPLATES_NAME, null);
        assertEquals("Wrong old value", "default,common", oldValue);
        assertEquals(PARAM_TEMPLATES_NAME + " should be reset", "default",
                generator.getUserConfig().getProperty(PARAM_TEMPLATES_NAME));
        generator.setProperty(PARAM_TEMPLATES_NAME, oldValue);

        generator.setProperty(PARAM_TEMPLATES_NAME, "");
        assertEquals(PARAM_TEMPLATES_NAME + " should be reset", "default",
                generator.getUserConfig().getProperty(PARAM_TEMPLATES_NAME));

        generator.setProperty(PARAM_TEMPLATES_NAME, oldValue);
        assertEquals(PARAM_TEMPLATES_NAME + " should be modifiable", oldValue,
                generator.getUserConfig().getProperty(PARAM_TEMPLATES_NAME));

        oldValue = generator.setProperty(PARAM_FORCE_GENERATION, null);
        assertEquals("Wrong old value", "true", oldValue);
        assertEquals("Property should not be unset", oldValue,
                generator.getUserConfig().getProperty(PARAM_FORCE_GENERATION));

        generator.setProperty(PARAM_FORCE_GENERATION, "");
        assertEquals("Property should not be unset", oldValue,
                generator.getUserConfig().getProperty(PARAM_FORCE_GENERATION));

        generator.setProperty(PARAM_FORCE_GENERATION, "false");
        assertEquals(PARAM_FORCE_GENERATION + " should not be modifiable like this", oldValue,
                generator.getUserConfig().getProperty(PARAM_FORCE_GENERATION));
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
        var generator = generatorBuilder().init(true).build();

        final String testProperty = "test.sampled.prop";
        String oldValue = generator.setProperty(testProperty, "anotherValue");
        assertNull("Wrong old value", oldValue);
        assertEquals("Property not set", "anotherValue", generator.getUserConfig().getProperty(testProperty));
        oldValue = generator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "anotherValue", oldValue);
        assertNull("Property not unset", generator.getUserConfig().getProperty(testProperty));
        oldValue = generator.setProperty(testProperty, "");
        assertNull("Wrong old value", oldValue);
        assertNull("Property must not be set", generator.getUserConfig().getProperty(testProperty));
        generator.setProperty(testProperty, "someValue");
        oldValue = generator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertNull("Property not unset", generator.getUserConfig().getProperty(testProperty));
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
        var generator = generatorBuilder().init(true).build();

        final String testProperty = "test.sampled.prop2";
        String oldValue = generator.setProperty(testProperty, "anotherValue");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertEquals("Property not set", "anotherValue", generator.getUserConfig().getProperty(testProperty));
        oldValue = generator.setProperty(testProperty, null);
        assertEquals("Wrong old value", "anotherValue", oldValue);
        assertNull("Property not unset", generator.getUserConfig().getProperty(testProperty));
        oldValue = generator.setProperty(testProperty, "");
        assertNull("Wrong old value", oldValue);
        assertNull("Property must not be set", generator.getUserConfig().getProperty(testProperty));
        generator.setProperty(testProperty, "someValue");
        oldValue = generator.setProperty(testProperty, "");
        assertEquals("Wrong old value", "someValue", oldValue);
        assertNull("Property not unset", generator.getUserConfig().getProperty(testProperty));
    }

    @Test
    public void testAddRmTemplate() throws ConfigurationException {
        var generator = generatorBuilder().init(true).build();

        String originalTemplates = generator.getUserConfig().getProperty(PARAM_TEMPLATES_NAME);
        assertEquals("Error calculating db template", "default",
                generator.getUserConfig().getProperty(PARAM_TEMPLATE_DBTYPE));

        // newTemplate will include postgresql
        generator.addTemplate("newTemplate");
        assertEquals("Error calculating db template", "postgresql",
                generator.getUserConfig().getProperty(PARAM_TEMPLATE_DBTYPE));
        assertEquals("newTemplate not added", originalTemplates + ",newTemplate",
                generator.getUserConfig().getProperty(PARAM_TEMPLATES_NAME));

        generator.rmTemplate("newTemplate");
        assertEquals("Error calculating db template", "default",
                generator.getUserConfig().getProperty(PARAM_TEMPLATE_DBTYPE));
        assertEquals("newTemplate not removed", originalTemplates,
                generator.getUserConfig().getProperty(PARAM_TEMPLATES_NAME));
    }

    @Test
    public void testFreemarkerTemplate() throws ConfigurationException, IOException {
        var generator = generatorBuilder().build();

        generator.run();
        Path out = generator.getNuxeoHome().toPath().resolve("test-freemarker");
        assertTrue(Files.exists(out));
        String fileContents = Files.readString(out).trim();
        assertEquals(fileContents, "Success");

        var expectedConfiguration = rule.getResourcePath("testFreemarkerTemplate-configuration.properties");
        var actualConfiguration = generator.getDumpedConfig().toPath();
        List<String> actualLines = Files.readAllLines(actualConfiguration);
        assertFalse("The generated configuration.properties is empty", actualLines.isEmpty());
        // second line is a date - remove it as we don't control the clock
        assertTrue(actualLines.remove(1).startsWith("#"));
        // remove status key which is random
        assertTrue(actualLines.removeIf(line -> line.startsWith(Environment.SERVER_STATUS_KEY)));
        // paths are absolute to the computer - make them absolute to nuxeo home
        String toRemove = generator.getNuxeoHome().getParent();
        actualLines.replaceAll(line -> line.replace(toRemove, ""));

        String expected = Files.readString(expectedConfiguration);
        // add a new line at the end
        String actual = String.join(System.lineSeparator(), actualLines) + System.lineSeparator();
        assertEquals(expected, actual);
    }

    /**
     * NXP-22031 - test the configuration reloading after wizard setup when using Nuxeo GUI launcher.
     */
    @Test
    public void testReloadConfigurationWhenConfigurationFileWasEditedByAnotherGenerator() throws Exception {
        var generator = generatorBuilder().init(true).build();
        var generator2 = generatorBuilder().init(true).build();
        // Update template - write it to nuxeo.conf
        generator.saveConfiguration(Collections.singletonMap(PARAM_TEMPLATES_NAME, "default,mongodb"));

        // Test configuration generator context before reloading it
        assertEquals("default", generator2.getUserConfig().getProperty(PARAM_TEMPLATES_NAME));

        // Reload it
        // At this point we test that we flush correctly the configuration generator context
        assertTrue(generator2.init(true));

        // Check values
        assertEquals("default,mongodb", generator2.getUserConfig().getProperty(PARAM_TEMPLATES_NAME));
    }

    @Test
    public void testIncludeProfile() throws Exception {
        String profileToTest = "testprofile";

        // test without NUXEO_PROFILES environment variable
        ConfigurationGenerator generator = generatorBuilder().init(true).build();
        assertFalse("Profile should not be included", isTemplateIncluded(generator, profileToTest));
        assertNotEquals("true", generator.getUserConfig().getProperty("nuxeo.profile.added.by.test"));

        // test with NUXEO_PROFILES environment variable
        generator = generatorBuilder().environment(Map.of(ENV_NUXEO_PROFILES, profileToTest)).init(true).build();
        assertTrue("Profile should be included", isTemplateIncluded(generator, profileToTest));
        assertEquals("true", generator.getUserConfig().getProperty("nuxeo.profile.added.by.test"));
    }

    protected boolean isTemplateIncluded(ConfigurationGenerator generator, String template) {
        return generator.getIncludedTemplates().stream().map(File::getName).anyMatch(isEqual(template));
    }

    protected ConfigurationGenerator.Builder generatorBuilder() {
        // protect System properties - putAll usage on purpose for loadConfiguration to work correctly
        var systemProperties = new Properties();
        systemProperties.putAll(System.getProperties());
        return ConfigurationGenerator.builder().systemProperties(systemProperties);
    }
}
