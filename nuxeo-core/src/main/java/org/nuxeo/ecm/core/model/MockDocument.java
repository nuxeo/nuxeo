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
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.DocumentTypeImpl;
import org.nuxeo.ecm.core.schema.Prefetch;
import org.nuxeo.ecm.core.schema.types.ComplexType;

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
    public String getName() {
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
        return new DocumentTypeImpl("FakeDocument");
    }

    @Override
    public String getPath() throws DocumentException {
        return "/path/" + uuid;
    }

    @Override
    public String getRepositoryName() {
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
    public void followTransition(String transition) {
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
    public void setReadOnly(boolean readonly) {
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void remove() throws DocumentException {
    }

    @Override
    public void save() throws DocumentException {
    }

    @Override
    public void setSystemProp(String name, Serializable value)
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
    public Serializable getPropertyValue(String name) throws DocumentException {
        if (name != null && name.equals("dc:creator")) {
            return creator;
        }
        return null;
    }

    @Override
    public void setPropertyValue(String name, Serializable value)
            throws DocumentException {
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
    public void readDocumentPart(DocumentPart dp) {
    }

    @Override
    public void readPrefetch(ComplexType complexType, Prefetch prefetch,
            Set<String> fieldNames, Set<String> docSchemas)
            throws PropertyException {
    }

    @Override
    public void writeDocumentPart(DocumentPart dp) {
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
