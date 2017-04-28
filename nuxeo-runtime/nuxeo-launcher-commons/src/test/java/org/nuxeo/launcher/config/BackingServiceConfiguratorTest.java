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

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class BackingServiceConfiguratorTest extends AbstractConfigurationTest {

    private File bundles;
    private BackingServiceConfigurator bsc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

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

        configGenerator.setProperty("nuxeo.backing.check.retry.maxRetries","5");
        configGenerator.setProperty("nuxeo.backing.check.retry.delayInMs","20");

    }

    @Test
    public void canReferenceRelativePathInClasspathEntry() throws Exception {
        File conf = configGenerator.getTemplateConf("backing");
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(conf.getParentFile(), "lib");
        assertThat(jars).hasSize(2);

        jars = bsc.getJarsFromClasspathEntry(conf.getParentFile(), "lib/fake.jar");
        assertThat(jars).hasSize(1);
    }

    @Test
    public void canReferenceAbsolutePathInClassPathEntry() throws Exception {
        File conf = configGenerator.getTemplateConf("backing");
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(conf.getParentFile(), bundles.getAbsolutePath());
        assertThat(jars).hasSize(1);


    }

    @Test
    public void cannotReferenceAbsolutPathOutsideOfNuxeoHome() throws Exception {
        File conf = configGenerator.getTemplateConf("backing");
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(conf.getParentFile(), getResourceFile("versioned-1.0.jar").getParent());
        assertThat(jars).isEmpty();
    }

    @Test
    public void canReferenceGlobPatternInClasspathEntry() throws Exception {
        File conf = configGenerator.getTemplateConf("backing");
        Collection<? extends File> jars = bsc.getJarsFromClasspathEntry(conf.getParentFile(), bundles.getAbsolutePath() + "/versioned-*.jar");
        assertThat(jars).hasSize(1);

        jars = bsc.getJarsFromClasspathEntry(conf.getParentFile(), bundles.getAbsolutePath() + "/other-*.jar");
        assertThat(jars).hasSize(0);
    }


    @Test
    public void canUseParametersInClasspath() throws Exception {
        configGenerator.setProperty("backing.check.classpath","${nuxeo.home}/nxserver/bundles/versioned-*.jar");
        assertThat(bsc.getClasspathForTemplate("backing")).isEqualTo(bundles.getAbsolutePath() + "/versioned-*.jar");
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
        configGenerator.setProperty("nuxeo.backing.check.retry.enabled","true");
        FakeCheck.setReady(false);
        try {
            configGenerator.verifyInstallation();
        } catch (ConfigurationException e) {
        }

        assertThat(FakeCheck.getCallCount()).isEqualTo(6);


        configGenerator.setProperty("nuxeo.backing.check.retry.enabled","false");
        FakeCheck.reset();
        try {
            configGenerator.verifyInstallation();
        } catch (ConfigurationException e) {
        }

        assertThat(FakeCheck.getCallCount()).isEqualTo(1);



    }




}
