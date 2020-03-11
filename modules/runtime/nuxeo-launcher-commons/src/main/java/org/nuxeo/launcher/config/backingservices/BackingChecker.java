/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.launcher.config.backingservices;

import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * A backing checker checks for the availability of a backing service.
 *
 * @since 9.2
 */
public interface BackingChecker {

    /**
     * Test if the check has to be done for the given configuration.
     * @param cg The current configuration
     * @return true if {@link BackingChecker#check(ConfigurationGenerator)} has to be called.
     */
    boolean accepts(ConfigurationGenerator cg);

    /**
     * Test the availbilty of the backing service.
     * @param cg The current configuration
     * @throws ConfigurationException if backing service is not available.
     */
    void check(ConfigurationGenerator cg) throws ConfigurationException;
}
