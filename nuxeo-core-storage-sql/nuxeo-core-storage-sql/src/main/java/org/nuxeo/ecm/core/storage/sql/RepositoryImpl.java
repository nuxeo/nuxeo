/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map.Entry;
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

    private final BinaryManager binaryManager;

    private final XADataSource xadatasource;

    private boolean initialized; // initialized at first access

    private Dialect dialect;

    private Model model;

    private SQLInfo sqlInfo;

    private Mapper clusterMapper; // used synchronized

    // modified only under clusterMapper synchronization
    private long clusterLastInvalidationTimeMillis;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor,
            SchemaManager schemaManager) throws StorageException {
        this.repositoryDescriptor = repositoryDescriptor;
        this.schemaManager = schemaManager;
        sessions = new CopyOnWriteArrayList<SessionImpl>();
        xadatasource = getXADataSource();
        try {
            Class<? extends BinaryManager> klass = repositoryDescriptor.binaryManagerClass;
            if (klass == null) {
                klass = DefaultBinaryManager.class;
            }
            Constructor<? extends BinaryManager> constructor = klass.getConstructor(RepositoryDescriptor.class);
            binaryManager = constructor.newInstance(repositoryDescriptor);
        } catch (InvocationTargetException e) {
            throw new StorageException(e.getCause());
        } catch (Exception e) {
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

        Mapper mapper = new Mapper(this, model, sqlInfo, xadatasource);

        if (!initialized) {
            // first connection, initialize the database
            mapper.createDatabase();
            if (repositoryDescriptor.clusteringEnabled) {
                log.info("Clustering enabled with "
                        + repositoryDescriptor.clusteringDelay
                        + " ms delay for repository: " + getName());
                // use the mapper that created the database as cluster mapper
                clusterMapper = mapper;
                clusterMapper.createClusterNode();
                processClusterInvalidationsNext();
                mapper = new Mapper(this, model, sqlInfo, xadatasource);
            }
            initialized = true;
        }

        SessionImpl session = new SessionImpl(this, schemaManager, mapper,
                credentials);

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
        if (clusterMapper != null) {
            synchronized (clusterMapper) {
                try {
                    clusterMapper.removeClusterNode();
                } catch (StorageException e) {
                    log.error(e.getMessage(), e);
                }
                clusterMapper.close();
            }
            clusterMapper = null;
        }
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

    public void processClusterInvalidationsNext() {
        clusterLastInvalidationTimeMillis = System.currentTimeMillis()
                - repositoryDescriptor.clusteringDelay - 1;
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
                name = Character.toLowerCase(name.charAt(0))
                        + name.substring(1);
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
                dialect = Dialect.createDialect(connection,
                        repositoryDescriptor);
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
     * Sends invalidation data to relevant sessions.
     *
     * @param invalidations the invalidations
     * @param fromSession the session from which these invalidations originate,
     *            or {@code null} if they come from another cluster node
     * @throws StorageException on failure to insert invalidation information
     *             into the cluster invalidation tables
     */
    protected void invalidate(Invalidations invalidations,
            SessionImpl fromSession) throws StorageException {
        // local invalidations
        for (SessionImpl session : sessions) {
            if (session != fromSession) {
                session.invalidate(invalidations);
            }
        }
        // cluster invalidations
        if (clusterMapper != null) {
            synchronized (clusterMapper) {
                clusterMapper.insertClusterInvalidations(invalidations);
            }
        }
    }

    /**
     * Reads cluster invalidations and queues them locally.
     */
    protected void receiveClusterInvalidations() throws StorageException {
        if (clusterMapper != null) {
            Invalidations invalidations;
            synchronized (clusterMapper) {
                if (clusterLastInvalidationTimeMillis
                        + repositoryDescriptor.clusteringDelay > System.currentTimeMillis()) {
                    // delay hasn't expired
                    return;
                }
                invalidations = clusterMapper.getClusterInvalidations();
                clusterLastInvalidationTimeMillis = System.currentTimeMillis();
            }
            if (invalidations.isEmpty()) {
                return;
            }
            for (SessionImpl session : sessions) {
                session.invalidate(invalidations);
            }
        }
    }

}
