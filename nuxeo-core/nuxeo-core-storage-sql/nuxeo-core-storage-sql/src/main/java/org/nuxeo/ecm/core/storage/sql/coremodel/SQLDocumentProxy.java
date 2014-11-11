/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;
import org.nuxeo.ecm.core.model.DocumentProxy;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * A proxy is a shortcut to a target document (a version or normal document).
 *
 * @author Florent Guillaume
 */
public class SQLDocumentProxy implements SQLDocument, DocumentProxy {

    /** The proxy seen as a normal doc ({@link SQLDocument}). */
    private final Document proxy;

    /** The target. */
    private Document target;

    // private SQLDocumentVersion version;

    protected SQLDocumentProxy(Document proxy, Document target)
            throws DocumentException {
        this.proxy = proxy;
        this.target = target;
    }

    /*
     * ----- SQLDocument -----
     */

    public Node getNode() {
        return ((SQLDocument) proxy).getNode();
    }

    public org.nuxeo.ecm.core.model.Property getACLProperty()
            throws DocumentException {
        return ((SQLDocument) proxy).getACLProperty();
    }

    public void checkWritable() throws DocumentException {
        ((SQLDocument) target).checkWritable();
    }

    /*
     * ----- Document -----
     */

    public boolean isProxy() {
        return true;
    }

    public String getName() throws DocumentException {
        return proxy.getName();
    }

    public String getUUID() throws DocumentException {
        return proxy.getUUID();
    }

    public Document getParent() throws DocumentException {
        return proxy.getParent();
    }

    public String getPath() throws DocumentException {
        return proxy.getPath();
    }

    public void remove() throws DocumentException {
        proxy.remove();
    }

    public DocumentType getType() {
        return target.getType();
    }

    public Repository getRepository() {
        return target.getRepository();
    }

    public Session getSession() {
        return target.getSession();
    }

    public boolean isFolder() {
        return target.isFolder();
    }

    public Calendar getLastModified() throws DocumentException {
        return target.getLastModified();
    }

    public void save() throws DocumentException {
        target.save();
    }

    public boolean isDirty() throws DocumentException {
        return target.isDirty();
    }

    public void setDirty(boolean value) throws DocumentException {
        target.setDirty(value);
    }

    public void readDocumentPart(DocumentPart dp) throws Exception {
        target.readDocumentPart(dp);
    }

