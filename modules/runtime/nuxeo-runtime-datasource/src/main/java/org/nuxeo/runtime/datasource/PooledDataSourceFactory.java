/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.datasource;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.LocalTransactionException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.ResourceAllocationException;
import javax.security.auth.Subject;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.ClassLoaderUtil;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.datasource.PooledDataSourceRegistry.PooledDataSource;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainer.ConnectionManagerWrapper;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.tranql.connector.AbstractManagedConnection;
import org.tranql.connector.CredentialExtractor;
import org.tranql.connector.ExceptionSorter;
import org.tranql.connector.ManagedConnectionHandle;
import org.tranql.connector.UserPasswordManagedConnectionFactory;
import org.tranql.connector.jdbc.AutocommitSpecCompliant;
import org.tranql.connector.jdbc.ConnectionHandle;
import org.tranql.connector.jdbc.KnownSQLStateExceptionSorter;
import org.tranql.connector.jdbc.LocalDataSourceWrapper;
import org.tranql.connector.jdbc.TranqlDataSource;
import org.tranql.connector.jdbc.XADataSourceWrapper;

public class PooledDataSourceFactory {

    public static class NuxeoDataSource extends TranqlDataSource implements PooledDataSource {

        protected ConnectionManagerWrapper wrapper;

        public NuxeoDataSource(ManagedConnectionFactory mcf, ConnectionManagerWrapper wrapper) {
            super(mcf, wrapper);
            this.wrapper = wrapper;
        }

        @Override
        public void dispose() {
            wrapper.dispose();
        }

        @Override
        public Connection getConnection(boolean noSharing) throws SQLException {
            if (!noSharing) {
                return getConnection();
            }
            wrapper.getManager().enterNoSharing();
            try {
                return getConnection();
            } finally {
                wrapper.getManager().exitNoSharing();
            }
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("not yet available");
        }
    }

    public PooledDataSource createPooledDataSource(Map<String, String> properties) {
        ManagedConnectionFactory mcf;
        ConnectionManagerWrapper cm;
        try {
            mcf = createFactory(properties);
            cm = createManager(properties);
        } catch (ResourceException | NamingException e) {
            throw new RuntimeServiceException(e);
        }
        return new NuxeoDataSource(mcf, cm);
    }

    protected ConnectionManagerWrapper createManager(Map<String, String> properties) {
        NuxeoConnectionManagerConfiguration config = NuxeoConnectionManagerFactory.getConfig(properties);
        boolean isXA = properties.containsKey("xaDataSource");
        config.setXAMode(isXA);
        return NuxeoContainer.initConnectionManager(config);
    }

    protected ManagedConnectionFactory createFactory(Map<String, String> properties)
            throws NamingException, InvalidPropertyException {
        String xaDataSourceClassName = properties.get("xaDataSource");
        if (!isBlank(xaDataSourceClassName)) {
            XADataSource ds = createXADataSource(xaDataSourceClassName, properties);
            String user = getProperty(properties, "User", "");
            String password = getProperty(properties, "Password", "");
            XADataSourceWrapper wrapper = new XADataSourceWrapper(ds);
            wrapper.setUserName(user);
            wrapper.setPassword(password);
            return wrapper;
        }

        // datasource username / password
        String user = getProperty(properties, "username", "");
        if (user.isEmpty()) {
            user = getProperty(properties, "user", "");
            if (!user.isEmpty()) {
                LogFactory.getLog(PooledDataSourceFactory.class)
                .warn("wrong attribute 'user' in datasource descriptor, should use 'username' instead");
            }
        }
        String password = getProperty(properties, "password", "");

        // datasource from JNDI lookup (unused?)
        String dataSourceName = properties.get("dataSource");
        if (!isBlank(dataSourceName)) {
            String dsname = DataSourceHelper.getDataSourceJNDIName(dataSourceName);
            DataSource ds = NuxeoContainer.lookup(dsname, DataSource.class);
            LocalDataSourceWrapper wrapper = new LocalDataSourceWrapper(ds);
            wrapper.setUserName(user);
            wrapper.setPassword(password);
            return wrapper;
        }

        // datasource from driver class name
        String driver = getProperty(properties, "driverClassName", null);
        String url = getProperty(properties, "url", null);
        String sqlExceptionSorter = getProperty(properties, "sqlExceptionSorter",
                DatasourceExceptionSorter.class.getName());
        boolean commitBeforeAutocommit = Boolean.parseBoolean(
                getProperty(properties, "commitBeforeAutocommit", "true"));
        JdbcConnectionFactory factory = new JdbcConnectionFactory();
        factory.setDriver(driver);
        factory.setUserName(user);
        factory.setPassword(password);
        factory.setConnectionURL(url);
        factory.setExceptionSorterClass(sqlExceptionSorter);
        factory.setCommitBeforeAutocommit(commitBeforeAutocommit);
        return factory;
    }

