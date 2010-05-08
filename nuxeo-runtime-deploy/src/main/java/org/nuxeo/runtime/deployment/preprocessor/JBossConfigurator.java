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
public class JBossConfigurator extends ServerConfigurator {

    public static final String JBOSS_CONFIG = "server/default/deploy/nuxeo.ear/config";

    /**
     * @param configurationGenerator
     */
    public JBossConfigurator(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    /**
     * @return true if "config" files directory already exists
     */
    public boolean isConfigured() {
        log.debug("Detected JBoss server.");
        return new File(generator.getNuxeoHome(), JBOSS_CONFIG).exists();
    }

    @Override
    protected File getOutputDirectory() {
        return new File(generator.getNuxeoHome(),
                new File(JBOSS_CONFIG).getParent());
    }

}
