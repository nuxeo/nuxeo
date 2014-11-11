/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import javax.naming.NamingException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.management.jtajca.DatabaseConnectionMonitor;
import org.nuxeo.ecm.core.management.jtajca.Defaults;
import org.nuxeo.ecm.core.management.jtajca.StorageConnectionMonitor;
import org.nuxeo.ecm.core.management.jtajca.TransactionMonitor;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainerListener;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.metrics.MetricsServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component used to install/uninstall the monitors (transaction and
 * connections).
 *
 * @since 5.6
 */
public class DefaultMonitorComponent extends DefaultComponent {

    private final ConnectionManagerUpdater cmUpdater = new ConnectionManagerUpdater();

    private class ConnectionManagerUpdater implements NuxeoContainerListener {
        @Override
        public void handleNewConnectionManager(String name,
                AbstractConnectionManager cm) {
            StorageConnectionMonitor monitor = new DefaultStorageConnectionMonitor(
                    name, cm);
            monitor.install();
            storageConnectionMonitors.put(name, monitor);
        }

        @Override
        public void handleConnectionManagerReset(String name,
                AbstractConnectionManager cm) {
            DefaultStorageConnectionMonitor monitor = (DefaultStorageConnectionMonitor) storageConnectionMonitors.get(name);
            monitor.handleNewConnectionManager(cm);
        }

        @Override
        public void handleConnectionManagerDispose(String name, AbstractConnectionManager mgr) {
            StorageConnectionMonitor monitor = storageConnectionMonitors.remove(name);
            monitor.uninstall();
        }

    }

    protected final Log log = LogFactory.getLog(DefaultMonitorComponent.class);

    protected CoreSessionMonitor coreSessionMonitor;

    protected TransactionMonitor transactionMonitor;

    protected Map<String, StorageConnectionMonitor> storageConnectionMonitors = new HashMap<String, StorageConnectionMonitor>();

    protected Map<String, DatabaseConnectionMonitor> databaseConnectionMonitors = new HashMap<String, DatabaseConnectionMonitor>();

    // don't use activate, it would be too early
    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        uninstall();
        install();
    }

    @Override
    public int getApplicationStartedOrder() {
        // should deploy after metrics service
        return ((MetricsServiceImpl) Framework.getRuntime().getComponent(
                MetricsService.class.getName())).getApplicationStartedOrder() + 1;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        uninstall();
        super.deactivate(context);
    }

    protected boolean installed;

    protected void install() throws Exception {
        installed = true;

        coreSessionMonitor = new DefaultCoreSessionMonitor();
        coreSessionMonitor.install();

        transactionMonitor = new DefaultTransactionMonitor();
        transactionMonitor.install();

        try {
            installDatabaseStorageMonitors();
        } catch (ClientException cause) {
            log.warn("Cannot install database monitors", cause);
        }
        try {
            installRepositoryStorageMonitors();
        } catch (ClientException cause) {
            log.warn("Cannot install storage monitors", cause);
        }

    }

    protected void installRepositoryStorageMonitors() throws ClientException, LoginException {
        NuxeoContainer.addListener(cmUpdater);
        ClientException errors = new ClientException("Cannot install repository storage monitors");
        LoginContext loginContext = Framework.login();
        try {
            for (String name : Framework.getLocalService(
                    RepositoryService.class).getRepositoryNames()) {
                try (CoreSession session = CoreInstance.openCoreSession(name)) {
                    ;
                } catch (ClientException cause) {
                    errors.addSuppressed(cause);
                }
            }
        } finally {
            loginContext.logout();
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    protected void installDatabaseStorageMonitors() throws ClientException {
        ClientException errors = new ClientException("Cannot install database storage monitors");
        Map<String, DataSource> dsByName;
        try {
            dsByName = DataSourceHelper.getDatasources();
        } catch (NamingException cause) {
            errors.addSuppressed(cause);
            throw errors;
        }
        for (Map.Entry<String, DataSource> dsEntry : dsByName.entrySet()) {
            String name = dsEntry.getKey();
            DataSource ds = dsEntry.getValue();
            DatabaseConnectionMonitor monitor = null;
            if (ds instanceof org.apache.commons.dbcp.BasicDataSource) {
                monitor = new CommonsDatabaseConnectionMonitor(name,
                        (org.apache.commons.dbcp.BasicDataSource) ds);
            } else if (ds instanceof org.apache.tomcat.dbcp.dbcp.BasicDataSource) {
                monitor = new TomcatDatabaseConnectionMonitor(name,
                        (org.apache.tomcat.dbcp.dbcp.BasicDataSource) ds);
            } else {
                continue;
            }
            monitor.install();
            databaseConnectionMonitors.put(name, monitor);
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    /**
     * Make sure we open the repository, to initialize its connection manager.
     */
    protected void activateRepository(String repositoryName)
            throws ClientException {
        try (CoreSession session = CoreInstance.openCoreSessionSystem(repositoryName)) {
            // do nothing, just open and close
        }
    }

    protected void uninstall() throws JMException {
        if (!installed) {
            return;
        }
        installed = false;
        NuxeoContainer.removeListener(cmUpdater);
        for (StorageConnectionMonitor storage : storageConnectionMonitors.values()) {
            storage.uninstall();
        }
        for (DatabaseConnectionMonitor ds : databaseConnectionMonitors.values()) {
            ds.uninstall();
        }
        coreSessionMonitor.uninstall();
        transactionMonitor.uninstall();
        storageConnectionMonitors.clear();
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

    protected static ObjectInstance bind(Class<?> itf, Object managed,
            String name) {
        MBeanServer mbs = Framework.getLocalService(ServerLocator.class).lookupServer();
        name = Defaults.instance.name(itf, name);
        try {
            return mbs.registerMBean(managed, new ObjectName(name));
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException | MalformedObjectNameException e) {
            throw new UnsupportedOperationException("Cannot bind " + managed
                    + " on " + name, e);
        }
    }

    protected static void unbind(ObjectInstance instance) {
        MBeanServer mbs = Framework.getLocalService(ServerLocator.class).lookupServer();
        try {
            mbs.unregisterMBean(instance.getObjectName());
        } catch (MBeanRegistrationException | InstanceNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Cannot unbind " + instance, e);
        }
    }
}
