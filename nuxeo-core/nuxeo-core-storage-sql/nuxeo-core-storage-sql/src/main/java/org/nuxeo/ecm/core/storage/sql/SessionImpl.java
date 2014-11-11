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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.EventConstants;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Invalidations.InvalidationsPair;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowBatch;
import org.nuxeo.ecm.core.storage.sql.coremodel.BinaryTextListener;
import org.nuxeo.runtime.api.Framework;

/**
 * The session is the main high level access point to data from the underlying
 * database.
 */
public class SessionImpl implements Session, XAResource {

    private static final Log log = LogFactory.getLog(SessionImpl.class);

    private final RepositoryImpl repository;

    private final Mapper mapper;

    private final Model model;

    private final EventProducer eventProducer;

    // public because used by unit tests
    public final PersistenceContext context;

    private boolean live;

    private boolean inTransaction;

    private Node rootNode;

    private long threadId;

    private boolean readAclsChanged;

    private String threadName;

    public SessionImpl(RepositoryImpl repository, Model model, Mapper mapper,
            Credentials credentials) throws StorageException {
        this.repository = repository;
        this.mapper = mapper;
        ((CachingMapper) mapper).setSession(this);
        // this.credentials = credentials;
        this.model = model;
        context = new PersistenceContext(model, mapper, this);
        live = true;
        readAclsChanged = false;

        try {
            eventProducer = Framework.getService(EventProducer.class);
        } catch (Exception e) {
            throw new StorageException("Unable to find EventProducer", e);
        }

        computeRootNode();
    }

    private void checkLive() {
        if (!live) {
            throw new IllegalStateException("Session is not live");
        }
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
        log.debug(msg, new Exception(msg));
    }

    protected void checkThreadStart() {
        threadId = Thread.currentThread().getId();
        threadName = Thread.currentThread().getName();
    }

