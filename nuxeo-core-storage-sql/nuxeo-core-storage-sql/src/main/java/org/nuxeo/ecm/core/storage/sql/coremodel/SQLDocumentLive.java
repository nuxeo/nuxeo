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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.runtime.api.Framework;

public class SQLDocumentLive implements SQLDocument {

    protected final Node node;

    protected final Type type;

    protected SQLSession session;

    /** Proxy-induced types. */
    protected final List<Schema> proxySchemas;

    /**
     * Read-only flag, used to allow/disallow writes on versions.
     */
    protected boolean readonly;

    protected SQLDocumentLive(Node node, ComplexType type, SQLSession session,
            boolean readonly) {
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
    public Document getParent() throws DocumentException {
        return session.getParent(getNode());
    }

    @Override
    public String getPath() throws DocumentException {
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
    public void remove() throws DocumentException {
        session.remove(getNode());
    }

    /**
     * Reads into the {@link DocumentPart} the values from this
     * {@link SQLDocument}.
     */
    @Override
    public void readDocumentPart(DocumentPart dp) throws PropertyException {
        session.readComplexProperty((ComplexProperty) dp, getNode());
    }

    @Override
    public Map<String, Serializable> readPrefetch(ComplexType complexType,
            Set<String> xpaths) throws PropertyException {
        return session.readPrefetch(getNode(), complexType, xpaths);
    }

    /**
     * Writes into this {@link SQLDocument} the values from the
     * {@link DocumentPart}.
     */
    @Override
    public void writeDocumentPart(DocumentPart dp) throws PropertyException {
        session.writeComplexProperty((ComplexProperty) dp, getNode(), this);
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

    @Override
    public Serializable getPropertyValue(String name) throws DocumentException {
        try {
            return getNode().getSimpleProperty(name).getValue();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public void setPropertyValue(String name, Serializable value)
            throws DocumentException {
        try {
            getNode().setSimpleProperty(name, value);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    protected static final Map<String, String> systemPropNameMap;

    static {
        systemPropNameMap = new HashMap<String, String>();
        systemPropNameMap.put(FULLTEXT_JOBID_SYS_PROP,
                Model.FULLTEXT_JOBID_PROP);
    }

    @Override
    public void setSystemProp(String name, Serializable value)
            throws DocumentException {
        String propertyName;
        if (name.startsWith(SIMPLE_TEXT_SYS_PROP)) {
            propertyName = name.replace(SIMPLE_TEXT_SYS_PROP,
                    Model.FULLTEXT_SIMPLETEXT_PROP);
        } else if (name.startsWith(BINARY_TEXT_SYS_PROP)) {
            propertyName = name.replace(BINARY_TEXT_SYS_PROP,
                    Model.FULLTEXT_BINARYTEXT_PROP);
        } else {
            propertyName = systemPropNameMap.get(name);
        }
        if (propertyName == null) {
            throw new DocumentException("Unknown system property: " + name);
        }
        setPropertyValue(propertyName, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
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

    /*
     * ----- LifeCycle -----
     */

    @Override
    public String getLifeCyclePolicy() throws LifeCycleException {
        try {
            return (String) getPropertyValue(Model.MISC_LIFECYCLE_POLICY_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get policy", e);
        }
    }

    @Override
    public void setLifeCyclePolicy(String policy) throws LifeCycleException {
        try {
            setPropertyValue(Model.MISC_LIFECYCLE_POLICY_PROP, policy);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set policy", e);
        }
    }

    @Override
    public String getLifeCycleState() throws LifeCycleException {
        try {
            return (String) getPropertyValue(Model.MISC_LIFECYCLE_STATE_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get state", e);
        }
    }

    @Override
    public void setCurrentLifeCycleState(String state)
            throws LifeCycleException {
        try {
            setPropertyValue(Model.MISC_LIFECYCLE_STATE_PROP, state);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set state", e);
        }
    }

    @Override
    public void followTransition(String transition)
            throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        service.followTransition(this, transition);
    }

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

    /*
     * ----- org.nuxeo.ecm.core.model.Lockable -----
     */

    @Override
    public Lock getLock() throws DocumentException {
        return session.getLock(getNode());
    }

    @Override
    public Lock setLock(Lock lock) throws DocumentException {
        return session.setLock(getNode(), lock);
    }

    @Override
    public Lock removeLock(String owner) throws DocumentException {
        return session.removeLock(getNode(), owner);
    }

    /*
     * ----- org.nuxeo.ecm.core.versioning.VersionableDocument -----
     */

    @Override
    public boolean isVersion() {
        return false;
    }

    @Override
    public Document getBaseVersion() throws DocumentException {
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
    public String getVersionSeriesId() throws DocumentException {
        return getUUID();
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        return this;
    }

    @Override
    public Document checkIn(String label, String checkinComment)
            throws DocumentException {
        return session.checkIn(getNode(), label, checkinComment);
    }

    @Override
    public void checkOut() throws DocumentException {
        session.checkOut(getNode());
    }

    @Override
    public boolean isCheckedOut() throws DocumentException {
        return !Boolean.TRUE.equals(getPropertyValue(Model.MAIN_CHECKED_IN_PROP));
    }

    @Override
    public boolean isMajorVersion() throws DocumentException {
        return false;
    }

    @Override
    public boolean isLatestVersion() throws DocumentException {
        return false;
    }

    @Override
    public boolean isLatestMajorVersion() throws DocumentException {
        return false;
    }

    @Override
    public boolean isVersionSeriesCheckedOut() throws DocumentException {
        return isCheckedOut();
    }

    @Override
    public String getVersionLabel() throws DocumentException {
        return (String) getPropertyValue(Model.VERSION_LABEL_PROP);
    }

    @Override
    public String getCheckinComment() throws DocumentException {
        return (String) getPropertyValue(Model.VERSION_DESCRIPTION_PROP);
    }

    @Override
    public Document getWorkingCopy() throws DocumentException {
        return this;
    }

    @Override
    public Calendar getVersionCreationDate() throws DocumentException {
        return (Calendar) getPropertyValue(Model.VERSION_CREATED_PROP);
    }

    @Override
    public void restore(Document version) throws DocumentException {
        if (!version.isVersion()) {
            throw new DocumentException("Cannot restore a non-version: "
                    + version);
        }
        session.restore(getNode(), ((SQLDocument) version).getNode());
    }

    @Override
    public List<String> getVersionsIds() throws DocumentException {
        String versionSeriesId = getVersionSeriesId();
        Collection<Document> versions = session.getVersions(versionSeriesId);
        List<String> ids = new ArrayList<String>(versions.size());
        for (Document version : versions) {
            ids.add(version.getUUID());
        }
        return ids;
    }

    @Override
    public Document getVersion(String label) throws DocumentException {
        String versionSeriesId = getVersionSeriesId();
        return session.getVersionByLabel(versionSeriesId, label);
    }

    @Override
    public List<Document> getVersions() throws DocumentException {
        String versionSeriesId = getVersionSeriesId();
        return session.getVersions(versionSeriesId);
    }

    @Override
    public Document getLastVersion() throws DocumentException {
        String versionSeriesId = getVersionSeriesId();
        return session.getLastVersion(versionSeriesId);
    }

    @Override
    public Document getChild(String name) throws DocumentException {
        return session.getChild(getNode(), name);
    }

    @Override
    public Iterator<Document> getChildren() throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        List<Document> children = session.getChildren(getNode());
        return new SQLDocumentListIterator(children);
    }

    @Override
    public List<String> getChildrenIds() throws DocumentException {
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
    public boolean hasChild(String name) throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChild(getNode(), name);
    }

    @Override
    public boolean hasChildren() throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChildren(getNode());
    }

    @Override
    public Document addChild(String name, String typeName)
            throws DocumentException {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        return session.addChild(getNode(), name, null, typeName);
    }

    @Override
    public void orderBefore(String src, String dest) throws DocumentException {
        SQLDocument srcDoc = (SQLDocument) getChild(src);
        if (srcDoc == null) {
            throw new DocumentException("Document " + this + " has no child: "
                    + src);
        }
        SQLDocument destDoc;
        if (dest == null) {
            destDoc = null;
        } else {
            destDoc = (SQLDocument) getChild(dest);
            if (destDoc == null) {
                throw new DocumentException("Document " + this
                        + " has no child: " + dest);
            }
        }
        session.orderBefore(getNode(), srcDoc.getNode(), destDoc == null ? null
                : destDoc.getNode());
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
    public boolean addFacet(String facet) throws DocumentException {
        return session.addMixinType(getNode(), facet);
    }

    @Override
    public boolean removeFacet(String facet) throws DocumentException {
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
    public void setTargetDocument(Document target) throws DocumentException {
        throw new DocumentException();
    }

}

class SQLDocumentListIterator implements DocumentIterator {

    private final int size;

    private final Iterator<Document> iterator;

    public SQLDocumentListIterator(List<Document> list) {
        size = list.size();
        iterator = list.iterator();
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Document next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
