/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.model;

import java.io.Serializable;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.Version;

/**
 * The component registration info.
 * <p>
 * A registration info object is keeping all the information needed to deploy a component, like the component
 * implementation, properties, dependencies and also the defined extension points and contributed extensions.
 * <p>
 * When a component is activated the registration info is creating a component instance using the current runtime
 * context.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RegistrationInfo extends Serializable {

    int UNREGISTERED = 0;

    int REGISTERED = 1;

    /**
     * Component dependencies are resolved
     */
    int RESOLVED = 2;

    /**
     * Component activation successful
     */
    int ACTIVATED = 3;

    /**
     * Before component activation
     */
    int ACTIVATING = 4;

    int DEACTIVATING = 5;

    /**
     * Notification of applicationStarted fails
     *
     * @since 7.4
     */
    int START_FAILURE = 6;

    /**
     * Gets the component version.
     */
    Version getVersion();

    /**
     * Get the owner bundle symbolic name of that component. If null the default owner is used.
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
     * Whether this component is disabled. For now this is used only for persistent components.
     *
     * @return
     */
    boolean isDisabled();

    /**
     * Gets the component instance or null if the component was not yet activated.
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
     * Checks whether this component is resolved (i&dot;e&dot; all its dependencies are satisfied).
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
     * Whether or not this registration is persisted by the user (not part of a real bundle).
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
     * Give the class name for the component implementation if this is a java component
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
     * @since 5.6
     */
    int getApplicationStartedOrder();

    /**
     * Notify the component instance that the Nuxeo application started
     */
    void notifyApplicationStarted();


    /**
     * Notify the component instance that the Nuxeo is about to shutdown
     */
    void notifyApplicationStandby(Instant instant);
}
