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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.NameNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class ServiceManager implements org.nuxeo.runtime.ServiceManager {

    private static final Log log = LogFactory.getLog(ServiceManager.class);

    private static final ServiceManager instance = new ServiceManager();

    private final Map<String, ServiceDescriptor> services = new HashMap<String, ServiceDescriptor>();

    private final List<ServiceHost> servers = new Vector<ServiceHost>();

    private final Map<String, ServiceGroup> groups = new HashMap<String, ServiceGroup>();

    // Singleton.
    private ServiceManager() {
    }

    public static ServiceManager getInstance() {
        return instance;
    }

    public ServiceDescriptor[] getServiceDescriptors() {
        return services.values().toArray(new ServiceDescriptor[services.size()]);
    }

    public ServiceDescriptor getServiceDescriptor(Class<?> serviceClass) {
        return getServiceDescriptor(serviceClass.getName());
    }

    public ServiceDescriptor getServiceDescriptor(String serviceClass) {
        return services.get(serviceClass);
    }

    public ServiceDescriptor getServiceDescriptor(Class<?> serviceClass,
            String name) {
        return getServiceDescriptor(serviceClass.getName(), name);
    }

    public ServiceDescriptor getServiceDescriptor(String serviceClass,
            String name) {
        return services.get(serviceClass + '#' + name);
    }

    public void registerService(ServiceDescriptor sd) {
        String key = sd.getInstanceName();
        synchronized (services) {
            if (services.containsKey(key)) {
                String msg = "Duplicate service registration: " + key;
                log.error(msg);
                Framework.getRuntime().getWarnings().add(msg);
                return;
            }
            services.put(key, sd);
            sd.getGroup().addService(sd);
            log.info("Registered service: " + key);
        }
    }

    public void unregisterService(ServiceDescriptor sd) {
        String key = sd.getInstanceName();
        synchronized (services) {
            sd = services.remove(key);
            if (sd == null) {
                log.warn(String.format(
                        "Cannot unregister service '%s': either already"
                                + " unregistered or not registered at all", key));
            } else {
                sd.getGroup().removeService(sd);
                log.info("Unregistered service: " + key);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) throws Exception {
        ServiceDescriptor sd = services.get(serviceClass.getName());
        if (sd != null) {
            try {
                T svc = (T) sd.getGroup().getServer().lookup(sd);
                if (svc != null) {
                    return svc;
                }
            } catch (NameNotFoundException e) {
                // When all facades are deployed (JBoss for now), then lookup
                // errors are legitimate. Otherwise it may be just a bad
                // packaging issue, so don't log. TODO fix packaging
                if (J2EEContainerDescriptor.getSelected() == J2EEContainerDescriptor.JBOSS) {
                    log.warn("Existing binding but unreachable service for "
                            + serviceClass.getName()
                            + " ! Fallback on local service...");
                    log.debug(e.getMessage() + " Check binding declaration: "
                            + sd.toString());
                }
            }
        }
        return Framework.getLocalService(serviceClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass, String name)
            throws Exception {
        ServiceDescriptor sd = services.get(serviceClass.getName() + '#' + name);
        log.trace("Known services" + services.keySet().toString());
        if (sd == null) {
            return Framework.getLocalService(serviceClass);
        }
        return (T) sd.getGroup().getServer().lookup(sd);
    }

    /**
     * Dynamically lookup services given a service URI.
     * <p>
     * This is a dynamic lookup in the sense that the service bindings should
     * not be registered through extension points but all the information about
     * how to locate the service are passed through the URI.
     * <p>
     * This method is not portable since the URI depends on the target server
     * and configuration. Examples of service URIs:
     * <ul>
     * <li><code>jboss://localhost:1099/nuxeo/TypeManagerBean/remote</code> -
     * locate a service on jboss</li>
     * <li><code>glassfish://localhost:1234/org.nuxeo.ecm.platform.types.TypeManager</code> -
     * locate a service on glassfish</li>
     * </ul>
     *
     * @param serviceUri the service uri
     * @return the service
     */
    public Object getService(String serviceUri) throws Exception {
        URI uri = new URI(serviceUri);
        ServiceLocatorFactory factory = ServiceLocatorFactory.getFactory(uri.getScheme());
        if (factory != null) {
            ServiceLocator locator = factory.createLocator(uri);
            return locator.lookup(uri.getPath().substring(1)); // avoid leading /
        }
        return null;
    }

    public ServiceGroup getOrCreateGroup(String name) {
        if (name == null || name.length() == 0) {
            name = "*";
        }
        synchronized (groups) {
            ServiceGroup group = groups.get(name);
            if (group == null) {
                group = new ServiceGroup(name);
                groups.put(name, group);
            }
            return group;
        }
    }

    public ServiceGroup getGroup(String name) {
        synchronized (groups) {
            return groups.get(name);
        }
    }

    public void removeGroup(String name) {
        synchronized (groups) {
            groups.remove(name);
        }
    }

    public void addGroup(String name) {
        if (name == null || name.length() == 0) {
            name = "*";
        }
        ServiceGroup group = new ServiceGroup(name);
        synchronized (groups) {
            groups.put(name, group);
        }
    }

    public ServiceGroup getRootGroup() {
        return getOrCreateGroup("*");
    }

    public void registerServer(ServiceHost server) {
        servers.add(server);
    }

    public void unregisterServer(ServiceHost server) {
        servers.remove(server);
        server.dispose();
    }

    /**
     * Removes all registered servers.
     */
    public void removeServers() {
        for (ServiceHost server : servers) {
            server.dispose();
        }
        servers.clear();
    }

    public void removeServices() {
        services.clear();
    }

    public void removeGroups() {
        groups.clear();
    }

    public void reset() {
        removeServices();
        removeGroups();
        removeServers();
    }

    public ServiceHost[] getServers() {
        return servers.toArray(new ServiceHost[servers.size()]);
    }

}
