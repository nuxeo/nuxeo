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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class ResourcePublisherService extends DefaultComponent implements
        ResourcePublisher, ResourcePublisherMBean {

    public static final String SERVICES_EXT_KEY = "services";

    public static final String FACTORIES_EXT_KEY = "factories";

    public static final String SHORTCUTS_EXT_KEY = "shortcuts";

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.management.ResourcePublisher");

    private static final Log log = LogFactory.getLog(ResourcePublisherService.class);

    protected final ShortcutsRegistry shortcutsRegistry = new ShortcutsRegistry();

    protected final FactoriesRegistry factoriesRegistry = new FactoriesRegistry();

    protected final ResourcesRegistry resourcesRegistry = new ResourcesRegistry();

    protected ServerLocatorService serverLocatorService;

    public ResourcePublisherService() {
        super(); // enables breaking
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(SERVICES_EXT_KEY)) {
            resourcesRegistry.doRegisterResource((ServiceDescriptor) contribution);
        } else if (extensionPoint.equals(FACTORIES_EXT_KEY)) {
            factoriesRegistry.doRegisterFactory((ResourceFactoryDescriptor) contribution);
        } else if (extensionPoint.equals(SHORTCUTS_EXT_KEY)) {
            shortcutsRegistry.doRegisterShortcut((ShortcutDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(SERVICES_EXT_KEY)) {
            resourcesRegistry.doUnregisterResource((ServiceDescriptor) contribution);
        } else if (extensionPoint.equals(FACTORIES_EXT_KEY)) {
            factoriesRegistry.doUnregisterFactory((ResourceFactoryDescriptor) contribution);
        } else if (extensionPoint.equals(SHORTCUTS_EXT_KEY)) {
            shortcutsRegistry.doUnregisterShortcut((ShortcutDescriptor) contribution);
        }
    }

    protected class FactoriesRegistry {

        protected final Map<Class<? extends ResourceFactory>, ResourceFactory> registry = new HashMap<Class<? extends ResourceFactory>, ResourceFactory>();

        protected void doRegisterFactory(ResourceFactoryDescriptor descriptor) {
            ResourceFactory factory;
            Class<? extends ResourceFactory> factoryClass = descriptor.getFactoryClass();
            try {
                factory = factoryClass.newInstance();
            } catch (Exception e) {
                throw new ManagementRuntimeException("Cannot create factory "
                        + factoryClass, e);
            }
            factory.configure(ResourcePublisherService.this, descriptor);
            registry.put(factoryClass, factory);
        }

        protected void doUnregisterFactory(ResourceFactoryDescriptor descriptor) {
            registry.remove(descriptor.getFactoryClass());
        }

        protected void doRegisterResources() {
            for (ResourceFactory factory : registry.values()) {
                factory.registerResources();
            }
        }
    }

    protected class ShortcutsRegistry {
        protected final Map<String, ObjectName> registry = new TreeMap<String, ObjectName>();

        protected void doRegisterShortcut(ShortcutDescriptor descriptor) {
            doRegisterShortcut(descriptor.getShortName(),
                    descriptor.getQualifiedName());
        }

        protected void doRegisterShortcut(String shortName, String qualifiedName) {
            registry.put(shortName,
                    ObjectNameFactory.getObjectName(qualifiedName));
        }

        protected void doRegisterShortcut(String shortName,
                ObjectName qualifiedName) {
            registry.put(shortName, qualifiedName);
        }

        protected void doUnregisterShortcut(ShortcutDescriptor descriptor) {
            doUnregisterShortcut(descriptor.getShortName());
        }

        protected void doUnregisterShortcut(String name) {
            registry.remove(name);
        }

        public void unregisterShortcut(String name) {
            doUnregisterShortcut(name);
        }
    }

    protected class ResourcesRegistry {

        protected final Map<ObjectName, Resource> registry = new HashMap<ObjectName, Resource>();

        protected void doRegisterResource(String qualifiedName, Class<?> info,
                Object instance) {
            Resource resource = new Resource(
                    ObjectNameFactory.getObjectName(qualifiedName), info,
                    instance);
            doRegisterResource(resource);
        }

        protected void doRegisterResource(ServiceDescriptor descriptor) {
            Resource resource = doResolveServiceDescriptor(descriptor);
            doRegisterResource(resource);
            String shortName = descriptor.getName();
            if (!StringUtils.isEmpty(shortName)) {
                shortcutsRegistry.doRegisterShortcut(shortName,
                        resource.getManagementName());
            }
        }

        protected final ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

        protected RequiredModelMBean doBind(MBeanServer server,
                ObjectName name, Object instance, Class<?> clazz)
                throws Exception {
            RequiredModelMBean mbean = new RequiredModelMBean();
            mbean.setManagedResource(instance, "ObjectReference");
            mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(clazz));
            server.registerMBean(mbean, name);
            return mbean;
        }

        protected void doBind(Resource resource) {
            if (resource.mbean != null) {
                throw new IllegalStateException(resource + " is already bound");
            }
            MBeanServer server = serverLocatorService.lookupServer(resource.managementName.getDomain());
            try {
                resource.mbean = doBind(server, resource.managementName,
                        resource.instance, resource.clazz);
                if (ResourcePublisherService.log.isDebugEnabled()) {
                    ResourcePublisherService.log.debug("bound " + resource);
                }
            } catch (Exception e) {
                ResourcePublisherService.log.error("Cannot bind " + resource, e);
            }
        }

        protected void doUnbind(Resource resource) {
            if (resource.mbean == null) {
                throw new IllegalStateException(resource + " is not bound");
            }
            try {
                MBeanServer server = serverLocatorService.lookupServer(resource.managementName);
                server.unregisterMBean(resource.managementName);
            } catch (Exception e) {
                throw ManagementRuntimeException.wrap("Cannot unbind "
                        + resource, e);
            } finally {
                resource.mbean = null;
                if (ResourcePublisherService.log.isDebugEnabled()) {
                    ResourcePublisherService.log.debug("unbound " + resource);
                }
            }
        }

        protected void doRegisterResource(Resource resource) {
            registry.put(resource.getManagementName(), resource);
            if (log.isDebugEnabled()) {
                log.debug("registered " + resource.getManagementName());
            }
        }

        protected ObjectName doResolveServiceName(ServiceDescriptor descriptor) {
            String qualifiedName = descriptor.getName();
            if (qualifiedName == null) {
                qualifiedName = ObjectNameFactory.getQualifiedName(descriptor.getResourceClass().getCanonicalName());
            }
            return ObjectNameFactory.getObjectName(qualifiedName);
        }

        protected Resource doResolveServiceDescriptor(
                ServiceDescriptor descriptor) {
            Class<?> resourceClass = descriptor.getResourceClass();
            Object resourceInstance = doResolveService(resourceClass,
                    descriptor);
            ObjectName managementName = doResolveServiceName(descriptor);
            Class<?> ifaceClass = descriptor.getIfaceClass();
            Class<?> managementClass = ifaceClass != null ? ifaceClass
                    : resourceClass;
            return new Resource(managementName, managementClass,
                    resourceInstance);
        }

        protected <T> T doResolveService(Class<T> resourceClass,
                ServiceDescriptor descriptor) {
            T service;
            try {
                service = Framework.getService(resourceClass);
            } catch (Exception e) {
                throw ManagementRuntimeException.wrap(
                        "Cannot locate resource using " + resourceClass, e);
            }
            if (service == null) {
                throw new ManagementRuntimeException(
                        "Cannot locate resource using " + resourceClass);
            }
            return service;
        }

        protected void doUnregisterResources() {
            Iterator<Entry<ObjectName, Resource>> iterator = registry.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<ObjectName, Resource> entry = iterator.next();
                iterator.remove();
                Resource resource = entry.getValue();
                if (resource.mbean != null) {
                    doUnbind(entry.getValue());
                }
            }
        }

        protected void doUnregisterResource(ServiceDescriptor descriptor) {
            ObjectName objectName = doResolveServiceName(descriptor);
            doUnregisterResource(objectName);
            String shortName = descriptor.getName();
            if (!StringUtils.isEmpty(shortName)) {
                shortcutsRegistry.unregisterShortcut(shortName);
            }
        }

        protected void doUnregisterResource(String qualifiedName) {
            ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
            doUnregisterResource(objectName);
        }

        protected void doUnregisterResource(ObjectName objectName) {
            Resource resource = registry.remove(objectName);
            if (resource == null) {
                throw new IllegalArgumentException(objectName
                        + " is not registered");
            }
            if (resource.mbean != null) {
                doUnbind(resource);
            }
        }

    }

    public void registerResource(String shortName, String qualifiedName,
            Class<?> managementClass, Object instance) {
        resourcesRegistry.doRegisterResource(qualifiedName, managementClass,
                instance);
        if (shortName != null) {
            shortcutsRegistry.doRegisterShortcut(shortName, qualifiedName);
        }
    }

    public void unregisterResource(String shortName, String qualifiedName) {
        resourcesRegistry.doUnregisterResource(qualifiedName);
        if (shortName != null) {
            shortcutsRegistry.doUnregisterShortcut(shortName);
        }
    }

    public void registerShortcut(String shortName, String qualifiedName) {
        shortcutsRegistry.doRegisterShortcut(shortName, qualifiedName);
    }

    public void unregisterShortcut(String shortName) {
        shortcutsRegistry.doUnregisterShortcut(shortName);
    }

    @Override
    public Set<String> getShortcutsName() {
        return new HashSet<String>(shortcutsRegistry.registry.keySet());
    }

    @Override
    public Set<ObjectName> getResourcesName() {
        return new HashSet<ObjectName>(resourcesRegistry.registry.keySet());
    }

    @Override
    public ObjectName lookupName(String name) {
        if (!shortcutsRegistry.registry.containsKey(name)) {
            return ObjectNameFactory.getObjectName(name);
        }
        return shortcutsRegistry.registry.get(name);
    }

    protected void doBindResources() {
        for (Resource resource : resourcesRegistry.registry.values()) {
            if (resource.mbean == null) {
                resourcesRegistry.doBind(resource);
            }
        }
    }

    @Override
    public void bindResources() {
        doBindResources();
    }

    protected void doUnbindResources() {
        for (Resource resource : resourcesRegistry.registry.values()) {
            if (resource.mbean == null) {
                resourcesRegistry.doUnbind(resource);
            }
        }
    }

    @Override
    public void unbindResources() {
        doUnbindResources();
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        factoriesRegistry.doRegisterResources();
        doBindResources();
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        serverLocatorService = (ServerLocatorService) Framework.getLocalService(ServerLocator.class);
    }

    @Override
    public void deactivate(ComponentContext context) {
        resourcesRegistry.doUnregisterResources();
    }

    public void bindResource(ObjectName name) {
        Resource resource = resourcesRegistry.registry.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        resourcesRegistry.doBind(resource);
    }

    public void unbindResource(ObjectName name) {
        Resource resource = resourcesRegistry.registry.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        resourcesRegistry.doUnbind(resource);
    }

    protected void bindForTest(MBeanServer server, ObjectName name,
            Object instance, Class<?> clazz) throws Exception {
        resourcesRegistry.doBind(server, name, instance, clazz);
    }

}
