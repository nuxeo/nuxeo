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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.JBossConfigurator;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class NuxeoJBossLauncher extends NuxeoLauncher {

    private static final String BIND_ADDRESS_PARAM = "nuxeo.bind.address";

    private static final String BIND_ADDRESS_DEFAULT = "0.0.0.0";

    /**
     * @param configurationGenerator
     */
    public NuxeoJBossLauncher(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    @Override
    protected String getClassPath() {
        String cp = ".";
        cp = addToClassPath(cp, "bin" + File.separator + "run.jar");
        return cp;
    }

    @Override
    protected void setServerStartCommand(List<String> command) {
        command.add(JBossConfigurator.STARTUP_CLASS);
        command.add("start");
        command.add("-b");
        command.add(configurationGenerator.getUserConfig().getProperty(
                BIND_ADDRESS_PARAM, BIND_ADDRESS_DEFAULT));
    }

    @Override
    protected void setServerStopCommand(List<String> command) {
        command.add(JBossConfigurator.SHUTDOWN_CLASS);
        command.add("--shutdown");
    }

    @Override
    protected Collection<? extends String> getServerProperties() {
        ArrayList<String> serverProperties = new ArrayList<String>();
        // serverProperties.add("-Dprogram.name=nuxeoctl");
        serverProperties.add("-Djava.endorsed.dirs="
                + new File(configurationGenerator.getNuxeoHome(), "lib"
                        + File.separator + "endorsed"));
        serverProperties.add("-Djboss.server.log.dir="
                + configurationGenerator.getLogDir());
        serverProperties.add("-Djboss.server.temp.dir="
                + configurationGenerator.getTmpDir());
        if (overrideJavaTmpDir) {
            serverProperties.add("-Djboss.server.temp.dir.overrideJavaTmpDir=true");
        }
        return serverProperties;
    }

    @Override
    protected String getServerPrint() {
        return JBossConfigurator.STARTUP_CLASS;
    }

}