    public void writeDocumentPart(DocumentPart dp) throws Exception {
        target.writeDocumentPart(dp);
    }

    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {
        target.setSystemProp(name, value);
    }

    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        return target.getSystemProp(name, type);
    }

    /*
     * ----- LifeCycle -----
     */

    public String getLifeCyclePolicy() throws LifeCycleException {
        return target.getLifeCyclePolicy();
    }

    public void setLifeCyclePolicy(String policy) throws LifeCycleException {
        target.setLifeCyclePolicy(policy);
    }

    public String getCurrentLifeCycleState() throws LifeCycleException {
        return target.getCurrentLifeCycleState();
    }

    public void setCurrentLifeCycleState(String state)
            throws LifeCycleException {
        target.setCurrentLifeCycleState(state);
    }

    public boolean followTransition(String transition)
            throws LifeCycleException {
        return target.followTransition(transition);
    }

    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        return target.getAllowedStateTransitions();
    }

    /*
     * ----- Lockable -----
     */

    public boolean isLocked() throws DocumentException {
        return target.isLocked();
    }

    public String getLock() throws DocumentException {
        return target.getLock();
    }

    public void setLock(String key) throws DocumentException {
        target.setLock(key);
    }

    public String unlock() throws DocumentException {
        return target.unlock();
    }

    /*
     * ----- VersionableDocument -----
     */

    public boolean isVersion() {
        return false;
    }

    public Document getSourceDocument() throws DocumentException {
        // this is what the rest of Nuxeo expects for a proxy
        return target;
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
        return target.isCheckedOut();
    }

    public void restore(String label) throws DocumentException {
        target.restore(label);
    }

    public List<String> getVersionsIds() throws DocumentException {
        return target.getVersionsIds();
    }

    public Document getVersion(String label) throws DocumentException {
        return target.getVersion(label);
    }

    public DocumentVersionIterator getVersions() throws DocumentException {
        return target.getVersions();
    }

    public DocumentVersion getLastVersion() throws DocumentException {
        return target.getLastVersion();
    }

    public boolean hasVersions() throws DocumentException {
        return target.hasVersions();
    }

    /*
     * ----- DocumentContainer -----
     */

    public Document resolvePath(String path) throws DocumentException {
        return proxy.resolvePath(path);
    }

    public Document getChild(String name) throws DocumentException {
        return proxy.getChild(name);
    }

    public Iterator<Document> getChildren() throws DocumentException {
        return proxy.getChildren();
    }

    public DocumentIterator getChildren(int start) throws DocumentException {
        return proxy.getChildren(start);
    }

    public List<String> getChildrenIds() throws DocumentException {
        return proxy.getChildrenIds();
    }

    public boolean hasChild(String name) throws DocumentException {
        return proxy.hasChild(name);
    }

    public boolean hasChildren() throws DocumentException {
        return proxy.hasChildren();
    }

    public Document addChild(String name, String typeName)
            throws DocumentException {
        return proxy.addChild(name, typeName);
    }

    public void orderBefore(String src, String dest) throws DocumentException {
        proxy.orderBefore(src, dest);
    }

    public void removeChild(String name) throws DocumentException {
        proxy.removeChild(name);
    }

    /*
     * ----- DocumentProxy -----
     */

    public Document getTargetDocument() {
        return target;
    }

    public void setTargetDocument(Document target) throws DocumentException {
        ((SQLDocument) proxy).checkWritable();
        try {
            ((SQLDocument) proxy).getNode().setSingleProperty(
                    Model.PROXY_TARGET_PROP, target.getUUID());
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        this.target = target;
    }

    /*
     * ----- PropertyContainer -----
     */

    public boolean isPropertySet(String name) throws DocumentException {
        return target.isPropertySet(name);
    }

    public Property getProperty(String name) throws DocumentException {
        return target.getProperty(name);
    }

    public Collection<Property> getProperties() throws DocumentException {
        return target.getProperties();
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return target.getPropertyIterator();
    }

    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        return target.exportFlatMap(schemas);
    }

    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        return target.exportMap(schemas);
    }

    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        return target.exportMap(schemaName);
    }

    public void importFlatMap(Map<String, Object> map) throws DocumentException {
        target.importFlatMap(map);
    }

    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
        target.importMap(map);
    }

    public List<String> getDirtyFields() {
        return target.getDirtyFields();
    }

    public Object getPropertyValue(String name) throws DocumentException {
        return target.getPropertyValue(name);
    }

    public String getString(String name) throws DocumentException {
        return target.getString(name);
    }

    public boolean getBoolean(String name) throws DocumentException {
        return target.getBoolean(name);
    }

    public long getLong(String name) throws DocumentException {
        return target.getLong(name);
    }

    public double getDouble(String name) throws DocumentException {
        return target.getDouble(name);
    }

    public Calendar getDate(String name) throws DocumentException {
        return target.getDate(name);
    }

    public Blob getContent(String name) throws DocumentException {
        return target.getContent(name);
    }

    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        target.setPropertyValue(name, value);
    }

    public void setString(String name, String value) throws DocumentException {
        target.setString(name, value);
    }

    public void setBoolean(String name, boolean value) throws DocumentException {
        target.setBoolean(name, value);
    }

    public void setLong(String name, long value) throws DocumentException {
        target.setLong(name, value);
    }

    public void setDouble(String name, double value) throws DocumentException {
        target.setDouble(name, value);
    }

    public void setDate(String name, Calendar value) throws DocumentException {
        target.setDate(name, value);
    }

    public void setContent(String name, Blob value) throws DocumentException {
        target.setContent(name, value);
    }

    public void removeProperty(String name) throws DocumentException {
        target.removeProperty(name);
    }

    /*
     * ----- Property -----
     */

    public Object getValue() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public boolean isNull() {
        throw new UnsupportedOperationException();
    }

    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public void setValue(Object value) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- toString/equals/hashcode -----
     */

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + target + ')';
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof SQLDocumentProxy) {
            return equals((SQLDocumentProxy) other);
        }
        return false;
    }

    private boolean equals(SQLDocumentProxy other) {
        return proxy.equals(other.proxy) && target.equals(other.target);
    }

    @Override
    public int hashCode() {
        return proxy.hashCode() + target.hashCode();
    }

}
