/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *      Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsesoft.mmbi.NamedModelMBean;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ResourceFactory.Callback;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class ManagementServiceImpl extends DefaultComponent implements
        ManagementService {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.management.ManagementService");

    private static final Log log = LogFactory.getLog(ManagementServiceImpl.class);

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("resources")) {
            doRegister((ResourceDescriptor) contribution);
        } else if (extensionPoint.equals("factories")) {
            doRegisterFactory((ResourceFactoryDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("resources")) {
            doUnregister((ResourceDescriptor) contribution);
        } else {
            doUnregisterFactory((ResourceFactoryDescriptor) contribution);
        }
    }

    protected final Map<ResourceFactoryDescriptor, ResourceFactory> factories = new HashMap<ResourceFactoryDescriptor, ResourceFactory>();

    @SuppressWarnings("unchecked")
    protected void doRegisterFactory(ResourceFactoryDescriptor descriptor) {
        String factoryClassName = descriptor.getClassName();
        ResourceFactory factory = null;
        try {
            Class<ResourceFactory> factoryClass = (Class<ResourceFactory>) getClass().getClassLoader().loadClass(
                    descriptor.getClassName());
            try {
                factory = factoryClass.getConstructor(
                        new Class[] { ResourceFactoryDescriptor.class }).newInstance(
                        descriptor);
            } catch (NoSuchMethodException e) {
                factory = factoryClass.newInstance();
            }
        } catch (Exception e) {
            throw new ManagementRuntimeException("Cannot create factory "
                    + factoryClassName, e);
        }
        factories.put(descriptor, factory);
        factory.registerResources(new Callback() {
            public void invokeFor(ObjectName name, Class<?> info,
                    Object instance) {
                doRegister(name, instance, info);
            }
        });
    }

    protected void doUnregisterFactory(ResourceFactoryDescriptor descriptor) {
        factories.remove(descriptor);
    }

    protected final Map<ResourceDescriptor, Resource> resources = new HashMap<ResourceDescriptor, Resource>();

    protected void doRegister(ObjectName name, Object instance, Class<?> info) {
        ResourceDescriptor descriptor = new ResourceDescriptor(name, info);
        Resource resource = new Resource(descriptor, name, info, instance);
        doRegister(resource);
    }

    protected void doRegister(ResourceDescriptor descriptor) {
        Resource resource = doResolve(descriptor);
        doRegister(resource);
        if (log.isInfoEnabled()) {
            log.info("registered management contribution for " + descriptor);
        }
    }

    protected void doUnregister(ResourceDescriptor descriptor) {
        Resource resource = resources.remove(descriptor);
        if (resource == null) {
            throw new IllegalArgumentException(descriptor
                    + " is not registered");
        }
        if (resource.isRegistered()) {
            doUnbind(resource);
        }
    }

    protected Class<?> doLoadClass(String className) {
        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw ManagementRuntimeException.wrap(e);
        }
    }

    protected <T> T doCreateResourceInstance(Class<T> resourceClass,
            ResourceDescriptor descriptor) {
        try {
            return resourceClass.getConstructor(ResourceDescriptor.class).newInstance(
                    descriptor);
        } catch (Exception e) {
            try {
                return resourceClass.newInstance();
            } catch (Exception cause) {
                throw new ManagementRuntimeException(
                        "Cannot create resource for " + descriptor, cause);
            }
        }
    }

    protected <T> T doGetResourceInstance(Class<T> resourceClass,
            ResourceDescriptor descriptor) {
        if (descriptor.isAdapted()) {
            return doCreateResourceInstance(resourceClass, descriptor);
        }
        T resource;
        try {
            resource = Framework.getService(resourceClass);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(
                    "Cannot locate resource using " + resourceClass, e);
        }
        if (resource == null) {
            throw new ManagementRuntimeException(
                    "Cannot locate resource using " + resourceClass);
        }
        return resource;
    }

    protected Resource doResolve(ResourceDescriptor descriptor) {
        Class<?> resourceClass = doLoadClass(descriptor.getClassName());
        Object resourceInstance = doGetResourceInstance(resourceClass,
                descriptor);
        String resourceName = descriptor.getName();
        if (resourceName == null) {
            resourceName = resourceClass.getSimpleName();
        }
        ObjectName managementName = ObjectNameFactory.getObjectName(resourceName);
        String ifaceClassName = descriptor.getIfaceClassName();
        Class<?> managementClass = ifaceClassName != null ? doLoadClass(ifaceClassName)
                : resourceClass;
        return new Resource(descriptor, managementName, managementClass,
                resourceInstance);
    }

    protected final ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    public void register(ObjectName name, Object instance, Class<?> clazz) {
        doRegister(name, instance, clazz);
    }

    protected void doRegister(Resource resource) {
        resources.put(resource.getDescriptor(), resource);
        if (log.isDebugEnabled()) {
            log.debug("registered " + resource.getName());
        }
    }

    public void unregister(ObjectName name) {
        doUnregister(name);
    }

    protected void doUnregister(ObjectName objectName) {
        Resource resource = resources.remove(objectName);
        if (resource == null) {
            throw new ManagementRuntimeException(objectName
                    + " is not registered");
        }
        if (resource.mbean != null) {
            doUnbind(resource);
        }
    }

    protected void doBind(Resource resource) {
        if (resource.mbean != null) {
            throw new IllegalStateException(resource.getDescriptor()
                    + " is already bound");
        }
        NamedModelMBean mbean = null;
        try {
            mbean = new NamedModelMBean();
            mbean.setManagedResource(resource.getInstance(), "ObjectReference");
            mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(resource.getClazz()));
            mbean.setInstance(mbeanServer.registerMBean(mbean,
                    resource.getName()));
            if (log.isDebugEnabled()) {
                log.debug("bound " + resource.getName());
            }
        } catch (Exception e) {
            log.error("Cannot bind "
                    + resource.getDescriptor(), e);
        }
        resource.mbean = mbean;
    }

    protected void doUnbind(Resource resource) {
        if (resource.mbean == null) {
            throw new IllegalStateException(resource.getDescriptor()
                    + " is not bound");
        }
        try {
            mbeanServer.unregisterMBean(resource.mbean.getName());
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot unbind "
                    + resource.getDescriptor(), e);
        } finally {
            resource.mbean = null;
            if (log.isDebugEnabled()) {
                log.debug("unbound " + resource.getName());
            }
        }

    }

    public class ManagementAdapter implements ManagementServiceMBean {

        public Set<ObjectName> getResourcesName() {
            Set<ObjectName> names = new HashSet<ObjectName>();
            for (Resource resource : resources.values()) {
                names.add(resource.getName());
            }
            return names;
        }

        private boolean isEnabled = false;

        public void enable() {
            if (isEnabled) {
                throw new IllegalStateException("already enabled");
            }
            try {
                doBindResources();
            } finally {
                isEnabled = true;
            }
        }

        public void disable() {
            if (!isEnabled) {
                throw new IllegalStateException("already disabled");
            }
            try {
                doUnbindResources();
            } finally {
                isEnabled = false;
            }
        }

        public boolean isEnabled() {
            return resources != null;
        }
    }

    protected void doBindResources() {
        for (Resource resource : resources.values()) {
            if (resource.mbean == null) {
                doBind(resource);
            }
        }
    }

    public void bindResources() {
        doBindResources();

    }

    protected void doUnbindResources() {
        for (Resource resource : resources.values()) {
            if (resource.mbean == null) {
                doUnbind(resource);
            }
        }
    }

    public void unbindResources() {
        doUnbindResources();
    }

    @Override
    public void activate(ComponentContext context) {
        try {
            ModelMBean mbean = new RequiredModelMBean();
            mbean.setManagedResource(new ManagementAdapter(), "ObjectReference");
            mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(ManagementAdapter.class));
            mbeanServer.registerMBean(mbean,
                    ObjectNameFactory.getObjectName(NAME.getName()));
        } catch (Exception cause) {
            throw new ManagementRuntimeException(
                    "Cannot bind service as a mbean", cause);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        Iterator<Entry<ResourceDescriptor, Resource>> iterator = resources.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<ResourceDescriptor, Resource> entry = iterator.next();
            iterator.remove();
            Resource resource = entry.getValue();
            if (resource.mbean != null) {
                doUnbind(entry.getValue());
            }
        }
        try {
            mbeanServer.unregisterMBean(ObjectNameFactory.getObjectName(NAME.getName()));
        } catch (Exception e) {
            throw new ManagementRuntimeException(
                    "Cannot unbind management service from mbean server");
        }
    }
}
