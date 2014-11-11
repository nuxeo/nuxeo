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

package org.nuxeo.launcher.config;

import java.io.File;

import org.nuxeo.common.Environment;

/**
 * @author jcarsique
 */
public class JettyConfigurator extends ServerConfigurator {

    public static final String JETTY_CONFIG = "config/sql.properties";

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
        log.debug("Detected Jetty server.");
    }

    /**
     * @return true if {@link #JETTY_CONFIG} file directory already exists
     */
    @Override
    protected boolean isConfigured() {
        return new File(generator.getNuxeoHome(), JETTY_CONFIG).exists();
    }

    @Override
    public void checkPaths() throws ConfigurationException {
        super.checkPaths();
        // Currently no check for Jetty.
    }

    @Override
    public File getLogConfFile() {
        return new File(getConfigDir(), "log4j.xml");
    }

    @Override
    public File getConfigDir() {
        return new File(generator.getNuxeoHome(),
                Environment.DEFAULT_CONFIG_DIR);
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
