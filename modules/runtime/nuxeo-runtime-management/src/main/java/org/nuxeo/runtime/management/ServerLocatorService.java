/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.runtime.management;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ServerLocatorService extends DefaultComponent implements ServerLocator {

    public static final String LOCATORS_EXT_KEY = "locators";

    private static final Log log = LogFactory.getLog(ServerLocatorService.class);

    protected final Map<String, MBeanServer> servers = new HashMap<>();

    protected MBeanServer defaultServer = ManagementFactory.getPlatformMBeanServer();

    protected String hostname;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(LOCATORS_EXT_KEY)) {
            doRegisterLocator((ServerLocatorDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(LOCATORS_EXT_KEY)) {
            doUnregisterLocator((ServerLocatorDescriptor) contribution);
        }
    }

    protected void doRegisterLocator(ServerLocatorDescriptor descriptor) {
        MBeanServer server = descriptor.isExisting ? doFindServer(descriptor.domainName) : doCreateServer(descriptor);
        servers.put(descriptor.domainName, server);
        if (descriptor.isDefault) {
            defaultServer = server;
        }
    }

    protected String doGetHostname() {
        if (hostname != null) {
            return hostname;
        }
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        return hostname;
    }

    protected JMXServiceURL doFormatServerURL(ServerLocatorDescriptor descriptor) {
        try {
            return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + descriptor.rmiPort + "/"
                    + descriptor.domainName + "/jmxrmi");
        } catch (MalformedURLException e) {
            throw new ManagementRuntimeException("Cannot format url for " + descriptor.domainName);
        }
    }

    protected String doFormatThreadName(ServerLocatorDescriptor descriptor) {
        return "mbeanServer-" + descriptor.domainName;
    }

    protected MBeanServer doCreateServer(final ServerLocatorDescriptor descriptor) {
        MBeanServer server = MBeanServerFactory.createMBeanServer();
        JMXServiceURL url = doFormatServerURL(descriptor);
        if (!descriptor.remote) {
            return server;
        }
        final RMIConnectorServer connector;
        try {
            connector = new RMIConnectorServer(url, null, server);
        } catch (IOException e) {
            throw new ManagementRuntimeException("Cannot start connector for " + descriptor.domainName, e);
        }
        try {
            connector.start();
        } catch (IOException e) {
            try {
                LocateRegistry.createRegistry(descriptor.rmiPort);
            } catch (RemoteException e2) {
                throw new ManagementRuntimeException("Cannot start RMI connector for " + descriptor.domainName, e);
            }
            try {
                connector.start();
            } catch (IOException e2) {
                throw new ManagementRuntimeException("Cannot start RMI connector for " + descriptor.domainName, e2);
            }
        }
        assert connector.isActive();
        log.info("Started a mbean server : " + url);
        return server;
    }

    @SuppressWarnings("cast")
    protected MBeanServer doFindServer(String domainName) {
        for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
            String domain = server.getDefaultDomain();
            if (domain == null || !domain.equals(domainName)) {
                continue;
            }
            return server;
        }
        return defaultServer;
    }

    protected void doUnregisterLocator(ServerLocatorDescriptor descriptor) {
        servers.remove(descriptor.domainName);
        if (descriptor.isDefault) {
            defaultServer = ManagementFactory.getPlatformMBeanServer();
        }
    }

    @Override
    @SuppressWarnings("cast")
    public MBeanServer lookupServer(ObjectName qualifiedName) {
        if (defaultServer.isRegistered(qualifiedName)) {
            return defaultServer;
        }
        for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
            if (server.isRegistered(qualifiedName)) {
                return server;
            }
        }
        throw new ManagementRuntimeException(qualifiedName + " is not registered");
    }

    @Override
    public MBeanServer lookupServer() {
        return defaultServer;
    }

    @Override
    public MBeanServer lookupServer(String domainName) {
        return doFindServer(domainName);
    }

    public void registerLocator(String domain, boolean isDefault) {
        doRegisterLocator(new ServerLocatorDescriptor(domain, isDefault));
    }

}
