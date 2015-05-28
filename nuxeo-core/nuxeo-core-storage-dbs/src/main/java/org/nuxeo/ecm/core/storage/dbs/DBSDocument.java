/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import static java.lang.Boolean.TRUE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.BaseDocument;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.lock.AbstractLockManager;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocumentVersion.VersionNotModifiableException;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of a {@link Document} for Document-Based Storage. The document is stored as a JSON-like Map. The keys
 * of the Map are the property names (including special names for system properties), and the values Map are
 * Serializable values, either:
 * <ul>
 * <li>a scalar (String, Long, Double, Boolean, Calendar, Binary),
 * <li>an array of scalars,
 * <li>a List of Maps, recursively,
 * <li>or another Map, recursively.
 * </ul>
 * An ACP value is stored as a list of maps. Each map has a keys for the ACL name and the actual ACL which is a list of
 * ACEs. An ACE is a map having as keys username, permission, and grant.
 *
 * @since 5.9.4
 */
public class DBSDocument extends BaseDocument<State> {

    private static final Long ZERO = Long.valueOf(0);

    public static final String SYSPROP_FULLTEXT_SIMPLE = "fulltextSimple";

    public static final String SYSPROP_FULLTEXT_BINARY = "fulltextBinary";

    public static final String SYSPROP_FULLTEXT_JOBID = "fulltextJobId";

    public static final String KEY_PREFIX = "ecm:";

    public static final String KEY_ID = "ecm:id";

    public static final String KEY_PARENT_ID = "ecm:parentId";

    public static final String KEY_ANCESTOR_IDS = "ecm:ancestorIds";

    public static final String KEY_PRIMARY_TYPE = "ecm:primaryType";

    public static final String KEY_MIXIN_TYPES = "ecm:mixinTypes";

    public static final String KEY_NAME = "ecm:name";

    public static final String KEY_POS = "ecm:pos";

    public static final String KEY_ACP = "ecm:acp";

    public static final String KEY_ACL_NAME = "name";

    public static final String KEY_PATH_INTERNAL = "ecm:__path";

    public static final String KEY_ACL = "acl";

    public static final String KEY_ACE_USER = "user";

    public static final String KEY_ACE_PERMISSION = "perm";

    public static final String KEY_ACE_GRANT = "grant";

    public static final String KEY_READ_ACL = "ecm:racl";

    public static final String KEY_IS_CHECKED_IN = "ecm:isCheckedIn";

    public static final String KEY_IS_VERSION = "ecm:isVersion";

    public static final String KEY_IS_LATEST_VERSION = "ecm:isLatestVersion";

    public static final String KEY_IS_LATEST_MAJOR_VERSION = "ecm:isLatestMajorVersion";

    public static final String KEY_MAJOR_VERSION = "ecm:majorVersion";

    public static final String KEY_MINOR_VERSION = "ecm:minorVersion";

    public static final String KEY_VERSION_SERIES_ID = "ecm:versionSeriesId";

    public static final String KEY_VERSION_CREATED = "ecm:versionCreated";

    public static final String KEY_VERSION_LABEL = "ecm:versionLabel";

    public static final String KEY_VERSION_DESCRIPTION = "ecm:versionDescription";

    public static final String KEY_BASE_VERSION_ID = "ecm:baseVersionId";

    public static final String KEY_IS_PROXY = "ecm:isProxy";

    public static final String KEY_PROXY_TARGET_ID = "ecm:proxyTargetId";

    public static final String KEY_PROXY_VERSION_SERIES_ID = "ecm:proxyVersionSeriesId";

    public static final String KEY_PROXY_IDS = "ecm:proxyIds";

    public static final String KEY_LIFECYCLE_POLICY = "ecm:lifeCyclePolicy";

    public static final String KEY_LIFECYCLE_STATE = "ecm:lifeCycleState";

    public static final String KEY_LOCK_OWNER = "ecm:lockOwner";

    public static final String KEY_LOCK_CREATED = "ecm:lockCreated";

    public static final String KEY_BLOB_NAME = "name";

