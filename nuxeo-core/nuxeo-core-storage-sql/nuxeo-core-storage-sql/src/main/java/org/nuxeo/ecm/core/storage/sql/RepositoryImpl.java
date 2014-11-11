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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.Reference;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCBackend;
import org.nuxeo.ecm.core.storage.sql.net.NetBackend;
import org.nuxeo.ecm.core.storage.sql.net.NetServer;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link Repository} implementation, to be extended by backend-specific
 * initialization code.
 *
 * @see RepositoryBackend
 */
public class RepositoryImpl implements Repository {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RepositoryImpl.class);

    protected final RepositoryDescriptor repositoryDescriptor;

    protected final SchemaManager schemaManager;

    protected final BinaryManager binaryManager;

    private final RepositoryBackend backend;

    private final Collection<SessionImpl> sessions;

    private Model model;

    private Mapper clusterMapper; // used synchronized

    // modified only under clusterMapper synchronization
    private long clusterLastInvalidationTimeMillis;

    private Object server;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor)
            throws StorageException {
        this.repositoryDescriptor = repositoryDescriptor;
        sessions = new CopyOnWriteArrayList<SessionImpl>();
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            throw new StorageException(e);
        }

        binaryManager = createBinaryManager();
        backend = createBackend();
        server = createServer();
    }

    protected BinaryManager createBinaryManager() throws StorageException {
        try {
            Class<? extends BinaryManager> klass = repositoryDescriptor.binaryManagerClass;
            if (klass == null) {
                klass = DefaultBinaryManager.class;
            }
            BinaryManager binaryManager = klass.newInstance();
            binaryManager.initialize(repositoryDescriptor);
            return binaryManager;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    protected RepositoryBackend createBackend() throws StorageException {
        Class<? extends RepositoryBackend> backendClass = repositoryDescriptor.backendClass;
        List<ServerDescriptor> connect = repositoryDescriptor.connect;
        if (backendClass == null) {
            if (!connect.isEmpty()) {
                backendClass = NetBackend.class;
            } else {
                backendClass = JDBCBackend.class;
            }
        } else {
            if (!connect.isEmpty()) {
                log.error("Repository descriptor specifies both backendClass and connect,"
                        + " only the backend will be used.");
            }
        }
        try {
            RepositoryBackend backend = backendClass.newInstance();
            backend.initialize(this);
            return backend;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    protected Object createServer() {
        ServerDescriptor serverDescriptor = repositoryDescriptor.listen;
        if (serverDescriptor != null && !serverDescriptor.disabled) {
            return NetServer.startServer(repositoryDescriptor);
        } else {
            return null;
        }
    }

    public RepositoryDescriptor getRepositoryDescriptor() {
        return repositoryDescriptor;
    }

    public BinaryManager getBinaryManager() {
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

        boolean initialized = model != null;
        if (!initialized) {
            log.debug("Initializing");
            ModelSetup modelSetup = new ModelSetup();
            modelSetup.repositoryDescriptor = repositoryDescriptor;
            modelSetup.schemaManager = schemaManager;
            backend.initializeModelSetup(modelSetup);
            model = new Model(modelSetup);
            backend.initializeModel(model);
        }

        SessionPathResolver pathResolver = new SessionPathResolver();
        Mapper mapper = backend.newMapper(model, pathResolver);

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
                mapper = backend.newMapper(model, pathResolver);
            }
        }

        SessionImpl session = newSession(mapper, credentials);
        pathResolver.setSession(session);
        sessions.add(session);
        return session;
    }

    protected SessionImpl newSession(Mapper mapper, Credentials credentials)
            throws StorageException {
        mapper = new CachingMapper(mapper);
        return new SessionImpl(this, model, mapper, credentials);
    }

    public static class SessionPathResolver implements PathResolver {

        private Session session;

        protected void setSession(Session session) {
            this.session = session;
        }

        public Serializable getIdForPath(String path) throws StorageException {
            Node node = session.getNodeByPath(path, null);
            return node == null ? null : node.getId();
        }
    }

    /*
     * -----
     */

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

    public synchronized void close() throws StorageException {
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
        model = null;
        if (server != null) {
            NetServer.stopServer(server);
            server = null;
        }
        backend.shutdown();
    }

    protected synchronized void closeAllSessions() throws StorageException {
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
            if (!invalidations.isEmpty()) {
                for (SessionImpl session : sessions) {
                    session.invalidate(invalidations);
                }
            }
        }
    }

}
