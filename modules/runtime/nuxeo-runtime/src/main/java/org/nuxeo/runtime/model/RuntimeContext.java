/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.net.URL;

import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.osgi.framework.Bundle;

/**
 * The runtime context.
 * <p>
 * Runtime contexts are used to create components. They provides custom methods to load classes and find resources.
 * <p>
 * Runtime contexts are generally attached to a bundle context (or module deployment context) Note that all undeploy
 * methods are deprectaed! see {@link DefaultRuntimeContext} for more information
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RuntimeContext {

    /**
     * Gets the current runtime service.
     *
     * @return the runtime service
     */
    RuntimeService getRuntime();

    /**
     * Gets the container bundle or null if we are not running in an OSGi environment.
     */
    Bundle getBundle();

    /**
     * Get the component names deployed by this context
     *
     * @return the list of components. Return an empty array if no components where deployed.
     * @since 9.2
     */
    ComponentName[] getComponents();

    /**
     * Loads the class given its name.
     *
     * @param className the class name
     * @return the class object
     * @throws ClassNotFoundException if no such class were found
     * @see ClassLoader#loadClass(String)
     */
    Class<?> loadClass(String className) throws ClassNotFoundException;

    /**
     * Finds a resource having the given name.
     *
     * @param name the resource name
     * @return an URL to the resource having that name or null if not was found
     * @see ClassLoader#getResource(String)
     */
    URL getResource(String name);

    /**
     * Finds a local resource having the given name.
     * <p>
     * Only the current bundle should be searched for the resource.
     *
     * @param name the local resource name
     * @return an URL to the resource having that name or null if not was found
     * @see ClassLoader#getResource(String)
     */
    URL getLocalResource(String name);

    /**
     * Deploys a component XML descriptor given its URL.
     * <p>
     * Do nothing if component is already deployed.
     * <p>
     * Returns the registration info of the new deployed component or null if the component was not deployed.
     *
     * @param url the url of the XML descriptor
     * @return the component registration info or null if registration failed for some reason
     */
    RegistrationInfo deploy(URL url) throws IOException;

    /**
     * Same as {@link #deploy(URL)} but using a {@link StreamRef} as argument.
     */
    RegistrationInfo deploy(StreamRef ref) throws IOException;

    /**
     * Undeploys a component XML descriptor given its URL.
     * <p>
     * Do nothing if no component was registered for the given URL.
     *
     * @param url the URL of the XML descriptor
     */
    void undeploy(URL url) throws IOException;

    /**
     * Same as {@link #undeploy(URL)} but using a {@link StreamRef} as stream reference.
     */
    void undeploy(StreamRef ref) throws IOException;

    /**
     * Checks whether the component XML file at given URL was deployed.
     *
     * @param url the URL to check
     * @return true it deployed, false otherwise
     */
    boolean isDeployed(URL url);

    /**
     * Checks whether the component XML file given by the StreamRef argument was deployed.
     */
    boolean isDeployed(StreamRef ref);

    /**
     * Deploys the component whose XML descriptor is at the given location.
     * <p>
     * If the component is already deployed do nothing.
     * <p>
     * The location is interpreted as a relative path inside the bundle (jar) containing the component - and will be
     * loaded using {@link RuntimeContext#getLocalResource(String)}.
     * <p>
     * Returns the registration info of the new deployed component or null if the component was not deployed.
     *
     * @param location the location
     * @return the component registration info or null if registration failed for some reason
     */
    RegistrationInfo deploy(String location);

    /**
     * Undeploys the component at the given location if any was deployed.
     * <p>
     * If the component was not deployed do nothing.
     *
     * @param location the location of the component to undeploy
     */
    void undeploy(String location);

    /**
     * Checks if the component at the given location is deployed.
     *
     * @param location the component location to check
     * @return true if deployed, false otherwise
     */
    boolean isDeployed(String location);

    /**
     * Destroys this context.
     */
    void destroy();

}
