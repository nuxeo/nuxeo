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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
            doRegisterResource((ResourceDescriptor) contribution);
        } else if (extensionPoint.equals("factories")) {
            doRegisterFactory((ResourceFactoryDescriptor) contribution);
        } else if (extensionPoint.equals("shortcuts")) {
            doRegisterShortcut((ShortcutDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("resources")) {
            doUnregisterResource((ResourceDescriptor) contribution);
        } else if (extensionPoint.equals("factories")) {
            doUnregisterFactory((ResourceFactoryDescriptor) contribution);
        } else if (extensionPoint.equals("shortcuts")) {
            doUnregisterShortcut((ShortcutDescriptor) contribution);
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

    protected final Map<String, ObjectName> shortcuts = new TreeMap<String, ObjectName>();

    protected void doRegisterShortcut(ShortcutDescriptor descriptor) {
        doRegisterShortcut(descriptor.getShortName(),
                descriptor.getQualifiedName());
    }

    protected void doRegisterShortcut(String shortName, String qualifiedName) {
        shortcuts.put(shortName, ObjectNameFactory.getObjectName(qualifiedName));
    }
    
    public void registerShortcut(String name, String qualifiedName) {
        doRegisterShortcut(name, qualifiedName);
    }

    protected void doUnregisterShortcut(ShortcutDescriptor descriptor) {
        doUnregisterShortcut(descriptor.getShortName());
    }
    
    protected void doUnregisterShortcut(String name) {
        shortcuts.remove(name);
    }
    
    public void unregisterShortcut(String name) {
        doUnregisterShortcut(name);
    }

    protected final Map<ObjectName, Resource> resources = new HashMap<ObjectName, Resource>();

    protected void doRegisterResource(String qualifiedName, Class<?> info,
            Object instance) {
        Resource resource = new Resource(
                ObjectNameFactory.getObjectName(qualifiedName), info, instance);
        doRegisterResource(resource);
    }

    protected void doRegisterResource(ResourceDescriptor descriptor) {
        Resource resource = doResolveResource(descriptor);
        doRegisterResource(resource);
    }

    protected void doRegisterResource(Resource resource) {
        resources.put(resource.getManagementName(), resource);
        doBind(resource);
        if (log.isDebugEnabled()) {
            log.debug("registered " + resource.getManagementName());
        }
    }
    
    protected ObjectName doResolveResourceName(ResourceDescriptor descriptor) {
        String qualifiedName = descriptor.getName();
        if (qualifiedName == null) {
            qualifiedName = ObjectNameFactory.getQualifiedName(descriptor.getClassName());
        }
        return ObjectNameFactory.getObjectName(qualifiedName);
    }
    
    protected Resource doResolveResource(ResourceDescriptor descriptor) {
        Class<?> resourceClass = doResolveResourceClass(descriptor.getClassName());
        Object resourceInstance = doResolveResourceInstance(resourceClass,
                descriptor);
        ObjectName managementName = doResolveResourceName(descriptor);
        String ifaceClassName = descriptor.getIfaceClassName();
        Class<?> managementClass = ifaceClassName != null ? doResolveResourceClass(ifaceClassName)
                : resourceClass;
        return new Resource(managementName, managementClass, resourceInstance);
    }

    protected Class<?> doResolveResourceClass(String className) {
        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw ManagementRuntimeException.wrap(e);
        }
    }

    protected <T> T doResolveResourceInstance(Class<T> resourceClass,
            ResourceDescriptor descriptor) {
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

    protected void doUnregisterResource(ResourceDescriptor descriptor) {
        ObjectName objectName = doResolveResourceName(descriptor);
        Resource resource = resources.remove(objectName);
        if (resource == null) {
            throw new IllegalArgumentException(descriptor
                    + " is not registered");
        }
        if (resource.isRegistered()) {
            doUnbind(resource);
        }
    }

    protected final ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    public void registerResource(String shortName, String qualifiedName,
            Class<?> managementClass, Object instance) {
        doRegisterResource(qualifiedName, managementClass, instance);
        doRegisterShortcut(shortName, qualifiedName);
    }

    public void unregisterResource(String name, String qualifiedName) {
        doUnregisterResource(qualifiedName);
        doUnregisterShortcut(name);
    }

    protected void doUnregisterResource(String qualifiedName) {
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        Resource resource = resources.remove(objectName);
        if (resource == null) {
            throw new ManagementRuntimeException(qualifiedName
                    + " is not registered");
        }
        if (resource.mbean != null) {
            doUnbind(resource);
        }
    }

    protected void doBind(Resource resource) {
        if (resource.mbean != null) {
            throw new IllegalStateException(resource
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
                log.debug("bound " + resource);
            }
        } catch (Exception e) {
            log.error("Cannot bind " + resource, e);
        }
        resource.mbean = mbean;
    }

    protected void doUnbind(Resource resource) {
        if (resource.mbean == null) {
            throw new IllegalStateException(resource
                    + " is not bound");
        }
        try {
            mbeanServer.unregisterMBean(resource.mbean.getName());
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot unbind "
                    + resource, e);
        } finally {
            resource.mbean = null;
            if (log.isDebugEnabled()) {
                log.debug("unbound " + resource);
            }
        }

    }

    public Set<String> getShortcutsName() {
        return shortcuts.keySet();
    }

    public Set<ObjectName> getResourcesName() {
        return resources.keySet();
    }

    public ObjectName lookupName(String name) {
        if (!shortcuts.containsKey(name))
            return ObjectNameFactory.getObjectName(name);
        return shortcuts.get(name);
    }

    public class ManagementAdapter implements ManagementServiceMBean {

        public void bindResources() {
            doBindResources();
        }

        public void unbindResources() {
            doUnbindResources();
        }

        public Set<ObjectName> getResourcesName() {
            return resources.keySet();
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
        Iterator<Entry<ObjectName, Resource>> iterator = resources.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<ObjectName, Resource> entry = iterator.next();
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

    public void bindResource(ObjectName name) {
        Resource resource = resources.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        doBind(resource);
    }

    public void unbindResource(ObjectName name) {
        Resource resource = resources.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        doUnbind(resource);
    }

}
