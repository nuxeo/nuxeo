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
 *     Nuxeo - initial API and implementation
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.model.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Adaptable;
import org.nuxeo.runtime.model.Component;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.Property;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.service.TimestampedService;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComponentInstanceImpl implements ComponentInstance {

    private static final Log log = LogFactory.getLog(ComponentInstanceImpl.class);

    protected Object instance;

    protected RegistrationInfo ri;

    protected List<OSGiServiceFactory> factories;

    public ComponentInstanceImpl(RegistrationInfo ri) {
        this.ri = ri;
        if (ri.getImplementation() == null) {
            // TODO: should be an extension component
            instance = this;
        } else {
            // TODO: load class only once when creating the registration info
            instance = createInstance();
        }
    }

    @Override
    public Object getInstance() {
        return instance;
    }

    public void create() {
        if (ri.getImplementation() == null) {
            instance = this; // should be an extension component
        } else {
            // TODO: load class only once when creating the reshgitration info
            instance = createInstance();
        }
    }

    protected Object createInstance() {
        Object object;
        try {
            object = ri.getContext().loadClass(ri.getImplementation()).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new RuntimeServiceException(e);
        }
        if (object instanceof Component) {
            ((Component) object).setName(ri.getName().getName());
        }
        return object;
    }

    @Override
    public void destroy() {
        deactivate();
        instance = null;
        ri = null;
        factories = null;
    }

    @Override
    public RuntimeContext getContext() {
        return ri.getContext();
    }

    @Override
    public ComponentName getName() {
        return ri.getName();
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void activate() {
        // activate the implementation instance
        try {
            if (instance instanceof Component) {
                ((Component) instance).activate(this);
            } else if (instance != this) {
                // try by reflection
                MethodUtils.invokeMethod(instance, true, "activate", this);
            }
            registerServices();
        } catch (NoSuchMethodException e) {
            // ignore this exception since the activate method is not mandatory
        } catch (ReflectiveOperationException e) {
            handleError("Failed to activate component: " + getName(), Source.COMPONENT, getName().getName(), e);
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void deactivate() {
        // activate the implementation instance
        try {
            unregisterServices();
            if (instance instanceof Component) {
                ((Component) instance).deactivate(this);
            } else if (instance != this) {
                // try by reflection
                MethodUtils.invokeMethod(instance, true, "deactivate", this);
            }
        } catch (NoSuchMethodException e) {
            // ignore this exception since the deactivate method is not mandatory
        } catch (ReflectiveOperationException e) {
            handleError("Failed to deactivate component: " + getName(), Source.COMPONENT, getName().getName(), e);
        }
    }

    /**
     * @since 9.3
     */
    @Override
    public void start() {
        if (instance instanceof Component) {
            ((Component) instance).start(this);
        }
    }

    /**
     * @since 9.3
     */
    @Override
    public void stop() throws InterruptedException {
        if (instance instanceof Component) {
            ((Component) instance).stop(this);
        }
    }

    /**
     * @deprecated since 9.3, but in fact since 5.6, only usage in {@link RegistrationInfoImpl}
     */
    @Deprecated
    @Override
    public void reload() {
        // activate the implementation instance
        try {
            MethodUtils.invokeMethod(instance, true, "reload", this);
        } catch (NoSuchMethodException e) {
            // ignore this exception since the reload method is not mandatory
        } catch (ReflectiveOperationException e) {
            handleError("Failed to reload component: " + getName(), Source.COMPONENT, getName().getName(), e);
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void registerExtension(Extension extension) {
        // if this the target extension point is extending another extension
        // point from another component
        // then delegate the registration to the that component component
        Optional<ExtensionPoint> optXp = ri.getExtensionPoint(extension.getExtensionPoint());
        if (optXp.isPresent()) {
            String superCo = optXp.get().getSuperComponent();
            if (superCo != null) {
                // we don't implement extension point overriding for now in new implementation of RegistrationInfo
                ((ExtensionImpl) extension).target = new ComponentName(superCo);
                ((RegistrationInfoImpl) ri).manager.registerExtension(extension);
                return;
            }
            // this extension is for us - register it
            // activate the implementation instance
            if (instance instanceof Component) {
                ((Component) instance).registerExtension(extension);
            } else if (instance != this) {
                // try by reflection, avoiding stack overflow
                try {
                    MethodUtils.invokeMethod(instance, true, "registerExtension", extension);
                } catch (ReflectiveOperationException e) {
                    ComponentName compName = extension.getComponent().getName();
                    handleError("Error registering " + compName, Source.EXTENSION, compName.getName(), e);
                }
            }
        } else {
            ComponentName compName = extension.getComponent().getName();
            String message = "Warning: target extension point '" + extension.getExtensionPoint() + "' of '"
                    + extension.getTargetComponent().getName() + "' is unknown. Check your extension in component "
                    + compName;
            handleError(message, Source.EXTENSION, compName.getName(), null);
        }
    }

    // TODO: cache info about implementation to avoid computing it each time
    @Override
    public void unregisterExtension(Extension extension) {
        // activate the implementation instance
        if (instance instanceof Component) {
            ((Component) instance).unregisterExtension(extension);
        } else if (instance != this) {
            // try by reflection, avoiding stack overflow
            try {
                MethodUtils.invokeMethod(instance, true, "unregisterExtension", extension);
            } catch (ReflectiveOperationException e) {
                ComponentName compName = extension.getComponent().getName();
                handleError("Error unregistering " + compName, Source.EXTENSION, compName.getName(), e);
            }
        }
    }

    protected void handleError(String message, Source source, String sourceId, Exception e) {
        Exception ee = e;
        if (e != null) {
            ee = ExceptionUtils.unwrapInvoke(e);
        }
        log.error(message, ee);
        Framework.getRuntime()
                 .getMessageHandler()
                 .addMessage(new RuntimeMessage(Level.ERROR, message, source, sourceId));
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        T res = null;
        Object object = getInstance();
        if (object == null) {
            return null;
        }
        if (object instanceof Adaptable) {
            res = ((Adaptable) object).getAdapter(adapter);
        } else if (adapter.isAssignableFrom(object.getClass())) {
            res = adapter.cast(object);
        }
        // to handle hot reload
        if (res instanceof TimestampedService && object instanceof TimestampedService) {
            Long lastModified = ((TimestampedService) object).getLastModified();
            ((TimestampedService) res).setLastModified(lastModified);
        }
        return res;
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

    /**
     * Register provided services as OSGi services
     */
    public void registerServices() {
        if (!Framework.isOSGiServiceSupported()) {
            return;
        }
        String[] names = getProvidedServiceNames();
        if (names != null && names.length > 0) {
            factories = new ArrayList<>();
            for (String className : names) {
                OSGiServiceFactory factory = new OSGiServiceFactory(className);
                factory.register();
                factories.add(factory);
            }
        }
    }

    public void unregisterServices() {
        // TODO the reload method is not reloading services. do we want this?
        if (factories != null) {
            for (OSGiServiceFactory factory : factories) {
                factory.unregister();
            }
            factories = null;
        }
    }

    @Override
    public String toString() {
        if (ri == null) {
            return super.toString();
        }
        return ri.toString();
    }

    protected class OSGiServiceFactory implements ServiceFactory {
        protected Class<?> clazz;

        protected ServiceRegistration reg;

        public OSGiServiceFactory(String className) {
            this(ri.getContext().getBundle(), className);
        }

        public OSGiServiceFactory(Bundle bundle, String className) {
            try {
                clazz = ri.getContext().getBundle().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeServiceException(e);
            }
        }

        @Override
        public Object getService(Bundle bundle, ServiceRegistration registration) {
            return getAdapter(clazz);
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            // do nothing
        }

        public void register() {
            reg = ri.getContext().getBundle().getBundleContext().registerService(clazz.getName(), this, null);
        }

        public void unregister() {
            if (reg != null) {
                reg.unregister();
            }
            reg = null;
        }
    }

    @Override
    public RegistrationInfo getRegistrationInfo() {
        return ri;
    }

}
