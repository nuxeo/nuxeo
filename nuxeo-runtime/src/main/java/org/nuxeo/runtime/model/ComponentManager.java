/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.runtime.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.ComponentListener;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface ComponentManager {

    /**
     * Adds a component listener.
     * <p>
     * Does nothing if the given listener is already registered.
     *
     * @param listener the component listener to add
     */
    void addComponentListener(ComponentListener listener);

    /**
     * Removes a component listener.
     * <p>
     * Does nothing if the given listener is not registered.
     *
     * @param listener the component listener to remove
     */
    void removeComponentListener(ComponentListener listener);

    /**
     * Handles the registration of the given registration info.
     * <p>
     * This is called by the main registry when all dependencies of this
     * registration info were solved and the object can be registered.
     * <p>
     * If true is returned, the object will be added to the main registry under
     * the name given in RegistrationInfo.
     *
     * @param ri the registration info
     * @return
     * @throws RuntimeModelException
     */
    void register(RegistrationInfo ri) throws RuntimeModelException;

    /**
     * Handles the unregistration of the given registration info.
     * <p>
     * This is called by the main registry when the object is unregistered.
     * <p>
     * If true is returned, the object will be removed from the main registry.
     *
     * @param ri the registration info
     */
    void unregister(RegistrationInfo ri);

    /**
     * Unregisters a component given its name.
     *
     * @param name the component name
     */
    void unregister(ComponentName name);

    /**
     * Gets the component if there is one having the given name.
     *
     * @param name the component name
     * @return the component if any was registered with that name, null otherwise
     */
    RegistrationInfo getRegistrationInfo(ComponentName name);

    /**
     * Gets object instance managed by the named component.
     *
     * @param name the object name
     * @return the object instance if any. may be null
     */
    ComponentInstance getComponent(ComponentName name);

    /**
     * Checks whether or not a component with the given name was registered.
     *
     * @param name the object name
     * @return true if an object with the given name was registered, false otherwise
     */
    boolean isRegistered(ComponentName name);

    /**
     * Gets the registered components.
     *
     * @return a read-only collection of components
     */
    Collection<RegistrationInfo> getRegistrations();

    /**
     * Gets the pending registrations and their dependencies.
     *
     * @return the pending registrations
     */
    Map<ComponentName, Set<RegistrationInfo>> getPendingRegistrations();

    /**
     * Gets the pending extensions by component.
     *
     * @return the pending extensions
     */
    Collection<ComponentName> getActivatingRegistrations();

    /**
     * Gets the number of registered objects in this registry.
     *
     * @return the number of registered objects
     */
    int size();

    /**
     * Shuts down the component registry.
     * <p>
     * This unregisters all objects registered in this registry.
     */
    void shutdown();

    /**
     * Gets the service of type serviceClass if such a service was declared by a
     * resolved runtime component.
     * <p>
     * If the component is not yet activated it will be prior to return the
     * service.
     *
     * @param <T> the service type
     * @param serviceClass the service class
     * @return the service object
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * Get the list of all registered service names
     * An empty array is returned if no registered services are found.
     *
     * @return an array of registered service.
     */
    String[] getServices();

    /**
     * Gets the component that provides the given service.
     *
     * @param serviceClass the service class
     * @return the component or null if none
     */
    ComponentInstance getComponentProvidingService(Class<?> serviceClass);

    Set<String> getBlacklist();

    void setBlacklist(Set<String> blacklist);

}
