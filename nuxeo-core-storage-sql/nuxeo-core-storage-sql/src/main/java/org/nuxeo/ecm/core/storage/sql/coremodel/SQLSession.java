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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.FilterableQuery;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.query.UnsupportedQueryTypeException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
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

    private final Repository repository;

    private final Map<String, Serializable> context;

    private final org.nuxeo.ecm.core.storage.sql.Session session;

    private Document root;

    private final String userSessionId;

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

        Node rootNode;
        try {
            rootNode = session.getRootNode();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        root = newDocument(rootNode);

        userSessionId = (String) context.get("SESSION_ID");
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Session -----
     */

    @Override
    public Document getRootDocument() {
        return root;
    }

    @Override
    public Document getNullDocument() {
        return new SQLDocumentLive(null, null, null, this, true);
    }

    // not called
    @Override
    public XAResource getXAResource() {
        throw new RuntimeException();
    }

    @Override
    public void close() throws DocumentException {
        try {
            session.save();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        dispose();
    }

    @Override
    public void save() throws DocumentException {
        try {
            session.save();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public void cancel() throws DocumentException {
        // TODO
        // throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLive() {
        return session != null && session.isLive();
    }

    @Override
    public void dispose() {
        try {
            session.close();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
        root = null;
    }

    // not used?
    @Override
    public long getSessionId() {
        throw new RuntimeException();
        // return sid;
    }

    @Override
    public String getUserSessionId() {
        return userSessionId;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public Map<String, Serializable> getSessionContext() {
        return context;
    }

    @Override
    public SchemaManager getTypeManager() {
        return repository.getTypeManager();
    }

    @Override
    public SecurityManager getSecurityManager() {
        return repository.getNuxeoSecurityManager();
    }

    @Override
    public Document getDocumentByUUID(String uuid) throws DocumentException {
        try {
            /**
             * Document ids coming from higher level have been turned into
             * strings (by {@link SQLDocument#getUUID}) but the backend may
             * actually expect them to be Longs (for database-generated integer
             * ids).
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

    @Override
    public Document resolvePath(String path) throws DocumentException {
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        Node node;
        try {
            node = session.getNodeByPath(path, session.getRootNode());
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        Document doc = newDocument(node);
        if (doc == null) {
            throw new NoSuchDocumentException("No such document: " + path);
        }
        return doc;
    }

    protected void orderBefore(Node node, Node src, Node dest)
            throws DocumentException {
        try {
            session.orderBefore(node, src, dest);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public Document move(Document source, Document parent, String name)
            throws DocumentException {
        assert source instanceof SQLDocument;
        assert parent instanceof SQLDocument;
        try {
            if (name == null) {
                name = source.getName();
            }
            Node result = session.move(((SQLDocument) source).getNode(),
                    ((SQLDocument) parent).getNode(), name);
            return newDocument(result);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    private static final Pattern dotDigitsPattern = Pattern.compile("(.*)\\.[0-9]+$");

    protected String findFreeName(Node parentNode, String name)
            throws StorageException {
        if (session.hasChildNode(parentNode, name, false)) {
            Matcher m = dotDigitsPattern.matcher(name);
            if (m.matches()) {
                // remove trailing dot and digits
                name = m.group(1);
            }
            // add dot + unique digits
            name += "." + System.currentTimeMillis();
        }
        return name;
    }

    @Override
    public Document copy(Document source, Document parent, String name)
            throws DocumentException {
        assert source instanceof SQLDocument;
        assert parent instanceof SQLDocument;
        try {
            if (name == null) {
                name = source.getName();
            }
            Node parentNode = ((SQLDocument) parent).getNode();
            name = findFreeName(parentNode, name);
            Node copy = session.copy(((SQLDocument) source).getNode(),
                    parentNode, name);
            return newDocument(copy);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public Document getVersion(String versionableId, VersionModel versionModel)
            throws DocumentException {
        try {
            Serializable vid = session.getModel().unHackStringId(versionableId);
            Node versionNode = session.getVersionByLabel(vid,
                    versionModel.getLabel());
            if (versionNode == null) {
                return null;
            }
            versionModel.setDescription(versionNode.getSimpleProperty(
                    Model.VERSION_DESCRIPTION_PROP).getString());
            versionModel.setCreated((Calendar) versionNode.getSimpleProperty(
                    Model.VERSION_CREATED_PROP).getValue());
            return newDocument(versionNode);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public Document createProxy(Document doc, Document folder)
            throws DocumentException {
        try {
            Node folderNode = ((SQLDocument) folder).getNode();
            Node targetNode = ((SQLDocument) doc).getNode();
            Serializable targetId = targetNode.getId();
            Serializable versionableId;
            if (doc.isVersion()) {
                versionableId = (Serializable) doc.getProperty(
                        Model.VERSION_VERSIONABLE_PROP).getValue();
            } else if (doc.isProxy()) {
                // copy the proxy
                targetId = (Serializable) doc.getProperty(
                        Model.PROXY_TARGET_PROP).getValue();
                versionableId = (Serializable) doc.getProperty(
                        Model.PROXY_VERSIONABLE_PROP).getValue();
            } else {
                // working copy (live document)
                versionableId = targetId;
            }
            String name = findFreeName(folderNode, doc.getName());
            Node proxy = session.addProxy(targetId, versionableId, folderNode,
                    name, null);
            return newDocument(proxy);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public Collection<Document> getProxies(Document document, Document parent)
            throws DocumentException {
        Collection<Node> proxyNodes;
        try {
            proxyNodes = session.getProxies(((SQLDocument) document).getNode(),
                    parent == null ? null : ((SQLDocument) parent).getNode());
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        List<Document> proxies = new ArrayList<Document>(proxyNodes.size());
        for (Node proxyNode : proxyNodes) {
            proxies.add(newDocument(proxyNode));
        }
        return proxies;
    }

    @Override
    public void setProxyTarget(Document proxy, Document target)
            throws DocumentException {
        Node proxyNode = ((SQLDocument) proxy).getNode();
        String targetId = target.getUUID();
        try {
            session.setProxyTarget(proxyNode, targetId);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public InputStream getDataStream(String key) throws DocumentException {
        // XXX TODO
        throw new UnsupportedOperationException();
    }

    // returned document is r/w even if a version or a proxy, so that normal
    // props can be set
    @Override
    public Document importDocument(String uuid, Document parent, String name,
            String typeName, Map<String, Serializable> properties)
            throws DocumentException {
        assert Model.PROXY_TYPE == CoreSession.IMPORT_PROXY_TYPE;
        boolean isProxy = typeName.equals(Model.PROXY_TYPE);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        Long pos = null; // TODO pos
        if (!isProxy) {
            // version & live document
            props.put(Model.MISC_LIFECYCLE_POLICY_PROP,
                    properties.get(CoreSession.IMPORT_LIFECYCLE_POLICY));
            props.put(Model.MISC_LIFECYCLE_STATE_PROP,
                    properties.get(CoreSession.IMPORT_LIFECYCLE_STATE));
            // compat with old lock import
            String key = (String) properties.get(CoreSession.IMPORT_LOCK);
            if (key != null) {
                String[] values = key.split(":");
                if (values.length == 2) {
                    String owner = values[0];
                    Calendar created = new GregorianCalendar();
                    try {
                        created.setTimeInMillis(DateFormat.getDateInstance(
                                DateFormat.MEDIUM).parse(values[1]).getTime());
                    } catch (ParseException e) {
                        // use current date
                    }
                    props.put(Model.LOCK_OWNER_PROP, owner);
                    props.put(Model.LOCK_CREATED_PROP, created);
                }
            }

            Serializable importLockOwnerProp = properties.get(CoreSession.IMPORT_LOCK_OWNER);
            if (importLockOwnerProp != null) {
                props.put(Model.LOCK_OWNER_PROP, importLockOwnerProp);
            }
            Serializable importLockCreatedProp = properties.get(CoreSession.IMPORT_LOCK_CREATED);
            if (importLockCreatedProp != null) {
                props.put(Model.LOCK_CREATED_PROP, importLockCreatedProp);
            }

            props.put(Model.MAIN_MAJOR_VERSION_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_MAJOR));
            props.put(Model.MAIN_MINOR_VERSION_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_MINOR));
            props.put(Model.MAIN_IS_VERSION_PROP,
                    properties.get(CoreSession.IMPORT_IS_VERSION));
        }
        Node parentNode;
        if (parent == null) {
            // version
            parentNode = null;
            props.put(Model.VERSION_VERSIONABLE_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_VERSIONABLE_ID));
            props.put(Model.VERSION_CREATED_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_CREATED));
            props.put(Model.VERSION_LABEL_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_LABEL));
            props.put(Model.VERSION_DESCRIPTION_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_DESCRIPTION));
            props.put(Model.VERSION_IS_LATEST_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_IS_LATEST));
            props.put(Model.VERSION_IS_LATEST_MAJOR_PROP,
                    properties.get(CoreSession.IMPORT_VERSION_IS_LATEST_MAJOR));
        } else {
            parentNode = ((SQLDocument) parent).getNode();
            if (isProxy) {
                // proxy
                props.put(Model.PROXY_TARGET_PROP,
                        properties.get(CoreSession.IMPORT_PROXY_TARGET_ID));
                props.put(Model.PROXY_VERSIONABLE_PROP,
                        properties.get(CoreSession.IMPORT_PROXY_VERSIONABLE_ID));
            } else {
                // live document
                props.put(Model.MAIN_BASE_VERSION_PROP,
                        properties.get(CoreSession.IMPORT_BASE_VERSION_ID));
                props.put(Model.MAIN_CHECKED_IN_PROP,
                        properties.get(CoreSession.IMPORT_CHECKED_IN));
            }
        }
        return importChild(uuid, parentNode, name, pos, typeName, props);
    }

    @Override
    public Query createQuery(String query, String queryType, String... params)
            throws QueryException {
        if (params != null && params.length != 0) {
            throw new QueryException("Parameters not supported");
        }
        try {
            return new SQLSessionQuery(query, queryType);
        } catch (QueryParseException e) {
            throw new QueryException(e.getMessage() + ": " + query, e);
        }
    }

    @Override
    public Query createQuery(String query, Query.Type qType, String... params)
            throws QueryException {
        if (qType != Query.Type.NXQL) {
            throw new UnsupportedQueryTypeException(qType);
        }
        if (params != null && params.length != 0) {
            throw new QueryException("Parameters not supported");
        }
        try {
            return new SQLSessionQuery(query);
        } catch (QueryParseException e) {
            throw new QueryException(e.getMessage() + ": " + query, e);
        }
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws QueryException {
        return new SQLSessionQuery(query, queryType).executeAndFetch(
                queryFilter, params);
    }

    protected static final Pattern ORDER_BY_PATH_ASC = Pattern.compile(
            "(.*)\\s+ORDER\\s+BY\\s+" + NXQL.ECM_PATH + "\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected static final Pattern ORDER_BY_PATH_DESC = Pattern.compile(
            "(.*)\\s+ORDER\\s+BY\\s+" + NXQL.ECM_PATH + "\\s+DESC\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected class SQLSessionQuery implements FilterableQuery {

        protected final String query;

        protected final String queryType;

        public SQLSessionQuery(String query) {
            this.query = query;
            queryType = NXQL.NXQL;
        }

        public SQLSessionQuery(String query, String queryType) {
            this.query = query;
            this.queryType = queryType;
        }

        @Override
        public QueryResult execute() throws QueryException {
            return execute(QueryFilter.EMPTY, false);
        }

        @Override
        public QueryResult execute(boolean countTotal) throws QueryException {
            return execute(QueryFilter.EMPTY, countTotal);
        }

        @Override
        public QueryResult execute(QueryFilter queryFilter, boolean countTotal)
                throws QueryException {
            try {
                String query = this.query;
                // do ORDER BY ecm:path by hand in SQLQueryResult as we can't
                // do it in SQL (and has to do limit/offset as well)
                Boolean orderByPath;
                Matcher matcher = ORDER_BY_PATH_ASC.matcher(query);
                if (matcher.matches()) {
                    orderByPath = Boolean.TRUE; // ASC
                } else {
                    matcher = ORDER_BY_PATH_DESC.matcher(query);
                    if (matcher.matches()) {
                        orderByPath = Boolean.FALSE; // DESC
                    } else {
                        orderByPath = null;
                    }
                }
                long limit = 0;
                long offset = 0;
                if (orderByPath != null) {
                    query = matcher.group(1);
                    limit = queryFilter.getLimit();
                    offset = queryFilter.getOffset();
                    queryFilter = QueryFilter.withoutLimitOffset(queryFilter);
                }
                PartialList<Serializable> list = session.query(query,
                        queryType, queryFilter, countTotal);
                return new SQLQueryResult(SQLSession.this, list, orderByPath,
                        limit, offset);
            } catch (StorageException e) {
                throw new QueryException(e.getMessage(), e);
            }
        }

        public IterableQueryResult executeAndFetch(QueryFilter queryFilter,
                Object... params) throws QueryException {
            try {
                return session.queryAndFetch(query, queryType, queryFilter,
                        params);
            } catch (StorageException e) {
                throw new QueryException(e.getMessage(), e);
            }
        }

    }

    /*
     * ----- called by SQLDocument -----
     */

    private Document newDocument(Node node) throws DocumentException {
        return newDocument(node, true);
    }

    // "readonly" meaningful for proxies and versions, used for import
    private Document newDocument(Node node, boolean readonly)
            throws DocumentException {
        if (node == null) {
            // root's parent
            return null;
        }

        Node targetNode = null;
        String typeName = node.getPrimaryType();
        if (node.isProxy()) {
            try {
                Serializable targetId = node.getSimpleProperty(
                        Model.PROXY_TARGET_PROP).getValue();
                if (targetId == null) {
                    throw new DocumentException("Proxy has null target");
                }
                targetNode = session.getNodeById(targetId);
                typeName = targetNode.getPrimaryType();
            } catch (StorageException e) {
                throw new DocumentException(e);
            }
        }
        TypeProvider typeProvider = getTypeManager();
        DocumentType type = typeProvider.getDocumentType(typeName);
        if (type == null) {
            throw new DocumentException("Unknown document type: " + typeName);
        }
        String[] mixins = node.getMixinTypes();
        List<CompositeType> mixinTypes = new ArrayList<CompositeType>(
                mixins.length);
        for (String mixin : mixins) {
            CompositeType mixinType = typeProvider.getFacet(mixin);
            if (mixinType != null) {
                mixinTypes.add(mixinType);
            }
        }

        if (node.isProxy()) {
            // proxy seen as a normal document
            Document proxy = new SQLDocumentLive(node, type, mixinTypes, this,
                    false);
            Document target = newDocument(targetNode, readonly);
            return new SQLDocumentProxy(proxy, target);
        } else if (node.isVersion()) {
            return new SQLDocumentVersion(node, type, mixinTypes, this,
                    readonly);
        } else {
            return new SQLDocumentLive(node, type, mixinTypes, this, false);
        }
    }

    // called by SQLQueryResult iterator & others
    protected Document getDocumentById(Serializable id)
            throws DocumentException {
        try {
            Node node = session.getNodeById(id);
            return node == null ? null : newDocument(node);
        } catch (StorageException e) {
            throw new DocumentException("Failed to get document: " + id, e);
        }
    }

    // called by SQLQueryResult iterator
    protected List<Document> getDocumentsById(List<Serializable> ids)
            throws DocumentException {
        List<Document> docs = new ArrayList<Document>(ids.size());
        try {
            List<Node> nodes = session.getNodesByIds(ids);
            for (Node node : nodes) {
                if (node == null) {
                    continue;
                }
                docs.add(newDocument(node));
            }
        } catch (StorageException e) {
            throw new DocumentException(e.toString(), e);
        }
        return docs;
    }

    // called by SQLContentProperty
    protected Binary getBinary(InputStream in) throws DocumentException {
        try {
            return session.getBinary(in);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
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
        Node childNode;
        try {
            childNode = session.getChildNode(node, name, false);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        Document doc = newDocument(childNode);
        if (doc == null) {
            throw new NoSuchDocumentException("No such document: " + name);
        }
        return doc;
    }

    // XXX change to iterator?
    protected List<Document> getChildren(Node node) throws DocumentException {
        List<Node> nodes;
        try {
            nodes = session.getChildren(node, null, false);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        List<Document> children = new ArrayList<Document>(nodes.size());
        for (Node n : nodes) {
            try {
                children.add(newDocument(n));
            } catch (DocumentException e) {
                // ignore error retrieving one of the children
                continue;
            }
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

    protected Document addChild(Node parent, String name, Long pos,
            String typeName) throws DocumentException {
        try {
            return newDocument(session.addChildNode(parent, name, pos,
                    typeName, false));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Document importChild(String uuid, Node parent, String name,
            Long pos, String typeName, Map<String, Serializable> props)
            throws DocumentException {
        try {
            Serializable id = session.getModel().unHackStringId(uuid);
            Node node = session.addChildNode(id, parent, name, pos, typeName,
                    false);
            for (Entry<String, Serializable> entry : props.entrySet()) {
                node.setSimpleProperty(entry.getKey(), entry.getValue());
            }
            return newDocument(node, false); // not readonly
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected List<Node> getComplexList(Node node, String name)
            throws DocumentException {
        List<Node> nodes;
        try {
            nodes = session.getChildren(node, name, true);
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

    protected void removeProperty(Node node) throws DocumentException {
        try {
            session.removePropertyNode(node);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Document checkIn(Node node, String label, String checkinComment)
            throws DocumentException {
        try {
            Node versionNode = session.checkIn(node, label, checkinComment);
            return versionNode == null ? null : newDocument(versionNode);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected void checkOut(Node node) throws DocumentException {
        try {
            session.checkOut(node);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected void restore(Node node, Node version) throws DocumentException {
        try {
            session.restore(node, version);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Document getVersionByLabel(Serializable versionSeriesId,
            String label) throws DocumentException {
        try {
            Node versionNode = session.getVersionByLabel(versionSeriesId, label);
            return versionNode == null ? null : newDocument(versionNode);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected List<Document> getVersions(Serializable versionSeriesId)
            throws DocumentException {
        List<Node> versionNodes;
        try {
            versionNodes = session.getVersions(versionSeriesId);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        List<Document> versions = new ArrayList<Document>(versionNodes.size());
        for (Node versionNode : versionNodes) {
            versions.add(newDocument(versionNode));
        }
        return versions;
    }

    public Document getLastVersion(Serializable versionSeriesId)
            throws DocumentException {
        try {
            Node versionNode = session.getLastVersion(versionSeriesId);
            if (versionNode == null) {
                return null;
            }
            return newDocument(versionNode);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Node getNodeById(Serializable id) throws DocumentException {
        try {
            return session.getNodeById(id);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Lock getLock(Node node) throws DocumentException {
        try {
            return session.getLock(node.getId());
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Lock setLock(Node node, Lock lock) throws DocumentException {
        try {
            return session.setLock(node.getId(), lock);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected Lock removeLock(Node node, String owner) throws DocumentException {
        try {
            return session.removeLock(node.getId(), owner, false);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    /*
     * ----- property helpers -----
     */

    protected Property makeACLProperty(Node node) throws DocumentException {
        CollectionProperty property;
        try {
            property = node.getCollectionProperty(Model.ACL_PROP);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        return new SQLCollectionProperty(this, property, null, false);
    }

    /** Make a property. */
    protected Property makeProperty(Node node, String name,
            ComplexType parentType, List<CompositeType> mixinTypes,
            boolean readonly) throws DocumentException {
        return makeProperties(node, name, parentType, mixinTypes, readonly, 0).get(
                0);
    }

    /**
     * Make properties, either a single one, or the whole list for complex list
     * elements.
     */
    protected List<Property> makeProperties(Node node, String name,
            Type parentType, List<CompositeType> mixinTypes, boolean readonly,
            int complexListSize) throws DocumentException {
        boolean complexList = parentType instanceof ListType;
        Model model;
        try {
            model = session.getModel();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        Type type = model.getSpecialPropertyType(name);
        if (type == null) {
            Field field;
            if (complexList) {
                field = ((ListType) parentType).getField();
            } else {
                field = ((ComplexType) parentType).getField(name);
                if (field == null) {
                    // check mixin types
                    for (CompositeType mixinType : mixinTypes) {
                        field = mixinType.getField(name);
                        if (field != null) {
                            break;
                        }
                    }
                }
                if (field == null) {
                    throw new NoSuchPropertyException(name);
                }
                // qualify if necessary (some callers pass unprefixed names)
                name = field.getName().getPrefixedName();
            }
            type = field.getType();
        }

        if (type.isSimpleType()) {
            SimpleProperty prop;
            try {
                prop = node.getSimpleProperty(name);
            } catch (StorageException e) {
                throw new DocumentException(e);
            }
            Property property = new SQLSimpleProperty(prop, type, readonly);
            return Collections.singletonList(property);
        } else if (type.isListType()) {
            Property property;
            ListType listType = (ListType) type;
            if (listType.getFieldType().isSimpleType()) {
                CollectionProperty prop;
                try {
                    prop = node.getCollectionProperty(name);
                } catch (StorageException e) {
                    throw new DocumentException(e);
                }
                property = new SQLCollectionProperty(this, prop, listType,
                        readonly);
            } else {
                property = new SQLComplexListProperty(node, listType, name,
                        this, readonly);
            }
            return Collections.singletonList(property);
        } else {
            // complex type, may be part of a complex list or not
            List<Node> childNodes;
            try {
                if (complexList) {
                    if (complexListSize == -1) {
                        // get existing
                        childNodes = session.getChildren(node, name, true);
                    } else {
                        // create with given size (after a remove)
                        childNodes = new ArrayList<Node>(complexListSize);
                        for (int i = 0; i < complexListSize; i++) {
                            Node childNode = session.addChildNode(node, name,
                                    Long.valueOf(i), type.getName(), true);
                            childNodes.add(childNode);
                        }
                    }
                } else {
                    Node childNode = session.getChildNode(node, name, true);
                    if (childNode == null) {
                        // Create the needed complex property. This could also
                        // be done lazily when an actual write is done -- this
                        // would mean refactoring the various SQL*Property
                        // classes to hold parent information.
                        childNode = session.addChildNode(node, name, null,
                                type.getName(), true);
                    }
                    childNodes = Collections.singletonList(childNode);
                }
            } catch (StorageException e) {
                throw new DocumentException(e);
            }
            List<Property> properties = new ArrayList<Property>(
                    childNodes.size());
            for (Node childNode : childNodes) {
                Property property = newSQLComplexProperty(childNode,
                        (ComplexType) type, readonly);
                properties.add(property);
            }
            return properties;
        }
    }

    /**
     * Makes a property from a complex list element.
     *
     * @since 5.5
     */
    protected Property makeProperty(Node node, String name, Type parentType,
            boolean readonly, int pos) throws DocumentException {
        Type type = ((ListType) parentType).getField().getType();
        List<Node> childNodes;
        try {
            childNodes = session.getChildren(node, name, true);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        if (pos < 0 || pos >= childNodes.size()) {
            throw new NoSuchPropertyException(name + '/' + pos);
        }
        Node childNode = childNodes.get(pos);
        return newSQLComplexProperty(childNode, (ComplexType) type, readonly);
    }

    protected Property newSQLComplexProperty(Node childNode, ComplexType type,
            boolean readonly) {
        // TODO use a better switch
        if (TypeConstants.isContentType(type)) {
            return new SQLContentProperty(childNode, type, this, readonly);
        } else {
            return new SQLComplexProperty(childNode, type, this, readonly);
        }
    }

    /**
     * This method flag the current session, the read ACLs update will be done
     * automatically at save time.
     */
    public void requireReadAclsUpdate() {
        session.requireReadAclsUpdate();
    }

}
