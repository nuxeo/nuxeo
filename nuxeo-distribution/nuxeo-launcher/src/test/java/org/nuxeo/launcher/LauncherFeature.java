/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.launcher;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Binder;

@Features(PlatformFeature.class)
public class LauncherFeature implements RunnerFeature {

    NuxeoLauncher launcher;

    public File getResourceFile(String resource) {
        URL url = getClass().getClassLoader().getResource(resource);
        try {
            return new File(URLDecoder.decode(url.getPath(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("Cannot get " + resource);
        }
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(NuxeoLauncher.class).toProvider(() -> launcher);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        File nuxeoHome = runner.getFeature(RuntimeFeature.class).getHarness().getWorkingDir();
        File nuxeoConf = getResourceFile("config/nuxeo.conf");
        FileUtils.copyFileToDirectory(nuxeoConf, nuxeoHome);
        FileUtils.copyDirectory(getResourceFile("templates"), new File(nuxeoHome, "templates"));
        System.setProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());
        System.setProperty(ConfigurationGenerator.NUXEO_CONF, new File(nuxeoHome, nuxeoConf.getName()).getPath());
        System.setProperty(TomcatConfigurator.TOMCAT_HOME, Environment.getDefault().getServerHome().getPath());
        ConfigurationGenerator configGenerator = new ConfigurationGenerator();
        assertTrue(configGenerator.init());
        launcher = NuxeoLauncher.createLauncher(new String[] { "showconf" });
    }

}
