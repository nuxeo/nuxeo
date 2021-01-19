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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @since 9.2
 */
public class ConfigurationCheckerTest {

    /** @since 11.5 */
    @Rule
    public final ConfigurationRule rule = new ConfigurationRule("configuration/backing");

    protected ConfigurationChecker checker = new ConfigurationChecker(System.getProperties());

    protected ConfigurationHolder configHolder;

    protected Path bundles;

    protected Path backingPath;

    @Before
    public void setUp() throws Exception {
        // NXP-23518, assume instead of ConditionalIgnoreRule to not import nuxeo-runtime-test that brings too much deps
        assumeFalse(SystemUtils.IS_OS_WINDOWS);

        FakeCheck.reset();
        FakeCheck.setReady(true);

        // init configuration and service
        configHolder = new ConfigurationHolder(rule.getNuxeoHome(), rule.getNuxeoConf());
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
                bundles.resolve("versioned-*.jar").toString());
        assertEquals(1, jars.count());

        jars = checker.getJarsFromClasspathEntry(configHolder, backingPath, bundles.resolve("other-*.jar").toString());
        assertEquals(0, jars.count());
    }

    @Test
    public void canUseParametersInClasspath() {
        configHolder.put("backing.check.classpath", "${nuxeo.home}/nxserver/bundles/versioned-*.jar");
        assertEquals(bundles.resolve("versioned-*.jar").toString(),
                checker.getBackingCheckerClasspath(configHolder, "backing"));
    }

    @Test
    public void backingCheckerAreCalled() throws Exception {
        assertEquals(0, FakeCheck.getCallCount());
        checker.checkBackingServices(configHolder);
        assertEquals(1, FakeCheck.getCallCount());
    }

    @Test
    public void checksAreRetried() {
        configHolder.put(ConfigurationChecker.PARAM_RETRY_POLICY_ENABLED, "true");
        FakeCheck.setReady(false);
        assertThrows(ConfigurationException.class, () -> checker.checkBackingServices(configHolder));
        assertEquals(6, FakeCheck.getCallCount());

        configHolder.put(ConfigurationChecker.PARAM_RETRY_POLICY_ENABLED, "false");
        FakeCheck.reset();
        assertThrows(ConfigurationException.class, () -> checker.checkBackingServices(configHolder));
        assertEquals(1, FakeCheck.getCallCount());
    }

    protected Path getTemplatePath(String templateName) {
        return configHolder.getTemplatesPath().resolve(templateName);
    }
}
