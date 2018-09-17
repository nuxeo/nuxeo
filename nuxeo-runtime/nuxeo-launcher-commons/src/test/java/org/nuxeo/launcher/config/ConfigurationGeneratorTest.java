/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.launcher.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.launcher.config.ConfigurationGenerator.JVMCHECK_FAIL;
import static org.nuxeo.launcher.config.ConfigurationGenerator.JVMCHECK_NOFAIL;
import static org.nuxeo.launcher.config.ConfigurationGenerator.JVMCHECK_PROP;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationGeneratorTest extends AbstractConfigurationTest {

    Map<String, String> env = new HashMap<>();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        env.put("NUXEO_DB_HOST", "10.0.0.1");
        FileUtils.copyDirectory(getResourceFile("templates/jboss"), new File(nuxeoHome, "templates"));
        setSystemProperty("jboss.home.dir", nuxeoHome.getPath());
        configGenerator = new ConfigurationGenerator() {

            @Override
            protected String getEnvironmentVariableValue(String key) {
                return env.get(key);
            }

        };
        assertTrue(configGenerator.init());
        log.debug(
                "Test with " + configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_BIND_ADDRESS));
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
        log.debug(
                "Test with " + configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_BIND_ADDRESS));
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
     * <q>{@link ConfigurationGenerator#PARAM_WIZARD_DONE}, {@link ConfigurationGenerator#PARAM_TEMPLATES_NAME} and
     * {@link ConfigurationGenerator#PARAM_FORCE_GENERATION} cannot be unset</q>
     *
     * <pre>
     * nuxeo.templates=default,common,testinclude
     * nuxeo.wizard.done=false
     * nuxeo.force.generation=true
     * </pre>
     */
    @Test
    public void testSetSpecialProperties() throws ConfigurationException {
        String oldValue = configGenerator.setProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, null);
        assertEquals("Wrong old value", "default,common,testinclude,testenv,backing", oldValue);
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
        String fileContents = FileUtils.readFileToString(outfile, UTF_8).trim();
        assertEquals(fileContents, "Success");
    }

    @Test
    public void testChangeDatabase() {
        String originalTemplates = configGenerator.getUserConfig()
                                                  .getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        configGenerator.changeDBTemplate("postgresql");
        assertEquals("Failed to change database default to postgresql",
                originalTemplates.replaceFirst("default", "postgresql"), configGenerator.getUserTemplates());
    }

    @Test
    public void testChangeDatabaseFromCustom() throws Exception {
        configGenerator.changeTemplates("testinclude2");
        String originalTemplates = configGenerator.getUserConfig()
                                                  .getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME);
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
    public void testChangeSecondaryDatabase() {
        String originalTemplates = configGenerator.getUserConfig()
                                                  .getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("marklogic");
        assertEquals("Failed to set secondary database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
    }

    @Test
    public void testChangeSecondaryDatabaseFromCustom() throws Exception {
        configGenerator.changeTemplates("testinclude2");
        String originalTemplates = configGenerator.getUserConfig()
                                                  .getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("marklogic");
        assertEquals("Failed to set Secondary database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBSECONDARY_NAME, "marklogic");
        configGenerator.saveFilteredConfiguration(customParameters);
        // Check stored value
        assertTrue(configGenerator.init(true));
        assertEquals("Failed to set Secondary database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
        assertEquals("Failed to set Secondary database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("Failed to set Secondary database to marklogic", "marklogic",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBSECONDARY_NAME));
    }

    @Test
    public void testChangeMarkLogicDatabase() {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        String originalTemplates = configGenerator.getUserConfig()
                                                  .getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("marklogic");
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
    }

    @Test
    public void testChangeMarkLogicDatabaseFromCustom() throws Exception {
        configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        configGenerator.changeTemplates("testinclude2");
        String originalTemplates = configGenerator.getUserConfig()
                                                  .getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME, "");
        configGenerator.changeDBTemplate("marklogic");
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBSECONDARY_NAME, "marklogic");
        configGenerator.saveFilteredConfiguration(customParameters);
        // Check stored value
        assertTrue(configGenerator.init(true));
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserTemplates());
        assertEquals("Failed to set NoSQL database to marklogic", originalTemplates.concat(",marklogic"),
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
        assertEquals("Failed to set NoSQL database to marklogic", "marklogic",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATE_DBSECONDARY_NAME));
    }

    @Test
    public void testCheckJavaVersionFail() {
        testCheckJavaVersion(true);
    }

    @Test
    public void testCheckJavaVersionNoFail() {
        testCheckJavaVersion(false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongJavaVersionFail() {
        ConfigurationGenerator.checkJavaVersion("1.not-a-version", "1.8.0_40", false, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPreJdk9VersionFail() {
        ConfigurationGenerator.checkJavaVersion("1.not-a-version", "1.8.0_40", false, false);
    }

    @Test
    public void testWrongJavaVersionNoFail() {
        runJVMCheck(false, () -> ConfigurationGenerator.checkJavaVersion("not-a-version", "1.8.0_40", true, true));
    }

    protected void testCheckJavaVersion(boolean fail) {
        runJVMCheck(fail, () -> checkJavaVersions(!fail));
    }

    protected void runJVMCheck(boolean fail, Runnable runnable) {
        String old = System.getProperty(JVMCHECK_PROP);
        try {
            System.setProperty(JVMCHECK_PROP, fail ? JVMCHECK_FAIL : JVMCHECK_NOFAIL);
            runnable.run();
        } finally {
            if (old == null) {
                System.clearProperty(JVMCHECK_PROP);
            } else {
                System.setProperty(JVMCHECK_PROP, old);
            }
        }
    }

    protected void checkJavaVersions(boolean compliant) {
        // ok
        checkJavaVersion(true, "1.7.0_10", "1.7.0_1");
        checkJavaVersion(true, "1.8.0_92", "1.7.0_1");
        checkJavaVersion(true, "1.8.0_40", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_45", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_101", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_400", "1.8.0_40");
        checkJavaVersion(true, "1.8.0_72-internal", "1.8.0_40");
        checkJavaVersion(true, "1.8.0-internal", "1.8.0");
        checkJavaVersion(true, "1.9.0_1", "1.8.0_40");
        // compliant if jvmcheck=nofail
        checkJavaVersion(compliant, "1.7.0_1", "1.8.0_40");
        checkJavaVersion(compliant, "1.7.0_40", "1.8.0_40");
        checkJavaVersion(compliant, "1.7.0_101", "1.8.0_40");
        checkJavaVersion(compliant, "1.7.0_400", "1.8.0_40");
        checkJavaVersion(compliant, "1.8.0_1", "1.8.0_40");
        checkJavaVersion(compliant, "1.8.0_25", "1.8.0_40");
        checkJavaVersion(compliant, "1.8.0_39", "1.8.0_40");
    }

    protected void checkJavaVersion(boolean compliant, String version, String requiredVersion) {
        assertEquals(version + " vs " + requiredVersion, compliant,
                ConfigurationGenerator.checkJavaVersion(version, requiredVersion, true, false));
    }

    @Test
    public void testParseJVMVersion() throws Exception {
        checkParsed("7.10", "1.7.0_10");
        checkParsed("8.45", "1.8.0_45");
        checkParsed("8.72", "1.8.0_72-internal");
        checkParsed("9.0", "9");
        checkParsed("9.0", "9.0");
        checkParsed("9.0", "9.0.1");
        checkParsed("9.0", "9.0.1.15");
        checkParsed("9.4", "9.4.5.6");
        checkParsed("10.0", "10.0.1");
        checkParsed("15.0", "15");
    }

    protected void checkParsed(String expected, String version) throws Exception {
        JVMVersion v = JVMVersion.parse(version);
        assertEquals(expected, v.toString());
    }

    @Test
    public void testCheckJavaVersionCompliant() throws Exception {
        final LogCaptureAppender logCaptureAppender = new LogCaptureAppender(Level.WARN);
        Logger.getRootLogger().addAppender(logCaptureAppender);
        try {
            // Nuxeo 6.0 case
            ConfigurationGenerator.checkJavaVersion("1.7.0_10", new String[] { "1.7.0_1", "1.8.0_1" });
            assertTrue(logCaptureAppender.isEmpty());
            ConfigurationGenerator.checkJavaVersion("1.8.0_92", new String[] { "1.7.0_1", "1.8.0_1" });
            assertTrue(logCaptureAppender.isEmpty());
            // Nuxeo 7.10/8.10 case
            ConfigurationGenerator.checkJavaVersion("1.8.0_50", new String[] { "1.8.0_40" });
            assertTrue(logCaptureAppender.isEmpty());

            // may log warn message cases
            ConfigurationGenerator.checkJavaVersion("1.8.0_92", new String[] { "1.7.0_1" });
            assertEquals(1, logCaptureAppender.size());
            assertEquals("Nuxeo requires Java 1.7.0_1+ (detected 1.8.0_92).", logCaptureAppender.get(0));
            logCaptureAppender.clear();

            ConfigurationGenerator.checkJavaVersion("1.8.0_92", new String[] { "1.6.0_1", "1.7.0_1" });
            assertEquals(1, logCaptureAppender.size());
            assertEquals("Nuxeo requires Java 1.7.0_1+ (detected 1.8.0_92).", logCaptureAppender.get(0));
            logCaptureAppender.clear();

            // jvmcheck=nofail case
            runJVMCheck(false, () -> {
                try {
                    ConfigurationGenerator.checkJavaVersion("1.6.0_1", new String[] { "1.7.0_1" });
                    assertEquals(1, logCaptureAppender.size());
                    assertEquals("Nuxeo requires Java 1.7.0_1+ (detected 1.6.0_1).", logCaptureAppender.get(0));
                    logCaptureAppender.clear();
                } catch (Exception e) {
                    fail("Exception thrown " + e.getMessage());
                }
            });

            // fail case
            try {
                ConfigurationGenerator.checkJavaVersion("1.6.0_1", new String[] { "1.7.0_1" });
            } catch (ConfigurationException ce) {
                assertEquals(
                        "Nuxeo requires Java {1.7.0_1} (detected 1.6.0_1). See 'jvmcheck' option to bypass version check.",
                        ce.getMessage());
            }
        } finally {
            Logger.getRootLogger().removeAppender(logCaptureAppender);
        }
    }

    @Test
    public void testEnvironmentVariablesExpansion() {

        // Nominal case
        assertEquals("10.0.0.1", configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_DB_HOST));

        // No env variable with no default value
        assertNull(configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_DB_JDBC_URL));

        // No env variable with default value
        assertEquals("myvalue", configGenerator.getUserConfig().getProperty("nuxeo.default.prop"));

        // Nominal case for boolean env variables
        assertEquals("true", configGenerator.getUserConfig().getProperty("nuxeo.env.prop4"));

        assertEquals("false", configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_FAKE_WINDOWS));

        // Case where only part of the value has to be replaced
        assertEquals("jdbc://10.0.0.1", configGenerator.getUserConfig().getProperty("nuxeo.env.prop2"));

        assertEquals("jdbc://10.0.0.1 false", configGenerator.getUserConfig().getProperty("nuxeo.env.prop3"));
    }

    @Test
    public void testEnvironmentVariableInTemplates() {
        configGenerator.getUserConfig().setProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME,
                "${env:NUXEO_DB_TYPE:default},docker,${env:NUXEO_DB_HOST:docker}");
        assertEquals("default,docker,10.0.0.1", String.join(",", configGenerator.getTemplateList()));
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
        assertEquals("default,common,testinclude,testenv,backing", configGenerator.getUserTemplates());
        assertEquals("default,common,testinclude,testenv,backing",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));

        // Reload it
        // At this point we test that we flush correctly the configuration generator context
        assertTrue(configGenerator.init(true));

        // Check values
        // userConfig was filled with values from nuxeo.conf and getUserTemplates re-load templates from userConfig
        assertEquals("default,mongodb", configGenerator.getUserTemplates());
        assertEquals("default,mongodb",
                configGenerator.getUserConfig().getProperty(ConfigurationGenerator.PARAM_TEMPLATES_NAME));
    }

    @Test
    public void testCheckEncoding() throws Exception {
        Path tempFile = Files.createTempFile("", "",
                PosixFilePermissions.asFileAttribute(new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))));
        // Test UTF8
        Files.write(tempFile, "nuxéo".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        try {
            Charset charset = ConfigurationGenerator.checkFileCharset(tempFile.toFile());
            assertEquals(StandardCharsets.UTF_8, charset);
        } finally {
            Files.deleteIfExists(tempFile);
        }
        // test ISO_8859_1
        Files.write(tempFile, "nuxéo".getBytes(StandardCharsets.ISO_8859_1), StandardOpenOption.CREATE);
        try {
            Charset charset = ConfigurationGenerator.checkFileCharset(tempFile.toFile());
            assertEquals(StandardCharsets.ISO_8859_1, charset);
        } finally {
            Files.deleteIfExists(tempFile);
        }
        // test US_ASCII
        Files.write(tempFile, "nuxeo".getBytes(StandardCharsets.US_ASCII), StandardOpenOption.CREATE);
        try {
            Charset charset = ConfigurationGenerator.checkFileCharset(tempFile.toFile());
            assertEquals(StandardCharsets.US_ASCII, charset);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static class LogCaptureAppender extends AppenderSkeleton {

        private final List<String> messages = new ArrayList<>();

        private final Level level;

        public LogCaptureAppender(Level level) {
            this.level = level;
        }

        @Override
        protected void append(LoggingEvent event) {
            if ("org.nuxeo.launcher.config.ConfigurationGenerator".equals(event.getLoggerName())
                    && level.equals(event.getLevel())) {
                messages.add(event.getRenderedMessage());
            }
        }

        @Override
        public void close() {
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        public boolean isEmpty() {
            return messages.isEmpty();
        }

        public String get(int i) {
            return messages.get(i);
        }

        public int size() {
            return messages.size();
        }

        public void clear() {
            messages.clear();
        }

    }

}
