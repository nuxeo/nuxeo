/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.launcher.config;

import java.io.File;
import java.util.Properties;

/**
 * @author jcarsique
 */
public class JBossConfigurator extends ServerConfigurator {

    /**
     * @deprecated Use {@link #getConfigPath()}
     */
    @Deprecated
    public static final String JBOSS_CONFIG = "server/default/deploy/nuxeo.ear/config";

    public static final String DEFAULT_CONFIGURATION = "default";

    /**
     * @since 5.4.2
     */
    public static final String STARTUP_CLASS = "org.jboss.Main";

    /**
     * @since 5.4.2
     */
    public static final String SHUTDOWN_CLASS = "org.jboss.Shutdown";

    private String configuration = null;

    /**
     * @since 5.6
     */
    public static final String JBOSS_HOME_DIR = "jboss.home.dir";

    public String getConfiguration() {
        if (configuration == null) {
            Properties userConfig = generator.getUserConfig();
            if (userConfig != null) {
                configuration = generator.getUserConfig().getProperty(
                        "org.nuxeo.ecm.jboss.configuration",
                        DEFAULT_CONFIGURATION);
            } else {
                configuration = System.getProperty(
                        "org.nuxeo.ecm.jboss.configuration",
                        DEFAULT_CONFIGURATION);
            }
        }
        return configuration;
    }

    public JBossConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if "config" files directory already exists
     */
    @Override
    public boolean isConfigured() {
        log.info("Detected JBoss server.");
        return getConfigDir().exists();
    }

    public String getConfigPath() {
        return getEARPath() + File.separator + "config";
    }

    private String getEARPath() {
        return "server" + File.separator + getConfiguration() + File.separator
                + "deploy" + File.separator + "nuxeo.ear";
    }

    @Override
    public String getDefaultDataDir() {
        final String defaultDataDir = "server" + File.separator
                + getConfiguration() + File.separator + "data" + File.separator
                + "NXRuntime" + File.separator + "data";
        return defaultDataDir;
    }

    @Override
    public File getLogConfFile() {
        return new File(generator.getNuxeoHome(), "server" + File.separator
                + getConfiguration() + File.separator + "conf" + File.separator
                + "jboss-log4j.xml");
    }

    @Override
    public void checkPaths() throws ConfigurationException {
        super.checkPaths();
        checkPaths(getConfiguration());
        if (!getConfiguration().equals(DEFAULT_CONFIGURATION)) {
            checkPaths(DEFAULT_CONFIGURATION);
        }

        File oldPath = new File(generator.getDataDir(), "NXRuntime"
                + File.separator + "binaries");
        String message = "Please move 'binaries' directory from"
                + oldPath.getParent() + "to " + generator.getDataDir();
        checkPath(oldPath, message);
    }

    private void checkPaths(String jbossConfig) throws ConfigurationException {
        File oldPath = new File(generator.getNuxeoHome(), "server"
                + File.separator + jbossConfig + File.separator + "data"
                + File.separator + "h2");
        String message = "Please move 'h2' and 'derby' directories from"
                + oldPath.getParent() + "to " + generator.getDataDir();
        checkPath(oldPath, message);

        oldPath = new File(generator.getNuxeoHome(), "server" + File.separator
                + jbossConfig + File.separator + "data" + File.separator
                + "derby");
        checkPath(oldPath, message);

        oldPath = new File(generator.getNuxeoHome(), "server" + File.separator
                + jbossConfig + File.separator + "data" + File.separator
                + "NXRuntime" + File.separator + "binaries");
        message = "Please move 'binaries' directory from" + oldPath.getParent()
                + "to " + generator.getDataDir();
        checkPath(oldPath, message);
    }

    @Override
    public String getDefaultTmpDir() {
        final String defaultTmpDir = "server" + File.separator
                + getConfiguration() + File.separator + "tmp";
        return defaultTmpDir;
    }

    @Override
    public File getConfigDir() {
        return new File(generator.getNuxeoHome(), getConfigPath());
    }

    @Override
    public void prepareWizardStart() {
        // Nothing to do
    }

    @Override
    public void cleanupPostWizard() {
        // Nothing to do
    }

    @Override
    public boolean isWizardAvailable() {
        return false;
    }

    @Override
    protected File getRuntimeHome() {
        return new File(generator.getNuxeoHome(), getEARPath());
    }

    @Override
    public File getServerLibDir() {
        return new File(generator.getNuxeoHome(), "common" + File.separator
                + "lib");
    }

}
