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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;

public class BackingServiceConfiguratorTest extends AbstractConfigurationTest {

    private File bundles;
    private BackingServiceConfigurator bsc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // NXP-23518, assume instead of ConditionalIgnoreRule to not import nuxeo-runtime-test that brings too much deps
        assumeFalse(SystemUtils.IS_OS_WINDOWS);

        FakeCheck.reset();
        FakeCheck.setReady(true);


        FileUtils.copyDirectory(getResourceFile("templates/jboss"), new File(nuxeoHome, "templates"));
        bundles = new File(nuxeoHome, "nxserver/bundles");
        bundles.mkdirs();
        FileUtils.copyFileToDirectory(getResourceFile("versioned-1.0.jar"), bundles);
        configGenerator = new ConfigurationGenerator();


        bsc = new BackingServiceConfigurator(configGenerator);
        assertThat(configGenerator.init()).isTrue();
        configGenerator.setProperty("nuxeo.home", nuxeoHome.getAbsolutePath());

        configGenerator.setProperty(BackingServiceConfigurator.PARAM_RETRY_POLICY_MAX_RETRIES,"5");
        configGenerator.setProperty(BackingServiceConfigurator.PARAM_RETRY_POLICY_DELAY_IN_MS,"20");

    }

    @Test
    public void canReferenceRelativePathInClasspathEntry() throws Exception {
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(getTemplateParentPath("backing"), "lib");
        assertThat(jars).hasSize(2);

        jars = bsc.getJarsFromClasspathEntry(getTemplateParentPath("backing"), "lib/fake.jar");
        assertThat(jars).hasSize(1);
    }

    @Test
    public void canReferenceAbsolutePathInClassPathEntry() throws Exception {
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(getTemplateParentPath("backing"), bundles.getAbsolutePath());
        assertThat(jars).hasSize(1);


    }

    @Test
    public void cannotReferenceAbsolutPathOutsideOfNuxeoHome() throws Exception {
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(getTemplateParentPath("backing"), getResourceFile("versioned-1.0.jar").getParent());
        assertThat(jars).isEmpty();
    }

    @Test
    public void canReferenceGlobPatternInClasspathEntry() throws Exception {
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(getTemplateParentPath("backing"), bundles.getAbsolutePath() + "/versioned-*.jar");
        assertThat(jars).hasSize(1);

        jars = bsc.getJarsFromClasspathEntry(getTemplateParentPath("backing"), bundles.getAbsolutePath() + "/other-*.jar");
        assertThat(jars).hasSize(0);
    }


    @Test
    public void canUseParametersInClasspath() throws Exception {
        configGenerator.setProperty("backing.check.classpath","${nuxeo.home}/nxserver/bundles/versioned-*.jar");
        assertThat(bsc.getClasspathForTemplate("backing")).isEqualTo(bundles.getAbsolutePath() + File.separator + "versioned-*.jar");
    }

    @Test
    public void backingCheckerAreCalled() throws Exception {
        configGenerator.setProperty("backing.check.class","org.nuxeo.launcher.config.FakeCheck");
        assertThat(FakeCheck.getCallCount()).isEqualTo(0);
        configGenerator.verifyInstallation();
        assertThat(FakeCheck.getCallCount()).isEqualTo(1);
    }

    @Test
    public void checksAreRetried() throws Exception {
        configGenerator.setProperty(BackingServiceConfigurator.PARAM_RETRY_POLICY_ENABLED,"true");
        FakeCheck.setReady(false);
        try {
            configGenerator.verifyInstallation();
        } catch (ConfigurationException e) {
        }

        assertThat(FakeCheck.getCallCount()).isEqualTo(6);

        configGenerator.setProperty(BackingServiceConfigurator.PARAM_RETRY_POLICY_ENABLED,"false");
        FakeCheck.reset();
        try {
            configGenerator.verifyInstallation();
        } catch (ConfigurationException e) {
        }

        assertThat(FakeCheck.getCallCount()).isEqualTo(1);
    }

    protected Path getTemplateParentPath(String templateName) throws ConfigurationException {
        File conf = configGenerator.getTemplateConf("backing");
        return conf.getParentFile().toPath();
    }
}
