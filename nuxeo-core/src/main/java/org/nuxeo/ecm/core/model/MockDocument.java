/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FakeDocument.java 26383 2007-10-23 16:21:34Z bstefanescu $
 */

package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.DocumentTypeImpl;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class MockDocument implements Document {

    public String uuid;

    public String creator;

    public Lock lock;

    public boolean checkedout;

    public MockDocument(String uuid, String creator) {
        this.uuid = uuid;
        this.creator = creator;
    }

    @Override
    public String getName() throws DocumentException {
        return uuid;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    @Override
    public Session getSession() {
        return null;
    }

    @Override
    public DocumentType getType() {
        return new DocumentTypeImpl((DocumentType) null, "FakeDocument");
    }

    @Override
    public String getPath() throws DocumentException {
        return "/path/" + uuid;
    }

    @Override
    public Repository getRepository() {
        return null;
    }

    // not implemented (useless)

    @Override
    public void setLifeCyclePolicy(String policy) {
    }

    @Override
    public void setCurrentLifeCycleState(String state) {
    }

    @Override
    public boolean followTransition(String transition)
            throws LifeCycleException {
        return false;
    }

    @Override
    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        return null;
    }

    @Override
    public Document getBaseVersion() throws DocumentException {
        return null;
    }

    @Override
    public String getLifeCycleState() throws LifeCycleException {
        return null;
    }

    @Override
    public Calendar getLastModified() throws DocumentException {
        return null;
    }

    @Override
    public String getLifeCyclePolicy() throws LifeCycleException {
        return null;
    }

    @Override
    public Document getParent() throws DocumentException {
        return null;
    }

    @Override
    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        return null;
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public void remove() throws DocumentException {
    }

    @Override
    public void save() throws DocumentException {
    }

    @Override
    public <T extends Serializable> void setSystemProp(String name, T value)
            throws DocumentException {
    }

    @Override
    public Document checkIn(String label, String description)
            throws DocumentException {
        return null;
    }

    @Override
    public void checkOut() throws DocumentException {
    }

    @Override
    public Document getLastVersion() throws DocumentException {
        return null;
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        return null;
    }

    @Override
    public List<String> getVersionsIds() throws DocumentException {
        return null;
    }

    @Override
    public Document getVersion(String label) throws DocumentException {
        return null;
    }

    @Override
    public List<Document> getVersions() throws DocumentException {
        return null;
    }

    @Override
    public boolean hasVersions() throws DocumentException {
        return false;
    }

    @Override
    public boolean isCheckedOut() throws DocumentException {
        return checkedout;
    }

    @Override
    public boolean isVersion() {
        return false;
    }

    @Override
    public String getVersionSeriesId() throws DocumentException {
        return null;
    }

    @Override
    public void restore(Document version) throws DocumentException {
    }

    @Override
    public Document addChild(String name, String typeName)
            throws DocumentException {
        return null;
    }

    @Override
    public Document getChild(String name) throws DocumentException {
        return null;
    }

    @Override
    public Iterator<Document> getChildren() throws DocumentException {
        return null;
    }

    @Override
    public DocumentIterator getChildren(int start) throws DocumentException {
        return null;
    }

    @Override
    public List<String> getChildrenIds() throws DocumentException {
        return null;
    }

    @Override
    public boolean hasChild(String name) throws DocumentException {
        return false;
    }

    @Override
    public boolean hasChildren() throws DocumentException {
        return false;
    }

    @Override
    public void removeChild(String name) throws DocumentException {
    }

    @Override
    public Document resolvePath(String relPath) throws DocumentException {
        return null;
    }

    @Override
    public Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        return null;
    }

    @Override
    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        return null;
    }

    @Override
    public boolean getBoolean(String name) throws DocumentException {
        return false;
    }

    @Override
    public Blob getContent(String name) throws DocumentException {
        return null;
    }

    @Override
    public Calendar getDate(String name) throws DocumentException {
        return null;
    }

    @Override
    public List<String> getDirtyFields() {
        return null;
    }

    @Override
    public double getDouble(String name) throws DocumentException {
        return 0;
    }

    @Override
    public long getLong(String name) throws DocumentException {
        return 0;
    }

    @Override
    public Collection<Property> getProperties() throws DocumentException {
        return null;
    }

    @Override
    public Property getProperty(String name) throws DocumentException {
        return null;
    }

    @Override
    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return null;
    }

    @Override
    public Object getPropertyValue(String name) throws DocumentException {
        if (name != null && name.equals("dc:creator")) {
            return creator;
        }
        return null;
    }

    @Override
    public String getString(String name) throws DocumentException {
        return null;
    }

    @Override
    public void importFlatMap(Map<String, Object> map) throws DocumentException {
    }

    @Override
    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
    }

    @Override
    public boolean isPropertySet(String path) throws DocumentException {
        return false;
    }

    @Override
    public void removeProperty(String name) throws DocumentException {
    }

    @Override
    public void setBoolean(String name, boolean value) throws DocumentException {
    }

    @Override
    public void setContent(String name, Blob value) throws DocumentException {
    }

    @Override
    public void setDate(String name, Calendar value) throws DocumentException {
    }

    @Override
    public void setDouble(String name, double value) throws DocumentException {
    }

    @Override
    public void setLong(String name, long value) throws DocumentException {
    }

    @Override
    public void setPropertyValue(String name, Object value)
            throws DocumentException {
    }

    @Override
    public void setString(String name, String value) throws DocumentException {
    }

    @Override
    public Lock getLock() throws DocumentException {
        return lock;
    }

    @Override
    public Lock setLock(Lock lock) throws DocumentException {
        this.lock = lock;
        return null;
    }

    @Override
    public Lock removeLock(String owner) throws DocumentException {
        Lock l = lock;
        lock = null;
        return l;
    }

    @Override
    public void readDocumentPart(DocumentPart dp) throws Exception {
    }

    @Override
    public void writeDocumentPart(DocumentPart dp) throws Exception {
    }

    @Override
    public void orderBefore(String src, String dest) throws DocumentException {
    }

    @Override
    public Calendar getVersionCreationDate() throws DocumentException {
        return null;
    }

    @Override
    public String getVersionLabel() throws DocumentException {
        return null;
    }

    @Override
    public boolean isLatestVersion() throws DocumentException {
        return false;
    }

    @Override
    public boolean isMajorVersion() throws DocumentException {
        return false;
    }

    @Override
    public boolean isLatestMajorVersion() throws DocumentException {
        return false;
    }

    @Override
    public boolean isVersionSeriesCheckedOut() throws DocumentException {
        return true;
    }

    @Override
    public Document getWorkingCopy() throws DocumentException {
        return null;
    }

    @Override
    public String getCheckinComment() throws DocumentException {
        return null;
    }

    @Override
    public Set<String> getAllFacets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getFacets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasFacet(String facet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addFacet(String facet) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeFacet(String facet) throws DocumentException {
        throw new UnsupportedOperationException();
    }

}
