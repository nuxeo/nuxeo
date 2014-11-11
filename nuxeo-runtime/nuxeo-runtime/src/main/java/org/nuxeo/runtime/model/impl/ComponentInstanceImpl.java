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
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.lang.reflect.Method;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Adaptable;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.Property;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.ReloadableComponent;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ComponentInstanceImpl implements ComponentInstance {

    private static final Log log = LogFactory.getLog(ComponentInstanceImpl.class);

    protected Object instance;
    protected RegistrationInfoImpl ri;

    public ComponentInstanceImpl(RegistrationInfoImpl ri) throws Exception {
        this.ri = ri;
        if (ri.implementation == null) {
            //TODO
            //should be an extension component
            instance = this;
        } else {
            // TODO: load class only once when creating the registration info
            instance = this.ri.context.loadClass(this.ri.implementation)
                    .newInstance();
        }
    }

    @Override
    public Object getInstance() {
        switch (ri.state) {
        case RegistrationInfo.RESOLVED:
            // if not already activated activate it now
            try {
                ri.activate();
                return instance;
            } catch (Exception e) {
                log.error(e);
                // fatal error if development mode - exit
                Framework.handleDevError(e);
            }
            return null;
        case RegistrationInfo.ACTIVATED:
            return instance;
        default:
            return null;
        }
    }

    public void create() throws Exception {
        if (ri.implementation == null) {
            instance = this; // should be an extension component
        } else {
            // TODO: load class only once when creating the reshgitration info
            instance = ri.context.loadClass(ri.implementation).newInstance();
        }
    }

    @Override
    public void destroy() throws Exception {
        deactivate();
        instance = null;
        ri = null;
    }

    @Override
    public RuntimeContext getContext() {
        return ri.context;
    }

    @Override
    public ComponentName getName() {
        return ri.name;
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void activate() throws Exception {
        // activate the implementation instance
        try {
            if (instance instanceof Component) {
                ((Component) instance).activate(this);
            } else { // try by reflection
                Method meth = instance.getClass().getDeclaredMethod("activate",
                        ComponentContext.class);
                meth.setAccessible(true);
                meth.invoke(instance, this);
            }
        } catch (NoSuchMethodException e) {
            // ignore this exception since the activate method is not mandatory
        } catch (Exception e) {
            log.error("Failed to activate component: "+getName(), e);
            Framework.handleDevError(e);
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void deactivate() throws Exception {
        // activate the implementation instance
        try {
            if (instance instanceof Component) {
                ((Component) instance).deactivate(this);
            } else {
                // try by reflection
                Method meth = instance.getClass().getDeclaredMethod(
                        "deactivate", ComponentContext.class);
                meth.setAccessible(true);
                meth.invoke(instance, this);
            }
        } catch (NoSuchMethodException e) {
            // ignore this exception since the activate method is not mandatory
        } catch (Exception e) {
            log.error("Failed to deactivate component: "+getName(), e);
            Framework.handleDevError(e);
        }
    }

    @Override
    public void reload() throws Exception {
        // activate the implementation instance
        try {
            if (instance instanceof ReloadableComponent) {
                ((ReloadableComponent) instance).reload(this);
            } else {
                Method meth = instance.getClass().getDeclaredMethod("reload",
                        ComponentContext.class);
                meth.setAccessible(true);
                meth.invoke(instance, this);
            }
        } catch (NoSuchMethodException e) {
            // ignore this exception since the reload method is not mandatory
        } catch (Exception e) {
            log.error("Failed to reload component: "+getName(), e);
            Framework.handleDevError(e);
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void registerExtension(Extension extension)
            throws Exception {
        // if this the target extension point is extending another extension point from another component
        // then delegate the registration to the that component component
        ExtensionPoint xp = ri.getExtensionPoint(extension.getExtensionPoint());
        if (xp != null) {
            String superCo = xp.getSuperComponent();
            if (superCo != null) {
                ((ExtensionImpl)extension).target = new ComponentName(superCo);
                ri.manager.registerExtension(extension);
                return;
            }
        } else {
            log.error("Warning: target extension point '"
                    + extension.getExtensionPoint() + "' of '"
                    + extension.getTargetComponent().getName()
                    + "' is unknown. Check your extension in component "
                    + extension.getComponent().getName());
            // fatal error if development mode - exit
            Framework.handleDevError(null);
        }
        // this extension is for us - register it
        // activate the implementation instance
        if (instance instanceof Component) {
            ((Component) instance).registerExtension(extension);
        } else {
            // try by reflection
            try {
                Method meth = instance.getClass().getDeclaredMethod(
                        "registerExtension", Extension.class);
                meth.setAccessible(true);
                meth.invoke(instance, extension);
            } catch (Exception e) {
                // no such method
                Framework.handleDevError(e);
            }
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void unregisterExtension(Extension extension)
            throws Exception {
        // activate the implementation instance
        if (instance instanceof Component) {
            ((Component) instance).unregisterExtension(extension);
        } else {
            // try by reflection
            try {
                Method meth = instance.getClass().getDeclaredMethod(
                        "unregisterExtension", Extension.class);
                meth.setAccessible(true);
                meth.invoke(instance, extension);
            } catch (Exception e) {
                // no such method
                Framework.handleDevError(e);
            }
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        Object object = getInstance();
        if (object instanceof Adaptable) {
            return ((Adaptable) object).getAdapter(adapter);
        }
        if (adapter.isAssignableFrom(object.getClass())) {
            return adapter.cast(object);
        }
        return null;
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> set = ri.getProperties().keySet();
        return set.toArray(new String[set.size()]);
    }

    @Override
    public Property getProperty(String property) {
        return ri.getProperties().get(property);
    }

    @Override
    public RuntimeContext getRuntimeContext() {
        return ri.getContext();
    }

    @Override
    public Object getPropertyValue(String property) {
        return getPropertyValue(property, null);
    }

    @Override
    public Object getPropertyValue(String property, Object defValue) {
        Property prop = getProperty(property);
        if (prop != null) {
            return prop.getValue();
        } else {
            return defValue;
        }
    }

    @Override
    public String[] getProvidedServiceNames() {
        return ri.getProvidedServiceNames();
    }

    @Override
    public String toString() {
        if (ri == null) {
            return super.toString();
        }
        return ri.toString();
    }

}
