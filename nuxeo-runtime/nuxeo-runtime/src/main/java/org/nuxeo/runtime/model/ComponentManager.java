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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.runtime.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;

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
     * This is called by the main registry when all dependencies of this registration info were solved and the object
     * can be registered.
     * <p>
     * If true is returned, the object will be added to the main registry under the name given in RegistrationInfo.
     *
     * @param ri the registration info
     */
    void register(RegistrationInfo ri);

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
     * This method was added only to support unregistering by location which is used by some tests. Removing by location
     * should be managed at a higher level (it is useful only for tests) and this method should be removed
     *
     * @param sourceId the location from where the component was deployed
     * @return false if no component was registered from that location, true otherwise
     * @see DefaultRuntimeContext for more on this
     * @since 9.2
     * @deprecated since 9.2
     */
    @Deprecated
    boolean unregisterByLocation(String sourceId);

    /**
     * Given a source location tests if a component was deployed from that location <br>
     * This method was added to support undeploying by location needed by tests. Should be removed and a test specific
     * helper implemented to support locations
     *
     * @deprecated since 9.2
     */
    @Deprecated
    boolean hasComponentFromLocation(String sourceId);

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
    Map<ComponentName, Set<ComponentName>> getPendingRegistrations();

    /**
     * Returns the missing registrations, linked to missing target extension points.
     *
     * @since 8.10
     */
    Map<ComponentName, Set<Extension>> getMissingRegistrations();

    /**
     * Gets the pending extensions by component.
     *
     * @return the pending extensions
     */
    Collection<ComponentName> getActivatingRegistrations();

    /**
     * Gets the resolved component names in the order they were resolved
     *
     * @since 9.2
     */
    Collection<ComponentName> getResolvedRegistrations();

    /**
     * Gets the components that fail on applicationStarted notification
     *
     * @since 7.4
     */
    Collection<ComponentName> getStartFailureRegistrations();

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
     * Gets the service of type serviceClass if such a service was declared by a resolved runtime component.
     * <p>
     * If the component is not yet activated it will be prior to return the service.
     *
     * @param <T> the service type
     * @param serviceClass the service class
     * @return the service object
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * Get the list of all registered service names An empty array is returned if no registered services are found.
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

    /**
     * Activate and start all resolved components. If components were already started do nothing.
     *
     * @return false if components were already started, true otherwise
     * @since 9.2
     */
    boolean start();

    /**
     * Stop and deactivate all resolved components. If components were not yet started do nothing
     *
     * @return false if components were not yet started, true otherwise
     * @since 9.2
     */
    boolean stop();

    /**
     * Same as {@link #stop()} but log a warning if the timeout is reached while stopping components
     *
     * @since 9.2
     */
    void stop(int timeout);

    /**
     * Stop all started components but don't deactivate them. After calling this method you can safely contribute new
     * extensions (i.e. modify extension registries).
     * <p>
     * If any components were previously started do nothing
     *
     * @since 9.2
     */
    void
    standby();

    /**
     * Same as {@link #standby()} but log a warning if the timeout is reached while stopping components
     *
     * @since 9.2
     */
    void standby(int timeout);

    /**
     * Start standby components. If components are not in standby mode the it does nothing.
     *
     * @since 9.2
     */
    void resume();

    /**
     * Make a snapshot of the component registry. When calling restart
     *
     * @since 9.2
     */
    void snapshot();

    /**
     * Optionally reset the registry to the last snapshot and restart the components.
     * <p>
     * When restarting components all components will be stopped, deactivated and re-instantiated. It means that all
     * references to components before a restart will become invalid after the restart.
     * <p>
     * If no snapshot was created then the components will be restarted without changing the registry.
     * <p>
     * If the <code>reset</code> argument is true then the registry will be reverted to the last snapshot before
     * starting the components.
     *
     * @param reset whether or not to revert to the last snapshot
     * @since 9.2
     */
    void restart(boolean reset);

    /**
     * Reset the registry to the last snapshot if any and stop the components (if they are currently started). After a
     * reset all the components are stopped so we can contribute new components if needed. You must call
     * {@link #start()} to start again the components
     *
     * @return true if the components were stopped, false otherwise
     * @since 9.2
     */
    boolean reset();

    /**
     * Refresh the registry using stashed registrations if any. If the <code>reset</code> argument is true then the
     * registry will be reverted to the last snapshot before applying the stash.
     * <p>
     * If the stash is empty it does nothing and return true, otherwise it will:
     * <ol>
     * <li>stop the components (if they are started)
     * <li>revert to the last snapshot (if reset flag is true)
     * <li>apply the stash (the stash will remain empty after this operation)
     * <li>start the components (if they was started)
     * </ol>
     *
     * @param reset whether or not to revert to the last snapshot
     * @return false if stash is empty and nothing was done, true otherwise
     * @since 9.2
     */
    boolean refresh(boolean reset);

    /**
     * Shortcut for refresh(false).
     *
     * @see #refresh(boolean)
     * @since 9.2
     */
    default boolean refresh() {
        return refresh(false);
    }

    /**
     * Tests whether the components were already started.
     *
     * @return true if components are started, false
     * @since 9.2
     */
    boolean isStarted();

    /**
     * Tests whether the components are in standby mode. That means they were started and then stopped - waiting to be
     * started again.
     * <p>
     * When putting components in standby they are stopped but not deactivated. You start back the standby components by
     * calling #resume
     * <p>
     * While in standby mode the component manager remains in running state.
     *
     * @since 9.2
     */
    boolean isStandby();

    /**
     * Tests whether the components are running. That means they are either started either in standby mode.
     *
     * @since 9.2
     */
    boolean isRunning();

    /**
     * Tests whether components were deployed over the initial snapshot (i.e. the actual registry differs from the
     * snapshot) If no snapshot was done returns false.
     *
     * @since 9.2
     */
    boolean hasChanged();

    /**
     * Check if a snapshot was done
     *
     * @return true if a snapshot already exists, false otherwise
     * @since 9.2
     */
    boolean hasSnapshot();

    /**
     * Tests if the stash is empty
     *
     * @since 9.2
     */
    boolean isStashEmpty();

    /**
     * Apply the stash if not empty. This is a low level operation and may not be safe to call when the component
     * manager is running {@link #isRunning()}.
     * <p>
     * For compatibility reasons (to be able to emulate the old hot deploy mechanism or to speed up tests) this method
     * will force a registry refresh in all 3 component manager states: stopped, standby, started.
     * <p>
     * Usually you should apply the stash by calling {@link #refresh()} which is similar to
     * <code>stop(); [restoreSnapshot();] unstash(); start();</code>
     *
     * @since 9.2
     */
    void unstash();

    /**
     * Add a listener to be notified on manager actions like start / stop components.
     *
     * @since 9.2
     */
    void addListener(ComponentManager.Listener listener);

    /**
     * Remove the component manager listener previously added by {@link #addListener(Listener)}. If the listener were
     * not added then nothing is done.
     *
     * @since 9.2
     */
    void removeListener(ComponentManager.Listener listener);

    /**
     * Listener interface for component manager events
     *
     * @author bogdan
     * @since 9.2
     */
    interface Listener {

        /**
         * Called just before activating components. This is fired when entering {@link ComponentManager#start()}
         */
        default void beforeActivation(ComponentManager mgr) {
        }

        /**
         * Called just after all the components were activated.
         */
        default void afterActivation(ComponentManager mgr) {
        }

        /**
         * Called just before activating components.
         */
        default void beforeDeactivation(ComponentManager mgr) {
        }

        /**
         * Called just after all the components were deactivated. This is fired just before exiting from
         * {@link ComponentManager#stop()}.
         */
        default void afterDeactivation(ComponentManager mgr) {
        }

        /**
         * Called just before starting components.
         *
         * @param isResume true if the event was initiated by a {@link ComponentManager#resume()} call, false otherwise.
         */
        default void beforeStart(ComponentManager mgr, boolean isResume) {
        }

        /**
         * Called just after all components were started
         *
         * @param isResume true if the event was initiated by a {@link ComponentManager#resume()} call, false otherwise.
         */
        default void afterStart(ComponentManager mgr, boolean isResume) {
        }

        /**
         * Called just before stopping components.
         *
         * @param isStandby true if the event was initiated by a {@link ComponentManager#standby()} call, false
         *            otherwise
         */
        default void beforeStop(ComponentManager mgr, boolean isStandby) {
        }

        /**
         * Called just after the components were stopped.
         *
         * @param isStandby true if the event was initiated by a {@link ComponentManager#standby()} call, false
         *            otherwise
         */
        default void afterStop(ComponentManager mgr, boolean isStandby) {
        }

        default Listener install() {
            Framework.getRuntime().getComponentManager().addListener(this);
            return this;
        }

        default Listener uninstall() {
            Framework.getRuntime().getComponentManager().removeListener(this);
            return this;
        }

    }

}
