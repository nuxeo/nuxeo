/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.util.HashMap;
import java.util.HashSet;
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
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("component")
public class RegistrationInfoImpl implements RegistrationInfo {

    private static final long serialVersionUID = -4135715215018199522L;

    private static final Log log = LogFactory.getLog(RegistrationInfo.class);

    // Note: some of these instance variables are accessed directly from other
    // classes in this package.

    transient ComponentManagerImpl manager;

    @XNode("@service")
    ServiceDescriptor serviceDescriptor;

    // the managed object name
    @XNode("@name")
    ComponentName name;

    @XNode("configuration")
    ConfigurationDescriptor config;

    // the registration state
    int state = UNREGISTERED;

    // the object names I depend of
    @XNodeList(value = "require", type = HashSet.class, componentType = ComponentName.class)
    Set<ComponentName> requires;

    // the object names I depend of and that are blocking my registration
    Set<ComponentName> waitsFor;

    // registration that depends on me
    Set<RegistrationInfoImpl> dependsOnMe;

    @XNode("implementation@class")
    String implementation;

    @XNodeList(value = "extension-point", type = ExtensionPointImpl[].class, componentType = ExtensionPointImpl.class)
    ExtensionPointImpl[] extensionPoints;

    @XNodeList(value = "extension", type = ExtensionImpl[].class, componentType = ExtensionImpl.class)
    ExtensionImpl[] extensions;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = Property.class)
    Map<String, Property> properties;

    @XNode("@version")
    Version version = Version.ZERO;

    @XContent("documentation")
    String documentation;


    transient RuntimeContext context;

    // the managed component
    transient ComponentInstance component;


    public final boolean isPending() {
        return waitsFor != null;
    }

    public void destroy() {
        if (waitsFor != null) {
            waitsFor.clear();
            waitsFor = null;
        }
        if (requires != null) {
            requires.clear();
            requires = null;
        }
        if (dependsOnMe != null) {
            dependsOnMe.clear();
            dependsOnMe = null;
        }
        component = null;
        name = null;
        manager = null;
    }

    public final boolean isDisposed() {
        return name == null;
    }

    public ExtensionPoint[] getExtensionPoints() {
        return extensionPoints;
    }

    public ComponentInstance getComponent() {
        return component;
    }

    public ComponentName getName() {
        return name;
    }

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

    public int getState() {
        return state;
    }

    public Extension[] getExtensions() {
        return extensions;
    }

    public Set<ComponentName> getRequiredComponents() {
        return requires;
    }

    public RuntimeContext getContext() {
        return context;
    }

    public Version getVersion() {
        return version;
    }

    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String toString() {
        return "RegistrationInfo: " + name;
    }

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

    synchronized void unregister() throws Exception {
        if (state == UNREGISTERED) {
            return;
        }
        if (state == ACTIVATED || state == RESOLVED) {
            unresolve();
        }
        state = UNREGISTERED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_UNREGISTERED, this));
        destroy();
    }

    protected ComponentInstance createComponentInstance() throws Exception {
        return new ComponentInstanceImpl(this);
    }

    synchronized void activate() throws Exception {
        if (state != RESOLVED) {
            return;
        }

        component = createComponentInstance();

        state = ACTIVATING;
        manager.sendEvent(new ComponentEvent(ComponentEvent.ACTIVATING_COMPONENT, this));

        // activate component
        component.activate();

        state = ACTIVATED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_ACTIVATED, this));

        // register contributed extensions if any
        if (extensions != null) {
            for (Extension xt : extensions) {
                xt.setComponent(component);
                manager.registerExtension(xt);
            }
        }

        // register pending extensions if any
        ComponentManagerImpl mgr = manager;
        Set<Extension> pendingExt = mgr.pendingExtensions.remove(name);
        if (pendingExt != null) {
            for (Extension xt : pendingExt) {
                mgr.loadContributions(this, xt);
                try {
                    component.registerExtension(xt);
                } catch (Exception e) {
                    log.error("Failed to register extension", e);
                }
            }
        }

    }

    synchronized void deactivate() throws Exception {
        if (state != ACTIVATED) {
            return;
        }

        state = DEACTIVATING;
        manager.sendEvent(new ComponentEvent(ComponentEvent.DEACTIVATING_COMPONENT, this));

        // unregister contributed extensions if any
        if (extensions != null) {
            for (Extension xt : extensions) {
                try {
                    manager.unregisterExtension(xt);
                } catch (Exception e) {
                    log.error("Failed to unregister exception: " + e.getMessage(), e);
                }
            }
        }

        component.deactivate();

        component = null;

        state = RESOLVED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_DEACTIVATED, this));
    }

    synchronized void resolve() throws Exception {
        if (state != REGISTERED) {
            return;
        }

        // register services
        manager.registerServices(this);

        state = RESOLVED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_RESOLVED, this));
        //TODO lazy activation
        activate();
    }

    synchronized void unresolve() throws Exception {
        if (state == REGISTERED || state == UNREGISTERED) {
            return;
        }

        // un-register services
        manager.unregisterServices(this);

        if (state == ACTIVATED) {
            deactivate();
        }
        state = REGISTERED;
        manager.sendEvent(new ComponentEvent(ComponentEvent.COMPONENT_UNRESOLVED, this));
    }

    public synchronized boolean isActivated() {
        return state == ACTIVATED;
    }

    public synchronized boolean isResolved() {
        return state == RESOLVED;
    }

    public String[] getProvidedServiceNames() {
        if (serviceDescriptor != null) {
            return serviceDescriptor.services;
        }
        return null;
    }

}