    protected String getProperty(Map<String, String> props, String key, String defvalue) {
        String value = props.get(key);
        if (value == null) {
            if (defvalue == null) {
                throw new IllegalArgumentException(key + " address is mandatory");
            }
            return defvalue;
        }
        return value;
    }

    protected XADataSource createXADataSource(String className, Map<String, String> properties) {
        XADataSource ds;
        try {
            Class<?> klass = ClassLoaderUtil.loadClass(className, //
                    getClass().getClassLoader(), //
                    Thread.currentThread().getContextClassLoader());
            ds = (XADataSource) klass.getConstructor().newInstance();
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            throw new RuntimeServiceException(className, e);
        }
        // initialize properties
        for (Entry<String, String> es : properties.entrySet()) {
            String name = es.getKey();
            String value = es.getValue();
            try {
                BeanUtils.setProperty(ds, name, value);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(name, e);
            }
        }
        return ds;
    }

    static class JdbcConnectionFactory implements UserPasswordManagedConnectionFactory, AutocommitSpecCompliant {
        private static final long serialVersionUID = 4317141492511322929L;
        private Driver driver;
        private String url;
        private String user;
        private String password;
        private ExceptionSorter exceptionSorter = new KnownSQLStateExceptionSorter();
        private boolean commitBeforeAutocommit = false;

        private PrintWriter log;

        @Override
        public Object createConnectionFactory() throws ResourceException {
            throw new NotSupportedException("ConnectionManager is required");
        }

        @Override
        public Object createConnectionFactory(ConnectionManager connectionManager) throws ResourceException {
            return new TranqlDataSource(this, connectionManager);
        }

