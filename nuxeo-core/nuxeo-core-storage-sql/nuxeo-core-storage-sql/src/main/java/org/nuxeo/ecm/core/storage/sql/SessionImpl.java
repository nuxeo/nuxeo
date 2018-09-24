/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentExistsException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.FulltextExtractorWork;
import org.nuxeo.ecm.core.storage.sql.PersistenceContext.PathAndId;
import org.nuxeo.ecm.core.storage.sql.RowMapper.NodeInfo;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowBatch;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * The session is the main high level access point to data from the underlying database.
 */
public class SessionImpl implements Session, XAResource {

    private static final Log log = LogFactory.getLog(SessionImpl.class);

    /**
     * Set this system property to false if you don't want repositories to be looked up under the compatibility name
     * "default" in the "repositories" table.
     * <p>
     * Only do this if you start from an empty database, or if you have migrated the "repositories" table by hand, or if
     * you need to create a new repository in a database already containing a "default" repository (table sharing, not
     * recommended).
     */
    public static final String COMPAT_REPOSITORY_NAME_KEY = "org.nuxeo.vcs.repository.name.default.compat";

    private static final boolean COMPAT_REPOSITORY_NAME = Boolean.parseBoolean(
            Framework.getProperty(COMPAT_REPOSITORY_NAME_KEY, "true"));

    protected final RepositoryImpl repository;

    private final Mapper mapper;

    private final Model model;

    // public because used by unit tests
    public final PersistenceContext context;

    protected final boolean changeTokenEnabled;

    private volatile boolean live;

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

    private static final java.lang.String LOG_MIN_DURATION_KEY = "org.nuxeo.vcs.query.log_min_duration_ms";

    private static final long LOG_MIN_DURATION_NS = Long.parseLong(Framework.getProperty(LOG_MIN_DURATION_KEY, "-1"))
            * 1000000;

