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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("server")
public class ServiceHost implements Serializable {

    private static final long serialVersionUID = 632838284857927463L;

    private static final Log log = LogFactory.getLog(ServiceHost.class);

    public static final ServiceHost LOCAL_SERVER = new ServiceHost(
            RuntimeServiceLocator.class);

    @XNode("@class")
    private Class<? extends ServiceLocator> serviceLocatorClass;

    @XNode("@host")
    private String host;

    @XNode("@port")
    private int port;

    private Properties properties;

    private transient ServiceGroup[] groups;

    private transient ServiceLocator serviceLocator;

    public ServiceHost() {
    }

    public ServiceHost(Class<? extends ServiceLocator> serverClass) {
        serviceLocatorClass = serverClass;
    }

    public ServiceHost(Class<? extends ServiceLocator> serverClass,
            String[] groups) {
        this(serverClass);
        setGroups(groups);
    }

    public String getServiceLocatorClass() {
        if (serviceLocatorClass == null) {
            log.warn("Shouldn't be asking a service host for a service locator... don't have one!");
            return null;
        }
        return serviceLocatorClass.getName();
    }

    protected ServiceLocator createServiceLocator() throws Exception {
        ServiceLocator serviceLocator = serviceLocatorClass.newInstance();
        serviceLocator.initialize(host, port, properties);
        return serviceLocator;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @XNodeMap(value = "property", key = "@name", componentType = String.class, type = Properties.class, trim = true)
    public void setProperties(Properties properties) {
        this.properties = new Properties();
        // expand properties
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value.getClass() == String.class) {
                this.properties.put(entry.getKey().toString(),
                        Framework.expandVars(value.toString().trim()));
            } else {
                this.properties.put(entry.getKey(), value);
            }
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defValue) {
        return properties.getProperty(key, defValue);
    }

    @XNodeList(value = "group", componentType = String.class, type = String[].class, trim = true)
    public void setGroups(String[] groups) {
        this.groups = new ServiceGroup[groups.length];

        ServiceManager mgr = ServiceManager.getInstance();
        for (int i = 0; i < groups.length; i++) {
            this.groups[i] = mgr.getOrCreateGroup(groups[i]);
            this.groups[i].setServer(this);
        }
    }

    public ServiceGroup[] getGroups() {
        if (groups == null) {
            groups = new ServiceGroup[] { ServiceManager.getInstance().getRootGroup() };
        }
        return groups;
    }

    public ServiceLocator getServiceLocator() throws Exception {
        if (serviceLocator == null) {
            serviceLocator = createServiceLocator();
        }
        return serviceLocator;
    }

    public void dispose() {
        if (groups != null) {
            // remove cached information from groups
            for (ServiceGroup group : groups) {
                group.setServer(null);
            }
        }
        if (serviceLocator != null) {
            serviceLocator.dispose();
            serviceLocator = null;
        }
        serviceLocatorClass = null;
        properties = null;
        groups = null;
    }

    public Object lookup(ServiceDescriptor sd) throws Exception {
        Object service = getServiceLocator().lookup(sd);
        if (service == null) {
            return null;
        }
        ServiceAdapter adapter = sd.getAdapter();
        if (adapter != null) {
            return adapter.adapt(sd, service);
        }
        return service;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ServiceHost)) {
            return false;
        }
        ServiceHost server = (ServiceHost) obj;
        return server.serviceLocatorClass == serviceLocatorClass;
    }

    @Override
    public int hashCode() {
        return serviceLocatorClass != null ? serviceLocatorClass.hashCode() : 0;
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        int len = in.readInt();
        String[] ar = new String[len];
        for (int i = 0; i < len; i++) {
            ar[i] = (String) in.readObject();
        }
        setGroups(ar);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (groups != null) {
            out.writeInt(groups.length);
            for (ServiceGroup group : groups) {
                out.writeObject(group.getName());
            }
        } else {
            out.writeInt(0);
        }
    }

}
