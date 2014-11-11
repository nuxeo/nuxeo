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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FakeDocument.java 26383 2007-10-23 16:21:34Z bstefanescu $
 */

package org.nuxeo.ecm.core.security;

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
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.DocumentTypeImpl;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class MockDocument implements Document {

    final String uuid;

    final String creator;

    String lock;

    public MockDocument(String uuid, String creator) {
        this.uuid = uuid;
        this.creator = creator;
    }

    public String getName() throws DocumentException {
        return uuid;
    }

    public String getUUID() throws DocumentException {
        return uuid;
    }

    public Session getSession() {
        return null;
    }

    public DocumentType getType() {
        return new DocumentTypeImpl((DocumentType) null, "FakeDocument");
    }

    public String getPath() throws DocumentException {
        return "/path/" + uuid;
    }

    public Repository getRepository() {
        return null;
    }

    // not implemented (useless)

    public void setLifeCyclePolicy(String policy) {
    }

    public void setCurrentLifeCycleState(String state) {
    }

    public boolean followTransition(String transition)
            throws LifeCycleException {
        return false;
    }

    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        return null;
    }

    public String getCurrentLifeCycleState() throws LifeCycleException {
        return null;
    }

    public Calendar getLastModified() throws DocumentException {
        return null;
    }

    public String getLifeCyclePolicy() throws LifeCycleException {
        return null;
    }

    public Document getParent() throws DocumentException {
        return null;
    }

    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        return null;
    }

    public boolean isDirty() throws DocumentException {
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    public boolean isProxy() {
        return false;
    }

    public void remove() throws DocumentException {
    }

    public void save() throws DocumentException {
    }

    public void setDirty(boolean value) throws DocumentException {
    }

    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {
    }

    public void checkIn(String label, String description)
            throws DocumentException {
    }

    public void checkIn(String label) throws DocumentException {
    }

    public void checkOut() throws DocumentException {
    }

    public DocumentVersion getLastVersion() throws DocumentException {
        return null;
    }

    public Document getSourceDocument() throws DocumentException {
        return null;
    }

    public List<String>getVersionsIds() throws DocumentException {
        return null;
    }

    public Document getVersion(String label) throws DocumentException {
        return null;
    }

    public DocumentVersionIterator getVersions() throws DocumentException {
        return null;
    }

    public boolean hasVersions() throws DocumentException {
        return false;
    }

    public boolean isCheckedOut() throws DocumentException {
        return false;
    }

    public boolean isVersion() {
        return false;
    }

    public void restore(String label) throws DocumentException {
    }

    public Document addChild(String name, String typeName)
            throws DocumentException {
        return null;
    }

    public Document getChild(String name) throws DocumentException {
        return null;
    }

    public Iterator<Document> getChildren() throws DocumentException {
        return null;
    }

    public DocumentIterator getChildren(int start) throws DocumentException {
        return null;
    }

    public List<String> getChildrenIds() throws DocumentException {
        return null;
    }

    public boolean hasChild(String name) throws DocumentException {
        return false;
    }

    public boolean hasChildren() throws DocumentException {
        return false;
    }

    public void removeChild(String name) throws DocumentException {
    }

    public Document resolvePath(String relPath) throws DocumentException {
        return null;
    }

    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        return null;
    }

    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        return null;
    }

    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        return null;
    }

    public boolean getBoolean(String name) throws DocumentException {
        return false;
    }

    public Blob getContent(String name) throws DocumentException {
        return null;
    }

    public Calendar getDate(String name) throws DocumentException {
        return null;
    }

    public List<String> getDirtyFields() {
        return null;
    }

    public double getDouble(String name) throws DocumentException {
        return 0;
    }

    public long getLong(String name) throws DocumentException {
        return 0;
    }

    public Collection<Property> getProperties() throws DocumentException {
        return null;
    }

    public Property getProperty(String name) throws DocumentException {
        return null;
    }

    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return null;
    }

    public Object getPropertyValue(String name) throws DocumentException {
        if (name != null && name.equals("dc:creator")) {
            return creator;
        }
        return null;
    }

    public String getString(String name) throws DocumentException {
        return null;
    }

    public void importFlatMap(Map<String, Object> map) throws DocumentException {
    }

    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
    }

    public boolean isPropertySet(String path) throws DocumentException {
        return false;
    }

    public void removeProperty(String name) throws DocumentException {
    }

    public void setBoolean(String name, boolean value) throws DocumentException {
    }

    public void setContent(String name, Blob value) throws DocumentException {
    }

    public void setDate(String name, Calendar value) throws DocumentException {
    }

    public void setDouble(String name, double value) throws DocumentException {
    }

    public void setLong(String name, long value) throws DocumentException {
    }

    public void setPropertyValue(String name, Object value)
            throws DocumentException {
    }

    public void setString(String name, String value) throws DocumentException {
    }

    public String getLock() throws DocumentException {
        return lock;
    }

    public boolean isLocked() throws DocumentException {
        return lock != null;
    }

    public void setLock(String key) throws DocumentException {
        lock = key;
    }

    public String unlock() throws DocumentException {
        return null;
    }

    public void readDocumentPart(DocumentPart dp) throws Exception {
    }

    public void writeDocumentPart(DocumentPart dp) throws Exception {
    }

    public void orderBefore(String src, String dest) throws DocumentException {
    }

}
