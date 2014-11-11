/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject(value = "service", order = { "serviceClass", "name" })
public class ServiceDescriptor implements Serializable {

    private static final long serialVersionUID = 5490362136607217161L;

    @XNode("@name")
    private String name;

    private String serviceClassName;

    // this should not be loaded when sending service descriptors to a client
    // because
    // the class may not exists on the client. the class should be loaded only
    // if the client explicitly
    // lookup the service
    private transient Class<?> serviceClass;

    @XNode("@class")
    private void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
        serviceClassName = serviceClass.getName();
    }

    @XNode("adapter")
    private Class<ServiceAdapter> adapterClass;

    private ServiceAdapter adapter;

    @XNode("locator")
    private String locatorPattern; // the locator pattern used to compute the

    // final locator

    private transient String locator; // this will be re-computed on the

    // client side

    private ServiceGroup group;

    /**
     * To be used by XMap.
     */
    public ServiceDescriptor() {
    }

    public ServiceDescriptor(String serviceClassName) {
        this.serviceClassName = serviceClassName;
    }

    public ServiceDescriptor(String serviceClassName, String group) {
        this.serviceClassName = serviceClassName;
        this.group = ServiceManager.getInstance().getOrCreateGroup(group);
    }

    public ServiceDescriptor(String serviceClassName, String group, String name) {
        this.serviceClassName = serviceClassName;
        this.group = ServiceManager.getInstance().getOrCreateGroup(group);
        this.name = name;
    }

    public ServiceDescriptor(Class<?> serviceClass) {
        setServiceClass(serviceClass);
    }

    public ServiceDescriptor(Class<?> serviceClass, String group) {
        setServiceClass(serviceClass);
        this.group = ServiceManager.getInstance().getOrCreateGroup(group);
    }

    public ServiceDescriptor(Class<?> serviceClass, String group, String name) {
        setServiceClass(serviceClass);
        this.group = ServiceManager.getInstance().getOrCreateGroup(group);
        this.name = name;
    }

    @XNode("@group")
    public void setGroup(String group) {
        this.group = ServiceManager.getInstance().getOrCreateGroup(group);
    }

    public ServiceGroup getGroup() {
        return group;
    }

    public ServiceHost getServer() {
        return group.getServer();
    }

    public String getName() {
        return name;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public String getServiceClassSimpleName() {
        int p = serviceClassName.lastIndexOf('.');
        if (p == -1) {
            return serviceClassName;
        }
        return serviceClassName.substring(p + 1);
    }

    // this should be called only for service lookup to avoid class loader
    // problems (ClassNotFoundException)
    Class<?> getServiceClass() throws ClassNotFoundException {
        if (serviceClass == null) {
            serviceClass = Thread.currentThread().getContextClassLoader().loadClass(
                    serviceClassName);
        }
        return serviceClass;
    }

    public String getInstanceName() {
        return name != null ? serviceClassName + '#' + name : serviceClassName;
    }

    public String getLocator() {
        return locator == null ? locatorPattern : locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public ServiceAdapter getAdapter() throws Exception {
        if (adapterClass == null) {
            return null;
        } else if (adapter == null) {
            adapter = adapterClass.newInstance();
        }
        return adapter;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ServiceDescriptor) {
            ServiceDescriptor sd = (ServiceDescriptor) obj;
            if (!serviceClassName.equals(sd.serviceClassName)) {
                return false;
            }
            if (name != null) {
                if (!name.equals(sd.name)) {
                    return false;
                }
            } else {
                if (name != sd.name) {
                    return false;
                }
            }
            if (!group.getName().equals(sd.group.getName())) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result;
        result = name != null ? name.hashCode() : 0;
        result = 31 * result + (serviceClassName != null ? serviceClassName.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "[name=" + getName() + ", group=" + getGroup()
                + ", instanceName=" + getInstanceName() + " locator: "
                + getLocator() + "]";
    }

}
