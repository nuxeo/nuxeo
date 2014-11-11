/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.launcher.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

/**
 * @author jcarsique
 */
public class TomcatConfigurator extends ServerConfigurator {

    /**
     * @deprecated Use {@link #getTomcatConfig()}
     */
    @Deprecated
    public static final String TOMCAT_CONFIG = "conf/Catalina/localhost/nuxeo.xml";

    public static final String DEFAULT_DATA_DIR = "nxserver" + File.separator
            + "data";

    /**
     * @since 5.4.2
     */
    public static final String DEFAULT_TMP_DIR = "tmp";

    /**
     * @since 5.4.2
     */
    public static final String STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

    private String contextName = null;

    /**
     * @since 5.6
     */
    public static final String TOMCAT_HOME = "tomcat.home";

    public TomcatConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if {@link #getTomcatConfig()} file already exists
     */
    @Override
    protected boolean isConfigured() {
        log.info("Detected Tomcat server.");
        return new File(generator.getNuxeoHome(), getTomcatConfig()).exists();
    }

    @Override
    protected String getDefaultDataDir() {
        return DEFAULT_DATA_DIR;
    }

    @Override
    public void checkPaths() throws ConfigurationException {
        super.checkPaths();
        File oldPath = new File(getRuntimeHome(), "data" + File.separator
                + "vcsh2repo");
        String message = "Please rename 'vcsh2repo' directory from "
                + oldPath
                + "to "
                + new File(generator.getDataDir(), "h2" + File.separator
                        + "nuxeo");
        checkPath(oldPath, message);

        oldPath = new File(getRuntimeHome(), "data" + File.separator + "derby"
                + File.separator + "nxsqldirectory");
        message = "It is not possible to migrate Derby data."
                + System.getProperty("line.separator")
                + "Please remove 'nx*' directories from "
                + oldPath.getParent()
                + System.getProperty("line.separator")
                + "or edit templates/default/"
                + getTomcatConfig()
                + System.getProperty("line.separator")
                + "following http://hg.nuxeo.org/nuxeo/nuxeo-distribution/raw-file/release-5.3.2/nuxeo-distribution-resources/src/main/resources/templates-tomcat/default/conf/Catalina/localhost/nuxeo.xml";
        checkPath(oldPath, message);
    }

    @Override
    public String getDefaultTmpDir() {
        return DEFAULT_TMP_DIR;
    }

    @Override
    public File getLogConfFile() {
        return new File(getServerLibDir(), "log4j.xml");
    }

    @Override
    public File getConfigDir() {
        return new File(getRuntimeHome(), "config");
    }

    /**
     * @since 5.4.2
     * @return Path to Tomcat configuration of Nuxeo context
     */
    public String getTomcatConfig() {
        return "conf" + File.separator + "Catalina" + File.separator
                + "localhost" + File.separator + getContextName() + ".xml";
    }

    /**
     * @return Configured context name
     * @since 5.4.2
     */
    public String getContextName() {
        if (contextName == null) {
            Properties userConfig = generator.getUserConfig();
            if (userConfig != null) {
                contextName = generator.getUserConfig().getProperty(
                        ConfigurationGenerator.PARAM_CONTEXT_PATH,
                        DEFAULT_CONTEXT_NAME).substring(1);
            } else {
                contextName = DEFAULT_CONTEXT_NAME.substring(1);
            }
        }
        return contextName;
    }

    @Override
    public void prepareWizardStart() {
        try {
            // overwrite server.xml with server.xml.nx (default Tomcat file)
            File serverXMLBase = new File(generator.getNuxeoHome(), "conf");
            File nuxeoServerXML = new File(serverXMLBase, "server.xml");
            File nuxeoServerXMLOrg = new File(serverXMLBase, "server.xml.nx");
            nuxeoServerXML.delete();
            FileUtils.moveFile(nuxeoServerXMLOrg, nuxeoServerXML);

            // remove Tomcat configuration of Nuxeo context
            File contextXML = new File(generator.getNuxeoHome(),
                    getTomcatConfig());
            contextXML.delete();

            // deploy wizard WAR
            File wizardWAR = new File(generator.getNuxeoHome(), "templates"
                    + File.separator + "nuxeo-wizard.war");
            File nuxeoWAR = new File(generator.getNuxeoHome(), "webapps"
                    + File.separator + getContextName() + ".war");
            nuxeoWAR.delete();
            FileUtils.copyFile(wizardWAR, nuxeoWAR);
        } catch (IOException e) {
            log.error(
                    "Could not change Tomcat configuration to run wizard instead of Nuxeo.",
                    e);
        }
    }

    @Override
    public void cleanupPostWizard() {
        File nuxeoWAR = new File(generator.getNuxeoHome(), "webapps"
                + File.separator + getContextName());
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
        File wizardWAR = new File(generator.getNuxeoHome(), "templates"
                + File.separator + "nuxeo-wizard.war");
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
}
