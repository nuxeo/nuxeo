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
import java.util.HashMap;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class SessionImpl implements Session {

    private static final Log log = LogFactory.getLog(SessionImpl.class);

    private final Repository repository;

    protected final SchemaManager schemaManager;

    private final Model model;

    private final Credentials credentials;

    private final PersistenceContext context;

    private boolean live = true;

    private Node rootNode;

    SessionImpl(Repository repository, SchemaManager schemaManager,
            Mapper mapper, Credentials credentials) throws StorageException {
        this.repository = repository;
        this.schemaManager = schemaManager;
        this.credentials = credentials;
        model = mapper.getModel();
        context = new PersistenceContext(mapper);
        computeRootNode();
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    public void close() throws ResourceException {
        context.close();
        live = false;
    }

    public Interaction createInteraction() throws ResourceException {
        throw new RuntimeException("Not implemented");
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new RuntimeException("Not implemented");
    }

    public ConnectionMetaData getMetaData() throws ResourceException {
        throw new RuntimeException("Not implemented");
    }

    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new RuntimeException("Not implemented");
    }

    /*
     * ----- Session -----
     */

    public Model getModel() {
        return model;
    }

    public Node getRootNode() throws StorageException {
        checkLive();
        return rootNode;
    }

    public void save() throws StorageException {
        checkLive();
        context.save();
    }

    public Node addNode(Node parent, String name, String typeName)
            throws StorageException {
        checkLive();
        if (name == null || name.contains("/") || name.equals(".") ||
                name.equals("..")) {
            // XXX real parsing
            throw new StorageException("Illegal name: " + name);
        }
        // XXX do namespace transformations

        // use a temporary id that's guaranteed to not be in the db
        Serializable id = "T" + repository.getNextTemporaryId();

        // create the underlying main row
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(model.MAIN_PRIMARY_TYPE_KEY, typeName);
        SingleRow mainRow = (SingleRow) context.createSingleRow(
                model.MAIN_TABLE_NAME, id, map);

        // find all schemas for this type and create fragment entities
        FragmentsMap fragments = new FragmentsMap();
        // TODO typeName could be a document type or a complex type
        // XXX don't use schema, ask the model
        DocumentType type = schemaManager.getDocumentType(typeName);
        for (Schema schema : type.getSchemas()) {
            String schemaName = schema.getName();
            // TODO fill data instead of null XXX or just have fragments empty
            Fragment fragment = context.createSingleRow(schemaName, id, null);
            fragments.put(schemaName, fragment);
        }

        // add to hierarchy table
        // TODO if folder is ordered, we have to compute the pos as max+1...
        map = new HashMap<String, Serializable>();
        map.put(model.HIER_PARENT_KEY, parent.mainFragment.getId());
        map.put(model.HIER_CHILD_POS_KEY, null);
        map.put(model.HIER_CHILD_NAME_KEY, name);
        SingleRow hierRow = (SingleRow) context.createSingleRow(
                model.HIER_TABLE_NAME, id, map);
        // TODO put in in a collection context instead

        FragmentGroup rowGroup = new FragmentGroup(mainRow, hierRow, fragments);

        return new Node(type, this, context, rowGroup);
    }

    public Node getNode(Node parent, String name) throws StorageException {
        checkLive();
        if (name == null || name.contains("/") || name.equals(".") ||
                name.equals("..")) {
            // XXX real parsing
            throw new StorageException("Illegal name: " + name);
        }

        // XXX namespace transformations

        // find child hier row
        Serializable parentId = parent.getId();
        SingleRow childHier = context.getByHier(parentId, name);
        if (childHier == null) {
            // not found
            return null;
        }
        Serializable childId = childHier.getId();

        // get main row
        SingleRow childMain = (SingleRow) context.get(model.MAIN_TABLE_NAME,
                childId);
        String childTypeName = (String) childMain.get(model.MAIN_PRIMARY_TYPE_KEY);
        DocumentType childType = schemaManager.getDocumentType(childTypeName);

        // TODO get all non-cached fragments at once using join / union
        FragmentsMap childFragments = new FragmentsMap();
        for (Schema schema : childType.getSchemas()) {
            String schemaName = schema.getName();
            Fragment fragment = context.get(schemaName, childId);
            childFragments.put(schemaName, fragment);
        }

        FragmentGroup childGroup = new FragmentGroup(childMain, childHier,
                childFragments);

        return new Node(childType, this, context, childGroup);
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
        return credentials.getUserID();
    }

    public boolean isLive() {
        return live;
    }

    private void checkLive() throws StorageException {
        if (!live) {
            throw new StorageException("Session is not live");
        }
    }

    private void computeRootNode() throws StorageException {
        SingleRow repoInfo = (SingleRow) context.get(model.REPOINFO_TABLE_NAME,
                Long.valueOf(0));
        if (repoInfo == null) {
            log.debug("Creating root");
            rootNode = addRootNode();
            save();

            // record information about the root id
            Map<String, Serializable> map = new HashMap<String, Serializable>();
            map.put(model.REPOINFO_ROOTID_KEY, rootNode.getId());
            repoInfo = (SingleRow) context.createSingleRow(
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
        SingleRow mainRow = (SingleRow) context.createSingleRow(
                model.MAIN_TABLE_NAME, id, map);

        // add to hierarchy table
        map = new HashMap<String, Serializable>();
        map.put(model.HIER_PARENT_KEY, null);
        map.put(model.HIER_CHILD_POS_KEY, null);
        map.put(model.HIER_CHILD_NAME_KEY, "");
        SingleRow hierRow = (SingleRow) context.createSingleRow(
                model.HIER_TABLE_NAME, id, map);

        DocumentType type = schemaManager.getDocumentType(model.ROOT_TYPE);
        FragmentGroup rowGroup = new FragmentGroup(mainRow, hierRow, null);

        return new Node(type, this, context, rowGroup);
    }

    // public Node newNodeInstance() needed ?

    public boolean hasNode(Node parent, String name) throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

    /**
     * Gets a node given its id.
     *
     * @param id
     * @return
     * @throws StorageException
     */
    public Node getNodeById(Serializable id) throws StorageException {
        if (id == null) {
            throw new StorageException("Illegal id: " + id);
        }

        // get main row
        SingleRow childMain = (SingleRow) context.get(model.MAIN_TABLE_NAME, id);
        if (childMain == null) {
            // not found
            return null;
        }
        String childTypeName = (String) childMain.get(model.MAIN_PRIMARY_TYPE_KEY);
        DocumentType childType = schemaManager.getDocumentType(childTypeName);

        // find hier row
        SingleRow childHier = (SingleRow) context.get(model.HIER_TABLE_NAME, id);

        // TODO get all non-cached fragments at once using join / union
        FragmentsMap childFragments = new FragmentsMap();
        for (Schema schema : childType.getSchemas()) {
            String schemaName = schema.getName();
            Fragment fragment = context.get(schemaName, id);
            childFragments.put(schemaName, fragment);
        }

        FragmentGroup childGroup = new FragmentGroup(childMain, childHier,
                childFragments);

        return new Node(childType, this, context, childGroup);
    }

    // ----------

    // ----------

    // ----------

    public Node getNode(String path) throws StorageException {
        checkLive();
        if (path == null || !path.startsWith("/")) {
            throw new RuntimeException("Illegal path: " + path);
        }
        Node node = getRootNode();
        path = path.substring(1);
        while (path.length() > 0) {

        }
        return node;
    }

    public boolean hasNode(String absPath) throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

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

    public String copy(String srcAbsPath, String destAbsPath)
            throws StorageException {
        checkLive();
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }

}
