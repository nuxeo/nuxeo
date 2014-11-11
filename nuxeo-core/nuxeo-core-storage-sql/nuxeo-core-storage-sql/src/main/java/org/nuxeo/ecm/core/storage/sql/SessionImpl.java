/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.EventConstants;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Invalidations.InvalidationsPair;
import org.nuxeo.ecm.core.storage.sql.PersistenceContext.PathAndId;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowBatch;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.services.streaming.FileSource;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * The session is the main high level access point to data from the underlying
 * database.
 */
public class SessionImpl implements Session, XAResource {

    private static final Log log = LogFactory.getLog(SessionImpl.class);

    /**
     * Set this system property to false if you don't want repositories to be
     * looked up under the compatibility name "default" in the "repositories"
     * table.
     * <p>
     * Only do this if you start from an empty database, or if you have migrated
     * the "repositories" table by hand, or if you need to create a new
     * repository in a database already containing a "default" repository (table
     * sharing, not recommended).
     */
    public static final String COMPAT_REPOSITORY_NAME_KEY = "org.nuxeo.vcs.repository.name.default.compat";

    private static final boolean COMPAT_REPOSITORY_NAME = Boolean.parseBoolean(Framework.getProperty(
            COMPAT_REPOSITORY_NAME_KEY, "true"));

    protected final RepositoryImpl repository;

    private final Mapper mapper;

    private final Model model;

    protected final FulltextParser fulltextParser;

    // public because used by unit tests
    public final PersistenceContext context;

    private boolean live;

    private boolean inTransaction;

    private Node rootNode;

    private long threadId;

    private String threadName;

    private Throwable threadStack;

    private boolean readAclsChanged;


    // @since 5.7
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    private final Timer saveTimer;

    private final Timer queryTimer;

    private final Timer aclrUpdateTimer;

    public SessionImpl(RepositoryImpl repository, Model model, Mapper mapper,
            Credentials credentials) throws StorageException {
        this.repository = repository;
        this.mapper = mapper;
        if (mapper instanceof CachingMapper) {
            ((CachingMapper) mapper).setSession(this);
        }
        // this.credentials = credentials;
        this.model = model;
        context = new PersistenceContext(model, mapper, this);
        live = true;
        readAclsChanged = false;

        try {
            fulltextParser = repository.fulltextParserClass.newInstance();
        } catch (Exception e) {
            throw new StorageException(e);
        }
        saveTimer =  registry.timer(MetricRegistry.name("nuxeo", "repositories", repository.getName(), "saves"));
        queryTimer =  registry.timer(MetricRegistry.name("nuxeo", "repositories", repository.getName(), "queries"));
        aclrUpdateTimer =  registry.timer(MetricRegistry.name("nuxeo", "repositories", repository.getName(), "aclr-updates"));

        computeRootNode();
    }

    public void checkLive() {
        if (!live) {
            throw new IllegalStateException("Session is not live");
        }
        checkThread();
    }

    // called by NetServlet when forwarding remote NetMapper calls.
    @Override
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * Gets the XAResource. Called by the ManagedConnectionImpl, which actually
     * wraps it in a connection-aware implementation.
     */
    public XAResource getXAResource() {
        return this;
    }

    /**
     * Clears all the caches. Called by RepositoryManagement.
     */
    protected int clearCaches() {
        if (inTransaction) {
            // avoid potential multi-threaded access to active session
            return 0;
        }
        checkThreadEnd();
        return context.clearCaches();
    }

    protected PersistenceContext getContext() {
        return context;
    }

    protected void rollback() {
        context.clearCaches();
    }

    protected void checkThread() {
        if (threadId == 0) {
            return;
        }
        long currentThreadId = Thread.currentThread().getId();
        if (threadId == currentThreadId) {
            return;
        }
        String currentThreadName = Thread.currentThread().getName();
        String msg = String.format(
                "Concurrency Error: Session was started in thread %s (%s)"
                        + " but is being used in thread %s (%s)", threadId,
                threadName, currentThreadId, currentThreadName);
        throw new IllegalStateException(msg, threadStack);
    }

    protected void checkThreadStart() {
        threadId = Thread.currentThread().getId();
        threadName = Thread.currentThread().getName();
        if (log.isDebugEnabled()) {
            threadStack = new Throwable("owner stack trace");
        }
    }

    protected void checkThreadEnd() {
        threadId = 0;
        threadName = null;
        threadStack = null;
    }

    /**
     * Generates a new id, or used a pre-generated one (import).
     */
    protected Serializable generateNewId(Serializable id)
            throws StorageException {
        return context.generateNewId(id);
    }

