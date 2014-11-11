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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.StorageBlob;
import org.nuxeo.ecm.core.storage.binary.Binary;
import org.nuxeo.ecm.core.storage.binary.BinaryManager;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerStreamSupport;
import org.nuxeo.ecm.core.storage.sql.LockManager;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocumentVersion.VersionNotModifiableException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Implementation of a {@link Document} for Document-Based Storage.
 *
 * The document is stored as a JSON-like Map. The keys of the Map are the
 * property names (including special names for system properties), and the
 * values Map are Serializable values, either:
 * <ul>
 * <li>a scalar (String, Long, Double, Boolean, Calendar, Binary),
 * <li>an array of scalars,
 * <li>a List of Maps, recursively,
 * <li>or another Map, recursively.
 * </ul>
 * An ACP value is stored as a list of maps. Each map has a keys for the ACL
 * name and the actual ACL which is a list of ACEs. An ACE is a map having as
 * keys username, permission, and grant.
 *
 * @since 5.9.4
 */
public class DBSDocument implements Document {

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

    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected final String id;

    protected final DBSDocumentState docState;

    protected final DocumentType type;

    protected final DBSSession session;

    protected boolean readonly;

    public DBSDocument(DBSDocumentState docState, DocumentType type,
            DBSSession session, boolean readonly) {
        // no state for NullDocument (parent of placeless children)
        this.id = docState == null ? null : (String) docState.get(KEY_ID);
        this.docState = docState;
        this.type = type;
        this.session = session;
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
    public String getUUID() {
        return id;
    }

    @Override
    public String getName() {
        return docState.getName();
    }

    @Override
    public Document getParent() throws DocumentException {
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
    public Iterator<Document> getChildren() throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
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
    public Document addChild(String name, String typeName)
            throws DocumentException {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        return session.createChild(null, id, name, null, typeName);
    }

    @Override
    public void orderBefore(String src, String dest) throws DocumentException {
        Document srcDoc = getChild(src);
        if (srcDoc == null) {
            throw new DocumentException("Document " + this + " has no child: "
                    + src);
        }
        Document destDoc;
        if (dest == null) {
            destDoc = null;
        } else {
            destDoc = getChild(dest);
            if (destDoc == null) {
                throw new DocumentException("Document " + this
                        + " has no child: " + dest);
            }
        }
        session.orderBefore(id, srcDoc.getUUID(), destDoc == null ? null
                : destDoc.getUUID());
    }

    // simple property only
    @Override
    public Serializable getPropertyValue(String name) throws DocumentException {
        DBSDocumentState docState = getStateMaybeProxyTarget(name);
        return docState.get(name);
    }

    // simple property only
    @Override
    public void setPropertyValue(String name, Serializable value)
            throws DocumentException {
        DBSDocumentState docState = getStateMaybeProxyTarget(name);
        docState.put(name, value);
    }

    @Override
    public Document checkIn(String label, String checkinComment)
            throws DocumentException {
        if (isProxy()) {
            throw new DocumentException("Proxies cannot be checked in");
        } else if (isVersion()) {
            throw new VersionNotModifiableException();
        } else {
            return session.checkIn(id, label, checkinComment);
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
            throw new DocumentException("Cannot restore a non-version: "
                    + version);
        }
        session.restoreVersion(this, version);
    }

    @Override
    public Document getVersion(String label) throws DocumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
            return versionSeriesId == null ? null
                    : session.getDocument(versionSeriesId);
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
            if (oldLock != null
                    && !LockManager.canLockBeRemoved(oldLock, owner)) {
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
    public void setCurrentLifeCycleState(String lifeCycleState)
            throws LifeCycleException {
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
    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
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
    public void setSystemProp(String name, Serializable value)
            throws DocumentException {
        String propertyName;
        if (name.equals(SYSPROP_FULLTEXT_SIMPLE)) {
            propertyName = KEY_FULLTEXT_SIMPLE;
        } else if (name.equals(SYSPROP_FULLTEXT_BINARY)) {
            propertyName = KEY_FULLTEXT_BINARY;
        } else if (name.equals(SYSPROP_FULLTEXT_JOBID)) {
            propertyName = KEY_FULLTEXT_JOBID;
        } else {
            throw new DocumentException("Unknown system property: " + name);
        }
        setPropertyValue(propertyName, value);
    }

    @Override
    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the given schema should be resolved on the proxy or the target.
     */
    protected DBSDocumentState getStateMaybeProxyTarget(Type type)
            throws PropertyException {
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

    protected DBSDocumentState getStateMaybeProxyTarget(String xpath)
            throws DocumentException {
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
        readComplexProperty((ComplexProperty) dp, docState.getState());
    }

    protected String internalName(String name) {
        switch (name) {
        case "major_version":
            return KEY_MAJOR_VERSION;
        case "minor_version":
            return KEY_MINOR_VERSION;
        }
        return name;
    }

    protected void readComplexProperty(ComplexProperty complexProperty,
            State state) throws PropertyException {
        if (state == null) {
            complexProperty.init(null);
            return;
        }
        if (complexProperty instanceof BlobProperty) {
            StorageBlob value = readBlob(state);
            complexProperty.init(value);
            return;
        }
        for (Property property : complexProperty) {
            String name = property.getField().getName().getPrefixedName();
            name = internalName(name);
            Type type = property.getType();
            if (type.isSimpleType()) {
                // simple property
                Serializable value = state.get(name);
                property.init(value);
            } else if (type.isListType()) {
                ListType listType = (ListType) type;
                if (listType.getFieldType().isSimpleType()) {
                    // array
                    Object[] array = (Object[]) state.get(name);
                    array = typedArray(listType.getFieldType(), array);
                    property.init(array);
                } else {
                    // complex list
                    @SuppressWarnings("unchecked")
                    List<Serializable> list = (List<Serializable>) state.get(name);
                    if (list == null) {
                        property.init(null);
                    } else {
                        Field listField = listType.getField();
                        List<Serializable> value = new ArrayList<Serializable>(
                                list.size());
                        for (Serializable subMapSer : list) {
                            State childMap = (State) subMapSer;
                            ComplexProperty p = (ComplexProperty) complexProperty.getRoot().createProperty(
                                    property, listField, 0);
                            readComplexProperty(p, childMap);
                            value.add(p.getValue());
                        }
                        property.init((Serializable) value);
                    }
                }
            } else {
                // complex property
                State childMap = (State) state.get(name);
                readComplexProperty((ComplexProperty) property, childMap);
                ((ComplexProperty) property).removePhantomFlag();
            }
        }
    }

    protected static Object[] typedArray(Type type, Object[] array) {
        if (array == null) {
            return null;
        }
        Class<?> klass;
        if (type instanceof StringType) {
            klass = String.class;
        } else if (type instanceof BooleanType) {
            klass = Boolean.class;
        } else if (type instanceof LongType) {
            klass = Long.class;
        } else if (type instanceof DoubleType) {
            klass = Double.class;
        } else if (type instanceof DateType) {
            klass = Calendar.class;
        } else if (type instanceof BinaryType) {
            klass = Binary.class;
        } else if (type instanceof IntegerType) {
            throw new RuntimeException("Unimplemented primitive type: "
                    + type.getClass().getName());
        } else if (type instanceof SimpleTypeImpl) {
            // simple type with constraints -- ignore constraints XXX
            return typedArray(type.getSuperType(), array);
        } else {
            throw new RuntimeException("Invalid primitive type: "
                    + type.getClass().getName());
        }
        int len = array.length;
        Object[] copy = (Object[]) Array.newInstance(klass, len);
        System.arraycopy(array, 0, copy, 0, len);
        return copy;
    }

    protected StorageBlob readBlob(State state) {
        Serializable data = state.get(KEY_BLOB_DATA);
        if (data == null) {
            return null;
        }
        Binary binary = session.getBinaryManager().getBinary((String) data);
        if (binary == null) {
            return null;
        }
        String name = (String) state.get(KEY_BLOB_NAME);
        String mimeType = (String) state.get(KEY_BLOB_MIME_TYPE);
        String encoding = (String) state.get(KEY_BLOB_ENCODING);
        String digest = (String) state.get(KEY_BLOB_DIGEST);
        Long length = (Long) state.get(KEY_BLOB_LENGTH);
        return new StorageBlob(binary, name, mimeType, encoding, digest,
                length.longValue());
    }

    protected void writeBlobProperty(BlobProperty blobProperty, State state)
            throws PropertyException {
        Serializable value = blobProperty.getValueForWrite();
        String data;
        String name;
        String mimeType;
        String encoding;
        String digest;
        Long length;
        if (value == null) {
            data = null;
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
            Binary binary;
            try {
                binary = getBinary(blob);
            } catch (DocumentException e) {
                throw new PropertyException("Cannot get binary", e);
            }
            data = binary.getDigest();
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

        state.put(KEY_BLOB_DATA, data);
        state.put(KEY_BLOB_NAME, name);
        state.put(KEY_BLOB_MIME_TYPE, mimeType);
        state.put(KEY_BLOB_ENCODING, encoding);
        state.put(KEY_BLOB_DIGEST, digest);
        state.put(KEY_BLOB_LENGTH, length);
    }

    protected Binary getBinary(Blob blob) throws DocumentException {
        if (blob instanceof StorageBlob) {
            return ((StorageBlob) blob).getBinary();
        }
        if (blob instanceof StreamingBlob) {
            StreamingBlob sb = (StreamingBlob) blob;
            StreamSource source = sb.getStreamSource();
            if (source instanceof FileSource && sb.isTemporary()) {
                return getBinary((FileSource) source);
            }
        }
        try {
            InputStream stream = blob.getStream();
            return getBinary(stream);
        } catch (IOException e) {
            throw new DocumentException(e);
        }
    }

    protected Binary getBinary(FileSource source) throws DocumentException {
        BinaryManager binaryManager = session.getBinaryManager();
        try {
            if (binaryManager instanceof BinaryManagerStreamSupport) {
                return ((BinaryManagerStreamSupport) binaryManager).getBinary(source);
            }
            return binaryManager.getBinary(source.getStream());
        } catch (IOException e) {
            throw new DocumentException(e);
        }
    }

    protected Binary getBinary(InputStream in) throws DocumentException {
        BinaryManager repositoryManager = session.getBinaryManager();
        try {
            return repositoryManager.getBinary(in);
        } catch (IOException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public Map<String, Serializable> readPrefetch(ComplexType complexType,
            Set<String> xpaths) throws PropertyException {
        DBSDocumentState docState = getStateMaybeProxyTarget(complexType);
        Map<String, Serializable> prefetch = new HashMap<String, Serializable>();
        for (String xpath : xpaths) {
            try {
                readPrefetch(complexType, docState.getState(), xpath, 0,
                        prefetch);
            } catch (IllegalStateException e) {
                throw new IllegalStateException(e.getMessage() + " xpath="
                        + xpath + ", data=" + docState, e);
            }
        }
        return prefetch;
    }

    protected static void readPrefetch(ComplexType type, State state,
            String xpath, int start, Map<String, Serializable> prefetch) {
        int i = xpath.indexOf('/', start);
        boolean last = i == -1;
        String prop = last ? xpath : xpath.substring(start, i);
        Serializable v = state == null ? null : state.get(prop);
        Field propType = type.getField(prop);
        if (last) {
            if (v instanceof State || v instanceof List) {
                throw new IllegalStateException("xpath=" + xpath + " start="
                        + start + " last element is not scalar");
            }
            if (v instanceof Object[]) {
                // convert to typed array
                Type lt = ((ListType) propType.getType()).getFieldType();
                v = typedArray(lt, (Object[]) v);
            }
            prefetch.put(xpath, v);
        } else {
            int len = xpath.length();
            if (i + 3 < len && xpath.charAt(i + 1) == '*'
                    && xpath.charAt(i + 2) == '/') {
                // list
                if (v != null && !(v instanceof List)) {
                    throw new IllegalStateException("xpath=" + xpath
                            + " start=" + start + " not a List");
                }
                List<?> list = v == null ? Collections.emptyList()
                        : (List<?>) v;
                String base = xpath.substring(0, i + 1);
                for (int n = 0; n < list.size(); n++) {
                    String xp = base + n;
                    Object elem = list.get(n);
                    if (!(elem instanceof State)) {
                        throw new IllegalStateException("xp=" + xp
                                + " not a Map");
                    }
                    State subMap = (State) elem;
                    Type lt = ((ListType) propType.getType()).getFieldType();
                    readPrefetch((ComplexType) lt, subMap, xp, i + 3, prefetch);
                }
            } else {
                // map
                if (v != null && !(v instanceof State)) {
                    throw new IllegalStateException("xpath=" + xpath
                            + " start=" + start + " not a Map");
                }
                State subMap = (State) v;
                // XXX
                readPrefetch((ComplexType) propType.getType(), subMap, xpath,
                        i + 1, prefetch);
            }
        }
    }

    @Override
    public void writeDocumentPart(DocumentPart dp) throws PropertyException {
        final DBSDocumentState docState = getStateMaybeProxyTarget(dp.getType());
        // markDirty callback, which has to be called *before*
        // we change the state
        Runnable markDirty = new Runnable() {
            @Override
            public void run() {
                docState.markDirty();
            }
        };
        writeComplexProperty((ComplexProperty) dp, docState.getState(),
                markDirty);
        clearDirtyFlags(dp);
    }

    protected static void clearDirtyFlags(Property property) {
        if (property.isContainer()) {
            for (Property p : property) {
                clearDirtyFlags(p);
            }
        }
        property.clearDirtyFlags();
    }

    protected void writeComplexProperty(ComplexProperty complexProperty,
            State state, Runnable markDirty) throws PropertyException {
        if (complexProperty instanceof BlobProperty) {
            writeBlobProperty((BlobProperty) complexProperty, state);
            return;
        }
        for (Property property : complexProperty) {
            String name = property.getField().getName().getPrefixedName();
            name = internalName(name);
            // TODO XXX
            // if (checkReadOnlyIgnoredWrite(doc, property, map)) {
            // continue;
            // }
            Type type = property.getType();
            if (type.isSimpleType()) {
                // simple property
                Serializable value = property.getValueForWrite();
                markDirty.run();
                state.put(name, value);
            } else if (type.isListType()) {
                ListType listType = (ListType) type;
                if (listType.getFieldType().isSimpleType()) {
                    // array
                    Serializable value = property.getValueForWrite();
                    if (value instanceof List) {
                        value = ((List<?>) value).toArray(new Object[0]);
                    } else if (!(value == null || value instanceof Object[])) {
                        throw new IllegalStateException(value.toString());
                    }
                    markDirty.run();
                    state.put(name, value);
                } else {
                    // complex list
                    Collection<Property> children = property.getChildren();
                    List<Serializable> childMaps = new ArrayList<Serializable>(
                            children.size());
                    for (Property childProperty : children) {
                        State childMap = new State();
                        writeComplexProperty((ComplexProperty) childProperty,
                                childMap, markDirty);
                        childMaps.add(childMap);
                    }
                    markDirty.run();
                    state.put(name, (Serializable) childMaps);
                }
            } else {
                // complex property
                State childMap = (State) state.get(name);
                if (childMap == null) {
                    childMap = new State();
                    markDirty.run();
                    state.put(name, childMap);
                }
                writeComplexProperty((ComplexProperty) property, childMap,
                        markDirty);
            }
        }
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
                throw new DocumentException(
                        "Cannot set proxy target to different version series");
            }
            session.setProxyTarget(this, target);
        } else {
            throw new DocumentException("Cannot set proxy target on non-proxy");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getName() + ',' + getUUID()
                + ')';
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
