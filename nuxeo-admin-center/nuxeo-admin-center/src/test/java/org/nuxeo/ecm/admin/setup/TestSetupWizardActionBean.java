/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.admin.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.admin.setup.TestSetupWizardActionBean.CustomLogFilter;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

import com.google.inject.Inject;

/**
 * @author jcarsique
 *
 */
@RunWith(FeaturesRunner.class)
@Features(LogCaptureFeature.class)
@LogCaptureFeature.FilterWith(value = CustomLogFilter.class)
public class TestSetupWizardActionBean {

    private SetupWizardActionBean setupWizardActionBean;

    private Map<String, String> parameters, advancedParameters;

    private File nuxeoHome, nuxeoConf, expectedNuxeoConf;

    private static final Log log = LogFactory.getLog(TestSetupWizardActionBean.class);

    @Inject
    LogCaptureFeature.Result capturedLog;

    public static class CustomLogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LoggingEvent event) {
            return Level.ERROR.equals(event.getLevel())
                    || Level.WARN.equals(event.getLevel());
        }
    }

    @Before
    public void setUp() throws Exception {
        nuxeoHome = File.createTempFile("nuxeo", null);
        Framework.trackFile(nuxeoHome, nuxeoHome);
        nuxeoHome.delete();
        nuxeoHome.mkdirs();
        System.setProperty(Environment.NUXEO_HOME, nuxeoHome.getPath());

        // Properties required by ConfigurationGenerator
        System.setProperty(Environment.NUXEO_DATA_DIR, new File(nuxeoHome,
                "data").getPath());
        System.setProperty(Environment.NUXEO_LOG_DIR,
                new File(nuxeoHome, "log").getPath());

        nuxeoConf = new File(nuxeoHome, "bin");
        nuxeoConf.mkdirs();
        nuxeoConf = new File(nuxeoConf, ConfigurationGenerator.NUXEO_CONF);
        FileUtils.copy(
                FileUtils.getResourceFileFromContext("configurator/nuxeo.conf"),
                nuxeoConf);
        System.setProperty(ConfigurationGenerator.NUXEO_CONF,
                nuxeoConf.getPath());

        FileUtils.copy(FileUtils.getResourceFileFromContext("templates/jboss"),
                new File(nuxeoHome, "templates"));
        System.setProperty("jboss.home.dir", nuxeoHome.getPath());

        setupWizardActionBean = new SetupWizardActionBean();
        // simulate Seam injection of variable setupConfigGenerator
        setupWizardActionBean.getConfigurationGenerator();

        /*
         * WARN [UnknownServerConfigurator] Unknown server.
         * WARN [ConfigurationGenerator] Server will be considered as not
         * configurable.
         * ERROR [ConfigurationGenerator] Template 'oldchange' not found with
         * relative or absolute path (...)
         * WARN [ConfigurationGenerator] Missing value for nuxeo.db.type, using
         * default
         */
        capturedLog.assertHasEvent();
        assertEquals(4, capturedLog.getCaughtEvents().size());
    }

    @Test
    public void testReadParameters() {
        parameters = setupWizardActionBean.getParameters();
        advancedParameters = setupWizardActionBean.getAdvancedParameters();
        assertEquals("0.0.0.0", parameters.get("nuxeo.bind.address"));
        assertNull(advancedParameters.get("nuxeo.bind.address"));
        assertNull(parameters.get("test.nuxeo.conf"));
        assertEquals("true", advancedParameters.get("test.nuxeo.conf"));
        assertNull(parameters.get("test.root.nuxeo.defaults"));
        assertEquals("true", advancedParameters.get("test.root.nuxeo.defaults"));
        assertNull(parameters.get("test.default.nuxeo.defaults"));
        assertEquals("true",
                advancedParameters.get("test.default.nuxeo.defaults"));
    }

    @Test
    public void testSaveParameters() throws IOException {
        parameters = setupWizardActionBean.getParameters();
        advancedParameters = setupWizardActionBean.getAdvancedParameters();
        parameters.put("nuxeo.bind.address", "127.0.0.1");
        parameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME,
                "postgresql");
        advancedParameters.put("test.default.nuxeo.defaults", "false");
        setupWizardActionBean.saveParameters();
        log.debug("Generated nuxeoConf: " + nuxeoConf);
        expectedNuxeoConf = FileUtils.getResourceFileFromContext("configurator/nuxeo.conf.expected");
        BufferedReader bfNew = new BufferedReader(new FileReader(nuxeoConf));
        BufferedReader bfExp = new BufferedReader(new FileReader(
                expectedNuxeoConf));
        String newStr, expStr;
        while ((newStr = bfNew.readLine()) != null) {
            expStr = bfExp.readLine();
            if (newStr.startsWith(ConfigurationGenerator.BOUNDARY_BEGIN)) {
                // BOUNDARY is generated, we can't test an exact match
                assertTrue(expStr.startsWith(ConfigurationGenerator.BOUNDARY_BEGIN));
            } else if (newStr.startsWith(ConfigurationGenerator.PARAM_STATUS_KEY)) {
                // server.status.key is generated, we can't test an exact match
                assertTrue(expStr.startsWith(ConfigurationGenerator.PARAM_STATUS_KEY));
            } else {
                assertEquals(expStr, newStr);
            }
        }
        expStr = bfExp.readLine();
        assertNull(expStr);
        bfNew.close();
        bfExp.close();
    }
}
