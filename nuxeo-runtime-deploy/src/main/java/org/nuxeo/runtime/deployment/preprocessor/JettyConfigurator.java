/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.deployment.preprocessor;

import java.io.File;

/**
 * @author jcarsique
 * 
 */
public class JettyConfigurator extends ServerConfigurator {

    public static final String JETTY_CONFIG = "config/sql.properties";

    /**
     * @param configurationGenerator
     */
    public JettyConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if "config" files directory already exists
     */
    protected boolean isConfigured() {
        log.debug("Detected Jetty server. Template configuration not yet implemented for Jetty.");
        return true;
//        return new File(generator.getNuxeoHome(), JETTY_CONFIG).exists();
    }

    @Override
    protected File getOutputDirectory() {
        return generator.getNuxeoHome();
    }

}
