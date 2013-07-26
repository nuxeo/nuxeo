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
 */

package org.nuxeo.runtime.model.impl;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.RuntimeModelException;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.ConfigurationDescriptor;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.Property;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject(value = "component", order = "require,extension")
public class RegistrationInfoImpl implements RegistrationInfo {

    private static final long serialVersionUID = -4135715215018199522L;

    private static final Log log = LogFactory.getLog(RegistrationInfoImpl.class);

    // Note: some of these instance variables are accessed directly from other
    // classes in this package.

    protected transient ComponentManagerImpl manager;

    @XNode("@service")
    protected ServiceDescriptor serviceDescriptor;

    // the managed object name
    @XNode("@name")
    protected ComponentName name;

    // my aliases
    @XNodeList(value = "alias", type = HashSet.class, componentType = ComponentName.class)
    protected Set<ComponentName> aliases = new HashSet<ComponentName>();

    protected final Set<ComponentName> names = new HashSet<ComponentName>();

    @XNode("@disabled")
    protected boolean disabled;

    @XNode("configuration")
    protected ConfigurationDescriptor config;

    // the registration state
    protected int state = UNREGISTERED;

    // the object names I depend of
    @XNodeList(value = "require", type = HashSet.class, componentType = ComponentName.class)
    protected Set<ComponentName> requires = new HashSet<ComponentName>();

    protected final Set<RegistrationInfoImpl> dependsOnMe = new HashSet<RegistrationInfoImpl>();

    protected final Set<ComponentName> requiredPendings = new HashSet<ComponentName>();

    protected final Set<RegistrationInfoImpl> requiredRegistered = new HashSet<RegistrationInfoImpl>();

    protected final Set<RegistrationInfoImpl> requiredResolved = new HashSet<RegistrationInfoImpl>();

    @XNode("implementation@class")
    protected String implementation;

    @XNodeList(value = "extension-point", type = ExtensionPointImpl[].class, componentType = ExtensionPointImpl.class)
    protected ExtensionPointImpl[] extensionPoints;

