/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.runtime.api.Framework;

/**
 * A proxy is a shortcut to a target document (a version or normal document).
 */
public class SQLDocumentProxy implements SQLDocument {

    /** The proxy seen as a normal doc ({@link SQLDocument}). */
    private final Document proxy;

    /** The target. */
    private Document target;

    // private SQLDocumentVersion version;

    protected SQLDocumentProxy(Document proxy, Document target) {
        this.proxy = proxy;
        this.target = target;
    }

    protected String getSchema(String xpath) {
        int p = xpath.indexOf(':');
        if (p == -1) {
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String prefix = xpath.substring(0, p);
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        Schema schema = schemaManager.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = schemaManager.getSchema(prefix);
            if (schema == null) {
                throw new PropertyNotFoundException(xpath, "No schema for prefix");
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
     * Checks if the given property should be resolved on the proxy or the target.
     */
    protected boolean isPropertyForProxy(String xpath) {
        if (Model.MAIN_MINOR_VERSION_PROP.equals(xpath) || Model.MAIN_MAJOR_VERSION_PROP.equals(xpath)) {
            return false;
        }
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
    public Long getPos() {
        return proxy.getPos();
    }

    @Override
    public Document getParent() {
        return proxy.getParent();
    }

    @Override
    public String getPath() {
        return proxy.getPath();
    }

    @Override
    public void remove() {
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
    public void readDocumentPart(DocumentPart dp) throws PropertyException {
        if (isSchemaForProxy(dp.getName())) {
            proxy.readDocumentPart(dp);
        } else {
            target.readDocumentPart(dp);
        }
    }

    @Override
    public Map<String, Serializable> readPrefetch(ComplexType complexType, Set<String> xpaths)
            throws PropertyException {
        if (isSchemaForProxy(complexType.getName())) {
            return proxy.readPrefetch(complexType, xpaths);
        } else {
            return target.readPrefetch(complexType, xpaths);
        }
    }

    @Override
    public WriteContext getWriteContext() {
        // proxy or target doesn't matter, this is about typing
        return proxy.getWriteContext();
    }

    @Override
    public boolean writeDocumentPart(DocumentPart dp, WriteContext writeContext) throws PropertyException {
        if (isSchemaForProxy(dp.getName())) {
            return proxy.writeDocumentPart(dp, writeContext);
        } else {
            return target.writeDocumentPart(dp, writeContext);
        }
    }

    @Override
    public void setSystemProp(String name, Serializable value) {
        target.setSystemProp(name, value);
    }

    @Override
    public <T extends Serializable> T getSystemProp(String name, Class<T> type) {
        return target.getSystemProp(name, type);
    }

    public static final String CHANGE_TOKEN_PROXY_SEP = "/";

    /*
     * The change token for a proxy must reflect the fact that either the proxy (name, parent, acls, etc.) or its target
     * may be changed.
     */
    @Override
    public String getChangeToken() {
        String proxyToken = proxy.getChangeToken();
        String targetToken = target.getChangeToken();
        return getProxyChangeToken(proxyToken, targetToken);
    }

    protected static String getProxyChangeToken(String proxyToken, String targetToken) {
        if (proxyToken == null && targetToken == null) {
            return null;
        } else {
            if (proxyToken == null) {
                proxyToken = "";
            } else if (targetToken == null) {
                targetToken = "";
            }
            return proxyToken + CHANGE_TOKEN_PROXY_SEP + targetToken;
        }
    }

    @Override
    public boolean validateChangeToken(String changeToken) {
        if (changeToken == null) {
            return true;
        }
        String[] parts = changeToken.split(CHANGE_TOKEN_PROXY_SEP, 2);
        if (parts.length != 2) {
            // invalid format
            return false;
        }
        String proxyToken = parts[0];
        if (proxyToken.isEmpty()) {
            proxyToken = null;
        }
        String targetToken = parts[1];
        if (targetToken.isEmpty()) {
            targetToken = null;
        }
        if (proxyToken == null && targetToken == null) {
            return true;
        }
        return proxy.validateChangeToken(proxyToken) && target.validateChangeToken(targetToken);
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
    public boolean addFacet(String facet) {
        return target.addFacet(facet); // TODO proxy facets
    }

    @Override
    public boolean removeFacet(String facet) {
        return target.removeFacet(facet); // TODO proxy facets
    }

    /*
     * ----- LifeCycle -----
     */

    @Override
    public String getLifeCyclePolicy() {
        return target.getLifeCyclePolicy();
    }

    @Override
    public void setLifeCyclePolicy(String policy) {
        target.setLifeCyclePolicy(policy);
    }

    @Override
    public String getLifeCycleState() {
        return target.getLifeCycleState();
    }

    @Override
    public void setCurrentLifeCycleState(String state) {
        target.setCurrentLifeCycleState(state);
    }

    @Override
    public void followTransition(String transition) throws LifeCycleException {
        target.followTransition(transition);
    }

    @Override
    public Collection<String> getAllowedStateTransitions() {
        return target.getAllowedStateTransitions();
    }

    @Override
    public Lock getLock() {
        return target.getLock();
    }

    @Override
    public Lock setLock(Lock lock) {
        return target.setLock(lock);
    }

    @Override
    public Lock removeLock(String owner) {
        return target.removeLock(owner);
    }

    @Override
    public boolean isVersion() {
        return false;
    }

    @Override
    public Document getBaseVersion() {
        return target.getBaseVersion();
    }

    @Override
    public String getVersionSeriesId() {
        return target.getVersionSeriesId();
    }

    @Override
    public Document getSourceDocument() {
        // this is what the rest of Nuxeo expects for a proxy
        return target;
    }

    @Override
    public Document checkIn(String label, String checkinComment) {
        return target.checkIn(label, checkinComment);
    }

    @Override
    public void checkOut() {
        target.checkOut();
    }

    @Override
    public boolean isCheckedOut() {
        return target.isCheckedOut();
    }

    @Override
    public boolean isLatestVersion() {
        return target.isLatestVersion();
    }

    @Override
    public boolean isMajorVersion() {
        return target.isMajorVersion();
    }

    @Override
    public boolean isLatestMajorVersion() {
        return target.isLatestMajorVersion();
    }

    @Override
    public boolean isVersionSeriesCheckedOut() {
        return target.isVersionSeriesCheckedOut();
    }

    @Override
    public String getVersionLabel() {
        return target.getVersionLabel();
    }

    @Override
    public String getCheckinComment() {
        return target.getCheckinComment();
    }

    @Override
    public Document getWorkingCopy() {
        return target.getWorkingCopy();
    }

    @Override
    public Calendar getVersionCreationDate() {
        return target.getVersionCreationDate();
    }

    @Override
    public void restore(Document version) {
        target.restore(version);
    }

    @Override
    public List<String> getVersionsIds() {
        return target.getVersionsIds();
    }

    @Override
    public Document getVersion(String label) {
        return target.getVersion(label);
    }

    @Override
    public List<Document> getVersions() {
        return target.getVersions();
    }

    @Override
    public Document getLastVersion() {
        return target.getLastVersion();
    }

    @Override
    public Document getChild(String name) {
        return proxy.getChild(name);
    }

    @Override
    public List<Document> getChildren() {
        return proxy.getChildren();
    }

    @Override
    public List<String> getChildrenIds() {
        return proxy.getChildrenIds();
    }

    @Override
    public boolean hasChild(String name) {
        return proxy.hasChild(name);
    }

    @Override
    public boolean hasChildren() {
        return proxy.hasChildren();
    }

    @Override
    public Document addChild(String name, String typeName) {
        return proxy.addChild(name, typeName);
    }

    @Override
    public void orderBefore(String src, String dest) {
        proxy.orderBefore(src, dest);
    }

    /*
     * ----- DocumentProxy -----
     */

    @Override
    public Document getTargetDocument() {
        return target;
    }

    @Override
    public void setTargetDocument(Document target) {
        if (((SQLDocumentLive) proxy).isReadOnly()) {
            throw new ReadOnlyPropertyException("Cannot write proxy: " + this);
        }
        if (!target.getVersionSeriesId().equals(getVersionSeriesId())) {
            throw new ReadOnlyPropertyException("Cannot set proxy target to different version series");
        }
        getSession().setProxyTarget(proxy, target);
        this.target = target;
    }

    @Override
    public Serializable getPropertyValue(String name) {
        if (isPropertyForProxy(name)) {
            return proxy.getPropertyValue(name);
        } else {
            return target.getPropertyValue(name);
        }
    }

    @Override
    public void setPropertyValue(String name, Serializable value) {
        if (isPropertyForProxy(name)) {
            proxy.setPropertyValue(name, value);
        } else {
            target.setPropertyValue(name, value);
        }
    }

    @Override
    public Object getValue(String xpath) throws PropertyException {
        if (isPropertyForProxy(xpath)) {
            return proxy.getValue(xpath);
        } else {
            return target.getValue(xpath);
        }
    }

    @Override
    public void setValue(String xpath, Object value) throws PropertyException {
        if (isPropertyForProxy(xpath)) {
            proxy.setValue(xpath, value);
        } else {
            target.setValue(xpath, value);
        }
    }

    @Override
    public void visitBlobs(Consumer<BlobAccessor> blobVisitor) throws PropertyException {
        // visit all blobs from the proxy AND the target
        proxy.visitBlobs(blobVisitor);
        target.visitBlobs(blobVisitor);
    }

    /*
     * ----- toString/equals/hashcode -----
     */

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + target + ',' + proxy.getUUID() + ')';
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
