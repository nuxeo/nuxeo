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
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
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
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class ManagementServiceImpl extends DefaultComponent implements
        ManagementService {

    private static final Log log = LogFactory.getLog(ManagementServiceImpl.class);

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("managedServices")) {
            doRegister((ManagedServiceDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("managedServices")) {
            doUnregister((ManagedServiceDescriptor) contribution);
        }
    }

    protected void doRegister(ManagedServiceDescriptor descriptor) {
        ManagedService service = doResolve(descriptor);
        doBind(service);
        managedServices.put(descriptor, service);
        if (log.isInfoEnabled()) {
            log.info("registered management contribution for " + descriptor);
        }
    }

    protected void doUnregister(ManagedServiceDescriptor descriptor) {
        ManagedService managedService = managedServices.remove(descriptor);
        if (managedService == null) {
            throw new IllegalArgumentException(descriptor
                    + " is not registered");
        }
        if (managedService.isRegistered()) {
            doUnbind(managedService);
        }
    }

    protected Class<?> doLoadServiceClass(String className) {
        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw ManagementRuntimeException.wrap(e);
        }
    }

    protected Object doGetServiceInstance(Class<?> serviceClass,
            boolean isAdapted) {
        Object service;
        if (isAdapted) {
            try {
                return serviceClass.newInstance();
            } catch (Exception e) {
                throw ManagementRuntimeException.wrap(
                        "Cannot create adapter for " + serviceClass, e);
            }
        }
        try {
            service = Framework.getService(serviceClass);
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(
                    "Cannot locate service using " + serviceClass, e);
        }
        if (service == null) {
            throw new ManagementRuntimeException("Cannot locate service using "
                    + serviceClass);
        }
        return service;
    }

    protected final Map<ManagedServiceDescriptor, ManagedService> managedServices = new HashMap<ManagedServiceDescriptor, ManagedService>();

    protected ManagedService doResolve(ManagedServiceDescriptor descriptor) {
        Class<?> serviceClass = doLoadServiceClass(descriptor.getServiceClassName());
        Object serviceInstance = doGetServiceInstance(serviceClass,
                descriptor.isAdapted());
        String serviceName = descriptor.getServiceName();
        if (serviceName == null) {
            serviceName = serviceClass.getSimpleName();
        }
        if (!ManagementNameFormatter.isQualified(serviceName)) {
            serviceName = ManagementNameFormatter.formatName(serviceName);
        }
        ObjectName managementName = ManagementNameFormatter.getObjectName(serviceName);
        String ifaceClassName = descriptor.getIfaceClassName();
        Class<?> managementClass = ifaceClassName != null ? doLoadServiceClass(ifaceClassName)
                : serviceClass;
        return new ManagedService(descriptor, managementName, managementClass,
                serviceInstance);
    }

    protected final ModelMBeanInfoFactory mbeanInfoFactory = new ModelMBeanInfoFactory();

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    protected void doBind(ManagedService service) {
        if (service.mbean != null) {
            throw new IllegalStateException(service.getDescriptor()
                    + " is already bound");
        }
        try {
            NamedModelMBean mbean = new NamedModelMBean();
            mbean.setManagedResource(service.getServiceInstance(),
                    "ObjectReference");
            mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(service.getManagementClass()));
            mbean.setInstance(mbeanServer.registerMBean(mbean,
                    service.getManagementName()));
            service.mbean = mbean;
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(
                    "Cannot register management contribution for "
                            + service.getDescriptor(), e);
        }
    }

    protected void doUnbind(ManagedService service) {
        if (service.mbean == null) {
            throw new IllegalStateException(service.getDescriptor()
                    + " is not bound");
        }
        try {
            mbeanServer.unregisterMBean(service.mbean.getName());
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot unregister "
                    + service.getDescriptor(), e);
        } finally {
            service.mbean = null;
        }
    }

    public class ManagementAdapter implements ManagementServiceMBean {

        public Set<ObjectName> getRegisteredServicesName() {
            return ManagementServiceImpl.this.getServicesName();
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
            return managedServices != null;
        }
    }

    protected void doEnable() {
        for (ManagedService service : managedServices.values()) {
            doBind(service);
        }
    }

    protected void doDisable() {
        for (ManagedService service : managedServices.values()) {
            doUnbind(service);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        try {
            ModelMBeanInfo mbeanInfo = mbeanInfoFactory.getModelMBeanInfo(ManagementAdapter.class);
            RequiredModelMBean mbean = new RequiredModelMBean(mbeanInfo);
            mbean.setManagedResource(new ManagementAdapter(), "ObjectReference");
            mbeanServer.registerMBean(mbean, new ObjectName(
                    "nx:type=service,name=management"));
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot enable management", e);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            mbeanServer.unregisterMBean(new ObjectName(
                    "nx:type=service,name=management"));
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot disable management",
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<ObjectName> getServicesName() {
        String qualifiedName = ManagementNameFormatter.formatTypeQuery("service");
        ObjectName objectName = ManagementNameFormatter.getObjectName(qualifiedName);
        return mbeanServer.queryNames(objectName, null);
    }

    @SuppressWarnings("unchecked")
    public Set<ObjectName> getResourcesName() {
        return mbeanServer.queryNames(null, null);
    }

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

    public Object getObjectAttribute(ObjectName objectName, MBeanAttributeInfo info) {
        try {
            return mbeanServer.getAttribute(objectName, info.getName());
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot get "
                    + info.getName() + " from " + objectName, e);
        }
    }
}
