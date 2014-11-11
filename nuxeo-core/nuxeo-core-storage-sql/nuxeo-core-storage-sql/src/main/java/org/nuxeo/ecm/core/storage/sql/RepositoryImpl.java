/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend.MapperKind;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCBackend;
import org.nuxeo.ecm.core.storage.sql.net.BinaryManagerClient;
import org.nuxeo.ecm.core.storage.sql.net.BinaryManagerServlet;
import org.nuxeo.ecm.core.storage.sql.net.MapperClientInfo;
import org.nuxeo.ecm.core.storage.sql.net.MapperServlet;
import org.nuxeo.ecm.core.storage.sql.net.NetBackend;
import org.nuxeo.ecm.core.storage.sql.net.NetServer;
import org.nuxeo.runtime.api.Framework;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;

/**
 * {@link Repository} implementation, to be extended by backend-specific
 * initialization code.
 *
 * @see RepositoryBackend
 */
public class RepositoryImpl implements Repository {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RepositoryImpl.class);

    public static final String RUNTIME_SERVER_HOST = "org.nuxeo.runtime.server.host";

    public static final String SERVER_PATH_VCS = "vcs";

    public static final String SERVER_PATH_BINARY = "binary";

    protected final RepositoryDescriptor repositoryDescriptor;

    protected final MultiThreadedHttpConnectionManager connectionManager;

    protected final HttpClient httpClient;

    protected final SchemaManager schemaManager;

    protected final EventService eventService;

    protected final Class<? extends FulltextParser> fulltextParserClass;

    protected final BinaryManager binaryManager;

    private final RepositoryBackend backend;

    private final Collection<SessionImpl> sessions;

    private final Counter sessionCount = Metrics.defaultRegistry().newCounter(
                getClass(), "session");

    private LockManager lockManager;

    /** Propagator of invalidations to all local mappers' caches. */
    private final InvalidationsPropagator cachePropagator;

    /**
     * Propagator of event invalidations to all event queues (only one queue if
     * there are not remote client repositories).
     */
    private final InvalidationsPropagator eventPropagator;

    /** Single event queue global to the repository. */
    private final InvalidationsQueue repositoryEventQueue;

    private Model model;

    private boolean serverStarted;

    private boolean binaryServerStarted;

    /**
     * Transient id for this repository assigned by the server on first
     * connection. This is not persisted.
     */
    public String repositoryId;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor)
            throws StorageException {
        this.repositoryDescriptor = repositoryDescriptor;
        sessions = new CopyOnWriteArrayList<SessionImpl>();
        cachePropagator = new InvalidationsPropagator("cache-" + this);
        eventPropagator = new InvalidationsPropagator("event-" + this);
        repositoryEventQueue = new InvalidationsQueue("repo-"
                + repositoryDescriptor.name);
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

        String className = repositoryDescriptor.fulltextParser;
        if (StringUtils.isBlank(className)) {
            className = FulltextParser.class.getName();
        }
        Class<?> klass;
        try {
            klass = Thread.currentThread().getContextClassLoader().loadClass(
                    className);
        } catch (ClassNotFoundException e) {
            throw new StorageException("Unknown fulltext parser class: "
                    + className, e);
        }
        if (!FulltextParser.class.isAssignableFrom(klass)) {
            throw new StorageException("Invalid fulltext parser class: "
                    + className);
        }
        fulltextParserClass = (Class<? extends FulltextParser>) klass;

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
                activateBinaryManagerServlet(binaryManager);
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

    protected Mapper createCachingMapper(Model model, Mapper mapper)
            throws StorageException {
        if (!repositoryDescriptor.cachingMapperEnabled) {
            log.warn("VCS Mapper cache is disabled.");
            return mapper;
        }
        Class<? extends CachingMapper> cachingMapperClass = repositoryDescriptor.cachingMapperClass;
        if (cachingMapperClass == null) {
            // default cache
            cachingMapperClass = SoftRefCachingMapper.class;
        }
        CachingMapper cachingMapper;
        try {
            log.info("VCS Mapper cache using: " + cachingMapperClass.getName());
            cachingMapper = cachingMapperClass.newInstance();
            cachingMapper.initialize(model, mapper, cachePropagator,
                    eventPropagator, repositoryEventQueue, repositoryDescriptor.cachingMapperProperties);
        } catch (Exception e) {
            throw new StorageException(e);
        }
        return cachingMapper;
    }

    protected void createServer() {
        ServerDescriptor serverDescriptor = repositoryDescriptor.listen;
        if (serverDescriptor != null && !serverDescriptor.disabled) {
            activateServletMapper();
        }
    }

    protected void activateServletMapper() {
        if (!serverStarted) {
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

    protected void deactivateServletMapper() {
        if (serverStarted) {
            String servletName = MapperServlet.getName(repositoryDescriptor.name);
            NetServer.remove(repositoryDescriptor.listen, servletName);
            serverStarted = false;
        }
    }

    protected void activateBinaryManagerServlet(BinaryManager binaryManager) {
        if (!binaryServerStarted) {
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
    }

    protected void deactivateBinaryManagerServlet() {
        if (binaryServerStarted) {
            String servletName = BinaryManagerServlet.getName(binaryManager);
            NetServer.remove(repositoryDescriptor.listen, servletName);
            binaryServerStarted = false;
        }
    }

    @Override
    public boolean isServerActivated() {
        return serverStarted;
    }

    @Override
    public String getServerURL() {
        String host = Framework.getProperty(RUNTIME_SERVER_HOST, "localhost");
        if (repositoryDescriptor.listen != null) {
            return String.format("http://%s:%d/%s", host,
                    repositoryDescriptor.listen.port,
                    repositoryDescriptor.listen.path);
        } else {
            return null;
        }
    }

    @Override
    public void activateServer() {
        activateServletMapper();
        activateBinaryManagerServlet(binaryManager);
    }

    @Override
    public void deactivateServer() {
        deactivateServletMapper();
        deactivateBinaryManagerServlet();
    }

    @Override
    public Collection<MapperClientInfo> getClientInfos() {
        if (!serverStarted) {
            return Collections.emptyList();
        }
        MapperServlet servlet = (MapperServlet) NetServer.get(
                repositoryDescriptor.listen,
                MapperServlet.getName(repositoryDescriptor.name));
        return servlet.getClientInfos();
    }

    public RepositoryDescriptor getRepositoryDescriptor() {
        return repositoryDescriptor;
    }

    public BinaryManager getBinaryManager() {
        return binaryManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    public Model getModel() {
        return model;
    }

    public Class<? extends FulltextParser> getFulltextParserClass() {
        return fulltextParserClass;
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
    @Override
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
    @Override
    public synchronized SessionImpl getConnection(ConnectionSpec connectionSpec)
            throws StorageException {
        assert connectionSpec == null
                || connectionSpec instanceof ConnectionSpecImpl;

        Credentials credentials = connectionSpec == null ? null
                : ((ConnectionSpecImpl) connectionSpec).getCredentials();

        boolean create = model == null;
        if (create) {
            log.debug("Initializing");
            ModelSetup modelSetup = new ModelSetup();
            modelSetup.repositoryDescriptor = repositoryDescriptor;
            modelSetup.schemaManager = schemaManager;
            backend.initializeModelSetup(modelSetup);
            model = new Model(modelSetup);
            backend.initializeModel(model);

            // create the lock manager, which creates its own mapper
            // creating this first, before the cluster node handler,
            // as we don't want invalidations in the lock manager's mapper
            Mapper lockManagerMapper = backend.newMapper(model, null,
                    MapperKind.LOCK_MANAGER);
            lockManager = new LockManager(lockManagerMapper,
                    repositoryDescriptor.clusteringEnabled);

            // create the mapper for the cluster node handler
            if (repositoryDescriptor.clusteringEnabled) {
                backend.newMapper(model, null, MapperKind.CLUSTER_NODE_HANDLER);
                log.info("Clustering enabled with "
                        + repositoryDescriptor.clusteringDelay
                        + " ms delay for repository: " + getName());
            }
        }

        SessionPathResolver pathResolver = new SessionPathResolver();
        Mapper mapper = backend.newMapper(model, pathResolver, null);
        SessionImpl session = newSession(model, mapper, credentials);
        pathResolver.setSession(session);
        sessions.add(session);
        sessionCount.inc();
        return session;
    }

    protected SessionImpl newSession(Model model, Mapper mapper,
            Credentials credentials) throws StorageException {
        mapper = createCachingMapper(model, mapper);
        return new SessionImpl(this, model, mapper, credentials);
    }

    public static class SessionPathResolver implements PathResolver {

        private Session session;

        protected void setSession(Session session) {
            this.session = session;
        }

        @Override
        public Serializable getIdForPath(String path) throws StorageException {
            Node node = session.getNodeByPath(path, null);
            return node == null ? null : node.getId();
        }
    }

    /*
     * -----
     */

    @Override
    public ResourceAdapterMetaData getMetaData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RecordFactory getRecordFactory() {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- javax.resource.Referenceable -----
     */

    private Reference reference;

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @Override
    public Reference getReference() {
        return reference;
    }

    /*
     * ----- Repository -----
     */

    @Override
    public synchronized void close() throws StorageException {
        closeAllSessions();

        model = null;

        deactivateServletMapper();
        deactivateBinaryManagerServlet();

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
        sessionCount.clear();
        if (lockManager != null) {
            lockManager.shutdown();
        }
    }

    /*
     * ----- RepositoryManagement -----
     */

    @Override
    public String getName() {
        return repositoryDescriptor.name;
    }

    @Override
    public int getActiveSessionsCount() {
        return sessions.size();
    }

    @Override
    public int clearCaches() {
        int n = 0;
        for (SessionImpl session : sessions) {
            n += session.clearCaches();
        }
        if (lockManager != null) {
            lockManager.clearCaches();
        }
        return n;
    }

    @Override
    public void processClusterInvalidationsNext() {
        // TODO pass through or something
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return binaryManager.getGarbageCollector();
    }

    @Override
    public void markReferencedBinaries(BinaryGarbageCollector gc) {
        try {
            SessionImpl conn = getConnection();
            try {
                conn.markReferencedBinaries(gc);
            } finally {
                conn.close();
            }
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        try {
            SessionImpl conn = getConnection();
            try {
                return conn.cleanupDeletedDocuments(max, beforeTime);
            } finally {
                conn.close();
            }
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * ----- -----
     */

    // callback by session at close time
    protected void closeSession(SessionImpl session) {
        sessions.remove(session);
        sessionCount.dec();
    }

}
