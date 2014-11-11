/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.launcher.config;

import java.io.File;

import org.nuxeo.common.Environment;

/**
 *
 *
 * @since 5.7
 */
public class UnknownServerConfigurator extends ServerConfigurator {

    /**
     * @param configurationGenerator
     */
    public UnknownServerConfigurator(
            ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
        log.warn("Unknown server.");
    }

    @Override
    boolean isConfigured() {
        return false;
    }

    @Override
    protected File getRuntimeHome() {
        return new File(System.getProperty(Environment.NUXEO_RUNTIME_HOME,
                generator.getNuxeoHome().getPath()));
    }

    @Override
    public File getLogConfFile() {
        return null;
    }

    @Override
    public File getConfigDir() {
        return new File(System.getProperty(Environment.NUXEO_CONFIG_DIR,
                getRuntimeHome().getPath() + File.separator
                        + Environment.DEFAULT_CONFIG_DIR));
    }

    @Override
    public File getServerLibDir() {
        return getNuxeoLibDir();
    }

}
