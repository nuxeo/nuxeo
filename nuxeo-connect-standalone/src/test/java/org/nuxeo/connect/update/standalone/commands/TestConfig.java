/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.PackageBuilder;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;
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
        File nuxeoConf = new File(
                org.nuxeo.common.Environment.getDefault().getServerHome(),
                "nuxeo.conf");
        FileUtils.copyFile(new File(URLDecoder.decode(url.getPath(), "UTF-8")),
                nuxeoConf);
        System.setProperty(ConfigurationGenerator.NUXEO_CONF,
                nuxeoConf.getPath());
        System.setProperty(
                TomcatConfigurator.TOMCAT_HOME,
                org.nuxeo.common.Environment.getDefault().getServerHome().getPath());
        url = locator.getTargetTestResource("templates");
        FileUtils.copyDirectory(
                new File(URLDecoder.decode(url.getPath(), "UTF-8")),
                new File(
                        org.nuxeo.common.Environment.getDefault().getServerHome(),
                        "templates"));

        configurationGenerator = new ConfigurationGenerator();
        assertTrue(configurationGenerator.init());
        configurationGenerator.addTemplate("oldtemplate");
        configurationGenerator.addTemplate("oldtemplate1,oldtemplate2");
    }

    @Override
    protected void updatePackage(PackageBuilder builder) throws Exception {
        // nothing to do
    }

    @Override
    protected void writeCommand(XmlWriter writer) {
        writer.start("config");
        writer.attr("addtemplate", "newtemplate");
        writer.end();
        writer.start("config");
        writer.attr("rmtemplate", "oldtemplate");
        writer.end();
        writer.start("config");
        writer.attr("addtemplate", "newtemplate1,newtemplate2");
        writer.end();
        writer.start("config");
        writer.attr("rmtemplate", "oldtemplate1,oldtemplate2");
        writer.end();
        writer.start("config");
        writer.attr("set", "test.property=some.value");
        writer.end();
        writer.start("config");
        writer.attr("set", "alreadyset.property=new.value");
        writer.end();
    }

    @Override
    protected void installDone(Task task, Throwable error) throws Exception {
        super.installDone(task, error);
        configurationGenerator = new ConfigurationGenerator();
        assertTrue(configurationGenerator.init());

        log.info("Install done. nuxeo.conf content:");
        BufferedReader reader = new BufferedReader(new FileReader(
                configurationGenerator.getNuxeoConf()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.info(line);
        }
        reader.close();
        log.info("END nuxeo.conf content:");

        String templates = configurationGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        assertTrue("newtemplate was not added",
                templates != null && templates.contains("newtemplate"));
        assertTrue("oldtemplate was not removed", templates != null
                && !templates.contains("oldtemplate"));
        assertEquals(
                "test.property was not set to some.value",
                "some.value",
                configurationGenerator.getUserConfig().getProperty(
                        "test.property"));
        assertEquals(
                "alreadyset.property was not set to its new value",
                "new.value",
                configurationGenerator.getUserConfig().getProperty(
                        "alreadyset.property"));
    }

    @Override
    protected void uninstallDone(Task task, Throwable error) throws Exception {
        super.uninstallDone(task, error);
        configurationGenerator = new ConfigurationGenerator();
        assertTrue(configurationGenerator.init());

        log.info("Uninstall done. nuxeo.conf content:");
        BufferedReader reader = new BufferedReader(new FileReader(
                configurationGenerator.getNuxeoConf()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.info(line);
        }
        reader.close();
        log.info("END nuxeo.conf content:");

        String templates = configurationGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_TEMPLATES_NAME);
        assertTrue("newtemplate was not removed", templates != null
                && !templates.contains("newtemplate"));
        assertTrue("oldtemplate was not reset",
                templates != null && templates.contains("oldtemplate"));
        assertNull(
                "test.property was not removed",
                configurationGenerator.getUserConfig().getProperty(
                        "test.property"));
        assertEquals(
                "alreadyset.property was not set to its old value",
                "old.value",
                configurationGenerator.getUserConfig().getProperty(
                        "alreadyset.property"));
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Properties sysProperties = System.getProperties();
        sysProperties.remove(ConfigurationGenerator.NUXEO_CONF);
        sysProperties.remove(Environment.NUXEO_HOME);
        sysProperties.remove(TomcatConfigurator.TOMCAT_HOME);
        sysProperties.remove(Environment.NUXEO_DATA_DIR);
        sysProperties.remove(Environment.NUXEO_LOG_DIR);
    }

}
