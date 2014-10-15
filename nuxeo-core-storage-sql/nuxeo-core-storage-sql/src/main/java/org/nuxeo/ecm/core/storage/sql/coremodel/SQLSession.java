/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.Comparator;
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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ConcurrentUpdateDocumentException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.core.storage.ConcurrentUpdateStorageException;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageBlob;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.binary.Binary;
import org.nuxeo.ecm.core.storage.lock.LockException;
import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocumentVersion.VersionNotModifiableException;
import org.nuxeo.runtime.api.Framework;
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

    public static final String BLOB_NAME = "name";

    public static final String BLOB_MIME_TYPE = "mime-type";

    public static final String BLOB_ENCODING = "encoding";

    public static final String BLOB_DIGEST = "digest";

    public static final String BLOB_LENGTH = "length";

    public static final String BLOB_DATA = "data";

    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    public static final String DC_ISSUED = "dc:issued";

    public static final String RELATED_TEXT_RESOURCES = "relatedtextresources";

    public static final String RELATED_TEXT_ID = "relatedtextid";

    public static final String RELATED_TEXT = "relatedtext";

    protected static final Set<String> VERSION_WRITABLE_PROPS = new HashSet<String>(
            Arrays.asList( //
                    Model.FULLTEXT_JOBID_PROP, //
                    Model.FULLTEXT_BINARYTEXT_PROP, //
                    Model.MISC_LIFECYCLE_STATE_PROP, //
                    Model.LOCK_OWNER_PROP, //
                    Model.LOCK_CREATED_PROP, //
                    DC_ISSUED, //
                    RELATED_TEXT_RESOURCES, //
                    RELATED_TEXT_ID, //
                    RELATED_TEXT //
            ));

    private final Repository repository;

    private final org.nuxeo.ecm.core.storage.sql.Session session;

    private Document root;

    private final String sessionId;

    public SQLSession(org.nuxeo.ecm.core.storage.sql.Session session,
            Repository repository, String sessionId) throws DocumentException {
        this.session = session;
        this.repository = repository;
        Node rootNode;
        try {
            rootNode = session.getRootNode();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        this.sessionId = sessionId;
        root = newDocument(rootNode);
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
        return new SQLDocumentLive(null, null, this, true);
    }

    @Override
    public void close() {
        root = null;
        try {
            session.close();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
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
    public boolean isLive() {
        // session can become non-live behind our back
        // through ConnectionAwareXAResource that closes
        // all handles (sessions) at tx end() time
        return session != null && session.isLive();
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        return session.isStateSharedByAllThreadSessions();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getRepositoryName() {
        return repository.getName();
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
            throw new NoSuchDocumentException(path);
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
                versionableId = targetNode.getSimpleProperty(
                        Model.VERSION_VERSIONABLE_PROP).getValue();
            } else if (doc.isProxy()) {
                // copy the proxy
                targetId = targetNode.getSimpleProperty(
                        Model.PROXY_TARGET_PROP).getValue();
                versionableId = targetNode.getSimpleProperty(
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
            @SuppressWarnings("deprecation")
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

    protected static final Pattern ORDER_BY_PATH_ASC = Pattern.compile(
            "(.*)\\s+ORDER\\s+BY\\s+" + NXQL.ECM_PATH + "\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected static final Pattern ORDER_BY_PATH_DESC = Pattern.compile(
            "(.*)\\s+ORDER\\s+BY\\s+" + NXQL.ECM_PATH + "\\s+DESC\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public DocumentModelList query(String query, String queryType,
            QueryFilter queryFilter, long countUpTo) throws QueryException {
        try {
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
            PartialList<Serializable> pl = session.query(query,
                    queryType, queryFilter, countUpTo);
            List<Serializable> ids = pl.list;

            // get Documents in bulk
            List<Document> docs;
            try {
                docs = getDocumentsById(ids);
            } catch (DocumentException e) {
                log.error("Could not fetch documents for ids: " + ids, e);
                docs = Collections.emptyList();
            }

            // build DocumentModels from Documents
            String[] schemas = { "common" };
            List<DocumentModel> list = new ArrayList<DocumentModel>(ids.size());
            for (Document doc : docs) {
                try {
                    list.add(DocumentModelFactory.createDocumentModel(doc, schemas));
                } catch (DocumentException e) {
                    log.error("Could not create document model for doc: " + doc, e);
                }
            }

            // order / limit
            if (orderByPath != null) {
                Collections.sort(list,
                        new PathComparator(orderByPath.booleanValue()));
            }
            if (limit != 0) {
                // do limit/offset by hand
                int size = list.size();
                list.subList(0, (int) (offset > size ? size : offset)).clear();
                size = list.size();
                if (limit < size) {
                    list.subList((int) limit, size).clear();
                }
            }
            return new DocumentModelListImpl(list, pl.totalSize);
        } catch (StorageException | QueryParseException e) {
            throw new QueryException(e.getMessage() + ": " + query, e);
        }
    }

    public static class PathComparator implements Comparator<DocumentModel> {

        private final int sign;

        public PathComparator(boolean asc) {
            this.sign = asc ? 1 : -1;
        }

        @Override
        public int compare(DocumentModel doc1, DocumentModel doc2) {
            String p1 = doc1.getPathAsString();
            String p2 = doc2.getPathAsString();
            if (p1 == null && p2 == null) {
                return sign * doc1.getId().compareTo(doc2.getId());
            } else if (p1 == null) {
                return sign;
            } else if (p2 == null) {
                return -1 * sign;
            }
            return sign * p1.compareTo(p2);
        }
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object[] params) throws QueryException {
        try {
            return session.queryAndFetch(query, queryType, queryFilter, params);
        } catch (StorageException e) {
            throw new QueryException(e.getMessage(), e);
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
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        DocumentType type = schemaManager.getDocumentType(typeName);
        if (type == null) {
            throw new DocumentException("Unknown document type: " + typeName);
        }

        if (node.isProxy()) {
            // proxy seen as a normal document
            Document proxy = new SQLDocumentLive(node, type, this, false);
            Document target = newDocument(targetNode, readonly);
            return new SQLDocumentProxy(proxy, target);
        } else if (node.isVersion()) {
            return new SQLDocumentVersion(node, type, this, readonly);
        } else {
            return new SQLDocumentLive(node, type, this, false);
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
            throw new NoSuchDocumentException(name);
        }
        return doc;
    }

    protected Node getChildProperty(Node node, String name, String typeName)
            throws StorageException {
        Node childNode = session.getChildNode(node, name, true);
        if (childNode == null) {
            // create the needed complex property immediately
            childNode = session.addChildNode(node, name, null, typeName, true);
        }
        return childNode;
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
        } catch (LockException e) {
            throw new DocumentException(e);
        }
    }

    protected Lock setLock(Node node, Lock lock) throws DocumentException {
        try {
            return session.setLock(node.getId(), lock);
        } catch (LockException e) {
            throw new DocumentException(e);
        }
    }

    protected Lock removeLock(Node node, String owner) throws DocumentException {
        try {
            return session.removeLock(node.getId(), owner, false);
        } catch (LockException e) {
            throw new DocumentException(e);
        }
    }

    /*
     * ----- property helpers -----
     */

    /**
     * Recursively reads a complex property from a node.
     *
     * @since 5.9.4
     */
    protected void readComplexProperty(ComplexProperty complexProperty, Node node)
            throws PropertyException {
        if (complexProperty instanceof BlobProperty) {
            try {
                StorageBlob value = readBlob(node);
                complexProperty.init(value);
                return;
            } catch (StorageException e) {
                throw new PropertyException("Property: "
                        + complexProperty.getName(), e);
            }
        }
        for (Property property : complexProperty) {
            String name = property.getField().getName().getPrefixedName();
            Type type = property.getType();
            try {
                if (type.isSimpleType()) {
                    // simple property
                    Serializable value = node.getSimpleProperty(name).getValue();
                    if (value instanceof Delta) {
                        value = ((Delta) value).getFullValue();
                    }
                    property.init(value);
                } else if (type.isListType()) {
                    ListType listType = (ListType) type;
                    if (listType.getFieldType().isSimpleType()) {
                        // array
                        Serializable[] value = node.getCollectionProperty(name).getValue();
                        property.init(value);
                    } else {
                        // complex list
                        List<Node> childNodes;
                        try {
                            childNodes = getComplexList(node, name);
                        } catch (DocumentException e) {
                            throw new PropertyException("Property: " + name, e);
                        }
                        Field listField = listType.getField();
                        ArrayList<Serializable> value = new ArrayList<Serializable>(
                                childNodes.size());
                        for (Node childNode : childNodes) {
                            ComplexProperty p = (ComplexProperty) complexProperty.getRoot().createProperty(
                                    property, listField, 0);
                            readComplexProperty(p, childNode);
                            value.add(p.getValue());
                        }
                        property.init(value);
                    }
                } else {
                    // complex property
                    Node childNode = getChildProperty(node, name,
                            type.getName());
                    readComplexProperty((ComplexProperty) property, childNode);
                    ((ComplexProperty) property).removePhantomFlag();
                }
            } catch (StorageException e) {
                throw new PropertyException("Property: " + name, e);
            }
        }
    }

    /**
     * Recursively reads prefetched properties.
     *
     * @since 5.9.4
     */
    public Map<String, Serializable> readPrefetch(Node node,
            ComplexType complexType, Set<String> xpaths)
            throws PropertyException {
        Map<String, Serializable> prefetch = new HashMap<String, Serializable>();
        readPrefetch(node, complexType, xpaths, null, null, prefetch);
        return prefetch;
    }

    protected void readPrefetch(Node node, ComplexType complexType,
            Set<String> xpaths, String xpathGeneric, String xpath,
            Map<String, Serializable> prefetch) throws PropertyException {
        if (TypeConstants.isContentType(complexType)) {
            if (!xpaths.contains(xpathGeneric)) {
                return;
            }
            try {
                StorageBlob value = readBlob(node);
                prefetch.put(xpath, value);
                return;
            } catch (StorageException e) {
                throw new PropertyException("Property: " + xpath, e);
            }
        }
        for (Field field : complexType.getFields()) {
            String name = field.getName().getPrefixedName();
            Type type = field.getType();
            String xpg = xpathGeneric == null ? name : xpathGeneric + '/' + name;
            String xp = xpath == null ? name : xpath + '/' + name;
            try {
                if (type.isSimpleType()) {
                    // simple property
                    if (!xpaths.contains(xpg)) {
                        continue;
                    }
                    Serializable value = node.getSimpleProperty(name).getValue();
                    prefetch.put(xp, value);
                } else if (type.isListType()) {
                    ListType listType = (ListType) type;
                    if (listType.getFieldType().isSimpleType()) {
                        // array
                        if (!xpaths.contains(xpg)) {
                            continue;
                        }
                        Serializable[] value = node.getCollectionProperty(name).getValue();
                        prefetch.put(xp, value);
                    } else {
                        // complex list
                        List<Node> childNodes;
                        try {
                            childNodes = getComplexList(node, name);
                        } catch (DocumentException e) {
                            throw new PropertyException("Property: " + name, e);
                        }
                        Field listField = listType.getField();
                        xpg += "/*";
                        int n = 0;
                        for (Node childNode : childNodes) {
                            readPrefetch(childNode,
                                    (ComplexType) listField.getType(),
                                    xpaths, xpg, xp + "/" + n++, prefetch);
                        }
                    }
                } else {
                    // complex property
                    Node childNode = getChildProperty(node, name,
                            type.getName());
                    readPrefetch(childNode, (ComplexType) type, xpaths,
                            xpg, xp, prefetch);
                }
            } catch (StorageException e) {
                throw new PropertyException("Property: " + name, e);
            }
        }
    }

    /**
     * Recursively writes a complex property into a node.
     *
     * @since 5.9.4
     */
    protected void writeComplexProperty(ComplexProperty complexProperty,
            Node node, SQLDocument doc) throws PropertyException {
        if (complexProperty instanceof BlobProperty) {
            try {
                writeBlobProperty((BlobProperty) complexProperty, node, doc);
            } catch (StorageException e) {
                throw new PropertyException("Property: "
                        + complexProperty.getName(), e);
            }
            return;
        }
        for (Property property : complexProperty) {
            String name = property.getField().getName().getPrefixedName();
            try {
                if (checkReadOnlyIgnoredWrite(doc, property, node)) {
                    continue;
                }
                Type type = property.getType();
                if (type.isSimpleType()) {
                    // simple property
                    Serializable value = property.getValueForWrite();
                    node.getSimpleProperty(name).setValue(value);
                    if (value instanceof Delta) {
                        value = ((Delta) value).getFullValue();
                        ((ScalarProperty) property).internalSetValue(value);
                    }
                    // TODO VersionNotModifiableException
                } else if (type.isListType()) {
                    ListType listType = (ListType) type;
                    if (listType.getFieldType().isSimpleType()) {
                        // array
                        Serializable value = property.getValueForWrite();
                        if (value instanceof List) {
                            value = ((List<?>) value).toArray(new Object[0]);
                        }
                        node.getCollectionProperty(name).setValue((Object[]) value);
                    } else {
                        // complex list
                        Collection<Property> childProperties = property.getChildren();
                        List<Node> childNodes;
                        try {
                            childNodes = getComplexList(node, name);
                        } catch (DocumentException e) {
                            throw new PropertyException("Property: " + name, e);
                        }

                        int oldSize = childNodes.size();
                        int newSize = childProperties.size();
                        // remove extra list elements
                        if (oldSize > newSize) {
                            for (int i = oldSize - 1; i >= newSize; i--) {
                                try {
                                    removeProperty(childNodes.remove(i));
                                } catch (DocumentException e) {
                                    throw new PropertyException("Property: "
                                            + name + '[' + i + ']', e);
                                }
                            }
                        }
                        // add new list elements
                        if (oldSize < newSize) {
                            for (int i = oldSize; i < newSize; i++) {
                                Node childNode = session.addChildNode(node,
                                        name, Long.valueOf(i),
                                        listType.getFieldType().getName(), true);
                                childNodes.add(childNode);
                            }
                        }

                        // write values
                        int i = 0;
                        for (Property childProperty : childProperties) {
                            Node childNode = childNodes.get(i++);
                            writeComplexProperty(
                                    (ComplexProperty) childProperty, childNode,
                                    doc);
                        }
                    }
                } else {
                    // complex property
                    Node childNode = getChildProperty(node, name,
                            type.getName());
                    writeComplexProperty((ComplexProperty) property, childNode,
                            doc);
                }
            } catch (StorageException e) {
                throw new PropertyException("Property: " + name, e);
            }
        }
    }

    protected StorageBlob readBlob(Node node) throws StorageException {
        Binary binary = (Binary) node.getSimpleProperty(BLOB_DATA).getValue();
        if (binary == null) {
            return null;
        }
        String name = node.getSimpleProperty(BLOB_NAME).getString();
        String mimeType = node.getSimpleProperty(BLOB_MIME_TYPE).getString();
        String encoding = node.getSimpleProperty(BLOB_ENCODING).getString();
        String digest = node.getSimpleProperty(BLOB_DIGEST).getString();
        Long length = node.getSimpleProperty(BLOB_LENGTH).getLong();
        return new StorageBlob(binary, name, mimeType, encoding, digest,
                length.longValue());
    }

    protected void writeBlobProperty(BlobProperty blobProperty, Node node,
            SQLDocument doc) throws StorageException, PropertyException {
        Serializable value = blobProperty.getValueForWrite();
        Binary binary;
        String name;
        String mimeType;
        String encoding;
        String digest;
        Long length;
        if (value == null) {
            binary = null;
            name = null;
            mimeType = null;
            encoding = null;
            digest = null;
            length = null;
        } else {
            if (!(value instanceof Blob)) {
                throw new PropertyException("Setting a non-Blob value: "
                        + value);
            }
            Blob blob = (Blob) value;
            try {
                binary = getBinary(blob);
            } catch (DocumentException e) {
                throw new PropertyException("Cannot get binary", e);
            }
            name = blob.getFilename();
            mimeType = blob.getMimeType();
            if (mimeType == null) {
                mimeType = APPLICATION_OCTET_STREAM;
            }
            encoding = blob.getEncoding();
            digest = blob.getDigest();
            // use binary length now that we know it,
            // the blob may not have known it (streaming blobs)
            length = Long.valueOf(binary.getLength());
        }

        node.getSimpleProperty(BLOB_DATA).setValue(binary);
        node.getSimpleProperty(BLOB_NAME).setValue(name);
        node.getSimpleProperty(BLOB_MIME_TYPE).setValue(mimeType);
        node.getSimpleProperty(BLOB_ENCODING).setValue(encoding);
        node.getSimpleProperty(BLOB_DIGEST).setValue(digest);
        node.getSimpleProperty(BLOB_LENGTH).setValue(length);
    }

    protected static boolean isVersionWritableProperty(String name) {
        return VERSION_WRITABLE_PROPS.contains(name) //
                || name.startsWith(Model.FULLTEXT_BINARYTEXT_PROP) //
                || name.startsWith(Model.FULLTEXT_SIMPLETEXT_PROP);
    }

    /**
     * Checks for ignored writes. May throw.
     *
     * @since 5.9.4
     */
    protected boolean checkReadOnlyIgnoredWrite(SQLDocument doc,
            Property property, Node node) throws PropertyException,
            StorageException {
        String name = property.getField().getName().getPrefixedName();
        if (!doc.isReadOnly() || isVersionWritableProperty(name)) {
            // do write
            return false;
        }
        if (!doc.isVersion()) {
            throw new PropertyException("Cannot write readonly property: "
                    + name);
        }
        if (!name.startsWith("dc:")) {
            throw new VersionNotModifiableException(
                    "Cannot set property on a version: " + name);
        }
        // ignore if value is unchanged (only for dublincore)
        // dublincore contains only scalars and arrays
        Serializable value = property.getValueForWrite();
        Serializable oldValue;
        if (property.getType().isSimpleType()) {
            oldValue = node.getSimpleProperty(name).getValue();
        } else {
            oldValue = node.getCollectionProperty(name).getValue();
        }
        if (!ArrayUtils.isEquals(value, oldValue)) {
            // do write
            return false;
        }
        // ignore attempt to write identical value
        return true;
    }

    /**
     * @param blob
     * @return
     * @throws DocumentException
     */
    public Binary getBinary(Blob blob) throws DocumentException {
        if (blob instanceof StorageBlob) {
            return ((StorageBlob) blob).getBinary();
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
    public void setACP(Document doc, ACP acp, boolean overwrite)
            throws SecurityException {
        if (!overwrite && acp == null) {
            return;
        }
        try {
            Node node = ((SQLDocument) doc).getNode();
            ACLRow[] aclrows;
            if (overwrite) {
                aclrows = acp == null ? null : acpToAclRows(acp);
            } else {
                aclrows = (ACLRow[]) node.getCollectionProperty(Model.ACL_PROP).getValue();
                aclrows = updateAclRows(aclrows, acp);
            }
            node.getCollectionProperty(Model.ACL_PROP).setValue(aclrows);
            session.requireReadAclsUpdate();
        } catch (StorageException e) {
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

    protected ACP getACP(Document doc) throws SecurityException {
        try {
            Node node = ((SQLDocument) doc).getNode();
            ACLRow[] aclrows = (ACLRow[]) node.getCollectionProperty(
                    Model.ACL_PROP).getValue();
            return aclRowsToACP(aclrows);
        } catch (StorageException e) {
            throw new SecurityException(e.getMessage(), e);
        }
    }

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
    public Map<String, String> getBinaryFulltext(String id)
            throws DocumentException {
        try {
            return session.getBinaryFulltext(idFromString(id));
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

}
