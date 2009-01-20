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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.commons.lang.StringUtils;
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
public class ResourcePublisherService extends DefaultComponent implements
        ResourcePublisher {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.management.ResourcePublisher");

    public ResourcePublisherService() {
        super(); // enables breaking
    }

    private static final Log log = LogFactory.getLog(ResourcePublisherService.class);

    public static final String SERVICES_EXT_KEY = "services";

    public static final String FACTORIES_EXT_KEY = "factories";

    public static final String SHORTCUTS_EXT_KEY = "shortcuts";

    public static final String SERVER_LOCATORS_EXT_KEY = "locators";

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
        } else if (extensionPoint.equals(SERVER_LOCATORS_EXT_KEY)) {
            serverLocatorsRegistry.doRegisterServerLocator((MBeanServerLocatorDescriptor) contribution);
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
            ResourceFactory factory = null;

            Class<? extends ResourceFactory> factoryClass = descriptor.getFactoryClass();

            try {
                factory = factoryClass.newInstance();
            } catch (Exception e) {
                throw new ManagementRuntimeException("Cannot create factory "
                        + factoryClass, e);
            }
            factory.configure(ResourcePublisherService.this, descriptor);
            factory.registerResources();
            registry.put(factoryClass, factory);
        }

        protected void doUnregisterFactory(ResourceFactoryDescriptor descriptor) {
            registry.remove(descriptor.getFactoryClass());
        }

    }

    protected final FactoriesRegistry factoriesRegistry = new FactoriesRegistry();

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

    protected ShortcutsRegistry shortcutsRegistry = new ShortcutsRegistry();

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

        protected void doRegisterResource(Resource resource) {
            registry.put(resource.getManagementName(), resource);
            serverLocatorsRegistry.doBind(resource);
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
                    serverLocatorsRegistry.doUnbind(entry.getValue());
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
                serverLocatorsRegistry.doUnbind(resource);
            }
        }

    }

    protected final ResourcesRegistry resourcesRegistry = new ResourcesRegistry();

    protected class ServerLocatorsRegistry {
        protected ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

        protected MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

        protected void doRegisterServerLocator(
                MBeanServerLocatorDescriptor descriptor) {
            mbeanServer = doLocateServer(descriptor);
        }

        @SuppressWarnings("unchecked")
        protected MBeanServer doLocateServer(
                MBeanServerLocatorDescriptor descriptor) {
            String domainName = descriptor.getDomainName();
            for (MBeanServer server : (List<MBeanServer>) MBeanServerFactory.findMBeanServer(null)) {
                if (server.getDefaultDomain().equals(domainName)) {
                    return mbeanServer = server;
                }
            }
            throw new ManagementRuntimeException(
                    "cannot locate mbean server containing domain "
                            + domainName);
        }

        protected void doBind(Resource resource) {
            if (resource.mbean != null) {
                throw new IllegalStateException(resource + " is already bound");
            }
            NamedModelMBean mbean = null;
            try {
                mbean = new NamedModelMBean();
                mbean.setManagedResource(resource.getInstance(),
                        "ObjectReference");
                mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(
                        resource.getClazz()));
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
                throw new IllegalStateException(resource + " is not bound");
            }
            if (resource.mbean.getInstance() == null) {
                return;
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
    }

    protected final ServerLocatorsRegistry serverLocatorsRegistry = new ServerLocatorsRegistry();

    public void registerResource(String shortName, String qualifiedName,
            Class<?> managementClass, Object instance) {
        resourcesRegistry.doRegisterResource(qualifiedName, managementClass,
                instance);
        if (shortName != null)
            shortcutsRegistry.doRegisterShortcut(shortName, qualifiedName);
    }

    public void unregisterResource(String shortName, String qualifiedName) {
        resourcesRegistry.doUnregisterResource(qualifiedName);
        if (shortName != null)
            shortcutsRegistry.doUnregisterShortcut(shortName);
    }

    public void registerShortcut(String shortName, String qualifiedName) {
        shortcutsRegistry.doRegisterShortcut(shortName, qualifiedName);
    }

    public void unregisterShortcut(String shortName) {
        shortcutsRegistry.doUnregisterShortcut(shortName);
    }

    public Set<String> getShortcutsName() {
        return shortcutsRegistry.registry.keySet();
    }

    public Set<ObjectName> getResourcesName() {
        return resourcesRegistry.registry.keySet();
    }

    public ObjectName lookupName(String name) {
        if (!shortcutsRegistry.registry.containsKey(name))
            return ObjectNameFactory.getObjectName(name);
        return shortcutsRegistry.registry.get(name);
    }

    public class ManagementAdapter implements ResourcePublisherMBean {

        public void bindResources() {
            doBindResources();
        }

        public void unbindResources() {
            doUnbindResources();
        }

        public Set<ObjectName> getResourcesName() {
            return resourcesRegistry.registry.keySet();
        }
    }

    protected void doBindResources() {
        for (Resource resource : resourcesRegistry.registry.values()) {
            if (resource.mbean == null) {
                serverLocatorsRegistry.doBind(resource);
            }
        }
    }

    public void bindResources() {
        doBindResources();
    }

    protected void doUnbindResources() {
        for (Resource resource : resourcesRegistry.registry.values()) {
            if (resource.mbean == null) {
                serverLocatorsRegistry.doUnbind(resource);
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
            mbean.setModelMBeanInfo(serverLocatorsRegistry.mbeanInfoFactory.getModelMBeanInfo(
                    ManagementAdapter.class));
            serverLocatorsRegistry.mbeanServer.registerMBean(mbean,
                    ObjectNameFactory.getObjectName(NAME.getName()));
        } catch (Exception cause) {
            throw new ManagementRuntimeException(
                    "Cannot bind service as a mbean", cause);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        resourcesRegistry.doUnregisterResources();
        try {
            serverLocatorsRegistry.mbeanServer.unregisterMBean(ObjectNameFactory.getObjectName(NAME.getName()));
        } catch (Exception e) {
            throw new ManagementRuntimeException(
                    "Cannot unbind management service from mbean server");
        }
    }

    public void bindResource(ObjectName name) {
        Resource resource = resourcesRegistry.registry.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        serverLocatorsRegistry.doBind(resource);
    }

    public void unbindResource(ObjectName name) {
        Resource resource = resourcesRegistry.registry.get(name);
        if (resource == null) {
            throw new IllegalArgumentException(name + " is not registered");
        }
        serverLocatorsRegistry.doUnbind(resource);
    }
}