    protected boolean isIdNew(Serializable id) {
        return context.isIdNew(id);
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    @Override
    public void close() throws ResourceException {
        try {
            closeSession();
        } catch (StorageException e) {
            throw new ResourceException(e);
        }
        repository.closeSession(this);
    }

    protected void closeSession() throws StorageException {
        checkLive();
        live = false;
        context.clearCaches();
        // close the mapper and therefore the connection
        mapper.close();
        // don't clean the caches, we keep the pristine cache around
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Session -----
     */

    @Override
    public boolean isLive() {
        return live;
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        // only the JCA handle returns true
        return false;
    }

    @Override
    public String getRepositoryName() {
        return repository.getName();
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public Node getRootNode() {
        checkThread();
        checkLive();
        return rootNode;
    }

    @Override
    public Binary getBinary(FileSource source) throws StorageException {
        BinaryManager mgr = repository.getBinaryManager();
        try {
            if (mgr instanceof BinaryManagerStreamSupport) {
                return ((BinaryManagerStreamSupport) mgr).getBinary(source);
            }
            return mgr.getBinary(source.getStream());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Binary getBinary(InputStream in) throws StorageException {
        BinaryManager mgr = repository.getBinaryManager();
        try {
            return mgr.getBinary(in);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void save() throws StorageException {
        final Timer.Context timerContext = saveTimer.time();
        try {
            checkLive();
            flushAndScheduleWork();
            if (!inTransaction) {
                sendInvalidationsToOthers();
                // as we don't have a way to know when the next
                // non-transactional
                // statement will start, process invalidations immediately
                processReceivedInvalidations();
            }
        } finally {
            timerContext.stop();
        }
    }

    protected void flushAndScheduleWork() throws StorageException {
        checkThread();
        List<Work> works;
        if (!repository.getRepositoryDescriptor().fulltextDisabled) {
            works = getFulltextWorks();
        } else {
            works = Collections.emptyList();
        }
        flush();
        if (readAclsChanged) {
            updateReadAcls();
        }
        scheduleWork(works);
        checkInvalidationsConflict();
    }

    protected void scheduleWork(List<Work> works) {
        // do async fulltext indexing only if high-level sessions are available
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        if (repositoryManager != null && !works.isEmpty()) {
            WorkManager workManager = Framework.getLocalService(WorkManager.class);
            for (Work work : works) {
                // schedule work post-commit
                // in non-tx mode, this may execute it nearly immediately
                workManager.schedule(work, Scheduling.IF_NOT_SCHEDULED, true);
            }
        }
    }

    protected void flush() throws StorageException {
        RowBatch batch = context.getSaveBatch();
        if (!batch.isEmpty()) {
            log.debug("Saving session");
            // execute the batch
            mapper.write(batch);
            log.debug("End of save");
        }
    }

    protected Serializable getContainingDocument(Serializable id)
            throws StorageException {
        return context.getContainingDocument(id);
    }

    /**
     * Gets the fulltext updates to do. Called at save() time.
     *
     * @return a list of {@link Work} instances to schedule post-commit.
     */
    protected List<Work> getFulltextWorks() throws StorageException {
        Set<Serializable> dirtyStrings = new HashSet<Serializable>();
        Set<Serializable> dirtyBinaries = new HashSet<Serializable>();
        context.findDirtyDocuments(dirtyStrings, dirtyBinaries);
        if (dirtyStrings.isEmpty() && dirtyBinaries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Work> works = new LinkedList<Work>();
        getFulltextSimpleWorks(works, dirtyStrings);
        getFulltextBinariesWorks(works, dirtyBinaries);
        return works;
    }

    protected void getFulltextSimpleWorks(List<Work> works,
            Set<Serializable> dirtyStrings) throws StorageException {
        // update simpletext on documents with dirty strings
        for (Serializable docId : dirtyStrings) {
            if (docId == null) {
                // cannot happen, but has been observed :(
                log.error("Got null doc id in fulltext update, cannot happen");
                continue;
            }
            Node document = getNodeById(docId);
            if (document == null) {
                // cannot happen
                continue;
            }
            if (document.isProxy()) {
                // proxies don't have any fulltext attached, it's
                // the target document that carries it
                continue;
            }
            String documentType = document.getPrimaryType();
            String[] mixinTypes = document.getMixinTypes();

            if (!model.getFulltextInfo().isFulltextIndexable(documentType)) {
                continue;
            }
            document.getSimpleProperty(Model.FULLTEXT_JOBID_PROP).setValue(
                    model.idToString(document.getId()));
            fulltextParser.setDocument(document, this);
            try {
                for (String indexName : model.getFulltextInfo().indexNames) {
                    Set<String> paths;
                    if (model.getFulltextInfo().indexesAllSimple.contains(indexName)) {
                        // index all string fields, minus excluded ones
                        // TODO XXX excluded ones...
                        paths = model.getSimpleTextPropertyPaths(documentType,
                                mixinTypes);
                    } else {
                        // index configured fields
                        paths = model.getFulltextInfo().propPathsByIndexSimple.get(indexName);
                    }
                    String text = fulltextParser.findFulltext(indexName, paths);
                    Work work = new FulltextUpdaterWork(repository.getName(),
                            model.idToString(docId), indexName, true, text,
                            false);
                    works.add(work);
                }
            } finally {
                fulltextParser.setDocument(null, this);
            }
        }
    }

    protected void getFulltextBinariesWorks(List<Work> works,
            final Set<Serializable> dirtyBinaries) throws StorageException {
        if (dirtyBinaries.isEmpty()) {
            return;
        }

        // mark indexing in progress, so that future copies (including versions)
        // will be indexed as well
        for (Node node : getNodesByIds(new ArrayList<Serializable>(
                dirtyBinaries))) {
            if (!model.getFulltextInfo().isFulltextIndexable(
                    node.getPrimaryType())) {
                continue;
            }
            node.getSimpleProperty(Model.FULLTEXT_JOBID_PROP).setValue(
                    model.idToString(node.getId()));
        }

        // FulltextExtractorWork does fulltext extraction using converters
        // and then schedules a FulltextUpdaterWork to write the results
        // single-threaded
        for (Serializable id : dirtyBinaries) {
            String docId = model.idToString(id);
            Work work = new FulltextExtractorWork(repository.getName(), docId);
            works.add(work);
        }
    }

    /**
     * Post-transaction invalidations notification.
     * <p>
     * Called post-transaction by session commit/rollback or transactionless
     * save.
     */
    protected void sendInvalidationsToOthers() throws StorageException {
        // XXX TODO repo invalidate adds to cluster, and sends event
        context.sendInvalidationsToOthers();
    }

    /**
     * Processes all invalidations accumulated.
     * <p>
     * Called pre-transaction by start or transactionless save;
     */
    protected void processReceivedInvalidations() throws StorageException {
        context.processReceivedInvalidations();
    }

    /**
     * Post transaction check invalidations processing.
     */
    protected void checkInvalidationsConflict() throws StorageException {
        // repository.receiveClusterInvalidations(this);
        context.checkInvalidationsConflict();
    }

    /**
     * Collect modified document IDs into two separate set, one for the docs and
     * the other for parents
     * <p>
     * Collects ids as Strings (not Serializables) as these are then sent to
     * high-level event code.
     *
     * @param invalidations
     * @param docs
     * @param parents
     */
    protected void collectModified(Invalidations invalidations,
            Set<String> docs, Set<String> parents) {
        if (invalidations == null || invalidations.modified == null) {
            return;
        }
        for (RowId rowId : invalidations.modified) {
            Serializable id = rowId.id;
            Serializable docId;
            try {
                docId = getContainingDocument(id);
            } catch (StorageException e) {
                log.error("Cannot get containing document for: " + id, e);
                docId = null;
            }
            if (docId == null) {
                continue;
            }
            if (Invalidations.PARENT.equals(rowId.tableName)) {
                if (docId.equals(id)) {
                    parents.add(model.idToString(docId));
                } else { // complex prop added/removed
                    docs.add(model.idToString(docId));
                }
            } else {
                docs.add(model.idToString(docId));
            }
        }
    }

    /**
     * Sends a Core Event about the invalidations.
     * <p>
     * Containing documents are looked up in this session.
     *
     * @param invalidations the invalidations
     * @param local {@code true} if these invalidations come from this cluster
     *            node (one of this repository's sessions), {@code false} if
     *            they come from a remote cluster node
     */
    protected void sendInvalidationEvent(Invalidations invalidations,
            boolean local) {
        sendInvalidationEvent(new InvalidationsPair(invalidations, null));
    }

    /**
     * Send a core event about the merged invalidations (NXP-5808)
     *
     * @param pair
     */
    protected void sendInvalidationEvent(InvalidationsPair pair) {
        if (!repository.repositoryDescriptor.sendInvalidationEvents) {
            return;
        }
        // compute modified doc ids and parent ids (as strings)
        HashSet<String> modifiedDocIds = new HashSet<String>();
        HashSet<String> modifiedParentIds = new HashSet<String>();

        // merge cache and events because of clustering (NXP-5808)
        collectModified(pair.cacheInvalidations, modifiedDocIds,
                modifiedParentIds);
        collectModified(pair.eventInvalidations, modifiedDocIds,
                modifiedParentIds);

        // TODO check what we can do about invalidations.deleted

        if (modifiedDocIds.isEmpty() && modifiedParentIds.isEmpty()) {
            return;
        }

        EventContext ctx = new EventContextImpl(null, null);
        ctx.setRepositoryName(repository.getName());
        ctx.setProperty(EventConstants.INVAL_MODIFIED_DOC_IDS, modifiedDocIds);
        ctx.setProperty(EventConstants.INVAL_MODIFIED_PARENT_IDS,
                modifiedParentIds);
        Event event = new EventImpl(EventConstants.EVENT_VCS_INVALIDATIONS, ctx);
        try {
            repository.eventService.fireEvent(event);
        } catch (ClientException e) {
            log.error("Failed to send invalidation event: " + e, e);
        }
    }

    /*
     * -------------------------------------------------------------
     * -------------------------------------------------------------
     * -------------------------------------------------------------
     */

    protected Node getNodeById(Serializable id, boolean prefetch)
            throws StorageException {
        List<Node> nodes = getNodesByIds(Collections.singletonList(id),
                prefetch);
        Node node = nodes.get(0);
        // ((JDBCMapper) ((CachingMapper)
        // mapper).mapper).logger.log("getNodeById " + id + " -> " + (node ==
        // null ? "missing" : "found"));
        return node;
    }

    @Override
    public Node getNodeById(Serializable id) throws StorageException {
        checkThread();
        checkLive();
        if (id == null) {
            throw new IllegalArgumentException("Illegal null id");
        }
        return getNodeById(id, true);
    }

    public List<Node> getNodesByIds(List<Serializable> ids, boolean prefetch)
            throws StorageException {
        // get hier fragments
        List<RowId> hierRowIds = new ArrayList<RowId>(ids.size());
        for (Serializable id : ids) {
            hierRowIds.add(new RowId(Model.HIER_TABLE_NAME, id));
        }

        List<Fragment> hierFragments = context.getMulti(hierRowIds, false);

        // find available paths
        Map<Serializable, String> paths = new HashMap<Serializable, String>();
        Set<Serializable> parentIds = new HashSet<Serializable>();
        for (Fragment fragment : hierFragments) {
            Serializable id = fragment.getId();
            PathAndId pathOrId = context.getPathOrMissingParentId(
                    (SimpleFragment) fragment, false);
            // find missing fragments
            if (pathOrId.path != null) {
                paths.put(id, pathOrId.path);
            } else {
                parentIds.add(pathOrId.id);
            }
        }
        // fetch the missing parents and their ancestors in bulk
        if (!parentIds.isEmpty()) {
            // fetch them in the context
            getHierarchyAndAncestors(parentIds);
            // compute missing paths using context
            for (Fragment fragment : hierFragments) {
                Serializable id = fragment.getId();
                if (paths.containsKey(id)) {
                    continue;
                }
                String path = context.getPath((SimpleFragment) fragment);
                paths.put(id, path);
            }
        }

        // prepare fragment groups to build nodes
        Map<Serializable, FragmentGroup> fragmentGroups = new HashMap<Serializable, FragmentGroup>(
                ids.size());
        for (Fragment fragment : hierFragments) {
            Serializable id = fragment.row.id;
            fragmentGroups.put(id, new FragmentGroup((SimpleFragment) fragment,
                    new FragmentsMap()));
        }

        if (prefetch) {
            List<RowId> bulkRowIds = new ArrayList<RowId>();
            Set<Serializable> proxyIds = new HashSet<Serializable>();

            // get rows to prefetch for hier fragments
            for (Fragment fragment : hierFragments) {
                findPrefetchedFragments((SimpleFragment) fragment, bulkRowIds,
                        proxyIds);
            }

            // proxies

            // get proxies fragments
            List<RowId> proxiesRowIds = new ArrayList<RowId>(proxyIds.size());
            for (Serializable id : proxyIds) {
                proxiesRowIds.add(new RowId(Model.PROXY_TABLE_NAME, id));
            }
            List<Fragment> proxiesFragments = context.getMulti(proxiesRowIds,
                    true);
            Set<Serializable> targetIds = new HashSet<Serializable>();
            for (Fragment fragment : proxiesFragments) {
                Serializable targetId = ((SimpleFragment) fragment).get(Model.PROXY_TARGET_KEY);
                targetIds.add(targetId);
            }

            // get hier fragments for proxies' targets
            targetIds.removeAll(ids); // only those we don't have already
            hierRowIds = new ArrayList<RowId>(targetIds.size());
            for (Serializable id : targetIds) {
                hierRowIds.add(new RowId(Model.HIER_TABLE_NAME, id));
            }
            hierFragments = context.getMulti(hierRowIds, true);
            for (Fragment fragment : hierFragments) {
                findPrefetchedFragments((SimpleFragment) fragment, bulkRowIds,
                        null);
            }

            // we have everything to be prefetched

            // fetch all the prefetches in bulk
            List<Fragment> fragments = context.getMulti(bulkRowIds, true);

            // put each fragment in the map of the proper group
            for (Fragment fragment : fragments) {
                FragmentGroup fragmentGroup = fragmentGroups.get(fragment.row.id);
                if (fragmentGroup != null) {
                    fragmentGroup.fragments.put(fragment.row.tableName,
                            fragment);
                }
            }
        }

        // assemble nodes from the fragment groups
        List<Node> nodes = new ArrayList<Node>(ids.size());
        for (Serializable id : ids) {
            FragmentGroup fragmentGroup = fragmentGroups.get(id);
            // null if deleted/absent
            Node node = fragmentGroup == null ? null : new Node(context,
                    fragmentGroup, paths.get(id));
            nodes.add(node);
        }

        return nodes;
    }

    /**
     * Finds prefetched fragments for a hierarchy fragment, takes note of the
     * ones that are proxies.
     */
    protected void findPrefetchedFragments(SimpleFragment hierFragment,
            List<RowId> bulkRowIds, Set<Serializable> proxyIds)
            throws StorageException {
        Serializable id = hierFragment.row.id;

        // find type
        String typeName = (String) hierFragment.get(Model.MAIN_PRIMARY_TYPE_KEY);
        if (Model.PROXY_TYPE.equals(typeName)) {
            if (proxyIds != null) {
                proxyIds.add(id);
            }
            return;
        }

        // find table names
        Set<String> tableNames = model.getTypePrefetchedFragments(typeName);
        if (tableNames == null) {
            return; // unknown (obsolete) type
        }

        // add row id for each table name
        Serializable parentId = hierFragment.get(Model.HIER_PARENT_KEY);
        for (String tableName : tableNames) {
            if (Model.HIER_TABLE_NAME.equals(tableName)) {
                continue; // already fetched
            }
            if (parentId != null && Model.VERSION_TABLE_NAME.equals(tableName)) {
                continue; // not a version, don't fetch this table
                // TODO incorrect if we have filed versions
            }
            bulkRowIds.add(new RowId(tableName, id));
        }
    }

    @Override
    public List<Node> getNodesByIds(List<Serializable> ids)
            throws StorageException {
        checkThread();
        checkLive();
        return getNodesByIds(ids, true);
    }

    @Override
    public Node getParentNode(Node node) throws StorageException {
        checkLive();
        if (node == null) {
            throw new IllegalArgumentException("Illegal null node");
        }
        Serializable id = node.getHierFragment().get(Model.HIER_PARENT_KEY);
        return id == null ? null : getNodeById(id);
    }

    @Override
    public String getPath(Node node) throws StorageException {
        checkLive();
        String path = node.getPath();
        if (path == null) {
            path = context.getPath(node.getHierFragment());
        }
        return path;
    }

    /* Does not apply to properties for now (no use case). */
    @Override
    public Node getNodeByPath(String path, Node node) throws StorageException {
        // TODO optimize this to use a dedicated path-based table
        checkLive();
        if (path == null) {
            throw new IllegalArgumentException("Illegal null path");
        }
        path = Normalizer.normalize(path, Normalizer.Form.NFKC);
        int i;
        if (path.startsWith("/")) {
            node = getRootNode();
            if (path.equals("/")) {
                return node;
            }
            i = 1;
        } else {
            if (node == null) {
                throw new IllegalArgumentException(
                        "Illegal relative path with null node: " + path);
            }
            i = 0;
        }
        String[] names = path.split("/", -1);
        for (; i < names.length; i++) {
            String name = names[i];
            if (name.length() == 0) {
                throw new IllegalArgumentException(
                        "Illegal path with empty component: " + path);
            }
            node = getChildNode(node, name, false);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    @Override
    public boolean addMixinType(Node node, String mixin)
            throws StorageException {
        if (model.getMixinPropertyInfos(mixin) == null) {
            throw new IllegalArgumentException("No such mixin: " + mixin);
        }
        if (model.getDocumentTypeFacets(node.getPrimaryType()).contains(mixin)) {
            return false; // already present in type
        }
        List<String> list = new ArrayList<String>(Arrays.asList(node.getMixinTypes()));
        if (list.contains(mixin)) {
            return false; // already present in node
        }
        list.add(mixin);
        String[] mixins = list.toArray(new String[list.size()]);
        node.hierFragment.put(model.MAIN_MIXIN_TYPES_KEY, mixins);
        // immediately create child nodes (for complex properties) in order
        // to avoid concurrency issue later on
        Map<String, String> childrenTypes = model.getMixinComplexChildren(mixin);
        for (Entry<String, String> es : childrenTypes.entrySet()) {
            String childName = es.getKey();
            String childType = es.getValue();
            addChildNode(node, childName, null, childType, true);
        }
        return true;
    }

    @Override
    public boolean removeMixinType(Node node, String mixin)
            throws StorageException {
        List<String> list = new ArrayList<String>(
                Arrays.asList(node.getMixinTypes()));
        if (!list.remove(mixin)) {
            return false; // not present in node
        }
        String[] mixins = list.toArray(new String[list.size()]);
        if (mixins.length == 0) {
            mixins = null;
        }
        node.hierFragment.put(model.MAIN_MIXIN_TYPES_KEY, mixins);
        // remove child nodes
        Map<String, String> childrenTypes = model.getMixinComplexChildren(mixin);
        for (String childName: childrenTypes.keySet()) {
            Node child = getChildNode(node, childName, true);
            removePropertyNode(child);
        }
        node.clearCache();
        return true;
    }

    @Override
    public Node addChildNode(Node parent, String name, Long pos,
            String typeName, boolean complexProp) throws StorageException {
        if (pos == null && !complexProp && parent != null) {
            pos = context.getNextPos(parent.getId(), complexProp);
        }
        return addChildNode(null, parent, name, pos, typeName, complexProp);
    }

    @Override
    public Node addChildNode(Serializable id, Node parent, String name,
            Long pos, String typeName, boolean complexProp)
            throws StorageException {
        checkLive();
        if (name == null) {
            throw new IllegalArgumentException("Illegal null name");
        }
        name = Normalizer.normalize(name, Normalizer.Form.NFKC);
        if (name.contains("/") || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        if (!model.isType(typeName)) {
            throw new IllegalArgumentException("Unknown type: " + typeName);
        }
        id = generateNewId(id);
        Serializable parentId = parent == null ? null
                : parent.hierFragment.getId();
        Node node = addNode(id, parentId, name, pos, typeName, complexProp);
        // immediately create child nodes (for complex properties) in order
        // to avoid concurrency issue later on
        Map<String, String> childrenTypes = model.getTypeComplexChildren(typeName);
        for (Entry<String, String> es : childrenTypes.entrySet()) {
            String childName = es.getKey();
            String childType = es.getValue();
            addChildNode(node, childName, null, childType, true);
        }
        return node;
    }

    protected Node addNode(Serializable id, Serializable parentId, String name,
            Long pos, String typeName, boolean complexProp)
            throws StorageException {
        requireReadAclsUpdate();
        // main info
        Row hierRow = new Row(Model.HIER_TABLE_NAME, id);
        hierRow.putNew(Model.HIER_PARENT_KEY, parentId);
        hierRow.putNew(Model.HIER_CHILD_NAME_KEY, name);
        hierRow.putNew(Model.HIER_CHILD_POS_KEY, pos);
        hierRow.putNew(Model.MAIN_PRIMARY_TYPE_KEY, typeName);
        hierRow.putNew(Model.HIER_CHILD_ISPROPERTY_KEY,
                Boolean.valueOf(complexProp));
        SimpleFragment hierFragment = context.createHierarchyFragment(hierRow);
        FragmentGroup fragmentGroup = new FragmentGroup(hierFragment,
                new FragmentsMap());
        return new Node(context, fragmentGroup, context.getPath(hierFragment));
    }

    @Override
    public Node addProxy(Serializable targetId, Serializable versionableId,
            Node parent, String name, Long pos) throws StorageException {
        if (!repository.getRepositoryDescriptor().proxiesEnabled) {
            throw new StorageException("Proxies are disabled by configuration");
        }
        Node proxy = addChildNode(parent, name, pos, Model.PROXY_TYPE, false);
        proxy.setSimpleProperty(Model.PROXY_TARGET_PROP, targetId);
        proxy.setSimpleProperty(Model.PROXY_VERSIONABLE_PROP, versionableId);
        SimpleFragment proxyFragment = (SimpleFragment) proxy.fragments.get(Model.PROXY_TABLE_NAME);
        context.createdProxyFragment(proxyFragment);
        return proxy;
    }

    @Override
    public void setProxyTarget(Node proxy, Serializable targetId)
            throws StorageException {
        if (!repository.getRepositoryDescriptor().proxiesEnabled) {
            throw new StorageException("Proxies are disabled by configuration");
        }
        SimpleProperty prop = proxy.getSimpleProperty(Model.PROXY_TARGET_PROP);
        Serializable oldTargetId = prop.getValue();
        if (!oldTargetId.equals(targetId)) {
            SimpleFragment proxyFragment = (SimpleFragment) proxy.fragments.get(Model.PROXY_TABLE_NAME);
            context.removedProxyTarget(proxyFragment);
            proxy.setSimpleProperty(Model.PROXY_TARGET_PROP, targetId);
            context.addedProxyTarget(proxyFragment);
        }
    }

    @Override
    public boolean hasChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        checkLive();
        // TODO could optimize further by not fetching the fragment at all
        SimpleFragment fragment = context.getChildHierByName(parent.getId(),
                name, complexProp);
        return fragment != null;
    }

    @Override
    public Node getChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        checkLive();
        if (name == null || name.contains("/") || name.equals(".")
                || name.equals("..")) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        SimpleFragment fragment = context.getChildHierByName(parent.getId(),
                name, complexProp);
        return fragment == null ? null : getNodeById(fragment.getId());
    }

    // TODO optimize with dedicated backend call
    @Override
    public boolean hasChildren(Node parent, boolean complexProp)
            throws StorageException {
        checkLive();
        List<SimpleFragment> children = context.getChildren(parent.getId(),
                null, complexProp);
        return children.size() > 0;
    }

    @Override
    public List<Node> getChildren(Node parent, String name, boolean complexProp)
            throws StorageException {
        checkLive();
        List<SimpleFragment> fragments = context.getChildren(parent.getId(),
                name, complexProp);
        List<Node> nodes = new ArrayList<Node>(fragments.size());
        for (SimpleFragment fragment : fragments) {
            Node node = getNodeById(fragment.getId());
            if (node == null) {
                // cannot happen
                log.error("Child node cannot be created: " + fragment.getId());
                continue;
            }
            nodes.add(node);
        }
        return nodes;
    }

    @Override
    public void orderBefore(Node parent, Node source, Node dest)
            throws StorageException {
        checkLive();
        context.orderBefore(parent.getId(), source.getId(), dest == null ? null
                : dest.getId());
    }

    @Override
    public Node move(Node source, Node parent, String name)
            throws StorageException {
        checkLive();
        if (!parent.getId().equals(source.getParentId())) {
            flush(); // needed when doing many moves for circular stuff
        }
        context.move(source, parent.getId(), name);
        requireReadAclsUpdate();
        return source;
    }

    @Override
    public Node copy(Node source, Node parent, String name)
            throws StorageException {
        checkLive();
        flush();
        Serializable id = context.copy(source, parent.getId(), name);
        requireReadAclsUpdate();
        return getNodeById(id);
    }

    @Override
    public void removeNode(Node node) throws StorageException {
        checkLive();
        flush();
        context.removeNode(node.getHierFragment());
    }

    @Override
    public void removePropertyNode(Node node) throws StorageException {
        checkLive();
        // no flush needed
        context.removePropertyNode(node.getHierFragment());
    }

    @Override
    public Node checkIn(Node node, String label, String checkinComment)
            throws StorageException {
        checkLive();
        flush();
        Serializable id = context.checkIn(node, label, checkinComment);
        requireReadAclsUpdate();
        // save to reflect changes immediately in database
        flush();
        return getNodeById(id);
    }

    @Override
    public void checkOut(Node node) throws StorageException {
        checkLive();
        context.checkOut(node);
        requireReadAclsUpdate();
    }

    @Override
    public void restore(Node node, Node version) throws StorageException {
        checkLive();
        // save done inside method
        context.restoreVersion(node, version);
        requireReadAclsUpdate();
    }

    @Override
    public Node getVersionByLabel(Serializable versionSeriesId, String label)
            throws StorageException {
        if (label == null) {
            return null;
        }
        List<Node> versions = getVersions(versionSeriesId);
        for (Node node : versions) {
            String l = (String) node.getSimpleProperty(Model.VERSION_LABEL_PROP).getValue();
            if (label.equals(l)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Node getLastVersion(Serializable versionSeriesId)
            throws StorageException {
        checkLive();
        List<Serializable> ids = context.getVersionIds(versionSeriesId);
        return ids.isEmpty() ? null : getNodeById(ids.get(ids.size() - 1));
    }

    @Override
    public List<Node> getVersions(Serializable versionSeriesId)
            throws StorageException {
        checkLive();
        List<Serializable> ids = context.getVersionIds(versionSeriesId);
        List<Node> nodes = new ArrayList<Node>(ids.size());
        for (Serializable id : ids) {
            nodes.add(getNodeById(id));
        }
        return nodes;
    }

    @Override
    public List<Node> getProxies(Node document, Node parent)
            throws StorageException {
        checkLive();
        if (!repository.getRepositoryDescriptor().proxiesEnabled) {
            return Collections.emptyList();
        }

        List<Serializable> ids;
        if (document.isVersion()) {
            ids = context.getTargetProxyIds(document.getId());
        } else {
            Serializable versionSeriesId;
            if (document.isProxy()) {
                versionSeriesId = document.getSimpleProperty(
                        Model.PROXY_VERSIONABLE_PROP).getValue();
            } else {
                versionSeriesId = document.getId();
            }
            ids = context.getSeriesProxyIds(versionSeriesId);
        }

        List<Node> nodes = new LinkedList<Node>();
        for (Serializable id : ids) {
            Node node = getNodeById(id);
            if (node != null || Boolean.TRUE.booleanValue()) { // XXX
                // null if deleted, which means selection wasn't correctly
                // updated
                nodes.add(node);
            }
        }

        if (parent != null) {
            // filter by parent
            Serializable parentId = parent.getId();
            for (Iterator<Node> it = nodes.iterator(); it.hasNext();) {
                Node node = it.next();
                if (!parentId.equals(node.getParentId())) {
                    it.remove();
                }
            }
        }

        return nodes;
    }

    /**
     * Fetches the hierarchy fragment for the given rows and all their
     * ancestors.
     *
     * @param ids the fragment ids
     */
    protected List<Fragment> getHierarchyAndAncestors(
            Collection<Serializable> ids) throws StorageException {
        Set<Serializable> allIds = mapper.getAncestorsIds(ids);
        allIds.addAll(ids);
        List<RowId> rowIds = new ArrayList<RowId>(allIds.size());
        for (Serializable id : allIds) {
            rowIds.add(new RowId(Model.HIER_TABLE_NAME, id));
        }
        return context.getMulti(rowIds, true);
    }

    @Override
    public PartialList<Serializable> query(String query,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.query(query, NXQL.NXQL, queryFilter, countTotal);
        } finally  {
            timerContext.stop();
        }
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.query(query, queryType, queryFilter, countTotal);
        } finally  {
            timerContext.stop();
        }
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType,
            QueryFilter queryFilter, long countUpTo) throws StorageException {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.query(query, queryType, queryFilter, countUpTo);
        } finally  {
            timerContext.stop();
        }
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws StorageException {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.queryAndFetch(query, queryType, queryFilter, params);
        } finally  {
            timerContext.stop();
        }
    }

    @Override
    public Lock getLock(Serializable id) throws StorageException {
        return repository.getLockManager().getLock(id);
    }

    @Override
    public Lock setLock(Serializable id, Lock lock) throws StorageException {
        if (lock == null) {
            throw new NullPointerException("Attempt to use null lock on: " + id);
        }
        return repository.getLockManager().setLock(id, lock);
    }

    @Override
    public Lock removeLock(Serializable id, String owner, boolean force)
            throws StorageException {
        return repository.getLockManager().removeLock(id, owner);
    }

    @Override
    public void requireReadAclsUpdate() {
        readAclsChanged = true;
    }

    @Override
    public void updateReadAcls() throws StorageException {
        final Timer.Context timerContext = aclrUpdateTimer.time();
        try {
            mapper.updateReadAcls();
            readAclsChanged = false;
        } finally {
            timerContext.stop();
        }
    }

    @Override
    public void rebuildReadAcls() throws StorageException {
        mapper.rebuildReadAcls();
        readAclsChanged = false;
    }

    private void computeRootNode() throws StorageException {
        String repositoryId = repository.getName();
        Serializable rootId = mapper.getRootId(repositoryId);
        if (rootId == null && COMPAT_REPOSITORY_NAME) {
            // compat, old repositories had fixed id "default"
            rootId = mapper.getRootId("default");
        }
        if (rootId == null) {
            log.debug("Creating root");
            rootNode = addRootNode();
            addRootACP();
            save();
            // record information about the root id
            mapper.setRootId(repositoryId, rootNode.getId());
        } else {
            rootNode = getNodeById(rootId, false);
        }
    }

    // TODO factor with addChildNode
    private Node addRootNode() throws StorageException {
        Serializable id = generateNewId(null);
        return addNode(id, null, "", null, Model.ROOT_TYPE, false);
    }

    private void addRootACP() throws StorageException {
        ACLRow[] aclrows = new ACLRow[3];
        // TODO put groups in their proper place. like that now for consistency.
        aclrows[0] = new ACLRow(0, ACL.LOCAL_ACL, true,
                SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATORS,
                null);
        aclrows[1] = new ACLRow(1, ACL.LOCAL_ACL, true,
                SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATOR,
                null);
        aclrows[2] = new ACLRow(2, ACL.LOCAL_ACL, true, SecurityConstants.READ,
                SecurityConstants.MEMBERS, null);
        rootNode.setCollectionProperty(Model.ACL_PROP, aclrows);
        requireReadAclsUpdate();
    }

    // public Node newNodeInstance() needed ?

    public void checkPermission(String absPath, String actions)
            throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public boolean hasPendingChanges() throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public void markReferencedBinaries(BinaryGarbageCollector gc) {
        checkLive();
        try {
            mapper.markReferencedBinaries(gc);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        checkLive();
        if (!repository.getRepositoryDescriptor().softDeleteEnabled) {
            return 0;
        }
        try {
            return mapper.cleanupDeletedRows(max, beforeTime);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * ----- XAResource -----
     */

    @Override
    public boolean isSameRM(XAResource xaresource) {
        return xaresource == this;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        if (flags == TMNOFLAGS) {
            try {
                processReceivedInvalidations();
            } catch (Exception e) {
                log.error("Could not start transaction", e);
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
            }
        }
        mapper.start(xid, flags);
        inTransaction = true;
        checkThreadStart();
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        boolean failed = true;
        try {
            if (flags != TMFAIL) {
                try {
                    flushAndScheduleWork();
                } catch (Exception e) {
                    String msg = "Could not end transaction";
                    if (e instanceof ConcurrentModificationException) {
                        log.debug(msg, e);
                    } else {
                        log.error(msg, e);
                    }
                    throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
                }
            }
            failed = false;
            mapper.end(xid, flags);
        } finally {
            if (failed) {
                try {
                    mapper.end(xid, TMFAIL);
                } finally {
                    rollback(xid);
                }
            }
        }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        int res = mapper.prepare(xid);
        if (res == XA_RDONLY) {
            // Read-only optimization, commit() won't be called by the TM.
            // It's important to nevertheless send invalidations because
            // Oracle, in tightly-coupled transaction mode, can return
            // this status even when some changes were actually made
            // (they just will be committed by another resource).
            // See NXP-7943
            commitDone();
        }
        return res;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            mapper.commit(xid, onePhase);
        } finally {
            commitDone();
        }
    }

    protected void commitDone() throws XAException {
        inTransaction = false;
        try {
            try {
                sendInvalidationsToOthers();
            } finally {
                checkThreadEnd();
            }
        } catch (Exception e) {
            log.error("Could not send invalidations", e);
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        try {
            try {
                mapper.rollback(xid);
            } finally {
                rollback();
            }
        } finally {
            inTransaction = false;
            // no invalidations to send
            checkThreadEnd();
        }
    }

    @Override
    public void forget(Xid xid) throws XAException {
        mapper.forget(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return mapper.recover(flag);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return mapper.setTransactionTimeout(seconds);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return mapper.getTransactionTimeout();
    }

    public long getCacheSize() {
        return context.getCacheSize();
    }

    public long getCacheMapperSize() {
        return context.getCacheMapperSize();
    }

    public long getCachePristineSize() {
        return context.getCachePristineSize();
    }

    public long getCacheSelectionSize() {
        return context.getCacheSelectionSize();
    }

}
