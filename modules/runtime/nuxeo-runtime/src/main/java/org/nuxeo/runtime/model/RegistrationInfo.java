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

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.model.impl.ComponentRegistry;

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
public interface RegistrationInfo {

    int UNREGISTERED = 0;

    int REGISTERED = 1;

    /**
     * Component dependencies are resolved
     */
    int RESOLVED = 2;

    /**
     * Before component activation
     */
    int ACTIVATING = 3;

    int DEACTIVATING = 4;

    /**
     * Component activation successful
     */
    int ACTIVATED = 5;

    /**
     * Notification of applicationStarted fails
     *
     * @since 7.4
     */
    int START_FAILURE = 6;

    /**
     * Component was started
     *
     * @since 9.2
     */
    int STARTED = 7;

    /**
     * The component is being started
     */
    int STARTING = 8;

    /**
     * The component is being stopped
     */
    int STOPPING = 9;

    /**
     * Gets the component version.
     */
    Version getVersion();

    /**
     * Get the owner bundle symbolic name of that component. If null the default owner is used.
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
     * Gets the defined extension points with name.
     *
     * @param name the extension point name to retrieve
     * @return the defined extension points with name
     * @since 9.3
     */
    default Optional<ExtensionPoint> getExtensionPoint(String name) {
        return Stream.of(getExtensionPoints()).filter(xp -> xp.getName().equals(name)).findFirst();
    }

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
     * Returns true if the component is not enabled.
     * <p>
     * To be deployed, a component should be enabled and should not be disabled.
     *
     * @see #isEnabled()
     */
    boolean isDisabled();

    /**
     * Returns true if the component is enabled.
     * <p>
     * To be deployed, a component should be enabled and should not be disabled.
     *
     * @see #isDisabled()
     * @since 11.5
     */
    boolean isEnabled();

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
     * Checks whether this component is resolved (i.e. all its dependencies are satisfied).
     *
     * @return true if the component is resolved, false otherwise
     */
    boolean isResolved();

    /**
     * Checks whether this component is started
     *
     * @since 9.2
     */
    boolean isStarted();

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
     * The id of the content source used to create the registration (usually a StreamRef id)
     *
     * @since 9.2
     */
    String getSourceId();

    /**
     * The component notification order for {@link ComponentManager#start()}.
     *
     * @return the order, 1000 by default
     * @since 5.6
     */
    int getApplicationStartedOrder();

    /**
     * DON'T USE THIS METHOD - INTERNAL API.
     *
     * @param state the state to set in this registration info
     * @since 9.3
     */
    void setState(int state);

    /**
     * DON'T USE THIS METHOD - INTERNAL API.
     * <p>
     * This flag is used to introduce a new component lifecycle mechanism in order to introduce java pojo contribution.
     * This allow us to rework how component manager handles the component, without changing how it handles component
     * created by XML contributions (current behavior).
     *
     * @return whether or not {@link ComponentManager} or {@link ComponentRegistry} should use the former way to manage
     *         component lifecycle.
     * @since 9.3
     */
    default boolean useFormerLifecycleManagement() {
        return false;
    }

}