    public static final String KEY_BLOB_MIME_TYPE = "mime-type";

    public static final String KEY_BLOB_ENCODING = "encoding";

    public static final String KEY_BLOB_DIGEST = "digest";

    public static final String KEY_BLOB_LENGTH = "length";

    public static final String KEY_BLOB_DATA = "data";

    public static final String KEY_FULLTEXT_SIMPLE = "ecm:fulltextSimple";

    public static final String KEY_FULLTEXT_BINARY = "ecm:fulltextBinary";

    public static final String KEY_FULLTEXT_JOBID = "ecm:fulltextJobId";

    public static final String KEY_FULLTEXT_SCORE = "ecm:fulltextScore";

    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    protected final String id;

    protected final DBSDocumentState docState;

    protected final DocumentType type;

    protected final List<Schema> proxySchemas;

    protected final DBSSession session;

    protected boolean readonly;

    protected static final Map<String, String> systemPropNameMap;

    static {
        systemPropNameMap = new HashMap<String, String>();
        systemPropNameMap.put(SYSPROP_FULLTEXT_SIMPLE, KEY_FULLTEXT_SIMPLE);
        systemPropNameMap.put(SYSPROP_FULLTEXT_BINARY, KEY_FULLTEXT_BINARY);
        systemPropNameMap.put(SYSPROP_FULLTEXT_JOBID, KEY_FULLTEXT_JOBID);
    }

    public DBSDocument(DBSDocumentState docState, DocumentType type, DBSSession session, boolean readonly) {
        // no state for NullDocument (parent of placeless children)
        this.id = docState == null ? null : (String) docState.get(KEY_ID);
        this.docState = docState;
        this.type = type;
        this.session = session;
        if (docState != null && isProxy()) {
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            proxySchemas = schemaManager.getProxySchemas(type.getName());
        } else {
            proxySchemas = null;
        }
        this.readonly = readonly;
    }

    @Override
    public DocumentType getType() {
        return type;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public String getRepositoryName() {
        return session.getRepositoryName();
    }

    @Override
    protected List<Schema> getProxySchemas() {
        return proxySchemas;
    }

    @Override
    public String getUUID() {
        return id;
    }

    @Override
    public String getName() {
        return docState.getName();
    }

    @Override
    public Long getPos() {
        return (Long) docState.get(KEY_POS);
    }

    @Override
    public Document getParent() throws DocumentException {
        if (isVersion()) {
            return session.getDocument(getVersionSeriesId()).getParent();
        }
        String parentId = docState.getParentId();
        return parentId == null ? null : session.getDocument(parentId);
    }

    @Override
    public boolean isProxy() {
        return TRUE.equals(docState.get(KEY_IS_PROXY));
    }

    @Override
    public boolean isVersion() {
        return TRUE.equals(docState.get(KEY_IS_VERSION));
    }

    @Override
    public String getPath() throws DocumentException {
        if (isVersion()) {
            return session.getDocument(getVersionSeriesId()).getPath();
        }
        String name = getName();
        Document doc = getParent();
        if (doc == null) {
            if ("".equals(name)) {
                return "/"; // root
            } else {
                return name; // placeless, no slash
            }
        }
        LinkedList<String> list = new LinkedList<String>();
        list.addFirst(name);
        while (doc != null) {
            list.addFirst(doc.getName());
            doc = doc.getParent();
        }
        return StringUtils.join(list, '/');
    }

    @Override
    public Document getChild(String name) throws DocumentException {
        return session.getChild(id, name);
    }

    @Override
    public List<Document> getChildren() throws DocumentException {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        return session.getChildren(id);
    }

    @Override
    public List<String> getChildrenIds() throws DocumentException {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        return session.getChildrenIds(id);
    }

    @Override
    public boolean hasChild(String name) throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChild(id, name);
    }

