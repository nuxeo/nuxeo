/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Constants;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentException;
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
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author Florent Guillaume
 */
public class SQLDocument extends SQLComplexProperty implements Document {

    protected static final String BINARY_TEXT_SYS_PROP = "binaryText";

    private static final Log log = LogFactory.getLog(SQLDocument.class);

    // cache of the lock state, for efficiency
    protected String lock;

    protected SQLDocument(Node node, ComplexType type, SQLSession session,
            boolean readonly) {
        super(node, type, session, readonly);
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

    public Session getSession() {
        return session;
    }

    public boolean isFolder() {
        return _isFolder();
    }

    protected boolean _isFolder() {
        return ((DocumentType) type).isFolder();
    }

    public String getUUID() {
        return getHierarchyNode().getId().toString();
    }

    public Document getParent() throws DocumentException {
        return session.getParent(getHierarchyNode());
    }

    public String getPath() throws DocumentException {
        return session.getPath(getHierarchyNode());
    }

    public Calendar getLastModified() {
        throw new UnsupportedOperationException("unused");
    }

    public boolean isProxy() {
        return false;
    }

    public Repository getRepository() {
        return session.getRepository();
    }

    public void remove() throws DocumentException {
        session.remove(getHierarchyNode());
    }

    public void save() throws DocumentException {
        session.save();
    }

    /**
     * Checks if the document is "dirty", which means that a change on it was
     * made since the last time a snapshot of it was done (for publishing).
     */
    public boolean isDirty() throws DocumentException {
        Boolean value = (Boolean) getProperty(Model.MISC_DIRTY_PROP).getValue();
        // not set implies new => dirty
        return value == null ? true : value.booleanValue();
    }

    /**
     * Marks the document "dirty", which means that a change on it was made
     * since the last time a snapshot of it was done (for publishing).
     */
    public void setDirty(boolean value) throws DocumentException {
        setBoolean(Model.MISC_DIRTY_PROP, value);
    }

    /**
     * Reads into the {@link DocumentPart} the values from this
     * {@link SQLDocument}.
     */
    public void readDocumentPart(DocumentPart dp) throws Exception {
        for (Property property : dp) {
            property.init((Serializable) getPropertyValue(property.getName()));
        }
    }

    /**
     * Writes into this {@link SQLDocument} the values from the
     * {@link DocumentPart}.
     */
    public void writeDocumentPart(DocumentPart dp) throws Exception {
        for (Property property : dp) {
            setPropertyValue(property.getName(), property.getValueForWrite());
            //setPropertyValue(property.getName(), property.getValue());
        }
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

    protected static final Map<String, String> systemPropNameMap;

    static {
        systemPropNameMap = new HashMap<String, String>();
        systemPropNameMap.put("WfinProgress", Model.MISC_WF_IN_PROGRESS_PROP);
        systemPropNameMap.put("WfIncOption", Model.MISC_WF_INC_OPTION_PROP);
        systemPropNameMap.put(BINARY_TEXT_SYS_PROP,
                Model.FULLTEXT_BINARYTEXT_PROP);
    }

    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {
        String propertyName = systemPropNameMap.get(name);
        if (propertyName == null) {
            throw new DocumentException("Unknown system property: " + name);
        }
        getProperty(propertyName).setValue(value);
    }

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

    public String getLifeCyclePolicy() throws LifeCycleException {
        try {
            return getString(Model.MISC_LIFECYCLE_POLICY_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get policy", e);
        }
    }

    public void setLifeCyclePolicy(String policy) throws LifeCycleException {
        try {
            setString(Model.MISC_LIFECYCLE_POLICY_PROP, policy);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set policy", e);
        }
    }

    public String getCurrentLifeCycleState() throws LifeCycleException {
        try {
            return getString(Model.MISC_LIFECYCLE_STATE_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to get state", e);
        }
    }

    public void setCurrentLifeCycleState(String state)
            throws LifeCycleException {
        try {
            setString(Model.MISC_LIFECYCLE_STATE_PROP, state);
        } catch (DocumentException e) {
            throw new LifeCycleException("Failed to set state", e);
        }
    }

    public boolean followTransition(String transition)
            throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        if (service == null) {
            throw new LifeCycleException("LifeCycleService not available");
        }
        service.followTransition(this, transition);
        return true;
    }

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
        return lifeCycle.getAllowedStateTransitionsFrom(getCurrentLifeCycleState());
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Lockable -----
     */

    public boolean isLocked() throws DocumentException {
        return getLock() != null;
    }

    public String getLock() throws DocumentException {
        if (lock != null) {
            return lock == Constants.EMPTY_STRING ? null : lock;
        }
        String l = getString(Model.LOCK_PROP);
        lock = l == null ? Constants.EMPTY_STRING : l;
        return l;
    }

    public void setLock(String key) throws DocumentException {
        if (key == null) {
            throw new IllegalArgumentException("Lock key cannot be null");
        }
        if (isLocked()) {
            throw new DocumentException("Document already locked");
        }
        setString(Model.LOCK_PROP, key);
        lock = key;
    }

    public String unlock() throws DocumentException {
        String l = getLock();
        setString(Model.LOCK_PROP, null);
        lock = Constants.EMPTY_STRING;
        return l;
    }

    /*
     * ----- org.nuxeo.ecm.core.versioning.VersionableDocument -----
     */

    public boolean isVersion() {
        return false;
    }

    public Document getSourceDocument() throws DocumentException {
        return this;
    }

    public void checkIn(String label) throws DocumentException {
        checkIn(label, null);
    }

    public void checkIn(String label, String description)
            throws DocumentException {
        session.checkIn(getHierarchyNode(), label, description);
    }

    public void checkOut() throws DocumentException {
        session.checkOut(getHierarchyNode());
    }

    public boolean isCheckedOut() throws DocumentException {
        return !getBoolean(Model.MAIN_CHECKED_IN_PROP);
    }

    public void restore(String label) throws DocumentException {
        session.restoreByLabel(getHierarchyNode(), label);
    }

    public List<String> getVersionsIds() throws DocumentException {
        Collection<DocumentVersion> versions = session.getVersions(getHierarchyNode());
        List<String> ids = new ArrayList<String>(versions.size());
        for (DocumentVersion version : versions) {
            ids.add(version.getUUID());
        }
        return ids;
    }

    public Document getVersion(String label) throws DocumentException {
        return session.getVersionByLabel(getHierarchyNode(), label);
    }

    public DocumentVersionIterator getVersions() throws DocumentException {
        return new SQLDocumentVersionIterator(
                session.getVersions(getHierarchyNode()));
    }

    public DocumentVersion getLastVersion() throws DocumentException {
        return session.getLastVersion(getHierarchyNode());
    }

    public boolean hasVersions() throws DocumentException {
        log.error("hasVersions unimplemented, returning false");
        return false;
        // XXX TODO
        // throw new UnsupportedOperationException();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.DocumentContainer -----
     */

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
        return session.resolvePath(getHierarchyNode(), path);
    }

    public Document getChild(String name) throws DocumentException {
        return _getChild(name);
    }

    protected Document _getChild(String name) throws DocumentException {
        return session.getChild(getHierarchyNode(), name);
    }

    public Iterator<Document> getChildren() throws DocumentException {
        return _getChildren();
    }

    protected Iterator<Document> _getChildren() throws DocumentException {
        return getChildren(0);
    }

    public DocumentIterator getChildren(int start) throws DocumentException {
        return _getChildren(start);
    }

    protected DocumentIterator _getChildren(int start) throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        List<Document> children = session.getChildren(getHierarchyNode());
        if (start < 0) {
            throw new IllegalArgumentException(String.valueOf(start));
        }
        if (start >= children.size()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        return new SQLDocumentListIterator(children.subList(start,
                children.size()));
    }

    public List<String> getChildrenIds() throws DocumentException {
        return _getChildrenIds();
    }

    protected List<String> _getChildrenIds() throws DocumentException {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        // not optimized as this method doesn't seem to be used
        List<Document> children = session.getChildren(getHierarchyNode());
        List<String> ids = new ArrayList<String>(children.size());
        for (Document child : children) {
            ids.add(child.getUUID());
        }
        return ids;
    }

    public boolean hasChild(String name) throws DocumentException {
        return _hasChild(name);
    }

    protected boolean _hasChild(String name) throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChild(getHierarchyNode(), name);
    }

    public boolean hasChildren() throws DocumentException {
        return _hasChildren();
    }

    protected boolean _hasChildren() throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChildren(getHierarchyNode());
    }

    public Document addChild(String name, String typeName)
            throws DocumentException {
        return _addChild(name, typeName);
    }

    protected Document _addChild(String name, String typeName)
            throws DocumentException {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        // TODO pos
        return session.addChild(getHierarchyNode(), name, null, typeName);
    }

    public void orderBefore(String src, String dest) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void removeChild(String name) throws DocumentException {
        _removeChild(name);
    }

    protected void _removeChild(String name) throws DocumentException {
        if (!isFolder()) {
            return; // ignore non folder documents XXX urgh
        }
        Document doc = getChild(name);
        doc.remove();
    }

    /*
     * ----- PropertyContainer inherited from SQLComplexProperty -----
     */

    /*
     * ----- internal for SQLSecurityManager -----
     */

    protected org.nuxeo.ecm.core.model.Property getACLProperty()
            throws DocumentException {
        return session.makeACLProperty(getHierarchyNode());
    }

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
        if (other instanceof SQLDocument) {
            return equals((SQLDocument) other);
        }
        return false;
    }

    private boolean equals(SQLDocument other) {
        return getHierarchyNode().getId() == other.getHierarchyNode().getId();
    }

    @Override
    public int hashCode() {
        return getHierarchyNode().getId().hashCode();
    }

}

class SQLDocumentListIterator implements DocumentIterator {

    private final int size;

    private final Iterator<Document> iterator;

    public SQLDocumentListIterator(List<Document> list) {
        size = list.size();
        iterator = list.iterator();
    }

    public long getSize() {
        return size;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Document next() {
        return iterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}

class SQLDocumentVersionIterator implements DocumentVersionIterator {

    private final Iterator<DocumentVersion> iterator;

    public SQLDocumentVersionIterator(Collection<DocumentVersion> list) {
        iterator = list.iterator();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public DocumentVersion next() {
        return iterator.next();
    }

    public DocumentVersion nextDocumentVersion() {
        return iterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
