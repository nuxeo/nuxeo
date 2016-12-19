/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.sql.ACLRow;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.runtime.api.Framework;

/**
 * This class is the bridge between the Nuxeo SPI Session and the actual low-level implementation of the SQL storage
 * session.
 *
 * @author Florent Guillaume
 */
public class SQLSession implements Session {

    protected final Log log = LogFactory.getLog(SQLSession.class);

    /**
     * Framework property to control whether negative ACLs (deny) are allowed.
     *
     * @since 6.0
     */
    public static final String ALLOW_NEGATIVE_ACL_PROPERTY = "nuxeo.security.allowNegativeACL";

    /**
     * Framework property to disabled free-name collision detection for copy. This is useful when constraints have been
     * added to the database to detect collisions at the database level and raise a ConcurrentUpdateException, thus
     * letting the high-level application deal with collisions.
     *
     * @since 7.3
     */
    public static final String COPY_FINDFREENAME_DISABLED_PROP = "nuxeo.vcs.copy.findFreeName.disabled";

    private final Repository repository;

    private final org.nuxeo.ecm.core.storage.sql.Session session;

    private Document root;

    private final boolean negativeAclAllowed;

    private final boolean copyFindFreeNameDisabled;

    public SQLSession(org.nuxeo.ecm.core.storage.sql.Session session, Repository repository) {
        this.session = session;
        this.repository = repository;
        Node rootNode = session.getRootNode();
        root = newDocument(rootNode);
        negativeAclAllowed = Framework.isBooleanPropertyTrue(ALLOW_NEGATIVE_ACL_PROPERTY);
        copyFindFreeNameDisabled = Framework.isBooleanPropertyTrue(COPY_FINDFREENAME_DISABLED_PROP);
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
    public void save() {
        session.save();
    }

    @Override
    public boolean isLive() {
        // session can become non-live behind our back
        // through ConnectionAwareXAResource that closes
        // all handles (sessions) at tx end() time
        return session != null && session.isLive();
    }

    @Override
    public String getRepositoryName() {
        return repository.getName();
    }

    protected String idToString(Serializable id) {
        return session.getModel().idToString(id);
    }

    protected Serializable idFromString(String id) {
        return session.getModel().idFromString(id);
    }

    @Override
    public ScrollResult scroll(String query, int batchSize, int keepAliveSeconds) {
        return session.scroll(query, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        return session.scroll(scrollId);
    }

    @Override
    public Document getDocumentByUUID(String uuid) throws DocumentNotFoundException {
        /*
         * Document ids coming from higher level have been turned into strings (by {@link SQLDocument#getUUID}) but the
         * backend may actually expect them to be Longs (for database-generated integer ids).
         */
        Document doc = getDocumentById(idFromString(uuid));
        if (doc == null) {
            // required by callers such as AbstractSession.exists
            throw new DocumentNotFoundException(uuid);
        }
        return doc;
    }

    @Override
    public Document resolvePath(String path) throws DocumentNotFoundException {
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        Node node = session.getNodeByPath(path, session.getRootNode());
        Document doc = newDocument(node);
        if (doc == null) {
            throw new DocumentNotFoundException(path);
        }
        return doc;
    }

    protected void orderBefore(Node node, Node src, Node dest) {
        session.orderBefore(node, src, dest);
    }

    @Override
    public Document move(Document source, Document parent, String name) {
        assert source instanceof SQLDocument;
        assert parent instanceof SQLDocument;
        if (name == null) {
            name = source.getName();
        }
        Node result = session.move(((SQLDocument) source).getNode(), ((SQLDocument) parent).getNode(), name);
        return newDocument(result);
    }

    private static final Pattern dotDigitsPattern = Pattern.compile("(.*)\\.[0-9]+$");

    protected String findFreeName(Node parentNode, String name) {
        if (session.hasChildNode(parentNode, name, false)) {
            Matcher m = dotDigitsPattern.matcher(name);
            if (m.matches()) {
                // remove trailing dot and digits
                name = m.group(1);
            }
            // add dot + unique digits
            name += "." + System.nanoTime();
        }
        return name;
    }

    @Override
    public Document copy(Document source, Document parent, String name) {
        assert source instanceof SQLDocument;
        assert parent instanceof SQLDocument;
        if (name == null) {
            name = source.getName();
        }
        Node parentNode = ((SQLDocument) parent).getNode();
        if (!copyFindFreeNameDisabled) {
            name = findFreeName(parentNode, name);
        }
        Node copy = session.copy(((SQLDocument) source).getNode(), parentNode, name);
        return newDocument(copy);
    }

    @Override
    public Document getVersion(String versionableId, VersionModel versionModel) {
        Serializable vid = idFromString(versionableId);
        Node versionNode = session.getVersionByLabel(vid, versionModel.getLabel());
        if (versionNode == null) {
            return null;
        }
        versionModel.setDescription(versionNode.getSimpleProperty(Model.VERSION_DESCRIPTION_PROP).getString());
        versionModel.setCreated((Calendar) versionNode.getSimpleProperty(Model.VERSION_CREATED_PROP).getValue());
        return newDocument(versionNode);
    }

    @Override
    public Document createProxy(Document doc, Document folder) {
        Node folderNode = ((SQLDocument) folder).getNode();
        Node targetNode = ((SQLDocument) doc).getNode();
        Serializable targetId = targetNode.getId();
        Serializable versionableId;
        if (doc.isVersion()) {
            versionableId = targetNode.getSimpleProperty(Model.VERSION_VERSIONABLE_PROP).getValue();
        } else if (doc.isProxy()) {
            // copy the proxy
            targetId = targetNode.getSimpleProperty(Model.PROXY_TARGET_PROP).getValue();
            versionableId = targetNode.getSimpleProperty(Model.PROXY_VERSIONABLE_PROP).getValue();
        } else {
            // working copy (live document)
            versionableId = targetId;
        }
        String name = findFreeName(folderNode, doc.getName());
        Node proxy = session.addProxy(targetId, versionableId, folderNode, name, null);
        return newDocument(proxy);
    }

    @Override
    public List<Document> getProxies(Document document, Document parent) {
        List<Node> proxyNodes = session.getProxies(((SQLDocument) document).getNode(),
                parent == null ? null : ((SQLDocument) parent).getNode());
        List<Document> proxies = new ArrayList<>(proxyNodes.size());
        for (Node proxyNode : proxyNodes) {
            proxies.add(newDocument(proxyNode));
        }
        return proxies;
    }

    @Override
    public void setProxyTarget(Document proxy, Document target) {
        Node proxyNode = ((SQLDocument) proxy).getNode();
        Serializable targetId = idFromString(target.getUUID());
        session.setProxyTarget(proxyNode, targetId);
    }

    // returned document is r/w even if a version or a proxy, so that normal
    // props can be set
    @Override
    public Document importDocument(String uuid, Document parent, String name, String typeName,
            Map<String, Serializable> properties) {
        boolean isProxy = typeName.equals(Model.PROXY_TYPE);
        Map<String, Serializable> props = new HashMap<>();
        Long pos = null; // TODO pos
        if (!isProxy) {
            // version & live document
            props.put(Model.MISC_LIFECYCLE_POLICY_PROP, properties.get(CoreSession.IMPORT_LIFECYCLE_POLICY));
            props.put(Model.MISC_LIFECYCLE_STATE_PROP, properties.get(CoreSession.IMPORT_LIFECYCLE_STATE));

            Serializable importLockOwnerProp = properties.get(CoreSession.IMPORT_LOCK_OWNER);
            if (importLockOwnerProp != null) {
                props.put(Model.LOCK_OWNER_PROP, importLockOwnerProp);
            }
            Serializable importLockCreatedProp = properties.get(CoreSession.IMPORT_LOCK_CREATED);
            if (importLockCreatedProp != null) {
                props.put(Model.LOCK_CREATED_PROP, importLockCreatedProp);
            }

            props.put(Model.MAIN_MAJOR_VERSION_PROP, properties.get(CoreSession.IMPORT_VERSION_MAJOR));
            props.put(Model.MAIN_MINOR_VERSION_PROP, properties.get(CoreSession.IMPORT_VERSION_MINOR));
            props.put(Model.MAIN_IS_VERSION_PROP, properties.get(CoreSession.IMPORT_IS_VERSION));
        }
        Node parentNode;
        if (parent == null) {
            // version
            parentNode = null;
            props.put(Model.VERSION_VERSIONABLE_PROP,
                    idFromString((String) properties.get(CoreSession.IMPORT_VERSION_VERSIONABLE_ID)));
            props.put(Model.VERSION_CREATED_PROP, properties.get(CoreSession.IMPORT_VERSION_CREATED));
            props.put(Model.VERSION_LABEL_PROP, properties.get(CoreSession.IMPORT_VERSION_LABEL));
            props.put(Model.VERSION_DESCRIPTION_PROP, properties.get(CoreSession.IMPORT_VERSION_DESCRIPTION));
            props.put(Model.VERSION_IS_LATEST_PROP, properties.get(CoreSession.IMPORT_VERSION_IS_LATEST));
            props.put(Model.VERSION_IS_LATEST_MAJOR_PROP, properties.get(CoreSession.IMPORT_VERSION_IS_LATEST_MAJOR));
        } else {
            parentNode = ((SQLDocument) parent).getNode();
            if (isProxy) {
                // proxy
                props.put(Model.PROXY_TARGET_PROP,
                        idFromString((String) properties.get(CoreSession.IMPORT_PROXY_TARGET_ID)));
                props.put(Model.PROXY_VERSIONABLE_PROP,
                        idFromString((String) properties.get(CoreSession.IMPORT_PROXY_VERSIONABLE_ID)));
            } else {
                // live document
                props.put(Model.MAIN_BASE_VERSION_PROP,
                        idFromString((String) properties.get(CoreSession.IMPORT_BASE_VERSION_ID)));
                props.put(Model.MAIN_CHECKED_IN_PROP, properties.get(CoreSession.IMPORT_CHECKED_IN));
            }
        }
        return importChild(uuid, parentNode, name, pos, typeName, props);
    }

    protected static final Pattern ORDER_BY_PATH_ASC = Pattern.compile(
            "(.*)\\s+ORDER\\s+BY\\s+" + NXQL.ECM_PATH + "\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    protected static final Pattern ORDER_BY_PATH_DESC = Pattern.compile(
            "(.*)\\s+ORDER\\s+BY\\s+" + NXQL.ECM_PATH + "\\s+DESC\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public PartialList<Document> query(String query, String queryType, QueryFilter queryFilter, long countUpTo) {
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
        PartialList<Serializable> pl = session.query(query, queryType, queryFilter, countUpTo);

        // get Documents in bulk, returns a newly-allocated ArrayList
        List<Document> list = getDocumentsById(pl.list);

        // order / limit
        if (orderByPath != null) {
            Collections.sort(list, new PathComparator(orderByPath.booleanValue()));
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
        return new PartialList<>(list, pl.totalSize);
    }

    public static class PathComparator implements Comparator<Document> {

        private final int sign;

        public PathComparator(boolean asc) {
            this.sign = asc ? 1 : -1;
        }

        @Override
        public int compare(Document doc1, Document doc2) {
            String p1 = doc1.getPath();
            String p2 = doc2.getPath();
            if (p1 == null && p2 == null) {
                return sign * doc1.getUUID().compareTo(doc2.getUUID());
            } else if (p1 == null) {
                return sign;
            } else if (p2 == null) {
                return -1 * sign;
            }
            return sign * p1.compareTo(p2);
        }
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object[] params) {
        return session.queryAndFetch(query, queryType, queryFilter, distinctDocuments, params);
    }

    /*
     * ----- called by SQLDocument -----
     */

    private Document newDocument(Node node) {
        return newDocument(node, true);
    }

    // "readonly" meaningful for proxies and versions, used for import
    private Document newDocument(Node node, boolean readonly) {
        if (node == null) {
            // root's parent
            return null;
        }

        Node targetNode = null;
        String typeName = node.getPrimaryType();
        if (node.isProxy()) {
            Serializable targetId = node.getSimpleProperty(Model.PROXY_TARGET_PROP).getValue();
            if (targetId == null) {
                throw new DocumentNotFoundException("Proxy has null target");
            }
            targetNode = session.getNodeById(targetId);
            typeName = targetNode.getPrimaryType();
        }
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        DocumentType type = schemaManager.getDocumentType(typeName);
        if (type == null) {
            throw new DocumentNotFoundException("Unknown document type: " + typeName);
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
    protected Document getDocumentById(Serializable id) {
        Node node = session.getNodeById(id);
        return node == null ? null : newDocument(node);
    }

    // called by SQLQueryResult iterator
    protected List<Document> getDocumentsById(List<Serializable> ids) {
        List<Document> docs = new ArrayList<>(ids.size());
        List<Node> nodes = session.getNodesByIds(ids);
        for (int index = 0; index < ids.size(); ++index) {
            Node eachNode = nodes.get(index);
            if (eachNode == null) {
                if (log.isTraceEnabled()) {
                    Serializable id = ids.get(index);
                    log.trace("Cannot fetch document with id: " + id, new Throwable("debug stack trace"));
                }
                continue;
            }
            Document doc;
            try {
                doc = newDocument(eachNode);
            } catch (DocumentNotFoundException e) {
                // unknown type in db, ignore
                continue;
            }
            docs.add(doc);
        }
        return docs;
    }

    protected Document getParent(Node node) {
        return newDocument(session.getParentNode(node));
    }

    protected String getPath(Node node) {
        return session.getPath(node);
    }

    protected Document getChild(Node node, String name) throws DocumentNotFoundException {
        Node childNode = session.getChildNode(node, name, false);
        Document doc = newDocument(childNode);
        if (doc == null) {
            throw new DocumentNotFoundException(name);
        }
        return doc;
    }

    protected Node getChildProperty(Node node, String name, String typeName) {
        // all complex property children have already been created by SessionImpl.addChildNode or
        // SessionImpl.addMixinType
        // if one is missing here, it means that it was concurrently deleted and we're only now finding out
        // or that a schema change was done and we now expect a new child
        // return null in that case
        return session.getChildNode(node, name, true);
    }

    protected Node getChildPropertyForWrite(Node node, String name, String typeName) {
        Node childNode = getChildProperty(node, name, typeName);
        if (childNode == null) {
            // create the needed complex property immediately
            childNode = session.addChildNode(node, name, null, typeName, true);
        }
        return childNode;
    }

    protected List<Document> getChildren(Node node) {
        List<Node> nodes = session.getChildren(node, null, false);
        List<Document> children = new ArrayList<>(nodes.size());
        for (Node n : nodes) {
            try {
                children.add(newDocument(n));
            } catch (DocumentNotFoundException e) {
                // ignore error retrieving one of the children
                continue;
            }
        }
        return children;
    }

    protected boolean hasChild(Node node, String name) {
        return session.hasChildNode(node, name, false);
    }

    protected boolean hasChildren(Node node) {
        return session.hasChildren(node, false);
    }

    protected Document addChild(Node parent, String name, Long pos, String typeName) {
        return newDocument(session.addChildNode(parent, name, pos, typeName, false));
    }

    protected Node addChildProperty(Node parent, String name, Long pos, String typeName) {
        return session.addChildNode(parent, name, pos, typeName, true);
    }

    protected Document importChild(String uuid, Node parent, String name, Long pos, String typeName,
            Map<String, Serializable> props) {
        Serializable id = idFromString(uuid);
        Node node = session.addChildNode(id, parent, name, pos, typeName, false);
        for (Entry<String, Serializable> entry : props.entrySet()) {
            node.setSimpleProperty(entry.getKey(), entry.getValue());
        }
        return newDocument(node, false); // not readonly
    }

    protected boolean addMixinType(Node node, String mixin) {
        return session.addMixinType(node, mixin);
    }

    protected boolean removeMixinType(Node node, String mixin) {
        return session.removeMixinType(node, mixin);
    }

    protected List<Node> getComplexList(Node node, String name) {
        List<Node> nodes = session.getChildren(node, name, true);
        return nodes;
    }

    protected void remove(Node node) {
        session.removeNode(node);
    }

    protected void removeProperty(Node node) {
        session.removePropertyNode(node);
    }

    protected Document checkIn(Node node, String label, String checkinComment) {
        Node versionNode = session.checkIn(node, label, checkinComment);
        return versionNode == null ? null : newDocument(versionNode);
    }

    protected void checkOut(Node node) {
        session.checkOut(node);
    }

    protected void restore(Node node, Node version) {
        session.restore(node, version);
    }

    protected Document getVersionByLabel(String versionSeriesId, String label) {
        Serializable vid = idFromString(versionSeriesId);
        Node versionNode = session.getVersionByLabel(vid, label);
        return versionNode == null ? null : newDocument(versionNode);
    }

    protected List<Document> getVersions(String versionSeriesId) {
        Serializable vid = idFromString(versionSeriesId);
        List<Node> versionNodes = session.getVersions(vid);
        List<Document> versions = new ArrayList<>(versionNodes.size());
        for (Node versionNode : versionNodes) {
            versions.add(newDocument(versionNode));
        }
        return versions;
    }

    public Document getLastVersion(String versionSeriesId) {
        Serializable vid = idFromString(versionSeriesId);
        Node versionNode = session.getLastVersion(vid);
        if (versionNode == null) {
            return null;
        }
        return newDocument(versionNode);
    }

    protected Node getNodeById(Serializable id) {
        return session.getNodeById(id);
    }

    @Override
    public LockManager getLockManager() {
        return session.getLockManager();
    }

    @Override
    public boolean isNegativeAclAllowed() {
        return negativeAclAllowed;
    }

    @Override
    public void setACP(Document doc, ACP acp, boolean overwrite) {
        if (!overwrite && acp == null) {
            return;
        }
        checkNegativeAcl(acp);
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
    }

    protected void checkNegativeAcl(ACP acp) {
        if (negativeAclAllowed) {
            return;
        }
        if (acp == null) {
            return;
        }
        for (ACL acl : acp.getACLs()) {
            if (acl.getName().equals(ACL.INHERITED_ACL)) {
                continue;
            }
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted()) {
                    continue;
                }
                String permission = ace.getPermission();
                if (permission.equals(SecurityConstants.EVERYTHING)
                        && ace.getUsername().equals(SecurityConstants.EVERYONE)) {
                    continue;
                }
                // allow Write, as we're sure it doesn't include Read/Browse
                if (permission.equals(SecurityConstants.WRITE)) {
                    continue;
                }
                throw new IllegalArgumentException("Negative ACL not allowed: " + ace);
            }
        }
    }

    @Override
    public ACP getMergedACP(Document doc) {
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
        if (acp == null || acp.getAccess(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING) != Access.DENY) {
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
    }

    /*
     * ----- internal methods -----
     */

    protected ACP getACP(Document doc) {
        Node node = ((SQLDocument) doc).getNode();
        ACLRow[] aclrows = (ACLRow[]) node.getCollectionProperty(Model.ACL_PROP).getValue();
        return aclRowsToACP(aclrows);
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
            acl.add(ACE.builder(user, aclrow.permission)
                       .isGranted(aclrow.grant)
                       .creator(aclrow.creator)
                       .begin(aclrow.begin)
                       .end(aclrow.end)
                       .build());
        }
        if (acl != null) {
            acp.addACL(acl);
        }
        return acp;
    }

    // unit tested
    protected static ACLRow[] acpToAclRows(ACP acp) {
        List<ACLRow> aclrows = new LinkedList<>();
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
        List<ACLRow> newaclrows = new LinkedList<>();
        Map<String, ACL> aclmap = new HashMap<>();
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
                aces = acl == null ? Collections.<ACE> emptyList() : new LinkedList<>(Arrays.asList(acl.getACEs()));
                aceKeys = new HashSet<>();
                for (ACE ace : aces) {
                    aceKeys.add(getACEkey(ace));
                }
            }
            if (!aceKeys.contains(getACLrowKey(aclrow))) {
                // no match, keep the aclrow info instead of the ace
                newaclrows.add(new ACLRow(newaclrows.size(), name, aclrow.grant, aclrow.permission, aclrow.user,
                        aclrow.group, aclrow.creator, aclrow.begin, aclrow.end, aclrow.status));
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
        aclrows.add(new ACLRow(aclrows.size(), name, ace.isGranted(), ace.getPermission(), user, group,
                ace.getCreator(), ace.getBegin(), ace.getEnd(), ace.getLongStatus()));
    }

    protected ACL getInheritedACLs(Document doc) {
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
                if (acp.getAccess(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING) == Access.DENY) {
                    break;
                }
            }
            doc = doc.getParent();
        }
        return merged;
    }

    @Override
    public Map<String, String> getBinaryFulltext(String id) {
        return session.getBinaryFulltext(idFromString(id));
    }

}
