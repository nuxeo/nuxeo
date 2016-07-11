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
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.management.jtajca.Defaults;
import org.nuxeo.ecm.core.management.jtajca.ConnectionPoolMonitor;
import org.nuxeo.ecm.core.management.jtajca.TransactionMonitor;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainerListener;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.metrics.MetricsServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Component used to install/uninstall the monitors (transaction and connections).
 *
 * @since 5.6
 */
public class DefaultMonitorComponent extends DefaultComponent {

    private final ConnectionManagerUpdater cmUpdater = new ConnectionManagerUpdater();

    private class ConnectionManagerUpdater implements NuxeoContainerListener {
        @Override
        public void handleNewConnectionManager(String name, AbstractConnectionManager cm) {
            ConnectionPoolMonitor monitor = new DefaultConnectionPoolMonitor(name, cm);
            monitor.install();
            poolConnectionMonitors.put(name, monitor);
        }

        @Override
        public void handleConnectionManagerReset(String name, AbstractConnectionManager cm) {
            DefaultConnectionPoolMonitor monitor = (DefaultConnectionPoolMonitor) poolConnectionMonitors.get(name);
            monitor.handleNewConnectionManager(cm);
        }

        @Override
        public void handleConnectionManagerDispose(String name, AbstractConnectionManager mgr) {
            ConnectionPoolMonitor monitor = poolConnectionMonitors.remove(name);
            monitor.uninstall();
        }

    }

    protected final Log log = LogFactory.getLog(DefaultMonitorComponent.class);

    protected CoreSessionMonitor coreSessionMonitor;

    protected TransactionMonitor transactionMonitor;

    protected Map<String, ConnectionPoolMonitor> poolConnectionMonitors = new HashMap<String, ConnectionPoolMonitor>();

    // don't use activate, it would be too early
    @Override
    public void applicationStarted(ComponentContext context) {
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
        return ((MetricsServiceImpl) Framework.getRuntime().getComponent(MetricsService.class.getName())).getApplicationStartedOrder() + 1;
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

        TransactionHelper.runInTransaction(this::installPoolMonitors);
    }

    protected void installPoolMonitors() {
        NuxeoContainer.addListener(cmUpdater);
        NuxeoException errors = new NuxeoException("Cannot install pool monitors");
        try {
            LoginContext loginContext = Framework.login();
            try {
                RepositoryService repositoryService = Framework.getService(RepositoryService.class);
                for (String name : repositoryService.getRepositoryNames()) {
                    try (CoreSession session = CoreInstance.openCoreSession(name)) {
                        // nothing
                    } catch (NuxeoException e) {
                        e.addInfo("For repository: " + name);
                        errors.addSuppressed(e);
                    }
                }
            } finally {
                loginContext.logout();
            }
        } catch (LoginException e) {
            errors.addSuppressed(e);
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
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

    protected static ObjectInstance bind(Object managed) {
        return bind(managed, "default");
    }

    protected static ObjectInstance bind(Class<?> itf, Object managed) {
        return bind(itf, managed, "default");
    }

    protected static ObjectInstance bind(Object managed, String name) {
        return bind(managed.getClass().getInterfaces()[0], managed, name);
    }

    protected static ObjectInstance bind(Class<?> itf, Object managed, String name) {
        MBeanServer mbs = Framework.getLocalService(ServerLocator.class).lookupServer();
        name = Defaults.instance.name(itf, name);
        try {
            return mbs.registerMBean(managed, new ObjectName(name));
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                | MalformedObjectNameException e) {
            throw new UnsupportedOperationException("Cannot bind " + managed + " on " + name, e);
        }
    }

    protected static void unbind(ObjectInstance instance) {
        MBeanServer mbs = Framework.getLocalService(ServerLocator.class).lookupServer();
        try {
            mbs.unregisterMBean(instance.getObjectName());
        } catch (MBeanRegistrationException | InstanceNotFoundException e) {
            throw new UnsupportedOperationException("Cannot unbind " + instance, e);
        }
    }

}
