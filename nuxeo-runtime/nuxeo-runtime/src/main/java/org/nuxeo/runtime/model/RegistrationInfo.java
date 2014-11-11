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

import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.Version;

/**
 * The component registration info.
 * <p>
 * A registration info object is keeping all the information needed to deploy a
 * component, like the component implementation, properties, dependencies and
 * also the defined extension points and contributed extensions.
 * <p>
 * When a component is activated the registration info is creating a component
 * instance using the current runtime context.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RegistrationInfo extends Serializable {

    int UNREGISTERED = 0;

    int REGISTERED = 1;

    int RESOLVED = 2;

    int ACTIVATED = 3;

    int ACTIVATING = 4;

    int DEACTIVATING = 5;

    /**
     * Gets the component version.
     */
    Version getVersion();

    /**
     * Get the owner bundle symbolic name of that component. If null the default
     * owner is used.
     *
     * @return
     */
    String getBundle();

    /**
     * Gets any comments on this component.
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
     * Gets the list of aliases.
     *
     * @return the aliases
     */
    Set<ComponentName> getAliases();

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
     * Whether this component is disabled. For now this is used only for
     * persistent components.
     *
     * @return
     */
    boolean isDisabled();

    /**
     * Gets the component instance or null if the component was not yet
     * activated.
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
     * Checks whether this component is resolved (i&dot;e&dot; all its
     * dependencies are satisfied).
     *
     * @return true if the component is resolved, false otherwise
     */
    boolean isResolved();

    /**
     * Gets the list of provided services or null if no service is provided.
     *
     * @return an array containing the service class names or null if no service
     *         are provided
     */
    String[] getProvidedServiceNames();

    /**
     * Whether or not this registration is persisted by the user (not part of a
     * real bundle).
     *
     * @return true if persisted, false otherwise
     */
    boolean isPersistent();

    /**
     * Set the persistent flag on this registration
     *
     * @param isPersistent
     */
    void setPersistent(boolean isPersistent);

    /**
     * Give the class name for the component implementation if this is a java
     * component
     *
     * @return class name
     */
    String getImplementation();

    /**
     * Retrieve the URL of the XML file used to declare the component
     *
     * @return the XML file URL
     */
    URL getXmlFileUrl();

    /**
     * The component notification order for {@link #notifyApplicationStarted}.
     *
     * @return the order, 1000 by default
     *
     * @since 5.6
     */
    int getApplicationStartedOrder();

    /**
     * Notify the component instance that the Nuxeo application started
     *
     * @throws Exception
     */
    void notifyApplicationStarted() throws Exception;

    /**
     * @return
     * @since 5.7
     */
	void activate() throws Exception;
}
