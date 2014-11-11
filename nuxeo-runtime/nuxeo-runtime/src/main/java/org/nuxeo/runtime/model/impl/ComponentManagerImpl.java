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

package org.nuxeo.runtime.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class ComponentManagerImpl implements ComponentManager {

    private static final Log log = LogFactory.getLog(ComponentManagerImpl.class);

    // must use an ordered Set to avoid loosing the order of the pending extensions
    protected final Map<ComponentName, Set<Extension>> pendingExtensions;

    private ListenerList listeners;

    private Map<ComponentName, RegistrationInfoImpl> registry;

    private Map<ComponentName, Set<RegistrationInfoImpl>> dependsOnMe;

    private final Map<String, RegistrationInfoImpl> services;

    protected Set<String> blacklist;


    public ComponentManagerImpl(RuntimeService runtime) {
        registry = new HashMap<ComponentName, RegistrationInfoImpl>();
        dependsOnMe = new HashMap<ComponentName, Set<RegistrationInfoImpl>>();
        pendingExtensions = new HashMap<ComponentName, Set<Extension>>();
        listeners = new ListenerList();
        services = new Hashtable<String, RegistrationInfoImpl>();
        blacklist = new HashSet<String>();
    }

    @Override
    public Collection<RegistrationInfo> getRegistrations() {
        return new ArrayList<RegistrationInfo>(registry.values());
    }

    @Override
    public Map<ComponentName, Set<ComponentName>> getPendingRegistrations() {
        Map<ComponentName, Set<ComponentName>> pending = new HashMap<ComponentName, Set<ComponentName>>();
        for (RegistrationInfo ri : registry.values()) {
            if (ri.getState() == RegistrationInfo.REGISTERED) {
                pending.put(ri.getName(), ri.getRequiredComponents());
            }
        }
        for (Entry<ComponentName, Set<RegistrationInfoImpl>> e : dependsOnMe.entrySet()) {
            for (RegistrationInfo ri : e.getValue()) {
                pending.put(ri.getName(), Collections.singleton(e.getKey()));
            }
        }
        for (Set<Extension> exts : pendingExtensions.values()) {
            for (Extension ext : exts) {
                pending.put(ext.getComponent().getName(),
                        Collections.singleton(ext.getTargetComponent()));
            }
        }
        return pending;
    }

    public Collection<ComponentName> getNeededRegistrations() {
        return pendingExtensions.keySet();
    }

    public Collection<Extension> getPendingExtensions(ComponentName name) {
        return pendingExtensions.get(name);
    }

    @Override
    public RegistrationInfo getRegistrationInfo(ComponentName name) {
        return registry.get(name);
    }

    @Override
    public synchronized boolean isRegistered(ComponentName name) {
        return registry.containsKey(name);
    }

    @Override
    public synchronized int size() {
        return registry.size();
    }

    @Override
    public ComponentInstance getComponent(ComponentName name) {
        RegistrationInfoImpl ri = registry.get(name);
        return ri != null ? ri.getComponent() : null;
    }

    @Override
    public synchronized void shutdown() {
        // unregister me -> this will unregister all objects that depends on me
        List<RegistrationInfo> elems = new ArrayList<RegistrationInfo>(
                registry.values());
        for (RegistrationInfo ri : elems) {
            try {
                unregister(ri);
            } catch (Exception e) {
                log.error("failed to shutdown component manager", e);
            }
        }
        try {
            listeners = null;
            registry.clear();
            registry = null;
            dependsOnMe.clear();
            dependsOnMe = null;
        } catch (Exception e) {
            log.error("Failed to shutdown registry manager");
        }
    }

    @Override
    public synchronized void register(RegistrationInfo regInfo) {
        _register((RegistrationInfoImpl) regInfo);
    }

    @Override
    public Set<String> getBlacklist() {
        return blacklist;
    }

    @Override
    public void setBlacklist(Set<String> blacklist) {
        this.blacklist = blacklist;
    }

    private void _register(RegistrationInfoImpl ri) {
        ComponentName name = ri.getName();
        if (blacklist.contains(name.getName())) {
            log.warn("Component "+name.getName()+" was blacklisted. Ignoring.");
            return;
        }
        if (isRegistered(name)) {
            if (name.getName().startsWith("org.nuxeo.runtime.")) {
                // XXX we hide the fact that nuxeo-runtime bundles are
                // registered twice
                // TODO fix the root cause and remove this
                return;
            }
            String msg = "Duplicate component name: " + name;
            log.error(msg);
            Framework.getRuntime().getWarnings().add(msg);
            return;
            //throw new IllegalStateException("Component was already registered: " + name);
        }

        ri.manager = this;

        try {
            ri.register();
        } catch (Exception e) {
            String msg = "Failed to register component: " + name;
            log.error(msg, e);
            msg += " (" + e.toString() + ')';
            Framework.getRuntime().getWarnings().add(msg);
            return;
        }

        // compute blocking dependencies
        boolean hasBlockingDeps = computeBlockingDependencies(ri);

        // check if blocking dependencies were found
        if (!hasBlockingDeps) {
            // check if there is any object waiting for me
            Set<RegistrationInfoImpl> pendings = removeDependencies(name);
            // update set the dependsOnMe member
            ri.dependsOnMe = pendings;

            // no blocking dependencies found - register it
            log.info("Registering component: " + name);
            // create the component
            try {
                registry.put(name, ri);
                ri.resolve();

                // if some objects are waiting for me notify them about my registration
                if (ri.dependsOnMe != null) {
                    // notify all components that deonds on me about my registration
                    for (RegistrationInfoImpl pending : ri.dependsOnMe) {
                        if (pending.waitsFor == null) {
                            _register(pending);
                        } else {
                            // remove object dependence on me
                            pending.waitsFor.remove(name);
                            // if object has no more dependencies register it
                            if (pending.waitsFor.isEmpty()) {
                                pending.waitsFor = null;
                                _register(pending);
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                registry.remove(name);
                String msg = "Failed to create component: " + name;
                log.error(msg, e);
                msg += " (" + e.toString() + ')';
                Framework.getRuntime().getWarnings().add(msg);
            }

        } else {
            log.info("Registration delayed for component: " + name
                    + ". Waiting for: " + ri.waitsFor);
        }
    }

    @Override
    public synchronized void unregister(RegistrationInfo regInfo) {
        _unregister((RegistrationInfoImpl) regInfo);
    }

    private void _unregister(RegistrationInfoImpl ri) {
        // remove me as a dependent on other objects
        if (ri.requires != null) {
            for (ComponentName dep : ri.requires) {
                RegistrationInfoImpl depRi = registry.get(dep);
                if (depRi != null) { // can be null if comp is unresolved and waiting for this dep.
                    if (depRi.dependsOnMe != null) {
                        depRi.dependsOnMe.remove(ri);
                    }
                }
            }
        }
        // unresolve also the dependent objects
        if (ri.dependsOnMe != null) {
            List<RegistrationInfoImpl> deps = new ArrayList<RegistrationInfoImpl>(
                    ri.dependsOnMe);
            for (RegistrationInfoImpl dep : deps) {
                try {
                    dep.unresolve();
                    // TODO ------------- keep waiting comp. in the registry -
                    // otherwise the unresolved comp will never be unregistered
                    // add a blocking dependence on me
                    if (dep.waitsFor == null) {
                        dep.waitsFor = new HashSet<ComponentName>();
                    }
                    dep.waitsFor.add(ri.name);
                    addDependency(ri.name, dep);
                    // remove from registry
                    registry.remove(dep);
                    // TODO -------------
                } catch (Exception e) {
                    log.error("Failed to unresolve component: " + dep.getName(), e);
                }
            }
        }

        log.info("Unregistering component: " + ri.name);

        try {
            if (registry.remove(ri.name) == null) {
                // may be a pending component
                //TODO -> put pending components in the registry
            }
            ri.unregister();
        } catch (Exception e) {
            log.error("Failed to unregister component: " + ri.getName(), e);
        }
    }

    @Override
    public synchronized void unregister(ComponentName name) {
        RegistrationInfoImpl ri = registry.get(name);
        if (ri != null) {
            _unregister(ri);
        }
    }

    @Override
    public void addComponentListener(ComponentListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeComponentListener(ComponentListener listener) {
        listeners.remove(listener);
    }

    @Override
    public ComponentInstance getComponentProvidingService(Class<?> serviceClass) {
        try {
            RegistrationInfoImpl ri = services.get(serviceClass.getName());
            if (ri != null) {
                if (!ri.isActivated()) {
                    if (ri.isResolved()) {
                        ri.activate(); // activate the component if not yet activated
                    } else {
                        // Hack to avoid messages during TypeService activation
                        if (!serviceClass.getSimpleName().equals("TypeProvider")) {
                            log.debug("The component exposing the service " +
                                    serviceClass + " is not resolved");
                        }
                        return null;
                    }
                }
                return ri.getComponent();
            }
        } catch (Exception e) {
            log.error("Failed to get service: " + serviceClass);
        }
        return null;
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        ComponentInstance comp = getComponentProvidingService(serviceClass);
        return comp != null ? comp.getAdapter(serviceClass) : null;
    }

    @Override
    public Collection<ComponentName> getActivatingRegistrations() {
        Collection<ComponentName> activating = new ArrayList<ComponentName>();
        for (RegistrationInfo ri : registry.values()) {
            if (ri.getState() == RegistrationInfo.ACTIVATING) {
                activating.add(ri.getName());
            }
        }
        return activating;
    }

    void sendEvent(ComponentEvent event) {
        log.debug("Dispatching event: " + event);
        Object[] listeners = this.listeners.getListeners();
        for (Object listener : listeners) {
            ((ComponentListener) listener).handleEvent(event);
        }
    }

    protected boolean computeBlockingDependencies(RegistrationInfoImpl ri) {
        if (ri.requires != null) {
            for (ComponentName dep : ri.requires) {
                RegistrationInfoImpl depRi = registry.get(dep);
                if (depRi == null) {
                    // dep is not yet registered - add it to the blocking deps queue
                    if (ri.waitsFor == null) {
                        ri.waitsFor = new HashSet<ComponentName>();
                    }
                    ri.waitsFor.add(dep);
                    addDependency(dep, ri);
                } else {
                    // we need this when unregistering depRi
                    // to be able to unregister dependent components
                    if (depRi.dependsOnMe == null) {
                        depRi.dependsOnMe = new HashSet<RegistrationInfoImpl>();
                    }
                    depRi.dependsOnMe.add(ri);
                }
            }
        }
        return ri.waitsFor != null;
    }

    protected synchronized void addDependency(ComponentName name,
            RegistrationInfoImpl dependent) {
        Set<RegistrationInfoImpl> pendings = dependsOnMe.get(name);
        if (pendings == null) {
            pendings = new HashSet<RegistrationInfoImpl>();
            dependsOnMe.put(name, pendings);
        }
        pendings.add(dependent);
    }

    protected synchronized Set<RegistrationInfoImpl> removeDependencies(
            ComponentName name) {
        return dependsOnMe.remove(name);
    }

    public void registerExtension(Extension extension) throws Exception {
        ComponentName name = extension.getTargetComponent();
        RegistrationInfoImpl ri = registry.get(name);
        if (ri != null) {
            if (log.isDebugEnabled()) {
                log.debug("Register contributed extension: " + extension);
            }
            loadContributions(ri, extension);
            ri.component.registerExtension(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_REGISTERED,
                    ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
        } else { // put the extension in the pending queue
            if (log.isDebugEnabled()) {
                log.debug("Enqueue contributed extension to pending queue: " + extension);
            }
            Set<Extension> extensions = pendingExtensions.get(name);
            if (extensions == null) {
                extensions = new LinkedHashSet<Extension>(); // must keep order in which extensions are contributed
                pendingExtensions.put(name, extensions);
            }
            extensions.add(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_PENDING,
                    ((ComponentInstanceImpl) extension.getComponent()).ri, extension));
        }
    }

    public void unregisterExtension(Extension extension) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Unregister contributed extension: " + extension);
        }
        ComponentName name = extension.getTargetComponent();
        RegistrationInfo ri = registry.get(name);
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
                ((ComponentInstanceImpl) extension.getComponent()).ri,
                extension));
    }

    public static void loadContributions(RegistrationInfoImpl ri, Extension xt) {
        ExtensionPointImpl xp = ri.getExtensionPoint(xt.getExtensionPoint());
        if (xp != null && xp.contributions != null) {
            try {
                Object[] contribs = xp.loadContributions(ri, xt);
                xt.setContributions(contribs);
            } catch (Exception e) {
                log.error("Failed to create contribution objects", e);
            }
        }
    }

    public void registerServices(RegistrationInfoImpl ri) {
        if (ri.serviceDescriptor == null) {
            return;
        }
        for (String service : ri.serviceDescriptor.services) {
            log.info("Registering service: " + service);
            services.put(service, ri);
            // TODO: send notifications
        }
    }

    public void unregisterServices(RegistrationInfoImpl ri) {
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

}
