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
            // overwrite server.xml with server.xml.nx (default Tomcat file)
            File serverXMLBase = new File(
                    configurationGenerator.getNuxeoHome(), "conf");
            File nuxeoServerXML = new File(serverXMLBase, "server.xml");
            File nuxeoServerXMLOrg = new File(serverXMLBase, "server.xml.nx");
            nuxeoServerXML.delete();
            FileUtils.moveFile(nuxeoServerXMLOrg, nuxeoServerXML);

            // remove conf/Catalina/localhost/nuxeo.xml
            File contextXML = new File(configurationGenerator.getNuxeoHome(),
                    "conf" + File.separator + "Catalina" + File.separator
                            + "localhost" + File.separator + "nuxeo.xml");
            contextXML.delete();

            // deploy wizard WAR
            File wizardWAR = new File(configurationGenerator.getNuxeoHome(),
                    "templates" + File.separator + "nuxeo-wizard.war");
            File nuxeoWAR = new File(configurationGenerator.getNuxeoHome(),
                    "webapps" + File.separator + "nuxeo.war");
            nuxeoWAR.delete();
            FileUtils.copyFile(wizardWAR, nuxeoWAR);

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
        File wizardWAR = new File(configurationGenerator.getNuxeoHome(),
                "templates" + File.separator + "nuxeo-wizard.war");
        return (configurationGenerator.isWizardRequired() && wizardWAR.exists());
    }

    @Override
    protected void cleanupPostWizard() {
        File nuxeoWAR = new File(configurationGenerator.getNuxeoHome(),
                "webapps" + File.separator + "nuxeo.war");
        nuxeoWAR.delete();
    }
}