    public SessionImpl(RepositoryImpl repository, Model model, Mapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.model = model;
        context = new PersistenceContext(model, mapper, this);
        changeTokenEnabled = repository.isChangeTokenEnabled();
        live = true;
        readAclsChanged = false;

        saveTimer = registry.timer(MetricRegistry.name("nuxeo", "repositories", repository.getName(), "saves"));
        queryTimer = registry.timer(MetricRegistry.name("nuxeo", "repositories", repository.getName(), "queries"));
        aclrUpdateTimer = registry.timer(
                MetricRegistry.name("nuxeo", "repositories", repository.getName(), "aclr-updates"));

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
     * Gets the XAResource. Called by the ManagedConnectionImpl, which actually wraps it in a connection-aware
     * implementation.
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
                "Concurrency Error: Session was started in thread %s (%s)" + " but is being used in thread %s (%s)",
                threadId, threadName, currentThreadId, currentThreadName);
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
    protected Serializable generateNewId(Serializable id) {
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
            checkLive();
            closeSession();
            repository.closeSession(this);
        } catch (Exception cause) {
            throw new ResourceException(cause);
        }
    }

    protected void closeSession() {
        live = false;
        context.clearCaches();
        // close the mapper and therefore the connection
        mapper.close();
        // don't clean the caches, we keep the pristine cache around
        // TODO this is getting destroyed, we can clean everything
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
    public String getRepositoryName() {
        return repository.getName();
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public Node getRootNode() {
        checkLive();
        return rootNode;
    }

    @Override
    public void save() {
        final Timer.Context timerContext = saveTimer.time();
        try {
            checkLive();
            flush();
            if (!inTransaction) {
                sendInvalidationsToOthers();
                // as we don't have a way to know when the next
                // non-transactional
                // statement will start, process invalidations immediately
            }
            processReceivedInvalidations();
        } finally {
            timerContext.stop();
        }
    }

    protected void flush() {
        checkThread();
        List<Work> works;
        if (!repository.getRepositoryDescriptor().getFulltextDescriptor().getFulltextDisabled()) {
            works = getFulltextWorks();
        } else {
            works = Collections.emptyList();
        }
        doFlush();
        if (readAclsChanged) {
            updateReadAcls();
        }
        scheduleWork(works);
        checkInvalidationsConflict();
    }

    protected void scheduleWork(List<Work> works) {
        // do async fulltext indexing only if high-level sessions are available
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        if (repositoryManager != null && !works.isEmpty()) {
            WorkManager workManager = Framework.getService(WorkManager.class);
            for (Work work : works) {
                // schedule work post-commit
                // in non-tx mode, this may execute it nearly immediately
                workManager.schedule(work, Scheduling.IF_NOT_SCHEDULED, true);
            }
        }
    }

    protected void doFlush() {
        List<Fragment> fragmentsToClearDirty = new ArrayList<>(0);
        RowBatch batch = context.getSaveBatch(fragmentsToClearDirty);
        if (!batch.isEmpty()) {
            log.debug("Saving session");
            // execute the batch
            mapper.write(batch);
            log.debug("End of save");
            for (Fragment fragment : fragmentsToClearDirty) {
                fragment.clearDirty();
            }
        }
    }

    protected Serializable getContainingDocument(Serializable id) {
        return context.getContainingDocument(id);
    }

    /**
     * Gets the fulltext updates to do. Called at save() time.
     *
     * @return a list of {@link Work} instances to schedule post-commit.
     */
    protected List<Work> getFulltextWorks() {
        Set<Serializable> dirtyStrings = new HashSet<>();
        Set<Serializable> dirtyBinaries = new HashSet<>();
        context.findDirtyDocuments(dirtyStrings, dirtyBinaries);
        if (model.getFulltextConfiguration().fulltextSearchDisabled) {
            // We only need to update dirty simple strings if fulltext search is not disabled
            // because in that case Elasticsearch will do its own extraction/indexing.
            // We need to detect dirty binary strings in all cases, because Elasticsearch
            // will need them even if the repository itself doesn't use them for search.
            dirtyStrings = Collections.emptySet();
        }
        Set<Serializable> dirtyIds = new HashSet<>();
        dirtyIds.addAll(dirtyStrings);
        dirtyIds.addAll(dirtyBinaries);
        if (dirtyIds.isEmpty()) {
            return Collections.emptyList();
        }
        markIndexingInProgress(dirtyIds);
        List<Work> works = new ArrayList<>(dirtyIds.size());
        for (Serializable id : dirtyIds) {
            boolean updateSimpleText = dirtyStrings.contains(id);
            boolean updateBinaryText = dirtyBinaries.contains(id);
            Work work = new FulltextExtractorWork(repository.getName(), model.idToString(id), updateSimpleText,
                    updateBinaryText, true);
            works.add(work);
        }
        return works;
    }

    /**
     * Mark indexing in progress, so that future copies (including versions) will be indexed as well.
     */
    protected void markIndexingInProgress(Set<Serializable> dirtyIds) {
        FulltextConfiguration fulltextConfiguration = model.getFulltextConfiguration();
        for (Node node : getNodesByIds(dirtyIds)) {
            if (!fulltextConfiguration.isFulltextIndexable(node.getPrimaryType())) {
                continue;
            }
            node.getSimpleProperty(Model.FULLTEXT_JOBID_PROP).setValue(model.idToString(node.getId()));
        }
    }

    /**
     * Post-transaction invalidations notification.
     * <p>
     * Called post-transaction by session commit/rollback or transactionless save.
     */
    protected void sendInvalidationsToOthers() {
        context.sendInvalidationsToOthers();
    }

    /**
     * Processes all invalidations accumulated.
     * <p>
     * Called pre-transaction by start or transactionless save;
     */
    protected void processReceivedInvalidations() {
        context.processReceivedInvalidations();
    }

    /**
     * Post transaction check invalidations processing.
     */
    protected void checkInvalidationsConflict() {
        // repository.receiveClusterInvalidations(this);
        context.checkInvalidationsConflict();
    }

    /*
     * -------------------------------------------------------------
     * -------------------------------------------------------------
     * -------------------------------------------------------------
     */

    protected Node getNodeById(Serializable id, boolean prefetch) {
        List<Node> nodes = getNodesByIds(Collections.singletonList(id), prefetch);
        Node node = nodes.get(0);
        // ((JDBCMapper) ((CachingMapper)
        // mapper).mapper).logger.log("getNodeById " + id + " -> " + (node ==
        // null ? "missing" : "found"));
        return node;
    }

    @Override
    public Node getNodeById(Serializable id) {
        checkLive();
        if (id == null) {
            throw new IllegalArgumentException("Illegal null id");
        }
        return getNodeById(id, true);
    }

    public List<Node> getNodesByIds(Collection<Serializable> ids, boolean prefetch) {
        // get hier fragments
        List<RowId> hierRowIds = new ArrayList<>(ids.size());
        for (Serializable id : ids) {
            hierRowIds.add(new RowId(Model.HIER_TABLE_NAME, id));
        }

        List<Fragment> hierFragments = context.getMulti(hierRowIds, false);

        // find available paths
        Map<Serializable, String> paths = new HashMap<>();
        Set<Serializable> parentIds = new HashSet<>();
        for (Fragment fragment : hierFragments) {
            Serializable id = fragment.getId();
            PathAndId pathOrId = context.getPathOrMissingParentId((SimpleFragment) fragment, false);
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
        Map<Serializable, FragmentGroup> fragmentGroups = new HashMap<>(ids.size());
        for (Fragment fragment : hierFragments) {
            Serializable id = fragment.row.id;
            fragmentGroups.put(id, new FragmentGroup((SimpleFragment) fragment, new FragmentsMap()));
        }

        if (prefetch) {
            List<RowId> bulkRowIds = new ArrayList<>();
            Set<Serializable> proxyIds = new HashSet<>();

            // get rows to prefetch for hier fragments
            for (Fragment fragment : hierFragments) {
                findPrefetchedFragments((SimpleFragment) fragment, bulkRowIds, proxyIds);
            }

            // proxies

            // get proxies fragments
            List<RowId> proxiesRowIds = new ArrayList<>(proxyIds.size());
            for (Serializable id : proxyIds) {
                proxiesRowIds.add(new RowId(Model.PROXY_TABLE_NAME, id));
            }
            List<Fragment> proxiesFragments = context.getMulti(proxiesRowIds, true);
            Set<Serializable> targetIds = new HashSet<>();
            for (Fragment fragment : proxiesFragments) {
                Serializable targetId = ((SimpleFragment) fragment).get(Model.PROXY_TARGET_KEY);
                targetIds.add(targetId);
            }

            // get hier fragments for proxies' targets
            targetIds.removeAll(ids); // only those we don't have already
            hierRowIds = new ArrayList<>(targetIds.size());
            for (Serializable id : targetIds) {
                hierRowIds.add(new RowId(Model.HIER_TABLE_NAME, id));
            }
            hierFragments = context.getMulti(hierRowIds, true);
            for (Fragment fragment : hierFragments) {
                findPrefetchedFragments((SimpleFragment) fragment, bulkRowIds, null);
            }

            // we have everything to be prefetched

            // fetch all the prefetches in bulk
            List<Fragment> fragments = context.getMulti(bulkRowIds, true);

            // put each fragment in the map of the proper group
            for (Fragment fragment : fragments) {
                FragmentGroup fragmentGroup = fragmentGroups.get(fragment.row.id);
                if (fragmentGroup != null) {
                    fragmentGroup.fragments.put(fragment.row.tableName, fragment);
                }
            }
        }

        // assemble nodes from the fragment groups
        List<Node> nodes = new ArrayList<>(ids.size());
        for (Serializable id : ids) {
            FragmentGroup fragmentGroup = fragmentGroups.get(id);
            // null if deleted/absent
            Node node = fragmentGroup == null ? null : new Node(context, fragmentGroup, paths.get(id));
            nodes.add(node);
        }

        return nodes;
    }

    /**
     * Finds prefetched fragments for a hierarchy fragment, takes note of the ones that are proxies.
     */
    protected void findPrefetchedFragments(SimpleFragment hierFragment, List<RowId> bulkRowIds,
            Set<Serializable> proxyIds) {
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
    public List<Node> getNodesByIds(Collection<Serializable> ids) {
        checkLive();
        return getNodesByIds(ids, true);
    }

    @Override
    public Node getParentNode(Node node) {
        checkLive();
        if (node == null) {
            throw new IllegalArgumentException("Illegal null node");
        }
        Serializable id = node.getHierFragment().get(Model.HIER_PARENT_KEY);
        return id == null ? null : getNodeById(id);
    }

    @Override
    public String getPath(Node node) {
        checkLive();
        String path = node.getPath();
        if (path == null) {
            path = context.getPath(node.getHierFragment());
        }
        return path;
    }

    /*
     * Normalize using NFC to avoid decomposed characters (like 'e' + COMBINING ACUTE ACCENT instead of LATIN SMALL
     * LETTER E WITH ACUTE). NFKC (normalization using compatibility decomposition) is not used, because compatibility
     * decomposition turns some characters (LATIN SMALL LIGATURE FFI, TRADE MARK SIGN, FULLWIDTH SOLIDUS) into a series
     * of characters ('f'+'f'+'i', 'T'+'M', '/') that cannot be re-composed into the original, and therefore loses
     * information.
     */
    protected String normalize(String path) {
        return Normalizer.normalize(path, Normalizer.Form.NFC);
    }

    /* Does not apply to properties for now (no use case). */
    @Override
    public Node getNodeByPath(String path, Node node) {
        // TODO optimize this to use a dedicated path-based table
        checkLive();
        if (path == null) {
            throw new IllegalArgumentException("Illegal null path");
        }
        path = normalize(path);
        int i;
        if (path.startsWith("/")) {
            node = getRootNode();
            if (path.equals("/")) {
                return node;
            }
            i = 1;
        } else {
            if (node == null) {
                throw new IllegalArgumentException("Illegal relative path with null node: " + path);
            }
            i = 0;
        }
        String[] names = path.split("/", -1);
        for (; i < names.length; i++) {
            String name = names[i];
            if (name.length() == 0) {
                throw new IllegalArgumentException("Illegal path with empty component: " + path);
            }
            node = getChildNode(node, name, false);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    @Override
    public boolean addMixinType(Node node, String mixin) {
        if (model.getMixinPropertyInfos(mixin) == null) {
            throw new IllegalArgumentException("No such mixin: " + mixin);
        }
        if (model.getDocumentTypeFacets(node.getPrimaryType()).contains(mixin)) {
            return false; // already present in type
        }
        List<String> list = new ArrayList<>(Arrays.asList(node.getMixinTypes()));
        if (list.contains(mixin)) {
            return false; // already present in node
        }
        Set<String> otherChildrenNames = getChildrenNames(node.getPrimaryType(), list);
        list.add(mixin);
        String[] mixins = list.toArray(new String[list.size()]);
        node.hierFragment.put(Model.MAIN_MIXIN_TYPES_KEY, mixins);
        // immediately create child nodes (for complex properties) in order
        // to avoid concurrency issue later on
        Map<String, String> childrenTypes = model.getMixinComplexChildren(mixin);
        for (Entry<String, String> es : childrenTypes.entrySet()) {
            String childName = es.getKey();
            String childType = es.getValue();
            // child may already exist if the schema is part of the primary type or another facet
            if (otherChildrenNames.contains(childName)) {
                continue;
            }
            addChildNode(node, childName, null, childType, true);
        }
        return true;
    }

    @Override
    public boolean removeMixinType(Node node, String mixin) {
        List<String> list = new ArrayList<>(Arrays.asList(node.getMixinTypes()));
        if (!list.remove(mixin)) {
            return false; // not present in node
        }
        String[] mixins = list.toArray(new String[list.size()]);
        if (mixins.length == 0) {
            mixins = null;
        }
        node.hierFragment.put(Model.MAIN_MIXIN_TYPES_KEY, mixins);
        Set<String> otherChildrenNames = getChildrenNames(node.getPrimaryType(), list);
        Map<String, String> childrenTypes = model.getMixinComplexChildren(mixin);
        for (String childName : childrenTypes.keySet()) {
            // child must be kept if the schema is part of primary type or another facet
            if (otherChildrenNames.contains(childName)) {
                continue;
            }
            Node child = getChildNode(node, childName, true);
            removePropertyNode(child);
        }
        node.clearCache();
        return true;
    }

    @Override
    public ScrollResult<String> scroll(String query, int batchSize, int keepAliveSeconds) {
        return mapper.scroll(query, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return mapper.scroll(scrollId);
    }

    /**
     * Gets complex children names defined by the primary type and the list of mixins.
     */
    protected Set<String> getChildrenNames(String primaryType, List<String> mixins) {
        Map<String, String> cc = model.getTypeComplexChildren(primaryType);
        if (cc == null) {
            cc = Collections.emptyMap();
        }
        Set<String> childrenNames = new HashSet<>(cc.keySet());
        for (String mixin : mixins) {
            cc = model.getMixinComplexChildren(mixin);
            if (cc != null) {
                childrenNames.addAll(cc.keySet());
            }
        }
        return childrenNames;
    }

    @Override
    public Node addChildNode(Node parent, String name, Long pos, String typeName, boolean complexProp) {
        if (pos == null && !complexProp && parent != null) {
            pos = context.getNextPos(parent.getId(), complexProp);
        }
        return addChildNode(null, parent, name, pos, typeName, complexProp);
    }

    @Override
    public Node addChildNode(Serializable id, Node parent, String name, Long pos, String typeName,
            boolean complexProp) {
        checkLive();
        if (name == null) {
            throw new IllegalArgumentException("Illegal null name");
        }
        name = normalize(name);
        if (name.contains("/") || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        if (!model.isType(typeName)) {
            throw new IllegalArgumentException("Unknown type: " + typeName);
        }
        id = generateNewId(id);
        Serializable parentId = parent == null ? null : parent.hierFragment.getId();
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

    protected Node addNode(Serializable id, Serializable parentId, String name, Long pos, String typeName,
            boolean complexProp) {
        requireReadAclsUpdate();
        // main info
        Row hierRow = new Row(Model.HIER_TABLE_NAME, id);
        hierRow.putNew(Model.HIER_PARENT_KEY, parentId);
        hierRow.putNew(Model.HIER_CHILD_NAME_KEY, name);
        hierRow.putNew(Model.HIER_CHILD_POS_KEY, pos);
        hierRow.putNew(Model.MAIN_PRIMARY_TYPE_KEY, typeName);
        hierRow.putNew(Model.HIER_CHILD_ISPROPERTY_KEY, Boolean.valueOf(complexProp));
        if (changeTokenEnabled) {
            hierRow.putNew(Model.MAIN_SYS_CHANGE_TOKEN_KEY, Model.INITIAL_SYS_CHANGE_TOKEN);
        }
        SimpleFragment hierFragment = context.createHierarchyFragment(hierRow);
        FragmentGroup fragmentGroup = new FragmentGroup(hierFragment, new FragmentsMap());
        return new Node(context, fragmentGroup, context.getPath(hierFragment));
    }

    @Override
    public Node addProxy(Serializable targetId, Serializable versionableId, Node parent, String name, Long pos) {
        if (!repository.getRepositoryDescriptor().getProxiesEnabled()) {
            throw new NuxeoException("Proxies are disabled by configuration");
        }
        Node proxy = addChildNode(parent, name, pos, Model.PROXY_TYPE, false);
        proxy.setSimpleProperty(Model.PROXY_TARGET_PROP, targetId);
        proxy.setSimpleProperty(Model.PROXY_VERSIONABLE_PROP, versionableId);
        if (changeTokenEnabled) {
            proxy.setSimpleProperty(Model.MAIN_SYS_CHANGE_TOKEN_PROP, Model.INITIAL_SYS_CHANGE_TOKEN);
            proxy.setSimpleProperty(Model.MAIN_CHANGE_TOKEN_PROP, Model.INITIAL_CHANGE_TOKEN);
        }
        SimpleFragment proxyFragment = (SimpleFragment) proxy.fragments.get(Model.PROXY_TABLE_NAME);
        context.createdProxyFragment(proxyFragment);
        return proxy;
    }

    @Override
    public void setProxyTarget(Node proxy, Serializable targetId) {
        if (!repository.getRepositoryDescriptor().getProxiesEnabled()) {
            throw new NuxeoException("Proxies are disabled by configuration");
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
    public boolean hasChildNode(Node parent, String name, boolean complexProp) {
        checkLive();
        // TODO could optimize further by not fetching the fragment at all
        SimpleFragment fragment = context.getChildHierByName(parent.getId(), normalize(name), complexProp);
        return fragment != null;
    }

    @Override
    public Node getChildNode(Node parent, String name, boolean complexProp) {
        checkLive();
        if (name == null || name.contains("/") || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        SimpleFragment fragment = context.getChildHierByName(parent.getId(), name, complexProp);
        return fragment == null ? null : getNodeById(fragment.getId());
    }

    // TODO optimize with dedicated backend call
    @Override
    public boolean hasChildren(Node parent, boolean complexProp) {
        checkLive();
        List<SimpleFragment> children = context.getChildren(parent.getId(), null, complexProp);
        if (complexProp) {
            return !children.isEmpty();
        }
        if (children.isEmpty()) {
            return false;
        }
        // we have to check that type names are not obsolete, as they wouldn't be returned
        // by getChildren and we must be consistent
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        for (SimpleFragment simpleFragment : children) {
            String primaryType = simpleFragment.getString(Model.MAIN_PRIMARY_TYPE_KEY);
            if (primaryType.equals(Model.PROXY_TYPE)) {
                Node node = getNodeById(simpleFragment.getId(), false);
                Serializable targetId = node.getSimpleProperty(Model.PROXY_TARGET_PROP).getValue();
                if (targetId == null) {
                    // missing target, should not happen, ignore
                    continue;
                }
                Node target = getNodeById(targetId, false);
                if (target == null) {
                    continue;
                }
                primaryType = target.getPrimaryType();
            }
            DocumentType type = schemaManager.getDocumentType(primaryType);
            if (type == null) {
                // obsolete type, ignored in getChildren
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public List<Node> getChildren(Node parent, String name, boolean complexProp) {
        checkLive();
        List<SimpleFragment> fragments = context.getChildren(parent.getId(), name, complexProp);
        List<Node> nodes = new ArrayList<>(fragments.size());
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
    public void orderBefore(Node parent, Node source, Node dest) {
        checkLive();
        context.orderBefore(parent.getId(), source.getId(), dest == null ? null : dest.getId());
    }

    @Override
    public Node move(Node source, Node parent, String name) {
        checkLive();
        if (!parent.getId().equals(source.getParentId())) {
            flush(); // needed when doing many moves for circular stuff
        }
        context.move(source, parent.getId(), name);
        requireReadAclsUpdate();
        return source;
    }

    @Override
    public Node copy(Node source, Node parent, String name) {
        checkLive();
        flush();
        Serializable id = context.copy(source, parent.getId(), name);
        requireReadAclsUpdate();
        return getNodeById(id);
    }

    @Override
    public void removeNode(Node node) {
        checkLive();
        flush();
        // remove the lock using the lock manager
        // TODO children locks?
        Serializable id = node.getId();
        getLockManager().removeLock(model.idToString(id), null);
        // find all descendants
        List<NodeInfo> nodeInfos = context.getNodeAndDescendantsInfo(node.getHierFragment());

        // check that there is no active retention
        Set<Serializable> retentionActiveIds = nodeInfos.stream() //
                                                        .filter(info -> info.isRetentionActive)
                                                        .map(info -> info.id)
                                                        .collect(Collectors.toSet());
        if (!retentionActiveIds.isEmpty()) {
            if (retentionActiveIds.contains(id)) {
                throw new DocumentExistsException("Cannot remove " + id + ", it is under active retention");
            } else {
                throw new DocumentExistsException("Cannot remove " + id + ", subdocument "
                        + retentionActiveIds.iterator().next() + " is under active retention");
            }
        }

        // if a proxy target is removed, check that all proxies to it are removed
        if (repository.getRepositoryDescriptor().getProxiesEnabled()) {
            Set<Serializable> removedIds = nodeInfos.stream().map(info -> info.id).collect(Collectors.toSet());
            // find proxies pointing to any removed document
            Set<Serializable> proxyIds = context.getTargetProxies(removedIds);
            for (Serializable proxyId : proxyIds) {
                if (!removedIds.contains(proxyId)) {
                    Node proxy = getNodeById(proxyId);
                    Serializable targetId = (Serializable) proxy.getSingle(Model.PROXY_TARGET_PROP);
                    throw new DocumentExistsException(
                            "Cannot remove " + id + ", subdocument " + targetId + " is the target of proxy " + proxyId);
                }
            }
        }

        // remove all nodes
        context.removeNode(node.getHierFragment(), nodeInfos);
    }

    @Override
    public void removePropertyNode(Node node) {
        checkLive();
        // no flush needed
        context.removePropertyNode(node.getHierFragment());
    }

    @Override
    public Node checkIn(Node node, String label, String checkinComment) {
        checkLive();
        flush();
        Serializable id = context.checkIn(node, label, checkinComment);
        requireReadAclsUpdate();
        // save to reflect changes immediately in database
        flush();
        return getNodeById(id);
    }

    @Override
    public void checkOut(Node node) {
        checkLive();
        context.checkOut(node);
        requireReadAclsUpdate();
    }

    @Override
    public void restore(Node node, Node version) {
        checkLive();
        // save done inside method
        context.restoreVersion(node, version);
        requireReadAclsUpdate();
    }

    @Override
    public Node getVersionByLabel(Serializable versionSeriesId, String label) {
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
    public Node getLastVersion(Serializable versionSeriesId) {
        checkLive();
        List<Serializable> ids = context.getVersionIds(versionSeriesId);
        return ids.isEmpty() ? null : getNodeById(ids.get(ids.size() - 1));
    }

    @Override
    public List<Node> getVersions(Serializable versionSeriesId) {
        checkLive();
        List<Serializable> ids = context.getVersionIds(versionSeriesId);
        List<Node> nodes = new ArrayList<>(ids.size());
        for (Serializable id : ids) {
            nodes.add(getNodeById(id));
        }
        return nodes;
    }

    @Override
    public List<Node> getProxies(Node document, Node parent) {
        checkLive();
        if (!repository.getRepositoryDescriptor().getProxiesEnabled()) {
            return Collections.emptyList();
        }

        List<Serializable> ids;
        if (document.isVersion()) {
            ids = context.getTargetProxyIds(document.getId());
        } else {
            Serializable versionSeriesId;
            if (document.isProxy()) {
                versionSeriesId = document.getSimpleProperty(Model.PROXY_VERSIONABLE_PROP).getValue();
            } else {
                versionSeriesId = document.getId();
            }
            ids = context.getSeriesProxyIds(versionSeriesId);
        }

        List<Node> nodes = getNodes(ids);

        if (parent != null) {
            // filter by parent
            Serializable parentId = parent.getId();
            nodes.removeIf(node -> !parentId.equals(node.getParentId()));
        }

        return nodes;
    }

    protected List<Node> getNodes(List<Serializable> ids) {
        List<Node> nodes = new LinkedList<>();
        for (Serializable id : ids) {
            Node node = getNodeById(id);
            if (node != null || Boolean.TRUE.booleanValue()) { // XXX
                // null if deleted, which means selection wasn't correctly
                // updated
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public List<Node> getProxies(Node document) {
        checkLive();
        if (!repository.getRepositoryDescriptor().getProxiesEnabled()) {
            return Collections.emptyList();
        }
        List<Serializable> ids = context.getTargetProxyIds(document.getId());
        return getNodes(ids);
    }

    /**
     * Fetches the hierarchy fragment for the given rows and all their ancestors.
     *
     * @param ids the fragment ids
     */
    protected List<Fragment> getHierarchyAndAncestors(Collection<Serializable> ids) {
        Set<Serializable> allIds = mapper.getAncestorsIds(ids);
        allIds.addAll(ids);
        List<RowId> rowIds = new ArrayList<>(allIds.size());
        for (Serializable id : allIds) {
            rowIds.add(new RowId(Model.HIER_TABLE_NAME, id));
        }
        return context.getMulti(rowIds, true);
    }

    @Override
    public PartialList<Serializable> query(String query, QueryFilter queryFilter, boolean countTotal) {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.query(query, NXQL.NXQL, queryFilter, countTotal);
        } finally {
            timerContext.stop();
        }
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, long countUpTo) {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.query(query, queryType, queryFilter, countUpTo);
        } finally {
            long duration = timerContext.stop();
            if ((LOG_MIN_DURATION_NS >= 0) && (duration > LOG_MIN_DURATION_NS)) {
                String msg = String.format("duration_ms:\t%.2f\t%s %s\tquery\t%s", duration / 1000000.0, queryFilter,
                        countUpToAsString(countUpTo), query);
                if (log.isTraceEnabled()) {
                    log.info(msg, new Throwable("Slow query stack trace"));
                } else {
                    log.info(msg);
                }
            }
        }
    }

    private String countUpToAsString(long countUpTo) {
        if (countUpTo > 0) {
            return String.format("count total results up to %d", countUpTo);
        }
        return countUpTo == -1 ? "count total results UNLIMITED" : "";
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            Object... params) {
        return queryAndFetch(query, queryType, queryFilter, false, params);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object... params) {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.queryAndFetch(query, queryType, queryFilter, distinctDocuments, params);
        } finally {
            long duration = timerContext.stop();
            if ((LOG_MIN_DURATION_NS >= 0) && (duration > LOG_MIN_DURATION_NS)) {
                String msg = String.format("duration_ms:\t%.2f\t%s\tqueryAndFetch\t%s", duration / 1000000.0,
                        queryFilter, query);
                if (log.isTraceEnabled()) {
                    log.info(msg, new Throwable("Slow query stack trace"));
                } else {
                    log.info(msg);
                }
            }
        }
    }

    @Override
    public PartialList<Map<String, Serializable>> queryProjection(String query, String queryType,
            QueryFilter queryFilter, boolean distinctDocuments, long countUpTo, Object... params) {
        final Timer.Context timerContext = queryTimer.time();
        try {
            return mapper.queryProjection(query, queryType, queryFilter, distinctDocuments, countUpTo, params);
        } finally {
            long duration = timerContext.stop();
            if ((LOG_MIN_DURATION_NS >= 0) && (duration > LOG_MIN_DURATION_NS)) {
                String msg = String.format("duration_ms:\t%.2f\t%s\tqueryProjection\t%s", duration / 1000000.0,
                        queryFilter, query);
                if (log.isTraceEnabled()) {
                    log.info(msg, new Throwable("Slow query stack trace"));
                } else {
                    log.info(msg);
                }
            }
        }
    }

    @Override
    public LockManager getLockManager() {
        return repository.getLockManager();
    }

    @Override
    public void requireReadAclsUpdate() {
        readAclsChanged = true;
    }

    @Override
    public void updateReadAcls() {
        final Timer.Context timerContext = aclrUpdateTimer.time();
        try {
            mapper.updateReadAcls();
            readAclsChanged = false;
        } finally {
            timerContext.stop();
        }
    }

    @Override
    public void rebuildReadAcls() {
        mapper.rebuildReadAcls();
        readAclsChanged = false;
    }

    private void computeRootNode() {
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
    private Node addRootNode() {
        Serializable id = generateNewId(null);
        return addNode(id, null, "", null, Model.ROOT_TYPE, false);
    }

    private void addRootACP() {
        ACLRow[] aclrows = new ACLRow[3];
        // TODO put groups in their proper place. like that now for consistency.
        aclrows[0] = new ACLRow(0, ACL.LOCAL_ACL, true, SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATORS,
                null);
        aclrows[1] = new ACLRow(1, ACL.LOCAL_ACL, true, SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATOR,
                null);
        aclrows[2] = new ACLRow(2, ACL.LOCAL_ACL, true, SecurityConstants.READ, SecurityConstants.MEMBERS, null);
        rootNode.setCollectionProperty(Model.ACL_PROP, aclrows);
        requireReadAclsUpdate();
    }

    // public Node newNodeInstance() needed ?

    public void checkPermission(String absPath, String actions) {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public boolean hasPendingChanges() {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public void markReferencedBinaries() {
        checkLive();
        mapper.markReferencedBinaries();
    }

    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        checkLive();
        if (!repository.getRepositoryDescriptor().getSoftDeleteEnabled()) {
            return 0;
        }
        return mapper.cleanupDeletedRows(max, beforeTime);
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
            } catch (NuxeoException e) {
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
                    flush();
                } catch (ConcurrentUpdateException e) {
                    TransactionHelper.noteSuppressedException(e);
                    log.debug("Exception during transaction commit", e);
                    // set rollback only manually instead of throwing, this avoids
                    // a spurious log in Geronimo TransactionImpl and has the same effect
                    TransactionHelper.setTransactionRollbackOnly();
                    return;
                } catch (NuxeoException e) {
                    log.error("Exception during transaction commit", e);
                    throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
                }
            }
            failed = false;
            mapper.end(xid, flags);
        } finally {
            if (failed) {
                mapper.end(xid, TMFAIL);
                // rollback done by tx manager
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
        } catch (NuxeoException e) {
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

    @Override
    public Map<String, String> getBinaryFulltext(Serializable id) {
        if (repository.getRepositoryDescriptor().getFulltextDescriptor().getFulltextDisabled()) {
            return null;
        }
        RowId rowId = new RowId(Model.FULLTEXT_TABLE_NAME, id);
        return mapper.getBinaryFulltext(rowId);
    }

    @Override
    public boolean isChangeTokenEnabled() {
        return changeTokenEnabled;
    }

    @Override
    public void markUserChange(Serializable id) {
        context.markUserChange(id);
    }

}