    @XNodeList(value = "extension", type = ExtensionImpl[].class, componentType = ExtensionImpl.class)
    protected ExtensionImpl[] extensions;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = Property.class)
    protected Map<String, Property> properties;

    @XNode("@version")
    protected Version version = Version.ZERO;

    /**
     * To be set when deploying configuration components that are not in a
     * bundle (e.g. from config. dir).
     *
     * Represent the bundle that will be assumed to be the owner of the
     * component.
     */
    @XNode("@bundle")
    protected String bundle;

    @XContent("documentation")
    protected String documentation;

    protected URL xmlFileUrl;

    /**
     * This is used by the component persistence service to identify
     * registration that was dynamically created and persisted by users.
     */
    protected boolean isPersistent;

    protected transient AbstractRuntimeContext context;

    // the managed component
    protected transient ComponentInstanceImpl component;

    public RegistrationInfoImpl() {
    }

    /**
     * Useful when dynamically registering components
     *
     * @param name the component name
     */
    public RegistrationInfoImpl(ComponentName name) {
        this.name = name;
    }

    /**
     * Attach to a manager - this method must be called after all registration
     * fields are initialized.
     *
     * @param manager
     * @throws RuntimeModelException
     */
    public void attach(ComponentManagerImpl manager) throws RuntimeModelException {
        if (this.manager != null) {
            throw new IllegalStateException("Registration '" + name
                    + "' was already attached to a manager");
        }
        this.manager = manager;
        computeNames();
        computePendings();
    }

    protected void computeNames() {
        names.add(name);
        if (aliases != null) {
            names.addAll(aliases);
        }
    }

    protected void computePendings() throws RuntimeModelException {
        if (requires == null || requires.isEmpty()) {
            return;
        }
        // fill the requirements and pending map
        RuntimeModelException.CompoundBuilder  errors = RuntimeModelException.newErrors();
        for (ComponentName otherName : requires) {
            RegistrationInfoImpl other = manager.getRegistrationInfo(otherName);
            if (other != null) {
                if (other.isResolved()) {
                    requiredResolved.add(other);
                } else {
                    requiredRegistered.add(other);
                }
                other.dependsOnMe.add(this);
            } else {
                requiredPendings.add(otherName);
            }
        }
        errors.throwOnError();
    }

    public void setContext(AbstractRuntimeContext rc) {
        context = rc;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public final boolean isPersistent() {
        return isPersistent;
    }

    @Override
    public void setPersistent(boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    public final boolean isDisposed() {
        return manager == null;
    }

    @Override
    public ExtensionPoint[] getExtensionPoints() {
        return extensionPoints;
    }

    @Override
    public ComponentInstanceImpl getComponent() {
        return component;
    }

    /**
     * Reload the underlying component if reload is supported
     *
     * @throws Exception
     */
    public synchronized void reload() throws Exception {
        if (component != null) {
            component.reload();
        }
    }

    @Override
    public ComponentName getName() {
        return name;
    }

    @Override
    public Map<String, Property> getProperties() {
        return properties;
    }

    public ExtensionPointImpl getExtensionPoint(String name) {
        for (ExtensionPointImpl xp : extensionPoints) {
            if (xp.name.equals(name)) {
                return xp;
            }
        }
        return null;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public Extension[] getExtensions() {
        return extensions;
    }

    @Override
    public Set<ComponentName> getAliases() {
        return aliases == null ? Collections.<ComponentName> emptySet()
                : Collections.unmodifiableSet(aliases);
    }

    @Override
    public Set<ComponentName> getRequiredComponents() {
        return Collections.unmodifiableSet(requires);
    }

    @Override
    public RuntimeContext getContext() {
        return context;
    }

    @Override
    public String getBundle() {
        return bundle;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String toString() {
        return "RegistrationInfo: " + name;
    }

    @Override
    public ComponentManager getManager() {
        return manager;
    }

    protected synchronized void register(
            Set<? extends RegistrationInfoImpl> dependsOnMe) throws RuntimeModelException {
        if (state != UNREGISTERED) {
            throw new IllegalStateException("Component not in registered state"
                    + this);
        }
        this.dependsOnMe.addAll(dependsOnMe);
        state = REGISTERED;
        handlePreRegistered();
        manager.sendEvent(new ComponentEvent(
                ComponentEvent.COMPONENT_REGISTERED, this));
        handleRegistered();
    }

    protected void handlePreRegistered() {
        for (RegistrationInfoImpl other : dependsOnMe) { // unaliased
            RegistrationInfoImpl otherImpl = other;
            otherImpl.requiredPendings.removeAll(names);
            otherImpl.requiredRegistered.add(this);
        }
    }

    protected void handleRegistered() throws RuntimeModelException {
        if (requiredPendings.isEmpty() && requiredRegistered.isEmpty()) {
            resolve();
        }
    }

    synchronized void unregister() throws Exception {
        if (state != REGISTERED) {
            throw new IllegalStateException("Component not in registered state"
                    + this);
        }
        state = UNREGISTERED;
        manager.sendEvent(new ComponentEvent(
                ComponentEvent.COMPONENT_UNREGISTERED, this));
        for (RegistrationInfoImpl other : requiredResolved) {
            other.dependsOnMe.remove(this);
        }
        manager = null;
    }

    protected ComponentInstanceImpl createComponentInstance() throws RuntimeModelException {
        try {
            return new ComponentInstanceImpl(this);
        } catch (Exception e) {
            String msg = "Failed to instantiate component: " + implementation;
            log.error(msg, e);
            msg += " (" + e.toString() + ')';
            Framework.getRuntime().getWarnings().add(msg);
            Framework.handleDevError(e);
            throw e;
        }
    }

    public synchronized void restart() throws Exception {
        deactivate();
        activate();
    }

    @Override
    public int getApplicationStartedOrder() {
        if (component == null) {
            return 0;
        }
        Object ci = component.getInstance();
        if (!(ci instanceof Component)) {
            return 0;
        }
        return ((Component) ci).getApplicationStartedOrder();
    }

    @Override
    public void notifyApplicationStarted() throws Exception {
        if (component != null) {
            Object ci = component.getInstance();
            if (ci instanceof Component) {
                try {
                    ((Component) ci).applicationStarted(component);
                } catch (Exception e) {
                    log.error(
                            "Component notification of application started failed.",
                            e);
                    state = RESOLVED;
                }
            }
        }
    }

    public boolean lazyActivate() {
        if (state != RESOLVED) {
            return isActivated();
        }
        try {
            activate();
        } catch (Exception e) {
            log.error("Cannot lazy activate " + this, e);
            Framework.handleDevError(e);
            return false;
        }
        return true;
    }

    @Override
    public synchronized void activate() throws RuntimeModelException {
        if (state != RESOLVED) {
            throw new IllegalStateException("component not in resolved state ("
                    + this + ")");
        }

        if (context.state != RuntimeContext.ACTIVATING
                && context.state != RuntimeContext.ACTIVATED) {
            throw new IllegalStateException("context not in activating state ("
                    + context + ")");
        }

        state = ACTIVATING;
        manager.sendEvent(new ComponentEvent(
                ComponentEvent.ACTIVATING_COMPONENT, this));

        handleActivating();

        state = ACTIVATED;
        manager.sendEvent(new ComponentEvent(
                ComponentEvent.COMPONENT_ACTIVATED, this));

        handleActivated();

    }

    protected void handleActivating() throws RuntimeModelException {
        // check required component
        for (RegistrationInfoImpl other : requiredResolved) {
            if (context == other.context) {
                other.lazyActivate();
            } else if (other.state != ACTIVATED) {
                throw new IllegalStateException(
                        "required component is not activated (" + this + "->"
                                + other);
            }
        }

        component = createComponentInstance();

        component.activate();
        log.info("Component activated: " + name);

        RuntimeModelException.CompoundBuilder errors = new RuntimeModelException.CompoundBuilder();
        // register contributed extensions if any
        if (extensions != null) {
            for (Extension xt : extensions) {
                if (xt.getTargetComponent() == null) {
                    errors.add(new RuntimeModelException(
                            "Bad extension declaration (no target attribute specified). Component: "
                                    + getName()));
                    continue;
                }
                xt.setComponent(component);
                try {
                    manager.registerExtension(xt);
                } catch (RuntimeModelException e) {
                    errors.add(e);
                }
            }
        }

        // register pending extensions if any
        ComponentManagerImpl mgr = manager;
        Set<Extension> contributedExtensions = mgr.extensionPendingsByComponent.remove(name);
        if (contributedExtensions == null) {
            return;
        }

        for (Extension xt : contributedExtensions) {
            ComponentManagerImpl.loadContributions(this, xt);
            try {
                component.registerExtension(xt);
            } catch (Exception e) {
                errors.add(new RuntimeModelException("Failed to register extension to: "
                        + xt.getTargetComponent() + ", xpoint: "
                        + xt.getExtensionPoint() + " in component: "
                        + xt.getComponent().getName(), e));
            }
        }
        errors.throwOnError();
    }

    protected void handleActivated() {

    }

    public synchronized void deactivate() throws RuntimeModelException {
        if (state != ACTIVATED) {
            return;
        }

        state = DEACTIVATING;
        RuntimeModelException.CompoundBuilder errors = new RuntimeModelException.CompoundBuilder();

        try {
            manager.sendEvent(new ComponentEvent(
                    ComponentEvent.DEACTIVATING_COMPONENT, this));
        } catch (RuntimeModelException e) {
            errors.add(e);
        }

        handleDeactivating();

        state = RESOLVED;
        try {
            manager.sendEvent(new ComponentEvent(
                    ComponentEvent.COMPONENT_DEACTIVATED, this));
        } catch (RuntimeModelException e) {
            errors.add(e);
        }

        handleDeactivated();

        errors.throwOnError();
    }

    protected void handleDeactivating() throws RuntimeModelException {
        // unregister contributed extensions if any
        RuntimeModelException.CompoundBuilder errors = RuntimeModelException.newErrors();
        if (extensions != null) {
            for (Extension xt : extensions) {
                try {
                    manager.unregisterExtension(xt);
                } catch (RuntimeModelException e) {
                    errors.add(e);
                }
            }
        }

        // deactivate depends
        Iterator<RegistrationInfoImpl> it = dependsOnMe.iterator();
        for (RegistrationInfoImpl other:dependsOnMe) {
            try {
                other.deactivate();
            } catch (RuntimeModelException e) {
                errors.add(e);
            }
        }

        // deactivate component
        try {
            component.deactivate();
        } catch (RuntimeModelException e) {
            log.error("Failed to de-activate " + this, e);
            Framework.handleDevError(e);
        }
        component = null;
        errors.throwOnError();
    }

    protected void handleDeactivated() {
        log.info("Component deactivated: " + name);
    }

    public synchronized void resolve() throws RuntimeModelException {
        if (state != REGISTERED) {
            return;
        }

        state = RESOLVED;

        handleResolving();

        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_RESOLVED,
                this));

        handleResolved();

    }

    protected void handleResolving() throws RuntimeModelException {
        manager.registerServices(this);
        RuntimeModelException.CompoundBuilder errors = RuntimeModelException.newErrors();
        for (RegistrationInfoImpl other : dependsOnMe) {
            if (other.context != context) {
                continue;
            }
            other.requiredRegistered.remove(this);
            other.requiredResolved.add(this);
            if (other.requiredPendings.isEmpty()
                    && other.requiredRegistered.isEmpty()) {
                try {
                    other.resolve();
                } catch (RuntimeModelException e) {
                    errors.add(e);
                }
            }
        }
        errors.throwOnError();
    }

    protected void handleResolved() throws RuntimeModelException {
        for (RegistrationInfoImpl other : dependsOnMe) {
            if (other.context == context) {
                continue;
            }
            other.requiredRegistered.remove(this);
            other.requiredResolved.add(this);
            if (other.requiredPendings.isEmpty()
                    && other.requiredRegistered.isEmpty()) {
                other.resolve();
            }
        }
    }

    public synchronized void unresolve() throws Exception {
        if (state == REGISTERED || state == UNREGISTERED) {
            return;
        }

        if (state == ACTIVATED) {
            deactivate();
        }

        manager.unregisterServices(this);

        state = REGISTERED;
        manager.sendEvent(new ComponentEvent(
                ComponentEvent.COMPONENT_UNRESOLVED, this));
    }

    @Override
    public synchronized boolean isActivated() {
        return state >= ACTIVATED;
    }

    @Override
    public synchronized boolean isResolved() {
        return state >= RESOLVED;
    }

    @Override
    public String[] getProvidedServiceNames() {
        if (serviceDescriptor != null) {
            return serviceDescriptor.services;
        }
        return null;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    @Override
    public String getImplementation() {
        return implementation;
    }

    @Override
    public URL getXmlFileUrl() {
        return xmlFileUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RegistrationInfo) {
            return name.equals(((RegistrationInfo) obj).getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
