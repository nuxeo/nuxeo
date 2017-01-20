/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.management.jtajca.Defaults;
import org.nuxeo.ecm.core.management.jtajca.ConnectionPoolMonitor;
import org.nuxeo.ecm.core.management.jtajca.TransactionMonitor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManager;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainerListener;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component used to install/uninstall the monitors (transaction and connections).
 *
 * @since 5.6
 */
public class DefaultMonitorComponent extends DefaultComponent {

    private final ConnectionManagerUpdater cmUpdater = new ConnectionManagerUpdater();

    private class ConnectionManagerUpdater implements NuxeoContainerListener {
        @Override
        public void handleNewConnectionManager(String name, NuxeoConnectionManager cm) {
            ConnectionPoolMonitor monitor = new DefaultConnectionPoolMonitor(name, cm);
            monitor.install();
            poolConnectionMonitors.put(name, monitor);
        }

        @Override
        public void handleConnectionManagerReset(String name, NuxeoConnectionManager cm) {
            DefaultConnectionPoolMonitor monitor = (DefaultConnectionPoolMonitor) poolConnectionMonitors.get(name);
            monitor.handleNewConnectionManager(cm);
        }

        @Override
        public void handleConnectionManagerDispose(String name, NuxeoConnectionManager mgr) {
            ConnectionPoolMonitor monitor = poolConnectionMonitors.remove(name);
            monitor.uninstall();
        }

    }

    protected final Log log = LogFactory.getLog(DefaultMonitorComponent.class);

    protected CoreSessionMonitor coreSessionMonitor;

    protected TransactionMonitor transactionMonitor;

    protected Map<String, ConnectionPoolMonitor> poolConnectionMonitors = new HashMap<>();

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        install();
    }

    @Override
    public void deactivate(ComponentContext context) {
        uninstall();
        super.deactivate(context);
    }

    protected boolean installed;

    protected void install() {
        installed = true;

        coreSessionMonitor = new DefaultCoreSessionMonitor();
        coreSessionMonitor.install();

        transactionMonitor = new DefaultTransactionMonitor();
        transactionMonitor.install();

        NuxeoContainer.addListener(cmUpdater);
    }

    /**
     * Make sure we open the repository, to initialize its connection manager.
     */
    protected void activateRepository(String repositoryName) {
        try (CoreSession session = CoreInstance.openCoreSessionSystem(repositoryName)) {
            // do nothing, just open and close
        }
    }

    protected void uninstall() {
        if (!installed) {
            return;
        }
        // temporary log to help diagnostics
        log.warn("Total commits during server life: " + transactionMonitor.getTotalCommits());
        installed = false;
        NuxeoContainer.removeListener(cmUpdater);
        for (ConnectionPoolMonitor storage : poolConnectionMonitors.values()) {
            storage.uninstall();
        }
        coreSessionMonitor.uninstall();
        transactionMonitor.uninstall();
        poolConnectionMonitors.clear();
        coreSessionMonitor = null;
        transactionMonitor = null;
    }

    public static class ServerInstance {
        public final MBeanServer server;
        public final ObjectName name;

        ServerInstance(MBeanServer server, ObjectName name) {
            this.server = server;
            this.name = name;
        }
    }

    protected static ServerInstance bind(Object managed) {
        return bind(managed, "default");
    }

    protected static ServerInstance bind(Class<?> itf, Object managed) {
        return bind(itf, managed, "default");
    }

    protected static ServerInstance bind(Object managed, String name) {
        return bind(managed.getClass().getInterfaces()[0], managed, name);
    }

    protected static ServerInstance bind(Class<?> itf, Object managed, String name) {
        MBeanServer mbs = Framework.getLocalService(ServerLocator.class).lookupServer();
        name = Defaults.instance.name(itf, name);
        try {
            ObjectInstance oi = mbs.registerMBean(managed, new ObjectName(name));
            return new ServerInstance(mbs, oi.getObjectName());
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                | MalformedObjectNameException e) {
            throw new UnsupportedOperationException("Cannot bind " + managed + " on " + name, e);
        }
    }

    protected static void unbind(ServerInstance instance) {
        try {
            instance.server.unregisterMBean(instance.name);
        } catch (MBeanRegistrationException | InstanceNotFoundException e) {
            LogFactory.getLog(DefaultMonitorComponent.class).error("Cannot unbind " + instance, e);
        }
    }

}
