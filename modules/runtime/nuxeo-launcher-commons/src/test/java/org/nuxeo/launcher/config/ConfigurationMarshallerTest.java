/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.launcher.config.ConfigurationMarshaller.NEW_FILES;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.launcher.config.ConfigurationMarshaller.NuxeoConfContent;

/**
 * @since 11.5
 */
public class ConfigurationMarshallerTest {

    @Rule
    public TestName name = new TestName();

    protected Path testDirectory;

    protected Path nuxeoConf;

    protected ConfigurationMarshaller marshaller = new ConfigurationMarshaller(System.getProperties());

    protected ConfigurationHolder configHolder;

    @Before
    public void setUp() throws Exception {
        testDirectory = getResourcePath("configuration/marshaller").resolve(name.getMethodName());
        // copy the file, some IDEs don't like file rewriting, so keep a source
        nuxeoConf = Files.copy(testDirectory.resolve("nuxeo.conf"), testDirectory.resolve("actual-nuxeo.conf"),
                REPLACE_EXISTING);
        configHolder = new ConfigurationHolder(testDirectory, nuxeoConf);
        configHolder.putAll(new ConfigurationLoader(Map.of(), Map.of(), true).loadProperties(nuxeoConf));
    }

    // -----------
    // Public APIs
    // -----------

    @Test
    public void testPersistNuxeoConfIdempotence() throws Exception {
        var modifiedTime = Files.readAttributes(nuxeoConf, BasicFileAttributes.class).lastModifiedTime();

        marshaller.persistNuxeoConf(configHolder);

        var actualModifiedTime = Files.readAttributes(nuxeoConf, BasicFileAttributes.class).lastModifiedTime();
        // nuxeo.conf should not have been rewritten
        assertEquals(modifiedTime, actualModifiedTime);

        assertNuxeoConf();
    }

    @Test
    public void testPersistNuxeoConfConfiguratorParameters() throws Exception {
        configHolder.userConfig.put("nuxeo.parameter", "next");
        marshaller.persistNuxeoConf(configHolder);

        assertNuxeoConf();
    }

    @Test
    public void testPersistNuxeoConfUniqueParameters() throws Exception {
        marshaller.persistNuxeoConf(configHolder);

        assertNuxeoConf();
    }

    @Test
    public void testPersistNuxeoConfNoTemplatesParameter() throws Exception {
        configHolder.put(ConfigurationConstants.PARAM_TEMPLATES_NAME, "default,testTemplate");
        marshaller.persistNuxeoConf(configHolder);

        assertNuxeoConf();
    }

    @Test
    public void testPersistNuxeoConfCommentedParameters() throws Exception {
        configHolder.userConfig.remove("nuxeo.parameter2");
        marshaller.persistNuxeoConf(configHolder);

        assertNuxeoConf();
    }

    // NXP-31197
    @Test
    public void testPersistNuxeoConfWithBackslash() throws Exception {
        marshaller.persistNuxeoConf(configHolder);

        assertNuxeoConf();
    }

    protected void assertNuxeoConf() throws IOException {
        var expectedNuxeoConf = testDirectory.resolve("expected-nuxeo.conf");
        var expectedNuxeoConfFile = expectedNuxeoConf.toFile();
        var actualNuxeoConfFile = nuxeoConf.toFile();
        if (!FileUtils.contentEquals(expectedNuxeoConfFile, actualNuxeoConfFile)) {
            throw new ComparisonFailure(
                    name.getMethodName() + "/expected-nuxeo.conf file is not equal to marshalled one",
                    FileUtils.readFileToString(expectedNuxeoConfFile, UTF_8),
                    FileUtils.readFileToString(actualNuxeoConfFile, UTF_8));
        }
    }

    // --------------
    // Protected APIs
    // --------------

