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

/**
 * @author jcarsique
 */
public class JettyConfigurator extends ServerConfigurator {

    public static final String JETTY_CONFIG = "config/sql.properties";

    public static final String DEFAULT_DATA_DIR = "data";

    /**
     * @since 5.4.2
     */
    public static final String DEFAULT_TMP_DIR = "tmp";

    /**
     * @since 5.4.2
     */
    public static final String LAUNCHER_CLASS = "org.nuxeo.runtime.launcher.Main";

    /**
     * @since 5.4.2
     */
    public static final String STARTUP_CLASS = "org.nuxeo.osgi.application.Main";

    /**
     * @since 5.6
     */
    public static final String JETTY_HOME = "jetty.home";

    public JettyConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if {@link #JETTY_CONFIG} file directory already exists
     */
    @Override
    protected boolean isConfigured() {
        log.info("Detected Jetty server.");
        return new File(generator.getNuxeoHome(), JETTY_CONFIG).exists();
    }

    @Override
    protected String getDefaultDataDir() {
        return DEFAULT_DATA_DIR;
    }

    @Override
    public void checkPaths() throws ConfigurationException {
        super.checkPaths();
        // Currently no check for Jetty.
    }

    @Override
    public String getDefaultTmpDir() {
        return DEFAULT_TMP_DIR;
    }

    @Override
    public File getLogConfFile() {
        return new File(getConfigDir(), "log4j.xml");
    }

    @Override
    public File getConfigDir() {
        return new File(generator.getNuxeoHome(), "config");
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
        return generator.getNuxeoHome();
    }

    @Override
    public File getServerLibDir() {
        return getNuxeoLibDir();
    }

}
