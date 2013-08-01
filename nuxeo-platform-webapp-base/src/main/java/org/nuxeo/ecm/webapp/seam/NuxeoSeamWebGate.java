/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webapp.seam;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Listener for Seam cache flush requests
 *
 * @since 5.5
 */
public class NuxeoSeamWebGate implements ServletContextListener {

    protected static NuxeoSeamWebGate instance;

    protected static Log log = LogFactory.getLog(NuxeoSeamWebGate.class);

    protected boolean initialized;

    public NuxeoSeamWebGate() {
        instance = this;
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        initialized = false;
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        initialized = true;
    }

    @MXBean
    interface WebConnector {
        String getStateName();
    }

    protected final Set<WebConnector> waitingConnectors = fetchConnectors();

    protected Set<WebConnector> fetchConnectors() {
        Set<WebConnector> connectors = new HashSet<WebConnector>();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName names;
        try {
            names = new ObjectName(
                    "Catalina:type=Connector,port=*,address=*");
        } catch (MalformedObjectNameException e) {
            log.error("Cannot query for tomcat connectors", e);
            return connectors;
        }
        Set<ObjectInstance> ois = mbs.queryMBeans(names, null);
        for (ObjectInstance oi : ois) {
            WebConnector connector = JMX.newMBeanProxy(mbs, oi.getObjectName(),
                    WebConnector.class);
            connectors.add(connector);
        }
        return connectors;
    }

    protected synchronized boolean checkConnectorsUp() {
        Iterator<WebConnector> it = waitingConnectors.iterator();
        while (it.hasNext()) {
            WebConnector connector = it.next();
            if ("STARTED".equals(connector.getStateName())) {
                it.remove();
            }
        }
        return waitingConnectors.isEmpty();
    }

    public static boolean isInitialized() {
        if (instance == null) {
            return false;
        }
        if (instance.initialized == false) {
            return false;
        }
        return instance.checkConnectorsUp();
    }

}