    @Test
    public void testReadConfiguration() throws Exception {
        NuxeoConfContent content = marshaller.readConfiguration(nuxeoConf);

        assertEquals(6, content.lines.size());
        assertEquals("nuxeo.templates=default", content.lines.get(0));
        assertEquals("", content.lines.get(1));
        assertEquals("nuxeo.another.parameter=aValue", content.lines.get(2));
        assertEquals("", content.lines.get(3));
        assertEquals("# comments are taken", content.lines.get(4));
        assertEquals("", content.lines.get(5));

        assertEquals(2, content.configuratorProperties.size());
        assertEquals("3fde2aa6", content.configuratorProperties.get("server.status.key"));
        assertEquals("taken", content.configuratorProperties.get("nuxeo.configurator.parameter"));

        assertEquals("9f294138c1f76ba4a3fd9929b1a20806", content.digest);
    }

    /**
     * {@link ConfigurationMarshaller#editUserConf(NuxeoConfContent, ConfigurationHolder)} will comment parameters
     * missing in {@link #configHolder}.
     */
    @Test
    public void testEditUserConf() throws Exception {
        NuxeoConfContent content = marshaller.readConfiguration(nuxeoConf);

        configHolder.userConfig.put("nuxeo.parameter1", "whatever");
        configHolder.userConfig.remove("nuxeo.parameter2");
        marshaller.editUserConf(content, configHolder);

        assertEquals(10, content.lines.size());
        // nuxeo.data.dir is present, see ConfigurationHolder constructor
        assertEquals("nuxeo.data.dir=/var/lib/nuxeo", content.lines.get(0));
        assertEquals("", content.lines.get(1));
        // only one nuxeo.templates in nuxeo.conf file, first index preserved
        assertEquals("nuxeo.templates=mongodb", content.lines.get(2));
        assertEquals("", content.lines.get(3));
        assertEquals("", content.lines.get(4));
        // only one nuxeo.force.generation in nuxeo.conf file, first index preserved
        assertEquals("nuxeo.force.generation=once", content.lines.get(5));
        assertEquals("", content.lines.get(6));
        assertEquals("", content.lines.get(7));
        // taken because present in user configuration
        assertEquals("nuxeo.parameter1=taken", content.lines.get(8));
        // commented because missing in user configuration
        assertEquals("#nuxeo.parameter2=commented", content.lines.get(9));
    }

    /**
     * {@link ConfigurationMarshaller#editUserConf(NuxeoConfContent, ConfigurationHolder)} will prefer nuxeo.templates
     * in user configuration over the one in nuxeo.conf.
     */
    @Test
    public void testEditUserConfWithNuxeoTemplates() throws Exception {
        NuxeoConfContent content = marshaller.readConfiguration(nuxeoConf);

        configHolder.userConfig.put("nuxeo.templates", "postgresql");
        marshaller.editUserConf(content, configHolder);

        assertEquals(1, content.lines.size());
        assertEquals("nuxeo.templates=postgresql", content.lines.get(0));
    }

    /**
     * {@link ConfigurationMarshaller#editConfiguratorConf(NuxeoConfContent, ConfigurationHolder)} will remove
     * configurator parameters if they are defined in user configuration and have the same value.
     */
    @Test
    public void testEditConfiguratorConf() throws Exception {
        NuxeoConfContent content = marshaller.readConfiguration(nuxeoConf);

        configHolder.userConfig.put("nuxeo.parameter1", "shouldBeRemoved");
        configHolder.userConfig.put("nuxeo.parameter2", "whatever");
        marshaller.editConfiguratorConf(content, configHolder);

        assertEquals(2, content.lines.size());
        assertEquals("nuxeo.templates=default", content.lines.get(0));
        assertEquals("", content.lines.get(1));

        // parameters below will be removed from configuratorProperties as they didn't change
        assertFalse(content.configuratorProperties.containsKey("server.status.key"));
        assertFalse(content.configuratorProperties.containsKey("nuxeo.parameter1"));
        // parameters below will be kept in configuratorProperties as they did change
        assertEquals("shouldBeKept", content.configuratorProperties.get("nuxeo.parameter2"));
    }

