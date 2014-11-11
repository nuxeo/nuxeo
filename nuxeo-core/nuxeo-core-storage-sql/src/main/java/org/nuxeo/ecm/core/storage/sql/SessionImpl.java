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
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.security.AccessControlException;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * The session is the main high level access point to data from the underlying
 * database.
 *
 * @author Florent Guillaume
 */
public class SessionImpl implements Session {

    private static final Log log = LogFactory.getLog(SessionImpl.class);

    private final RepositoryImpl repository;

    protected final SchemaManager schemaManager;

    private final Mapper mapper;

    private final Model model;

    private final PersistenceContext context;

    private boolean live;

    private final TransactionalSession transactionalSession;

    private Node rootNode;

    SessionImpl(RepositoryImpl repository, SchemaManager schemaManager,
            Mapper mapper, RepositoryImpl.Invalidators invalidators,
            Credentials credentials) throws StorageException {
        this.repository = repository;
        this.schemaManager = schemaManager;
        this.mapper = mapper;
        // this.credentials = credentials;
        model = mapper.getModel();
        context = new PersistenceContext(mapper, invalidators);
        live = true;
        transactionalSession = new TransactionalSession(mapper, context);
        computeRootNode();
    }

    /**
     * Gets the XAResource. Called by the ManagedConnectionImpl, which actually
     * wraps it in a connection-aware implementation.
     */
    public XAResource getXAResource() {
        return transactionalSession;
    }

