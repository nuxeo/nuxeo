/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.LockManager;
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

    public static final String KEY_ACE_CREATOR = "creator";

    public static final String KEY_ACE_BEGIN = "begin";

    public static final String KEY_ACE_END = "end";

    public static final String KEY_ACE_STATUS = "status";

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

    public static final String KEY_CHANGE_TOKEN = "ecm:changeToken";

    // used instead of ecm:changeToken when change tokens are disabled
    public static final String KEY_DC_MODIFIED = "dc:modified";

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

    public static final String INITIAL_CHANGE_TOKEN = "0";

    protected final String id;

    protected final DBSDocumentState docState;

    protected final DocumentType type;

    protected final List<Schema> proxySchemas;

    protected final DBSSession session;

    protected boolean readonly;

    protected static final Map<String, String> systemPropNameMap;

    static {
        systemPropNameMap = new HashMap<String, String>();
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
    public Document getParent() {
        if (isVersion()) {
            Document workingCopy = session.getDocument(getVersionSeriesId());
            return workingCopy == null ? null : workingCopy.getParent();
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
    public String getPath() {
        if (isVersion()) {
            Document workingCopy = session.getDocument(getVersionSeriesId());
            return workingCopy == null ? null : workingCopy.getPath();
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
    public Document getChild(String name) {
        return session.getChild(id, name);
    }

    @Override
    public List<Document> getChildren() {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        return session.getChildren(id);
    }

    @Override
    public List<String> getChildrenIds() {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        return session.getChildrenIds(id);
    }

    @Override
    public boolean hasChild(String name) {
        if (!isFolder()) {
            return false;
        }
        return session.hasChild(id, name);
    }

    @Override
    public boolean hasChildren() {
        if (!isFolder()) {
            return false;
        }
        return session.hasChildren(id);
    }

    @Override
    public Document addChild(String name, String typeName) {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        return session.createChild(null, id, name, null, typeName);
    }

    @Override
    public void orderBefore(String src, String dest) {
        Document srcDoc = getChild(src);
        if (srcDoc == null) {
            throw new DocumentNotFoundException("Document " + this + " has no child: " + src);
        }
        Document destDoc;
        if (dest == null) {
            destDoc = null;
        } else {
            destDoc = getChild(dest);
            if (destDoc == null) {
                throw new DocumentNotFoundException("Document " + this + " has no child: " + dest);
            }
        }
        session.orderBefore(id, srcDoc.getUUID(), destDoc == null ? null : destDoc.getUUID());
    }

    // simple property only
    @Override
    public Serializable getPropertyValue(String name) {
        DBSDocumentState docState = getStateOrTarget(name);
        return docState.get(name);
    }

    // simple property only
    @Override
    public void setPropertyValue(String name, Serializable value) {
        DBSDocumentState docState = getStateOrTarget(name);
        docState.put(name, value);
    }

    // helpers for getValue / setValue

    @Override
    protected State getChild(State state, String name, Type type) {
        return (State) state.get(name);
    }

    @Override
    protected State getChildForWrite(State state, String name, Type type) throws PropertyException {
        State child = getChild(state, name, type);
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
    protected void updateList(State state, String name, Field field, String xpath, List<Object> values) {
        List<State> childStates = new ArrayList<>(values.size());
        int i = 0;
        for (Object v : values) {
            State childState = new State();
            setValueComplex(childState, field, xpath + '/' + i, v);
            childStates.add(childState);
            i++;
        }
        state.put(name, (Serializable) childStates);
    }

    @Override
    protected List<State> updateList(State state, String name, Property property) throws PropertyException {
        Collection<Property> properties = property.getChildren();
        int newSize = properties.size();
        @SuppressWarnings("unchecked")
        List<State> childStates = (List<State>) state.get(name);
        if (childStates == null) {
            childStates = new ArrayList<>(newSize);
            state.put(name, (Serializable) childStates);
        }
        int oldSize = childStates.size();
        // remove extra list elements
        if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                childStates.remove(i);
            }
        }
        // add new list elements
        if (oldSize < newSize) {
            for (int i = oldSize; i < newSize; i++) {
                childStates.add(new State());
            }
        }
        return childStates;
    }

    @Override
    public Object getValue(String xpath) throws PropertyException {
        DBSDocumentState docState = getStateOrTarget(xpath);
        return getValueObject(docState.getState(), xpath);
    }

    @Override
    public void setValue(String xpath, Object value) throws PropertyException {
        DBSDocumentState docState = getStateOrTarget(xpath);
        // markDirty has to be called *before* we change the state
        docState.markDirty();
        setValueObject(docState.getState(), xpath, value);
    }

    @Override
    public void visitBlobs(Consumer<BlobAccessor> blobVisitor) throws PropertyException {
        if (isProxy()) {
            getTargetDocument().visitBlobs(blobVisitor);
            // fall through for proxy schemas
        }
        Runnable markDirty = () -> docState.markDirty();
        visitBlobs(docState.getState(), blobVisitor, markDirty);
    }

    @Override
    public Document checkIn(String label, String checkinComment) {
        if (isProxy()) {
            return getTargetDocument().checkIn(label, checkinComment);
        } else if (isVersion()) {
            throw new VersionNotModifiableException();
        } else {
            Document version = session.checkIn(id, label, checkinComment);
            DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
            blobManager.freezeVersion(version);
            return version;
        }
    }

    @Override
    public void checkOut() {
        if (isProxy()) {
            getTargetDocument().checkOut();
        } else if (isVersion()) {
            throw new VersionNotModifiableException();
        } else {
            session.checkOut(id);
        }
    }

    @Override
    public List<String> getVersionsIds() {
        return session.getVersionsIds(getVersionSeriesId());
    }

    @Override
    public List<Document> getVersions() {
        List<String> ids = session.getVersionsIds(getVersionSeriesId());
        List<Document> versions = new ArrayList<Document>();
        for (String id : ids) {
            versions.add(session.getDocument(id));
        }
        return versions;
    }

    @Override
    public Document getLastVersion() {
        return session.getLastVersion(getVersionSeriesId());
    }

    @Override
    public Document getSourceDocument() {
        if (isProxy()) {
            return getTargetDocument();
        } else if (isVersion()) {
            return getWorkingCopy();
        } else {
            return this;
        }
    }

    @Override
    public void restore(Document version) {
        if (!version.isVersion()) {
            throw new NuxeoException("Cannot restore a non-version: " + version);
        }
        session.restoreVersion(this, version);
    }

    @Override
    public Document getVersion(String label) {
        DBSDocumentState state = session.getVersionByLabel(getVersionSeriesId(), label);
        return session.getDocument(state);
    }

    @Override
    public Document getBaseVersion() {
        if (isProxy()) {
            return getTargetDocument().getBaseVersion();
        } else if (isVersion()) {
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
    public boolean isCheckedOut() {
        if (isProxy()) {
            return getTargetDocument().isCheckedOut();
        } else if (isVersion()) {
            return false;
        } else {
            return !TRUE.equals(docState.get(KEY_IS_CHECKED_IN));
        }
    }

    @Override
    public String getVersionSeriesId() {
        if (isProxy()) {
            return (String) docState.get(KEY_PROXY_VERSION_SERIES_ID);
        } else if (isVersion()) {
            return (String) docState.get(KEY_VERSION_SERIES_ID);
        } else {
            return getUUID();
        }
    }

    @Override
    public Calendar getVersionCreationDate() {
        DBSDocumentState docState = getStateOrTarget();
        return (Calendar) docState.get(KEY_VERSION_CREATED);
    }

    @Override
    public String getVersionLabel() {
        DBSDocumentState docState = getStateOrTarget();
        return (String) docState.get(KEY_VERSION_LABEL);
    }

    @Override
    public String getCheckinComment() {
        DBSDocumentState docState = getStateOrTarget();
        return (String) docState.get(KEY_VERSION_DESCRIPTION);
    }

    @Override
    public boolean isLatestVersion() {
        return isEqualOnVersion(TRUE, KEY_IS_LATEST_VERSION);
    }

    @Override
    public boolean isMajorVersion() {
        return isEqualOnVersion(ZERO, KEY_MINOR_VERSION);
    }

    @Override
    public boolean isLatestMajorVersion() {
        return isEqualOnVersion(TRUE, KEY_IS_LATEST_MAJOR_VERSION);
    }

    protected boolean isEqualOnVersion(Object ob, String key) {
        if (isProxy()) {
            // TODO avoid getting the target just to check if it's a version
            // use another specific property instead
            if (getTargetDocument().isVersion()) {
                return ob.equals(docState.get(key));
            } else {
                // if live version, return false
                return false;
            }
        } else if (isVersion()) {
            return ob.equals(docState.get(key));
        } else {
            return false;
        }
    }

    @Override
    public boolean isVersionSeriesCheckedOut() {
        if (isProxy() || isVersion()) {
            Document workingCopy = getWorkingCopy();
            return workingCopy == null ? false : workingCopy.isCheckedOut();
        } else {
            return isCheckedOut();
        }
    }

    @Override
    public Document getWorkingCopy() {
        if (isProxy() || isVersion()) {
            String versionSeriesId = getVersionSeriesId();
            return versionSeriesId == null ? null : session.getDocument(versionSeriesId);
        } else {
            return this;
        }
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
    public void remove() {
        session.remove(id);
    }

    @Override
    public String getLifeCycleState() {
        DBSDocumentState docState = getStateOrTarget();
        return (String) docState.get(KEY_LIFECYCLE_STATE);
    }

    @Override
    public void setCurrentLifeCycleState(String lifeCycleState) throws LifeCycleException {
        DBSDocumentState docState = getStateOrTarget();
        docState.put(KEY_LIFECYCLE_STATE, lifeCycleState);
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        blobManager.notifyChanges(this, Collections.singleton(KEY_LIFECYCLE_STATE));
    }

    @Override
    public String getLifeCyclePolicy() {
        DBSDocumentState docState = getStateOrTarget();
        return (String) docState.get(KEY_LIFECYCLE_POLICY);
    }

    @Override
    public void setLifeCyclePolicy(String policy) throws LifeCycleException {
        DBSDocumentState docState = getStateOrTarget();
        docState.put(KEY_LIFECYCLE_POLICY, policy);
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        blobManager.notifyChanges(this, Collections.singleton(KEY_LIFECYCLE_POLICY));
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
    public void setSystemProp(String name, Serializable value) {
        String propertyName;
        if (name.startsWith(SYSPROP_FULLTEXT_SIMPLE)) {
            propertyName = name.replace(SYSPROP_FULLTEXT_SIMPLE, KEY_FULLTEXT_SIMPLE);
        } else if (name.startsWith(SYSPROP_FULLTEXT_BINARY)) {
            propertyName = name.replace(SYSPROP_FULLTEXT_BINARY, KEY_FULLTEXT_BINARY);
        } else {
            propertyName = systemPropNameMap.get(name);
        }
        if (propertyName == null) {
            throw new PropertyNotFoundException(name, "Unknown system property");
        }
        setPropertyValue(propertyName, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T getSystemProp(String name, Class<T> type) {
        String propertyName = systemPropNameMap.get(name);
        if (propertyName == null) {
            throw new PropertyNotFoundException(name, "Unknown system property: ");
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

    public static final String CHANGE_TOKEN_PROXY_SEP = "/";

    @Override
    public String getChangeToken() {
        if (session.changeTokenEnabled) {
            String changeToken = docState.getChangeToken();
            if (isProxy()) {
                String targetToken = getTargetDocument().docState.getChangeToken();
                return getProxyChangeToken(changeToken, targetToken);
            } else {
                return changeToken;
            }
        } else {
            DBSDocumentState docState = getStateOrTarget();
            Calendar modified = (Calendar) docState.get(KEY_DC_MODIFIED);
            return modified == null ? null : String.valueOf(modified.getTimeInMillis());
        }
    }

    protected static String getProxyChangeToken(String proxyToken, String targetToken) {
        if (proxyToken == null && targetToken == null) {
            return null;
        } else {
            if (proxyToken == null) {
                proxyToken = "";
            } else if (targetToken == null) {
                targetToken = "";
            }
            return proxyToken + CHANGE_TOKEN_PROXY_SEP + targetToken;
        }
    }

    @Override
    public boolean validateChangeToken(String changeToken) {
        if (changeToken == null) {
            return true;
        }
        if (isProxy()) {
            return validateProxyChangeToken(changeToken, docState, getTargetDocument().docState);
        } else {
            return docState.validateChangeToken(changeToken);
        }
    }

    protected static boolean validateProxyChangeToken(String changeToken, DBSDocumentState proxyState,
            DBSDocumentState targetState) {
        String[] parts = changeToken.split(CHANGE_TOKEN_PROXY_SEP, 2);
        if (parts.length != 2) {
            // invalid format
            return false;
        }
        String proxyToken = parts[0];
        if (proxyToken.isEmpty()) {
            proxyToken = null;
        }
        String targetToken = parts[1];
        if (targetToken.isEmpty()) {
            targetToken = null;
        }
        if (proxyToken == null && targetToken == null) {
            return true;
        }
        return proxyState.validateChangeToken(proxyToken) && targetState.validateChangeToken(targetToken);
    }

    protected DBSDocumentState getStateOrTarget(Type type) throws PropertyException {
        return getStateOrTargetForSchema(type.getName());
    }

    protected DBSDocumentState getStateOrTarget(String xpath) {
        return getStateOrTargetForSchema(getSchema(xpath));
    }

    /**
     * Checks if the given schema should be resolved on the proxy or the target.
     */
    protected DBSDocumentState getStateOrTargetForSchema(String schema) {
        if (isProxy() && !isSchemaForProxy(schema)) {
            return getTargetDocument().docState;
        } else {
            return docState;
        }
    }

    /**
     * Gets the target state if this is a proxy, or the regular state otherwise.
     */
    protected DBSDocumentState getStateOrTarget() {
        if (isProxy()) {
            return getTargetDocument().docState;
        } else {
            return docState;
        }
    }

    protected boolean isSchemaForProxy(String schema) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        return schemaManager.isProxySchema(schema, getType().getName());
    }

    protected String getSchema(String xpath) {
        switch (xpath) {
        case KEY_MAJOR_VERSION:
        case KEY_MINOR_VERSION:
        case "major_version":
        case "minor_version":
            return "uid";
        case KEY_FULLTEXT_JOBID:
        case KEY_LIFECYCLE_POLICY:
        case KEY_LIFECYCLE_STATE:
            return "__ecm__";
        }
        if (xpath.startsWith(KEY_FULLTEXT_SIMPLE) || xpath.startsWith(KEY_FULLTEXT_BINARY)) {
            return "__ecm__";
        }
        String[] segments = xpath.split("/");
        String segment = segments[0];
        Field field = type.getField(segment);
        if (field == null) {
            // check facets
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            for (String facet : getFacets()) {
                CompositeType facetType = schemaManager.getFacet(facet);
                field = facetType.getField(segment);
                if (field != null) {
                    break;
                }
            }
        }
        if (field == null && getProxySchemas() != null) {
            // check proxy schemas
            for (Schema schema : getProxySchemas()) {
                field = schema.getField(segment);
                if (field != null) {
                    break;
                }
            }
        }
        if (field == null) {
            throw new PropertyNotFoundException(xpath);
        }
        return field.getDeclaringType().getName();
    }

    @Override
    public void readDocumentPart(DocumentPart dp) throws PropertyException {
        DBSDocumentState docState = getStateOrTarget(dp.getType());
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
        DBSDocumentState docState = getStateOrTarget(complexType);
        return readPrefetch(docState.getState(), complexType, xpaths);
    }

    @Override
    public boolean writeDocumentPart(DocumentPart dp, WriteContext writeContext) throws PropertyException {
        DBSDocumentState docState = getStateOrTarget(dp.getType());
        // markDirty has to be called *before* we change the state
        docState.markDirty();
        boolean changed = writeComplexProperty(docState.getState(), (ComplexProperty) dp, writeContext);
        clearDirtyFlags(dp);
        return changed;
    }

    @Override
    public Set<String> getAllFacets() {
        Set<String> facets = new HashSet<String>(getType().getFacets());
        facets.addAll(Arrays.asList(getFacets()));
        return facets;
    }

    @Override
    public String[] getFacets() {
        DBSDocumentState docState = getStateOrTarget();
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
    public boolean addFacet(String facet) {
        if (getType().getFacets().contains(facet)) {
            return false; // already present in type
        }
        DBSDocumentState docState = getStateOrTarget();
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
    public boolean removeFacet(String facet) {
        DBSDocumentState docState = getStateOrTarget();
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
        // remove the fields belonging to the facet
        // except for schemas still present due to the primary type or another facet
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        CompositeType ft = schemaManager.getFacet(facet);
        Set<String> otherSchemas = getSchemas(getType(), list);
        for (Schema schema : ft.getSchemas()) {
            if (otherSchemas.contains(schema.getName())) {
                continue;
            }
            for (Field field : schema.getFields()) {
                String name = field.getName().getPrefixedName();
                if (docState.containsKey(name)) {
                    docState.put(name, null);
                }
            }
        }
        return true;
    }

    protected static Set<String> getSchemas(DocumentType type, List<Object> facets) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Set<String> schemas = new HashSet<>(Arrays.asList(type.getSchemaNames()));
        for (Object facet : facets) {
            CompositeType ft = schemaManager.getFacet((String) facet);
            if (ft != null) {
                schemas.addAll(Arrays.asList(ft.getSchemaNames()));
            }
        }
        return schemas;
    }

    @Override
    public DBSDocument getTargetDocument() {
        if (isProxy()) {
            String targetId = (String) docState.get(KEY_PROXY_TARGET_ID);
            return session.getDocument(targetId);
        } else {
            return null;
        }
    }

    @Override
    public void setTargetDocument(Document target) {
        if (isProxy()) {
            if (isReadOnly()) {
                throw new ReadOnlyPropertyException("Cannot write proxy: " + this);
            }
            if (!target.getVersionSeriesId().equals(getVersionSeriesId())) {
                throw new ReadOnlyPropertyException("Cannot set proxy target to different version series");
            }
            session.setProxyTarget(this, target);
        } else {
            throw new NuxeoException("Cannot set proxy target on non-proxy");
        }
    }

    @Override
    protected Lock getDocumentLock() {
        String owner = (String) docState.get(KEY_LOCK_OWNER);
        if (owner == null) {
            return null;
        }
        Calendar created = (Calendar) docState.get(KEY_LOCK_CREATED);
        return new Lock(owner, created);
    }

    @Override
    protected Lock setDocumentLock(Lock lock) {
        String owner = (String) docState.get(KEY_LOCK_OWNER);
        if (owner != null) {
            // return old lock
            Calendar created = (Calendar) docState.get(KEY_LOCK_CREATED);
            return new Lock(owner, created);
        }
        docState.put(KEY_LOCK_OWNER, lock.getOwner());
        docState.put(KEY_LOCK_CREATED, lock.getCreated());
        return null;
    }

    @Override
    protected Lock removeDocumentLock(String owner) {
        String oldOwner = (String) docState.get(KEY_LOCK_OWNER);
        if (oldOwner == null) {
            // no previous lock
            return null;
        }
        Calendar oldCreated = (Calendar) docState.get(KEY_LOCK_CREATED);
        if (!LockManager.canLockBeRemoved(oldOwner, owner)) {
            // existing mismatched lock, flag failure
            return new Lock(oldOwner, oldCreated, true);
        }
        // remove lock
        docState.put(KEY_LOCK_OWNER, null);
        docState.put(KEY_LOCK_CREATED, null);
        // return old lock
        return new Lock(oldOwner, oldCreated);
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
