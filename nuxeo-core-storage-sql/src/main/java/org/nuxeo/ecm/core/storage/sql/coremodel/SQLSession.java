/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.CollectionProperty;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.SimpleProperty;

/**
 * This class is the bridge between the Nuxeo SPI Session and the actual
 * low-level implementation of the SQL storage session.
 *
 * @author Florent Guillaume
 */
public class SQLSession implements Session {

    private static final Log log = LogFactory.getLog(SQLSession.class);

    private final Repository repository;

    private final Map<String, Serializable> context;

    private final org.nuxeo.ecm.core.storage.sql.Session session;

    private SQLDocument root;

    private String userSessionId;

    public SQLSession(org.nuxeo.ecm.core.storage.sql.Session session,
            Repository repository, Map<String, Serializable> context)
            throws DocumentException {
        this.session = session;
        this.repository = repository;
        if (context == null) {
            context = new HashMap<String, Serializable>();
        }
        this.context = context;
        context.put("creationTime", Long.valueOf(System.currentTimeMillis()));

        try {
            root = newDocument(session.getRootNode());
        } catch (StorageException e) {
            throw new DocumentException(e);
        }

        userSessionId = (String) context.get("SESSION_ID");
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Session -----
     */

    public Document getRootDocument() {
        return root;
    }

    // not called
    public XAResource getXAResource() {
        throw new RuntimeException();
    }

    public void close() throws DocumentException {
        try {
            session.save();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        dispose();
    }

    public void save() throws DocumentException {
        try {
            session.save();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    public void cancel() throws DocumentException {
        try {
            // TODO session.refresh or revert;
            throw new StorageException("unimplemented");
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    public boolean isLive() {
        return session != null && session.isLive();
    }

    public void dispose() {
        try {
            session.close();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
        root = null;
    }

    // not used?
    public long getSessionId() {
        throw new RuntimeException();
        // return sid;
    }

    public String getUserSessionId() {
        return userSessionId;
    }

    public Repository getRepository() {
        return repository;
    }

    public Map<String, Serializable> getSessionContext() {
        return context;
    }

    public SchemaManager getTypeManager() {
        return repository.getTypeManager();
    }

    public SecurityManager getSecurityManager() {
        return repository.getSecurityManager();
    }

    public Document getDocumentByUUID(String uuid) throws DocumentException {
        try {
            /**
             * Document ids coming from higher level have been turned into
             * strings (by {@link SQLDocument#getUUID}) but are really Longs for
             * the backend.
             */
            Serializable id = session.getModel().unHackStringId(uuid);
            Node node = session.getNodeById(id);
            if (node == null) {
                // required by callers such as AbstractSession.exists
                throw new NoSuchDocumentException(uuid);
            }
            return newDocument(node);
        } catch (StorageException e) {
            throw new DocumentException("Failed to get document by UUID", e);
        }
    }

    public Document resolvePath(String path) throws DocumentException {
        Document doc;
        try {
            doc = newDocument(session.getNodeByPath(path, session.getRootNode()));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        if (doc == null) {
            throw new NoSuchDocumentException("No such document: " + path);
        }
        return doc;
    }

    public Document copy(Document source, Document parent, String name)
            throws DocumentException {
        assert source instanceof SQLDocument;
        assert parent instanceof SQLDocument;
        // XXX TODO
        throw new UnsupportedOperationException();
        // Versioning.getService().fixupAfterCopy((SQLDocument) child);
    }

    public Document move(Document source, Document parent, String name)
            throws DocumentException {
        assert source instanceof SQLDocument;
        assert parent instanceof SQLDocument;
        // XXX TODO
        throw new UnsupportedOperationException();
    }

    public Document createProxyForVersion(Document parent, Document document,
            String versionLabel) throws DocumentException {
        // Document frozenDoc = document.getVersion(versionLabel);
        // String name = document.getName() + '_' + System.currentTimeMillis();
        // // TODO
        // Node node = pnode.addNode(ModelAdapter.getChildPath(name),
        // NodeConstants.ECM_NT_DOCUMENT_PROXY.rawname);
        // node.setProperty(NodeConstants.ECM_REF_FROZEN_NODE.rawname,
        // ((SQLDocument) frozenDoc).getNode());
        // node.setProperty(NodeConstants.ECM_REF_UUID.rawname,
        // document.getUUID());
        // return new NuxeoSQLDocumentProxy(this, node);
        // XXX TODO
        throw new UnsupportedOperationException();
    }

    public Collection<Document> getProxies(Document document, Document parent)
            throws DocumentException {
        log.error("getProxies unimplemented, returning empty list");
        return Collections.emptyList();
        // XXX TODO
        // throw new UnsupportedOperationException();
    }

    public InputStream getDataStream(String key) throws DocumentException {
        // XXX TODO
        throw new UnsupportedOperationException();
    }

    public Query createQuery(String query, Query.Type qType, String... params)
            throws QueryException {
        // XXX TODO
        throw new UnsupportedOperationException();
    }

    /*
     * ----- called by SQLDocument -----
     */

    private SQLDocument newDocument(Node node) throws StorageException {
        if (node == null) {
            // root's parent
            return null;
        }

        // TODO proxies / versions

        return new SQLDocument(node, this);
    }

    /**
     * Resolves a node given its absolute path, or given an existing node and a
     * relative path.
     */
    protected Document resolvePath(Node node, String path)
            throws DocumentException {
        try {
            return newDocument(session.getNodeByPath(path, node));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Document getParent(Node node) throws DocumentException {
        try {
            return newDocument(session.getParentNode(node));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected String getPath(Node node) throws DocumentException {
        try {
            return session.getPath(node);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Document getChild(Node node, String name)
            throws DocumentException {
        Document doc;
        try {
            doc = newDocument(session.getChildNode(node, name, false));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        if (doc == null) {
            throw new NoSuchDocumentException("No such document: " + name);
        }
        return doc;
    }

    // XXX change to iterator?
    protected List<Document> getChildren(Node node) throws DocumentException {
        List<Node> nodes;
        try {
            nodes = session.getChildren(node, false, null);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        List<Document> children = new ArrayList<Document>(nodes.size());
        for (Node n : nodes) {
            Document doc;
            try {
                doc = newDocument(n);
            } catch (StorageException e) {
                throw new DocumentException(e);
            }
            children.add(doc);
        }
        return children;
    }

    protected boolean hasChild(Node node, String name) throws DocumentException {
        try {
            return session.hasChildNode(node, name, false);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected boolean hasChildren(Node node) throws DocumentException {
        try {
            return session.hasChildren(node, false);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Document addChild(Node parent, String name, String typeName)
            throws DocumentException {
        try {
            return newDocument(session.addChildNode(parent, name, typeName,
                    false));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected List<Node> getComplexList(Node node, String name)
            throws DocumentException {
        List<Node> nodes;
        try {
            nodes = session.getChildren(node, true, name);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        return nodes;
    }

    protected void remove(Node node) throws DocumentException {
        try {
            session.removeNode(node);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    /*
     * ----- property helpers -----
     */

    /**
     * Create a property. Node may be null if the parent property hasn't be
     * created yet.
     */
    protected Property makeProperty(Node node, ComplexType complexType,
            String name) throws DocumentException {

        Field field;
        if (Model.SYSTEM_LIFECYCLE_POLICY_PROP.equals(name)) {
            field = Model.SYSTEM_LIFECYCLE_POLICY_FIELD;
        } else if (Model.SYSTEM_LIFECYCLE_STATE_PROP.equals(name)) {
            field = Model.SYSTEM_LIFECYCLE_STATE_FIELD;
        } else if (Model.SYSTEM_DIRTY_PROP.equals(name)) {
            field = Model.SYSTEM_DIRTY_FIELD;
        } else {
            field = complexType.getField(name);
            // qualify if necessary (some callers pass unprefixed names)
            name = field.getName().getPrefixedName();
        }

        if (field == null) {
            throw new NoSuchPropertyException(name);
        }

        Type type = field.getType();
        if (type.isSimpleType()) {
            SimpleProperty property;
            try {
                property = node.getSimpleProperty(name);
            } catch (StorageException e) {
                throw new DocumentException(e);
            }
            return new SQLSimpleProperty(property, type);
        } else if (type.isListType()) {
            ListType listType = (ListType) type;
            if (listType.getFieldType().isSimpleType()) {
                CollectionProperty property;
                try {
                    property = node.getCollectionProperty(name);
                } catch (StorageException e) {
                    throw new DocumentException(e);
                }
                return new SQLCollectionProperty(property, listType);
            } else {
                return new SQLComplexListProperty(node, listType, name, this);
            }
        } else {
            // complex type
            ComplexType fieldComplexType = (ComplexType) type;
            Node childNode;
            try {
                childNode = session.getChildNode(node, name, true);
                if (childNode == null) {
                    // Create the needed complex property. This could also be
                    // done lazily when an actual write is done -- this would
                    // mean refactoring the various SQL*Property classes to hold
                    // parent information.
                    childNode = session.addChildNode(node, name,
                            type.getName(), true);
                }
            } catch (StorageException e) {
                throw new DocumentException(e);
            }
            // TODO use a better switch
            if (type.getName().equals("content")) {
                return new SQLContentProperty(childNode, fieldComplexType, this);
            } else {
                return new SQLComplexProperty(childNode, fieldComplexType, this);
            }
        }
    }

}
