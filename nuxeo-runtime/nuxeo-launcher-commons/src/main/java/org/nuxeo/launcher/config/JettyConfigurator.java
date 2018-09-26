/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
        log.info("Detected Jetty server.");
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
        return new File(getConfigDir(), "log4j2.xml");
    }

    @Override
    public File getConfigDir() {
        return new File(generator.getNuxeoHome(), Environment.DEFAULT_CONFIG_DIR);
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
