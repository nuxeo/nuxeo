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
 */

package org.nuxeo.runtime.model.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.nuxeo.runtime.model.ComponentInstance;
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
 */
@XObject("component")
public class RegistrationInfoImpl implements RegistrationInfo {

    private static final long serialVersionUID = -4135715215018199522L;

    private static final Log log = LogFactory.getLog(RegistrationInfoImpl.class);

    // Note: some of these instance variables are accessed directly from other
    // classes in this package.

    transient ComponentManagerImpl manager;

    @XNode("@service")
    ServiceDescriptor serviceDescriptor;

    // the managed object name
    @XNode("@name")
    ComponentName name;

    @XNode("@disabled")
    boolean disabled;

    @XNode("configuration")
    ConfigurationDescriptor config;

    // the registration state
    int state = UNREGISTERED;

    // my aliases
    @XNodeList(value = "alias", type = HashSet.class, componentType = ComponentName.class)
    Set<ComponentName> aliases = new HashSet<>();

    // the object names I depend of
    @XNodeList(value = "require", type = HashSet.class, componentType = ComponentName.class)
    Set<ComponentName> requires = new HashSet<>();

    @XNode("implementation@class")
    String implementation;

    @XNodeList(value = "extension-point", type = ExtensionPointImpl[].class, componentType = ExtensionPointImpl.class)
    ExtensionPointImpl[] extensionPoints = new ExtensionPointImpl[0];

