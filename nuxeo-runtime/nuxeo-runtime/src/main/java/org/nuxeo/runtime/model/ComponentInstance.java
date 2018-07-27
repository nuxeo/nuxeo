/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.model;

/**
 * A component instance is a proxy to the component implementation object.
 * <p>
 * Component instance objects are created each time a component is activated, and destroyed at component deactivation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ComponentInstance extends ComponentContext, Extensible, Adaptable {

    /**
     * Gets the actual component implementation instance.
     *
     * @return the component implementation instance
     */
    Object getInstance();

    /**
     * Gets the name of the component.
     *
     * @return the component name
     */
    ComponentName getName();

    /**
     * Gets the runtime context attached to this instance.
     *
     * @return the runtime context
     */
    RuntimeContext getContext();

    /**
     * Activates the implementation instance.
     */
    void activate();

    /**
     * Starts the implementation instance.
     *
     * @since 9.3
     */
    void start();

    /**
     * Stops the implementation instance.
     *
     * @since 9.3
     */
    void stop() throws InterruptedException;

    /**
     * Deactivates the implementation instance.
     */
    void deactivate();

    /**
     * Destroys this instance.
     */
    void destroy();

    /**
     * Reload the component. All the extensions and registries are reloaded.
     *
     * @deprecated since 9.3, but in fact since 5.6, only usage in
     *             {@link org.nuxeo.runtime.model.impl.RegistrationInfoImpl}
     */
    @Deprecated
    void reload();

    /**
     * Gets the list of provided services, or null if no service is provided.
     *
     * @return an array containing the service class names or null if no service is provided
     */
    String[] getProvidedServiceNames();

    /**
     * Gets the Registration Info
     *
     * @return the registration info
     * @since 10.3
     */
    RegistrationInfo getRegistrationInfo();

}
