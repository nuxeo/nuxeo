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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
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
            doRegisterResource((ResourceDescriptor) contribution, null);
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

    protected final Map<String, ResourceFactory> factories = new HashMap<String, ResourceFactory>();

    @SuppressWarnings("unchecked")
    protected void doRegisterFactory(ResourceFactoryDescriptor descriptor) {
        String factoryClassName = descriptor.getClassName();
        ResourceFactory factory = null;

        Class<ResourceFactory> factoryClass;
        try {
            factoryClass = (Class<ResourceFactory>) getClass().getClassLoader().loadClass(
                    descriptor.getClassName());
        } catch (Exception e) {
            throw new ManagementRuntimeException("Cannot load factory "
                    + factoryClassName, e);
        }
        try {
            factory = factoryClass.getConstructor(
                    new Class[] { ManagementServiceImpl.class,
                            ResourceFactoryDescriptor.class, }).newInstance(
                    this, descriptor);
        } catch (Exception e) {
            throw new ManagementRuntimeException("Cannot create factory "
                    + factoryClassName, e);
        }
        factory.register();
        factories.put(descriptor.getClassName(), factory);
    }

    protected void doUnregisterFactory(ResourceFactoryDescriptor descriptor) {
        factories.remove(descriptor);
    }

    protected final Map<String, Resource> resources = new HashMap<String, Resource>();

    protected void doRegisterResource(String shortName, String qualifiedName,
            Class<?> info, Object instance) {
        ResourceDescriptor descriptor = new ResourceDescriptor(shortName,
                qualifiedName, info);
        doRegisterResource(descriptor, instance);
    }

    protected void doRegisterResource(ResourceDescriptor descriptor,
            Object instance) {
        Resource resource = doResolveResource(descriptor, instance);
        doRegisterResource(resource);
        doBind(resource);
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

    protected Resource doResolveResource(ResourceDescriptor descriptor,
            Object resourceInstance) {
        Class<?> resourceClass = doLoadClass(descriptor.getClassName());
        if (resourceInstance == null) {
            resourceInstance = doGetResourceInstance(resourceClass, descriptor);
        }
        String qualifiedName = descriptor.getQualifiedName();
        if (qualifiedName == null) {
            qualifiedName = ObjectNameFactory.getQualifiedName(resourceClass.getName());
        }
        ObjectName managementName = ObjectNameFactory.getObjectName(qualifiedName);
        String shortName = descriptor.getShortName();
        if (shortName == null) {
            shortName = ObjectNameFactory.formatShortName(managementName);
        }
        String ifaceClassName = descriptor.getIfaceClassName();
        Class<?> managementClass = ifaceClassName != null ? doLoadClass(ifaceClassName)
                : resourceClass;
        return new Resource(descriptor, shortName, managementName,
                managementClass, resourceInstance);
    }

    protected final ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    public void registerResource(String shortName, String qualifiedName,
            Class<?> managementClass, Object instance) {
        doRegisterResource(shortName, qualifiedName, managementClass, instance);
    }

    protected void doRegisterResource(Resource resource) {
        resources.put(resource.shortName, resource);
        if (log.isDebugEnabled()) {
            log.debug("registered " + resource.descriptor.getQualifiedName());
        }
    }

    public void unregisterResource(String name) {
        if (ObjectNameFactory.isQualified(name)) {
            name = ObjectNameFactory.formatShortName(name);
        }
        doUnregisterResource(name);
    }

    protected void doUnregisterResource(String shortName) {
        Resource resource = resources.remove(shortName);
        if (resource == null) {
            throw new ManagementRuntimeException(shortName
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
                    resource.getManagementName()));
            if (log.isDebugEnabled()) {
                log.debug("bound " + resource.getShortName());
            }
        } catch (Exception e) {
            log.error("Cannot bind " + resource.getDescriptor(), e);
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
                log.debug("unbound " + resource.getShortName());
            }
        }

    }

    public Set<String> getShortNames() {
        Set<String> names = new HashSet<String>();
        for (Resource resource:resources.values()) {
            String name = resource.getDescriptor().getShortName();
            if (resource.getDescriptor().getShortName() == null) continue;
            names.add(name);
        }
        return names;
    }
    
    
    public Set<String> getQualifiedNames() {
        Set<String> names = new HashSet<String>();
        for (Resource resource:resources.values()) {
            names.add(resource.descriptor.getQualifiedName());
        }
        return names;
    }

    public ObjectName lookupName(String name) {
        if (!resources.containsKey(name))
            return ObjectNameFactory.getObjectName(name);
        return resources.get(name).getManagementName();
    }

    public class ManagementAdapter implements ManagementServiceMBean {

        public void bindResources() {
            doBindResources();
        }

        public void unbindResources() {
            doUnbindResources();
        }

        public Set<ObjectName> getResourcesName() {
            Set<ObjectName> names = new HashSet<ObjectName>();
            for (Resource resource : resources.values()) {
                names.add(resource.getManagementName());
            }
            return names;
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
        Iterator<Entry<String, Resource>> iterator = resources.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Resource> entry = iterator.next();
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

    public void bindResource(String name) {
        if (ObjectNameFactory.isQualified(name)) {
            name = ObjectNameFactory.formatShortName(name);
        }
        Resource resource = resources.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        doBind(resource);
    }

    public void unbindResource(String name) {
        Resource resource = resources.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        doUnbind(resource);
    }

}
