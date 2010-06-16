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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.Reference;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.EventConstants;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCBackend;
import org.nuxeo.ecm.core.storage.sql.net.BinaryManagerClient;
import org.nuxeo.ecm.core.storage.sql.net.BinaryManagerServlet;
import org.nuxeo.ecm.core.storage.sql.net.MapperServlet;
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

    public static final String SERVER_PATH_VCS = "vcs";

    public static final String SERVER_PATH_BINARY = "binary";

    protected final RepositoryDescriptor repositoryDescriptor;

    protected final MultiThreadedHttpConnectionManager connectionManager;

    protected final HttpClient httpClient;

    protected final SchemaManager schemaManager;

    protected final EventService eventService;

    protected final BinaryManager binaryManager;

    private final RepositoryBackend backend;

    private final Collection<SessionImpl> sessions;

    private Model model;

    private Mapper clusterMapper; // used synchronized

    // modified only under clusterMapper synchronization
    private long clusterLastInvalidationTimeMillis;

    private boolean serverStarted;

    private boolean binaryServerStarted;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor)
            throws StorageException {
        this.repositoryDescriptor = repositoryDescriptor;
        sessions = new CopyOnWriteArrayList<SessionImpl>();
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            throw new StorageException(e);
        }
        try {
            eventService = Framework.getService(EventService.class);
        } catch (Exception e) {
            throw new StorageException(e);
        }

        connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = connectionManager.getParams();
        params.setDefaultMaxConnectionsPerHost(20);
        params.setMaxTotalConnections(20);
        httpClient = new HttpClient(connectionManager);

        binaryManager = createBinaryManager();
        backend = createBackend();
        createServer();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    protected BinaryManager createBinaryManager() throws StorageException {
        try {
            Class<? extends BinaryManager> klass = repositoryDescriptor.binaryManagerClass;
            if (klass == null) {
                klass = DefaultBinaryManager.class;
            }
            BinaryManager binaryManager = klass.newInstance();
            binaryManager.initialize(repositoryDescriptor);
            if (repositoryDescriptor.binaryManagerConnect) {
                List<ServerDescriptor> connect = repositoryDescriptor.connect;
                if (connect.isEmpty() || connect.get(0).disabled) {
                    log.error("Repository descriptor specifies binaryManager connect "
                            + "without a global connect");
                } else {
                    binaryManager = new BinaryManagerClient(binaryManager,
                            httpClient);
                    binaryManager.initialize(repositoryDescriptor);
                }
            }
            if (repositoryDescriptor.binaryManagerListen) {
                ServerDescriptor serverDescriptor = repositoryDescriptor.listen;
                if (serverDescriptor == null || serverDescriptor.disabled) {
                    log.error("Repository descriptor specifies binaryManager listen "
                            + "without a global listen");
                } else {
                    BinaryManagerServlet servlet = new BinaryManagerServlet(
                            binaryManager);
                    String servletName = BinaryManagerServlet.getName(binaryManager);
                    String url = NetServer.add(serverDescriptor, servletName,
                            servlet, SERVER_PATH_BINARY);
                    log.info(String.format(
                            "VCS server for binary manager of repository '%s' started on: %s",
                            repositoryDescriptor.name, url));
                    binaryServerStarted = true;
                }
            }
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

    protected void createServer() {
        ServerDescriptor serverDescriptor = repositoryDescriptor.listen;
        if (serverDescriptor != null && !serverDescriptor.disabled) {
            MapperServlet servlet = new MapperServlet(repositoryDescriptor.name);
            String servletName = MapperServlet.getName(repositoryDescriptor.name);
            String url = NetServer.add(repositoryDescriptor.listen,
                    servletName, servlet, SERVER_PATH_VCS);
            log.info(String.format(
                    "VCS server for repository '%s' started on: %s",
                    repositoryDescriptor.name, url));
            serverStarted = true;
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
        if (serverStarted) {
            String servletName = MapperServlet.getName(repositoryDescriptor.name);
            NetServer.remove(repositoryDescriptor.listen, servletName);
            serverStarted = false;
        }
        if (binaryServerStarted) {
            String servletName = BinaryManagerServlet.getName(binaryManager);
            NetServer.remove(repositoryDescriptor.listen, servletName);
            binaryServerStarted = false;
        }
        backend.shutdown();
        connectionManager.shutdown();
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
     * <p>
     * Called post-transaction by commit/rollback or transactionless save.
     *
     * @param invalidations the invalidations
     * @param fromSession the session from which these invalidations originate
     * @throws StorageException on failure to insert invalidation information
     *             into the cluster invalidation tables
     */
    protected void invalidate(Invalidations invalidations,
            SessionImpl fromSession) throws StorageException {
        // caller makes sure invalidations is not empty
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
        sendInvalidationEvent(invalidations, true, fromSession);
    }

    /**
     * Reads cluster invalidations and queues them locally.
     *
     * @param fromSession the session that triggered the action
     */
    protected void receiveClusterInvalidations(SessionImpl fromSession)
            throws StorageException {
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
                sendInvalidationEvent(invalidations, false, fromSession);
            }
        }
    }

    /**
     * Sends a Core Event about the invalidations.
     *
     * @param invalidations the invalidations
     * @param local {@code true} if these invalidations come from this cluster
     *            node (one of this repository's sessions), {@code false} if
     *            they come from a remote cluster node
     * @param session a session which can be used to lookup containing documents
     */
    protected void sendInvalidationEvent(Invalidations invalidations,
            boolean local, SessionImpl session) {
        if (!repositoryDescriptor.sendInvalidationEvents) {
            return;
        }
        // compute modified doc ids and parent ids
        HashSet<String> modifiedDocIds = new HashSet<String>();
        HashSet<String> modifiedParentIds = new HashSet<String>();

        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                String id = (String) rowId.id;
                String docId;
                try {
                    docId = (String) session.getContainingDocument(id);
                } catch (StorageException e) {
                    log.error("Cannot get containing document for: " + id, e);
                    docId = null;
                }
                if (docId == null) {
                    continue;
                }
                if (Invalidations.PARENT.equals(rowId.tableName)) {
                    if (docId.equals(id)) {
                        modifiedParentIds.add(docId);
                    } else { // complex prop added/removed
                        modifiedDocIds.add(docId);
                    }
                } else {
                    modifiedDocIds.add(docId);
                }
            }
        }
        // TODO check what we can do about invalidations.deleted

        EventContext ctx = new EventContextImpl(null, null);
        ctx.setRepositoryName(getName());
        ctx.setProperty(EventConstants.INVAL_MODIFIED_DOC_IDS, modifiedDocIds);
        ctx.setProperty(EventConstants.INVAL_MODIFIED_PARENT_IDS,
                modifiedParentIds);
        ctx.setProperty(EventConstants.INVAL_LOCAL, Boolean.valueOf(local));
        Event event = new EventImpl(EventConstants.EVENT_VCS_INVALIDATIONS, ctx);
        try {
            eventService.fireEvent(event);
        } catch (ClientException e) {
            log.error("Failed to send invalidation event: " + e, e);
        }
    }

}
