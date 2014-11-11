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

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.jsesoft.mmbi.NamedModelMBean;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ServerLocatorService extends DefaultComponent implements
        ServerLocator {

    public static final String LOCATORS_EXT_KEY = "locators";

    protected Map<String, MBeanServer> otherServers = new HashMap<String, MBeanServer>();

    protected MBeanServer defaultServer = ManagementFactory.getPlatformMBeanServer();

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

    @SuppressWarnings("unchecked")
    protected void doRegisterLocator(ServerLocatorDescriptor descriptor) {
        String domainName = descriptor.getDomainName();
        for (MBeanServer server : (List<MBeanServer>) MBeanServerFactory.findMBeanServer(null)) {
            if (server.getDefaultDomain().equals(domainName)) {
                otherServers.put(domainName, server);
                if (descriptor.isDefaulServer) {
                    defaultServer = server;
                }
                return;
            }
        }
        throw new ManagementRuntimeException(
                "cannot locate mbean server containing domain " + domainName);
    }

    protected void doUnregisterLocator(ServerLocatorDescriptor descriptor) {
        otherServers.remove(descriptor.domainName);
        if (descriptor.isDefaulServer) {
            defaultServer = ManagementFactory.getPlatformMBeanServer();
        }
    }


    @SuppressWarnings("unchecked")
    public MBeanServer lookupServer(ObjectName qualifiedName) {
        if (defaultServer.isRegistered(qualifiedName)) {
            return defaultServer;
        }
        for (MBeanServer server : (List<MBeanServer>) MBeanServerFactory.findMBeanServer(null)) {
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

    public MBeanServer lookupServer(String domainName) {
        if (otherServers.containsKey(domainName)) {
            return otherServers.get(domainName);
        }
        return defaultServer;
    }

    public void registerLocator(String domain, boolean isDefault) {
        doRegisterLocator(new ServerLocatorDescriptor(domain, isDefault));
    }

}