        @Override
        public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {

            class ManagedJDBCConnection extends AbstractManagedConnection<Connection, ConnectionHandle> {
                final CredentialExtractor credentialExtractor;
                final LocalTransactionImpl localTx;
                final LocalTransactionImpl localClientTx;
                final boolean commitBeforeAutoCommit;

                Exception fatalError;

                ManagedJDBCConnection(UserPasswordManagedConnectionFactory mcf, Connection physicalConnection,
                        CredentialExtractor credentialExtractor, ExceptionSorter exceptionSorter, boolean commitBeforeAutoCommit) {
                    super(mcf, physicalConnection, exceptionSorter);
                    this.credentialExtractor = credentialExtractor;
                    localTx = new LocalTransactionImpl(true);
                    localClientTx = new LocalTransactionImpl(false);
                    this.commitBeforeAutoCommit = commitBeforeAutoCommit;
                }

                @Override
                public boolean matches(ManagedConnectionFactory mcf, Subject subject, ConnectionRequestInfo connectionRequestInfo)
                        throws ResourceAdapterInternalException {
                    return credentialExtractor.matches(subject, connectionRequestInfo, (UserPasswordManagedConnectionFactory) mcf);
                }

                @Override
                public LocalTransaction getClientLocalTransaction() {
                    return localClientTx;
                }

                @Override
                public LocalTransaction getLocalTransaction() throws ResourceException {
                    return localTx;
                }

                @Override
                protected void localTransactionStart(boolean isSPI) throws ResourceException {
                    try {
                        physicalConnection.setAutoCommit(false);
                    } catch (SQLException e) {
                        throw new LocalTransactionException("Unable to disable autoCommit", e);
                    }
                    super.localTransactionStart(isSPI);
                }

                @Override
                protected void localTransactionCommit(boolean isSPI) throws ResourceException {
                    try {
                        if (commitBeforeAutoCommit) {
                            physicalConnection.commit();
                        }
                    } catch (SQLException e) {
                        try {
                            physicalConnection.rollback();
                        } catch (SQLException e1) {
                            if (log != null) {
                                e.printStackTrace(log);
                            }
                        }
                        throw new LocalTransactionException("Unable to commit", e);
                    } finally {
                        try {
                            physicalConnection.setAutoCommit(true);
                        } catch (SQLException e) {
                            // don't rethrow inside finally
                            LogFactory.getLog(PooledDataSourceFactory.class)
                                      .error("Unable to enable autoCommit after rollback", e);
                        }
                    }
                    super.localTransactionCommit(isSPI);
                }

                @Override
                protected void localTransactionRollback(boolean isSPI) throws ResourceException {
                    Connection c = physicalConnection;
                    try {
                        c.rollback();
                    } catch (SQLException e) {
                        throw new LocalTransactionException("Unable to rollback", e);
                    }
                    super.localTransactionRollback(isSPI);
                    try {
                        c.setAutoCommit(true);
                    } catch (SQLException e) {
                        throw new ResourceAdapterInternalException("Unable to enable autoCommit after rollback", e);
                    }
                }

                @Override
                public XAResource getXAResource() throws ResourceException {
                    throw new NotSupportedException("XAResource not available from a LocalTransaction connection");
                }

                @Override
                protected void closePhysicalConnection() throws ResourceException {
                    Connection c = physicalConnection;
                    try {
                        c.close();
                    } catch (SQLException e) {
                        throw new ResourceAdapterInternalException("Error attempting to destroy managed connection", e);
                    }
                }

                @Override
                public ManagedConnectionMetaData getMetaData() throws ResourceException {
                    throw new NotSupportedException("no metadata available yet");
                }

                @Override
                public void connectionError(Exception e) {
                    if (fatalError != null) {
                        return;
                    }

                    if (isFatal(e)) {
                        fatalError = e;
                        if (exceptionSorter.rollbackOnFatalException()) {
                            if (TransactionHelper.isTransactionActive()) {
                                // will roll-back at tx end through #localTransactionRollback
                                TransactionHelper.setTransactionRollbackOnly();
                            } else {
                                attemptRollback();
                            }
                        }
                    }
                }

                @Override
                public void cleanup() throws ResourceException {
                    super.cleanup();
                    if (fatalError != null) {
                        ResourceException error = new ResourceException(String.format("fatal error occurred on %s, destroying", this), fatalError);
                        LogFactory.getLog(ManagedJDBCConnection.class).warn(error.getMessage(), error.getCause());
                        throw error;
                    }
                }

                protected boolean isFatal(Exception e) {
                    if (exceptionSorter.isExceptionFatal(e)) {
                        return true;
                    }
                    try {
                        return !physicalConnection.isValid(10);
                    } catch (SQLException cause) {
                        return false; // could not state
                    } catch (LinkageError cause) {
                        return false; // not compliant JDBC4 driver
                    }
                }

                @Override
                protected void attemptRollback() {
                    try {
                        physicalConnection.rollback();
                    } catch (SQLException e) {
                        // ignore.... presumably the connection is actually dead
                    }
                }

                @Override
                public String toString() {
                    return super.toString() + ". jdbc=" + physicalConnection;
                }
            }

            CredentialExtractor credentialExtractor = new CredentialExtractor(subject, connectionRequestInfo, this);
            return new ManagedJDBCConnection(this, getPhysicalConnection(subject, credentialExtractor),
                    credentialExtractor, exceptionSorter, commitBeforeAutocommit);
        }

