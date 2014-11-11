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
import java.io.Serializable;
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
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
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

    public Interaction createInteraction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    public ConnectionMetaData getMetaData() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Session -----
     */

    public boolean isLive() {
        return live;
    }

    public String getRepositoryName() {
        return repository.getName();
    }

    public Model getModel() {
        return model;
    }

    public Node getRootNode() {
        checkThread();
        checkLive();
        return rootNode;
    }

    public Binary getBinary(InputStream in) throws StorageException {
        try {
            return repository.getBinaryManager().getBinary(in);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

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

    // also called by TransactionalSession#end
    protected void flush() throws StorageException {
        checkThread();
        if (!repository.getRepositoryDescriptor().fulltextDisabled) {
            updateFulltext();
        }
        log.debug("Saving session");
        RowBatch batch = context.getSaveBatch();
        if (!batch.isEmpty()) {
            // execute the batch
            mapper.write(batch);
        }
        log.debug("End of save");
        if (readAclsChanged) {
            updateReadAcls();
        }
        checkReceivedInvalidations();
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

            for (String indexName : model.getFulltextInfo().indexNames) {
                Set<String> paths;
                if (model.getFulltextInfo().indexesAllSimple.contains(indexName)) {
                    // index all string fields, minus excluded ones
                    // TODO XXX excluded ones...
                    paths = model.getTypeSimpleTextPropertyPaths(document.getPrimaryType());
                } else {
                    // index configured fields
                    paths = model.getFulltextInfo().propPathsByIndexSimple.get(indexName);
                }
                String strings = findFulltext(document, paths);
                // Set the computed full text
                // On INSERT/UPDATE a trigger will change the actual fulltext
                String propName = model.FULLTEXT_SIMPLETEXT_PROP
                        + model.getFulltextIndexSuffix(indexName);
                document.setSingleProperty(propName, strings);
            }
        }

        if (!dirtyBinaries.isEmpty()) {
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
    }

    protected String findFulltext(Node document, Set<String> paths)
            throws StorageException {
        if (paths == null) {
            return "";
        }

        String documentType = document.getPrimaryType();
        List<String> strings = new LinkedList<String>();

        for (String path : paths) {
            ModelProperty pi = model.getPathPropertyInfo(documentType, path);
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
     * Called post-transaction by commit/rollback or transactionless save.
     */
    protected void sendInvalidationsToOthers() throws StorageException {
        Invalidations invalidations = context.gatherInvalidations();
        if (!invalidations.isEmpty()) {
            repository.invalidate(invalidations, this);
        }
    }

    /**
     * Processes all invalidations accumulated.
     * <p>
     * Called pre-transaction by start or transactionless save;
     */
    protected void processReceivedInvalidations() throws StorageException {
        repository.receiveClusterInvalidations();
        context.processReceivedInvalidations();
    }

    /**
     * Post transaction check invalidations processing.
     */
    protected void checkReceivedInvalidations() throws StorageException {
        repository.receiveClusterInvalidations();
        context.checkReceivedInvalidations();
    }

    /**
     * Processes invalidations received by another session or cluster node.
     * <p>
     * Invalidations from other local session can happen asynchronously at any
     * time (when the other session commits). Invalidations from another cluster
     * node happen when the transaction starts.
     */
    protected void invalidate(Invalidations invalidations) {
        context.invalidate(invalidations);
    }

    /*
     * -------------------------------------------------------------
     * -------------------------------------------------------------
     * -------------------------------------------------------------
     */

    public Node getNodeById(Serializable id) throws StorageException {
        checkThread();
        checkLive();
        if (id == null) {
            throw new IllegalArgumentException("Illegal null id");
        }
        if (context.isDeleted(id)) {
            return null;
        }
        List<Node> nodes = getNodesByIds(Collections.singletonList(id));
        return nodes.get(0);
    }

    public List<Node> getNodesByIds(List<Serializable> ids)
            throws StorageException {
        checkThread();
        checkLive();

        // get main fragments
        // TODO ctx: order of fragments
        List<RowId> hierRowIds = new ArrayList<RowId>(ids.size());
        for (Serializable id : ids) {
            hierRowIds.add(new RowId(model.mainTableName, id));
        }

        List<Fragment> hierFragments = context.getMulti(hierRowIds, false);
        // the ids usually come from a query, in which case we don't need to
        // check if they are removed using isDeleted()

        // find what types we have and the associated rows to fetch
        List<RowId> rowIds = new ArrayList<RowId>();
        Map<Serializable, FragmentGroup> groups = new HashMap<Serializable, FragmentGroup>(
                ids.size());
        for (Fragment fragment : hierFragments) {
            Serializable id = fragment.row.id;
            // prepare fragment groups
            groups.put(id, new FragmentGroup((SimpleFragment) fragment,
                    new FragmentsMap()));

            // find type and table names
            SimpleFragment hierFragment = (SimpleFragment) fragment;
            String typeName = (String) hierFragment.get(model.MAIN_PRIMARY_TYPE_KEY);
            Serializable parentId = hierFragment.get(model.HIER_PARENT_KEY);
            Set<String> tableNames = model.getTypePrefetchedFragments(typeName);
            if (tableNames == null) {
                continue; // unknown (obsolete) type
            }
            // add row id for each table name
            for (String tableName : tableNames) {
                if (tableName.equals(model.mainTableName)) {
                    continue; // already fetched
                }
                if (parentId != null
                        && tableName.equals(model.VERSION_TABLE_NAME)) {
                    continue; // not a version, don't fetch this table
                }
                rowIds.add(new RowId(tableName, id));
            }
        }

        List<Fragment> fragments = context.getMulti(rowIds, true);

        // put fragment in map of proper group
        for (Fragment fragment : fragments) {
            // always in groups because id is of a requested row
            FragmentsMap fragmentsMap = groups.get(fragment.row.id).fragments;
            fragmentsMap.put(fragment.row.tableName, fragment);
        }

        // assemble nodes from the groups
        List<Node> nodes = new ArrayList<Node>(ids.size());
        for (Serializable id : ids) {
            FragmentGroup fragmentGroup = groups.get(id);
            Node node;
            if (fragmentGroup == null) {
                node = null; // deleted/absent
            } else {
                node = new Node(context, fragmentGroup);
            }
            nodes.add(node);
        }

        return nodes;
    }

    public Node getParentNode(Node node) throws StorageException {
        checkLive();
        if (node == null) {
            throw new IllegalArgumentException("Illegal null node");
        }
        Serializable id = node.getHierFragment().get(model.HIER_PARENT_KEY);
        return id == null ? null : getNodeById(id);
    }

    public String getPath(Node node) throws StorageException {
        checkLive();
        List<String> list = new LinkedList<String>();
        while (node != null) {
            list.add(node.getName());
            node = getParentNode(node);
        }
        if (list.size() == 1) {
            // root, special case
            return "/";
        }
        Collections.reverse(list);
        return StringUtils.join(list, "/");
    }

    /* Does not apply to properties for now (no use case). */
    public Node getNodeByPath(String path, Node node) throws StorageException {
        // TODO optimize this to use a dedicated path-based table
        checkLive();
        if (path == null) {
            throw new IllegalArgumentException("Illegal null path");
        }
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

    public Node addChildNode(Node parent, String name, Long pos,
            String typeName, boolean complexProp) throws StorageException {
        if (pos == null && !complexProp) {
            pos = context.getNextPos(parent.getId(), complexProp);
        }
        return addChildNode(null, parent, name, pos, typeName, complexProp);
    }

    public Node addChildNode(Serializable id, Node parent, String name,
            Long pos, String typeName, boolean complexProp)
            throws StorageException {
        checkLive();
        if (name == null || name.contains("/") || name.equals(".")
                || name.equals("..")) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        if (!model.isType(typeName)) {
            throw new IllegalArgumentException("Unknown type: " + typeName);
        }

        id = generateNewId(id);
        Serializable parentId = parent == null ? null
                : parent.hierFragment.getId();

        // main info
        Row hierRow = new Row(model.mainTableName, id);
        hierRow.putNew(model.MAIN_PRIMARY_TYPE_KEY, typeName);
        // TODO if folder is ordered, we have to compute the pos as max+1...
        hierRow.putNew(model.HIER_PARENT_KEY, parentId);
        hierRow.putNew(model.HIER_CHILD_POS_KEY, pos);
        hierRow.putNew(model.HIER_CHILD_NAME_KEY, name);
        hierRow.putNew(model.HIER_CHILD_ISPROPERTY_KEY,
                Boolean.valueOf(complexProp));

        SimpleFragment hierFragment = context.createSimpleFragment(hierRow);

        FragmentsMap fragments = new FragmentsMap();
        // TODO if non-lazy creation of some fragments, create them here
        // for (String tableName : model.getTypeSimpleFragments(typeName)) {

        FragmentGroup fragmentGroup = new FragmentGroup(hierFragment, fragments);

        requireReadAclsUpdate();

        return new Node(context, fragmentGroup);
    }

    public Node addProxy(Serializable targetId, Serializable versionableId,
            Node parent, String name, Long pos) throws StorageException {
        Node proxy = addChildNode(parent, name, pos, Model.PROXY_TYPE, false);
        proxy.setSingleProperty(model.PROXY_TARGET_PROP, targetId);
        proxy.setSingleProperty(model.PROXY_VERSIONABLE_PROP, versionableId);
        return proxy;
    }

    public boolean hasChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        checkLive();
        // TODO could optimize further by not fetching the fragment at all
        SimpleFragment fragment = context.getChildHierByName(parent.getId(),
                name, complexProp);
        return fragment != null;
    }

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
    public boolean hasChildren(Node parent, boolean complexProp)
            throws StorageException {
        checkLive();
        List<SimpleFragment> children = context.getChildren(parent.getId(),
                null, complexProp);
        return children.size() > 0;
    }

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

    public void orderBefore(Node parent, Node source, Node dest)
            throws StorageException {
        checkLive();
        context.orderBefore(parent.getId(), source.getId(), dest == null ? null
                : dest.getId());
    }

    public Node move(Node source, Node parent, String name)
            throws StorageException {
        checkLive();
        flush(); // needed when doing many moves for circular stuff
        context.move(source, parent.getId(), name);
        requireReadAclsUpdate();
        return source;
    }

    public Node copy(Node source, Node parent, String name)
            throws StorageException {
        checkLive();
        flush();
        Serializable id = context.copy(source, parent.getId(), name);
        requireReadAclsUpdate();
        return getNodeById(id);
    }

    public void removeNode(Node node) throws StorageException {
        checkLive();
        context.removeNode(node.getHierFragment());
    }

    public Node checkIn(Node node, String label, String description)
            throws StorageException {
        checkLive();
        flush();
        Serializable id = context.checkIn(node, label, description);
        requireReadAclsUpdate();
        // save to reflect changes immediately in database
        flush();
        return getNodeById(id);
    }

    public void checkOut(Node node) throws StorageException {
        checkLive();
        context.checkOut(node);
        requireReadAclsUpdate();
    }

    public void restoreByLabel(Node node, String label) throws StorageException {
        checkLive();
        // find the version
        Serializable versionId = mapper.getVersionIdByLabel(node.getId(), label);
        if (versionId == null) {
            throw new StorageException("Unknown version: " + label);
        }
        // save done inside method
        context.restoreVersion(node, versionId);
        requireReadAclsUpdate();
    }

    public Node getVersionByLabel(Serializable versionableId, String label)
            throws StorageException {
        checkLive();
        flush();
        Serializable id = mapper.getVersionIdByLabel(versionableId, label);
        return id == null ? null : getNodeById(id);
    }

    public Node getLastVersion(Node node) throws StorageException {
        checkLive();
        flush();
        Serializable id = mapper.getLastVersionId(node.getId());
        return id == null ? null : getNodeById(id);
    }

    public List<Node> getVersions(Node versionableNode) throws StorageException {
        checkLive();
        flush();
        List<Serializable> ids = context.getVersionIds(versionableNode.getId());
        List<Node> nodes = new ArrayList<Node>(ids.size());
        for (Serializable id : ids) {
            nodes.add(getNodeById(id));
        }
        return nodes;
    }

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

    public PartialList<Serializable> query(String query,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        return mapper.query(query, queryFilter, countTotal);
    }

    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws StorageException {
        return mapper.queryAndFetch(query, queryType, queryFilter, params);
    }

    public void requireReadAclsUpdate() {
        readAclsChanged = true;
    }

    public void updateReadAcls() throws StorageException {
        mapper.updateReadAcls();
        readAclsChanged = false;
    }

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
            rootNode = getNodeById(rootId);
        }
    }

    // TODO factor with addChildNode
    private Node addRootNode() throws StorageException {
        requireReadAclsUpdate();
        Serializable id = generateNewId(null);

        Row hierRow = new Row(model.mainTableName, id);
        hierRow.putNew(model.MAIN_PRIMARY_TYPE_KEY, model.ROOT_TYPE);
        hierRow.putNew(model.HIER_PARENT_KEY, null);
        hierRow.putNew(model.HIER_CHILD_POS_KEY, null);
        hierRow.putNew(model.HIER_CHILD_NAME_KEY, "");
        hierRow.putNew(model.HIER_CHILD_ISPROPERTY_KEY, Boolean.FALSE);

        SimpleFragment hierFragment = context.createSimpleFragment(hierRow);
        FragmentGroup fragmentGroup = new FragmentGroup(hierFragment,
                new FragmentsMap());
        return new Node(context, fragmentGroup);
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

    /*
     * ----- XAResource -----
     */

    public boolean isSameRM(XAResource xaresource) {
        return xaresource == this;
    }

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

    public int prepare(Xid xid) throws XAException {
        return mapper.prepare(xid);
    }

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

    public void rollback(Xid xid) throws XAException {
        try {
            try {
                mapper.rollback(xid);
            } finally {
                rollback();
            }
        } finally {
            inTransaction = false;
            try {
                try {
                    sendInvalidationsToOthers();
                } finally {
                    checkThreadEnd();
                }
            } catch (Exception e) {
                log.error("Could not rollback transaction", e);
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
            }
        }
    }

    public void forget(Xid xid) throws XAException {
        mapper.forget(xid);
    }

    public Xid[] recover(int flag) throws XAException {
        return mapper.recover(flag);
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return mapper.setTransactionTimeout(seconds);
    }

    public int getTransactionTimeout() throws XAException {
        return mapper.getTransactionTimeout();
    }

}
