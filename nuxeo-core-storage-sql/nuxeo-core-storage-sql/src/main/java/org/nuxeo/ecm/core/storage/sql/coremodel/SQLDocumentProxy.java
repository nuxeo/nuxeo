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
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentProxy;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.Prefetch;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.runtime.api.Framework;

/**
 * A proxy is a shortcut to a target document (a version or normal document).
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

    protected String getSchema(String xpath) throws DocumentException {
        int p = xpath.indexOf(':');
        if (p == -1) {
            throw new DocumentException("Schema not specified: " + xpath);
        }
        String prefix = xpath.substring(0, p);
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        Schema schema = schemaManager.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = schemaManager.getSchema(prefix);
            if (schema == null) {
                throw new DocumentException("No schema for prefix: " + xpath);
            }
        }
        return schema.getName();
    }

    /**
     * Checks if the given schema should be resolved on the proxy or the target.
     */
    protected boolean isSchemaForProxy(String schema) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        return schemaManager.isProxySchema(schema, getType().getName());
    }

    /**
     * Checks if the given property should be resolved on the proxy or the
     * target.
     */
    protected boolean isPropertyForProxy(String xpath) throws DocumentException {
        return isSchemaForProxy(getSchema(xpath));
    }

    /*
     * ----- SQLDocument -----
     */

    @Override
    public Node getNode() {
        return ((SQLDocument) proxy).getNode();
    }

    /*
     * ----- Document -----
     */

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public String getUUID() {
        return proxy.getUUID();
    }

    @Override
    public String getName() {
        return proxy.getName();
    }

    @Override
    public Document getParent() throws DocumentException {
        return proxy.getParent();
    }

    @Override
    public String getPath() throws DocumentException {
        return proxy.getPath();
    }

    @Override
    public void remove() throws DocumentException {
        proxy.remove();
    }

    @Override
    public DocumentType getType() {
        return target.getType();
    }

    @Override
    public String getRepositoryName() {
        return target.getRepositoryName();
    }

    @Override
    public Session getSession() {
        return target.getSession();
    }

    @Override
    public boolean isFolder() {
        return target.isFolder();
    }

    @Override
    public void setReadOnly(boolean readonly) {
        target.setReadOnly(readonly);
    }

    @Override
    public boolean isReadOnly() {
        return target.isReadOnly();
    }

    @Override
    public void save() throws DocumentException {
        target.save();
    }

    @Override
    public void readDocumentPart(DocumentPart dp) throws PropertyException {
        if (isSchemaForProxy(dp.getName())) {
            proxy.readDocumentPart(dp);
        } else {
            target.readDocumentPart(dp);
        }
    }

    @Override
    public void readPrefetch(ComplexType complexType, Prefetch prefetch,
            Set<String> fieldNames, Set<String> docSchemas)
            throws PropertyException {
        if (isSchemaForProxy(complexType.getName())) {
            proxy.readPrefetch(complexType, prefetch, fieldNames, docSchemas);
        } else {
            target.readPrefetch(complexType, prefetch, fieldNames, docSchemas);
        }
    }

    @Override
    public void writeDocumentPart(DocumentPart dp) throws PropertyException {
        if (isSchemaForProxy(dp.getName())) {
            proxy.writeDocumentPart(dp);
        } else {
            target.writeDocumentPart(dp);
        }
    }

    @Override
    public void setSystemProp(String name, Serializable value)
            throws DocumentException {
        target.setSystemProp(name, value);
    }

    @Override
    public <T extends Serializable> T getSystemProp(String name, Class<T> type)
            throws DocumentException {
        return target.getSystemProp(name, type);
    }

    @Override
    public Set<String> getAllFacets() {
        return target.getAllFacets(); // TODO proxy facets
    }

    @Override
    public String[] getFacets() {
        return target.getFacets(); // TODO proxy facets
    }

    @Override
    public boolean hasFacet(String facet) {
        return target.hasFacet(facet); // TODO proxy facets
    }

    @Override
    public boolean addFacet(String facet) throws DocumentException {
        return target.addFacet(facet); // TODO proxy facets
    }

    @Override
    public boolean removeFacet(String facet) throws DocumentException {
        return target.removeFacet(facet); // TODO proxy facets
    }

    /*
     * ----- LifeCycle -----
     */

    @Override
    public String getLifeCyclePolicy() throws LifeCycleException {
        return target.getLifeCyclePolicy();
    }

    @Override
    public void setLifeCyclePolicy(String policy) throws LifeCycleException {
        target.setLifeCyclePolicy(policy);
    }

    @Override
    public String getLifeCycleState() throws LifeCycleException {
        return target.getLifeCycleState();
    }

    @Override
    public void setCurrentLifeCycleState(String state)
            throws LifeCycleException {
        target.setCurrentLifeCycleState(state);
    }

    @Override
    public void followTransition(String transition) throws LifeCycleException {
        target.followTransition(transition);
    }

    @Override
    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        return target.getAllowedStateTransitions();
    }

    @Override
    public Lock getLock() throws DocumentException {
        return target.getLock();
    }

    @Override
    public Lock setLock(Lock lock) throws DocumentException {
        return target.setLock(lock);
    }

    @Override
    public Lock removeLock(String owner) throws DocumentException {
        return target.removeLock(owner);
    }

    @Override
    public boolean isVersion() {
        return false;
    }

    @Override
    public Document getBaseVersion() throws DocumentException {
        return null;
    }

    @Override
    public String getVersionSeriesId() throws DocumentException {
        return target.getVersionSeriesId();
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        // this is what the rest of Nuxeo expects for a proxy
        return target;
    }

    @Override
    public Document checkIn(String label, String checkinComment)
            throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkOut() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCheckedOut() throws DocumentException {
        return target.isCheckedOut();
    }

    @Override
    public boolean isLatestVersion() throws DocumentException {
        return target.isLatestVersion();
    }

    @Override
    public boolean isMajorVersion() throws DocumentException {
        return target.isMajorVersion();
    }

    @Override
    public boolean isLatestMajorVersion() throws DocumentException {
        return target.isLatestMajorVersion();
    }

    @Override
    public boolean isVersionSeriesCheckedOut() throws DocumentException {
        return target.isVersionSeriesCheckedOut();
    }

    @Override
    public String getVersionLabel() throws DocumentException {
        return target.getVersionLabel();
    }

    @Override
    public String getCheckinComment() throws DocumentException {
        return target.getCheckinComment();
    }

    @Override
    public Document getWorkingCopy() throws DocumentException {
        return target.getWorkingCopy();
    }

    @Override
    public Calendar getVersionCreationDate() throws DocumentException {
        return target.getVersionCreationDate();
    }

    @Override
    public void restore(Document version) throws DocumentException {
        target.restore(version);
    }

    @Override
    public List<String> getVersionsIds() throws DocumentException {
        return target.getVersionsIds();
    }

    @Override
    public Document getVersion(String label) throws DocumentException {
        return target.getVersion(label);
    }

    @Override
    public List<Document> getVersions() throws DocumentException {
        return target.getVersions();
    }

    @Override
    public Document getLastVersion() throws DocumentException {
        return target.getLastVersion();
    }

    @Override
    public Document getChild(String name) throws DocumentException {
        return proxy.getChild(name);
    }

    @Override
    public Iterator<Document> getChildren() throws DocumentException {
        return proxy.getChildren();
    }

    @Override
    public List<String> getChildrenIds() throws DocumentException {
        return proxy.getChildrenIds();
    }

    @Override
    public boolean hasChild(String name) throws DocumentException {
        return proxy.hasChild(name);
    }

    @Override
    public boolean hasChildren() throws DocumentException {
        return proxy.hasChildren();
    }

    @Override
    public Document addChild(String name, String typeName)
            throws DocumentException {
        return proxy.addChild(name, typeName);
    }

    @Override
    public void orderBefore(String src, String dest) throws DocumentException {
        proxy.orderBefore(src, dest);
    }

    @Override
    public void removeChild(String name) throws DocumentException {
        proxy.removeChild(name);
    }

    /*
     * ----- DocumentProxy -----
     */

    @Override
    public Document getTargetDocument() {
        return target;
    }

    @Override
    public void setTargetDocument(Document target) throws DocumentException {
        if (((SQLDocumentLive) proxy).isReadOnly()) {
            throw new DocumentException("Cannot write proxy: " + this);
        }
        if (!target.getVersionSeriesId().equals(getVersionSeriesId())) {
            throw new DocumentException(
                    "Cannot set proxy target to different version series");
        }
        getSession().setProxyTarget(proxy, target);
        this.target = target;
    }

    /*
     * ----- PropertyContainer -----
     */

    @Override
    public Serializable getPropertyValue(String name) throws DocumentException {
        if (isPropertyForProxy(name)) {
            return proxy.getPropertyValue(name);
        } else {
            return target.getPropertyValue(name);
        }
    }

    @Override
    public void setPropertyValue(String name, Serializable value)
            throws DocumentException {
        if (isPropertyForProxy(name)) {
            proxy.setPropertyValue(name, value);
        } else {
            target.setPropertyValue(name, value);
        }
    }

    /*
     * ----- toString/equals/hashcode -----
     */

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + target + ','
                + proxy.getUUID() + ')';
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
