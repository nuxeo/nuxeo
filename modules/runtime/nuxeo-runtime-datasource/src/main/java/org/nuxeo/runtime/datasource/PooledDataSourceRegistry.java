/*
 * (C) Copyright 2012-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.datasource;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.apache.commons.dbcp2.managed.TransactionRegistry;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class PooledDataSourceRegistry {

    protected final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    protected final Map<String, DataSource> dataSourcesNoSharing = new ConcurrentHashMap<>();

    public <T> T getDataSource(String name, Class<T> type, boolean noSharing) {
        Map<String, DataSource> map = noSharing ? dataSourcesNoSharing : dataSources;
        return type.cast(map.get(name));
    }

    public void registerPooledDataSource(String name, Map<String, String> properties) {
        dataSources.computeIfAbsent(name, k -> createPooledDataSource(properties, false));
        dataSourcesNoSharing.computeIfAbsent(name, k -> createPooledDataSource(properties, true));
    }

    /**
     * A {@link BasicManagedDataSource} that can configure its internal {@link XADataSource}.
     *
     * @since 11.1
     */
    public static class ConfigurableManagedDataSource extends BasicManagedDataSource {

        private static final Logger log = LogManager.getLogger(ConfigurableManagedDataSource.class);

        protected final Map<String, String> properties;

        public ConfigurableManagedDataSource(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        protected ConnectionFactory createConnectionFactory() throws SQLException {
            ConnectionFactory connectionFactory = super.createConnectionFactory();
            if (getXADataSource() == null) {
                // fix for https://issues.apache.org/jira/browse/DBCP-564
                // in non-XA mode, the transactionRegistry computed by super isn't enough,
                // we need one that includes the JTA TransactionSynchronizationRegistry
                try {
                    TransactionRegistry transactionRegistry = new TransactionRegistry(getTransactionManager(),
                            getTransactionSynchronizationRegistry());
                    FieldUtils.writeField(this, "transactionRegistry", transactionRegistry, true);
                    FieldUtils.writeField(connectionFactory, "transactionRegistry", transactionRegistry, true);
                } catch (IllegalAccessException e) {
                    throw new RuntimeServiceException(e);
                }
            }
            configureXADataSource(getXaDataSourceInstance());
            return connectionFactory;
        }

        // initialize the XADataSource through JavaBeans
        protected void configureXADataSource(XADataSource xaDataSource) {
            if (xaDataSource != null) {
                try {
                    BeanUtils.populate(xaDataSource, properties);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error(e, e);
                }
            }
        }
    }

    public BasicManagedDataSource createPooledDataSource(Map<String, String> properties, boolean noSharing) {
        // compatibility with previous Geronimo Connection configuration
        properties.computeIfAbsent("minTotal", k -> properties.get("minPoolSize"));
        properties.computeIfAbsent("maxTotal", k -> properties.get("maxPoolSize"));
        properties.computeIfAbsent("maxWaitMillis", k -> properties.get("blockingTimeoutMillis"));
        // compatibility aliases for username
        properties.computeIfAbsent("username", k -> defaultString( //
                properties.get("user"), //
                properties.get("User")));
        // JavaBeans sucks
        properties.computeIfAbsent("XADataSource", k -> properties.get("xaDataSource"));
        properties.computeIfAbsent("URL", k -> properties.get("url"));

        BasicManagedDataSource ds = new ConfigurableManagedDataSource(properties);

        // properties for which the DBCP default is not suitable
        ds.setMaxWaitMillis(1000);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        // populate datasource via JavaBeans properties
        try {
            BeanUtils.populate(ds, properties);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeServiceException(e);
        }
        // populate connection properties
        properties.forEach(ds::addConnectionProperty);

        // transaction management
        TransactionManager transactionManager;
        TransactionSynchronizationRegistry transactionSynchronizationRegistry;
        if (noSharing) {
            // pretend we never have a transaction
            transactionManager = new TransactionManagerWithoutTransaction();
            transactionSynchronizationRegistry = null;
        } else {
            try {
                transactionManager = TransactionHelper.lookupTransactionManager();
                transactionSynchronizationRegistry = TransactionHelper.lookupSynchronizationRegistry();
            } catch (NamingException e) {
                throw new RuntimeServiceException(e);
            }
        }
        ds.setTransactionManager(transactionManager);
        ds.setTransactionSynchronizationRegistry(transactionSynchronizationRegistry);

        return ds;
    }

    protected void unregisterPooledDataSource(String name) {
        dataSources.remove(name);
        dataSourcesNoSharing.remove(name);
    }

    public void createAlias(String name, DataSource ds) {
        // alias noSharing version too
        for (Entry<String, DataSource> es : dataSources.entrySet()) {
            if (es.getValue() == ds) {
                DataSource noSharingDs = dataSourcesNoSharing.get(es.getKey());
                if (noSharingDs != null) {
                    dataSourcesNoSharing.put(name, noSharingDs);
                }
                break;
            }
        }
        dataSources.put(name, ds);
    }

    public void removeAlias(String name) {
        unregisterPooledDataSource(name);
    }

    /**
     * Transaction Manager that is never in a transaction and doesn't allow starting one.
     *
     * @since 11.1
     */
    public static class TransactionManagerWithoutTransaction implements TransactionManager {

        @Override
        public Transaction getTransaction() {
            return null;
        }

        @Override
        public int getStatus() {
            return Status.STATUS_NO_TRANSACTION;
        }

        @Override
        public void setTransactionTimeout(int seconds) {
            // nothing
        }

        @Override
        public void begin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commit() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rollback() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resume(Transaction transaction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRollbackOnly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Transaction suspend() {
            throw new UnsupportedOperationException();
        }
    }

}