    @Override
    public boolean hasChildren() throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChildren(id);
    }

    @Override
    public Document addChild(String name, String typeName) throws DocumentException {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        return session.createChild(null, id, name, null, typeName);
    }

    @Override
    public void orderBefore(String src, String dest) throws DocumentException {
        Document srcDoc = getChild(src);
        if (srcDoc == null) {
            throw new DocumentException("Document " + this + " has no child: " + src);
        }
        Document destDoc;
        if (dest == null) {
            destDoc = null;
        } else {
            destDoc = getChild(dest);
            if (destDoc == null) {
                throw new DocumentException("Document " + this + " has no child: " + dest);
            }
        }
        session.orderBefore(id, srcDoc.getUUID(), destDoc == null ? null : destDoc.getUUID());
    }

    // simple property only
    @Override
    public Serializable getPropertyValue(String name) throws DocumentException {
        DBSDocumentState docState = getStateMaybeProxyTarget(name);
        return docState.get(name);
    }

    // simple property only
    @Override
    public void setPropertyValue(String name, Serializable value) throws DocumentException {
        DBSDocumentState docState = getStateMaybeProxyTarget(name);
        docState.put(name, value);
    }

    // helpers for getValue / setValue

    @Override
    protected State getChild(State state, String name, Type type) {
        State child = (State) state.get(name);
        if (child == null) {
            state.put(name, child = new State());
        }
        return child;
    }

    @Override
    protected List<State> getChildAsList(State state, String name) {
        @SuppressWarnings("unchecked")
        List<State> list = (List<State>) state.get(name);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    @Override
    protected void updateList(State state, String name, List<Object> values, Field field) {
        List<State> childStates = new ArrayList<>(values.size());
        for (Object v : values) {
            State childState = new State();
            setValueComplex(childState, field, v);
            childStates.add(childState);
        }
        state.put(name, (Serializable) childStates);
    }

    @Override
    protected void updateList(State state, String name, Property property) throws PropertyException {
        Collection<Property> properties = property.getChildren();
        List<State> childStates = new ArrayList<>(properties.size());
        for (Property childProperty : properties) {
            State childState = new State();
            writeComplexProperty(childState, (ComplexProperty) childProperty);
            childStates.add(childState);
        }
        state.put(name, (Serializable) childStates);
    }

    @Override
    public Object getValue(String xpath) throws PropertyException, DocumentException {
        DBSDocumentState docState = getStateMaybeProxyTarget(xpath);
        return getValueObject(docState.getState(), xpath);
    }

    @Override
    public void setValue(String xpath, Object value) throws PropertyException, DocumentException {
        DBSDocumentState docState = getStateMaybeProxyTarget(xpath);
        // markDirty has to be called *before* we change the state
        docState.markDirty();
        setValueObject(docState.getState(), xpath, value);
    }

    @Override
    public void visitBlobs(Consumer<BlobAccessor> blobVisitor) throws PropertyException, DocumentException {
        if (isProxy()) {
            ((DBSDocument) getTargetDocument()).visitBlobs(blobVisitor);
            // fall through for proxy schemas
        }
        Runnable markDirty = () -> docState.markDirty();
        visitBlobs(docState.getState(), blobVisitor, markDirty);
    }

    @Override
    public Document checkIn(String label, String checkinComment) throws DocumentException {
        if (isProxy()) {
            throw new DocumentException("Proxies cannot be checked in");
        } else if (isVersion()) {
            throw new VersionNotModifiableException();
        } else {
            Document version = session.checkIn(id, label, checkinComment);
            Framework.getService(BlobManager.class).freezeVersion(version);
            return version;
        }
    }

    @Override
    public void checkOut() throws DocumentException {
        if (isProxy()) {
            throw new DocumentException("Proxies cannot be checked out");
        } else if (isVersion()) {
            throw new VersionNotModifiableException();
        } else {
            session.checkOut(id);
        }
    }

    @Override
    public List<String> getVersionsIds() throws DocumentException {
        return session.getVersionsIds(getVersionSeriesId());
    }

    @Override
    public List<Document> getVersions() throws DocumentException {
        List<String> ids = session.getVersionsIds(getVersionSeriesId());
        List<Document> versions = new ArrayList<Document>();
        for (String id : ids) {
            versions.add(session.getDocument(id));
        }
        return versions;
    }

    @Override
    public Document getLastVersion() throws DocumentException {
        return session.getLastVersion(getVersionSeriesId());
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        if (isProxy()) {
            return getTargetDocument();
        } else if (isVersion()) {
            return getWorkingCopy();
        } else {
            return this;
        }
    }

    @Override
    public void restore(Document version) throws DocumentException {
        if (!version.isVersion()) {
            throw new DocumentException("Cannot restore a non-version: " + version);
        }
        session.restoreVersion(this, version);
    }

    @Override
    public Document getVersion(String label) throws DocumentException {
        DBSDocumentState state = session.getVersionByLabel(getVersionSeriesId(), label);
        return session.getDocument(state);
    }

    @Override
    public Document getBaseVersion() throws DocumentException {
        if (isProxy() || isVersion()) {
            return null;
        } else {
            if (isCheckedOut()) {
                return null;
            } else {
                String id = (String) docState.get(KEY_BASE_VERSION_ID);
                if (id == null) {
                    // shouldn't happen
                    return null;
                }
                return session.getDocument(id);
            }
        }
    }

    @Override
    public boolean isCheckedOut() throws DocumentException {
        if (isVersion()) {
            return false;
        } else { // also if isProxy()
            return !TRUE.equals(docState.get(KEY_IS_CHECKED_IN));
        }
    }

    @Override
    public String getVersionSeriesId() throws DocumentException {
        if (isProxy()) {
            return (String) docState.get(KEY_PROXY_VERSION_SERIES_ID);
        } else if (isVersion()) {
            return (String) docState.get(KEY_VERSION_SERIES_ID);
        } else {
            return getUUID();
        }
    }

    @Override
    public Calendar getVersionCreationDate() throws DocumentException {
        return (Calendar) docState.get(KEY_VERSION_CREATED);
    }

    @Override
    public String getVersionLabel() throws DocumentException {
        return (String) docState.get(KEY_VERSION_LABEL);
    }

    @Override
    public String getCheckinComment() throws DocumentException {
        return (String) docState.get(KEY_VERSION_DESCRIPTION);
    }

    @Override
    public boolean isLatestVersion() throws DocumentException {
        if (isProxy() || isVersion()) {
            return TRUE.equals(docState.get(KEY_IS_LATEST_VERSION));
        } else {
            return false;
        }
    }

    @Override
    public boolean isMajorVersion() throws DocumentException {
        if (isProxy() || isVersion()) {
            return ZERO.equals(docState.get(KEY_MINOR_VERSION));
        } else {
            return false;
        }
    }

    @Override
    public boolean isLatestMajorVersion() throws DocumentException {
        if (isProxy() || isVersion()) {
            return TRUE.equals(docState.get(KEY_IS_LATEST_MAJOR_VERSION));
        } else {
            return false;
        }
    }

    @Override
    public boolean isVersionSeriesCheckedOut() throws DocumentException {
        if (isProxy() || isVersion()) {
            Document workingCopy = getWorkingCopy();
            return workingCopy == null ? false : workingCopy.isCheckedOut();
        } else {
            return isCheckedOut();
        }
    }

    @Override
    public Document getWorkingCopy() throws DocumentException {
        if (isProxy() || isVersion()) {
            String versionSeriesId = getVersionSeriesId();
            return versionSeriesId == null ? null : session.getDocument(versionSeriesId);
        } else {
            return this;
        }
    }

    @Override
    public Lock setLock(Lock lock) throws DocumentException {
        Lock oldLock = getLock();
        if (oldLock == null) {
            docState.put(KEY_LOCK_OWNER, lock.getOwner());
            docState.put(KEY_LOCK_CREATED, lock.getCreated());
        }
        return oldLock;
    }

    @Override
    public Lock removeLock(String owner) throws DocumentException {
        Lock oldLock = getLock();
        if (owner != null) {
            if (oldLock != null && !AbstractLockManager.canLockBeRemovedStatic(oldLock, owner)) {
                // existing mismatched lock, flag failure
                return new Lock(oldLock, true);
            }
        } else if (oldLock != null) {
            docState.put(KEY_LOCK_OWNER, null);
            docState.put(KEY_LOCK_CREATED, null);
        }
        return oldLock;
    }

    @Override
    public Lock getLock() throws DocumentException {
        String owner = (String) docState.get(KEY_LOCK_OWNER);
        if (owner == null) {
            return null;
        }
        Calendar created = (Calendar) docState.get(KEY_LOCK_CREATED);
        return new Lock(owner, created);
    }

    @Override
    public boolean isFolder() {
        return type == null // null document
                || type.isFolder();
    }

    @Override
    public void setReadOnly(boolean readonly) {
        this.readonly = readonly;
    }

    @Override
    public boolean isReadOnly() {
        return readonly;
    }

    @Override
    public void remove() throws DocumentException {
        session.remove(id);
    }

    @Override
    public String getLifeCycleState() throws LifeCycleException {
        return (String) docState.get(KEY_LIFECYCLE_STATE);
    }

    @Override
    public void setCurrentLifeCycleState(String lifeCycleState) throws LifeCycleException {
        docState.put(KEY_LIFECYCLE_STATE, lifeCycleState);
    }

    @Override
    public String getLifeCyclePolicy() throws LifeCycleException {
        return (String) docState.get(KEY_LIFECYCLE_POLICY);
    }

    @Override
    public void setLifeCyclePolicy(String policy) throws LifeCycleException {
        docState.put(KEY_LIFECYCLE_POLICY, policy);
    }

    // TODO generic
    @Override
    public void followTransition(String transition) throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        service.followTransition(this, transition);
    }

    // TODO generic
    @Override
    public Collection<String> getAllowedStateTransitions() throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        LifeCycle lifeCycle = service.getLifeCycleFor(this);
        if (lifeCycle == null) {
            return Collections.emptyList();
        }
        return lifeCycle.getAllowedStateTransitionsFrom(getLifeCycleState());
    }

    @Override
    public void setSystemProp(String name, Serializable value) throws DocumentException {

        String propertyName = systemPropNameMap.get(name);
        if (propertyName == null) {
            throw new DocumentException("Unknown system property: " + name);
        }
        setPropertyValue(propertyName, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T getSystemProp(String name, Class<T> type) throws DocumentException {
        String propertyName = systemPropNameMap.get(name);
        if (propertyName == null) {
            throw new DocumentException("Unknown system property: " + name);
        }
        Serializable value = getPropertyValue(propertyName);
        if (value == null) {
            if (type == Boolean.class) {
                value = Boolean.FALSE;
            } else if (type == Long.class) {
                value = Long.valueOf(0);
            }
        }
        return (T) value;
    }

    /**
     * Checks if the given schema should be resolved on the proxy or the target.
     */
    protected DBSDocumentState getStateMaybeProxyTarget(Type type) throws PropertyException {
        if (isProxy() && !isSchemaForProxy(type.getName())) {
            try {
                return ((DBSDocument) getTargetDocument()).docState;
            } catch (DocumentException e) {
                throw new PropertyException(e.getMessage(), e);
            }
        } else {
            return docState;
        }
    }

    protected DBSDocumentState getStateMaybeProxyTarget(String xpath) throws DocumentException {
        if (isProxy() && !isSchemaForProxy(getSchema(xpath))) {
            return ((DBSDocument) getTargetDocument()).docState;
        } else {
            return docState;
        }
    }

    protected boolean isSchemaForProxy(String schema) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        return schemaManager.isProxySchema(schema, getType().getName());
    }

    protected String getSchema(String xpath) throws DocumentException {
        int p = xpath.indexOf(':');
        if (p == -1) {
            throw new DocumentException("Schema not specified: " + xpath);
        }
        String prefix = xpath.substring(0, p);
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        Schema schema = schemaManager.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = schemaManager.getSchema(prefix);
            if (schema == null) {
                throw new DocumentException("No schema for prefix: " + xpath);
            }
        }
        return schema.getName();
    }

    @Override
    public void readDocumentPart(DocumentPart dp) throws PropertyException {
        DBSDocumentState docState = getStateMaybeProxyTarget(dp.getType());
        readComplexProperty(docState.getState(), (ComplexProperty) dp);
    }

    @Override
    protected String internalName(String name) {
        switch (name) {
        case "major_version":
            return KEY_MAJOR_VERSION;
        case "minor_version":
            return KEY_MINOR_VERSION;
        }
        return name;
    }

    @Override
    public Map<String, Serializable> readPrefetch(ComplexType complexType, Set<String> xpaths)
            throws PropertyException {
        DBSDocumentState docState = getStateMaybeProxyTarget(complexType);
        return readPrefetch(docState.getState(), complexType, xpaths);
    }

    @Override
    public void writeDocumentPart(DocumentPart dp) throws PropertyException {
        final DBSDocumentState docState = getStateMaybeProxyTarget(dp.getType());
        // markDirty has to be called *before* we change the state
        docState.markDirty();
        writeComplexProperty(docState.getState(), (ComplexProperty) dp);
        clearDirtyFlags(dp);
    }

    @Override
    public Set<String> getAllFacets() {
        Set<String> facets = new HashSet<String>(getType().getFacets());
        facets.addAll(Arrays.asList(getFacets()));
        return facets;
    }

    @Override
    public String[] getFacets() {
        Object[] mixins = (Object[]) docState.get(KEY_MIXIN_TYPES);
        if (mixins == null) {
            return EMPTY_STRING_ARRAY;
        } else {
            String[] res = new String[mixins.length];
            System.arraycopy(mixins, 0, res, 0, mixins.length);
            return res;
        }
    }

    @Override
    public boolean hasFacet(String facet) {
        return getAllFacets().contains(facet);
    }

    @Override
    public boolean addFacet(String facet) throws DocumentException {
        if (getType().getFacets().contains(facet)) {
            return false; // already present in type
        }
        Object[] mixins = (Object[]) docState.get(KEY_MIXIN_TYPES);
        if (mixins == null) {
            mixins = new Object[] { facet };
        } else {
            List<Object> list = Arrays.asList(mixins);
            if (list.contains(facet)) {
                return false; // already present in doc
            }
            list = new ArrayList<Object>(list);
            list.add(facet);
            mixins = list.toArray(new Object[list.size()]);
        }
        docState.put(KEY_MIXIN_TYPES, mixins);
        return true;
    }

    @Override
    public boolean removeFacet(String facet) throws DocumentException {
        Object[] mixins = (Object[]) docState.get(KEY_MIXIN_TYPES);
        if (mixins == null) {
            return false;
        }
        List<Object> list = new ArrayList<Object>(Arrays.asList(mixins));
        if (!list.remove(facet)) {
            return false; // not present in doc
        }
        mixins = list.toArray(new Object[list.size()]);
        if (mixins.length == 0) {
            mixins = null;
        }
        docState.put(KEY_MIXIN_TYPES, mixins);
        // remove the fields from the facet
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        CompositeType ft = schemaManager.getFacet(facet);
        for (Field field : ft.getFields()) {
            String name = field.getName().getPrefixedName();
            if (docState.containsKey(name)) {
                docState.put(name, null);
            }
        }
        return true;
    }

    @Override
    public Document getTargetDocument() throws DocumentException {
        if (isProxy()) {
            String targetId = (String) docState.get(KEY_PROXY_TARGET_ID);
            return session.getDocument(targetId);
        } else {
            return null;
        }
    }

    @Override
    public void setTargetDocument(Document target) throws DocumentException {
        if (isProxy()) {
            if (isReadOnly()) {
                throw new DocumentException("Cannot write proxy: " + this);
            }
            if (!target.getVersionSeriesId().equals(getVersionSeriesId())) {
                throw new DocumentException("Cannot set proxy target to different version series");
            }
            session.setProxyTarget(this, target);
        } else {
            throw new DocumentException("Cannot set proxy target on non-proxy");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getName() + ',' + getUUID() + ')';
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (other.getClass() == getClass()) {
            return equals((DBSDocument) other);
        }
        return false;
    }

    private boolean equals(DBSDocument other) {
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
