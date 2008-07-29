/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * @author Florent Guillaume
 */
public class RepositoryImpl implements Repository {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RepositoryImpl.class);

    protected final SchemaManager schemaManager;

    private final RepositoryDescriptor repositoryDescriptor;

    // TODO check if really want something weak w.r.t. remoting
    /** The sessions in a weak hash map. Values are unused. A weak set really. */
    private final Map<SessionImpl, Object> sessions;

    private XADataSource xadatasource;

    // initialized at first login

    private boolean initialized;

    private Dialect dialect;

    private Model model;

    private SQLInfo sqlInfo;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor,
            SchemaManager schemaManager) throws StorageException {
        this.repositoryDescriptor = repositoryDescriptor;
        this.schemaManager = schemaManager;
        sessions = new WeakHashMap<SessionImpl, Object>();
        xadatasource = getXADataSource();
    }

    /*
     * ----- javax.resource.cci.ConnectionFactory -----
     */

    /**
     * Gets a new connection by logging in to the repository with default
     * credentials.
     *
     * @return the session
     * @throws StorageException
     */
    public SessionImpl getConnection() throws StorageException {
        return getConnection(null);
    }

    /**
     * Gets a new connection by logging in to the repository with given
     * connection information (credentials).
     *
     * @param connectionSpec the parameters to use to connnect
     * @return the session
     * @throws StorageException
     */
    public synchronized SessionImpl getConnection(ConnectionSpec connectionSpec)
            throws StorageException {
        assert connectionSpec == null ||
                connectionSpec instanceof ConnectionSpecImpl;

        Credentials credentials = connectionSpec == null ? null
                : ((ConnectionSpecImpl) connectionSpec).getCredentials();

        XAConnection xaconnection;
        try {
            xaconnection = xadatasource.getXAConnection();
        } catch (SQLException e) {
            throw new StorageException("Cannot get XAConnection", e);
        }

        if (!initialized) {
            initialize(xaconnection);
        }

        Mapper mapper = new Mapper(model, sqlInfo, xaconnection);

        if (!initialized) {
            // first connection, initialize the database
            // XXX must check existing tables in the database XXX
            // XXX create always for now
            mapper.createDatabase();
            initialized = true;
        }

        SessionImpl session = new SessionImpl(schemaManager, mapper,
                credentials);

        sessions.put(session, null);
        return session;
    }

    public ResourceAdapterMetaData getMetaData() {
        throw new UnsupportedOperationException();
    }

    public RecordFactory getRecordFactory() {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- javax.resource.Referenceable -----
     */

    private Reference reference;

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public Reference getReference() {
        return reference;
    }

    /*
     * ----- Repository -----
     */

    public synchronized void close() {
        for (SessionImpl session : sessions.keySet()) {
            if (session.isLive()) {
                try {
                    session.close();
                } catch (ResourceException e) {
                    log.error("Error closing session", e);
                }
            }
        }
        sessions.clear();
    }

    /*
     * ----- -----
     */

    private XADataSource getXADataSource() throws StorageException {

        /*
         * Instantiate the datasource.
         */

        String className = repositoryDescriptor.xaDataSourceName;
        Class<?> klass;
        try {
            klass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new StorageException("Unknown class: " + className, e);
        }
        Object instance;
        try {
            instance = klass.newInstance();
        } catch (Exception e) {
            throw new StorageException(
                    "Cannot instantiate class: " + className, e);
        }
        if (!(instance instanceof XADataSource)) {
            throw new StorageException("Not a XADataSource: " + className);
        }
        XADataSource xadatasource = (XADataSource) instance;

        /*
         * Set JavaBean properties.
         */

        Class<?>[] types = new Class[] { String.class };
        for (Entry<String, String> entry : repositoryDescriptor.properties.entrySet()) {
            String propertyName = entry.getKey();
            String methodName = "set" +
                    Character.toUpperCase(propertyName.charAt(0)) +
                    propertyName.substring(1);
            Method method;
            try {
                method = xadatasource.getClass().getMethod(methodName, types);
            } catch (Exception e) {
                log.error("Cannot get JavaBean method " + methodName +
                        " for class: " + className, e);
                continue;
            }
            try {
                method.invoke(xadatasource, new Object[] { entry.getValue() });
            } catch (Exception e) {
                log.error("Cannot call JavaBean method " + methodName +
                        " for class: " + className, e);
                continue;
            }
        }

        return xadatasource;
    }

    /**
     * Lazy initialization, to delay dialect detection until the first
     * connection is really needed.
     */
    private void initialize(XAConnection xaconnection) throws StorageException {
        log.debug("Initializing");
        dialect = getDialect(xaconnection);
        model = new Model(repositoryDescriptor, schemaManager);
        sqlInfo = new SQLInfo(model, dialect);
    }

    /**
     * Gets the {@code Dialect}, by connecting to the datasource to check what
     * database is used.
     *
     * @throws StorageException if a SQL connection problem occurs
     */
    private Dialect getDialect(XAConnection xaconnection)
            throws StorageException {
        Connection connection = null;
        String dbname;
        int dbmajor;
        try {
            connection = xaconnection.getConnection();
            DatabaseMetaData metadata = connection.getMetaData();
            dbname = metadata.getDatabaseProductName();
            dbmajor = metadata.getDatabaseMajorVersion();
        } catch (SQLException e) {
            throw new StorageException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new StorageException(
                            "Cannot get metadata for class: " +
                                    repositoryDescriptor.xaDataSourceName, e);
                }
            }
        }
        try {
            return DialectFactory.determineDialect(dbname, dbmajor);
        } catch (HibernateException e) {
            throw new StorageException("Cannot determine dialect for class: " +
                    repositoryDescriptor.xaDataSourceName, e);
        }
    }

}
