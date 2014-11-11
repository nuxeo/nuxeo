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
 *     matic
 */
package org.nuxeo.runtime.management;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
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

public class ServerLocatorService extends DefaultComponent implements
        ServerLocator {

    public static final String LOCATORS_EXT_KEY = "locators";

    private static final Log log = LogFactory.getLog(ServerLocatorService.class);

    protected final Map<String, MBeanServer> servers = new HashMap<String, MBeanServer>();

    protected MBeanServer defaultServer = ManagementFactory.getPlatformMBeanServer();

    protected String hostname;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(LOCATORS_EXT_KEY)) {
            doRegisterLocator((ServerLocatorDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(LOCATORS_EXT_KEY)) {
            doUnregisterLocator((ServerLocatorDescriptor) contribution);
        }
    }

    protected void doRegisterLocator(ServerLocatorDescriptor descriptor) {
        MBeanServer server = descriptor.isExisting ? doFindServer(descriptor.domainName)
                : doCreateServer(descriptor);
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
            return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"
                    + descriptor.rmiPort + "/" + descriptor.domainName
                    + "/jmxrmi");
        } catch (MalformedURLException e) {
            throw new ManagementRuntimeException("Cannot format url for "
                    + descriptor.domainName);
        }
    }

    protected String doFormatThreadName(ServerLocatorDescriptor descriptor) {
        return "mbeanServer-" + descriptor.domainName;
    }

    protected MBeanServer doCreateServer(
            final ServerLocatorDescriptor descriptor) {
        MBeanServer server = MBeanServerFactory.createMBeanServer();
        JMXServiceURL url = doFormatServerURL(descriptor);
        final RMIConnectorServer connector;
        try {
            connector = new RMIConnectorServer(url, null, server);
        } catch (IOException e) {
            throw new ManagementRuntimeException("Cannot start connector for "
                    + descriptor.domainName, e);
        }
        try {
            connector.start();
        } catch (IOException e) {
            try {
                LocateRegistry.createRegistry(descriptor.rmiPort);
            } catch (Exception e2) {
                throw new ManagementRuntimeException(
                        "Cannot start RMI connector for "
                                + descriptor.domainName, e);
            }
            try {
                connector.start();
            } catch (Exception e2) {
                throw new ManagementRuntimeException(
                        "Cannot start RMI connector for "
                                + descriptor.domainName, e2);
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
        throw new ManagementRuntimeException(qualifiedName
                + " is not registered");
    }

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
