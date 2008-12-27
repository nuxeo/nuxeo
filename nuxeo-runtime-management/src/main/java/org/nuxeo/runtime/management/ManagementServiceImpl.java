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

    protected final Set<ManagedServiceDescriptor> registeredDescriptors = new HashSet<ManagedServiceDescriptor>();

    protected void doRegister(ManagedServiceDescriptor contribution) {
        registeredDescriptors.add(contribution);
        if (log.isInfoEnabled()) {
            log.info("registered management contribution for " + contribution);
        }
    }

    protected void doUnregister(ManagedServiceDescriptor descriptor) {
        if (registeredDescriptors.remove(descriptor) == false) {
            throw new IllegalArgumentException(descriptor
                    + " is not registered");
        }
        ManagedService managedService = managedServices.remove(descriptor);
        if (managedService == null || !managedService.isRegistered()) {
            return;
        }
        doUnregister(managedService);
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
            serviceName = ManagementNameFormatter.formatManagedName(serviceName);
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

    protected void doRegister(ManagedService service) {
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
                    "Cannot register management contribution for " + service, e);
        }
    }

    protected void doUnregister(ManagedService service) {
        NamedModelMBean mbean = service.mbean;
        if (mbean == null) {
            throw new IllegalStateException(service.getDescriptor()
                    + " is not registered into mbean server");
        }
        try {
            mbeanServer.unregisterMBean(mbean.getName());
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot unregister "
                    + service.getDescriptor(), e);
        } finally {
            service.mbean = null;
        }
    }

    public class ManagementAdapter implements ManagementServiceMBean {

        public Set<String> getRegisteredServicesClassName() {
            Set<String> registeredServicesClassName = new HashSet<String>();
            for (ManagedServiceDescriptor registeredDescriptor : registeredDescriptors) {
                registeredServicesClassName.add(registeredDescriptor.getServiceClassName());
            }
            return registeredServicesClassName;
        }

        public void enable() {
            doEnable();
        }

        public void disable() {
            doDisable();
        }
    }

    protected void doEnable() {
        for (ManagedServiceDescriptor descriptor : registeredDescriptors) {
            ManagedService service = doResolve(descriptor);
            doRegister(service);
            managedServices.put(descriptor, service);
        }
    }

    protected void doDisable() {
        Iterator<Entry<ManagedServiceDescriptor, ManagedService>> iterator = 
            managedServices.entrySet().iterator();
        while (iterator.hasNext()) {
            ManagedService service = iterator.next().getValue();
            doUnregister(service);
            iterator.remove();
        }
    }

    @Override
    public void activate(ComponentContext context) {
        try {
            ModelMBeanInfo mbeanInfo = mbeanInfoFactory.getModelMBeanInfo(ManagementAdapter.class);
            RequiredModelMBean mbean = new RequiredModelMBean(mbeanInfo);
            mbean.setManagedResource(new ManagementAdapter(), "ObjectReference");
            mbeanServer.registerMBean(mbean, new ObjectName(
                    "nx:service=management"));
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot enable management", e);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            mbeanServer.unregisterMBean(new ObjectName("nx:service=management"));
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap("Cannot disable management",
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<ObjectInstance> getManagedServices() {
        String qualifiedName = ManagementNameFormatter.formatManagedNames("service");
        ObjectName objectName = ManagementNameFormatter.getObjectName(qualifiedName);
        return mbeanServer.queryMBeans(objectName, null);
    }

    @SuppressWarnings("unchecked")
    public Set<ObjectInstance> getManagedResources() {
        return mbeanServer.queryMBeans(null, null);
    }

}
