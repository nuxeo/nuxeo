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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapterInternalException;
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
import org.tranql.connector.AbstractManagedConnection;
import org.tranql.connector.ManagedConnectionHandle;
import org.tranql.connector.jdbc.ConnectionHandle;
import org.tranql.connector.jdbc.JDBCDriverMCF;
import org.tranql.connector.jdbc.LocalDataSourceWrapper;
import org.tranql.connector.jdbc.TranqlDataSource;
import org.tranql.connector.jdbc.XADataSourceWrapper;

public class PooledDataSourceFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context ctx, Hashtable<?, ?> environment) {
        class PatchedDataSource extends TranqlDataSource implements PooledDataSource {

            protected ConnectionManagerWrapper wrapper;

            public PatchedDataSource(ManagedConnectionFactory mcf, ConnectionManagerWrapper wrapper) {
                super(mcf, wrapper);
                this.wrapper = wrapper;
            }

            @Override
            public void dispose() {
                wrapper.dispose();
            }

            @Override
            public Connection getConnection() throws SQLException {
                class ValidationErrorHandle implements ManagedConnectionHandle<Connection, ConnectionHandle> {

                    final AbstractManagedConnection<Connection, ConnectionHandle> association;

                    ValidationErrorHandle(AbstractManagedConnection<Connection, ConnectionHandle> target) {
                        association = target;
                    }

                    @Override
                    public Connection getPhysicalConnection() {
                        return association.getPhysicalConnection();
                    }

                    @Override
                    public void connectionClosed(ConnectionHandle handle) {
                        association.connectionClosed(handle);
                    }

                    @Override
                    public void connectionError(Exception error) {
                        try {
                            if (!association.getPhysicalConnection().isValid(Long.valueOf(TimeUnit.SECONDS.toMillis(10)).intValue())) {
                                association.connectionError(new SQLException("Connection error", "08003", error));
                            }
                        } catch (SQLException cause) {
                            association.connectionError(new SQLException("Connection error", "08003", error));
                        }
                        association.connectionError(error);
                    }

                    @Override
                    public boolean matches(ManagedConnectionFactory mcf, Subject subject, ConnectionRequestInfo connectionRequestInfo)
                            throws ResourceAdapterInternalException {
                        return association.matches(mcf, subject, connectionRequestInfo);
                    }

                    @Override
                    public LocalTransaction getClientLocalTransaction() {
                        return association.getClientLocalTransaction();
                    }

                    @Override
                    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
                        return association.getConnection(subject, cxRequestInfo);
                    }

                    @Override
                    public void destroy() throws ResourceException {
                        association.destroy();
                    }

                    @Override
                    public void cleanup() throws ResourceException {
                        association.cleanup();
                    }

                    @Override
                    public void associateConnection(Object connection) throws ResourceException {
                        association.associateConnection(connection);
                    }

                    @Override
                    public void addConnectionEventListener(ConnectionEventListener listener) {
                        association.addConnectionEventListener(listener);
                    }

                    @Override
                    public void removeConnectionEventListener(ConnectionEventListener listener) {
                        association.removeConnectionEventListener(listener);
                    }

                    @Override
                    public XAResource getXAResource() throws ResourceException {
                        return association.getXAResource();
                    }

                    @Override
                    public LocalTransaction getLocalTransaction() throws ResourceException {
                        return association.getLocalTransaction();
                    }

                    @Override
                    public ManagedConnectionMetaData getMetaData() throws ResourceException {
                        return association.getMetaData();
                    }

                    @Override
                    public void setLogWriter(PrintWriter out) throws ResourceException {
                        association.setLogWriter(out);
                    }

                    @Override
                    public PrintWriter getLogWriter() throws ResourceException {
                        return association.getLogWriter();
                    }
                }

                ConnectionHandle handle = (ConnectionHandle) super.getConnection();
                handle.setAssociation(
                        new ValidationErrorHandle((AbstractManagedConnection<Connection, ConnectionHandle>) handle.getAssociation()));
                return handle;
            }

            @Override
            public Connection getConnection(String user, String password) throws SQLException {
                return super.getConnection(user, password);
            }

            @Override
            public Connection getConnection(boolean noSharing) throws SQLException {
                if (!noSharing) {
                    return getConnection();
                }
                wrapper.enterNoSharing();
                try {
                    return getConnection();
                } finally {
                    wrapper.exitNoSharing();
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
        return new PatchedDataSource(mcf, cm);
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
            JDBCDriverMCF factory = new JDBCDriverMCF();
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
