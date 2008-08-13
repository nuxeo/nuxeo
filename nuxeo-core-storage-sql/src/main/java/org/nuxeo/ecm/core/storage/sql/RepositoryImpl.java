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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.Reference;
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

    private final Collection<SessionImpl> sessions;

    private final Invalidators invalidators;

    private final BinaryManager binaryManager;

    private final XADataSource xadatasource;

    // initialized at first login

    private boolean initialized;

    private Dialect dialect;

    private Model model;

    private SQLInfo sqlInfo;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor,
            SchemaManager schemaManager) throws StorageException {
        this.repositoryDescriptor = repositoryDescriptor;
        this.schemaManager = schemaManager;
        sessions = new CopyOnWriteArrayList<SessionImpl>();
        invalidators = new Invalidators();
        xadatasource = getXADataSource();
        try {
            binaryManager = new BinaryManager(repositoryDescriptor);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    protected RepositoryDescriptor getRepositoryDescriptor() {
        return repositoryDescriptor;
    }

    protected BinaryManager getBinaryManager() {
        return binaryManager;
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

        if (!initialized) {
            initialize();
        }

        Mapper mapper = new Mapper(model, sqlInfo, xadatasource);

        if (!initialized) {
            // first connection, initialize the database
            mapper.createDatabase();
            initialized = true;
        }

        SessionImpl session = new SessionImpl(this, schemaManager, mapper,
                invalidators, credentials);

        sessions.add(session);
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
        for (SessionImpl session : sessions) {
            if (!session.isLive()) {
                continue;
            }
            session.closeSession();
        }
        sessions.clear();
    }

    /*
     * ----- -----
     */

    // callback by session at close time
    protected void closeSession(SessionImpl session) {
        sessions.remove(session);
    }

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
    private void initialize() throws StorageException {
        log.debug("Initializing");
        try {
            XAConnection xaconnection = xadatasource.getXAConnection();
            Connection connection = null;
            try {
                connection = xaconnection.getConnection();
                dialect = getDialect(connection);
            } finally {
                if (connection != null) {
                    connection.close();
                }
                xaconnection.close();
            }
        } catch (SQLException e) {
            throw new StorageException("Cannot get XAConnection", e);
        }
        model = new Model(this, schemaManager);
        sqlInfo = new SQLInfo(model, dialect);
    }

    /**
     * Gets the {@code Dialect}, by connecting to the datasource to check what
     * database is used.
     *
     * @throws StorageException if a SQL connection problem occurs
     */
    private Dialect getDialect(Connection connection) throws StorageException {
        String dbname;
        int dbmajor;
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            dbname = metadata.getDatabaseProductName();
            dbmajor = metadata.getDatabaseMajorVersion();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
        try {
            return DialectFactory.determineDialect(dbname, dbmajor);
        } catch (HibernateException e) {
            throw new StorageException("Cannot determine dialect for class: " +
                    repositoryDescriptor.xaDataSourceName, e);
        }
    }

    // called by session
    public Binary getBinary(InputStream in) throws IOException {
        return binaryManager.getBinary(in);
    }

    /**
     * Deals with the invalidation of persistence contexts between sessions.
     */
    protected class Invalidators extends ConcurrentHashMap<String, Invalidator> {

        private static final long serialVersionUID = 1L;

        /**
         * Gets an invalidator, or creates one if missing.
         */
        public Invalidator getInvalidator(String tableName) {
            Invalidator invalidator = get(tableName);
            if (invalidator == null) {
                // create one if missing, in a concurrent-safe manner
                putIfAbsent(tableName, new Invalidator());
                invalidator = get(tableName);
            }
            return invalidator;
        }
    }

    /**
     * Class dealing with the cross-session invalidation of modified or deleted
     * fragments after a session is saved.
     */
    protected class Invalidator {

        public void invalidate(Context otherContext) {
            String tableName = otherContext.getTableName();
            for (SessionImpl session : sessions) {
                Context context = session.getContext(tableName);
                if (context == null || context == otherContext) {
                    continue;
                }
                context.invalidate(otherContext);
            }
        }
    }

}