    @Test
    public void testParseAndCopy() throws Exception {
        // enable templates
        var templatesPath = configHolder.getTemplatesPath();
        var loader = new ConfigurationLoader(Map.of(), Map.of(), true);
        ThrowableConsumer<Path, ConfigurationException> enableTemplate = path -> configHolder.putTemplateAll(path,
                loader.loadNuxeoDefaults(path));
        enableTemplate.accept(templatesPath.resolve("myTemplate"));
        enableTemplate.accept(templatesPath.resolve("customTarget"));

        marshaller.parseAndCopy(configHolder);

        List<String> content;

        var nxserverPath = configHolder.getRuntimeHomePath();
        var nxserverConfigPath = nxserverPath.resolve("config");
        // assert myTemplate result
        assertTrue(Files.exists(nxserverPath));
        assertTrue(Files.notExists(nxserverPath.resolve("nuxeo.defaults")));
        assertTrue(Files.exists(nxserverConfigPath));
        assertTrue(Files.exists(nxserverConfigPath.resolve("extension.ignored")));
        content = Files.readAllLines(nxserverConfigPath.resolve("extension.ignored"));
        assertEquals(1, content.size());
        assertEquals("This must not be parsed ${nuxeo.bind.address}", content.get(0));
        assertTrue(Files.exists(nxserverConfigPath.resolve("file.xml")));
        content = Files.readAllLines(nxserverConfigPath.resolve("file.xml"));
        assertEquals(3, content.size());
        assertEquals("<root>", content.get(0));
        assertEquals("    <element attribute=\"value\" />", content.get(1));
        assertEquals("</root>", content.get(2));

        var libPath = configHolder.getHomePath().resolve("lib");
        // assert customTarget result
        assertTrue(Files.exists(libPath));
        assertTrue(Files.exists(libPath.resolve("file.properties")));
        content = Files.readAllLines(libPath.resolve("file.properties"));
        assertEquals(1, content.size());
        assertEquals("a.prop=value-3fde2aa6", content.get(0));
    }

    @Test
    public void testStoreNewFilesList() throws Exception {
        Files.createDirectories(configHolder.getTemplatesPath());

        marshaller.storeNewFilesList(configHolder,
                List.of(configHolder.getHomePath().resolve("nxserver/config/template.nxftl").toString()));

        Path newFilesPath = configHolder.getTemplatesPath().resolve(NEW_FILES);
        List<String> newFiles = Files.readAllLines(newFilesPath);
        Files.delete(newFilesPath);
        assertEquals(1, newFiles.size());
        // assert path is relativized
        assertEquals("nxserver/config/template.nxftl", newFiles.get(0));
    }

    @Test
    public void testDumpProperties() throws Exception {
        configHolder.putDefaultAll(System.getProperties()); // System properties should be ignored

        marshaller.dumpProperties(configHolder);

        var expectedConfiguration = testDirectory.resolve("expected-configuration.properties");
        var actualConfiguration = configHolder.getDumpedConfigurationPath();
        List<String> actualLines = Files.readAllLines(actualConfiguration);
        assertFalse("The generated configuration.properties is empty", actualLines.isEmpty());
        // second line is a date - remove it as we don't control the clock
        assertTrue(actualLines.remove(1).startsWith("#"));

        String expected = Files.readString(expectedConfiguration);
        // paths are absolute to the computer - replace the path
        expected = expected.replace("$$NUXEO_HOME$$", testDirectory.toString());
        // add a new line at the end
        String actual = String.join(System.lineSeparator(), actualLines) + System.lineSeparator();
        assertEquals(expected, actual);
    }

    @SuppressWarnings("ConstantConditions")
    public Path getResourcePath(String resource) throws Exception {
        URL url = getClass().getClassLoader().getResource(resource);
        return Path.of(url.getPath());
    }
}
