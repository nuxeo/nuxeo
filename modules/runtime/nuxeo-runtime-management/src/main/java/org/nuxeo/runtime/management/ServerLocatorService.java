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
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class ServerLocatorService extends DefaultComponent implements ServerLocator {

    private static final Logger log = LogManager.getLogger(ServerLocatorService.class);

    public static final String LOCATORS_EXT_KEY = "locators";

    protected static final MBeanServer DEFAULT_SERVER = ManagementFactory.getPlatformMBeanServer();

    protected Map<String, MBeanServer> servers;

    /** StandbyComponent in core expects a default server to be available before start **/
    protected MBeanServer defaultServer = DEFAULT_SERVER;

    @Override
    public void start(ComponentContext context) {
        servers = new HashMap<>();
        this.<ServerLocatorDescriptor> getRegistryContributions(LOCATORS_EXT_KEY).forEach(descriptor -> {
            MBeanServer server = descriptor.isExisting ? doFindServer(descriptor.domainName)
                    : doCreateServer(descriptor);
            servers.put(descriptor.domainName, server);
            if (descriptor.isDefault) {
                defaultServer = server;
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        servers = null;
        defaultServer = DEFAULT_SERVER;
    }

    protected MBeanServer doCreateServer(final ServerLocatorDescriptor descriptor) {
        MBeanServer server = MBeanServerFactory.createMBeanServer();
        JMXServiceURL url;
        try {
            url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + descriptor.rmiPort + "/"
                    + descriptor.domainName + "/jmxrmi");
        } catch (MalformedURLException e) {
            throw new ManagementRuntimeException("Cannot format url for " + descriptor.domainName);
        }
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
        log.info("Started a mbean server: {}", url);
        return server;
    }

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

    @Override
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

}
