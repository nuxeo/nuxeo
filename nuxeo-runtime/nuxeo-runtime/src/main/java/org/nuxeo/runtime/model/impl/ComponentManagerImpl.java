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
package org.nuxeo.runtime.model.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.util.Watch;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class ComponentManagerImpl implements ComponentManager {

    private static final Log log = LogFactory.getLog(ComponentManagerImpl.class);

    private static final Log infoLog = LogFactory.getLog(ComponentManager.class);

    // must use an ordered Set to avoid loosing the order of the pending
    // extensions
    protected final ConcurrentMap<ComponentName, Set<Extension>> pendingExtensions;

    private ListenerList compListeners;

    /**
     * Manager listeners. Listen too events like start stop restart etc.
     *
     * @since 9.2
     */
    private Listeners listeners;

    private final ConcurrentMap<String, RegistrationInfoImpl> services;

    protected volatile Set<String> blacklist;

    /**
     * The list of started components (sorted according to the start order). This list is null if the components were
     * not yet started or were stopped
     *
     * @since 9.2
     */
    protected volatile List<RegistrationInfoImpl> started;

    /**
     * The list of standby components (sorted according to the start order) This list is null if component were not yet
     * started or not yet put in standby When putting components in standby all started components are stopped and the
     * {@link #started} list is assigned to {@link #standby} list then the {@link #started} field is nullified. When
     * resuming standby components the started list is restored from the standby list and the standby field is nullified
     *
     * @since 9.2
     */
    protected volatile List<RegistrationInfoImpl> standby;

    /**
     * A list of registrations that were deployed while the manager was started.
     *
     * @since 9.2
     */
    protected volatile Stash stash;

    /**
     * @since 9.2
     */
    protected volatile ComponentRegistry registry;

    /**
     * @since 9.2
     */
    protected volatile ComponentRegistry snapshot;

    /**
     * @since 9.2
     */
    protected volatile boolean isFlushingStash = false;

    /**
     * @since 9.2
     */
    protected volatile boolean changed = false;

    public ComponentManagerImpl(RuntimeService runtime) {
        registry = new ComponentRegistry();
        pendingExtensions = new ConcurrentHashMap<>();
        compListeners = new ListenerList();
        listeners = new Listeners();
        services = new ConcurrentHashMap<>();
        blacklist = new HashSet<>();
        stash = new Stash();
    }

    /**
     * @since 9.2
     */
    public final ComponentRegistry getRegistry() {
        return registry;
    }

    @Override
    public Collection<RegistrationInfo> getRegistrations() {
        return registry.getComponents();
    }

    /**
     * @since 9.2
     */
    @Override
    public Collection<ComponentName> getResolvedRegistrations() {
        return registry.getResolvedNames();
    }

    @Override
    public synchronized Map<ComponentName, Set<ComponentName>> getPendingRegistrations() {
        Map<ComponentName, Set<ComponentName>> pending = new HashMap<>();
        for (Map.Entry<ComponentName, Set<ComponentName>> p : registry.getPendingComponents().entrySet()) {
            pending.put(p.getKey(), new LinkedHashSet<>(p.getValue()));
        }
        return pending;
    }

    @Override
    public synchronized Map<ComponentName, Set<Extension>> getMissingRegistrations() {
        Map<ComponentName, Set<Extension>> missing = new HashMap<>();
        // also add pending extensions, not resolved because of missing target extension point
        for (Set<Extension> p : pendingExtensions.values()) {
            for (Extension e : p) {
                missing.computeIfAbsent(e.getComponent().getName(), k -> new LinkedHashSet<>()).add(e);
            }
        }
        return missing;
    }

    /**
     * Get the needed component names. The returned set is not a copy
     */
    public Set<ComponentName> getNeededRegistrations() {
        return pendingExtensions.keySet();
    }

    /**
     * Get the pending extensions. The returned set is not a copy
     */
    public Set<Extension> getPendingExtensions(ComponentName name) {
        return pendingExtensions.get(name);
    }

    @Override
    public RegistrationInfo getRegistrationInfo(ComponentName name) {
        return registry.getComponent(name);
    }

    @Override
    public boolean isRegistered(ComponentName name) {
        return registry.contains(name);
    }

    @Override
    public int size() {
        return registry.size();
    }

    @Override
    public ComponentInstance getComponent(ComponentName name) {
        RegistrationInfo ri = registry.getComponent(name);
        return ri != null ? ri.getComponent() : null;
    }

    @Override
    public synchronized void shutdown() {
        stop();
        compListeners = null;
        registry.destroy();
        registry = null;
        snapshot = null;
    }

    @Override
    public Set<String> getBlacklist() {
        return Collections.unmodifiableSet(blacklist);
    }

    @Override
    public void setBlacklist(Set<String> blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    public synchronized void register(RegistrationInfo regInfo) {
        RegistrationInfoImpl ri = (RegistrationInfoImpl) regInfo;
        ComponentName name = ri.getName();
        if (blacklist.contains(name.getName())) {
            log.warn("Component " + name.getName() + " was blacklisted. Ignoring.");
            return;
        }

        Set<ComponentName> componentsToRemove = stash.toRemove;
        // Look if the component is not going to be removed when applying the stash
        // before checking for duplicates.
        if (!componentsToRemove.contains(name)) {
            if (registry.contains(name)) {
                if (name.getName().startsWith("org.nuxeo.runtime.")) {
                    // XXX we hide the fact that nuxeo-runtime bundles are
                    // registered twice
                    // TODO fix the root cause and remove this
                    return;
                }
                handleError("Duplicate component name: " + name, null);
                return;
            }
            for (ComponentName n : ri.getAliases()) {
                if (registry.contains(n)) {
                    handleError("Duplicate component name: " + n + " (alias for " + name + ")", null);
                    return;
                }
            }
        }

        if (shouldStash()) { // stash the registration
            // should stash before calling ri.attach.
            stash.add(ri);
            return;
        }

        if (hasSnapshot()) {
            // we are modifying the registry after the snapshot was created
            changed = true;
        }

        ri.attach(this);

        try {
            log.info("Registering component: " + name);
            if (!registry.addComponent(ri)) {
                log.info("Registration delayed for component: " + name + ". Waiting for: "
                        + registry.getMissingDependencies(ri.getName()));
            }
        } catch (RuntimeException e) {
            // don't raise this exception,
            // we want to isolate component errors from other components
            handleError("Failed to register component: " + name + " (" + e.toString() + ')', e);
        }
    }

    @Override
    public synchronized void unregister(RegistrationInfo regInfo) {
        unregister(regInfo.getName());
    }

    @Override
    public synchronized void unregister(ComponentName name) {
        if (shouldStash()) { // stash the un-registration
            stash.remove(name);
            return;
        }
        if (hasSnapshot()) {
            changed = true;
        }
        try {
            log.info("Unregistering component: " + name);
            registry.removeComponent(name);
        } catch (RuntimeException e) {
            log.error("Failed to unregister component: " + name, e);
        }
    }

    @Override
    public synchronized boolean unregisterByLocation(String sourceId) {
        ComponentName name = registry.deployedFiles.remove(sourceId);
        if (name != null) {
            unregister(name);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasComponentFromLocation(String sourceId) {
        return registry.deployedFiles.containsKey(sourceId);
    }

    @Override
    public void addComponentListener(ComponentListener listener) {
        compListeners.add(listener);
    }

    @Override
    public void removeComponentListener(ComponentListener listener) {
        compListeners.remove(listener);
    }

    @Override
    public void addListener(ComponentManager.Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ComponentManager.Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public ComponentInstance getComponentProvidingService(Class<?> serviceClass) {
        RegistrationInfoImpl ri = services.get(serviceClass.getName());
        if (ri == null) {
            return null;
        }
        ComponentInstance ci = ri.getComponent();
        if (ci == null) {
            if (log.isDebugEnabled()) {
                log.debug("The component exposing the service " + serviceClass + " is not resolved or not started");
            }
        }
        return ci;
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        ComponentInstance comp = getComponentProvidingService(serviceClass);
        return comp != null ? comp.getAdapter(serviceClass) : null;
    }

    @Override
    public Collection<ComponentName> getActivatingRegistrations() {
        return getRegistrations(RegistrationInfo.ACTIVATING);
    }

    @Override
    public Collection<ComponentName> getStartFailureRegistrations() {
        return getRegistrations(RegistrationInfo.START_FAILURE);
    }

    protected Collection<ComponentName> getRegistrations(int state) {
        RegistrationInfo[] comps = registry.getComponentsArray();
        Collection<ComponentName> ret = new ArrayList<>();
        for (RegistrationInfo ri : comps) {
            if (ri.getState() == state) {
                ret.add(ri.getName());
            }
        }
        return ret;
    }

    void sendEvent(ComponentEvent event) {
        log.debug("Dispatching event: " + event);
        Object[] listeners = this.compListeners.getListeners();
        for (Object listener : listeners) {
            ((ComponentListener) listener).handleEvent(event);
        }
    }

    public synchronized void registerExtension(Extension extension) {
        ComponentName name = extension.getTargetComponent();
        RegistrationInfoImpl ri = registry.getComponent(name);
        if (ri != null && ri.component != null) {
            if (log.isDebugEnabled()) {
                log.debug("Register contributed extension: " + extension);
            }
            loadContributions(ri, extension);
            ri.component.registerExtension(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_REGISTERED,
                    ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
        } else {
            // put the extension in the pending queue
            if (log.isDebugEnabled()) {
                log.debug("Enqueue contributed extension to pending queue: " + extension);
            }
            Set<Extension> extensions = pendingExtensions.get(name);
            if (extensions == null) {
                extensions = new LinkedHashSet<>(); // must keep order in which extensions are contributed
                pendingExtensions.put(name, extensions);
            }
            extensions.add(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_PENDING,
                    ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
        }
    }

    public synchronized void unregisterExtension(Extension extension) {
        // TODO check if framework is shutting down and in that case do nothing
        if (log.isDebugEnabled()) {
            log.debug("Unregister contributed extension: " + extension);
        }
        ComponentName name = extension.getTargetComponent();
        RegistrationInfo ri = registry.getComponent(name);
        if (ri != null) {
            ComponentInstance co = ri.getComponent();
            if (co != null) {
                co.unregisterExtension(extension);
            }
        } else { // maybe it's pending
            Set<Extension> extensions = pendingExtensions.get(name);
            if (extensions != null) {
                // FIXME: extensions is a set of Extensions, not ComponentNames.
                extensions.remove(name);
                if (extensions.isEmpty()) {
                    pendingExtensions.remove(name);
                }
            }
        }
        sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_UNREGISTERED,
                ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
    }

    public static void loadContributions(RegistrationInfoImpl ri, Extension xt) {
        ExtensionPointImpl xp = ri.getExtensionPoint(xt.getExtensionPoint());
        if (xp != null && xp.contributions != null) {
            try {
                Object[] contribs = xp.loadContributions(ri, xt);
                xt.setContributions(contribs);
            } catch (RuntimeException e) {
                handleError("Failed to load contributions for component " + xt.getComponent().getName(), e);
            }
        }
    }

    public synchronized void registerServices(RegistrationInfoImpl ri) {
        if (ri.serviceDescriptor == null) {
            return;
        }
        for (String service : ri.serviceDescriptor.services) {
            log.info("Registering service: " + service);
            services.put(service, ri);
            // TODO: send notifications
        }
    }

    public synchronized void unregisterServices(RegistrationInfoImpl ri) {
        if (ri.serviceDescriptor == null) {
            return;
        }
        for (String service : ri.serviceDescriptor.services) {
            services.remove(service);
            // TODO: send notifications
        }
    }

    @Override
    public String[] getServices() {
        return services.keySet().toArray(new String[services.size()]);
    }

    protected static void handleError(String message, Exception e) {
        log.error(message, e);
        Framework.getRuntime().getWarnings().add(message);
    }

    /**
     * Activate all the resolved components and return the list of activated components in the activation order
     *
     * @return the list of the activated components in the activation order
     * @since 9.2
     */
    protected List<RegistrationInfoImpl> activateComponents() {
        Watch watch = new Watch();
        watch.start();
        listeners.beforeActivation();
        // make sure we start with a clean pending registry
        pendingExtensions.clear();

        List<RegistrationInfoImpl> ris = new ArrayList<>();
        // first activate resolved components
        for (RegistrationInfoImpl ri : registry.getResolvedRegistrationInfo()) {
            // TODO catch and handle errors
            watch.start(ri.getName().getName());
            ri.activate();
            ris.add(ri);
            watch.stop(ri.getName().getName());
        }
        listeners.afterActivation();
        watch.stop();

        if (infoLog.isInfoEnabled()) {
            infoLog.info("Components activated in " + watch.total.formatSeconds() + " sec.");
        }
        writeDevMetrics(watch, "activate");

        return ris;
    }

    /**
     * Deactivate all active components in the reverse resolve order
     *
     * @since 9.2
     */
    protected void deactivateComponents() {
        Watch watch = new Watch();
        watch.start();
        listeners.beforeDeactivation();
        Collection<RegistrationInfoImpl> resolved = registry.getResolvedRegistrationInfo();
        List<RegistrationInfoImpl> reverseResolved = new ArrayList<>(resolved);
        Collections.reverse(reverseResolved);
        for (RegistrationInfoImpl ri : reverseResolved) {
            if (ri.isActivated()) {
                watch.start(ri.getName().getName());
                ri.deactivate(false);
                watch.stop(ri.getName().getName());
            }
        }
        // make sure the pending extension map is empty since we didn't unregistered extensions by calling
        // ri.deactivate(false)
        pendingExtensions.clear();
        listeners.afterDeactivation();
        watch.stop();

        writeDevMetrics(watch, "deactivate");
    }

    /**
     * Start all given components
     *
     * @since 9.2
     */
    protected void startComponents(List<RegistrationInfoImpl> ris, boolean isResume) {
        Watch watch = new Watch();
        watch.start();
        listeners.beforeStart(isResume);
        for (RegistrationInfoImpl ri : ris) {
            watch.start(ri.getName().getName());
            ri.start();
            watch.stop(ri.getName().getName());
        }
        this.started = ris;
        listeners.afterStart(isResume);
        watch.stop();

        infoLog.info("Components started in " + watch.total.formatSeconds() + " sec.");
        writeDevMetrics(watch, "start");
    }

    /**
     * Stop all started components. Stopping components is done in reverse start order.
     *
     * @since 9.2
     */
    protected void stopComponents(boolean isStandby) {
        try {
            doStopComppnents(isStandby);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while stopping components", e);
        }
    }

    private void doStopComppnents(boolean isStandby) throws InterruptedException {
        Watch watch = new Watch();
        watch.start();
        listeners.beforeStop(isStandby);
        List<RegistrationInfoImpl> list = this.started;
        for (int i = list.size() - 1; i >= 0; i--) {
            RegistrationInfoImpl ri = list.get(i);
            if (ri.isStarted()) {
                watch.start(ri.getName().getName());
                ri.stop();
                watch.stop(ri.getName().getName());
            }
        }
        listeners.afterStop(isStandby);
        watch.stop();

        writeDevMetrics(watch, "stop");
    }

    @Override
    public synchronized boolean start() {
        if (this.started != null) {
            return false;
        }

        infoLog.info("Starting Nuxeo Components");

        List<RegistrationInfoImpl> ris = activateComponents();

        // TODO we sort using the old start order sorter (see OSGiRuntimeService.RIApplicationStartedComparator)
        Collections.sort(ris, new RIApplicationStartedComparator());

        // then start activated components
        startComponents(ris, false);

        return true;
    }

    @Override
    public synchronized boolean stop() {
        if (this.started == null) {
            return false;
        }

        infoLog.info("Stopping Nuxeo Components");

        try {
            stopComponents(false);
            // now deactivate all active components
            deactivateComponents();
        } finally {
            this.started = null;
        }

        return true;
    }

    @Override
    public void stop(int timeoutInSeconds) {
        try {
            runWihtinTimeout(timeoutInSeconds, TimeUnit.SECONDS, "Timed out on stop, blocking", this::stop);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while stopping components", e);
        }
    }

    @Override
    public synchronized void standby() {
        if (this.started != null) {
            try {
                stopComponents(true);
            } finally {
                this.standby = this.started;
                this.started = null;
            }
        }
    }

    @Override
    public void standby(int timeoutInSeconds) {
        try {
            runWihtinTimeout(timeoutInSeconds, TimeUnit.SECONDS, "Timed out on standby, blocking", this::standby);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while standbying components", e);
        }
    }

    @Override
    public synchronized void resume() {
        if (this.standby != null) {
            try {
                startComponents(this.standby, true);
            } finally {
                this.started = this.standby;
                this.standby = null;
            }
        }
    }

    @Override
    public boolean isStarted() {
        return this.started != null;
    }

    @Override
    public boolean isStandby() {
        return this.standby != null;
    }

    @Override
    public boolean isRunning() {
        return this.started != null || this.standby != null;
    }

    @Override
    public boolean hasSnapshot() {
        return this.snapshot != null;
    }

    @Override
    public boolean hasChanged() {
        return this.changed;
    }

    @Override
    public synchronized void snapshot() {
        this.snapshot = new ComponentRegistry(registry);
    }

    @Override
    public boolean isStashEmpty() {
        return stash.isEmpty();
    }

    @Override
    public synchronized void restart(boolean reset) {
        if (reset) {
            this.reset();
        } else {
            this.stop();
        }
        this.start();
    }

    @Override
    public synchronized boolean reset() {
        boolean r = this.stop();
        restoreSnapshot();
        return r;
    }

    @Override
    public synchronized boolean refresh() {
        return refresh(false);
    }

    @Override
    public synchronized boolean refresh(boolean reset) {
        if (this.stash.isEmpty()) {
            return false;
        }
        boolean requireStart;
        if (reset) {
            requireStart = reset();
        } else {
            requireStart = stop();
        }
        Stash currentStash = this.stash;
        this.stash = new Stash();
        applyStash(currentStash);
        if (requireStart) {
            start();
        }
        return true;
    }

    protected synchronized void restoreSnapshot() {
        if (changed && snapshot != null) {
            log.info("Restoring components snapshot");
            this.registry = new ComponentRegistry(snapshot);
            changed = false;
        }
    }

    /**
     * Tests whether new registrations should be stashed at registration time. If the component manager was started then
     * new components should be stashed otherwise they can be registered. When in standby mode components are not
     * stashed.
     * <p />
     * TODO: current implementation is stashing after the start completion. Should we also stashing while start is in
     * progress?
     */
    protected boolean shouldStash() {
        return this.started != null && !isFlushingStash;
    }

    protected synchronized void applyStash(Stash stash) {
        log.info("Applying stashed components");
        isFlushingStash = true;
        try {
            for (ComponentName name : stash.toRemove) {
                unregister(name);
            }
            for (RegistrationInfoImpl ri : stash.toAdd) {
                register(ri);
            }
        } finally {
            isFlushingStash = false;
        }
    }

    @Override
    public synchronized void unstash() {
        Stash currentStash = this.stash;
        this.stash = new Stash();

        if (!isRunning()) {
            applyStash(currentStash);
        } else {
            try {
                applyStashWhenRunning(currentStash);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while unstashing components", e);
            }
        }
    }

    private void applyStashWhenRunning(Stash stash) throws InterruptedException {
        List<RegistrationInfoImpl> toRemove = stash.getRegistrationsToRemove(registry);
        if (isStarted()) {
            for (RegistrationInfoImpl ri : toRemove) {
                this.started.remove(ri);
                ri.stop();
            }
        }
        for (RegistrationInfoImpl ri : toRemove) {
            if (isStandby()) {
                this.standby.remove(ri);
            }
            ri.deactivate();
        }

        applyStash(stash);

        // activate the new components
        for (RegistrationInfoImpl ri : stash.toAdd) {
            if (ri.isResolved()) {
                ri.activate();
            }
        }
        if (isStandby()) {
            // activate the new components
            for (RegistrationInfoImpl ri : stash.toAdd) {
                if (ri.isResolved()) {
                    ri.activate();
                    // add new components to standby list
                    this.standby.add(ri);
                }
            }
        } else if (isStarted()) {
            // start the new components and add them to the started list
            for (RegistrationInfoImpl ri : stash.toAdd) {
                if (ri.isResolved()) {
                    ri.activate();
                }
            }
            for (RegistrationInfoImpl ri : stash.toAdd) {
                if (ri.isActivated()) {
                    ri.start();
                    this.started.add(ri);
                }
            }
        }
    }

    /**
     * TODO we use for now the same sorter as OSGIRuntimeService - should be improved later.
     */
    protected static class RIApplicationStartedComparator implements Comparator<RegistrationInfo> {

        @Override
        public int compare(RegistrationInfo r1, RegistrationInfo r2) {
            int cmp = Integer.compare(r1.getApplicationStartedOrder(), r2.getApplicationStartedOrder());
            if (cmp == 0) {
                // fallback on name order, to be deterministic
                cmp = r1.getName().getName().compareTo(r2.getName().getName());
            }
            return cmp;
        }

    }

    protected void writeDevMetrics(Watch watch, String type) {
        if (!Framework.isDevModeSet()) {
            return;
        }
        File file = new File(Environment.getDefault().getTemp(), type + "-metrics.txt");
        try (PrintStream ps = new PrintStream(new FileOutputStream(file), false, "UTF-8")) {
            ps.println(watch.getTotal());
            // print first the longest intervals
            Arrays.stream(watch.getIntervals()).sorted(Comparator.reverseOrder()).forEach(ps::println);
            ps.flush();
        } catch (IOException e) {
            log.error("Failed to write metrics file: " + file, e);
        }
    }

    /**
     * Log a warning message if the timeout is reached while executing the given runnable.
     */
    protected static void runWihtinTimeout(long timeout, TimeUnit unit, String warn, Runnable runnable)
            throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(runnable::run);
            executor.shutdown();
            try {
                try {
                    future.get(timeout, unit);
                } catch (TimeoutException cause) {
                    log.warn(warn);
                    future.get();
                }
            } catch (ExecutionException cause) {
                throw new RuntimeException("Errors caught while stopping components, giving up", cause);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    protected class Listeners {

        protected ListenerList listeners = new ListenerList();

        public void add(ComponentManager.Listener listener) {
            listeners.add(listener);
        }

        public void remove(ComponentManager.Listener listener) {
            listeners.remove(listener);
        }

        public void beforeActivation() {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).beforeActivation(ComponentManagerImpl.this);
            }
        }

        public void afterActivation() {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).afterActivation(ComponentManagerImpl.this);
            }
        }

        public void beforeDeactivation() {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).beforeDeactivation(ComponentManagerImpl.this);
            }
        }

        public void afterDeactivation() {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).afterDeactivation(ComponentManagerImpl.this);
            }
        }

        public void beforeStart(boolean isResume) {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).beforeStart(ComponentManagerImpl.this, isResume);
            }
        }

        public void afterStart(boolean isResume) {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).afterStart(ComponentManagerImpl.this, isResume);
            }
        }

        public void beforeStop(boolean isStandby) {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).beforeStop(ComponentManagerImpl.this, isStandby);
            }
        }

        public void afterStop(boolean isStandby) {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).afterStop(ComponentManagerImpl.this, isStandby);
            }
        }

    }

    protected static class Stash {

        protected volatile List<RegistrationInfoImpl> toAdd;

        protected volatile Set<ComponentName> toRemove;

        public Stash() {
            toAdd = new ArrayList<>();
            toRemove = new HashSet<>();
        }

        public void add(RegistrationInfoImpl ri) {
            this.toAdd.add(ri);
        }

        public void remove(ComponentName name) {
            this.toRemove.add(name);
        }

        public boolean isEmpty() {
            return toAdd.isEmpty() && toRemove.isEmpty();
        }

        public List<RegistrationInfoImpl> getRegistrationsToRemove(ComponentRegistry reg) {
            ArrayList<RegistrationInfoImpl> ris = new ArrayList<>();
            for (ComponentName name : toRemove) {
                RegistrationInfoImpl ri = reg.getComponent(name);
                if (ri != null) {
                    ris.add(ri);
                }
            }
            return ris;
        }

    }

}
