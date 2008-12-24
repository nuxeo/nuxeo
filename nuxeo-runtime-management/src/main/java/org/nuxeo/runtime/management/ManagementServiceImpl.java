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
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsesoft.mmbi.NamedModelMBean;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoInstrumentorFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class ManagementServiceImpl extends DefaultComponent implements
        ManagementService {

    private static final Log log = LogFactory.getLog(ManagementServiceImpl.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.management.service.ManagementService#
     * getManagedServices()
     */
    @SuppressWarnings("unchecked")
    public Set<ObjectInstance> getManagedServices() {
        String qualifiedName = ManagementNameFormatter.formatManagedNames("service");
        ObjectName objectName = ManagementNameFormatter.getObjectName(qualifiedName);
        return mbeanServer.queryMBeans(objectName, null);
    }

    protected final MBeanServer mbeanServer = java.lang.management.ManagementFactory.getPlatformMBeanServer();

    @SuppressWarnings("unchecked")
    public Set<ObjectInstance> getManagedResources() {
        return mbeanServer.queryMBeans(null, null);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("managedServices")) {
            doRegisterManagedServiceContribution((ManagedServiceDescriptor) contribution);
        }
    }

    protected final Map<String, ManagedServiceDescriptor> descriptors = 
        new HashMap<String, ManagedServiceDescriptor>();

    protected Class<?> guardedLoadClass(String className) {
        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw ManagementRuntimeException.wrap(e);
        }
    }

    protected Object guardedService(Class<?> serviceClass) {
        Object service;
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

    protected final ModelMBeanInfoInstrumentorFactory mbeanInfoFactory = new ModelMBeanInfoInstrumentorFactory();

    protected final Map<ManagedServiceDescriptor, ManagedService> managedServices = new HashMap<ManagedServiceDescriptor, ManagedService>();

    protected void doRegisterManagedServiceContribution(
            ManagedServiceDescriptor contribution) {
        ManagedService service = doResolve(contribution);
        doExport(service);
        managedServices.put(service.getDescriptor(), service);
        if (log.isInfoEnabled()) {
            log.info("registered management contribution for " + contribution);
        }
    }

    protected ManagedService doResolve(ManagedServiceDescriptor descriptor) {
        Class<?> serviceClass = guardedLoadClass(descriptor.getServiceClassName());
        Object serviceInstance = guardedService(serviceClass);
        String serviceName = descriptor.getServiceName();
        if (serviceName == null) {
            serviceName = serviceClass.getSimpleName();
        }
        if (!ManagementNameFormatter.isQualified(serviceName)) {
            serviceName = ManagementNameFormatter.formatManagedName(serviceName);
        }
        ObjectName managementName = ManagementNameFormatter.getObjectName(serviceName);
        String ifaceClassName = descriptor.getIfaceClassName();
        Class<?> managementClass = ifaceClassName != null ? guardedLoadClass(ifaceClassName)
                : serviceClass;

        return new ManagedService(descriptor, managementName, managementClass,
                serviceInstance);
    }

    protected void doExport(ManagedService service) {
        try {
            NamedModelMBean mbean = new NamedModelMBean();
            mbean.setManagedResource(service.getServiceInstance(),
                    "ObjectReference");
            mbean.setModelMBeanInfo(mbeanInfoFactory.getModelMBeanInfo(service.getManagementClass()));
            mbean.setInstance(mbeanServer.registerMBean(mbean,
                    service.getManagementName()));
        } catch (Exception e) {
            throw ManagementRuntimeException.wrap(
                    "Cannot register management contribution for " + service, e);
        }
    }

}
