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

package org.nuxeo.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.1
 */
public class NuxeoTomcatLauncher extends NuxeoLauncher {

    /**
     * @param configurationGenerator
     */
    public NuxeoTomcatLauncher(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    @Override
    protected Collection<? extends String> getServerProperties() {
        ArrayList<String> serverProperties = new ArrayList<String>();
        serverProperties.add("-Djava.util.logging.manager="
                + "org.apache.juli.ClassLoaderLogManager");
        serverProperties.add("-Dcatalina.base="
                + configurationGenerator.getNuxeoHome().getPath());
        serverProperties.add("-Dcatalina.home="
                + configurationGenerator.getNuxeoHome().getPath());
        return serverProperties;
    }

    @Override
    protected String getClassPath() {
        String cp = ".";
        cp = addToClassPath(cp, "nxserver" + File.separator + "lib");
        cp = addToClassPath(cp, "bin" + File.separator + "bootstrap.jar");
        return cp;
    }

    @Override
    protected void setServerStartCommand(List<String> command) {
        command.add(TomcatConfigurator.STARTUP_CLASS);
        command.add("start");
    }

    @Override
    protected void setServerStopCommand(List<String> command) {
        command.add(TomcatConfigurator.STARTUP_CLASS);
        command.add("stop");
    }

    @Override
    protected String getServerPrint() {
        return TomcatConfigurator.STARTUP_CLASS;
    }

    @Override
    protected void prepareWizardStart() {
        try {
            // Make Tomcat start only wizard application, not Nuxeo
            File serverXMLBase = new File(
                    configurationGenerator.getNuxeoHome(), "conf");
            File contextXMLBase = new File(
                    configurationGenerator.getNuxeoHome(), "conf"
                            + File.separator + "Catalina" + File.separator
                            + "localhost");
            File webappsBase = new File(configurationGenerator.getNuxeoHome(),
                    "webapps");

            // manage server.xml
            File nuxeoServerXML = new File(serverXMLBase, "server.xml");
            File nuxeoServerXMLBackup = new File(serverXMLBase, "server.xml.nx");
            nuxeoServerXMLBackup.delete();
            FileUtils.moveFile(nuxeoServerXML, nuxeoServerXMLBackup);
            FileUtils.copyFile(new File(serverXMLBase, "server.xml.bak"),
                    nuxeoServerXML);

            // manage nuxeo.xml
            File nuxeoContextXMLBackup = new File(contextXMLBase,
                    "nuxeo.xml.nx");
            nuxeoContextXMLBackup.delete();
            FileUtils.moveFile(new File(contextXMLBase, "nuxeo.xml"),
                    nuxeoContextXMLBackup);

            // manage wizard WAR
            File wizardWAR = new File(webappsBase, "nuxeo-wizard.war");
            if (wizardWAR.exists()) {
                File nuxeoWAR = new File(webappsBase, "nuxeo.war");
                nuxeoWAR.delete();
                FileUtils.moveFile(wizardWAR, nuxeoWAR);
            }

            String paramsStr = "";
            for (String param : params) {
                paramsStr += " " + param;
            }
            System.setProperty(
                    ConfigurationGenerator.PARAM_WIZARD_RESTART_PARAMS,
                    paramsStr);
        } catch (IOException e) {
            log.error(
                    "Could not change Tomcat configuration to run wizard instead of Nuxeo.",
                    e);
        }
    }

    @Override
    public boolean isWizardRequired() {
        File webappsBase = new File(configurationGenerator.getNuxeoHome(),
                "webapps");
        return (configurationGenerator.isWizardRequired() && (new File(
                webappsBase, "nuxeo-wizard.war").exists() || new File(
                webappsBase, "nuxeo.war").exists()));
    }

    @Override
    protected void checkTomcatXMLConfFiles() {
        try {
            // Ensure Tomcat will start only Nuxeo, not the wizard application
            File serverXMLBase = new File(
                    configurationGenerator.getNuxeoHome(), "conf");
            File contextXMLBase = new File(
                    configurationGenerator.getNuxeoHome(), "conf"
                            + File.separator + "Catalina" + File.separator
                            + "localhost");
            File webappsBase = new File(configurationGenerator.getNuxeoHome(),
                    "webapps");

            // manage server.xml
            File nuxeoServerXMLBackup = new File(serverXMLBase, "server.xml.nx");
            if (nuxeoServerXMLBackup.exists()) {
                File nuxeoServerXML = new File(serverXMLBase, "server.xml");
                nuxeoServerXML.delete();
                FileUtils.moveFile(nuxeoServerXMLBackup, nuxeoServerXML);
            }

            // manage nuxeo.xml
            File nuxeoContextXMLBackup = new File(contextXMLBase,
                    "nuxeo.xml.nx");
            if (nuxeoContextXMLBackup.exists()) {
                File nuxeoContextXML = new File(contextXMLBase, "nuxeo.xml");
                nuxeoContextXML.delete();
                FileUtils.moveFile(nuxeoContextXMLBackup, nuxeoContextXML);
            }

            // manage wizard WAR
            File nuxeoWAR = new File(webappsBase, "nuxeo.war");
            if (nuxeoWAR.exists()) {
                File wizardWAR = new File(webappsBase, "nuxeo-wizard.war");
                wizardWAR.delete();
                FileUtils.moveFile(nuxeoWAR, wizardWAR);
            }
        } catch (IOException e) {
            log.error(
                    "Could not change Tocmat configuration to run Nuxeo instead of wizard.",
                    e);
        }
    }
}
