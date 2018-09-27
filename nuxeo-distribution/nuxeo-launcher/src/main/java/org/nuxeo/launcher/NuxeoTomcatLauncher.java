/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class NuxeoTomcatLauncher extends NuxeoLauncher {

    public NuxeoTomcatLauncher(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    @Override
    protected Collection<? extends String> getServerProperties() {
        ArrayList<String> serverProperties = new ArrayList<>();
        File home = configurationGenerator.getNuxeoHome();
        serverProperties.add("-Dcatalina.base=" + home.getPath());
        serverProperties.add("-Dcatalina.home=" + home.getPath());
        return serverProperties;
    }

    @Override
    protected String getClassPath() {
        String cp = ".";
        cp = addToClassPath(cp, "nxserver" + File.separator + "lib");
        cp = addToClassPath(cp, "bin" + File.separator + "bootstrap.jar");
        // Tomcat 7 needs tomcat-juli.jar for bootstrap as well
        cp = addToClassPath(cp, "bin" + File.separator + "tomcat-juli.jar");
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
    protected String getShutdownClassPath() {
        return getClassPath();
    }
}
