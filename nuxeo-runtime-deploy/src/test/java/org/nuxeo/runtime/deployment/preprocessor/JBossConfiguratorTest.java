/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;

/**
 * @author jcarsique
 * 
 */
public class JBossConfiguratorTest {

    private static final Object PROPERTY_TO_GENERATE = "<config-property name=\""
            + "property\" type=\"java.lang.String\">URL=jdbc:h2:${jboss.server.data.dir}"
            + "/h2/nuxeo;AUTO_SERVER=true</config-property>";

    private ConfigurationGenerator configGenerator;

    private File nuxeoConf;

    private File nuxeoHome;

    @Before
    public void setUp() throws Exception {
        nuxeoConf = FileUtils.getResourceFileFromContext("configurator/nuxeo.conf");
        System.setProperty(ConfigurationGenerator.NUXEO_CONF, nuxeoConf.getPath());
        nuxeoHome = File.createTempFile("nuxeo", null);
        nuxeoHome.delete();
        nuxeoHome.mkdirs();
        System.setProperty(ConfigurationGenerator.NUXEO_HOME, nuxeoHome.getPath());
        FileUtils.copy(FileUtils.getResourceFileFromContext("templates/jboss"),
                new File(nuxeoHome, "templates"));
        System.setProperty("jboss.home.dir",nuxeoHome.getPath());
        configGenerator = new ConfigurationGenerator();
    }

    @Test
    public void testGetConfifuration() throws Exception {
        Properties config = configGenerator.getConfiguration();
        assertTrue(config.getProperty("nuxeo.template").equals("default"));
        assertTrue(config.getProperty("test.nuxeo.conf").equals("true"));
        assertTrue(config.getProperty("test.nuxeo.defaults").equals("true"));
        assertTrue(config.getProperty("test.nuxeo.defaults.template.1").equals(
                "true"));
        assertTrue(config.getProperty("test.nuxeo.defaults.template.2").equals(
                "true"));
        assertTrue(config.getProperty("test.nuxeo.conf.override.defaults").equals(
                "true"));
        assertTrue(config.getProperty(
                "test.nuxeo.conf.override.defaults.template").equals("true"));
        assertTrue(config.getProperty("nuxeo.db.name").equals("nuxeo"));
        assertTrue(config.getProperty("nuxeo.db.user").equals("sa"));
    }

    @Test
    public void testGenerateFiles() throws ConfigurationException, IOException {
        configGenerator.run();
        File configDir = new File(nuxeoHome, JBossConfigurator.JBOSS_CONFIG);
        assertTrue(new File(configDir,"test2").exists());
        File generatedFile = new File(configDir.getParentFile(),
                "datasources/default-repository-ds.xml");
        String generatedProperty = new BufferedReader(new FileReader(
                generatedFile)).readLine();
        assertTrue(generatedProperty,
                PROPERTY_TO_GENERATE.equals(generatedProperty));
    }

}
