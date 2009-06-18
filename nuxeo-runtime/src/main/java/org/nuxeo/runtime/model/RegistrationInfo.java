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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.Version;

/**
 * The component registration info.
 * <p>
 * A registration info object is keeping all the information needed to deploy
 * a component, like the component implementation, properties, dependencies and
 * also the defined extension points and contributed extensions.
 * <p>
 * When a component is activated the registration info is creating a component
 * instance using the current runtime context.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RegistrationInfo extends Serializable {

    int UNREGISTERED    = 0;
    int REGISTERED      = 1;
    int RESOLVED        = 2;
    int ACTIVATED       = 3;
    int ACTIVATING      = 4;
    int DEACTIVATING    = 5;

    /**
     * Gets the component version.
     *
     * @return
     */
    Version getVersion();

    /**
     * Gets any comments on this component.
     *
     * @return
     */
    String getDocumentation();

    /**
     * Gets the runtime context that created this registration info.
     *
     * @return the runtime context
     */
    RuntimeContext getContext();

    /**
     * Gets the component properties.
     *
     * @return the component properties
     */
    Map<String, Property> getProperties();

    /**
     * Gets the list of the required components.
     *
     * @return the required components
     */
    Set<ComponentName> getRequiredComponents();

    /**
     * Gets the defined extension points.
     *
     * @return the defined extension points
     */
    ExtensionPoint[] getExtensionPoints();

    /**
     * Gets the extensions contributed by this component.
     *
     * @return the contributed extensions
     */
    Extension[] getExtensions();

    /**
     * Gets the name of the component.
     *
     * @return the component name
     */
    ComponentName getName();

    /**
     * Gets the component instance or null if the component
     * was not yet activated.
     *
     * @return the component instance
     */
    ComponentInstance getComponent();

    /**
     * Gets the component state.
     *
     * @return the component state
     */
    int getState();

    /**
     * Gets the component manager.
     *
     * @return the component manager
     */
    ComponentManager getManager();

    /**
     * Checks whether this component is activated.
     *
     * @return true if the component is activated, false otherwise
     */
    boolean isActivated();

    /**
     * Checks whether this component is resolved (i.e. all its dependencies are satisfied).
     *
     * @return true if the component is resolved, false otherwise
     */
    boolean isResolved();

    /**
     * Gets the list of provided services or null if no service is provided.
     *
     * @return an array containing the service class names or null if no service are provided
     */
    String[] getProvidedServiceNames();


    /**
     * Whether or not this registration is persisted by the user (not part of a real bundle)
     * @return true if persisted, false otherwise
     */
    boolean isPersistent();

}
