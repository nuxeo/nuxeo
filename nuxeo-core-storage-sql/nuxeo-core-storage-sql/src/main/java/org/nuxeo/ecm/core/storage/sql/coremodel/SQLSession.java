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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ConcurrentUpdateDocumentException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
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
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.core.storage.ConcurrentUpdateStorageException;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.CollectionProperty;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.SimpleProperty;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * This class is the bridge between the Nuxeo SPI Session and the actual
 * low-level implementation of the SQL storage session.
 *
 * @author Florent Guillaume
 */
public class SQLSession implements Session {

    protected final Log log = LogFactory.getLog(SQLSession.class);

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
        } catch (ConcurrentUpdateStorageException e) {
            throw new ConcurrentUpdateDocumentException(e);
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
    public boolean isStateSharedByAllThreadSessions() {
        return session.isStateSharedByAllThreadSessions();
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

    protected String idToString(Serializable id) {
        try {
            return session.getModel().idToString(id);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    protected Serializable idFromString(String id) {
        try {
            return session.getModel().idFromString(id);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Document getDocumentByUUID(String uuid) throws DocumentException {
        /*
         * Document ids coming from higher level have been turned into
         * strings (by {@link SQLDocument#getUUID}) but the backend may
         * actually expect them to be Longs (for database-generated integer
         * ids).
         */
        Document doc = getDocumentById(idFromString(uuid));
        if (doc == null) {
            // required by callers such as AbstractSession.exists
            throw new NoSuchDocumentException(uuid);
        }
        return doc;
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
            Serializable vid = idFromString(versionableId);
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
        Serializable targetId = idFromString(target.getUUID());
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
            props.put(
                    Model.VERSION_VERSIONABLE_PROP,
                    idFromString((String) properties.get(CoreSession.IMPORT_VERSION_VERSIONABLE_ID)));
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
                props.put(
                        Model.PROXY_TARGET_PROP,
                        idFromString((String) properties.get(CoreSession.IMPORT_PROXY_TARGET_ID)));
                props.put(
                        Model.PROXY_VERSIONABLE_PROP,
                        idFromString((String) properties.get(CoreSession.IMPORT_PROXY_VERSIONABLE_ID)));
            } else {
                // live document
                props.put(
                        Model.MAIN_BASE_VERSION_PROP,
                        idFromString((String) properties.get(CoreSession.IMPORT_BASE_VERSION_ID)));
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
            return execute(queryFilter, countTotal ? -1: 0);
        }

        @Override
        public QueryResult execute(QueryFilter queryFilter, long countUpTo)
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
                        queryType, queryFilter, countUpTo);
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

        @Override
        public QueryResult execute(long countUpTo) throws QueryException {
            return execute(QueryFilter.EMPTY, countUpTo);
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
            for (int index = 0; index < ids.size(); ++index) {
                Node eachNode = nodes.get(index);
                if (eachNode == null) {
                    Serializable eachId = ids.get(index);
                    log.warn("Cannot fetch document by id " + eachId, new Throwable("debug stack trace"));
                    continue;
                }
                docs.add(newDocument(eachNode));
            }
        } catch (StorageException e) {
            throw new DocumentException(e.toString(), e);
        }
        return docs;
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
            Serializable id = idFromString(uuid);
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

    protected boolean addMixinType(Node node, String mixin)
            throws DocumentException {
        try {
            return session.addMixinType(node, mixin);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected boolean removeMixinType(Node node, String mixin)
            throws DocumentException {
        try {
            return session.removeMixinType(node, mixin);
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
        } catch (ConcurrentUpdateStorageException e) {
            throw new ConcurrentUpdateDocumentException(e);
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

    protected Document getVersionByLabel(String versionSeriesId,
            String label) throws DocumentException {
        try {
            Serializable vid = idFromString(versionSeriesId);
            Node versionNode = session.getVersionByLabel(vid, label);
            return versionNode == null ? null : newDocument(versionNode);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected List<Document> getVersions(String versionSeriesId)
            throws DocumentException {
        List<Node> versionNodes;
        try {
            Serializable vid = idFromString(versionSeriesId);
            versionNodes = session.getVersions(vid);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        List<Document> versions = new ArrayList<Document>(versionNodes.size());
        for (Node versionNode : versionNodes) {
            versions.add(newDocument(versionNode));
        }
        return versions;
    }

    public Document getLastVersion(String versionSeriesId)
            throws DocumentException {
        try {
            Serializable vid = idFromString(versionSeriesId);
            Node versionNode = session.getLastVersion(vid);
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

    protected Property makeACLProperty(Node node, SQLDocument doc)
            throws DocumentException {
        CollectionProperty property;
        try {
            property = node.getCollectionProperty(Model.ACL_PROP);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        return new SQLCollectionProperty(property, null, doc);
    }

    /** Make a property. */
    protected Property makeProperty(Node node, String name,
            ComplexType parentType, SQLDocument doc,
            List<CompositeType> mixinTypes, List<Schema> proxySchemas)
            throws DocumentException {
        return makeProperties(node, name, parentType, doc, mixinTypes,
                proxySchemas, 0, 0).get(0);
    }

    /**
     * Make properties, either a single one, or the whole list for complex list
     * elements.
     */
    protected List<Property> makeProperties(Node node, String name,
            Type parentType, SQLDocument doc, List<CompositeType> mixinTypes,
            List<Schema> proxySchemas, int complexListStart, int complexListEnd)
            throws DocumentException {
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
                if (field == null && proxySchemas != null) {
                    // check proxy schemas
                    for (Schema schema : proxySchemas) {
                        field = schema.getField(name);
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
            Property property = new SQLSimpleProperty(prop, type, doc);
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
                property = new SQLCollectionProperty(prop, listType, doc);
            } else {
                property = new SQLComplexListProperty(node, listType, name, doc);
            }
            return Collections.singletonList(property);
        } else {
            // complex type, may be part of a complex list or not
            List<Node> childNodes;
            try {
                if (complexList) {
                    if (complexListStart == -1) {
                        // get existing
                        childNodes = getComplexList(node, name);
                    } else {
                        // create with given size (after a remove)
                        childNodes = new ArrayList<Node>(complexListEnd
                                - complexListStart);
                        for (int i = complexListStart; i < complexListEnd; i++) {
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
                        (ComplexType) type, doc);
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
            SQLDocument doc, int pos) throws DocumentException {
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
        return newSQLComplexProperty(childNode, (ComplexType) type, doc);
    }

    protected Property newSQLComplexProperty(Node childNode, ComplexType type,
            SQLDocument doc) {
        // TODO use a better switch
        if (TypeConstants.isContentType(type)) {
            return new SQLContentProperty(childNode, type, doc);
        } else {
            return new SQLComplexProperty(childNode, type, doc);
        }
    }

    /**
     * This method flag the current session, the read ACLs update will be done
     * automatically at save time.
     */
    public void requireReadAclsUpdate() {
        session.requireReadAclsUpdate();
    }

    /**
     * @param blob
     * @return
     * @throws DocumentException
     */
    public Binary getBinary(Blob blob) throws DocumentException {
        if (blob instanceof SQLBlob) {
            return ((SQLBlob) blob).binary;
        }
        StreamSource source;
        try {
            if (blob instanceof StreamingBlob) {
                StreamingBlob sb = (StreamingBlob) blob;
                source = sb.getStreamSource();
                if (source instanceof FileSource && sb.isTemporary()) {
                    return session.getBinary((FileSource)source);
                }
            }
            InputStream stream;
            try {
                stream = blob.getStream();
            } catch (IOException e) {
                throw new DocumentException(e);
            }
            return session.getBinary(stream);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public ACP getACP(Document doc) throws SecurityException {
        try {
            Property property = ((SQLDocument) doc).getACLProperty();
            return aclRowsToACP((ACLRow[]) property.getValue());
        } catch (DocumentException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }

    @Override
    public void setACP(Document doc, ACP acp, boolean overwrite)
            throws SecurityException {
        if (!overwrite && acp == null) {
            return;
        }
        try {
            Property property = ((SQLDocument) doc).getACLProperty();
            ACLRow[] aclrows;
            if (overwrite) {
                aclrows = acp == null ? null : acpToAclRows(acp);
            } else {
                aclrows = updateAclRows((ACLRow[]) property.getValue(), acp);
            }
            property.setValue(aclrows);
        } catch (DocumentException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }

    @Override
    public ACP getMergedACP(Document doc) throws SecurityException {
        try {
            Document base = doc.isVersion() ? doc.getSourceDocument() : doc;
            if (base == null) {
                return null;
            }
            ACP acp = getACP(base);
            if (doc.getParent() == null) {
                return acp;
            }
            // get inherited acls only if no blocking inheritance ACE exists in the top level acp.
            ACL acl = null;
            if (acp == null || acp.getAccess(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING) != Access.DENY) {
                acl = getInheritedACLs(doc);
            }
            if (acp == null) {
                if (acl == null) {
                    return null;
                }
                acp = new ACPImpl();
            }
            if (acl != null) {
                acp.addACL(acl);
            }
            return acp;
        } catch (DocumentException e) {
            throw new SecurityException("Failed to get merged acp", e);
        }
    }

    /*
     * ----- internal methods -----
     */

    // unit tested
    protected static ACP aclRowsToACP(ACLRow[] acls) {
        ACP acp = new ACPImpl();
        ACL acl = null;
        String name = null;
        for (ACLRow aclrow : acls) {
            if (!aclrow.name.equals(name)) {
                if (acl != null) {
                    acp.addACL(acl);
                }
                name = aclrow.name;
                acl = new ACLImpl(name);
            }
            // XXX should prefix user/group
            String user = aclrow.user;
            if (user == null) {
                user = aclrow.group;
            }
            acl.add(new ACE(user, aclrow.permission, aclrow.grant));
        }
        if (acl != null) {
            acp.addACL(acl);
        }
        return acp;
    }

    // unit tested
    protected static ACLRow[] acpToAclRows(ACP acp) {
        List<ACLRow> aclrows = new LinkedList<ACLRow>();
        for (ACL acl : acp.getACLs()) {
            String name = acl.getName();
            if (name.equals(ACL.INHERITED_ACL)) {
                continue;
            }
            for (ACE ace : acl.getACEs()) {
                addACLRow(aclrows, name, ace);
            }
        }
        ACLRow[] array = new ACLRow[aclrows.size()];
        return aclrows.toArray(array);
    }

    // unit tested
    protected static ACLRow[] updateAclRows(ACLRow[] aclrows, ACP acp) {
        List<ACLRow> newaclrows = new LinkedList<ACLRow>();
        Map<String, ACL> aclmap = new HashMap<String, ACL>();
        for (ACL acl : acp.getACLs()) {
            String name = acl.getName();
            if (ACL.INHERITED_ACL.equals(name)) {
                continue;
            }
            aclmap.put(name, acl);
        }
        List<ACE> aces = Collections.emptyList();
        Set<String> aceKeys = null;
        String name = null;
        for (ACLRow aclrow : aclrows) {
            // new acl?
            if (!aclrow.name.equals(name)) {
                // finish remaining aces
                for (ACE ace : aces) {
                    addACLRow(newaclrows, name, ace);
                }
                // start next round
                name = aclrow.name;
                ACL acl = aclmap.remove(name);
                aces = acl == null ? Collections.<ACE> emptyList()
                        : new LinkedList<ACE>(Arrays.asList(acl.getACEs()));
                aceKeys = new HashSet<String>();
                for (ACE ace : aces) {
                    aceKeys.add(getACEkey(ace));
                }
            }
            if (!aceKeys.contains(getACLrowKey(aclrow))) {
                // no match, keep the aclrow info instead of the ace
                newaclrows.add(new ACLRow(newaclrows.size(), name,
                        aclrow.grant, aclrow.permission, aclrow.user,
                        aclrow.group));
            }
        }
        // finish remaining aces for last acl done
        for (ACE ace : aces) {
            addACLRow(newaclrows, name, ace);
        }
        // do non-done acls
        for (ACL acl : aclmap.values()) {
            name = acl.getName();
            for (ACE ace : acl.getACEs()) {
                addACLRow(newaclrows, name, ace);
            }
        }
        ACLRow[] array = new ACLRow[newaclrows.size()];
        return newaclrows.toArray(array);
    }

    /** Key to distinguish ACEs */
    protected static String getACEkey(ACE ace) {
        // TODO separate user/group
        return ace.getUsername() + '|' + ace.getPermission();
    }

    /** Key to distinguish ACLRows */
    protected static String getACLrowKey(ACLRow aclrow) {
        // TODO separate user/group
        String user = aclrow.user;
        if (user == null) {
            user = aclrow.group;
        }
        return user + '|' + aclrow.permission;
    }

    protected static void addACLRow(List<ACLRow> aclrows, String name, ACE ace) {
        // XXX should prefix user/group
        String user = ace.getUsername();
        if (user == null) {
            // JCR implementation logs null and skips it
            return;
        }
        String group = null; // XXX all in user for now
        aclrows.add(new ACLRow(aclrows.size(), name, ace.isGranted(),
                ace.getPermission(), user, group));
    }

    protected ACL getInheritedACLs(Document doc) throws DocumentException {
        doc = doc.getParent();
        ACL merged = null;
        while (doc != null) {
            ACP acp = getACP(doc);
            if (acp != null) {
                ACL acl = acp.getMergedACLs(ACL.INHERITED_ACL);
                if (merged == null) {
                    merged = acl;
                } else {
                    merged.addAll(acl);
                }
                if (acp.getAccess(SecurityConstants.EVERYONE,
                        SecurityConstants.EVERYTHING) == Access.DENY) {
                    break;
                }
            }
            doc = doc.getParent();
        }
        return merged;
    }

    @Override
    public Map<String, String> getBinaryFulltext(Serializable id) throws DocumentException {
        try {
            return session.getBinaryFulltext(id);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

}
