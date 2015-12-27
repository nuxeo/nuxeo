/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.deployment.preprocessor;

import org.nuxeo.launcher.config.ConfigurationException;

/**
 * Builder for server configuration and datasource files from templates and properties.
 *
 * @author jcarsique
 * @deprecated Since 5.4.1. Use {@link org.nuxeo.launcher.config.ConfigurationGenerator}
 */
@Deprecated
public class ConfigurationGenerator {

    /**
     * Delegate call to {@link #org.nuxeo.launcher.config.ConfigurationGenerator.main(String[])}
     *
     * @param args
     * @throws ConfigurationException
     */
    public static void main(String[] args) throws ConfigurationException {
        throw new RuntimeException(ConfigurationGenerator.class + " is deprecated."
                + " Use org.nuxeo.launcher.config.ConfigurationGenerator "
                + "(org.nuxeo.runtime:nuxeo-launcher-commons)");
    }

}
