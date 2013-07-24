/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model;

import java.net.URL;

import org.nuxeo.runtime.RuntimeService;
import org.osgi.framework.Bundle;

/**
 * The runtime context.
 * <p>
 * Runtime contexts are used to create components. They provides custom methods
 * to load classes and find resources.
 * <p>
 * Runtime contexts are generally attached to a bundle context (or module
 * deployment context)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface RuntimeContext {

    int UNREGISTERED = 0;

    int REGISTERED = 1;

    int RESOLVED = 2;

    int ACTIVATING = 3;

    int ACTIVATED = 4;

    /**
     * Gets the current runtime service.
     *
     * @return the runtime service
     */
    RuntimeService getRuntime();

    /**
     * Gets the container bundle or null if we are not running in an OSGi
     * environment.
     */
    Bundle getBundle();

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
     * Returns this bundle class loader
     * @return
     */
    ClassLoader getClassLoader();

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
     * Returns the registration info of the new deployed component or null if
     * the component was not deployed.
     *
     * @param url the url of the XML descriptor
     * @return the component registration info or null if registration failed
     *         for some reason
     * @throws Exception if any error occurs
     */
    RegistrationInfo[] deploy(URL url) throws RuntimeModelException;

    /**
     * Same as {@link #deploy(URL)} but using a {@link StreamRef} as argument.
     *
     * @param ref
     * @return
     * @throws Exception
     */
    RegistrationInfo[] deploy(StreamRef ref) throws RuntimeModelException;

    /**
     * Undeploys a component XML descriptor given its URL.
     * <p>
     * Do nothing if no component was registered for the given URL.
     *
     * @param url the URL of the XML descriptor
     * @throws Exception if any error occurs
     */
    void undeploy(URL url) throws RuntimeModelException;

    /**
     * Same as {@link #undeploy(URL)} but using a {@link StreamRef} as stream
     * reference.
     *
     * @param ref
     * @throws Exception
     */
    void undeploy(StreamRef ref) throws RuntimeModelException;

    /**
     * Checks whether the component XML file at given URL was deployed.
     *
     * @param url the URL to check
     * @return true it deployed, false otherwise
     */
    boolean isDeployed(URL url);

    /**
     * Checks whether the component XML file given by the StreamRef argument was
     * deployed.
     *
     * @param ref
     * @return
     */
    boolean isDeployed(StreamRef ref);

    /**
     * Deploys the component whose XML descriptor is at the given location.
     * <p>
     * If the component is already deployed do nothing.
     * <p>
     * The location is interpreted as a relative path inside the bundle (jar)
     * containing the component - and will be loaded using
     * {@link RuntimeContext#getLocalResource(String)}.
     * <p>
     * Returns the registration info of the new deployed component or null if
     * the component was not deployed.
     *
     * @param location the location
     * @return the component registration info or null if registration failed
     *         for some reason
     * @throws Exception
     */
    RegistrationInfo[] deploy(String location) throws RuntimeModelException;

    /**
     * Undeploys the component at the given location if any was deployed.
     * <p>
     * If the component was not deployed do nothing.
     *
     * @param location the location of the component to undeploy
     * @throws Exception if any error occurs
     */
    void undeploy(String location) throws RuntimeModelException;

    /**
     * Checks if the component at the given location is deployed.
     *
     * @param location the component location to check
     * @return true if deployed, false otherwise
     */
    boolean isDeployed(String location);

    /**
     * @since 5.7
     */
    boolean isActivated();

    /**
     * Destroys this context.
     */
    void destroy();

    /**
     * @since 5.7
     */
    int getState();


}
