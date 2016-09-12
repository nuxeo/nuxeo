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

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
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

import org.apache.commons.logging.LogFactory;
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

public class PooledDataSourceFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context ctx, Hashtable<?, ?> environment) {
        class NuxeoDataSource extends TranqlDataSource implements PooledDataSource {

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
        Reference ref = (Reference) obj;
        ManagedConnectionFactory mcf;
        ConnectionManagerWrapper cm;
        try {
            mcf = createFactory(ref, ctx);
            cm = createManager(ref, ctx);
        } catch (ResourceException | NamingException e) {
            throw new RuntimeException(e);
        }
        return new NuxeoDataSource(mcf, cm);
    }

    protected ConnectionManagerWrapper createManager(Reference ref, Context ctx) throws ResourceException {
        NuxeoConnectionManagerConfiguration config = NuxeoConnectionManagerFactory.getConfig(ref);
        String className = ref.getClassName();
        config.setXAMode(XADataSource.class.getName().equals(className));
        return NuxeoContainer.initConnectionManager(config);
    }

    protected ManagedConnectionFactory createFactory(Reference ref, Context ctx) throws NamingException,
            InvalidPropertyException {
        String className = ref.getClassName();
        if (XADataSource.class.getName().equals(className)) {
            String user = refAttribute(ref, "User", "");
            String password = refAttribute(ref, "Password", "");
            String name = refAttribute(ref, "dataSourceJNDI", null);
            XADataSource ds = NuxeoContainer.lookup(name, XADataSource.class);
            XADataSourceWrapper wrapper = new XADataSourceWrapper(ds);
            wrapper.setUserName(user);
            wrapper.setPassword(password);
            return wrapper;
        }
        if (javax.sql.DataSource.class.getName().equals(className)) {
            String user = refAttribute(ref, "username", "");
            if (user.isEmpty()) {
                user = refAttribute(ref, "user", "");
                if (!user.isEmpty()) {
                    LogFactory.getLog(PooledDataSourceFactory.class).warn(
                            "wrong attribute 'user' in datasource descriptor, should use 'username' instead");
                }
            }
            String password = refAttribute(ref, "password", "");
            String dsname = refAttribute(ref, "dataSourceJNDI", "");
            if (!dsname.isEmpty()) {
                javax.sql.DataSource ds = NuxeoContainer.lookup(dsname, DataSource.class);
                LocalDataSourceWrapper wrapper = new LocalDataSourceWrapper(ds);
                wrapper.setUserName(user);
                wrapper.setPassword(password);
                return wrapper;
            }
            String name = refAttribute(ref, "driverClassName", null);
            String url = refAttribute(ref, "url", null);
            String sqlExceptionSorter = refAttribute(ref, "sqlExceptionSorter",
                    DatasourceExceptionSorter.class.getName());
            boolean commitBeforeAutocommit = Boolean.valueOf(refAttribute(ref, "commitBeforeAutocommit", "true")).booleanValue();
            JdbcConnectionFactory factory = new JdbcConnectionFactory();
            factory.setDriver(name);
            factory.setUserName(user);
            factory.setPassword(password);
            factory.setConnectionURL(url);
            factory.setExceptionSorterClass(sqlExceptionSorter);
            factory.setCommitBeforeAutocommit(commitBeforeAutocommit);
            return factory;
        }
        throw new IllegalArgumentException("unsupported class " + className);
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

                Connection physicalConnection() throws ResourceException {
                    return physicalConnection;
                }

                @Override
                protected void localTransactionStart(boolean isSPI) throws ResourceException {
                    Connection c = physicalConnection();
                    try {
                        c.setAutoCommit(false);
                    } catch (SQLException e) {
                        throw new LocalTransactionException("Unable to disable autoCommit", e);
                    }
                    super.localTransactionStart(isSPI);
                }

                @Override
                protected void localTransactionCommit(boolean isSPI) throws ResourceException {
                    Connection c = physicalConnection();
                    try {
                        if (commitBeforeAutoCommit) {
                            c.commit();
                        }
                    } catch (SQLException e) {
                        try {
                            c.rollback();
                        } catch (SQLException e1) {
                            if (log != null) {
                                e.printStackTrace(log);
                            }
                        }
                        throw new LocalTransactionException("Unable to commit", e);
                    } finally {
                        try {
                            c.setAutoCommit(true);
                        } catch (SQLException e) {
                            throw new ResourceAdapterInternalException("Unable to enable autoCommit after rollback", e);
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
            Connection sqlConnection = getPhysicalConnection(subject, credentialExtractor);
            return new ManagedJDBCConnection(this, sqlConnection, credentialExtractor, exceptionSorter, commitBeforeAutocommit);
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
        public ManagedConnection matchManagedConnections(Set set, Subject subject, ConnectionRequestInfo connectionRequestInfo)
                throws ResourceException {
            for (Iterator<Object> i = set.iterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof ManagedConnectionHandle) {
                    ManagedConnectionHandle mc = (ManagedConnectionHandle) o;
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
                this.driver = driverClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new InvalidPropertyException("Unable to load driver class: " + driver, e);
            } catch (InstantiationException e) {
                throw new InvalidPropertyException("Unable to instantiate driver class: " + driver, e);
            } catch (IllegalAccessException e) {
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
                exceptionSorter = clazz.newInstance();
            } catch (ClassNotFoundException e) {
                throw new InvalidPropertyException("Unable to load class: " + className, e);
            } catch (IllegalAccessException e) {
                throw new InvalidPropertyException("Unable to instantiate class: " + className, e);
            } catch (InstantiationException e) {
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

        private Class<?> loadClass(String name) throws ClassNotFoundException {
            // first try the TCL, then the classloader that defined us
            ClassLoader cl = getContextClassLoader();
            if (cl != null) {
                try {
                    return cl.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // ignore this
                }
            }
            return Class.forName(name);
        }

        private ClassLoader getContextClassLoader() {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    try {
                        return Thread.currentThread().getContextClassLoader();
                    } catch (SecurityException e) {
                        return null;
                    }
                }
            });
        }
    }

    protected String refAttribute(Reference ref, String key, String defvalue) {
        RefAddr addr = ref.get(key);
        if (addr == null) {
            if (defvalue == null) {
                throw new IllegalArgumentException(key + " address is mandatory");
            }
            return defvalue;
        }
        return (String) addr.getContent();
    }

}
