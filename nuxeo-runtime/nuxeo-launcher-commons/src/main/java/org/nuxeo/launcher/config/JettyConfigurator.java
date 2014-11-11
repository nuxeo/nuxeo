/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.net.MalformedURLException;

import org.apache.log4j.xml.DOMConfigurator;

/**
 * @author jcarsique
 */
public class JettyConfigurator extends ServerConfigurator {

    public static final String JETTY_CONFIG = "config/sql.properties";

    public static final String DEFAULT_DATA_DIR = "data";

    /**
     * @since 5.4.1
     */
    public static final String DEFAULT_TMP_DIR = "tmp";

    /**
     * @since 5.4.1
     */
    public static final String LAUNCHER_CLASS = "org.nuxeo.runtime.launcher.Main";

    /**
     * @since 5.4.1
     */
    public static final String STARTUP_CLASS = "org.nuxeo.osgi.application.Main";

    public JettyConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if "config" files directory already exists
     */
    @Override
    protected boolean isConfigured() {
        log.info("Detected Jetty server.");
        return new File(generator.getNuxeoHome(), JETTY_CONFIG).exists();
    }

    @Override
    protected File getOutputDirectory() {
        return generator.getNuxeoHome();
    }

    @Override
    protected String getDefaultDataDir() {
        return DEFAULT_DATA_DIR;
    }

    @Override
    public void initLogs() {
        File logFile = new File(generator.getNuxeoHome(), "config"
                + File.separator + "log4j.xml");
        try {
            System.out.println("Try to configure logs with " + logFile);
            DOMConfigurator.configure(logFile.toURI().toURL());
            log.info("Logs succesfully configured.");
        } catch (MalformedURLException e) {
            log.error("Could not initialize logs with " + logFile, e);
        }
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

}
