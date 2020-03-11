/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;

/**
 * @since 5.7
 */
public class UnknownServerConfigurator extends ServerConfigurator {

    private static final Logger log = LogManager.getLogger(UnknownServerConfigurator.class);

    /**
     * @param configurationGenerator
     */
    public UnknownServerConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
        log.warn("Unknown server.");
    }

    @Override
    boolean isConfigured() {
        return false;
    }

    @Override
    protected File getRuntimeHome() {
        return new File(System.getProperty(Environment.NUXEO_RUNTIME_HOME, generator.getNuxeoHome().getPath()));
    }

    @Override
    public File getLogConfFile() {
        return null;
    }

    @Override
    public File getConfigDir() {
        return new File(System.getProperty(Environment.NUXEO_CONFIG_DIR, getRuntimeHome().getPath() + File.separator
                + Environment.DEFAULT_CONFIG_DIR));
    }

    @Override
    public File getServerLibDir() {
        return getNuxeoLibDir();
    }

}
