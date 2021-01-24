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
 *
 */

package org.nuxeo.connect.update.standalone.commands;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.launcher.config.ConfigurationConstants;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.test.TargetResourceLocator;

public class TestConfig extends AbstractCommandTest {

    protected static final Logger log = LogManager.getLogger(TestConfig.class);

    @Inject
    protected TargetResourceLocator locator;

    protected ConfigurationGenerator configurationGenerator;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        URL url = locator.getTargetTestResource("config/nuxeo.conf");
        File nuxeoConf = new File(Environment.getDefault().getServerHome(), "nuxeo.conf");
        FileUtils.copyFile(new File(URLDecoder.decode(url.getPath(), UTF_8)), nuxeoConf);
        System.setProperty(ConfigurationConstants.PARAM_NUXEO_CONF, nuxeoConf.getPath());
        System.setProperty(ConfigurationConstants.TOMCAT_HOME, Environment.getDefault().getServerHome().getPath());
        url = locator.getTargetTestResource("templates");
        FileUtils.copyDirectory(new File(URLDecoder.decode(url.getPath(), UTF_8)),
                new File(Environment.getDefault().getServerHome(), "templates"));

        configurationGenerator = ConfigurationGenerator.build();
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
        configurationGenerator = ConfigurationGenerator.build();
        assertTrue(configurationGenerator.init());
        var configHolder = configurationGenerator.getConfigurationHolder();

        log.info("Install done. nuxeo.conf content:\n{}", Files.readString(configHolder.getNuxeoConfPath()));

        String templates = configurationGenerator.getUserConfig()
                                                 .getProperty(ConfigurationConstants.PARAM_TEMPLATES_NAME);
        assertNotNull(templates);
        assertTrue("newtemplate was not added", templates.contains("newtemplate"));
        assertFalse("oldtemplate was not removed", templates.contains("oldtemplate"));
        assertEquals("test.property was not set to some.value", "some.value",
                configurationGenerator.getUserConfig().getProperty("test.property"));
        assertEquals("alreadyset.property was not set to its new value", "new.value",
                configurationGenerator.getUserConfig().getProperty("alreadyset.property"));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        configurationGenerator = ConfigurationGenerator.build();
        assertTrue(configurationGenerator.init());
        var configHolder = configurationGenerator.getConfigurationHolder();

        log.info("Install done. nuxeo.conf content:\n{}", Files.readString(configHolder.getNuxeoConfPath()));

        String templates = configurationGenerator.getUserConfig()
                                                 .getProperty(ConfigurationConstants.PARAM_TEMPLATES_NAME);
        assertNotNull(templates);
        assertFalse("newtemplate was not removed", templates.contains("newtemplate"));
        assertTrue("oldtemplate was not reset", templates.contains("oldtemplate"));
        assertNull("test.property was not removed",
                configurationGenerator.getUserConfig().getProperty("test.property"));
        assertEquals("alreadyset.property was not set to its old value", "old.value",
                configurationGenerator.getUserConfig().getProperty("alreadyset.property"));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        System.clearProperty(ConfigurationConstants.PARAM_NUXEO_CONF);
        System.clearProperty(Environment.NUXEO_HOME);
        System.clearProperty(ConfigurationConstants.TOMCAT_HOME);
        System.clearProperty(Environment.NUXEO_DATA_DIR);
        System.clearProperty(Environment.NUXEO_LOG_DIR);
    }

}
