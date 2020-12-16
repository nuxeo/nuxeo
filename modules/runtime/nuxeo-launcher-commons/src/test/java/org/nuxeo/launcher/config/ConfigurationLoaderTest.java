/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.nuxeo.launcher.config.ConfigurationGenerator.NUXEO_ENVIRONMENT;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;

/**
 * @since 11.5
 */
public class ConfigurationLoaderTest {

    @Test
    public void testLoadNuxeoDefaults() throws Exception {
        var loader = newConfigurationLoader(Map.of());
        var directory = getResourcePath("configuration/loader/testLoadNuxeoDefaults");

        var properties = loader.loadNuxeoDefaults(directory);
        assertEquals("value1", properties.get("prop1"));
        assertEquals("value2", properties.get("prop2"));
    }

    /**
     * NXP-28146
     */
    @Test
    public void testLoadNuxeoDefaultsWithEnvironment() throws Exception {
        var loader = newConfigurationLoader(Map.of(NUXEO_ENVIRONMENT, "utest"));
        var directory = getResourcePath("configuration/loader/testLoadNuxeoDefaults");

        var properties = loader.loadNuxeoDefaults(directory);
        assertEquals("value1", properties.get("prop1"));
        assertEquals("value3", properties.get("prop2"));
        assertEquals("value4", properties.get("prop3"));
    }

    /**
     * Checks environment variable replacement within templates (NXP-29392).
     *
     * @since 11.2
     */
    @Test
    public void testEnvironmentVariableInNuxeoDefaults() throws Exception {
        var directory = getResourcePath("configuration/loader/testEnvironmentVariableInNuxeoDefaults");

        var loader = newConfigurationLoader(Map.of());
        var properties = loader.loadNuxeoDefaults(directory);
        assertEquals("myprop1defaultvalue", properties.getProperty("my.prop1"));
        assertEquals("", properties.getProperty("my.prop2"));

        loader = newConfigurationLoader(Map.of("MY_PROP_1", "myprop1newvalue", "MY_PROP_2", "myprop2newvalue"));
        properties = loader.loadNuxeoDefaults(directory);
        assertEquals("myprop1newvalue", properties.getProperty("my.prop1"));
        assertEquals("myprop2newvalue", properties.getProperty("my.prop2"));
    }

    @Test
    public void testLoadProperties() throws Exception {
        var loader = new ConfigurationLoader(Map.of("SOME_ENV", "value"), Map.of("old.prop", "new.prop"), true);
        var propertiesFile = getResourcePath("configuration/loader/test-load-properties.conf");

        var properties = loader.loadProperties(propertiesFile);
        assertEquals("value", properties.getProperty("prop.to.replace"));
        assertEquals("defaultValue", properties.getProperty("prop.to.replace.with.default"));
        assertEquals("very old", properties.getProperty("old.prop"));
        assertEquals("very old", properties.getProperty("new.prop"));
    }

    @Test
    public void testCheckFileCharset() throws Exception {
        var loader = newConfigurationLoader(Map.of());
        Path tempFile = Files.createTempFile("", "");
        // Test UTF8
        Files.writeString(tempFile, "nuxéo", UTF_8, CREATE);
        try {
            Charset charset = loader.checkFileCharset(tempFile);
            assertEquals(UTF_8, charset);
        } finally {
            Files.deleteIfExists(tempFile);
        }
        // test ISO_8859_1
        Files.writeString(tempFile, "nuxéo", ISO_8859_1, CREATE);
        try {
            Charset charset = loader.checkFileCharset(tempFile);
            assertEquals(ISO_8859_1, charset);
        } finally {
            Files.deleteIfExists(tempFile);
        }
        // test US_ASCII
        Files.writeString(tempFile, "nuxeo", US_ASCII, CREATE);
        try {
            Charset charset = loader.checkFileCharset(tempFile);
            assertEquals(US_ASCII, charset);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testEnvironmentVariablesReplacement() {
        var loader = newConfigurationLoader(Map.of("MY_TEST_PROP", "value"));

        // test null / empty
        assertNull(loader.replaceEnvironmentVariables(null));
        assertEquals("", loader.replaceEnvironmentVariables(""));

        // test existing environment property
        String value = loader.replaceEnvironmentVariables("${env:MY_TEST_PROP}");
        assertEquals("value", value);

        // test missing environment property
        value = loader.replaceEnvironmentVariables("${env:MISSING}");
        assertEquals("", value);

        // test missing environment property with default
        value = loader.replaceEnvironmentVariables("${env:MISSING:defaultValue}");
        assertEquals("defaultValue", value);

        // test existing boolean environment property
        value = loader.replaceEnvironmentVariables("${env??:MY_TEST_PROP}");
        assertEquals("true", value);

        // test missing boolean environment property
        value = loader.replaceEnvironmentVariables("${env??:MISSING}");
        assertEquals("false", value);

        // test partial replacement
        value = loader.replaceEnvironmentVariables("jdbc://${env:MY_TEST_PROP} ${env??:MISSING}");
        assertEquals("jdbc://value false", value);
    }

    @SuppressWarnings("ConstantConditions")
    public Path getResourcePath(String resource) throws Exception {
        URL url = getClass().getClassLoader().getResource(resource);
        return Path.of(url.getPath());
    }

    protected ConfigurationLoader newConfigurationLoader(Map<String, String> environment) {
        return new ConfigurationLoader(environment, Map.of(), false);
    }
}
