/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.model.AbstractDocument;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.core.model.NoSuchPropertyException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.properties.PropertyFactory;
import org.nuxeo.ecm.core.repository.jcr.versioning.Versioning;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.TypeException;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * An implementation of the Document interface over a JackRabbit node.
 * <p>
 * Please note that this is not a JCR implementation of the Document, but rather
 * a JackRabbit one - this way we can benefit from JackRabbit internal API to
 * optimize some operations.
 * <p>
 * Thus, the underlying node of the Document is of type {@link Node} and not
 * {@link Node}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JCRDocument extends AbstractDocument implements JCRNodeProxy {

    private static final Log log = LogFactory.getLog(JCRDocument.class);

    // the session
    protected JCRSession session;

    // the document type
    protected DocumentType type;

    // underlying JCR node
    protected Node node;

    // we store lock state on the document because it is frequently used
    // (on each permission check)
    protected String lock;

    // XXX AT: log dirty fields for NXP-666 compatibility
    protected final List<String> dirtyFields;

    /**
     * Empty constructor needed in overriding classes.
     */
    protected JCRDocument() {
        dirtyFields = new ArrayList<String>();
    }

    /**
     * Constructs a document that wraps the given JCR node.
     * <p>
     * Do not use this ctor from outside!! Use JCRSession.newDocument instead -
     * otherwise proxy docs will not work.
     *
     * @param session the current session
     * @param node the JCR node to wrap
     * @throws RepositoryException if any JCR exception occurs
     */
    JCRDocument(JCRSession session, Node node) throws RepositoryException {
        this.session = session;
        this.node = node;
        type = session.getDocumentType(node); // TODO lazy load
        dirtyFields = new ArrayList<String>();
    }

    public Session getSession() {
        return session;
    }

    public final JCRSession jcrSession() {
        return session;
    }

    public boolean isFolder() {
        return type.isFolder();
    }

    public String getName() throws DocumentException {
        try {
            return node.getName();
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public String getUUID() throws DocumentException {
        try {
            return node.getUUID();
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public Document getParent() throws DocumentException {
        try {
            Node parentNode = ModelAdapter.getParentNode(node);
            if (parentNode == null) {
                return null;
            }
            return session.newDocument(parentNode);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to get parent for document "
                    + getPath(), e);
        }
    }

    public String getPath() throws DocumentException {
        try {
            // return ModelAdapter.getPath(workspace.getName(), node);
            return ModelAdapter.getPath(null, node);
        } catch (Exception e) {
            throw new DocumentException("Failed to compute path for " + this, e);
        }
    }

    public Calendar getLastModified() throws DocumentException {
        try {
            return node.getProperty("jcr:lastModified").getDate();
        } catch (RepositoryException e) {
            throw new DocumentException("failed to get last modified property",
                    e);
        }
    }

    public DocumentType getType() {
        return type;
    }

    public Document resolvePath(String relPath) throws DocumentException {
        if (relPath.length() == 0) {
            return this;
        }
        return session.resolvePath(node, relPath);
    }

    public Document getChild(String name) throws DocumentException {
        try {
            return session.newDocument(ModelAdapter.getChild(node, name));
        } catch (PathNotFoundException e) {
            throw new NoSuchDocumentException(name, e);
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public Iterator<Document> getChildren() throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        try {
            assertIsFolder();
            return new JCRDocumentIterator(session, node);
        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    public DocumentIterator getChildren(int start) throws DocumentException {
        if (!isFolder()) {
            return EmptyDocumentIterator.INSTANCE;
        }
        try {
            assertIsFolder();
            return new JCRDocumentIterator(session, node, start);
        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    public List<String> getChildrenIds() throws DocumentException {
        if (!isFolder()) {
            return Collections.emptyList();
        }
        try {
            NodeIterator it = ModelAdapter.getContainerNode(node).getNodes();
            List<String> ids = new ArrayList<String>((int) it.getSize());
            while (it.hasNext()) {
                ids.add(it.nextNode().getUUID());
            }
            return ids;
        } catch (Exception e) {
            throw new DocumentException(e);
        }
    }

    public boolean hasChild(String name) throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        try {
            return ModelAdapter.hasChild(node, name);
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasChildren() throws DocumentException {
        if (!isFolder()) {
            return false;
        }
        try {
            return ModelAdapter.hasChildren(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Document addChild(String name, String typeName)
            throws DocumentException {
        try {
            assertIsFolder();
            Node child = ModelAdapter.addChild(node, name, typeName);
            Document doc = session.newDocument(child);
            if (doc.getType().isUnstructured()) { // unstructured document ->
                // add unstructured mixin
                // type
                ModelAdapter.setUnstructured(child);
            }
            return doc;
        } catch (Exception e) {
            throw new DocumentException("Failed to create document " + name, e);
        }
    }

    public void removeChild(String name) throws DocumentException {
        if (!isFolder()) {
            return; // ignore non folder documents
        }
        Document doc = getChild(name);
        doc.remove();
    }

    public void orderBefore(String src, String dest) throws DocumentException {
        assertIsFolder();
        try {
            ModelAdapter.getContainerNode(node).orderBefore(src, dest);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to reorder documents", e);
        }
    }

    public boolean isDirty() throws DocumentException {
        try {
            try {
                javax.jcr.Property prop = node.getProperty(NodeConstants.ECM_DIRTY.rawname);
                return prop.getBoolean();
            } catch (PathNotFoundException e) {
                // if dirty flag is not set it means the doc is
                // new so it is dirty
                return true;
            }
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to retrieve document flags", e);
        }
    }

    public void setDirty(boolean value) throws DocumentException {
        try {
            node.setProperty(NodeConstants.ECM_DIRTY.rawname, value);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to retrieve document flags", e);
        }
    }

    public void remove() throws DocumentException {
        if (log.isDebugEnabled()) {
            log.debug("removing doc " + getPath());
        }
        try {
            // version removal is done at the AbstractSession level
            node.remove();
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    public void save() throws DocumentException {
        try {
            node.save();
        } catch (RepositoryException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return getName();
        } catch (DocumentException e) {
            return super.toString();
        }
    }

    public final void assertIsFolder() {
        if (!type.isFolder()) {
            throw new UnsupportedOperationException(
                    "This document is not a folder");
        }
    }

    // Version-related functions

    public void checkIn(String label) throws DocumentException {
        // be sure document is not dirty otherwise checkin will fail (conf. to jcr specs)
        JCRHelper.saveNode(node);
        Versioning.getService().checkin(this, label);
    }

    public void checkIn(String label, String description)
            throws DocumentException {
        // be sure document is not dirty otherwise checkin will fail (conf. to jcr specs)
        JCRHelper.saveNode(node);
        Versioning.getService().checkin(this, label, description);
    }

    public void checkOut() throws DocumentException {
        Versioning.getService().checkout(this);
    }

    public boolean isCheckedOut() throws DocumentException {
        return Versioning.getService().isCheckedOut(this);
    }

    public void restore(String label) throws DocumentException {
        Versioning.getService().restore(this, label);
    }

    public List<String>getVersionsIds() throws DocumentException {
        return Versioning.getService().getVersionsIds(this);
    }

    public Document getVersion(String label) throws DocumentException {
        return Versioning.getService().getVersion(this, label);
    }

    public DocumentVersionIterator getVersions() throws DocumentException {
        /*
         * try { return new JCRDocumentVersionIterator(session,
         * node.getVersionHistory() .getAllVersions()); } catch
         * (UnsupportedRepositoryOperationException e) { throw new
         * DocumentException(e); } catch (RepositoryException e) { throw new
         * DocumentException(e); }
         */
        return Versioning.getService().getVersions(this);
    }

    public DocumentVersion getLastVersion() throws DocumentException {
        return Versioning.getService().getLastVersion(this);
    }

    // END - Version-related function

    // ------------- property management -------------------

    public boolean getBoolean(String name) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return PropertyContainerAdapter.getBoolean(type, node,
                field.getName().getPrefixedName());
    }

    public Blob getContent(String name) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return PropertyContainerAdapter.getContent(type, node,
                field.getName().getPrefixedName());
    }

    public Calendar getDate(String name) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return PropertyContainerAdapter.getDate(type, node,
                field.getName().getPrefixedName());
    }

    public double getDouble(String name) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return PropertyContainerAdapter.getDouble(type, node,
                field.getName().getPrefixedName());
    }

    public long getLong(String name) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return PropertyContainerAdapter.getLong(type, node,
                field.getName().getPrefixedName());
    }

    public String getString(String name) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        return PropertyContainerAdapter.getString(type, node,
                field.getName().getPrefixedName());
    }

    public Property getProperty(String name) throws DocumentException {
        return PropertyFactory.getProperty(this, name);
    }

    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        // we log a debugging message here as it is a point where the
        // property name is known
        try {
            getProperty(name).setValue(value);
            if (!dirtyFields.contains(name)) {
                dirtyFields.add(name);
            }
        } catch (RuntimeException e) {
            log.error("RuntimeException setting value: " + value
                    + " on property: " + name);
            throw e;
        } catch (DocumentException e) {
            log.error("Error setting value: " + value + " on property: " + name);
            throw e;
        }
    }

    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {

        try {
            Node n = node.getNode(NodeConstants.ECM_SYSTEM_ANY.rawname);
            if (null == value) {
                n.setProperty(name, (Value) null);
                return;
            }

            if (value.getClass() == String.class) {
                n.setProperty(name, (String) value);
            } else if (value.getClass() == Long.class) {
                n.setProperty(name, (Long) value);
            } else if (value.getClass() == Integer.class) {
                // this is ok as long as data is not truncated
                n.setProperty(name, (Integer) value);
            } else if (value.getClass() == Boolean.class) {
                // this is ok as long as data is not truncated
                n.setProperty(name, (Boolean) value);
            } else {
                throw new DocumentException("unsupported type: "
                        + value.getClass());
            }
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set system property: "
                    + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        try {
            Node n = node.getNode(NodeConstants.ECM_SYSTEM_ANY.rawname);
            javax.jcr.Property p = n.getProperty(name);
            Value v = p.getValue();

            if (type == String.class) {
                return (T) v.getString();
            } else if (type == Long.class) {
                return (T) Long.valueOf(v.getLong());
            } else if (type == Boolean.class) {
                return (T) Boolean.valueOf(v.getBoolean());
            } else {
                throw new DocumentException("unsupported specified type: "
                        + type);
            }

        } catch (RepositoryException e) {
            throw new DocumentException("failed to get system property: "
                    + name, e);
        }
    }

    public Object getPropertyValue(String name) throws DocumentException {
        return getProperty(name).getValue();
    }

    public Collection<Property> getProperties() throws DocumentException {
        return PropertyContainerAdapter.getProperties(this);
    }

    public List<String> getDirtyFields() {
        return dirtyFields;
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return PropertyContainerAdapter.getPropertyIterator(this);
    }

    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        Map<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>();
        if (schemas != null) {
            for (String schemaName : schemas) {
                Schema schema = type.getSchema(schemaName);
                if (schema != null) {
                    Map<String, Object> subMap = new HashMap<String, Object>();
                    for (Field field : schema.getFields()) {
                        Property property = PropertyFactory.getProperty(this,
                                field);
                        subMap.put(field.getName().getLocalName(),
                                property.getValue());
                    }
                    map.put(schema.getName(), subMap);
                }
            }
        } else {
            Collection<Schema> allSchemas = type.getSchemas();
            for (Schema schema : allSchemas) {
                Map<String, Object> subMap = new HashMap<String, Object>();
                for (Field field : schema.getFields()) {
                    Property property = PropertyFactory.getProperty(this, field);
                    subMap.put(field.getName().getLocalName(),
                            property.getValue());
                }
                map.put(schema.getName(), subMap);
            }
        }
        return map;
    }

    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        Schema schema = type.getSchema(schemaName);
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : schema.getFields()) {
            Property property = PropertyFactory.getProperty(this, field);
            map.put(field.getName().getLocalName(), property.getValue());
        }
        return map;
    }

    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        Map<String, Object> map = new HashMap<String, Object>();
        if (schemas != null) {
            for (String schemaName : schemas) {
                Schema schema = type.getSchema(schemaName);
                if (schema != null) {
                    for (Field field : schema.getFields()) {
                        Property property = PropertyFactory.getProperty(this,
                                field);
                        map.put(field.getName().getPrefixedName(),
                                property.getValue());
                    }
                }
            }
        } else {
            Collection<Schema> allSchemas = type.getSchemas();
            for (Schema schema : allSchemas) {
                for (Field field : schema.getFields()) {
                    Property property = PropertyFactory.getProperty(this, field);
                    map.put(field.getName().getPrefixedName(),
                            property.getValue());
                }
            }
        }
        return map;
    }

    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            String schemaName = entry.getKey();
            Schema schema = type.getSchema(schemaName);
            if (schema != null) {
                String prefix = schema.getNamespace().prefix;
                int len = prefix.length();
                if (len != 0) {
                    buf.append(prefix).append(':');
                    len++;
                }
                for (Map.Entry<String, Object> subEntry : entry.getValue().entrySet()) {
                    buf.append(subEntry.getKey());
                    setPropertyValue(buf.toString(), entry.getValue());
                    buf.setLength(len);
                }
            }
        }
    }

    public void importFlatMap(Map<String, Object> map) throws DocumentException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            setPropertyValue(entry.getKey(), entry.getValue());
        }
    }

    public boolean isPropertySet(String path) throws DocumentException {
        return PropertyContainerAdapter.hasProperty(node, path);
    }

    public void removeProperty(String name) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        PropertyContainerAdapter.removeProperty(node,
                field.getName().getPrefixedName());
    }

    public void setBoolean(String name, boolean value) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        // TODO check constraints
        // checkValue(field, value));
        PropertyContainerAdapter.setBoolean(node,
                field.getName().getPrefixedName(), value);
    }

    public void setContent(String name, Blob value) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        // TODO check constraints
        // checkValue(field, value));
        PropertyContainerAdapter.setContent(node,
                field.getName().getPrefixedName(), value);
    }

    public void setDate(String name, Calendar value) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        // TODO check constraints
        // checkValue(field, value));
        PropertyContainerAdapter.setDate(node,
                field.getName().getPrefixedName(), value);
    }

    public void setDouble(String name, double value) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        // TODO check constraints
        // checkValue(field, value));
        PropertyContainerAdapter.setDouble(node,
                field.getName().getPrefixedName(), value);
    }

    public void setLong(String name, long value) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        // TODO check constraints
        // checkValue(field, value));
        PropertyContainerAdapter.setLong(node,
                field.getName().getPrefixedName(), value);
    }

    public void setString(String name, String value) throws DocumentException {
        Field field = getField(name);
        if (field == null) {
            throw new NoSuchPropertyException(name);
        }
        // TODO check constraints
        // checkValue(field, value));
        PropertyContainerAdapter.setString(node,
                field.getName().getPrefixedName(), value);
    }

    public Field getField(String name) {
        return type.getField(name);
    }

    public ComplexType getSchema(String schema) {
        return type.getSchema(schema);
    }

    public Node connect() throws DocumentException {
        return node;
    }

    public boolean isConnected() {
        return node != null;
    }

    public Node getNode() {
        return node;
    }

    public JCRDocument getDocument() {
        return this;
    }

    public Collection<Field> getFields() {
        return type.getFields();
    }

    public boolean hasVersions() throws DocumentException {
        // there will always be a root version - work around that by skipping
        // one version, for the moment at least.
        DocumentVersionIterator versionIterator = getVersions();
        versionIterator.nextDocumentVersion();

        return versionIterator.hasNext();
    }

    // TODO: optimize this since it is used in permission checks
    public boolean isLocked() throws DocumentException {
        return getLock() != null;
    }

    public String getLock() throws DocumentException {
        if (lock != null) {
            return (lock == Constants.EMPTY_STRING) ? null : lock;
        }
        try {
            javax.jcr.Property lock = node.getProperty(NodeConstants.ECM_LOCK.rawname);
            this.lock = lock.getString();
        } catch (PathNotFoundException e) {
            // no lock on that document - return null
            lock = Constants.EMPTY_STRING;
        } catch (RepositoryException e) {
            throw new DocumentException("Cannot get lock information for "
                    + getName(), e);
        }
        return lock == Constants.EMPTY_STRING ? null : lock;
    }

    public void setLock(String key) throws DocumentException {
        if (key == null) {
            throw new IllegalArgumentException("The lock key canot be null");
        }
        if (isLocked()) {
            throw new DocumentException("Document is already locked: "
                    + getName());
        }
        try {
            node.setProperty(NodeConstants.ECM_LOCK.rawname, key);
            lock = key;
            session.documentLocked(this);
        } catch (RepositoryException e) {
            throw new DocumentException("Failed to set lock on " + getName(), e);
        }
    }

    public String unlock() throws DocumentException {
        try {
            javax.jcr.Property lock = node.getProperty(NodeConstants.ECM_LOCK.rawname);
            String key = lock.getString();
            if (key == null) {
                return null;
            }
            lock.remove();
            this.lock = Constants.EMPTY_STRING;
            session.documentUnlocked(this);
            return key;
        } catch (PathNotFoundException e) {
            // no lock on that document - return null
        } catch (RepositoryException e) {
            throw new DocumentException("Cannot get lock information for "
                    + getName(), e);
        }
        return null;
    }

    public static void checkValue(Field field, Object value)
            throws DocumentException {
        try {
            if (!field.getType().validate(value)) {
                throw new DocumentException("constraints validation failed");
            }
        } catch (TypeException e) {
            throw new DocumentException("constraints validation failed", e);
        }
    }

    public boolean isProxy() {
        return false;
    }

    public boolean isVersion() {
        return false;
    }

    public Document getSourceDocument() throws DocumentException {
        return this;
    }

    public Repository getRepository() {
        return session.getRepository();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof JCRDocument) {
            return node == ((JCRDocument) obj).node;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    public void readDocumentPart(DocumentPart dp) throws Exception {
        DocumentPartReader.readDocumentPart(this, dp);
    }

    public void writeDocumentPart(DocumentPart dp) throws Exception {
        DocumentPartWriter.writeDocumentPart(this, dp);
    }

}
