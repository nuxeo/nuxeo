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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author Florent Guillaume
 */
public class SQLModelDocument implements Document {

    private static final Log log = LogFactory.getLog(SQLModelDocument.class);

    /** The session. */
    private SQLModelSession session;

    /** The underlying SQL node. */
    private Node node;

    // we store lock state on the document because it is frequently used
    // (on each permission check)
    // private String lock;

    /**
     * Constructs a document that wraps the given JCR node.
     * <p>
     * Do not use this ctor from outside!! Use JCRSession.newDocument instead -
     * otherwise proxy docs will not work.
     *
     * @param session the current session
     * @param node the JCR node to wrap
     * @throws StorageException if any JCR exception occurs
     */
    SQLModelDocument(Node node, SQLModelSession session)
            throws StorageException {
        this.node = node;
        this.session = session;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Document -----
     */

    public org.nuxeo.ecm.core.model.Session getSession() {
        return session;
    }

    public boolean isFolder() {
        return node.getType().isFolder();
    }

    public String getName() {
        return node.getName();
    }

    public String getUUID() {
        return node.getId().toString();
    }

    public Document getParent() throws DocumentException {
        return session.getParent(node);
    }

    public String getPath() throws DocumentException {
        return session.getPath(node);
    }

    public Calendar getLastModified() {
        throw new UnsupportedOperationException("unused");
    }

    public DocumentType getType() {
        return node.getType();
    }

    public boolean isProxy() {
        return false;
    }

    public Repository getRepository() {
        return session.getRepository();
    }

    public void remove() throws DocumentException {
        session.removeNode(node);
    }

    public void save() throws DocumentException {
        session.save();
    }

    /**
     * Checks if the document is "dirty", which means that a change on it was
     * made since the last time a snapshot of it was done (for publishing).
     */
    public boolean isDirty() throws DocumentException {
        return getBoolean(Model.SYSTEM_DIRTY_PROP);
    }

    /**
     * Marks the document "dirty", which means that a change on it was made
     * since the last time a snapshot of it was done (for publishing).
     */
    public void setDirty(boolean value) throws DocumentException {
        setBoolean(Model.SYSTEM_DIRTY_PROP, value);
    }

    public void readDocumentPart(DocumentPart dp) throws Exception {

        // proxy document is forwarding props to refered doc
        // Node parent = isProxy() ? ((JCRDocumentProxy)doc).getTargetNode() :
        // getNode();

        String schemaName = dp.getSchema().getName();
        for (org.nuxeo.ecm.core.api.model.Property property : dp) {
            property.init((Serializable) getPropertyValue(property.getName()));
        }
    }

    public void writeDocumentPart(DocumentPart dp) throws Exception {
        for (org.nuxeo.ecm.core.api.model.Property property : dp) {
            setPropertyValue(property.getName(), property.getValue());
        }
        dp.clearDirtyFlags();
    }

    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Lifecycle -----
     */

    public boolean followTransition(String transition)
            throws LifeCycleException {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        throw new UnsupportedOperationException();
    }

    public String getCurrentLifeCycleState() throws LifeCycleException {
        try {
            return getString(Model.SYSTEM_LIFECYCLE_STATE_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException(e.getMessage(), e);
        }
    }

    public String getLifeCyclePolicy() throws LifeCycleException {
        try {
            return getString(Model.SYSTEM_LIFECYCLE_POLICY_PROP);
        } catch (DocumentException e) {
            throw new LifeCycleException(e.getMessage(), e);
        }
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
        return session.resolvePath(node, path);
    }

    public Document getChild(String name) throws DocumentException {
        return session.getChild(node, name);
    }

    public Iterator<Document> getChildren() throws DocumentException {
        return getChildren(0);
    }

    public DocumentIterator getChildren(int start) throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        List<Document> children = session.getChildren(node);
        if (start < 0) {
            throw new IllegalArgumentException(String.valueOf(start));
        }
        if (start >= children.size()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        return new DocumentListIterator(
                children.subList(start, children.size()));
    }

    public List<String> getChildrenIds() throws DocumentException {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        // not optimized as this method doesn't seem to be used
        List<Document> children = session.getChildren(node);
        List<String> ids = new ArrayList<String>(children.size());
        for (Document child : children) {
            ids.add(child.getUUID());
        }
        return ids;
    }

    public boolean hasChild(String name) throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChild(node, name);
    }

    public boolean hasChildren() throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        return session.hasChildren(node);
    }

