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

    // must use an ordered Set to avoid loosing the order of the pending
    // extensions
    protected final Map<ComponentName, Set<Extension>> pendingExtensions;

    private ListenerList listeners;

    private final Map<String, RegistrationInfoImpl> services;

    protected Set<String> blacklist;

    protected ComponentRegistry reg;

    public ComponentManagerImpl(RuntimeService runtime) {
        reg = new ComponentRegistry();
        pendingExtensions = new HashMap<ComponentName, Set<Extension>>();
        listeners = new ListenerList();
        services = new ConcurrentHashMap<String, RegistrationInfoImpl>();
        blacklist = new HashSet<String>();
    }

    @Override
    public synchronized Collection<RegistrationInfo> getRegistrations() {
        return new ArrayList<RegistrationInfo>(reg.getComponents());
    }

    @Override
    public synchronized Map<ComponentName, Set<ComponentName>> getPendingRegistrations() {
        // TODO the set value is not cloned
        return new HashMap<ComponentName, Set<ComponentName>>(
                reg.getPendingComponents());
    }

    public synchronized Collection<ComponentName> getNeededRegistrations() {
        return pendingExtensions.keySet();
    }

    public synchronized Collection<Extension> getPendingExtensions(
            ComponentName name) {
        return pendingExtensions.get(name);
    }

    @Override
    public synchronized RegistrationInfo getRegistrationInfo(ComponentName name) {
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
    public synchronized ComponentInstance getComponent(ComponentName name) {
        RegistrationInfo ri = reg.getComponent(name);
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
            log.warn("Component " + name.getName()
                    + " was blacklisted. Ignoring.");
            return;
        }
        if (reg.contains(name)) {
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
            // throw new
            // IllegalStateException("Component was already registered: " +
            // name);
        }
        for (ComponentName n : ri.getAliases()) {
            if (reg.contains(n)) {
                String msg = "Duplicate component name: " + n + " (alias for "
                        + name + ")";
                log.error(msg);
                Framework.getRuntime().getWarnings().add(msg);
                return;
            }
        }

        ri.attach(this);

        try {
            log.info("Registering component: " + name);
            if (!reg.addComponent(ri)) {
                log.info("Registration delayed for component: " + name
                        + ". Waiting for: "
                        + reg.getMissingDependencies(ri.getName()));
            }
        } catch (Throwable e) {
            String msg = "Failed to register component: " + name;
            log.error(msg, e);
            msg += " (" + e.toString() + ')';
            Framework.getRuntime().getWarnings().add(msg);
            return;
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
        RegistrationInfoImpl ri = services.get(serviceClass.getName());
        if (ri != null && ri.isActivated()) {
            return ri.getComponent();
        }
        synchronized(this) {
	    if (ri != null && !ri.isActivated()) {
		if (ri.isResolved()) {
		    try {
			ri.activate();
			return ri.getComponent();
		    } catch (Exception e) {
			log.error("Failed to get service: " + serviceClass + ", " + e.getMessage());
		    }
		} else {
		    // Hack to avoid messages during TypeService activation
		    if (!serviceClass.getSimpleName().equals("TypeProvider")) {
			log.debug("The component exposing the service "
				  + serviceClass + " is not resolved");
		    }
		}
	    }
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

    void sendEvent(ComponentEvent event) {
        log.debug("Dispatching event: " + event);
        Object[] listeners = this.listeners.getListeners();
        for (Object listener : listeners) {
            ((ComponentListener) listener).handleEvent(event);
        }
    }

    public synchronized void registerExtension(Extension extension)
            throws Exception {
        ComponentName name = extension.getTargetComponent();
        RegistrationInfoImpl ri = reg.getComponent(name);
        if (ri != null && ri.component != null) {
            if (log.isDebugEnabled()) {
                log.debug("Register contributed extension: " + extension);
            }
            loadContributions(ri, extension);
            ri.component.registerExtension(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_REGISTERED,
                    ((ComponentInstanceImpl) extension.getComponent()).ri,
                    extension));
        } else { // put the extension in the pending queue
            if (log.isDebugEnabled()) {
                log.debug("Enqueue contributed extension to pending queue: "
                        + extension);
            }
            Set<Extension> extensions = pendingExtensions.get(name);
            if (extensions == null) {
                extensions = new LinkedHashSet<Extension>(); // must keep order
                                                             // in which
                                                             // extensions are
                                                             // contributed
                pendingExtensions.put(name, extensions);
            }
            extensions.add(extension);
            sendEvent(new ComponentEvent(ComponentEvent.EXTENSION_PENDING,
                    ((ComponentInstanceImpl) extension.getComponent()).ri,
                    extension));
        }
    }

    public synchronized void unregisterExtension(Extension extension)
            throws Exception {
        // TODO check if framework is shutting down and in that case do nothing
        if (log.isDebugEnabled()) {
            log.debug("Unregister contributed extension: " + extension);
        }
        ComponentName name = extension.getTargetComponent();
        RegistrationInfo ri = reg.getComponent(name);
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
