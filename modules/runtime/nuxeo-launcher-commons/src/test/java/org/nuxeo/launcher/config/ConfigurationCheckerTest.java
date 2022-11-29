/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.launcher.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.nuxeo.common.Environment.CRYPT_KEY;

import java.io.File;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @since 9.2
 */
public class ConfigurationCheckerTest {

    protected static final String CRYPT_KEY_VALUE = Base64.encodeBase64String("secret".getBytes());

    /** @since 11.5 */
    @Rule
    public final ConfigurationRule rule = new ConfigurationRule("configuration/backing");

    protected ConfigurationChecker checker = new ConfigurationChecker(System.getProperties());

    protected ConfigurationHolder configHolder;

    protected Path lib;

    protected Path nxserverLib;

    protected Path bundles;

    protected Path backingPath;

    @Before
    public void setUp() throws Exception {
        FakeCheck.reset();

        // init configuration and service
        configHolder = new ConfigurationHolder(rule.getNuxeoHome(), rule.getNuxeoConf());
        lib = rule.getNuxeoHome().resolve("lib");
        nxserverLib = rule.getNuxeoHome().resolve("nxserver/lib");
        bundles = rule.getNuxeoHome().resolve("nxserver/bundles");

        // load backing template
        var loader = new ConfigurationLoader(Map.of(), Map.of(), true);
        backingPath = configHolder.getTemplatesPath().resolve("backing");
        configHolder.putTemplateAll(backingPath, loader.loadNuxeoDefaults(backingPath));
        configHolder.put("nuxeo.home", rule.getNuxeoHome().toString()); // used by backing.check.classpath
        configHolder.put(ConfigurationChecker.PARAM_RETRY_POLICY_MAX_RETRIES, "5");
        configHolder.put(ConfigurationChecker.PARAM_RETRY_POLICY_DELAY_IN_MS, "20");
    }

    @Test
    public void canReferenceRelativePathInClasspathEntry() {
        Stream<Path> jars = checker.getJarsFromClasspathEntry(configHolder, backingPath, "lib");
        assertEquals(2, jars.count());

        jars = checker.getJarsFromClasspathEntry(configHolder, backingPath, "lib/fake.jar");
        assertEquals(1, jars.count());
    }

    @Test
    public void canReferenceAbsolutePathInClassPathEntry() {
        Stream<Path> jars = checker.getJarsFromClasspathEntry(configHolder, backingPath, bundles.toString());
        assertEquals(1, jars.count());
    }

    @Test
    public void cannotReferenceAbsolutPathOutsideOfNuxeoHome() {
        Stream<Path> jars = checker.getJarsFromClasspathEntry(configHolder, backingPath,
                rule.getResourcePath("testDefault/nxserver/bundles/versioned-1.0.jar").getParent().toString());
        assertEquals(0, jars.count());
    }

    @Test
    public void canReferenceGlobPatternInClasspathEntry() {
        Stream<Path> jars = checker.getJarsFromClasspathEntry(configHolder, backingPath,
                bundles.toString() + File.separator + "versioned-*.jar");
        assertEquals(1, jars.count());

        jars = checker.getJarsFromClasspathEntry(configHolder, backingPath,
                bundles.toString() + File.separator + "other-*.jar");
        assertEquals(0, jars.count());
    }

    @Test
    public void canUseParametersInClasspath() {
        configHolder.put("backing.check.classpath", "${nuxeo.home}/nxserver/bundles/versioned-*.jar");
        // getBackingCheckerClasspath doesn't replace the separator on Windows, getJarsFromClasspathEntry does
        String expected = lib.toString() + ':' + nxserverLib.toString() + ':' + bundles.toString() + File.separator
                + "versioned-*.jar";
        String actual = checker.getBackingCheckerClasspath(configHolder, "backing").replace('/', File.separatorChar);
        assertEquals(expected, actual);
    }

    // NXP-28880
    @Test
    public void canUseEncryptedParametersInClasspath() {
        configHolder.put(CRYPT_KEY, CRYPT_KEY_VALUE);
        configHolder.put("nuxeo.home.encrypted", encrypt(rule.getNuxeoHome().toString()));
        configHolder.put("backing.check.classpath", "${nuxeo.home.encrypted}/nxserver/bundles/versioned-*.jar");
        // getBackingCheckerClasspath doesn't replace the separator on Windows, getJarsFromClasspathEntry does
        String expected = lib.toString() + ':' + nxserverLib.toString() + ':' + bundles.toString() + File.separator
                + "versioned-*.jar";
        String actual = checker.getBackingCheckerClasspath(configHolder, "backing").replace('/', File.separatorChar);
        assertEquals(expected, actual);
    }

    @Test
    public void backingCheckerAreCalled() throws Exception {
        assertEquals(0, FakeCheck.getCallCount());
        checker.checkBackingServices(configHolder);
        assertEquals(1, FakeCheck.getCallCount());
    }

    @Test
    public void backingCheckerAreRetried() {
        configHolder.put(ConfigurationChecker.PARAM_RETRY_POLICY_ENABLED, "true");
        configHolder.put("backing.fake.check.ready", "false");
        assertThrows(ConfigurationException.class, () -> checker.checkBackingServices(configHolder));
        assertEquals(6, FakeCheck.getCallCount());

        configHolder.put(ConfigurationChecker.PARAM_RETRY_POLICY_ENABLED, "false");
        FakeCheck.reset();
        assertThrows(ConfigurationException.class, () -> checker.checkBackingServices(configHolder));
        assertEquals(1, FakeCheck.getCallCount());
    }

    // NXP-28880
    @Test
    public void backingCheckerCanReadDescriptorWithEncryptedParameters() throws Exception {
        configHolder.put(CRYPT_KEY, CRYPT_KEY_VALUE);
        configHolder.put("backing.fake.check.ready", encrypt("true"));
        configHolder.put("backing.fake.check.value", encrypt("String<With&Xml>CharsToBeEscaped"));
        assertEquals(0, FakeCheck.getCallCount());
        checker.checkBackingServices(configHolder);
        assertEquals(1, FakeCheck.getCallCount());

        FakeCheck.reset();

        configHolder.put("backing.fake.check.ready", encrypt("false"));
        assertEquals(0, FakeCheck.getCallCount());
        assertThrows(ConfigurationException.class, () -> checker.checkBackingServices(configHolder));
        assertEquals(1, FakeCheck.getCallCount());
    }

    protected String encrypt(String value) {
        if (!configHolder.stringPropertyNames().contains(CRYPT_KEY)) {
            throw new AssertionError("No CRYPT_KEY present in configuration, check your test");
        }
        try {
            return configHolder.userConfig.getCrypto().encrypt(value.getBytes(UTF_8));
        } catch (GeneralSecurityException e) {
            throw new AssertionError("Unable to encrypt the value: " + value, e);
        }
    }
}
