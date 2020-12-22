/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.model.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.XMapException;
import org.nuxeo.common.xmap.registry.NullRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DescriptorRegistry;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.util.Watch;

/**
 * Component handling all Runtime components.
 */
public class ComponentManagerImpl implements ComponentManager {

    private static final Logger log = LogManager.getLogger(ComponentManagerImpl.class);

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

    private final ConcurrentMap<String, RegistrationInfo> services;

    protected volatile Set<String> blacklist;

    /**
     * The list of started components (sorted according to the start order). This list is null if the components were
     * not yet started or were stopped
     *
     * @since 9.2
     */
    protected volatile List<RegistrationInfo> started;

    /**
     * The list of standby components (sorted according to the start order) This list is null if component were not yet
     * started or not yet put in standby When putting components in standby all started components are stopped and the
     * {@link #started} list is assigned to {@link #standby} list then the {@link #started} field is nullified. When
     * resuming standby components the started list is restored from the standby list and the standby field is nullified
     *
     * @since 9.2
     */
    protected volatile List<RegistrationInfo> standby;

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
     * @since 10.3
     */
    protected volatile DescriptorRegistry descriptors;

    /**
     * Target Component &gt; Extension Point &gt; Registry
     *
     * @since 11.5
     */
    protected final Map<String, Map<String, Registry>> registries;

