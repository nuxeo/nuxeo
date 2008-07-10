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

import java.io.Serializable;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * The session is the main high level access point to data from the underlying
 * database.
 *
 * @author Florent Guillaume
 */
public class SessionImpl implements Session, XAResource {

    private static final Log log = LogFactory.getLog(SessionImpl.class);

    private final Repository repository;

    protected final SchemaManager schemaManager;

    private final Mapper mapper;

    private final Credentials credentials;

    private final Model model;

    private final PersistenceContext context;

    private boolean live = true;

    private Node rootNode;

    SessionImpl(Repository repository, SchemaManager schemaManager,
            Mapper mapper, Credentials credentials) throws StorageException {
        this.repository = repository;
        this.schemaManager = schemaManager;
        this.mapper = mapper;
        this.credentials = credentials;
        model = mapper.getModel();
        context = new PersistenceContext(mapper);
        computeRootNode();
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    public void close() throws ResourceException {
        // this closes the mapper and therefore the connection
        context.close();
        live = false;
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
     * ----- javax.transaction.xa.XAResource -----
     */

    public boolean isSameRM(XAResource xaresource) {
        return xaresource == this;
    }

    public void start(Xid xid, int flags) throws XAException {
        mapper.start(xid, flags);
    }

    public int prepare(Xid xid) throws XAException {
        return mapper.prepare(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        mapper.commit(xid, onePhase);
    }

    public void end(Xid xid, int flags) throws XAException {
        mapper.end(xid, flags);
    }

    public void rollback(Xid xid) throws XAException {
        mapper.rollback(xid);
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

    /*
     * ----- Session -----
     */

    public Model getModel() {
        return model;
    }

    public Node getRootNode() {
        checkLive();
        return rootNode;
    }

    public void save() throws StorageException {
        checkLive();
        context.save();
    }

    public Node getNodeById(Serializable id) throws StorageException {
        checkLive();
        if (id == null) {
            throw new IllegalArgumentException("Illegal null id");
        }

        // get main row
        SimpleFragment childMain = (SimpleFragment) context.get(
                model.MAIN_TABLE_NAME, id, false);
        if (childMain == null) {
            // HACK try old id
            id = context.getOldId(id);
            if (id == null) {
                return null;
            }
            childMain = (SimpleFragment) context.get(model.MAIN_TABLE_NAME, id,
                    false);
            if (childMain == null) {
                return null;
            }
        }
        String childTypeName = (String) childMain.get(model.MAIN_PRIMARY_TYPE_KEY);
        DocumentType childType = schemaManager.getDocumentType(childTypeName);

        // find hier row
        SimpleFragment childHier = (SimpleFragment) context.get(
                model.HIER_TABLE_NAME, id, false);

        // TODO get all non-cached fragments at once using join / union
        FragmentsMap childFragments = new FragmentsMap();
        for (Schema schema : childType.getSchemas()) {
            String schemaName = schema.getName();
            Fragment fragment = context.get(schemaName, id, true);
            childFragments.put(schemaName, fragment);
        }

        FragmentGroup childGroup = new FragmentGroup(childMain, childHier,
                childFragments);

        return new Node(childType, this, context, childGroup);
    }

    public Node getParentNode(Node node) throws StorageException {
        checkLive();
        if (node == null) {
            throw new IllegalArgumentException("Illegal null node");
        }
        Serializable id = node.hierFragment.get(model.HIER_PARENT_KEY);
        if (id == null) {
            // root
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
            node = getChildNode(node, name, Boolean.FALSE);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    public Node addChildNode(Node parent, String name, String typeName,
            boolean complexProp) throws StorageException {
        checkLive();
        if (name == null || name.contains("/") || name.equals(".") ||
                name.equals("..")) {
            // XXX real parsing
            throw new IllegalArgumentException("Illegal name: " + name);
        }
        // XXX do namespace transformations

        // use a temporary id that's guaranteed to not be in the db
        Serializable id = "T" + repository.getNextTemporaryId();

        // create the underlying main row
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(model.MAIN_PRIMARY_TYPE_KEY, typeName);
        SimpleFragment mainRow = (SimpleFragment) context.createSimpleFragment(
                model.MAIN_TABLE_NAME, id, map);

        // find all schemas for this type and create fragment entities
        FragmentsMap fragments = new FragmentsMap();
        DocumentType type = schemaManager.getDocumentType(typeName);
        if (false) {
            // TODO typeName could be a document type or a complex type
            // XXX don't use schema, ask the model
            for (Schema schema : type.getSchemas()) {
                String schemaName = schema.getName();
                // TODO XXX fill in default values
                // TODO fill data instead of null XXX or just have fragments
                // empty
                Fragment fragment = context.createSimpleFragment(schemaName,
                        id, null);
                fragments.put(schemaName, fragment);
            }
        }

        // add to hierarchy table
        // TODO if folder is ordered, we have to compute the pos as max+1...
        map = new HashMap<String, Serializable>();
        map.put(model.HIER_PARENT_KEY, parent.mainFragment.getId());
        map.put(model.HIER_CHILD_NAME_KEY, name);
        map.put(model.HIER_CHILD_POS_KEY, null);
        map.put(model.HIER_CHILD_ISPROPERTY_KEY, Boolean.valueOf(complexProp));
        SimpleFragment hierRow = (SimpleFragment) context.createSimpleFragment(
                model.HIER_TABLE_NAME, id, map);
        // TODO put it in a collection context instead

        FragmentGroup rowGroup = new FragmentGroup(mainRow, hierRow, fragments);

        return new Node(type, this, context, rowGroup);
    }

    public boolean hasChildNode(Node parent, String name, Boolean complexProp)
            throws StorageException {
        checkLive();
        // TODO could optimize further by not fetching the fragment at all
        return context.getByHier(parent.getId(), name, complexProp) != null;
    }

    public Node getChildNode(Node parent, String name, Boolean complexProp)
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
        SimpleFragment childHier = context.getByHier(parentId, name, complexProp);
        if (childHier == null) {
            // not found
            return null;
        }
        Serializable childId = childHier.getId();

        // get main row
        SimpleFragment childMain = (SimpleFragment) context.get(
                model.MAIN_TABLE_NAME, childId, false);
        String childTypeName = (String) childMain.get(model.MAIN_PRIMARY_TYPE_KEY);
        DocumentType childType = schemaManager.getDocumentType(childTypeName);

        // TODO get all non-cached fragments at once using join / union
        FragmentsMap childFragments = new FragmentsMap();
        for (Schema schema : childType.getSchemas()) {
            String schemaName = schema.getName();
            Fragment fragment = context.get(schemaName, childId, true);
            childFragments.put(schemaName, fragment);
        }

        FragmentGroup childGroup = new FragmentGroup(childMain, childHier,
                childFragments);

        return new Node(childType, this, context, childGroup);
    }

    // TODO optimize with dedicated backend call
    public boolean hasChildren(Node parent, Boolean complex)
            throws StorageException {
        checkLive();
        return context.getHierChildren(parent.getId(), complex).size() > 0;
    }

    public List<Node> getChildren(Node parent, Boolean complex)
            throws StorageException {
        checkLive();
        Collection<SimpleFragment> fragments = context.getHierChildren(
                parent.getId(), complex);
        List<Node> nodes = new ArrayList<Node>(fragments.size());
        for (SimpleFragment fragment : fragments) {
            Serializable id = fragment.getId();
            nodes.add(getNodeById(id));
        }
        return nodes;
    }

    // TODO XXX remove recursively the children
    public void removeNode(Node node) throws StorageException {
        checkLive();
        node.remove();
    }

    /*
     * ----- -----
     */

    public String getUserID() {
        return credentials.getUserName();
    }

    public boolean isLive() {
        return live;
    }

    private void checkLive() throws IllegalStateException {
        if (!live) {
            throw new IllegalStateException("Session is not live");
        }
    }

    private void computeRootNode() throws StorageException {
        SimpleFragment repoInfo = (SimpleFragment) context.get(
                model.REPOINFO_TABLE_NAME, Long.valueOf(0), false);
        if (repoInfo == null) {
            log.debug("Creating root");
            rootNode = addRootNode();
            save();

            // record information about the root id
            Map<String, Serializable> map = new HashMap<String, Serializable>();
            map.put(model.REPOINFO_ROOTID_KEY, rootNode.getId());
            repoInfo = (SimpleFragment) context.createSimpleFragment(
                    model.REPOINFO_TABLE_NAME, Long.valueOf(0L), map);
            save();
        } else {
            Serializable rootId = repoInfo.get(model.REPOINFO_ROOTID_KEY);
            rootNode = getNodeById(rootId);
        }
    }

    private Node addRootNode() throws StorageException {
        // use a temporary id that's guaranteed to not be in the db
        Serializable id = "T" + repository.getNextTemporaryId();

        // create the underlying main row
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(model.MAIN_PRIMARY_TYPE_KEY, model.ROOT_TYPE);
        SimpleFragment mainRow = (SimpleFragment) context.createSimpleFragment(
                model.MAIN_TABLE_NAME, id, map);

        // add to hierarchy table
        map = new HashMap<String, Serializable>();
        map.put(model.HIER_PARENT_KEY, null);
        map.put(model.HIER_CHILD_POS_KEY, null);
        map.put(model.HIER_CHILD_NAME_KEY, "");
        SimpleFragment hierRow = (SimpleFragment) context.createSimpleFragment(
                model.HIER_TABLE_NAME, id, map);

        DocumentType type = schemaManager.getDocumentType(model.ROOT_TYPE);
        FragmentGroup rowGroup = new FragmentGroup(mainRow, hierRow, null);

        return new Node(type, this, context, rowGroup);
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

    public String move(String srcAbsPath, String destAbsPath)
            throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    public String copy(Node sourceNode, String parentNode)
            throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

}
