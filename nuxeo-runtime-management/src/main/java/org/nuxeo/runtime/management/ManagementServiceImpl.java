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
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsesoft.mmbi.NamedModelMBean;
import org.nuxeo.runtime.api.Framework;
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
            doRegisterResource((ResourceDescriptor) contribution);
        } else if (extensionPoint.equals("factories")) {
            doRegisterFactory((ResourceFactoryDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("resources")) {
            doUnregisterResource((ResourceDescriptor) contribution);
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
        for (ResourceDescriptor resourceDescriptor : factory.getDescriptors()) {
            doRegisterResource(resourceDescriptor);
        }
        factories.put(descriptor, factory);
    }

    protected void doUnregisterFactory(ResourceFactoryDescriptor descriptor) {
        factories.remove(descriptor);
    }

    protected final Map<ResourceDescriptor, Resource> resources = new HashMap<ResourceDescriptor, Resource>();

    protected void doRegisterResource(ResourceDescriptor descriptor) {
        Resource resource = doResolve(descriptor);
        doBind(resource);
        resources.put(descriptor, resource);
        if (log.isInfoEnabled()) {
            log.info("registered management contribution for " + descriptor);
        }
    }

    protected void doUnregisterResource(ResourceDescriptor descriptor) {
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

    public void bind(ObjectName name, Object instance, Class<?> clazz) {
        doBind(name, instance, clazz);
    }

    protected NamedModelMBean doBind(ObjectName objectName,
            Object objectInstance, Class<?> objectClass) {
        NamedModelMBean mbean = null;
        try {
            mbean = new NamedModelMBean();
            mbean.setManagedResource(objectInstance, "ObjectReference");
            mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(objectInstance.getClass()));
            mbean.setInstance(mbeanServer.registerMBean(mbean, objectName));
        } catch (Exception e) {
            throw new ManagementRuntimeException("Cannot register bean "
                    + objectName);
        }
        return mbean;
    }

    protected void doBind(Resource resource) {
        if (resource.mbean != null) {
            throw new IllegalStateException(resource.getDescriptor()
                    + " is already bound");
        }
        resource.mbean = doBind(resource.getName(), resource.getInstance(),
                resource.getClazz());
    }

    protected void doUnbind(ObjectName objectName, Object objectInstance) {
        try {
            mbeanServer.unregisterMBean(objectName);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot unregister "
                    + objectName, e);
        }
    }

    protected void doUnbind(Resource resource) {
        if (resource.mbean == null) {
            throw new IllegalStateException(resource.getDescriptor()
                    + " is not bound");
        }
        try {
            mbeanServer.unregisterMBean(resource.mbean.getName());
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot unregister "
                    + resource.getDescriptor(), e);
        } finally {
            resource.mbean = null;
        }
    }

    public class ManagementAdapter implements ManagementServiceMBean {

        public Set<ObjectName> getResourcesName() {
            return ManagementServiceImpl.this.getResourcesName();
        }

        private boolean isEnabled = true;

        public void enable() {
            if (isEnabled) {
                throw new IllegalStateException("already enabled");
            }
            doEnable();
            isEnabled = true;
        }

        public void disable() {
            if (!isEnabled) {
                throw new IllegalStateException("already disabled");
            }
            doDisable();
            isEnabled = false;
        }

        public boolean isEnabled() {
            return resources != null;
        }
    }

    protected void doEnable() {
        for (Resource resource : resources.values()) {
            doBind(resource);
        }
    }

    protected void doDisable() {
        for (Resource resource : resources.values()) {
            doUnbind(resource);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        try {
            ModelMBeanInfo mbeanInfo = mbeanInfoFactory.getModelMBeanInfo(ManagementAdapter.class);
            RequiredModelMBean mbean = new RequiredModelMBean(mbeanInfo);
            mbean.setManagedResource(new ManagementAdapter(), "ObjectReference");
            mbeanServer.registerMBean(mbean, new ObjectName(
                    "nx:type=resource,name=management"));
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot enable management", e);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            mbeanServer.unregisterMBean(new ObjectName(
                    "nx:type=resource,name=management"));
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot disable management",
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<ObjectName> getResourcesName() {
        String qualifiedName = ObjectNameFactory.formatTypeQuery("service");
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        return mbeanServer.queryNames(objectName, null);
    }

    @SuppressWarnings("unchecked")
    public ObjectName getObjectName(String name) throws ManagementException {
        Set<ObjectName> names;
        try {
            names = mbeanServer.queryNames(new ObjectName(name), null);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot get " + name, e);
        }
        if (names == null || names.size() == 0) {
            throw new ManagementException(name + " not found");
        }
        if (names.size() > 1) {
            throw new ManagementRuntimeException(
                    "identified more than one instance for " + name);
        }
        return names.iterator().next();
    }

    public MBeanInfo getObjectInfo(ObjectName objectName) {
        try {
            return mbeanServer.getMBeanInfo(objectName);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot get " + objectName
                    + "'s bean info", e);
        }
    }

    public Object getObjectAttribute(ObjectName objectName,
            MBeanAttributeInfo info) {
        try {
            return mbeanServer.getAttribute(objectName, info.getName());
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot get "
                    + info.getName() + " from " + objectName, e);
        }
    }
}
