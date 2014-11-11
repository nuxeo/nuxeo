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
public class TomcatConfigurator extends ServerConfigurator {

    public static final String TOMCAT_CONFIG = "conf/Catalina/localhost/nuxeo.xml";

    public static final String DEFAULT_DATA_DIR = "nxserver" + File.separator
            + "data";

    /**
     * @since 5.4.1
     */
    public static final String DEFAULT_TMP_DIR = "tmp";

    /**
     * @since 5.4.1
     */
    public static final String STARTUP_CLASS = "org.apache.catalina.startup.Bootstrap";

    public TomcatConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if "config" files directory already exists
     */
    @Override
    protected boolean isConfigured() {
        log.info("Detected Tomcat server.");
        return new File(generator.getNuxeoHome(), TOMCAT_CONFIG).exists();
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
        File logFile = new File(generator.getNuxeoHome(), "lib"
                + File.separator + "log4j.xml");
        try {
            System.out.println("Configuring logs with " + logFile);
            System.setProperty(Environment.NUXEO_LOG_DIR, getLogDir().getPath());
            DOMConfigurator.configure(logFile.toURI().toURL());
            log.info("Logs succesfully configured.");
        } catch (MalformedURLException e) {
            log.error("Could not initialize logs with " + logFile, e);
        }
    }

    @Override
    public void checkPaths() throws ConfigurationException {
        super.checkPaths();
        File oldPath = new File(generator.getNuxeoHome(), "nxserver"
                + File.separator + "data" + File.separator + "vcsh2repo");
        String message = "Please rename 'vcsh2repo' directory from "
                + oldPath
                + "to "
                + new File(generator.getDataDir(), "h2" + File.separator
                        + "nuxeo");
        checkPath(oldPath, message);

        oldPath = new File(generator.getNuxeoHome(), "nxserver"
                + File.separator + "data" + File.separator + "derby"
                + File.separator + "nxsqldirectory");
        message = "It is not possible to migrate Derby data."
                + System.getProperty("line.separator")
                + "Please remove 'nx*' directories from "
                + oldPath.getParent()
                + System.getProperty("line.separator")
                + "or edit templates/default/conf/Catalina/localhost/nuxeo.xml"
                + System.getProperty("line.separator")
                + "following http://hg.nuxeo.org/nuxeo/nuxeo-distribution/raw-file/release-5.3.2/nuxeo-distribution-resources/src/main/resources/templates-tomcat/default/conf/Catalina/localhost/nuxeo.xml";
        checkPath(oldPath, message);
    }

    @Override
    public String getDefaultTmpDir() {
        return DEFAULT_TMP_DIR;
    }

}