    @XNodeList(value = "extension", type = ExtensionImpl[].class, componentType = ExtensionImpl.class)
    ExtensionImpl[] extensions = new ExtensionImpl[0];

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = Property.class)
    Map<String, Property> properties = new HashMap<>();

    @XNode("@version")
    Version version = Version.ZERO;

    /**
     * To be set when deploying configuration components that are not in a bundle (e.g. from config. dir). Represent the
     * bundle that will be assumed to be the owner of the component.
     */
    @XNode("@bundle")
    String bundle;

    @XContent("documentation")
    String documentation;

    URL xmlFileUrl;

    /**
     * This is used by the component persistence service to identify registration that was dynamically created and
     * persisted by users.
     */
    boolean isPersistent;

    transient RuntimeContext context;

    // the managed component
    transient ComponentInstance component;

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
     * Attach to a manager - this method must be called after all registration fields are initialized.
     *
     * @param manager
     */
    public void attach(ComponentManagerImpl manager) {
        if (this.manager != null) {
            throw new IllegalStateException("Registration '" + name + "' was already attached to a manager");
        }
        this.manager = manager;
    }

    public void setContext(RuntimeContext rc) {
        this.context = rc;
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

    public void destroy() {
        requires.clear();
        aliases.clear();
        properties.clear();
        extensionPoints = new ExtensionPointImpl[0];
        extensions = new ExtensionImpl[0];
        version = null;
        component = null;
        name = null;
        manager = null;
    }

    public final boolean isDisposed() {
        return name == null;
    }

    @Override
    public ExtensionPoint[] getExtensionPoints() {
        return extensionPoints;
    }

    @Override
    public ComponentInstance getComponent() {
        return component;
    }

    /**
     * Reload the underlying component if reload is supported
     */
    public synchronized void reload() {
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
        return aliases == null ? Collections.<ComponentName> emptySet() : aliases;
    }

    @Override
    public Set<ComponentName> getRequiredComponents() {
        return requires;
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

    synchronized void register() {
        if (state != UNREGISTERED) {
            return;
        }
        state = REGISTERED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_REGISTERED, this));
    }

    synchronized void unregister() {
        if (state == UNREGISTERED) {
            return;
        }
        if (state == ACTIVATED || state == RESOLVED || state == START_FAILURE ) {
            unresolve();
        }
        state = UNREGISTERED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_UNREGISTERED, this));
        destroy();
    }

    protected ComponentInstance createComponentInstance() {
        try {
            return new ComponentInstanceImpl(this);
        } catch (RuntimeException e) {
            String msg = "Failed to instantiate component: " + implementation;
            log.error(msg, e);
            msg += " (" + e.toString() + ')';
            Framework.getRuntime().getErrors().add(msg);
            throw e;
        }
    }

    public synchronized void restart() {
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
    public void notifyApplicationStarted() {
        if (component != null) {
            Object ci = component.getInstance();
            if (ci instanceof Component) {
                try {
                    ((Component) ci).applicationStarted(component);
                } catch (RuntimeException e) {
                    log.error(String.format("Component %s notification of application started failed: %s",
                            component.getName(), e.getMessage()), e);
                    state = START_FAILURE;
                }
            }
        }
    }

    public synchronized void activate() {
        if (state != RESOLVED) {
            return;
        }

        component = createComponentInstance();

        state = ACTIVATING;
        manager.sendEvent(new ComponentEvent(ComponentEvent.ACTIVATING_COMPONENT, this));

        // activate component
        component.activate();
        log.info("Component activated: " + name);

        state = ACTIVATED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_ACTIVATED, this));

        // register contributed extensions if any
        if (extensions != null) {
            checkExtensions();
            for (Extension xt : extensions) {
                xt.setComponent(component);
                try {
                    manager.registerExtension(xt);
                } catch (RuntimeException e) {
                    String msg = "Failed to register extension to: " + xt.getTargetComponent() + ", xpoint: "
                            + xt.getExtensionPoint() + " in component: " + xt.getComponent().getName();
                    log.error(msg, e);
                    msg += " (" + e.toString() + ')';
                    Framework.getRuntime().getErrors().add(msg);
                }
            }
        }

        // register pending extensions if any
        List<ComponentName> names = new ArrayList<>(1 + aliases.size());
        names.add(name);
        names.addAll(aliases);
        for (ComponentName n : names) {
            Set<Extension> pendingExt = manager.pendingExtensions.remove(n);
            if (pendingExt == null) {
                continue;
            }
            for (Extension xt : pendingExt) {
                ComponentManagerImpl.loadContributions(this, xt);
                try {
                    component.registerExtension(xt);
                } catch (RuntimeException e) {
                    String msg = "Failed to register extension to: " + xt.getTargetComponent() + ", xpoint: "
                            + xt.getExtensionPoint() + " in component: " + xt.getComponent().getName();
                    log.error(msg, e);
                    msg += " (" + e.toString() + ')';
                    Framework.getRuntime().getErrors().add(msg);
                }
            }
        }
    }

    public synchronized void deactivate() {
        if (state != ACTIVATED && state != START_FAILURE) {
            return;
        }

        state = DEACTIVATING;
        manager.sendEvent(new ComponentEvent(ComponentEvent.DEACTIVATING_COMPONENT, this));

        // unregister contributed extensions if any
        if (extensions != null) {
            for (Extension xt : extensions) {
                try {
                    manager.unregisterExtension(xt);
                } catch (RuntimeException e) {
                    String message = "Failed to unregister extension. Contributor: " + xt.getComponent() + " to "
                            + xt.getTargetComponent() + "; xpoint: " + xt.getExtensionPoint();
                    log.error(message, e);
                    Framework.getRuntime().getErrors().add(message);
                }
            }
        }

        component.deactivate();

        component = null;

        state = RESOLVED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_DEACTIVATED, this));
    }

    public synchronized void resolve() {
        if (state != REGISTERED) {
            return;
        }

        // register services
        manager.registerServices(this);

        state = RESOLVED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_RESOLVED, this));
        // TODO lazy activation
        activate();
    }

    public synchronized void unresolve() {
        if (state == REGISTERED || state == UNREGISTERED) {
            return;
        }

        // un-register services
        manager.unregisterServices(this);

        if (state == ACTIVATED || state == START_FAILURE) {
            deactivate();
        }
        state = REGISTERED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_UNRESOLVED, this));
    }

    @Override
    // not synchronized, intermediate states from other synchronized methods
    // are not a problem
    public boolean isActivated() {
        return state == ACTIVATED;
    }

    @Override
    // not synchronized, intermediate states from other synchronized methods
    // are not a problem
    public boolean isResolved() {
        return state == RESOLVED;
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

    public void checkExtensions() {
        // HashSet<String> targets = new HashSet<String>();
        for (ExtensionImpl xt : extensions) {
            if (xt.target == null) {
                Framework.getRuntime().getWarnings().add(
                        "Bad extension declaration (no target attribute specified). Component: " + getName());
                continue;
            }
            // TODO do nothing for now -> fix the faulty components and then
            // activate these warnings
            // String key = xt.target.getName()+"#"+xt.getExtensionPoint();
            // if (targets.contains(key)) { // multiple extensions to same
            // target point declared in same component
            // String message =
            // "Component "+getName()+" contains multiple extensions to "+key;
            // Framework.getRuntime().getWarnings().add(message);
            // //TODO: un-comment the following line if you want to treat this
            // as a dev. error
            // //Framework.handleDevError(new Error(message));
            // } else {
            // targets.add(key);
            // }
        }
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
