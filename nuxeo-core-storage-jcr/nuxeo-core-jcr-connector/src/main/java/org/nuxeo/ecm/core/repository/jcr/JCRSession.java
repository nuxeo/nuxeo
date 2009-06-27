/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.QueryResult;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.XASessionImpl;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.UnsupportedQueryTypeException;
import org.nuxeo.ecm.core.repository.jcr.versioning.Versioning;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.utils.SIDGenerator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author M.-A. Darche
 */
public class JCRSession implements Session {

    private static final Log log = LogFactory.getLog(JCRSession.class);

    private JCRRepository repository;

    private Map<String, Serializable> context;

    // the underlying JCR session
    private XASessionImpl session;

    private JCRRoot root;

    private long sid; // the session id

    public JCRSession(JCRRepository repository,
            Map<String, Serializable> context) throws DocumentException {
        this(repository, null, context);
    }

    public JCRSession(JCRRepository repository, String workspaceName,
            Map<String, Serializable> context) throws DocumentException {
        this.repository = repository;
        connect(workspaceName, context);
    }

    private void connect(String name, Map<String, Serializable> context)
            throws DocumentException {
        sid = SIDGenerator.next();
        this.context = context;
        try {
            SimpleCredentials credentials = new SimpleCredentials("username",
                    "password".toCharArray());
            session = (XASessionImpl) repository.jcrRepository().login(
                    credentials, name);
            // TODO only for debug
            // ((XASessionWrapper)session).ecmSession = this;
            // for debug:
            if (context == null) {
                context = new HashMap<String, Serializable>();
            }
            context.put("creationTime", System.currentTimeMillis());
            // if (!context.containsKey("SESSION_ID")) {
            // log.warn("No User Session ID passed through the context -> lazy
            // fields will not work correctly");
            // }
            // context.put("creationStackTrace",
            // Thread.currentThread().getStackTrace());
            this.context = context;
            root = new JCRRoot(this);
            repository.sessionStarted(this);
        } catch (LoginException e) {
            throw new DocumentException(e);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public XASessionImpl getSession() {
        return session;
    }

    public Document getRootDocument() throws DocumentException {
        return root;
    }

    public XAResource getXAResource() {
        return session.getXAResource();
    }

    public void close() throws DocumentException {
        try {
            session.save();
            dispose();
        } catch (RepositoryException e) {
            throw new DocumentException("failed to save session", e);
        }
    }

    public void save() throws DocumentException {
        try {
            session.save();
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to save the session", e);
        }
    }

    public void cancel() throws DocumentException {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to save the session", e);
        }
    }

    public boolean isLive() {
        return session != null && session.isLive();
    }

    public void dispose() {
        log.debug("dispose session" + sid);
        repository.aboutToCloseSession(this);
        session.logout();
        repository.sessionClosed(this);
        root.dispose();
        session = null;
        root = null;
        repository = null;
        context = null;
    }

    public long getSessionId() {
        return sid;
    }

    public String getUserSessionId() {
        return (String) context.get("SESSION_ID");
    }

    public Repository getRepository() {
        return repository;
    }

    public Map<String, Serializable> getSessionContext() {
        return context;
    }

    public Query createQuery(String query, Query.Type qType, String... params)
            throws QueryException {
        if (Query.Type.NXQL == qType) {
            return new JCRQuery(this, query);
        }
        if (Query.Type.XPATH == qType) {
            return new JCRQueryXPath(this, query, params);
        }

        throw new UnsupportedQueryTypeException(qType);
    }

    public SchemaManager getTypeManager() {
        return repository.getTypeManager();
    }

    public SecurityManager getSecurityManager() {
        return repository.getNuxeoSecurityManager();
    }

    public javax.jcr.Session jcrSession() {
        return session;
    }

    public ComplexType getComplexFieldType(Node node)
            throws RepositoryException {
        return (ComplexType) repository.getTypeManager().getType(
                ModelAdapter.getLocalTypeName(node));
    }

    public DocumentType getDocumentType(Node node) throws RepositoryException {
        return repository.getTypeManager().getDocumentType(
                ModelAdapter.getLocalTypeName(node));
    }

    public JCRDocument newDocument(Node node) throws RepositoryException {
        if (node == root.node) {
            return root;
        }
        String jcrTypeName = node.getPrimaryNodeType().getName();
        if (jcrTypeName.equals(NodeConstants.ECM_NT_DOCUMENT_PROXY.rawname)) {
            // a proxy document
            return new JCRDocumentProxy(this, node);
        } else if (Versioning.getService().isVersionNode(node)) {
            // a version document
            return Versioning.getService().newDocumentVersion(this, node);
        }
        // a regular document
        return new JCRDocument(this, node);
    }

    public Document getDocumentByUUID(String uuid) throws DocumentException {
        try {
            Node node = session.getNodeByUUID(uuid);
            return newDocument(node);
        } catch (ItemNotFoundException e) {
            throw new NoSuchDocumentException("No such document width uuid: "
                    + uuid, e);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to get document by UUID", e);
        }
    }

    public Document resolvePath(String path) throws DocumentException {
        if (path.length() == 0) {
            return root;
        }
        return resolvePath(root.node, path);
    }

    /**
     * Resolves the given path relative to the given reference node.
     * <p>
     * The node must be a JCRDocument node.
     *
     * @param ref
     * @param relPath
     * @return
     * @throws DocumentException
     */
    protected final Document resolvePath(Node ref, String relPath)
            throws DocumentException {
        try {
            Path path = new Path(relPath);
            if (path.isAbsolute()) {
                if (path.isRoot()) {
                    return root;
                }
                path = path.makeRelative();
            }

            String jcrPath = ModelAdapter.path2Jcr(path);

            return newDocument(ref.getNode(jcrPath));
        } catch (PathNotFoundException e) {
            throw new NoSuchDocumentException(relPath, e);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public Document copy(Document src, Document dstContainer, String dstName)
            throws DocumentException {
        try {
            if (!dstContainer.isFolder()) {
                throw new DocumentException(
                        "The copy destination is not a folder");
            }

            String srcName = src.getName();
            if (dstName == null) {
                dstName = srcName;
            }

            // test whether source document and naive target document have
            // conflicting JCR paths
            String dstPath = ((JCRDocument) dstContainer).getNode().getPath()
                    + '/' + ModelAdapter.getChildPath(dstName);

            if (dstContainer.hasChild(dstName)) {
                // We have a conflict, generate a new locally unique name based
                dstName = generateDocumentName(src.getName(), dstContainer);
                dstPath = ((JCRDocument) dstContainer).getNode().getPath()
                        + '/' + ModelAdapter.getChildPath(dstName);
            }
            session.getWorkspace().copy(
                    ((JCRDocument) src).getNode().getPath(), dstPath);
            Document child = dstContainer.getChild(dstName);
            Versioning.getService().fixupAfterCopy((JCRDocument) child);
            session.save();
            return child;

        } catch (RepositoryException e) {
            throw new DocumentException("Could not copy the document to "
                    + dstContainer.getPath(), e);
        }
    }

    public Document move(Document src, Document dst, String name)
            throws DocumentException {
        try {
            if (!dst.isFolder()) {
                throw new DocumentException(
                        "The move destination is not a folder");
            }
            if (name == null) {
                name = src.getName();
            }
            String jcrPath = ((JCRDocument) dst).getNode().getPath() + '/'
                    + ModelAdapter.getChildPath(name);

            session.move(((JCRDocument) src).getNode().getPath(), jcrPath);
            return dst.getChild(name);
        } catch (RepositoryException e) {
            throw new DocumentException("Could not move the document to "
                    + dst.getPath(), e);
        }
    }

    public Document importDocument(String uuid, Document parent, String name,
            String typeName, Map<String, Serializable> props)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Document getVersion(String versionableId, VersionModel versionModel)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Document createProxyForVersion(Document parent, Document doc,
            String versionLabel) throws DocumentException {
        try {
            Document frozenDoc = doc.getVersion(versionLabel);
            String name = doc.getName() + '_' + System.currentTimeMillis(); // TODO
            Node pnode = ((JCRDocument) parent).getNode();
            Node node = pnode.addNode(ModelAdapter.getChildPath(name),
                    NodeConstants.ECM_NT_DOCUMENT_PROXY.rawname);
            // node.addMixin(NodeConstants.ECM_NT_DOCUMENT_PROXY.rawname);
            node.setProperty(NodeConstants.ECM_REF_FROZEN_NODE.rawname,
                    ((JCRDocument) frozenDoc).getNode());
            node.setProperty(NodeConstants.ECM_REF_UUID.rawname, doc.getUUID());
            return new JCRDocumentProxy(this, node);
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "Failed to create proxy for frozen document: "
                            + doc.getName(), e);
        }
    }

    public Collection<Document> getProxies(Document doc, Document folder)
            throws DocumentException {
        NodeIterator it = findProxyNodes(doc, folder);
        List<Document> proxies = new ArrayList<Document>();
        while (it.hasNext()) {
            try {
                proxies.add(newDocument(it.nextNode()));
            } catch (RepositoryException e) {
                throw new DocumentException("failed to create document proxy",
                        e);
            }
        }
        return proxies;
    }

    protected NodeIterator findProxyNodes(Document doc, Document folder)
            throws DocumentException {
        JCRName attribute;
        if (doc.isVersion()) {
            attribute = NodeConstants.ECM_REF_FROZEN_NODE;
        } else {
            attribute = NodeConstants.ECM_REF_UUID;
        }
        String uuid = doc.getUUID();
        try {
            String queryString;
            if (folder == null) {
                queryString = "/";
            } else {
                String path = ((JCRDocument) folder).getNode().getPath();
                queryString = "/jcr:root/" + JCRQueryXPath.quotePath(path)
                        + '/' + NodeConstants.ECM_CHILDREN.rawname;
            }
            queryString += "/*[@" + attribute.rawname + " = '" + uuid
                    + "' and @" + NodeConstants.ECM_ISPROXY.rawname + " = 1 ]";
            javax.jcr.query.Query query = session.getWorkspace().getQueryManager().createQuery(
                    queryString, javax.jcr.query.Query.XPATH);
            QueryResult result = query.execute();
            return result.getNodes();
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to find proxy nodes for "
                    + uuid, e);
        }
    }

    public InputStream getDataStream(String key) throws DocumentException {
        try {
            Item item = session.getItem(key);
            return ((Property) item).getStream();
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to fetch blob content from "
                    + key);
        }
    }

    void documentLocked(JCRDocument doc) {

    }

    void documentUnlocked(JCRDocument doc) {

    }

    @Override
    public String toString() {
        String sessionStr = session == null ? "N/A" : session.toString();
        return System.identityHashCode(this) + ":" + sid + ':' + sessionStr;
    }

    public String generateDocumentName(String name, Document parent)
            throws DocumentException {
        if (name == null || name.length() == 0) {
            name = IdUtils.generateStringId();
        }
        if (parent.hasChild(name)) {
            name = name + '.' + System.currentTimeMillis();
        }
        return name;
    }

    @Override
    protected void finalize() throws Throwable {
        if (session != null && session.isLive()) {
            dispose();
        }
    }

}
