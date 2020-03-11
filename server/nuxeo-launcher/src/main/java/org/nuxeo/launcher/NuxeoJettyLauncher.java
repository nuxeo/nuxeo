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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.JettyConfigurator;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class NuxeoJettyLauncher extends NuxeoLauncher {

    public NuxeoJettyLauncher(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getClassPath() {
        String cp = ".";
        cp = addToClassPath(cp, "lib");
        cp = addToClassPath(cp, "nuxeo-runtime-launcher-*.jar");
        cp = addToClassPath(cp, "bundles");
        return cp;
    }

    @Override
    protected void setServerStartCommand(List<String> command) {
        command.add(JettyConfigurator.STARTUP_CLASS);
        command.add("-home");
        command.add(configurationGenerator.getNuxeoHome().getPath());
    }

    @Override
    protected void setServerStopCommand(List<String> command) {
        command.add(JettyConfigurator.STARTUP_CLASS);
        command.add("stop");
    }

    @Override
    protected Collection<? extends String> getServerProperties() {
        return new ArrayList<>();
    }

    @Override
    protected String getServerPrint() {
        return JettyConfigurator.STARTUP_CLASS;
    }

    @Override
    protected String getShutdownClassPath() {
        return getClassPath();
    }

}
