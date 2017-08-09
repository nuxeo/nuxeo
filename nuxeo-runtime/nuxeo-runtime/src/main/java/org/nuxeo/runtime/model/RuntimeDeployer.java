/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.runtime.model;

import org.nuxeo.runtime.RuntimeService;

/**
 * This interface represents a Runtime deployer. This kind of services are used by the {@link RuntimeService} to deploy
 * new components to Nuxeo.
 *
 * @since 9.3
 */
public interface RuntimeDeployer {

    /**
     * @param component the component, could be a Java class name, a XML file, a YAML file...
     * @return whether or not this deployer should be used
     */
    boolean accept(String component);

    /**
     * Deploys the component declared as it in the OSGI bundle.
     * <p />
     * Returns the registration info of the new deployed component or null if the component was not deployed.
     *
     * @param component the component, could be a Java class name, a XML file, a YAML file...
     * @return the component registration info or null if registration failed for some reason
     */
    RegistrationInfo deploy(String component);

}