    protected void checkThreadEnd() {
        threadId = 0;
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
            closeSession();
        } catch (StorageException e) {
            throw new ResourceException(e);
        }
        repository.closeSession(this);
    }

    protected void closeSession() throws StorageException {
        live = false;
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
    public Binary getBinary(InputStream in) throws StorageException {
        try {
            return repository.getBinaryManager().getBinary(in);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void save() throws StorageException {
        checkLive();
        flush();
        if (!inTransaction) {
            sendInvalidationsToOthers();
            // as we don't have a way to know when the next non-transactional
            // statement will start, process invalidations immediately
            processReceivedInvalidations();
        }
    }

    protected void flush() throws StorageException {
        checkThread();
        if (!repository.getRepositoryDescriptor().fulltextDisabled) {
            updateFulltext();
        }
        flushWithoutFulltext();
        checkInvalidationsConflict();
    }

    protected void flushWithoutFulltext() throws StorageException {
        RowBatch batch = context.getSaveBatch();
        if (!batch.isEmpty() || readAclsChanged) {
            log.debug("Saving session");
            if (!batch.isEmpty()) {
                // execute the batch
                mapper.write(batch);
            }
            if (readAclsChanged) {
                updateReadAcls();
            }
            log.debug("End of save");
        }
    }

    protected Serializable getContainingDocument(Serializable id)
            throws StorageException {
        return context.getContainingDocument(id);
    }

    /**
     * Update fulltext. Called at save() time.
     */
    protected void updateFulltext() throws StorageException {
        Set<Serializable> dirtyStrings = new HashSet<Serializable>();
        Set<Serializable> dirtyBinaries = new HashSet<Serializable>();
        context.findDirtyDocuments(dirtyStrings, dirtyBinaries);
        if (dirtyStrings.isEmpty() && dirtyBinaries.isEmpty()) {
            return;
        }

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
            String documentType = document.getPrimaryType();
            String[] mixinTypes = document.getMixinTypes();

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
                String strings = findFulltext(document, paths);
                // Set the computed full text
                // On INSERT/UPDATE a trigger will change the actual fulltext
                String propName = model.FULLTEXT_SIMPLETEXT_PROP
                        + model.getFulltextIndexSuffix(indexName);
                document.setSimpleProperty(propName, strings);
            }
        }

        updateFulltextBinaries(dirtyBinaries);
    }

    protected void updateFulltextBinaries(final Set<Serializable> dirtyBinaries)
            throws StorageException {
        if (dirtyBinaries.isEmpty()) {
            return;
        }

        // mark indexation in progress
        for (Node node : getNodesByIds(new ArrayList<Serializable>(
                dirtyBinaries))) {
            node.getSimpleProperty(Model.FULLTEXT_JOBID_PROP).setValue(
                    node.getId());
        }

        log.debug("Queued documents for asynchronous fulltext extraction: "
                + dirtyBinaries.size());
        EventContext eventContext = new EventContextImpl(dirtyBinaries,
                model.getFulltextInfo());
        eventContext.setRepositoryName(getRepositoryName());
        Event event = eventContext.newEvent(BinaryTextListener.EVENT_NAME);
        try {
            eventProducer.fireEvent(event);
        } catch (ClientException e) {
            throw new StorageException(e);
        }
    }

    protected String findFulltext(Node document, Set<String> paths)
            throws StorageException {
        if (paths == null) {
            return "";
        }

        String documentType = document.getPrimaryType();
        String[] mixinTypes = document.getMixinTypes();

        List<String> strings = new LinkedList<String>();

        for (String path : paths) {
            ModelProperty pi = model.getPathPropertyInfo(documentType,
                    mixinTypes, path);
            if (pi == null) {
                continue; // doc type doesn't have this property
            }
            if (pi.propertyType != PropertyType.STRING
                    && pi.propertyType != PropertyType.ARRAY_STRING) {
                continue;
            }
            List<Node> nodes = new ArrayList<Node>(
                    Collections.singleton(document));
            String[] names = path.split("/");
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                List<Node> newNodes;
                if (i + 1 < names.length && "*".equals(names[i + 1])) {
                    // traverse complex list
                    i++;
                    newNodes = new ArrayList<Node>();
                    for (Node node : nodes) {
                        newNodes.addAll(getChildren(node, name, true));
                    }
                } else {
                    if (i == names.length - 1) {
                        // last path component: get value
                        for (Node node : nodes) {
                            if (pi.propertyType == PropertyType.STRING) {
                                String v = node.getSimpleProperty(name).getString();
                                if (v != null) {
                                    strings.add(v);
                                }
                            } else /* ARRAY_STRING */{
                                for (Serializable v : node.getCollectionProperty(
                                        name).getValue()) {
                                    if (v != null) {
                                        strings.add((String) v);
                                    }
                                }
                            }
                        }
                        newNodes = Collections.emptyList();
                    } else {
                        // traverse
                        newNodes = new ArrayList<Node>(nodes.size());
                        for (Node node : nodes) {
                            node = getChildNode(node, name, true);
                            if (node != null) {
                                newNodes.add(node);
                            }
                        }
                    }
                }
                nodes = newNodes;
            }

        }
        return StringUtils.join(strings, " ");
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
            String id = (String) rowId.id;
            String docId;
            try {
                docId = (String) getContainingDocument(id);
            } catch (StorageException e) {
                log.error("Cannot get containing document for: " + id, e);
                docId = null;
            }
            if (docId == null) {
                continue;
            }
            if (Invalidations.PARENT.equals(rowId.tableName)) {
                if (docId.equals(id)) {
                    parents.add(docId);
                } else { // complex prop added/removed
                    docs.add(docId);
                }
            } else {
                docs.add(docId);
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
        // compute modified doc ids and parent ids
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
        if (context.isDeleted(id)) {
            return null;
        }
        List<Node> nodes = getNodesByIds(Collections.singletonList(id),
                prefetch);
        return nodes.get(0);
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
            hierRowIds.add(new RowId(model.HIER_TABLE_NAME, id));
        }

        List<Fragment> hierFragments = context.getMulti(hierRowIds, false);
        // the ids usually come from a query, in which case we don't need to
        // check if they are removed using isDeleted()

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
                proxiesRowIds.add(new RowId(model.PROXY_TABLE_NAME, id));
            }
            List<Fragment> proxiesFragments = context.getMulti(proxiesRowIds,
                    true);
            Set<Serializable> targetIds = new HashSet<Serializable>();
            for (Fragment fragment : proxiesFragments) {
                Serializable targetId = ((SimpleFragment) fragment).get(model.PROXY_TARGET_KEY);
                targetIds.add(targetId);
            }

            // get hier fragments for proxies' targets
            targetIds.removeAll(ids); // only those we don't have already
            hierRowIds = new ArrayList<RowId>(targetIds.size());
            for (Serializable id : targetIds) {
                hierRowIds.add(new RowId(model.HIER_TABLE_NAME, id));
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
                    fragmentGroup);
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
        String typeName = (String) hierFragment.get(model.MAIN_PRIMARY_TYPE_KEY);
        if (model.PROXY_TYPE.equals(typeName)) {
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
        Serializable parentId = hierFragment.get(model.HIER_PARENT_KEY);
        for (String tableName : tableNames) {
            if (model.HIER_TABLE_NAME.equals(tableName)) {
                continue; // already fetched
            }
            if (parentId != null && model.VERSION_TABLE_NAME.equals(tableName)) {
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
        Serializable id = node.getHierFragment().get(model.HIER_PARENT_KEY);
        return id == null ? null : getNodeById(id);
    }

    @Override
    public String getPath(Node node) throws StorageException {
        checkLive();
        List<String> list = new LinkedList<String>();
        while (node != null) {
            list.add(node.getName());
            node = getParentNode(node);
        }
        if (list.size() == 1) {
            String name = list.get(0);
            if (name == null || name.length() == 0) {
                // root, special case
                // (empty string for normal databases, null for Oracle)
                return "/";
            } else {
                // placeless document, no initial slash
                return name;
            }
        }
        Collections.reverse(list);
        return StringUtils.join(list, "/");
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

        return addNode(id, parentId, name, pos, typeName, complexProp);
    }

    protected Node addNode(Serializable id, Serializable parentId, String name,
            Long pos, String typeName, boolean complexProp)
            throws StorageException {
        requireReadAclsUpdate();
        // main info
        Row hierRow = new Row(model.HIER_TABLE_NAME, id);
        hierRow.putNew(model.HIER_PARENT_KEY, parentId);
        hierRow.putNew(model.HIER_CHILD_NAME_KEY, name);
        hierRow.putNew(model.HIER_CHILD_POS_KEY, pos);
        hierRow.putNew(model.MAIN_PRIMARY_TYPE_KEY, typeName);
        hierRow.putNew(model.HIER_CHILD_ISPROPERTY_KEY,
                Boolean.valueOf(complexProp));
        SimpleFragment hierFragment = context.createSimpleFragment(hierRow);
        // TODO if non-lazy creation of some fragments, create them here
        // for (String tableName : model.getTypeSimpleFragments(typeName)) {
        FragmentGroup fragmentGroup = new FragmentGroup(hierFragment,
                new FragmentsMap());
        return new Node(context, fragmentGroup);
    }

    @Override
    public Node addProxy(Serializable targetId, Serializable versionableId,
            Node parent, String name, Long pos) throws StorageException {
        Node proxy = addChildNode(parent, name, pos, Model.PROXY_TYPE, false);
        proxy.setSimpleProperty(model.PROXY_TARGET_PROP, targetId);
        proxy.setSimpleProperty(model.PROXY_VERSIONABLE_PROP, versionableId);
        return proxy;
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
        boolean isVersion = Boolean.TRUE.equals(node.getSimpleProperty(
                model.MAIN_IS_VERSION_PROP).getValue());
        Serializable versionSeriesId = null;
        if (isVersion) {
            versionSeriesId = node.getSimpleProperty(
                    model.VERSION_VERSIONABLE_PROP).getValue();
        }

        context.removeNode(node.getHierFragment());

        // for versions there's stuff we have to recompute
        if (isVersion) {
            context.recomputeVersionSeries(versionSeriesId);
        }
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
        checkLive();
        flush();
        Serializable id = mapper.getVersionIdByLabel(versionSeriesId, label);
        return id == null ? null : getNodeById(id);
    }

    @Override
    public Node getLastVersion(Serializable versionSeriesId)
            throws StorageException {
        checkLive();
        flush();
        Serializable id = mapper.getLastVersionId(versionSeriesId);
        return id == null ? null : getNodeById(id);
    }

    @Override
    public List<Node> getVersions(Serializable versionSeriesId)
            throws StorageException {
        checkLive();
        flush();
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
        flush();

        // find the versionable id
        boolean byTarget;
        Serializable searchId;
        if (document.isVersion()) {
            byTarget = true;
            searchId = document.getId();
        } else {
            byTarget = false;
            if (document.isProxy()) {
                searchId = document.getSimpleProperty(
                        model.PROXY_VERSIONABLE_PROP).getString();
            } else {
                searchId = document.getId();
            }
        }
        Serializable parentId = parent == null ? null : parent.getId();

        List<Serializable> ids = context.getProxyIds(searchId, byTarget,
                parentId);
        List<Node> nodes = new ArrayList<Node>(ids.size());
        for (Serializable id : ids) {
            nodes.add(getNodeById(id));
        }
        return nodes;
    }

    @Override
    public PartialList<Serializable> query(String query,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        return mapper.query(query, queryFilter, countTotal);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws StorageException {
        return mapper.queryAndFetch(query, queryType, queryFilter, params);
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
        mapper.updateReadAcls();
        readAclsChanged = false;
    }

    @Override
    public void rebuildReadAcls() throws StorageException {
        mapper.rebuildReadAcls();
        readAclsChanged = false;
    }

    private void computeRootNode() throws StorageException {
        String repositoryId = "default"; // TODO use repo name
        Serializable rootId = mapper.getRootId(repositoryId);
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
        return addNode(id, null, "", null, model.ROOT_TYPE, false);
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
                    flush();
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
        return mapper.prepare(xid);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            mapper.commit(xid, onePhase);
        } finally {
            inTransaction = false;
            try {
                try {
                    sendInvalidationsToOthers();
                } finally {
                    checkThreadEnd();
                }
            } catch (Exception e) {
                log.error("Could not commit transaction", e);
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
            }
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

}
