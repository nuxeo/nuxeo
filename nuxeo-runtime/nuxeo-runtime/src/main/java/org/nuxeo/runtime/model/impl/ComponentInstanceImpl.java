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

import java.lang.reflect.Method;
import java.util.Set;

import org.nuxeo.runtime.model.Adaptable;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.Property;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    public Object getInstance() {
        switch (ri.state) {
        case RegistrationInfo.RESOLVED:
            // if not already activated activate it now
            try {
                ri.activate();
                return instance;
            } catch (Exception e) {
                log.error(e);
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

    public void destroy() throws Exception {
        deactivate();
        instance = null;
        ri = null;
    }

    public RuntimeContext getContext() {
        return ri.context;
    }

    public ComponentName getName() {
        return ri.name;
    }

    // TODO: cache info about implementation to avoid computing it each time
    public void activate() throws Exception {
        // activate the implementation instance
        if (instance instanceof Component) {
            ((Component) instance).activate(this);
        } else { // try by reflection
            try {
                Method meth = instance.getClass().getDeclaredMethod("activate",
                        ComponentContext.class);
                meth.setAccessible(true);
                meth.invoke(instance, this);
            } catch (Exception e) {
                // no such method
            }
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
    public void deactivate() throws Exception {
        // activate the implementation instance
        if (instance instanceof Component) {
            ((Component) instance).deactivate(this);
        } else {
            // try by reflection
            try {
                Method meth = instance.getClass().getDeclaredMethod(
                        "deactivate", ComponentContext.class);
                meth.setAccessible(true);
                meth.invoke(instance, this);
            } catch (Exception e) {
                // no such method
            }
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
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
            log.error("Warning: TARGET EXTENSION POINT IS UNKNOWN. Check your extension in "
                    +extension.getComponent().getName());
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
            }
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
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
            }
        }
    }

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

    public String[] getPropertyNames() {
        Set<String> set = ri.getProperties().keySet();
        return set.toArray(new String[set.size()]);
    }

    public Property getProperty(String property) {
        return ri.getProperties().get(property);
    }

    public RuntimeContext getRuntimeContext() {
        return ri.getContext();
    }

    public Object getPropertyValue(String property) {
        return getPropertyValue(property, null);
    }

    public Object getPropertyValue(String property, Object defValue) {
        Property prop = getProperty(property);
        if (prop != null) {
            return prop.getValue();
        } else {
            return defValue;
        }
    }

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
