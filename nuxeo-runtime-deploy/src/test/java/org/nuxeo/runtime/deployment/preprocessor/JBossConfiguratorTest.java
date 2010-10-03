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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;

/**
 * @author jcarsique
 *
 */
public class JBossConfiguratorTest {

    private static final Log log = LogFactory.getLog(JBossConfiguratorTest.class);

    private ConfigurationGenerator configGenerator;

    private ConfigurationGenerator configGenerator2;

    private File nuxeoHome;

    String propertyToGenerate;

    String propertyToGenerate2;

    @Before
    public void setUp() throws Exception {
        File nuxeoConf = FileUtils.getResourceFileFromContext("configurator/nuxeo.conf");
        System.setProperty(ConfigurationGenerator.NUXEO_CONF,
                nuxeoConf.getPath());
        nuxeoHome = File.createTempFile("nuxeo", null);
        nuxeoHome.delete();
        nuxeoHome.mkdirs();
        System.setProperty(ConfigurationGenerator.NUXEO_HOME,
                nuxeoHome.getPath());
        System.setProperty(Environment.NUXEO_DATA_DIR, new File(nuxeoHome,
                "data").getPath());
        propertyToGenerate = "<config-property name=\""
                + "property\" type=\"java.lang.String\">URL=jdbc:h2:"
                + System.getProperty(Environment.NUXEO_DATA_DIR)
                + File.separator + "h2" + File.separator
                + "testinclude;AUTO_SERVER=true</config-property>";
        System.setProperty(Environment.NUXEO_LOG_DIR,
                new File(nuxeoHome, "log").getPath());

        // Windows path
        propertyToGenerate2 = "<config-property name=\""
                + "property\" type=\"java.lang.String\">URL=jdbc:h2:"
                + "C:\\nuxeo-dm-5.4.0-SNAPSHOT-jboss\\server\\default\\data\\NXRuntime\\data"
                + File.separator + "h2" + File.separator
                + "testinclude;AUTO_SERVER=true</config-property>";

        FileUtils.copy(FileUtils.getResourceFileFromContext("templates/jboss"),
                new File(nuxeoHome, "templates"));
        System.setProperty("jboss.home.dir", nuxeoHome.getPath());
        configGenerator = new ConfigurationGenerator();

        nuxeoConf = FileUtils.getResourceFileFromContext("configurator/nuxeo.conf2");
        System.setProperty(ConfigurationGenerator.NUXEO_CONF,
                nuxeoConf.getPath());
        configGenerator2 = new ConfigurationGenerator();
    }

    @Test
    public void testConfiguration() throws Exception {
        configGenerator.init();
        log.debug(configGenerator.getIncludedTemplates());
        Properties config = configGenerator.getUserConfig();
        assertEquals("default,testinclude",
                config.getProperty("nuxeo.templates"));
        assertEquals("true", config.getProperty("test.nuxeo.conf"));
        assertEquals("true", config.getProperty("test.nuxeo.defaults"));
        assertEquals("true",
                config.getProperty("test.nuxeo.defaults.template.1"));
        assertEquals("true",
                config.getProperty("test.nuxeo.defaults.template.2"));
        assertEquals("true",
                config.getProperty("test.nuxeo.conf.override.defaults"));
        assertEquals(
                "true",
                config.getProperty("test.nuxeo.conf.override.defaults.template"));
        assertEquals("testinclude", config.getProperty("nuxeo.db.name"));
        assertEquals("sa", config.getProperty("nuxeo.db.user"));
    }

    @Test
    public void testGenerateFiles() throws Exception {
        configGenerator.run();
        log.debug(configGenerator.getIncludedTemplates());
        File configDir = new File(nuxeoHome, JBossConfigurator.JBOSS_CONFIG);
        assertTrue(new File(configDir, "test2").exists());
        File generatedFile = new File(configDir.getParentFile(),
                "datasources/default-repository-ds.xml");
        String generatedProperty = new BufferedReader(new FileReader(
                generatedFile)).readLine();
        assertEquals(generatedProperty, propertyToGenerate, generatedProperty);
        // Check windows path parsing
        generatedFile = new File(configDir.getParentFile(),
                "datasources/default-repository-ds_2.xml");
        generatedProperty = new BufferedReader(new FileReader(generatedFile)).readLine();
        assertEquals(generatedProperty, propertyToGenerate2, generatedProperty);
    }

    @Test
    public void testGenerateFiles2() throws Exception {
        configGenerator2.run();
        log.debug(configGenerator2.getIncludedTemplates());
        Properties config = configGenerator2.getUserConfig();
        assertEquals("common,testinclude2",
                config.getProperty("nuxeo.templates"));
        assertEquals("true", config.getProperty("test.nuxeo.conf"));
        assertEquals("true", config.getProperty("test.nuxeo.defaults"));
        assertEquals("true",
                config.getProperty("test.nuxeo.defaults.template.1"));
        assertEquals("true",
                config.getProperty("test.nuxeo.defaults.template.2"));
        assertEquals("true",
                config.getProperty("test.nuxeo.conf.override.defaults"));
        assertEquals(
                "true",
                config.getProperty("test.nuxeo.conf.override.defaults.template"));
        assertEquals("testinclude", config.getProperty("nuxeo.db.name"));
        assertEquals("sa", config.getProperty("nuxeo.db.user"));
        File configDir = new File(nuxeoHome, JBossConfigurator.JBOSS_CONFIG);
        assertTrue(new File(configDir, "test2").exists());
        File generatedFile = new File(configDir.getParentFile(),
                "datasources/default-repository-ds.xml");
        String generatedProperty = new BufferedReader(new FileReader(
                generatedFile)).readLine();
        assertEquals(generatedProperty, propertyToGenerate, generatedProperty);
    }

    @Test
    public void testForceGeneration() throws ConfigurationException {
        configGenerator2.run();
        File testFile = new File(nuxeoHome, JBossConfigurator.JBOSS_CONFIG);
        testFile = new File(testFile, "test2");
        testFile.delete();
        configGenerator2.setForceGeneration(true);
        configGenerator2.run();
        assertTrue(testFile.exists());
    }

}