    protected static final Registry NULL_REGISTRY = new NullRegistry();

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
        descriptors = new DescriptorRegistry();
        registries = new HashMap<>();
    }

    /**
     * @since 10.3
     */
    public DescriptorRegistry getDescriptors() {
        return descriptors;
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
    public synchronized void register(RegistrationInfo ri) {
        ComponentName name = ri.getName();
        String stringName = name.getName();
        if (blacklist.contains(stringName)) {
            log.debug("Component {} was blacklisted. Ignoring.", stringName);
            return;
        }

        // Look if the component is not going to be removed when applying the stash,
        // before checking for duplicates.
        if (!stash.isRemoving(name)) {
            if (registry.contains(name) || stash.isAdding(name)) {
                handleError("Duplicate component name: " + name, stringName, null);
                return;
            }
            for (ComponentName n : ri.getAliases()) {
                if (registry.contains(n)) {
                    handleError("Duplicate component name: " + n + " (alias for " + name + ")", stringName, null);
                    return;
                }
            }
        }

        if (shouldStash()) {
            // stash before calling ri.attach.
            stash.add(ri);
            return;
        }

        if (hasSnapshot()) {
            // we are modifying the registry after the snapshot was created
            changed = true;
        }

        if (ri.useFormerLifecycleManagement()) {
            ((RegistrationInfoImpl) ri).attach(this);
        }

        try {
            log.debug("Registering component: {}", name);
            if (!registry.addComponent(ri)) {
                log.info("Registration delayed for component: " + name + ". Waiting for: "
                        + registry.getMissingDependencies(name));
            }
        } catch (RuntimeException e) {
            // don't raise this exception,
            // we want to isolate component errors from other components
            handleError("Failed to register component: " + name + " (" + e.toString() + ')', stringName, e);
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
            log.debug("Unregistering component: {}", name);
            registry.removeComponent(name);
        } catch (RuntimeException e) {
            log.error("Failed to unregister component: {}", name, e);
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
        RegistrationInfo ri = services.get(serviceClass.getName());
        if (ri == null) {
            return null;
        }
        ComponentInstance ci = ri.getComponent();
        if (ci == null) {
            log.debug("The component exposing the service {} is not resolved or not started", serviceClass);
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
        log.trace("Dispatching event: {}", event);
        Object[] listeners = this.compListeners.getListeners();
        for (Object listener : listeners) {
            ((ComponentListener) listener).handleEvent(event);
        }
    }

    public synchronized void registerExtension(Extension extension) {
        ComponentName name = extension.getTargetComponent();
        RegistrationInfo ri = registry.getComponent(name);

        if (ri != null && ri.getComponent() != null
                && Set.of(RegistrationInfo.ACTIVATED, RegistrationInfo.STARTED).contains(ri.getState())) {
            log.debug("Register contributed extension: {}", extension);
            register(ri, extension);
            ri.getComponent().registerExtension(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_REGISTERED,
                    ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
        } else {
            // put the extension in the pending queue
            log.debug("Enqueue contributed extension to pending queue: {}", extension);
            // must keep order in which extensions are contributed
            pendingExtensions.computeIfAbsent(name, key -> new LinkedHashSet<>()).add(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_PENDING,
                    ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
        }
    }

    public synchronized void unregisterExtension(Extension extension) {
        // TODO check if framework is shutting down and in that case do nothing
        log.debug("Unregister contributed extension: {}", extension);
        ComponentName name = extension.getTargetComponent();
        RegistrationInfo ri = registry.getComponent(name);
        if (ri != null) {
            unregister(ri, extension);
            ComponentInstance co = ri.getComponent();
            if (co != null) {
                co.unregisterExtension(extension);
            }
        } else { // maybe it's pending
            Set<Extension> extensions = pendingExtensions.get(name);
            if (extensions != null) {
                extensions.remove(extension);
                if (extensions.isEmpty()) {
                    pendingExtensions.remove(name);
                }
            }
        }
        sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_UNREGISTERED,
                ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
    }

    /**
     * Registers the given extension on target extension point held by given registration info.
     *
     * @since 11.5
     */
    public void register(RegistrationInfo ri, Extension xt) {
        // in new java based system contributions don't need to be loaded, this is a XML specificity reflected by
        // ExtensionPointImpl coming from XML deserialization
        if (ri.useFormerLifecycleManagement()) {
            // Extension point needing to load contribution are ExtensionPointImpl
            ri.getExtensionPoint(xt.getExtensionPoint()).filter(xp -> xp.getContributions() != null).ifPresent(xp -> {
                try {
                    // should compute now the contributions
                    Class<?>[] contributions = xp.getContributions();
                    if (contributions != null) {
                        Context xctx = new XMapContext(xt.getContext());
                        Registry registry = getOrCreateRegistry(ri.getName().getName(), xp);
                        String tag = xt.getId();
                        if (registry.isNull()) {
                            // backward compatibility
                            if (xt.getContributions() == null) {
                                // overload use case: loaded contributions should use the old descriptor so should not
                                // be recomputed
                                xt.setContributions(xp.getXMap().loadAll(xctx, xt.getElement()));
                            }
                        } else if (!registry.isTagged(tag)) {
                            registry.tag(tag);
                            // fill registry
                            xp.getXMap().register(registry, xctx, xt.getElement(), tag);
                        }
                    }
                } catch (XMapException e) {
                    ComponentName compName = xt.getComponent().getName();
                    handleError(e.getMessage() + " while processing component: " + compName, compName.getName(), e);
                }
            });
        }

    }

    protected void unregister(RegistrationInfo ri, Extension xt) {
        if (ri.useFormerLifecycleManagement()) {
            ri.getExtensionPoint(xt.getExtensionPoint()).filter(xp -> xp.getContributions() != null).ifPresent(xp -> {
                try {
                    Optional<Registry> registry = getExtensionPointRegistry(ri.getName().getName(), xp.getName());
                    if (registry.isPresent() && !registry.get().isNull()) {
                        try {
                            xp.getXMap().unregister(registry.get(), xt.getId());
                        } catch (XMapException e) {
                            log.error(e.getMessage() + " while unprocessing component: "
                                    + xt.getComponent().getName().getName(), e);
                        }
                    }
                } catch (RuntimeException e) {
                    ComponentName compName = xt.getComponent().getName();
                    handleError("Failed to unregister contributions for component " + compName, compName.getName(), e);
                }
            });
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Registry> Optional<T> getExtensionPointRegistry(String component, String point) {
        Map<String, Registry> target = registries.get(component);
        if (target != null && target.containsKey(point)) {
            Registry registry = target.get(point);
            if (!registry.isNull()) {
                return Optional.of((T) registry);
            }
        }
        return Optional.empty();
    }

    protected Registry getOrCreateRegistry(String component, ExtensionPoint xp) {
        Optional<Registry> stored = getExtensionPointRegistry(component, xp.getName());
        if (stored.isPresent()) {
            return stored.get();
        }
        Registry registry = NULL_REGISTRY;
        String point = xp.getName();
        String registryClass = xp.getRegistryClass();
        if (registryClass != null) {
            try {
                Class<?> clazz = Class.forName(registryClass);
                Constructor<?> constructor = clazz.getConstructor();
                registry = (Registry) constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                String msg = String.format(
                        "Failed to create registry on component '%s', extension point '%s': error initializing class '%s' (%s).",
                        component, point, registryClass, e.toString());
                throw new RuntimeException(msg, e);
            }
        } else {
            Class<?>[] contributions = xp.getContributions();
            if (contributions.length != 0) {
                // compute registry from annotations, taking first registry
                XMap xmap = xp.getXMap();
                registry = Arrays.stream(contributions)
                                 .map(xmap::getObject)
                                 .filter(Objects::nonNull)
                                 .map(xmap::getRegistry)
                                 .filter(Objects::nonNull)
                                 .findFirst()
                                 .orElse(NULL_REGISTRY);
            }
        }
        registries.computeIfAbsent(component, k -> new ConcurrentHashMap<>()).put(point, registry);
        return registry;
    }

    protected void createRegistries(RegistrationInfo ri) {
        // instantiate extension point registries if any
        Stream.of(ri.getExtensionPoints()).filter(xp -> xp.getContributions() != null).forEach(xp -> {
            getOrCreateRegistry(ri.getName().getName(), xp);
        });
    }

    protected void initializeRegistries(RegistrationInfo ri) {
        String name = ri.getName().getName();
        if (registries.containsKey(name)) {
            registries.get(name).values().forEach(Registry::initialize);
        }
    }

    protected void resetRegistries(RegistrationInfo ri) {
        registries.remove(ri.getName().getName());
    }

    public synchronized void registerServices(RegistrationInfo ri) {
        String[] serviceNames = ri.getProvidedServiceNames();
        if (serviceNames == null) {
            return;
        }
        for (String serviceName : serviceNames) {
            log.trace("Registering service: {}", serviceName);
            services.put(serviceName, ri);
        }
    }

    public synchronized void unregisterServices(RegistrationInfo ri) {
        String[] serviceNames = ri.getProvidedServiceNames();
        if (serviceNames == null) {
            return;
        }
        for (String service : serviceNames) {
            services.remove(service);
        }
    }

    @Override
    public String[] getServices() {
        return services.keySet().toArray(new String[0]);
    }

    protected static void handleError(String message, String componentName, Exception e) {
        log.error(message, e);
        Framework.getRuntime()
                 .getMessageHandler()
                 .addMessage(new RuntimeMessage(Level.ERROR, message, Source.COMPONENT, componentName));
    }

    /**
     * Activate all the resolved components and return the list of activated components in the activation order
     *
     * @return the list of the activated components in the activation order
     * @since 9.2
     */
    protected List<RegistrationInfo> activateComponents() {
        log.info("Instantiate components");
        Watch iwatch = new Watch();
        iwatch.start();

        // first instantiate resolved components: that allows some to register as listeners on ComponentManager before
        // all components activation.
        List<RegistrationInfo> iris = new ArrayList<>();
        // first activate resolved components
        for (RegistrationInfo ri : registry.getResolvedRegistrationInfo()) {
            iwatch.start(ri.getName().getName());
            if (instantiateComponent(ri)) {
                iris.add(ri);
                // register potential listener
                ComponentInstance component = ri.getComponent();
                if (component != null && component.getInstance() instanceof Listener) {
                    addListener((Listener) component.getInstance());
                }
            }
            iwatch.stop(ri.getName().getName());
        }
        log.debug("Components instantiated in {}s", iwatch.total::formatSeconds);
        writeDevMetrics(iwatch, "instantiate");

        log.info("Activate components");
        Watch awatch = new Watch();
        awatch.start();
        listeners.beforeActivation();
        // make sure we start with a clean pending registry
        pendingExtensions.clear();
        List<RegistrationInfo> ris = new ArrayList<>();
        // first activate resolved components
        for (RegistrationInfo ri : iris) {
            awatch.start(ri.getName().getName());
            activateComponent(ri);
            ris.add(ri);
            awatch.stop(ri.getName().getName());
        }
        listeners.afterActivation();
        awatch.stop();

        log.debug("Components activated in {}s", awatch.total::formatSeconds);
        writeDevMetrics(awatch, "activate");

        return ris;
    }

    /**
     * Instantiates the given {@link RegistrationInfo}. This step will instantiate the component.
     * <p>
     * Allows registering listeners on ComponentManager at component instantiation, before all components activation.
     * <p>
     * Should be called before {@link #activateComponent(RegistrationInfo)}.
     *
     * @return false in case of error during instantiation, true otherwise.
     * @since 11.3.
     */
    protected boolean instantiateComponent(RegistrationInfo ri) {
        return ((RegistrationInfoImpl) ri).instantiate();
    }

    /**
     * Activates the given {@link RegistrationInfo}. This step will activate the component, register extensions and then
     * register services.
     *
     * @since 9.3
     */
    protected void activateComponent(RegistrationInfo ri) {
        createRegistries(ri);

        if (ri.useFormerLifecycleManagement()) {
            ((RegistrationInfoImpl) ri).activate();
            return;
        }
        if (ri.getState() != RegistrationInfo.RESOLVED) {
            return;
        }
        ri.setState(RegistrationInfo.ACTIVATING);

        if (!instantiateComponent(ri)) {
            return;
        }
        ComponentInstance component = ri.getComponent();
        component.activate();
        log.debug("Component activated: {}", ri.getName());

        // register contributed extensions if any
        Extension[] extensions = ri.getExtensions();
        if (extensions != null) {
            for (Extension xt : extensions) {
                xt.setComponent(component);
                try {
                    registerExtension(xt);
                } catch (RuntimeException e) {
                    String msg = "Failed to register extension to: " + xt.getTargetComponent() + ", xpoint: "
                            + xt.getExtensionPoint() + " in component: " + xt.getComponent().getName();
                    log.error(msg, e);
                    msg += " (" + e.toString() + ')';
                    Framework.getRuntime()
                             .getMessageHandler()
                             .addMessage(new RuntimeMessage(Level.ERROR, msg, Source.COMPONENT,
                                     xt.getComponent().getName().getName()));
                }
            }
        }

        // register pending extensions if any
        Set<ComponentName> aliases = ri.getAliases();
        List<ComponentName> names = new ArrayList<>(1 + aliases.size());
        names.add(ri.getName());
        names.addAll(aliases);
        for (ComponentName n : names) {
            Set<Extension> pendingExt = pendingExtensions.remove(n);
            if (pendingExt == null) {
                continue;
            }
            for (Extension xt : pendingExt) {
                try {
                    component.registerExtension(xt);
                    sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_REGISTERED,
                            ((ComponentInstanceImpl) xt.getComponent()).ri, xt));
                } catch (RuntimeException e) {
                    ComponentName compName = xt.getComponent().getName();
                    String msg = "Failed to register extension to: " + xt.getTargetComponent() + ", xpoint: "
                            + xt.getExtensionPoint() + " in component: " + compName;
                    log.error(msg, e);
                    msg += " (" + e.toString() + ')';
                    Framework.getRuntime()
                             .getMessageHandler()
                             .addMessage(new RuntimeMessage(Level.ERROR, msg, Source.EXTENSION, compName.getName()));
                }
            }
        }

        registerServices(ri);

        ri.setState(RegistrationInfo.ACTIVATED);
    }

    /**
     * Deactivate all active components in the reverse resolve order
     *
     * @since 9.2
     */
    protected void deactivateComponents(boolean isShutdown) {
        log.info("Deactivate components");
        Watch watch = new Watch();
        watch.start();
        listeners.beforeDeactivation();
        Collection<RegistrationInfo> resolved = registry.getResolvedRegistrationInfo();
        List<RegistrationInfo> reverseResolved = new ArrayList<>(resolved);
        Collections.reverse(reverseResolved);
        for (RegistrationInfo ri : reverseResolved) {
            if (ri.isActivated()) {
                watch.start(ri.getName().getName());
                deactivateComponent(ri, isShutdown);
                watch.stop(ri.getName().getName());
            }
        }
        // make sure the pending extension map is empty since we didn't unregistered extensions by calling
        // ri.deactivate(true)
        pendingExtensions.clear();
        listeners.afterDeactivation();
        watch.stop();

        log.debug("Components deactivated in {}s", watch.total::formatSeconds);
        writeDevMetrics(watch, "deactivate");
    }

    /**
     * Deactivates the given {@link RegistrationInfo}. This step will unregister the services, unregister the extensions
     * and then deactivate the component.
     *
     * @since 9.3
     */
    protected void deactivateComponent(RegistrationInfo ri, boolean isShutdown) {
        // reset registries (even when shutting down, to comply with HotDeployer logic in tests)
        resetRegistries(ri);
        // unregister potential listener
        ComponentInstance component = ri.getComponent();
        if (component.getInstance() instanceof Listener) {
            removeListener((Listener) component.getInstance());
        }

        if (ri.useFormerLifecycleManagement()) {
            // don't unregister extension if server is shutdown
            ((RegistrationInfoImpl) ri).deactivate(!isShutdown);
            return;
        }
        int state = ri.getState();
        if (state != RegistrationInfo.ACTIVATED && state != RegistrationInfo.START_FAILURE) {
            return;
        }
        ri.setState(RegistrationInfo.DEACTIVATING);

        unregisterServices(ri);

        // unregister contributed extensions if any
        Extension[] extensions = ri.getExtensions();
        if (extensions != null) {
            for (Extension xt : extensions) {
                try {
                    unregisterExtension(xt);
                } catch (RuntimeException e) {
                    String msg = "Failed to unregister extension. Contributor: " + xt.getComponent() + " to "
                            + xt.getTargetComponent() + "; xpoint: " + xt.getExtensionPoint();
                    log.error(msg, e);
                    Framework.getRuntime()
                             .getMessageHandler()
                             .addMessage(new RuntimeMessage(Level.ERROR, msg, Source.COMPONENT,
                                     xt.getComponent().getName().getName()));
                }
            }
        }

        component.deactivate();
        log.debug("Component deactivated: {}", ri.getName());
        ri.setState(RegistrationInfo.RESOLVED);
    }

    /**
     * Starts all given components.
     *
     * @since 9.2
     */
    protected void startComponents(List<RegistrationInfo> ris, boolean isResume) {
        log.info("Start components (isResume={})", isResume);
        Watch watch = new Watch();
        watch.start();
        listeners.beforeStart(isResume);
        for (RegistrationInfo ri : ris) {
            watch.start(ri.getName().getName());
            startComponent(ri);
            watch.stop(ri.getName().getName());
        }
        this.started = ris;
        listeners.afterStart(isResume);
        watch.stop();

        log.debug("Components started in {}s", watch.total::formatSeconds);
        writeDevMetrics(watch, "start");
    }

    /**
     * Starts the given {@link RegistrationInfo}. This step will start the component.
     *
     * @since 9.3
     */
    protected void startComponent(RegistrationInfo ri) {
        // make sure corresponding registries are initialized
        initializeRegistries(ri);
        if (ri.useFormerLifecycleManagement()) {
            ((RegistrationInfoImpl) ri).start();
            return;
        }
        if (ri.getState() != RegistrationInfo.ACTIVATED) {
            return;
        }
        try {
            ri.setState(RegistrationInfo.STARTING);
            ComponentInstance component = ri.getComponent();
            component.start();
            log.debug("Component started: {}", ri.getName());
            ri.setState(RegistrationInfo.STARTED);
        } catch (RuntimeException e) {
            log.error("Component {} notification of application started failed: {}", ri.getName(), e.getMessage(), e);
            ri.setState(RegistrationInfo.START_FAILURE);
        }
    }

    /**
     * Stop all started components. Stopping components is done in reverse start order.
     *
     * @since 9.2
     */
    protected void stopComponents(boolean isStandby) {
        log.info("Stop components (isStandby={})", isStandby);
        try {
            Watch watch = new Watch();
            watch.start();
            listeners.beforeStop(isStandby);
            List<RegistrationInfo> list = this.started;
            for (int i = list.size() - 1; i >= 0; i--) {
                RegistrationInfo ri = list.get(i);
                if (ri.isStarted()) {
                    watch.start(ri.getName().getName());
                    stopComponent(ri);
                    watch.stop(ri.getName().getName());
                }
            }
            listeners.afterStop(isStandby);
            watch.stop();

            log.debug("Components stopped in {}s", watch.total::formatSeconds);
            writeDevMetrics(watch, "stop");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while stopping components", e);
        }
    }

    /**
     * Stops the given {@link RegistrationInfo}. This step will stop the component.
     *
     * @since 9.3
     */
    protected void stopComponent(RegistrationInfo ri) throws InterruptedException {
        if (ri.useFormerLifecycleManagement()) {
            ((RegistrationInfoImpl) ri).stop();
            return;
        }
        if (ri.getState() != RegistrationInfo.STARTED) {
            return;
        }
        ri.setState(RegistrationInfo.STOPPING);
        ComponentInstance component = ri.getComponent();
        component.stop();
        log.debug("Component stopped: {}", ri.getName());
        ri.setState(RegistrationInfo.RESOLVED);
    }

    @Override
    public synchronized boolean start() {
        if (this.started != null) {
            return false;
        }

        log.info("Starting Nuxeo Components");

        List<RegistrationInfo> ris = activateComponents();

        // TODO we sort using the old start order sorter (see OSGiRuntimeService.RIApplicationStartedComparator)
        ris.sort(new RIApplicationStartedComparator());

        // then start activated components
        startComponents(ris, false);

        return true;
    }

    @Override
    public synchronized boolean stop() {
        if (this.started == null) {
            return false;
        }

        log.info("Stopping Nuxeo Components");

        try {
            stopComponents(false);
            // now deactivate all active components
            deactivateComponents(true);
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
     * new components should be stashed otherwise they can be registered.
     * <p>
     * TODO: current implementation is stashing after the start completion. Should we also stashing while start is in
     * progress?
     */
    protected boolean shouldStash() {
        return isRunning() && !isFlushingStash;
    }

    protected synchronized void applyStash(Stash stash) {
        log.debug("Applying stashed components");
        isFlushingStash = true;
        try {
            for (ComponentName name : stash.toRemove) {
                unregister(name);
            }
            for (RegistrationInfo ri : stash.toAdd) {
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

    /**
     * Applies the given stash when Nuxeo is started or is standby.
     * <p>
     * Started state corresponds to activated & started components.
     * <p>
     * Standby state corresponds to activated components.
     */
    private void applyStashWhenRunning(Stash stash) throws InterruptedException {
        List<RegistrationInfo> toRemove = stash.getRegistrationsToRemove(registry);
        // Nuxeo is started so stop components to remove first and remove them from started list
        if (isStarted()) {
            for (RegistrationInfo ri : toRemove) {
                this.started.remove(ri);
                stopComponent(ri);
            }
        }
        // deactivate components to remove (and remove them from standby list if needed)
        for (RegistrationInfo ri : toRemove) {
            if (isStandby()) {
                this.standby.remove(ri);
            }
            deactivateComponent(ri, false);
        }

        applyStash(stash);

        // activate components to add (and add them to standby list if needed)
        for (RegistrationInfo ri : stash.toAdd) {
            if (ri.isResolved()) {
                if (!instantiateComponent(ri)) {
                    continue;
                }
                activateComponent(ri);
                if (isStandby()) {
                    // add new components to standby list in order to start them latter
                    this.standby.add(ri);
                }
            }
        }
        // Nuxeo is started so start components to add and add them to the started list
        if (isStarted()) {
            for (RegistrationInfo ri : stash.toAdd) {
                if (ri.isActivated()) {
                    startComponent(ri);
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
            log.error("Failed to write metrics file: {}", file, e);
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
                ((ComponentManager.Listener) listener).beforeRuntimeActivation(ComponentManagerImpl.this);
            }
        }

        public void afterActivation() {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).afterRuntimeActivation(ComponentManagerImpl.this);
            }
        }

        public void beforeDeactivation() {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).beforeRuntimeDeactivation(ComponentManagerImpl.this);
            }
        }

        public void afterDeactivation() {
            for (Object listener : listeners.getListeners()) {
                ((ComponentManager.Listener) listener).afterRuntimeDeactivation(ComponentManagerImpl.this);
            }
        }

        /**
         * Sort listeners to follow start order on components.
         *
         * @since 11.5
         */
        protected Stream<ComponentManager.Listener> sortedForStart() {
            Stream<ComponentManager.Listener> regularListeners = Arrays.stream(listeners.getListeners())
                                                                       .filter(l -> !(l instanceof Component))
                                                                       .map(ComponentManager.Listener.class::cast);
            Stream<ComponentManager.Listener> componentListeners = Arrays.stream(listeners.getListeners())
                                                                         .filter(Component.class::isInstance)
                                                                         .map(Component.class::cast)
                                                                         .sorted(Comparator.comparingInt(
                                                                                 Component::getApplicationStartedOrder))
                                                                         .map(ComponentManager.Listener.class::cast);
            return Stream.concat(regularListeners, componentListeners);
        }

        /**
         * Sort listeners to follow stop order on components (reversing start order).
         *
         * @since 11.5
         */
        protected Stream<ComponentManager.Listener> sortedForStop() {
            return sortedForStart().collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                Collections.reverse(list);
                return list.stream();
            }));
        }

        public void beforeStart(boolean isResume) {
            sortedForStart().forEach(listener -> listener.beforeRuntimeStart(ComponentManagerImpl.this, isResume));
        }

        public void afterStart(boolean isResume) {
            sortedForStart().forEach(listener -> listener.afterRuntimeStart(ComponentManagerImpl.this, isResume));
        }

        public void beforeStop(boolean isStandby) {
            sortedForStop().forEach(listener -> listener.beforeRuntimeStop(ComponentManagerImpl.this, isStandby));
        }

        public void afterStop(boolean isStandby) {
            sortedForStop().forEach(listener -> listener.afterRuntimeStop(ComponentManagerImpl.this, isStandby));
        }

    }

    protected static class Stash {

        protected volatile List<RegistrationInfo> toAdd;

        protected volatile Set<ComponentName> toRemove;

        public Stash() {
            toAdd = new ArrayList<>();
            toRemove = new HashSet<>();
        }

        public void add(RegistrationInfo ri) {
            this.toAdd.add(ri);
        }

        public void remove(ComponentName name) {
            this.toRemove.add(name);
        }

        public boolean isEmpty() {
            return toAdd.isEmpty() && toRemove.isEmpty();
        }

        public List<RegistrationInfo> getRegistrationsToRemove(ComponentRegistry reg) {
            List<RegistrationInfo> ris = new ArrayList<>();
            for (ComponentName name : toRemove) {
                RegistrationInfo ri = reg.getComponent(name);
                if (ri != null) {
                    ris.add(ri);
                }
            }
            return ris;
        }

        /**
         * @since 11.3
         */
        public boolean isAdding(ComponentName name) {
            return toAdd.stream().anyMatch(ri -> name.equals(ri.getName()));
        }

        /**
         * @since 11.3
         */
        public boolean isRemoving(ComponentName name) {
            return toRemove.contains(name);
        }

    }

}
