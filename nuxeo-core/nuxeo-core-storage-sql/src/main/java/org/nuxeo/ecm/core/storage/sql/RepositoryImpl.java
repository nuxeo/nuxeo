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
import java.sql.Connection;
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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.db.dialect.Dialect;
import org.nuxeo.runtime.api.Framework;

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
        assert connectionSpec == null
                || connectionSpec instanceof ConnectionSpecImpl;

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
     * ----- RepositoryManagement -----
     */

    public String getName() {
        return repositoryDescriptor.name;
    }

    public int getActiveSessionsCount() {
        return sessions.size();
    }

    public int clearCaches() {
        int n = 0;
        for (SessionImpl session : sessions) {
            n += session.clearCaches();
        }
        return n;
    }

    /*
     * ----- -----
     */

    // callback by session at close time
    protected void closeSession(SessionImpl session) {
        sessions.remove(session);
    }

    private XADataSource getXADataSource() throws StorageException {
        // instantiate the datasource
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

        // set JavaBean properties
        for (Entry<String, String> entry : repositoryDescriptor.properties.entrySet()) {
            String name = entry.getKey();
            Object value = Framework.expandVars(entry.getValue());
            if (name.contains("/")) {
                // old syntax where non-String types were explicited
                name = name.substring(0, name.indexOf('/'));
            }
            // transform to proper JavaBean convention
            if (Character.isLowerCase(name.charAt(1))) {
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
            try {
                BeanUtils.setProperty(xadatasource, name, value);
            } catch (Exception e) {
                log.error(String.format("Cannot set %s = %s", name, value));
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
                dialect = Dialect.createDialect(connection, repositoryDescriptor);
            } finally {
                if (connection != null) {
                    connection.close();
                }
                xaconnection.close();
            }
        } catch (SQLException e) {
            throw new StorageException("Cannot get XAConnection", e);
        }
        model = new Model(this, schemaManager, dialect);
        sqlInfo = new SQLInfo(model, dialect);
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
