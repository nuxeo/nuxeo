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
 */
package org.nuxeo.launcher.config;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;

/**
 * @author jcarsique
 */
public class TomcatConfigurator extends ServerConfigurator {

    /**
     * @since 5.4.2
     */
    public static final String STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

    private String contextName = null;

    /**
     * @since 5.6
     */
    public static final String TOMCAT_HOME = "tomcat.home";

    /**
     * @since 5.7
     */
    public static final String PARAM_HTTP_TOMCAT_ADMIN_PORT = "nuxeo.server.tomcat_admin.port";

    public TomcatConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
        log.info("Detected Tomcat server.");
    }

    /**
     * @return true if {@link #getTomcatConfig()} file already exists
     */
    @Override
    protected boolean isConfigured() {
        return new File(generator.getNuxeoHome(), getTomcatConfig()).exists();
    }

    @Override
    protected String getDefaultDataDir() {
        return "nxserver" + File.separator + Environment.DEFAULT_DATA_DIR;
    }

    @Override
    public void checkPaths() throws ConfigurationException {
        super.checkPaths();
        File oldPath = new File(getRuntimeHome(), "data" + File.separator + "vcsh2repo");
        String message = String.format("NXP-5370, NXP-5460. " + "Please rename 'vcsh2repo' directory from %s to %s",
                oldPath, new File(generator.getDataDir(), "h2" + File.separator + "nuxeo"));
        checkPath(oldPath, message);

        oldPath = new File(getRuntimeHome(), "data" + File.separator + "derby" + File.separator + "nxsqldirectory");
        message = "NXP-5370, NXP-5460. " + "It is not possible to migrate Derby data."
                + System.getProperty("line.separator") + "Please remove 'nx*' directories from " + oldPath.getParent()
                + System.getProperty("line.separator") + "or edit templates/default/" + getTomcatConfig()
                + System.getProperty("line.separator")
                + "following https://github.com/nuxeo/nuxeo-distribution/blob/release-5.3.2/nuxeo-distribution-resources/src/main/resources/templates-tomcat/default/conf/Catalina/localhost/nuxeo.xml";
        checkPath(oldPath, message);
    }

    @Override
    public File getLogConfFile() {
        return new File(getServerLibDir(), "log4j.xml");
    }

    @Override
    public File getConfigDir() {
        return new File(getRuntimeHome(), Environment.DEFAULT_CONFIG_DIR);
    }

    /**
     * @since 5.4.2
     * @return Path to Tomcat configuration of Nuxeo context
     */
    public String getTomcatConfig() {
        return "conf" + File.separator + "Catalina" + File.separator + "localhost" + File.separator + getContextName()
                + ".xml";
    }

    /**
     * @return Configured context name
     * @since 5.4.2
     */
    public String getContextName() {
        if (contextName == null) {
            Properties userConfig = generator.getUserConfig();
            if (userConfig != null) {
                contextName = generator.getUserConfig()
                                       .getProperty(ConfigurationGenerator.PARAM_CONTEXT_PATH, DEFAULT_CONTEXT_NAME)
                                       .substring(1);
            } else {
                contextName = DEFAULT_CONTEXT_NAME.substring(1);
            }
        }
        return contextName;
    }

    @Override
    public void prepareWizardStart() {
        try {
            // remove Tomcat configuration of Nuxeo context
            File contextXML = new File(generator.getNuxeoHome(), getTomcatConfig());
            contextXML.delete();

            // deploy wizard WAR
            File wizardWAR = new File(generator.getNuxeoHome(), "templates" + File.separator + "nuxeo-wizard.war");
            File nuxeoWAR = new File(generator.getNuxeoHome(), "webapps" + File.separator + getContextName() + ".war");
            nuxeoWAR.delete();
            FileUtils.copyFile(wizardWAR, nuxeoWAR);
        } catch (IOException e) {
            log.error("Could not change Tomcat configuration to run wizard instead of Nuxeo.", e);
        }
    }

    @Override
    public void cleanupPostWizard() {
        File nuxeoWAR = new File(generator.getNuxeoHome(), "webapps" + File.separator + getContextName());
        if (nuxeoWAR.exists()) {
            try {
                FileUtils.deleteDirectory(nuxeoWAR);
            } catch (IOException e) {
                log.error("Could not delete " + nuxeoWAR, e);
            }
        }
        nuxeoWAR = new File(nuxeoWAR.getPath() + ".war");
        if (nuxeoWAR.exists()) {
            if (!FileUtils.deleteQuietly(nuxeoWAR)) {
                log.warn("Could not delete " + nuxeoWAR);
                try {
                    nuxeoWAR.deleteOnExit();
                } catch (SecurityException e) {
                    log.warn("Cannot delete " + nuxeoWAR);
                }
            }
        }
    }

    @Override
    public boolean isWizardAvailable() {
        File wizardWAR = new File(generator.getNuxeoHome(), "templates" + File.separator + "nuxeo-wizard.war");
        return wizardWAR.exists();
    }

    @Override
    public File getRuntimeHome() {
        return new File(generator.getNuxeoHome(), "nxserver");
    }

    @Override
    public File getServerLibDir() {
        return new File(generator.getNuxeoHome(), "lib");
    }

    @Override
    protected void checkNetwork() throws ConfigurationException {
        InetAddress bindAddress = generator.getBindAddress();
        ConfigurationGenerator.checkPortAvailable(bindAddress,
                Integer.parseInt(generator.getUserConfig().getProperty(PARAM_HTTP_TOMCAT_ADMIN_PORT)));
    }

    @Override
    protected void addServerSpecificParameters(Map<String, String> parametersmigration) {
        parametersmigration.put("nuxeo.server.tomcat-admin.port", PARAM_HTTP_TOMCAT_ADMIN_PORT);
    }

}
