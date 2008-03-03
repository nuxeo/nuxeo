/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model;

import java.util.Collection;

import org.nuxeo.runtime.ComponentListener;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
     * @param reg the main registry
     * @param ri the registration info
     * @return true if the object should be added to the main registry, false
     *         otherwise
     */
    void register(RegistrationInfo ri);

    /**
     * Handles the unregistration of the given registration info.
     * <p>
     * This is called by the main registry when the object is unregistered.
     * <p>
     * If true is returned, the object will be removed from the main registry.
     *
     * @param reg the main registry
     * @param ri the registration info
     * @return true if the object should be removed from the main registry,
     *         false otherwise
     */
    void unregister(RegistrationInfo ri);

    /**
     * Unregisters a component given its name.
     *
     * @param name the component name
     * @throws Exception if any error occurs
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
     * Gets the registrated components.
     *
     * @return a read only collection of components
     */
    Collection<RegistrationInfo> getRegistrations();

    /**
     * Gets the list of pending registrations.
     *
     * @return the pending registrations or an empty collection if none
     */
    Collection<ComponentName> getPendingRegistrations();

    /**
     * Gets the number of registered objects in this registry.
     *
     * @return the number fo registered objects
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
     * service
     *
     * @param <T> the service type
     * @param serviceClass the service class
     * @return the service object
     */
    <T> T getService(Class<T> serviceClass);

}