    public Document addChild(String name, String typeName)
            throws DocumentException {
        if (!isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        return session.addChild(node, name, typeName);
    }

    public void orderBefore(String src, String dest) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void removeChild(String name) throws DocumentException {
        if (!isFolder()) {
            return; // ignore non folder documents XXX urgh
        }
        Document doc = getChild(name);
        doc.remove();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.PropertyContainer -----
     */

    public Property getProperty(String name) throws DocumentException {
        try {
            return node.getProperty(name);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    public String getString(String name) throws DocumentException {
        return (String) getProperty(name).getValue();
    }

    public boolean getBoolean(String name) throws DocumentException {
        Boolean value = (Boolean) getProperty(name).getValue();
        return value == null ? false : value.booleanValue();
    }

    public Blob getContent(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Calendar getDate(String name) throws DocumentException {
        return (Calendar) getProperty(name).getValue();
    }

    public double getDouble(String name) throws DocumentException {
        Double value = (Double) getProperty(name).getValue();
        return value == null ? 0 : value.doubleValue();
    }

    public long getLong(String name) throws DocumentException {
        Long value = (Long) getProperty(name).getValue();
        return value == null ? 0 : value.longValue();
    }

    public Object getPropertyValue(String name) throws DocumentException {
        return getProperty(name).getValue();
    }

    public Collection<Property> getProperties() throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public List<String> getDirtyFields() {
        throw new UnsupportedOperationException("unused");
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public void importFlatMap(Map<String, Object> map) throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public boolean isPropertySet(String path) throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public void removeProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException("unused");
    }

    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        // TODO check constraints
        try {
            getProperty(name).setValue(value);
            // TODO mark dirty fields
        } catch (RuntimeException e) {
            log.error("RuntimeException setting value: " + value +
                    " on property: " + name);
            throw e;
        } catch (DocumentException e) {
            // we log a debugging message here as it is a point where the
            // property name is known
            log.error("Error setting value: " + value + " on property: " + name);
            throw e;
        }
    }

    public void setBoolean(String name, boolean value) throws DocumentException {
        setPropertyValue(name, Boolean.valueOf(value));
    }

    public void setContent(String name, Blob value) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setDate(String name, Calendar value) throws DocumentException {
        setPropertyValue(name, value);
    }

    public void setDouble(String name, double value) throws DocumentException {
        setPropertyValue(name, Double.valueOf(value));
    }

    public void setLong(String name, long value) throws DocumentException {
        setPropertyValue(name, Long.valueOf(value));
    }

    public void setString(String name, String value) throws DocumentException {
        setPropertyValue(name, value);
    }

    /*
     * ----- org.nuxeo.ecm.core.versioning.VersionableDocument -----
     */

    public boolean isVersion() {
        return false;
    }

    public Document getSourceDocument() {
        return this;
    }

    public void checkIn(String label) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void checkIn(String label, String description)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void checkOut() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public boolean isCheckedOut() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void restore(String label) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public List<String> getVersionsIds() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public Document getVersion(String label) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public DocumentVersionIterator getVersions() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public DocumentVersion getLastVersion() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public boolean hasVersions() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Lockable -----
     */

    // TODO: optimize this since it is used in permission checks
    public boolean isLocked() throws DocumentException {
        return getLock() != null;
    }

    public String getLock() throws DocumentException {
        return null;
        // throw new UnsupportedOperationException();
    }

    public void setLock(String key) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public String unlock() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- -----
     */

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getName() + ',' + getUUID() +
                ')';
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof SQLModelDocument) {
            return equals((SQLModelDocument) other);
        }
        return false;
    }

    private boolean equals(SQLModelDocument other) {
        return node.getId() == other.node.getId();
    }

    @Override
    public int hashCode() {
        return node.getId().hashCode();
    }

}

class DocumentListIterator implements DocumentIterator {

    private final int size;

    private final Iterator<Document> iterator;

    public DocumentListIterator(List<Document> list) {
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