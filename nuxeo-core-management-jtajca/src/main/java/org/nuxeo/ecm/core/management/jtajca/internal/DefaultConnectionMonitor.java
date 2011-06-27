/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ConnectionManager;

import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.nuxeo.ecm.core.management.jtajca.ConnectionMonitor;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * @author matic
 * 
 */
public class DefaultConnectionMonitor implements ConnectionMonitor {

    AbstractConnectionManager cm;

    protected DefaultConnectionMonitor(AbstractConnectionManager cm) {
        this.cm = cm;
    }

    protected static ConnectionMonitor monitor;

    public static void install() {
        AbstractConnectionManager cm = lookup();
        monitor = new DefaultConnectionMonitor(cm);
        bindManagementInterface();
    }

    public static void uninstall() throws MBeanRegistrationException,
            InstanceNotFoundException {
        if (monitor == null) {
            return;
        }
        unbindManagementInterface();
        monitor = null;
    }
    
    public static AbstractConnectionManager lookup() {
        ConnectionManager cm = NuxeoContainer.getConnectionManager();
        if (cm == null) { // try setup through NuxeoConnectionManagerFactory
            try {
                InitialContext ic = new InitialContext();
                cm = (ConnectionManager) ic.lookup("java:comp/env/NuxeoConnectionManager");
            } catch (NamingException cause) {
                throw new Error("Cannot lookup tx manager", cause);
            }
        }
        if (!(cm instanceof NuxeoContainer.ConnectionManagerWrapper)) {
            throw new Error("Nuxeo container not installed");
        }
        try {
            Field f = NuxeoContainer.ConnectionManagerWrapper.class.getDeclaredField("cm");
            f.setAccessible(true);
            return (AbstractConnectionManager) f.get(cm);
        } catch (Exception cause) {
            throw new Error("Cannot access to geronimo connection manager",
                    cause);
        }
    }

    protected static MBeanServer mbs;

    protected static void bindManagementInterface() {
        try {
            mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(monitor, new ObjectName(ConnectionMonitor.NAME));
        } catch (Exception cause) {
            throw new Error("Cannot register tx monitor", cause);
        }
    }

    protected static void unbindManagementInterface() {
        try {
            mbs.unregisterMBean(new ObjectName(ConnectionMonitor.NAME));
        } catch (Exception e) {
            throw new Error("Cannot unregister tx monitor");
        } finally {
            mbs = null;
        }
    }

    @Override
    public int getConnectionCount() {
        return cm.getConnectionCount();
    }

    @Override
    public int getIdleConnectionCount() {
        return cm.getIdleConnectionCount();
    }

    @Override
    public int getBlockingTimeoutMilliseconds() {
        return cm.getBlockingTimeoutMilliseconds();
    }

    @Override
    public int getIdleTimeoutMinutes() {
        return cm.getIdleTimeoutMinutes();
    }


    @Override
    public int getPartitionCount() {
        return cm.getPartitionCount();
    }

    @Override
    public int getPartitionMaxSize() {
        return cm.getPartitionMaxSize();
    }

    @Override
    public void setPartitionMaxSize(int maxSize) throws InterruptedException {
         cm.setPartitionMaxSize(maxSize);
    }


    @Override
    public int getPartitionMinSize() {
        return cm.getPartitionMinSize();
    }

 
    @Override
    public void setPartitionMinSize(int minSize) {
       cm.setPartitionMinSize(minSize);
    }

    @Override
    public void setBlockingTimeoutMilliseconds(int timeoutMilliseconds) {
        cm.setBlockingTimeoutMilliseconds(timeoutMilliseconds);
    }

    @Override
    public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
       cm.setIdleTimeoutMinutes(idleTimeoutMinutes);
    }

}
