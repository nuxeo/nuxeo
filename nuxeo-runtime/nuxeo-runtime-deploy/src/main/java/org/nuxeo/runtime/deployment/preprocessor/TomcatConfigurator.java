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
 */
public class TomcatConfigurator extends ServerConfigurator {

    public static final String TOMCAT_CONFIG = "conf/Catalina/localhost/nuxeo.xml";

    public TomcatConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if "config" files directory already exists
     */
    @Override
    protected boolean isConfigured() {
        log.info("Detected Tomcat server.");
        return new File(generator.getNuxeoHome(), TOMCAT_CONFIG).exists();
    }

    @Override
    protected File getOutputDirectory() {
        return generator.getNuxeoHome();
    }

}