        protected Connection getPhysicalConnection(Subject subject, CredentialExtractor credentialExtractor) throws ResourceException {
            try {
                if (!driver.acceptsURL(url)) {
                    throw new ResourceAdapterInternalException("JDBC Driver cannot handle url: " + url);
                }
            } catch (SQLException e) {
                throw new ResourceAdapterInternalException("JDBC Driver rejected url: " + url);
            }

            Properties info = new Properties();
            String user = credentialExtractor.getUserName();
            if (user != null) {
                info.setProperty("user", user);
            }
            String password = credentialExtractor.getPassword();
            if (password != null) {
                info.setProperty("password", password);
            }
            try {
                return driver.connect(url, info);
            } catch (SQLException e) {
                throw new ResourceAllocationException("Unable to obtain physical connection to " + url, e);
            }
        }

        @Override
        public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set set, Subject subject, ConnectionRequestInfo connectionRequestInfo)
                throws ResourceException {
            for (@SuppressWarnings("unchecked") Iterator<Object> i = set.iterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof ManagedConnectionHandle) {
                    ManagedConnectionHandle<?,?> mc = (ManagedConnectionHandle<?,?>) o;
                    if (mc.matches(this, subject, connectionRequestInfo)) {
                        return mc;
                    }
                }
            }
            return null;
        }

        @Override
        public PrintWriter getLogWriter() {
            return log;
        }

        @Override
        public void setLogWriter(PrintWriter log) {
            this.log = log;
        }

        void setDriver(String driver) throws InvalidPropertyException {
            if (driver == null || driver.length() == 0) {
                throw new InvalidPropertyException("Empty driver class name");
            }
            try {
                @SuppressWarnings("unchecked")
                Class<Driver> driverClass = (Class<Driver>) Class.forName(driver);
                this.driver = driverClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                throw new InvalidPropertyException("Unable to load driver class: " + driver, e);
            } catch (ReflectiveOperationException e) {
                throw new InvalidPropertyException("Unable to instantiate driver class: " + driver, e);
            } catch (ClassCastException e) {
                throw new InvalidPropertyException("Class is not a " + Driver.class.getName() + ": " + driver, e);
            }
        }

        void setConnectionURL(String url) throws InvalidPropertyException {
            if (url == null || url.length() == 0) {
                throw new InvalidPropertyException("Empty connection URL");
            }
            this.url = url;
        }

        @Override
        public String getUserName() {
            return user;
        }

        void setUserName(String user) {
            this.user = user;
        }

        @Override
        public String getPassword() {
            return password;
        }

        void setPassword(String password) {
            this.password = password;
        }

        @Override
        public Boolean isCommitBeforeAutocommit() {
            return Boolean.valueOf(commitBeforeAutocommit);
        }

        void setCommitBeforeAutocommit(Boolean commitBeforeAutocommit) {
            this.commitBeforeAutocommit = commitBeforeAutocommit != null && commitBeforeAutocommit.booleanValue();
        }

        void setExceptionSorterClass(String className) throws InvalidPropertyException {
            if (className == null || className.length() == 0) {
                throw new InvalidPropertyException("Empty class name");
            }
            try {
                @SuppressWarnings("unchecked")
                Class<ExceptionSorter> clazz = (Class<ExceptionSorter>) Class.forName(className);
                exceptionSorter = clazz.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                throw new InvalidPropertyException("Unable to load class: " + className, e);
            } catch (ReflectiveOperationException e) {
                throw new InvalidPropertyException("Unable to instantiate class: " + className, e);
            } catch (ClassCastException e) {
                throw new InvalidPropertyException("Class is not a " + ExceptionSorter.class.getName() + ": " + driver, e);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof JdbcConnectionFactory) {
                JdbcConnectionFactory other = (JdbcConnectionFactory) obj;
                return url == other.url || url != null && url.equals(other.url);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return url == null ? 0 : url.hashCode();
        }

        @Override
        public String toString() {
            return "Pooled JDBC Driver Connection Factory [" + user + "@" + url + "]";
        }

    }

}
