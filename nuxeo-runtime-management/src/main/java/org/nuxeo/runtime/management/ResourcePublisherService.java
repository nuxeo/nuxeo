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
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.ObjectName;
import javax.management.modelmbean.RequiredModelMBean;

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
        ResourcePublisher {

    public static final String SERVICES_EXT_KEY = "services";

    public static final String FACTORIES_EXT_KEY = "factories";

    public static final String SHORTCUTS_EXT_KEY = "shortcuts";

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.management.ResourcePublisher");

    private static final Log log = LogFactory.getLog(ResourcePublisherService.class);

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
        if (extensionPoint.equals(FACTORIES_EXT_KEY)) {
            factoriesRegistry.doRegisterFactory((ResourceFactoryDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
       if (extensionPoint.equals(FACTORIES_EXT_KEY)) {
            factoriesRegistry.doUnregisterFactory((ResourceFactoryDescriptor) contribution);
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

    protected class ResourcesRegistry {

        protected void doRegisterResource(String qualifiedName, Class<?> info,
                Object instance) {
            doBind(qualifiedName, info, instance);
        }

        protected final ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

        protected void doBind(String name, Class<?> info, Object instance) {
            try {
                ObjectName mname = new ObjectName(name);
                MBeanServer server = serverLocatorService.lookupServer(mname.getDomain());
                if (info.getAnnotation(MXBean.class) != null) {
                    server.registerMBean(instance, mname);
                } else {
                    RequiredModelMBean mbean = new RequiredModelMBean();
                    mbean.setManagedResource(instance, "ObjectReference");
                    mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(info));
                    server.registerMBean(mbean, mname);
                }
                if (ResourcePublisherService.log.isDebugEnabled()) {
                    ResourcePublisherService.log.debug("bound " + name);
                }
            } catch (Exception e) {
                ResourcePublisherService.log.error("Cannot bind " + name, e);
            }
        }

        protected void doUnbind(ObjectName name) {
            try {
                MBeanServer server = serverLocatorService.lookupServer(name);
                server.unregisterMBean(name);
            } catch (Exception e) {
                throw ManagementRuntimeException.wrap("Cannot unbind " + name,
                        e);
            } finally {
                if (ResourcePublisherService.log.isDebugEnabled()) {
                    ResourcePublisherService.log.debug("unbound " + name);
                }
            }
        }

        protected void doUnregisterResources() {
            MBeanServer server = serverLocatorService.lookupServer("org.nuxeo");
            for (ObjectName name : server.queryNames(null, null)) {
                doUnbind(name);
            }
        }


        protected void doUnregisterResource(String qualifiedName) {
            ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
            doUnregisterResource(objectName);
        }

        protected void doUnregisterResource(ObjectName objectName) {
            doUnbind(objectName);
        }

    }

    @Override
    public void registerResource(String shortName, String qualifiedName,
            Class<?> info, Object instance) {
        registerResource(qualifiedName, info, instance);
    }

    @Override
    public void registerResource(String name, Class<?> info, Object instance) {
        resourcesRegistry.doRegisterResource(name, info, instance);
    }

    @Override
    public void unregisterResource(String shortName, String qualifiedName) {
        unregisterResource(qualifiedName);
    }

    @Override
    public void unregisterResource(String qualifiedName) {
        resourcesRegistry.doUnregisterResource(qualifiedName);
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        factoriesRegistry.doRegisterResources();
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        serverLocatorService = (ServerLocatorService) Framework.getLocalService(ServerLocator.class);
    }

    @Override
    public void deactivate(ComponentContext context) {
        resourcesRegistry.doUnregisterResources();
    }

}
