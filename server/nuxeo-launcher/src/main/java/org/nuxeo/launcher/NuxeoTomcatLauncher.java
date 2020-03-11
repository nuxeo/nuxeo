/*
 * (C) Copyright 2010-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Frantz Fischer <ffischer@nuxeo.com>
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

    protected String getBinJarName(File binDir, String pattern) {
        File[] binJarFiles = ConfigurationGenerator.getJarFilesFromPattern(binDir, pattern);
        if (binJarFiles.length != 1) {
            throw new RuntimeException("There should be only 1 file but " + binJarFiles.length + " were found in " + binDir.getAbsolutePath() + " looking for " + pattern);
        }
        return binDir.getName() + File.separator + binJarFiles[0].getName();
    }

    @Override
    protected String getClassPath() {
        File binDir = configurationGenerator.getNuxeoBinDir();
        String cp = ".";
        cp = addToClassPath(cp, "nxserver" + File.separator + "lib");
        cp = addToClassPath(cp, getBinJarName(binDir, ConfigurationGenerator.BOOTSTRAP_JAR_REGEX));
        // Tomcat 7 needs tomcat-juli.jar for bootstrap as well
        cp = addToClassPath(cp, getBinJarName(binDir, ConfigurationGenerator.JULI_JAR_REGEX));
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
