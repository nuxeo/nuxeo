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

package org.nuxeo.ecm.core.storage.sql;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * This class is the bridge between the Nuxeo SPI Session and the actual
 * low-level implementation of the SQL storage session.
 *
 * @author Florent Guillaume
 */
public class SQLModelSession implements org.nuxeo.ecm.core.model.Session {

    private final SQLModelRepository repository;

    private final Map<String, Serializable> context;

    private final SessionImpl session;

    private SQLModelDocument root;

    private String userSessionId;

    public SQLModelSession(SQLModelRepository repository,
            Map<String, Serializable> context) throws DocumentException {
        this.repository = repository;
        if (context == null) {
            context = new HashMap<String, Serializable>();
        }
        this.context = context;

        try {
            // XXX credentials from context
            session = repository.getConnection();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }

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

    public org.nuxeo.ecm.core.model.Repository getRepository() {
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
            Serializable id;
            if (uuid.startsWith("T")) { // temporary id
                id = uuid;
            } else {
                // HACK document ids coming from higher level have been turned
                // into strings (by SQLModelDocument.getUUID) but are really
                // longs for the backend
                id = Long.valueOf(uuid);
            }
            Node node = session.getNodeById(id);
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
        assert source instanceof SQLModelDocument;
        assert parent instanceof SQLModelDocument;
        // XXX TODO
        throw new UnsupportedOperationException();
        // Versioning.getService().fixupAfterCopy((SQLModelDocument) child);
    }

    public Document move(Document source, Document parent, String name)
            throws DocumentException {
        assert source instanceof SQLModelDocument;
        assert parent instanceof SQLModelDocument;
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
        // ((SQLModelDocument) frozenDoc).getNode());
        // node.setProperty(NodeConstants.ECM_REF_UUID.rawname,
        // document.getUUID());
        // return new NuxeoSQLDocumentProxy(this, node);
        // XXX TODO
        throw new UnsupportedOperationException();
    }

    public Collection<Document> getProxies(Document document, Document parent)
            throws DocumentException {
        // XXX TODO
        throw new UnsupportedOperationException();
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
     * ----- Called by SQLModelDocument -----
     */

    private SQLModelDocument newDocument(Node node) throws StorageException {
        if (node == null) {
            // root's parent
            return null;
        }

        // TODO proxies / versions

        return new SQLModelDocument(node, this);
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
            doc = newDocument(session.getChildNode(node, name, Boolean.FALSE));
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
            nodes = session.getChildren(node, Boolean.FALSE);
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
            return session.hasChildNode(node, name, Boolean.FALSE);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected boolean hasChildren(Node node) throws DocumentException {
        try {
            return session.hasChildren(node, Boolean.FALSE);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Document addChild(Node parent, String name, String typeName)
            throws DocumentException {
        try {
            return newDocument(session.addChildNode(parent, name, typeName, false));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected void removeNode(Node node) throws DocumentException {

    }
}
