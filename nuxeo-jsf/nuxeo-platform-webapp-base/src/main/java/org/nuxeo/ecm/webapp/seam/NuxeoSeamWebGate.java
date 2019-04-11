/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
    public interface WebConnector {
        String getStateName();
    }

    protected final Set<WebConnector> waitingConnectors = fetchConnectors();

    protected Set<WebConnector> fetchConnectors() {
        Set<WebConnector> connectors = new HashSet<>();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName names;
        try {
            names = new ObjectName("Catalina:type=Connector,port=*,address=*");
        } catch (MalformedObjectNameException e) {
            log.error("Cannot query for tomcat connectors", e);
            return connectors;
        }
        Set<ObjectInstance> ois = mbs.queryMBeans(names, null);
        for (ObjectInstance oi : ois) {
            WebConnector connector = JMX.newMBeanProxy(mbs, oi.getObjectName(), WebConnector.class);
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
