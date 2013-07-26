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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.model.RuntimeModelException;
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

    // must use an ordered Set to avoid loosing the order of the pending
    // extensions
    protected final Map<ComponentName, Set<Extension>> extensionPendingsByComponent;

    private ListenerList listeners;

    private final Map<String, RegistrationInfoImpl> services;

    protected Set<String> blacklist;

    protected ComponentRegistry reg;

    public ComponentManagerImpl(RuntimeService runtime) {
        reg = new ComponentRegistry(this);
        extensionPendingsByComponent = new HashMap<ComponentName, Set<Extension>>();
        listeners = new ListenerList();
        services = new ConcurrentHashMap<String, RegistrationInfoImpl>();
        blacklist = new HashSet<String>();
    }

    @Override
    public synchronized Collection<RegistrationInfo> getRegistrations() {
        return new ArrayList<RegistrationInfo>(reg.getComponents());
    }

    @Override
    public synchronized Map<ComponentName, Set<RegistrationInfo>> getPendingRegistrations() {
        return reg.requiredPendings.map;
    }



    @Override
    public synchronized RegistrationInfoImpl getRegistrationInfo(
            ComponentName name) {
        return reg.getComponent(name);
    }

    @Override
    public synchronized boolean isRegistered(ComponentName name) {
        return reg.contains(name);
    }

    @Override
    public synchronized int size() {
        return reg.size();
    }

    @Override
    public synchronized ComponentInstanceImpl getComponent(ComponentName name) {
        RegistrationInfoImpl ri = reg.getComponent(name);
        return ri != null ? ri.getComponent() : null;
    }

    @Override
    public synchronized void shutdown() {
        ShutdownTask.shutdown(this);
        try {
            listeners = null;
            reg.destroy();
            reg = null;
        } catch (Exception e) {
            log.error("Failed to shutdown registry manager");
        }
    }

    @Override
    public Set<String> getBlacklist() {
        return blacklist;
    }

    @Override
    public void setBlacklist(Set<String> blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    public synchronized void register(RegistrationInfo regInfo) throws RuntimeModelException {
        RegistrationInfoImpl ri = (RegistrationInfoImpl) regInfo;
        ComponentName name = ri.getName();
        if (blacklist.contains(name.getName())) {
            log.warn("Component " + name.getName()
                    + " was blacklisted. Ignoring.");
            return;
        }
        if (reg.contains(name)) {
            throw new RuntimeModelException(this + " : Duplicate component name " + name);
        }
        for (ComponentName n : ri.getAliases()) {
            if (reg.contains(n)) {
                throw new RuntimeModelException(this + " : Duplicate component name " +  n + " (alias for "
                        + name + ")");
            }
        }

        ri.attach(this);

        log.info("Registering component: " + name);
        if (!reg.addComponent(ri)) {
            log.info("Registration delayed for component: " + name
                    + ". Waiting for: "
                    + reg.getMissingDependencies(ri.getName()));
        }
    }

    @Override
    public synchronized void unregister(RegistrationInfo regInfo) {
        unregister(regInfo.getName());
    }

    @Override
    public synchronized void unregister(ComponentName name) {
        try {
            log.info("Unregistering component: " + name);
            reg.removeComponent(name);
        } catch (Throwable e) {
            log.error("Failed to unregister component: " + name, e);
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
    public ComponentInstance getComponentProvidingService(
            Class<?> serviceClass) {
        try {
            RegistrationInfoImpl ri = services.get(serviceClass.getName());
            if (ri != null) {
                if (ri.lazyActivate()) {
                    return ri.getComponent();
                }
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
        RegistrationInfo[] comps = null;
        synchronized (this) {
            comps = reg.getComponentsArray();
        }
        Collection<ComponentName> activating = new ArrayList<ComponentName>();
        for (RegistrationInfo ri : comps) {
            if (ri.getState() == RegistrationInfo.ACTIVATING) {
                activating.add(ri.getName());
            }
        }
        return activating;
    }

    void sendEvent(ComponentEvent event) throws RuntimeModelException {
        if (log.isDebugEnabled()) {
            log.debug("Dispatching event: " + event);
        }
        Object[] listeners = this.listeners.getListeners();
        RuntimeModelException.CompoundBuilder errors = new RuntimeModelException.CompoundBuilder();
        for (Object listener : listeners) {
            try {
                ((ComponentListener) listener).handleEvent(event);
            } catch (RuntimeModelException e) {
                errors.add(e);
            }
        }
        errors.throwOnError();
    }

    public synchronized void registerExtension(Extension extension)
            throws RuntimeModelException {
        ComponentName name = extension.getTargetComponent();
        RegistrationInfoImpl ri = reg.getComponent(name);
        if (ri != null && ri.component != null) {
            if (log.isDebugEnabled()) {
                log.debug("Register contributed extension: " + extension);
            }
            loadContributions(ri, extension);
            try {
                ri.component.registerExtension(extension);
            } catch (Exception e) {
                throw new RuntimeModelException(ri + " : cannot register " + extension, e);
            }
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_REGISTERED,
                    ((ComponentInstanceImpl) extension.getComponent()).ri,
                    extension));
        } else { // put the extension in the pending queue
            if (log.isDebugEnabled()) {
                log.debug("Enqueue contributed extension to pending queue: "
                        + extension);
            }
            Set<Extension> extensions = extensionPendingsByComponent.get(name);
            if (extensions == null) {
                extensions = new LinkedHashSet<Extension>(); // must keep order
                                                             // in which
                                                             // extensions are
                                                             // contributed
                extensionPendingsByComponent.put(name, extensions);
            }
            extensions.add(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_PENDING,
                    ((ComponentInstanceImpl) extension.getComponent()).ri,
                    extension));
        }
    }

    public synchronized void unregisterExtension(Extension extension)
            throws RuntimeModelException {
        // TODO check if framework is shutting down and in that case do nothing
        if (log.isDebugEnabled()) {
            log.debug("Unregister contributed extension: " + extension);
        }
        ComponentName name = extension.getTargetComponent();
        RegistrationInfo ri = reg.getComponent(name);
        if (ri != null) {
            ComponentInstance co = ri.getComponent();
            if (co != null) {
                try {
                    co.unregisterExtension(extension);
                } catch (Exception e) {
                    throw new RuntimeModelException(ri + " : cannot unregister " + extension, e);
                }
            }
        } else { // maybe it's pending
            Set<Extension> extensions = extensionPendingsByComponent.get(name);
            if (extensions != null) {
                // FIXME: extensions is a set of Extensions, not ComponentNames.
                extensions.remove(name);
                if (extensions.isEmpty()) {
                    extensionPendingsByComponent.remove(name);
                }
            }
        }
        sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_UNREGISTERED,
                ((ComponentInstanceImpl) extension.getComponent()).ri,
                extension));
    }

    public static void loadContributions(RegistrationInfoImpl ri, Extension xt) {
        ExtensionPointImpl xp = ri.getExtensionPoint(xt.getExtensionPoint());
        if (xp == null) {
            throw new IllegalStateException(
                    "Cannot load contributions, extension point not registered ("
                            + xt + ")");
        }
        try {
            Object[] contribs = xp.loadContributions(ri, xt);
            xt.setContributions(contribs);
        } catch (Exception e) {
            log.error("Failed to create contribution objects", e);
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
    public synchronized String[] getServices() {
        return services.keySet().toArray(new String[services.size()]);
    }

}
