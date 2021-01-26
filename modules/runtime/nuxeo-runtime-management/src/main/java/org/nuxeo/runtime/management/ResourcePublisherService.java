/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *      Stephane Lacoin (Nuxeo EP Software Engineer)
 *      Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.management;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class ResourcePublisherService extends DefaultComponent implements ResourcePublisher, ResourcePublisherMBean {

    private static final Logger log = LogManager.getLogger(ResourcePublisherService.class);

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.management.ResourcePublisher");

    public static final String SERVICES_EXT_KEY = "services";

    public static final String FACTORIES_EXT_KEY = "factories";

    public static final String SHORTCUTS_EXT_KEY = "shortcuts";

    protected ShortcutsRegistry shortcutsRegistry;

    protected FactoriesRegistry factoriesRegistry;

    protected ResourcesRegistry resourcesRegistry;

    protected ServerLocatorService serverLocatorService;

    protected boolean started = false;

    @Override
    public void activate(ComponentContext context) {
        serverLocatorService = (ServerLocatorService) Framework.getService(ServerLocator.class);
    }

    @Override
    public void deactivate(ComponentContext context) {
        serverLocatorService = null;
    }

    @Override
    public int getApplicationStartedOrder() {
        return serverLocatorService.getApplicationStartedOrder() + 1;
    }

    @Override
    public void start(ComponentContext context) {
        shortcutsRegistry = new ShortcutsRegistry();
        factoriesRegistry = new FactoriesRegistry();
        resourcesRegistry = new ResourcesRegistry();

        this.<ServiceDescriptor> getRegistryContributions(SERVICES_EXT_KEY)
            .forEach(resourcesRegistry::doRegisterResource);
        this.<ResourceFactoryDescriptor> getRegistryContributions(FACTORIES_EXT_KEY)
            .forEach(factoriesRegistry::doRegisterFactory);
        this.<ShortcutDescriptor> getRegistryContributions(SHORTCUTS_EXT_KEY)
            .forEach(shortcutsRegistry::doRegisterShortcut);

        started = true;
        factoriesRegistry.doRegisterResources();
        doBindResources();
    }

    @Override
    public void stop(ComponentContext context) {
        started = false;
        doUnbindResources();

        shortcutsRegistry = null;
        factoriesRegistry = null;
        resourcesRegistry = null;
    }

    protected class FactoriesRegistry {

        protected final Map<Class<? extends ResourceFactory>, ResourceFactory> registry = new HashMap<>();

        protected void doRegisterFactory(ResourceFactoryDescriptor descriptor) {
            ResourceFactory factory;
            Class<? extends ResourceFactory> factoryClass = descriptor.getFactoryClass();
            try {
                factory = factoryClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new ManagementRuntimeException("Cannot create factory " + factoryClass, e);
            }
            factory.configure(ResourcePublisherService.this, descriptor);
            registry.put(factoryClass, factory);
        }

        protected void doRegisterResources() {
            registry.values().forEach(ResourceFactory::registerResources);
        }
    }

    protected class ShortcutsRegistry {
        protected final Map<String, ObjectName> registry = new TreeMap<>();

        protected void doRegisterShortcut(ShortcutDescriptor descriptor) {
            doRegisterShortcut(descriptor.getShortName(), descriptor.getQualifiedName());
        }

        protected void doRegisterShortcut(String shortName, String qualifiedName) {
            registry.put(shortName, ObjectNameFactory.getObjectName(qualifiedName));
        }

        protected void doRegisterShortcut(String shortName, ObjectName qualifiedName) {
            registry.put(shortName, qualifiedName);
        }

        protected void unregisterShortcut(String name) {
            registry.remove(name);
        }
    }

    protected class ResourcesRegistry {

        protected final Map<ObjectName, Resource> registry = new HashMap<>();

        protected void doRegisterResource(String qualifiedName, Class<?> info, Object instance) {
            Resource resource = new Resource(ObjectNameFactory.getObjectName(qualifiedName), info, instance);
            doRegisterResource(resource);
        }

        protected void doRegisterResource(ServiceDescriptor descriptor) {
            Resource resource = doResolveServiceDescriptor(descriptor);
            doRegisterResource(resource);
            String shortName = descriptor.getName();
            if (StringUtils.isNotEmpty(shortName)) {
                shortcutsRegistry.doRegisterShortcut(shortName, resource.getManagementName());
            }
        }

        protected final ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

        protected RequiredModelMBean doBind(MBeanServer server, ObjectName name, Object instance, Class<?> clazz)
                throws JMException, InvalidTargetObjectTypeException {
            RequiredModelMBean mbean = new RequiredModelMBean();
            mbean.setManagedResource(instance, "ObjectReference");
            mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(clazz));
            server.registerMBean(mbean, name);
            return mbean;
        }

        protected void doBind(Resource resource) {
            if (!started) {
                return;
            }
            if (resource.mbean != null) {
                throw new IllegalStateException(resource + " is already bound");
            }
            MBeanServer server = serverLocatorService.lookupServer(resource.managementName.getDomain());
            try {
                resource.mbean = doBind(server, resource.managementName, resource.instance, resource.clazz);
                if (ResourcePublisherService.log.isDebugEnabled()) {
                    ResourcePublisherService.log.debug("bound {}", resource);
                }
            } catch (JMException | InvalidTargetObjectTypeException e) {
                ResourcePublisherService.log.error("Cannot bind {}", resource, e);
            }
        }

        protected void doUnbind(Resource resource) {
            if (resource.mbean == null) {
                throw new IllegalStateException(resource.managementName + " is not bound");
            }
            try {
                MBeanServer server = serverLocatorService.lookupServer(resource.managementName);
                server.unregisterMBean(resource.managementName);
            } catch (JMException e) {
                throw ManagementRuntimeException.wrap("Cannot unbind " + resource, e);
            } finally {
                resource.mbean = null;
                if (ResourcePublisherService.log.isDebugEnabled()) {
                    ResourcePublisherService.log.debug("unbound {}", resource);
                }
            }
        }

        protected void doRegisterResource(Resource resource) {
            final ObjectName name = resource.getManagementName();
            if (registry.containsKey(name)) {
                return;
            }
            registry.put(name, resource);
            doBind(resource);
            if (log.isDebugEnabled()) {
                log.debug("registered {}", name);
            }
        }

        protected ObjectName doResolveServiceName(ServiceDescriptor descriptor) {
            String qualifiedName = descriptor.getName();
            if (qualifiedName == null) {
                qualifiedName = ObjectNameFactory.getQualifiedName(descriptor.getResourceClass().getCanonicalName());
            }
            return ObjectNameFactory.getObjectName(qualifiedName);
        }

        protected Resource doResolveServiceDescriptor(ServiceDescriptor descriptor) {
            Class<?> resourceClass = descriptor.getResourceClass();
            Object resourceInstance = doResolveService(resourceClass);
            ObjectName managementName = doResolveServiceName(descriptor);
            Class<?> ifaceClass = descriptor.getIfaceClass();
            Class<?> managementClass = ifaceClass != null ? ifaceClass : resourceClass;
            return new Resource(managementName, managementClass, resourceInstance);
        }

        protected <T> T doResolveService(Class<T> resourceClass) {
            T service = Framework.getService(resourceClass);
            if (service == null) {
                throw new ManagementRuntimeException("Cannot locate resource using " + resourceClass);
            }
            return service;
        }

        protected void doUnregisterResource(String qualifiedName) {
            ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
            doUnregisterResource(objectName);
        }

        protected void doUnregisterResource(ObjectName objectName) {
            Resource resource = registry.remove(objectName);
            if (resource == null) {
                throw new IllegalArgumentException(objectName + " is not registered");
            }
            if (resource.mbean != null) {
                doUnbind(resource);
            }
        }

    }

    @Override
    public void registerResource(String shortName, String qualifiedName, Class<?> managementClass, Object instance) {
        resourcesRegistry.doRegisterResource(qualifiedName, managementClass, instance);
        if (shortName != null) {
            shortcutsRegistry.doRegisterShortcut(shortName, qualifiedName);
        }
    }

    @Override
    public void unregisterResource(String shortName, String qualifiedName) {
        resourcesRegistry.doUnregisterResource(qualifiedName);
        if (shortName != null) {
            shortcutsRegistry.unregisterShortcut(shortName);
        }
    }

    @Override
    public Set<String> getShortcutsName() {
        return new HashSet<>(shortcutsRegistry.registry.keySet());
    }

    @Override
    public Set<ObjectName> getResourcesName() {
        return new HashSet<>(resourcesRegistry.registry.keySet());
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
            if (resource.mbean != null) {
                resourcesRegistry.doUnbind(resource);
            }
        }
    }

    @Override
    public void unbindResources() {
        doUnbindResources();
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

    protected void bindForTest(MBeanServer server, ObjectName name, Object instance, Class<?> clazz)
            throws JMException, InvalidTargetObjectTypeException {
        resourcesRegistry.doBind(server, name, instance, clazz);
    }

}
