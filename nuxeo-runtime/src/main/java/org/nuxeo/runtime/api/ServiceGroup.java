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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServiceGroup implements Serializable {

    private static final Log log = LogFactory.getLog(ServiceGroup.class);

    private static final long serialVersionUID = -206692130381710767L;

    private final String name;

    private final ServiceGroup parent;

    private ServiceHost server;

    public ServiceGroup(String name) {
        if (name == null || name.equals("*") || name.length() == 0) {
            this.name = "*";
            parent = null;
        } else {
            this.name = name;
            int p = name.lastIndexOf('/');
            if (p < 1) {
                parent = ServiceManager.getInstance().getRootGroup();
            } else {
                parent = ServiceManager.getInstance().getOrCreateGroup(
                        name.substring(0, p));
            }
        }
    }

    public String getName() {
        return name;
    }

    public ServiceHost getServer() {
        if (server == null) {
            if (parent == null) {
                // if no server was defined use the local runtime service
                // locator
                server = ServiceHost.LOCAL_SERVER;
            } else {
                server = parent.getServer();
            }
        }
        if (server.getServiceLocatorClass() == null) {
            log.warn("No ServiceLocator Returning a LOCAL_SERVER instead of a ServiceHost");
            return ServiceHost.LOCAL_SERVER;

        }
        return server;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) throws Exception {
        ServiceDescriptor sd = ServiceManager.getInstance().getServiceDescriptor(
                serviceClass);
        if (sd == null) {
            return null;
        }
        return (T) getServer().lookup(sd);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass, String name)
            throws Exception {
        ServiceDescriptor sd = ServiceManager.getInstance().getServiceDescriptor(
                serviceClass.getName(), name);
        if (sd == null) {
            return null;
        }
        return (T) getServer().lookup(sd);
    }

    public Object lookup(ServiceDescriptor sd) throws Exception {
        return getServer().lookup(sd);
    }

    public void setServer(ServiceHost server) {
        if (server == null) {
            log.debug("service group " + getName() + " no longer has a server!");
        } else if (server.getServiceLocatorClass() == null) {
            log.debug("service group " + getName()
                    + " just had its ServiceHost removed!");
        }
        this.server = server;
    }

    public void addService(ServiceDescriptor sd) {
        // do nothing
    }

    public void removeService(ServiceDescriptor sd) {
        // do nothing
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ServiceGroup)) {
            return false;
        }
        ServiceGroup sg = (ServiceGroup) obj;
        return sg.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

}
