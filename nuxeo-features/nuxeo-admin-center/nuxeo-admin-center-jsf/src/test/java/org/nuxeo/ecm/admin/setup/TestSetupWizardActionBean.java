/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.admin.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.admin.setup.TestSetupWizardActionBean.CustomLogFilter;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author jcarsique
 */
@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, RuntimeFeature.class })
@LogCaptureFeature.FilterWith(value = CustomLogFilter.class)
public class TestSetupWizardActionBean {

    private SetupWizardActionBean setupWizardActionBean;

    private Map<String, Serializable> parameters, advancedParameters;

    private File nuxeoHome, nuxeoConf, expectedNuxeoConf;

    private static final Log log = LogFactory.getLog(TestSetupWizardActionBean.class);

    @Inject
    LogCaptureFeature.Result capturedLog;

    public static class CustomLogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LogEvent event) {
            return Level.ERROR.equals(event.getLevel()) || Level.WARN.equals(event.getLevel());
        }
    }

    @Before
    public void setUp() throws Exception {
        Environment env = Environment.getDefault();
        nuxeoHome = env.getServerHome();
        nuxeoConf = new File(nuxeoHome, "bin");
        nuxeoConf.mkdirs();
        nuxeoConf = new File(nuxeoConf, ConfigurationGenerator.NUXEO_CONF);
        FileUtils.copy(FileUtils.getResourceFileFromContext("configurator/nuxeo.conf"), nuxeoConf);
        System.setProperty(ConfigurationGenerator.NUXEO_CONF, nuxeoConf.getPath());

        FileUtils.copy(FileUtils.getResourceFileFromContext("templates/jboss"), new File(nuxeoHome, "templates"));
        System.setProperty("jboss.home.dir", nuxeoHome.getPath());

        setupWizardActionBean = new SetupWizardActionBean();
        // simulate Seam injection of variable setupConfigGenerator
        setupWizardActionBean.getConfigurationGenerator();

        // WARN [UnknownServerConfigurator] Unknown server.
        // WARN [ConfigurationGenerator] Server will be considered as not configurable.
        // WARN [ConfigurationGenerator] Parameter mail.transport.username is deprecated ...
        // ERROR [ConfigurationGenerator] Template 'oldchange' not found ...
        // WARN [ConfigurationGenerator] Parameter mail.transport.username is deprecated ...
        // WARN [ConfigurationGenerator] Missing value for nuxeo.db.type, using default
        capturedLog.assertHasEvent();
        assertEquals(6, capturedLog.getCaughtEvents().size());
    }

    @After
    public void tearDown() {
        System.clearProperty(ConfigurationGenerator.NUXEO_CONF);
        System.clearProperty(Environment.NUXEO_HOME);
        System.clearProperty("jboss.home.dir");
        System.clearProperty(Environment.NUXEO_DATA_DIR);
        System.clearProperty(Environment.NUXEO_LOG_DIR);
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
        assertEquals("true", advancedParameters.get("test.default.nuxeo.defaults"));
    }

    @Test
    public void testSaveParameters() throws IOException {
        parameters = setupWizardActionBean.getParameters();
        advancedParameters = setupWizardActionBean.getAdvancedParameters();
        parameters.put("nuxeo.bind.address", "127.0.0.1");
        parameters.put(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME, "postgresql");
        advancedParameters.put("test.default.nuxeo.defaults", "false");
        setupWizardActionBean.saveParameters();
        log.debug("Generated nuxeoConf: " + nuxeoConf);
        expectedNuxeoConf = FileUtils.getResourceFileFromContext("configurator/nuxeo.conf.expected");
        BufferedReader bfNew = new BufferedReader(new FileReader(nuxeoConf));
        BufferedReader bfExp = new BufferedReader(new FileReader(expectedNuxeoConf));
        String newStr, expStr;
        while ((newStr = bfNew.readLine()) != null) {
            expStr = bfExp.readLine();
            if (newStr.startsWith(ConfigurationGenerator.BOUNDARY_BEGIN)) {
                // BOUNDARY is generated, we can't test an exact match
                assertTrue(expStr.startsWith(ConfigurationGenerator.BOUNDARY_BEGIN));
            } else if (newStr.startsWith(Environment.SERVER_STATUS_KEY)) {
                // server.status.key is generated, we can't test an exact match
                assertTrue(expStr.startsWith(Environment.SERVER_STATUS_KEY));
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