    /**
     * Clears all the caches. Called by RepositoryManagement.
     */
    protected int clearCaches() {
        if (transactionalSession.isInTransaction()) {
            // avoid potential multi-threaded access to active session
            return 0;
        }
        return context.clearCaches();
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    public void close() {
        closeSession();
        repository.closeSession(this);
    }

    protected void closeSession() {
        live = false;
        // this closes the mapper and therefore the connection
        context.close();
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

    public Model getModel() {
        return model;
    }

    public Node getRootNode() {
        checkLive();
        return rootNode;
    }

    public Binary getBinary(InputStream in) throws StorageException {
        try {
            return repository.getBinary(in);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public void save() throws StorageException {
        checkLive();
        context.save();
        if (!transactionalSession.isInTransaction()) {
            context.notifyInvalidations();
            // as we don't have a way to know when the next non-transactional
            // statement will start, process invalidations immediately
            context.processInvalidations();
        }
    }

    public Node getNodeById(Serializable id) throws StorageException {
        checkLive();
        if (id == null) {
            throw new IllegalArgumentException("Illegal null id");
        }

        // get main row
        SimpleFragment childMain = (SimpleFragment) context.get(
                model.mainTableName, id, false);
        if (childMain == null) {
            // HACK try old id
            id = context.getOldId(id);
            if (id == null) {
                return null;
            }
            childMain = (SimpleFragment) context.get(model.mainTableName, id,
                    false);
            if (childMain == null) {
                return null;
            }
        }
        String childTypeName = (String) childMain.get(model.MAIN_PRIMARY_TYPE_KEY);

        // find hier row if separate
        SimpleFragment childHier;
        Serializable parentId;
        String name;
        if (model.separateMainTable) {
            childHier = (SimpleFragment) context.get(model.hierTableName, id,
                    false);
            parentId = childHier.get(model.HIER_PARENT_KEY);
            name = childHier.getString(model.HIER_CHILD_NAME_KEY);
        } else {
            childHier = null;
            parentId = childMain.get(model.HIER_PARENT_KEY);
            name = childMain.getString(model.HIER_CHILD_NAME_KEY);
        }

        FragmentsMap childFragments = getFragments(id, childTypeName, parentId,
                name);

        FragmentGroup childGroup = new FragmentGroup(childMain, childHier,
                childFragments);

        return new Node(this, context, childGroup);
    }

    protected FragmentsMap getFragments(Serializable id, String typeName,
            Serializable parentId, String name) throws StorageException {
        // TODO get all non-cached fragments at once using join / union
        FragmentsMap fragments = new FragmentsMap();
        for (String fragmentName : model.getTypeSimpleFragments(typeName)) {
            Fragment fragment = context.get(fragmentName, id, true);
            fragments.put(fragmentName, fragment);
        }
        // check version too
        if (parentId == null && name != null && name.length() > 0) {
            // this is a version, fetch the version fragment too
            String fragmentName = model.VERSION_TABLE_NAME;
            Fragment fragment = context.get(fragmentName, id, true);
            fragments.put(fragmentName, fragment);
        }
        return fragments;
    }

    public Node getParentNode(Node node) throws StorageException {
        checkLive();
        if (node == null) {
            throw new IllegalArgumentException("Illegal null node");
        }
        Serializable id = node.getHierFragment().get(model.HIER_PARENT_KEY);
        if (id == null) {
            // root or version
            return null;
        }
        return getNodeById(id);
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
        checkLive();
        if (name == null || name.contains("/") || name.equals(".") ||
                name.equals("..")) {
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        if (!model.isType(typeName)) {
            throw new IllegalArgumentException("Unknown type: " + typeName);
        }

        Serializable id = context.generateNewId();

        // main info
        Map<String, Serializable> mainMap = new HashMap<String, Serializable>();
        mainMap.put(model.MAIN_PRIMARY_TYPE_KEY, typeName);

        // hierarchy info
        // TODO if folder is ordered, we have to compute the pos as max+1...
        Map<String, Serializable> hierMap;
        if (model.separateMainTable) {
            hierMap = new HashMap<String, Serializable>();
        } else {
            hierMap = mainMap;
        }
        hierMap.put(model.HIER_PARENT_KEY, parent.mainFragment.getId());
        hierMap.put(model.HIER_CHILD_POS_KEY, pos);
        hierMap.put(model.HIER_CHILD_NAME_KEY, name);
        hierMap.put(model.HIER_CHILD_ISPROPERTY_KEY,
                Boolean.valueOf(complexProp));

        SimpleFragment mainRow = context.createSimpleFragment(
                model.mainTableName, id, mainMap);

        SimpleFragment hierRow;
        if (model.separateMainTable) {
            // TODO put it in a collection context instead
            hierRow = context.createSimpleFragment(
                    model.hierTableName, id, hierMap);
        } else {
            hierRow = null;
        }

        FragmentsMap fragments = new FragmentsMap();
        if (false) {
            // TODO if non-lazy creation of some fragments, create them here
            for (String schemaName : model.getTypeSimpleFragments(typeName)) {
                // TODO XXX fill in default values
                // TODO fill data instead of null XXX or just have fragments
                // empty
                Fragment fragment = context.createSimpleFragment(schemaName,
                        id, null);
                fragments.put(schemaName, fragment);
            }
        }

        FragmentGroup rowGroup = new FragmentGroup(mainRow, hierRow, fragments);

        return new Node(this, context, rowGroup);
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
        return context.getChildByName(parent.getId(), name, complexProp) != null;
    }

    public Node getChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        checkLive();
        if (name == null || name.contains("/") || name.equals(".") ||
                name.equals("..")) {
            // XXX real parsing
            throw new IllegalArgumentException("Illegal name: " + name);
        }

        // XXX namespace transformations

        // find child hier row
        Serializable parentId = parent.getId();
        SimpleFragment childHier = context.getChildByName(parentId, name,
                complexProp);
        if (childHier == null) {
            // not found
            return null;
        }
        Serializable childId = childHier.getId();

        // get main row
        SimpleFragment childMain;
        if (model.separateMainTable) {
            childMain = (SimpleFragment) context.get(model.mainTableName,
                    childId, false);
        } else {
            childMain = childHier;
            childHier = null;
        }
        String childTypeName = (String) childMain.get(model.MAIN_PRIMARY_TYPE_KEY);

        FragmentsMap childFragments = getFragments(childId, childTypeName,
                parentId, name);

        FragmentGroup childGroup = new FragmentGroup(childMain, childHier,
                childFragments);

        return new Node(this, context, childGroup);
    }

    // TODO optimize with dedicated backend call
    public boolean hasChildren(Node parent, boolean complexProp)
            throws StorageException {
        checkLive();
        return context.getChildren(parent.getId(), null, complexProp).size() > 0;
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

    public Node move(Node source, Node parent, String name)
            throws StorageException {
        checkLive();
        context.save();
        context.move(source, parent.getId(), name);
        return source;
    }

    public Node copy(Node source, Node parent, String name)
            throws StorageException {
        checkLive();
        context.save();
        Serializable id = context.copy(source, parent.getId(), name);
        return getNodeById(id);
    }

    public void removeNode(Node node) throws StorageException {
        checkLive();
        node.remove();
        // TODO XXX remove recursively the children
    }

    public Node checkIn(Node node, String label, String description)
            throws StorageException {
        checkLive();
        context.save();
        Serializable id = context.checkIn(node, label, description);
        return getNodeById(id);
    }

    public void checkOut(Node node) throws StorageException {
        checkLive();
        context.checkOut(node);
    }

    public void restoreByLabel(Node node, String label) throws StorageException {
        checkLive();
        // save done inside method
        context.restoreByLabel(node, label);
    }

    public Node getVersionByLabel(Node node, String label)
            throws StorageException {
        checkLive();
        Serializable id = context.getVersionByLabel(node.getId(), label);
        if (id == null) {
            return null;
        }
        return getNodeById(id);
    }

    public Node getLastVersion(Node node) throws StorageException {
        checkLive();
        context.save();
        Serializable id = context.getLastVersion(node.getId());
        if (id == null) {
            return null;
        }
        return getNodeById(id);
    }

    public List<Node> getVersions(Node versionableNode) throws StorageException {
        checkLive();
        context.save();
        List<SimpleFragment> fragments = context.getVersions(versionableNode.getId());
        List<Node> nodes = new ArrayList<Node>(fragments.size());
        for (SimpleFragment fragment : fragments) {
            nodes.add(getNodeById(fragment.getId()));
        }
        return nodes;
    }

    public List<Node> getProxies(Node document, Node parent)
            throws StorageException {
        checkLive();
        context.save();
        List<SimpleFragment> fragments = context.getProxies(document, parent);
        List<Node> nodes = new ArrayList<Node>(fragments.size());
        for (SimpleFragment fragment : fragments) {
            nodes.add(getNodeById(fragment.getId()));
        }
        return nodes;
    }

    public List<Serializable> query(SQLQuery query, QueryFilter queryFilter)
            throws StorageException {
        try {
            return mapper.query(query, queryFilter, this);
        } catch (SQLException e) {
            throw new StorageException("Invalid query: " + query, e);
        }
    }

    // returns context or null if missing
    protected Context getContext(String tableName) {
        return context.getContextOrNull(tableName);
    }

    private void checkLive() {
        if (!live) {
            throw new IllegalStateException("Session is not live");
        }
    }

    private void computeRootNode() throws StorageException {
        String repositoryId = "default"; // TODO use repo name
        Serializable rootId = context.getRootId(repositoryId);
        if (rootId == null) {
            log.debug("Creating root");
            rootNode = addRootNode();
            addRootACP();
            save();
            // record information about the root id
            context.setRootId(repositoryId, rootNode.getId());
        } else {
            rootNode = getNodeById(rootId);
        }
    }

    // TODO factor with addChildNode
    private Node addRootNode() throws StorageException {
        Serializable id = context.generateNewId();

        // main info
        Map<String, Serializable> mainMap = new HashMap<String, Serializable>();
        mainMap.put(model.MAIN_PRIMARY_TYPE_KEY, model.ROOT_TYPE);

        // hierarchy info
        Map<String, Serializable> hierMap;
        if (model.separateMainTable) {
            hierMap = new HashMap<String, Serializable>();
        } else {
            hierMap = mainMap;
        }
        hierMap.put(model.HIER_PARENT_KEY, null);
        hierMap.put(model.HIER_CHILD_POS_KEY, null);
        hierMap.put(model.HIER_CHILD_NAME_KEY, "");
        hierMap.put(model.HIER_CHILD_ISPROPERTY_KEY, Boolean.FALSE);

        SimpleFragment mainRow = context.createSimpleFragment(
                model.mainTableName, id, mainMap);

        SimpleFragment hierRow;
        if (model.separateMainTable) {
            hierRow = context.createSimpleFragment(
                    model.hierTableName, id, hierMap);
        } else {
            hierRow = null;
        }

        FragmentGroup rowGroup = new FragmentGroup(mainRow, hierRow, null);

        return new Node(this, context, rowGroup);
    }

    private void addRootACP() throws StorageException {
        ACLRow[] aclrows = new ACLRow[4];
        // TODO put groups in their proper place. like that now for consistency.
        aclrows[0] = new ACLRow(0, ACL.LOCAL_ACL, true,
                SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATORS,
                null);
        aclrows[1] = new ACLRow(1, ACL.LOCAL_ACL, true,
                SecurityConstants.EVERYTHING, SecurityConstants.ADMINISTRATOR,
                null);
        aclrows[2] = new ACLRow(2, ACL.LOCAL_ACL, true, SecurityConstants.READ,
                SecurityConstants.MEMBERS, null);
        aclrows[3] = new ACLRow(3, ACL.LOCAL_ACL, true,
                SecurityConstants.VERSION, SecurityConstants.MEMBERS, null);
        rootNode.setCollectionProperty(Model.ACL_PROP, aclrows);
    }

    // public Node newNodeInstance() needed ?

    public void checkPermission(String absPath, String actions)
            throws AccessControlException, StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public boolean hasPendingChanges() throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

}
