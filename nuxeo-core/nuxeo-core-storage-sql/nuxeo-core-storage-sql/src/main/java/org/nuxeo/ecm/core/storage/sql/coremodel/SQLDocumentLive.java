/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.BaseDocument;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.runtime.api.Framework;

public class SQLDocumentLive extends BaseDocument<Node>implements SQLDocument {

    protected final Node node;

    protected final Type type;

    protected SQLSession session;

    /** Proxy-induced types. */
    protected final List<Schema> proxySchemas;

    /**
     * Read-only flag, used to allow/disallow writes on versions.
     */
    protected boolean readonly;

    protected SQLDocumentLive(Node node, ComplexType type, SQLSession session, boolean readonly) {
        this.node = node;
        this.type = type;
        this.session = session;
        if (node != null && node.isProxy()) {
            SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
            proxySchemas = schemaManager.getProxySchemas(type.getName());
        } else {
            proxySchemas = null;
        }
        this.readonly = readonly;
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
    public Node getNode() {
        return node;
    }

    @Override
    public String getName() {
        return getNode() == null ? null : getNode().getName();
    }

    @Override
    public Long getPos() {
        return getNode().getPos();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Document -----
     */

    @Override
    public DocumentType getType() {
        return (DocumentType) type;
    }

    @Override
    public SQLSession getSession() {
        return session;
    }

    @Override
    public boolean isFolder() {
        return type == null // null document
                || ((DocumentType) type).isFolder();
    }

    @Override
    public String getUUID() {
        return session.idToString(getNode().getId());
    }

    @Override
    public Document getParent() {
        return session.getParent(getNode());
    }

    @Override
    public String getPath() {
        return session.getPath(getNode());
    }

    @Override
    public boolean isProxy() {
        return false;
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
    public void remove() {
        session.remove(getNode());
    }

    /**
     * Reads into the {@link DocumentPart} the values from this {@link SQLDocument}.
     */
    @Override
    public void readDocumentPart(DocumentPart dp) throws PropertyException {
        readComplexProperty(getNode(), (ComplexProperty) dp);
    }

    @Override
    public Map<String, Serializable> readPrefetch(ComplexType complexType, Set<String> xpaths)
            throws PropertyException {
        return readPrefetch(getNode(), complexType, xpaths);
    }

    @Override
    public boolean writeDocumentPart(DocumentPart dp, WriteContext writeContext) throws PropertyException {
        boolean changed = writeComplexProperty(getNode(), (ComplexProperty) dp, writeContext);
        clearDirtyFlags(dp);
        return changed;
    }

    @Override
    protected Node getChild(Node node, String name, Type type) throws PropertyException {
        return session.getChildProperty(node, name, type.getName());
    }

    @Override
    protected Node getChildForWrite(Node node, String name, Type type) throws PropertyException {
        return session.getChildPropertyForWrite(node, name, type.getName());
    }

    @Override
    protected List<Node> getChildAsList(Node node, String name) throws PropertyException {
        return session.getComplexList(node, name);
    }

    @Override
    protected void updateList(Node node, String name, Field field, String xpath, List<Object> values)
            throws PropertyException {
        List<Node> childNodes = getChildAsList(node, name);
        int oldSize = childNodes.size();
        int newSize = values.size();
        // remove extra list elements
        if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                session.removeProperty(childNodes.remove(i));
            }
        }
        // add new list elements
        if (oldSize < newSize) {
            String typeName = field.getType().getName();
            for (int i = oldSize; i < newSize; i++) {
                Node childNode = session.addChildProperty(node, name, Long.valueOf(i), typeName);
                childNodes.add(childNode);
            }
        }
        // write values
        int i = 0;
        for (Object v : values) {
            Node childNode = childNodes.get(i);
            setValueComplex(childNode, field, xpath + '/' + i, v);
            i++;
        }
    }

    @Override
    protected List<Node> updateList(Node node, String name, Property property) throws PropertyException {
        Collection<Property> properties = property.getChildren();
        List<Node> childNodes = getChildAsList(node, name);
        int oldSize = childNodes.size();
        int newSize = properties.size();
        // remove extra list elements
        if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                session.removeProperty(childNodes.remove(i));
            }
        }
        // add new list elements
        if (oldSize < newSize) {
            String typeName = ((ListType) property.getType()).getFieldType().getName();
            for (int i = oldSize; i < newSize; i++) {
                Node childNode = session.addChildProperty(node, name, Long.valueOf(i), typeName);
                childNodes.add(childNode);
            }
        }
        return childNodes;
    }

    @Override
    protected String internalName(String name) {
        return name;
    }

    @Override
    public Object getValue(String xpath) throws PropertyException {
        return getValueObject(getNode(), xpath);
    }

    @Override
    public void setValue(String xpath, Object value) throws PropertyException {
        setValueObject(getNode(), xpath, value);
    }

    @Override
    public void visitBlobs(Consumer<BlobAccessor> blobVisitor) throws PropertyException {
        visitBlobs(getNode(), blobVisitor, NO_DIRTY);
    }

    @Override
    public Serializable getPropertyValue(String name) {
        return getNode().getSimpleProperty(name).getValue();
    }

    @Override
    public void setPropertyValue(String name, Serializable value) {
        getNode().setSimpleProperty(name, value);
    }

    protected static final Map<String, String> systemPropNameMap;

    static {
        systemPropNameMap = new HashMap<String, String>();
        systemPropNameMap.put(FULLTEXT_JOBID_SYS_PROP, Model.FULLTEXT_JOBID_PROP);
    }

    @Override
    public void setSystemProp(String name, Serializable value) {
        String propertyName;
        if (name.startsWith(SIMPLE_TEXT_SYS_PROP)) {
            propertyName = name.replace(SIMPLE_TEXT_SYS_PROP, Model.FULLTEXT_SIMPLETEXT_PROP);
        } else if (name.startsWith(BINARY_TEXT_SYS_PROP)) {
            propertyName = name.replace(BINARY_TEXT_SYS_PROP, Model.FULLTEXT_BINARYTEXT_PROP);
        } else {
            propertyName = systemPropNameMap.get(name);
        }
        if (propertyName == null) {
            throw new PropertyNotFoundException(name, "Unknown system property");
        }
        setPropertyValue(propertyName, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getSystemProp(String name, Class<T> type) {
        String propertyName = systemPropNameMap.get(name);
        if (propertyName == null) {
            throw new PropertyNotFoundException(name, "Unknown system property");
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

    @Override
    public String getChangeToken() {
        if (session.isChangeTokenEnabled()) {
            return (String) getPropertyValue(Model.MAIN_CHANGE_TOKEN_PROP);
        } else {
            Calendar modified;
            try {
                modified = (Calendar) getPropertyValue(DC_MODIFIED);
            } catch (PropertyNotFoundException e) {
                modified = null;
            }
            return modified == null ? null : String.valueOf(modified.getTimeInMillis());
        }
    }

    @Override
    public boolean validateChangeToken(String changeToken) {
        if (changeToken == null) {
            return true;
        }
        String currentToken = getChangeToken();
        return validateChangeToken(changeToken, currentToken);
    }

    /*
     * ----- LifeCycle -----
     */

    @Override
    public String getLifeCyclePolicy() {
        return (String) getPropertyValue(Model.MISC_LIFECYCLE_POLICY_PROP);
    }

    @Override
    public void setLifeCyclePolicy(String policy) {
        setPropertyValue(Model.MISC_LIFECYCLE_POLICY_PROP, policy);
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        blobManager.notifyChanges(this, Collections.singleton(Model.MISC_LIFECYCLE_POLICY_PROP));
    }

    @Override
    public String getLifeCycleState() {
        return (String) getPropertyValue(Model.MISC_LIFECYCLE_STATE_PROP);
    }

    @Override
    public void setCurrentLifeCycleState(String state) {
        setPropertyValue(Model.MISC_LIFECYCLE_STATE_PROP, state);
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        blobManager.notifyChanges(this, Collections.singleton(Model.MISC_LIFECYCLE_STATE_PROP));
    }

    @Override
    public void followTransition(String transition) throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new NuxeoException("LifeCycleService not available");
        }
        service.followTransition(this, transition);
    }

    @Override
    public Collection<String> getAllowedStateTransitions() {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new NuxeoException("LifeCycleService not available");
        }
        LifeCycle lifeCycle = service.getLifeCycleFor(this);
        if (lifeCycle == null) {
            return Collections.emptyList();
        }
        return lifeCycle.getAllowedStateTransitionsFrom(getLifeCycleState());
    }

    /*
     * ----- org.nuxeo.ecm.core.versioning.VersionableDocument -----
     */

    @Override
    public boolean isVersion() {
        return false;
    }

    @Override
    public Document getBaseVersion() {
        if (isCheckedOut()) {
            return null;
        }
        Serializable id = (Serializable) getPropertyValue(Model.MAIN_BASE_VERSION_PROP);
        if (id == null) {
            // shouldn't happen
            return null;
        }
        return session.getDocumentById(id);
    }

    @Override
    public String getVersionSeriesId() {
        return getUUID();
    }

    @Override
    public Document getSourceDocument() {
        return this;
    }

    @Override
    public Document checkIn(String label, String checkinComment) {
        Document version = session.checkIn(getNode(), label, checkinComment);
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        blobManager.freezeVersion(version);
        return version;
    }

    @Override
    public void checkOut() {
        session.checkOut(getNode());
    }

    @Override
    public boolean isCheckedOut() {
        return !Boolean.TRUE.equals(getPropertyValue(Model.MAIN_CHECKED_IN_PROP));
    }

    @Override
    public boolean isMajorVersion() {
        return false;
    }

    @Override
    public boolean isLatestVersion() {
        return false;
    }

    @Override
    public boolean isLatestMajorVersion() {
        return false;
    }

    @Override
    public boolean isVersionSeriesCheckedOut() {
        return isCheckedOut();
    }

    @Override
    public String getVersionLabel() {
        return (String) getPropertyValue(Model.VERSION_LABEL_PROP);
    }

    @Override
    public String getCheckinComment() {
        return (String) getPropertyValue(Model.VERSION_DESCRIPTION_PROP);
    }

    @Override
    public Document getWorkingCopy() {
        return this;
    }

    @Override
    public Calendar getVersionCreationDate() {
        return (Calendar) getPropertyValue(Model.VERSION_CREATED_PROP);
    }

    @Override
    public void restore(Document version) {
        if (!version.isVersion()) {
            throw new NuxeoException("Cannot restore a non-version: " + version);
        }
        session.restore(getNode(), ((SQLDocument) version).getNode());
    }

    @Override
    public List<String> getVersionsIds() {
        String versionSeriesId = getVersionSeriesId();
        Collection<Document> versions = session.getVersions(versionSeriesId);
        List<String> ids = new ArrayList<String>(versions.size());
        for (Document version : versions) {
            ids.add(version.getUUID());
        }
        return ids;
    }

    @Override
    public Document getVersion(String label) {
        String versionSeriesId = getVersionSeriesId();
        return session.getVersionByLabel(versionSeriesId, label);
    }

    @Override
    public List<Document> getVersions() {
        String versionSeriesId = getVersionSeriesId();
        return session.getVersions(versionSeriesId);
    }

    @Override
    public Document getLastVersion() {
        String versionSeriesId = getVersionSeriesId();
        return session.getLastVersion(versionSeriesId);
    }

    @Override
    public Document getChild(String name) {
        return session.getChild(getNode(), name);
    }

    @Override
    public List<Document> getChildren() {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        return session.getChildren(getNode()); // newly allocated
    }

    @Override
    public List<String> getChildrenIds() {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        // not optimized as this method doesn't seem to be used
        List<Document> children = session.getChildren(getNode());
        List<String> ids = new ArrayList<String>(children.size());
        for (Document child : children) {
            ids.add(child.getUUID());
        }
        return ids;
    }

    @Override
    public boolean hasChild(String name) {
        if (!isFolder()) {
            return false;
        }
        return session.hasChild(getNode(), name);
    }

    @Override
    public boolean hasChildren() {
        if (!isFolder()) {
            return false;
        }
        return session.hasChildren(getNode());
    }

    @Override
    public Document addChild(String name, String typeName) {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        return session.addChild(getNode(), name, null, typeName);
    }

    @Override
    public void orderBefore(String src, String dest) {
        SQLDocument srcDoc = (SQLDocument) getChild(src);
        if (srcDoc == null) {
            throw new DocumentNotFoundException("Document " + this + " has no child: " + src);
        }
        SQLDocument destDoc;
        if (dest == null) {
            destDoc = null;
        } else {
            destDoc = (SQLDocument) getChild(dest);
            if (destDoc == null) {
                throw new DocumentNotFoundException("Document " + this + " has no child: " + dest);
            }
        }
        session.orderBefore(getNode(), srcDoc.getNode(), destDoc == null ? null : destDoc.getNode());
    }

    @Override
    public Set<String> getAllFacets() {
        return getNode().getAllMixinTypes();
    }

    @Override
    public String[] getFacets() {
        return getNode().getMixinTypes();
    }

    @Override
    public boolean hasFacet(String facet) {
        return getNode().hasMixinType(facet);
    }

    @Override
    public boolean addFacet(String facet) {
        return session.addMixinType(getNode(), facet);
    }

    @Override
    public boolean removeFacet(String facet) {
        return session.removeMixinType(getNode(), facet);
    }

    /*
     * ----- PropertyContainer inherited from SQLComplexProperty -----
     */

    /*
     * ----- toString/equals/hashcode -----
     */

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
        if (other.getClass() == this.getClass()) {
            return equals((SQLDocumentLive) other);
        }
        return false;
    }

    private boolean equals(SQLDocumentLive other) {
        return getNode().equals(other.getNode());
    }

    @Override
    public int hashCode() {
        return getNode().hashCode();
    }

    @Override
    public Document getTargetDocument() {
        return null;
    }

    @Override
    public void setTargetDocument(Document target) {
        throw new NuxeoException("Not a proxy");
    }

    @Override
    protected Lock getDocumentLock() {
        // lock manager can get the lock even on a recently created and unsaved document
        throw new UnsupportedOperationException();
    }

    @Override
    protected Lock setDocumentLock(Lock lock) {
        // lock manager can set the lock even on a recently created and unsaved document
        throw new UnsupportedOperationException();
    }

    @Override
    protected Lock removeDocumentLock(String owner) {
        // lock manager can remove the lock even on a recently created and unsaved document
        throw new UnsupportedOperationException();
    }

}
