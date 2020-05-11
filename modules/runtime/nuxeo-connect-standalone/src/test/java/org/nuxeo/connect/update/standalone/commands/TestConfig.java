/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.connect.update.standalone.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.ServerConfigurator;
import org.nuxeo.runtime.test.TargetResourceLocator;

public class TestConfig extends AbstractCommandTest {

    @Inject
    TargetResourceLocator locator;

    private ConfigurationGenerator configurationGenerator;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        URL url = locator.getTargetTestResource("config/nuxeo.conf");
        File nuxeoConf = new File(Environment.getDefault().getServerHome(), "nuxeo.conf");
        FileUtils.copyFile(new File(URLDecoder.decode(url.getPath(), "UTF-8")), nuxeoConf);
        System.setProperty(ConfigurationGenerator.NUXEO_CONF, nuxeoConf.getPath());
        System.setProperty(ServerConfigurator.TOMCAT_HOME, Environment.getDefault().getServerHome().getPath());
        url = locator.getTargetTestResource("templates");
        FileUtils.copyDirectory(new File(URLDecoder.decode(url.getPath(), "UTF-8")), new File(
                Environment.getDefault().getServerHome(), "templates"));

        configurationGenerator = new ConfigurationGenerator();
        assertTrue(configurationGenerator.init());
        configurationGenerator.addTemplate("oldtemplate");
        configurationGenerator.addTemplate("oldtemplate1,oldtemplate2");
    }

    @Override
    protected File createPackage() throws IOException, URISyntaxException {
        return getTestPackageZip("test-config");
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        configurationGenerator = new ConfigurationGenerator();
        assertTrue(configurationGenerator.init());

        log.info("Install done. nuxeo.conf content:");
        BufferedReader reader = new BufferedReader(new FileReader(configurationGenerator.getNuxeoConf()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.info(line);
        }
        reader.close();
        log.info("END nuxeo.conf content:");

        String templates = configurationGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        assertTrue("newtemplate was not added", templates != null && templates.contains("newtemplate"));
        assertTrue("oldtemplate was not removed", templates != null && !templates.contains("oldtemplate"));
        assertEquals("test.property was not set to some.value", "some.value",
                configurationGenerator.getUserConfig().getProperty("test.property"));
        assertEquals("alreadyset.property was not set to its new value", "new.value",
                configurationGenerator.getUserConfig().getProperty("alreadyset.property"));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        configurationGenerator = new ConfigurationGenerator();
        assertTrue(configurationGenerator.init());

        log.info("Uninstall done. nuxeo.conf content:");
        BufferedReader reader = new BufferedReader(new FileReader(configurationGenerator.getNuxeoConf()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.info(line);
        }
        reader.close();
        log.info("END nuxeo.conf content:");

        String templates = configurationGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        assertTrue("newtemplate was not removed", templates != null && !templates.contains("newtemplate"));
        assertTrue("oldtemplate was not reset", templates != null && templates.contains("oldtemplate"));
        assertNull("test.property was not removed", configurationGenerator.getUserConfig().getProperty("test.property"));
        assertEquals("alreadyset.property was not set to its old value", "old.value",
                configurationGenerator.getUserConfig().getProperty("alreadyset.property"));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        System.clearProperty(ConfigurationGenerator.NUXEO_CONF);
        System.clearProperty(Environment.NUXEO_HOME);
        System.clearProperty(ServerConfigurator.TOMCAT_HOME);
        System.clearProperty(Environment.NUXEO_DATA_DIR);
        System.clearProperty(Environment.NUXEO_LOG_DIR);
    }

}
