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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLDocumentVersion.VersionNotModifiableException;

/**
 * @author Florent Guillaume
 */
public class SQLDocumentLive extends SQLComplexProperty implements SQLDocument {

    private static final Log log = LogFactory.getLog(SQLDocumentLive.class);

    /** Mixin types, updated when facets change. */
    protected final List<CompositeType> mixinTypes;

    protected SQLDocumentLive(Node node, ComplexType type,
            List<CompositeType> mixinTypes, SQLSession session, boolean readonly) {
        super(node, type, session, readonly);
        this.mixinTypes = mixinTypes;
    }

    /*
     * ----- SQLDocument -----
     */

    // getNode in SQLComplexProperty

    // checkWritable in SQLBaseProperty

    @Override
    public org.nuxeo.ecm.core.model.Property getACLProperty()
            throws DocumentException {
        return session.makeACLProperty(getNode());
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Document -----
     */

    // public String getName(); from SQLComplexProperty
    //
    @Override
    public DocumentType getType() {
        return (DocumentType) type;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public boolean isFolder() {
        return type == null // null document
                || ((DocumentType) type).isFolder();
    }

    @Override
    public String getUUID() {
        return getNode().getId().toString();
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
    public Calendar getLastModified() {
        throw new UnsupportedOperationException("unused");
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public Repository getRepository() {
        return session.getRepository();
    }

    @Override
    public void remove() throws DocumentException {
        session.remove(getNode());
    }

    @Override
    public void save() throws DocumentException {
        session.save();
    }

    /**
     * Reads into the {@link DocumentPart} the values from this
     * {@link SQLDocument}.
     */
    @Override
    public void readDocumentPart(DocumentPart dp) throws Exception {
        for (Property property : dp) {
            property.init((Serializable) getPropertyValue(property.getName()));
        }
    }

    @Override
    public org.nuxeo.ecm.core.model.Property getProperty(String name)
            throws DocumentException {
        return session.makeProperty(getNode(), name, (ComplexType) type,
                mixinTypes, readonly);
    }

    /**
     * Writes into this {@link SQLDocument} the values from the
     * {@link DocumentPart}.
     */
    @Override
    public void writeDocumentPart(DocumentPart dp) throws Exception {
        for (Property property : dp) {
            String name = property.getName();
            Serializable value = property.getValueForWrite();
            try {
                setPropertyValue(name, value);
            } catch (VersionNotModifiableException e) {
                // hack, only dublincore is allowed to change and
                // it contains only scalars and arrays
                // cf also SQLSimpleProperty.VERSION_WRITABLE_PROPS
                if (!name.startsWith("dc:")) {
                    throw e;
                }
                // ignore if value is unchanged
                Object oldValue = getPropertyValue(name);
                if (same(value, oldValue)) {
                    continue;
                }
                if (value == null || oldValue == null
                        || !sameArray(value, oldValue)) {
                    throw e;
                }
            }
        }
        clearDirtyFlags(dp);
    }

    protected static boolean same(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    protected static boolean sameArray(Object a, Object b) {
        Class<?> acls = a.getClass();
        Class<?> bcls = b.getClass();
        if (!acls.isArray() || !bcls.isArray()
                || Array.getLength(a) != Array.getLength(b)) {
            return false;
        }
        for (int i = 0; i < Array.getLength(a); i++) {
            if (!same(Array.get(a, i), Array.get(b, i))) {
                return false;
            }
        }
        return true;
    }

    protected static void clearDirtyFlags(Property property) {
        if (property.isContainer()) {
            for (Property p : property) {
                clearDirtyFlags(p);
            }
        }
        property.clearDirtyFlags();
    }

    protected static final Map<String, String> systemPropNameMap;

    static {
        systemPropNameMap = new HashMap<String, String>();
        systemPropNameMap.put(FULLTEXT_JOBID_SYS_PROP,
                Model.FULLTEXT_JOBID_PROP);
    }

    @Override
    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {
        String propertyName;
        if (name.startsWith(BINARY_TEXT_SYS_PROP)) {
            // system property for specific fulltext indices
            propertyName = name.replace(BINARY_TEXT_SYS_PROP,
                    Model.FULLTEXT_BINARYTEXT_PROP);
        } else {
            propertyName = systemPropNameMap.get(name);
        }
        if (propertyName == null) {
            throw new DocumentException("Unknown system property: " + name);
        }
        getProperty(propertyName).setValue(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        String propertyName = systemPropNameMap.get(name);
        if (propertyName == null) {
            throw new DocumentException("Unknown system property: " + name);
        }
        Object value = getProperty(propertyName).getValue();
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
            return getString(Model.MISC_LIFECYCLE_POLICY_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get policy", e);
        }
    }

    @Override
    public void setLifeCyclePolicy(String policy) throws LifeCycleException {
        try {
            setString(Model.MISC_LIFECYCLE_POLICY_PROP, policy);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set policy", e);
        }
    }

    @Override
    public String getLifeCycleState() throws LifeCycleException {
        try {
            return getString(Model.MISC_LIFECYCLE_STATE_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get state", e);
        }
    }

    @Override
    public void setCurrentLifeCycleState(String state)
            throws LifeCycleException {
        try {
            setString(Model.MISC_LIFECYCLE_STATE_PROP, state);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set state", e);
        }
    }

    @Override
    public boolean followTransition(String transition)
            throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        service.followTransition(this, transition);
        return true;
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
        String id = getString(Model.MAIN_BASE_VERSION_PROP);
        if (id == null) {
            // shouldn't happen
            return null;
        }
        return session.getDocumentByUUID(id);
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
        return !getBoolean(Model.MAIN_CHECKED_IN_PROP);
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
        return getString(Model.VERSION_LABEL_PROP);
    }

    @Override
    public String getCheckinComment() throws DocumentException {
        return getString(Model.VERSION_DESCRIPTION_PROP);
    }

    @Override
    public Document getWorkingCopy() throws DocumentException {
        return this;
    }

    @Override
    public Calendar getVersionCreationDate() throws DocumentException {
        return (Calendar) getProperty(Model.VERSION_CREATED_PROP).getValue();
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
    public boolean hasVersions() throws DocumentException {
        log.error("hasVersions unimplemented, returning false");
        return false;
        // XXX TODO
        // throw new UnsupportedOperationException();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.DocumentContainer -----
     */

    @Override
    public Document resolvePath(String path) throws DocumentException {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        if (path.length() == 0) {
            return this;
        }
        // this API doesn't take absolute paths
        if (path.startsWith("/")) {
            // TODO log warning
            path = path.substring(1);
        }
        return session.resolvePath(getNode(), path);
    }

    @Override
    public Document getChild(String name) throws DocumentException {
        return session.getChild(getNode(), name);
    }

    @Override
    public Iterator<Document> getChildren() throws DocumentException {
        return getChildren(0);
    }

    @Override
    public DocumentIterator getChildren(int start) throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        List<Document> children = session.getChildren(getNode());
        if (start < 0) {
            throw new IllegalArgumentException(String.valueOf(start));
        }
        if (start >= children.size()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        return new SQLDocumentListIterator(children.subList(start,
                children.size()));
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
    public void removeChild(String name) throws DocumentException {
        if (!isFolder()) {
            return; // ignore non folder documents XXX urgh
        }
        Document doc = getChild(name);
        doc.remove();
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
        try {
            boolean added = getNode().addMixinType(facet);
            if (added) {
                mixinTypes.add(session.getTypeManager().getFacet(facet));
            }
            return added;
        } catch (IllegalArgumentException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public boolean removeFacet(String facet) throws DocumentException {
        boolean removed = getNode().removeMixinType(facet);
        if (removed) {
            for (Iterator<CompositeType> it = mixinTypes.iterator(); it.hasNext();) {
                if (it.next().getName().equals(facet)) {
                    it.remove();
                    break;
                }
            }
        }
        return removed;
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
        if (other instanceof SQLDocumentLive) {
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
