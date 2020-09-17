/*
 * (C) Copyright 2012-2017 Nuxeo (http://nuxeo.com/) and others.
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
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.nuxeo.ecm.core.management.jtajca.ConnectionPoolMonitor;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.management.jtajca.Defaults;
import org.nuxeo.ecm.core.management.jtajca.TransactionMonitor;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManager;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainerListener;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.Component;
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
    public void start(ComponentContext context) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        if (repositoryService == null) {
            // RepositoryService failed to start, no need to go further
            return;
        }
        uninstall();
        install();
    }

    @Override
    public int getApplicationStartedOrder() {
        // should deploy after metrics service
        Component component = (Component) Framework.getRuntime().getComponent(MetricsService.class.getName());
        return component.getApplicationStartedOrder() + 1;
    }

    @Override
    public void stop(ComponentContext context) {
        uninstall();
    }

    protected boolean installed;

    protected void install() {
        installed = true;

        coreSessionMonitor = new DefaultCoreSessionMonitor();
        coreSessionMonitor.install();

        transactionMonitor = new DefaultTransactionMonitor();
        transactionMonitor.install();

        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        GenericKeyedObjectPool<String, ?> pool = repositoryService.getPool();
        repositoryService.getRepositoryNames().forEach(repositoryName -> {
            String name = "repository/" + repositoryName;
            ConnectionPoolMonitor monitor = new ObjectPoolMonitor(name, pool, repositoryName);
            monitor.install();
            poolConnectionMonitors.put(name, monitor);
        });

        NuxeoContainer.addListener(cmUpdater);
    }

    /**
     * Does nothing.
     *
     * @deprecated since 11.1, unused
     */
    @Deprecated
    protected void activateRepository(String repositoryName) {
        // nothing
    }

    protected void uninstall() {
        if (!installed) {
            return;
        }
        // temporary log to help diagnostics
        log.info("Total commits during server life: " + transactionMonitor.getTotalCommits());
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
        MBeanServer mbs = Framework.getService(ServerLocator.class).lookupServer();
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
