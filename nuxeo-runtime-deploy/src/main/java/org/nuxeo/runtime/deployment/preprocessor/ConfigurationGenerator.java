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

import org.nuxeo.launcher.config.ConfigurationException;

/**
 * Builder for server configuration and datasource files from templates and
 * properties.
 *
 * @author jcarsique
 * @deprecated Use {@link org.nuxeo.launcher.config.ConfigurationGenerator}
 */
@Deprecated
public class ConfigurationGenerator {

    /**
     * Delegate call to
     * {@link #org.nuxeo.launcher.config.ConfigurationGenerator.main(String[])}
     *
     * @param args
     * @throws ConfigurationException
     */
    public static void main(String[] args) throws ConfigurationException {
        throw new RuntimeException(ConfigurationGenerator.class
                + " is deprecated."
                + " Use org.nuxeo.launcher.config.ConfigurationGenerator "
                + "(org.nuxeo.runtime:nuxeo-launcher-commons)");
    }

}
