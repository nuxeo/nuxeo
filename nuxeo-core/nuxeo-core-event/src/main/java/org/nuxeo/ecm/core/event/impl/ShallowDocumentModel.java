/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.resolver.DocumentPropertyObjectResolverImpl;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.schema.DocumentType;

/**
 * Light weight {@link DocumentModel} implementation Only holds {@link DocumentRef}, RepositoryName, name, path and
 * context data. Used to reduce memory footprint of {@link Event} stacked in {@link EventBundle}.
 *
 * @author Thierry Delprat
 */
public class ShallowDocumentModel implements DocumentModel {

    private static final long serialVersionUID = 1L;

    private final String id;

    private final String repoName;

    private final String name;

    private final Path path;

    private final String type;

    private final boolean isFolder;

    private final boolean isVersion;

    private final boolean isProxy;

    private final boolean isImmutable;

    private final ScopedMap contextData;

    private final Set<String> facets;

    private final String lifecycleState;

    public ShallowDocumentModel(DocumentModel doc) {
        id = doc.getId();
        repoName = doc.getRepositoryName();
        name = doc.getName();
        path = doc.getPath();
        type = doc.getType();
        isFolder = doc.isFolder();
        isVersion = doc.isVersion();
        isProxy = doc.isProxy();
        isImmutable = doc.isImmutable();
        contextData = doc.getContextData();
        facets = doc.getFacets();
        if (doc.isLifeCycleLoaded()) {
            lifecycleState = doc.getCurrentLifeCycleState();
        } else {
            lifecycleState = null;
        }
    }

    public ShallowDocumentModel(String id, String repoName, String name, Path path, String type, boolean isFolder,
            boolean isVersion, boolean isProxy, boolean isImmutable, ScopedMap contextData, Set<String> facets,
            String lifecycleState) {
        this.id = id;
        this.repoName = repoName;
        this.name = name;
        this.path = path;
        this.type = type;
        this.isFolder = isFolder;
        this.isVersion = isVersion;
        this.isProxy = isProxy;
        this.isImmutable = isImmutable;
        this.contextData = contextData;
        this.facets = facets;
        this.lifecycleState = lifecycleState;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public DocumentRef getRef() {
        return id == null ? null : new IdRef(id);
    }

    @Override
    public String getRepositoryName() {
        return repoName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getPos() {
        return null;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public String getPathAsString() {
        if (path != null) {
            return path.toString();
        }
        return null;
    }

    @Override
    public DocumentRef getParentRef() {
        if (path != null) {
            return new PathRef(path.removeLastSegments(1).toString());
        }
        return null;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean isFolder() {
        return isFolder;
    }

    @Override
    public boolean isVersion() {
        return isVersion;
    }

    @Override
    public void copyContent(DocumentModel sourceDoc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyContextData(DocumentModel otherDocument) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean followTransition(String transition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ACP getACP() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getAdapter(Class<T> itf) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getAllowedStateTransitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCacheKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScopedMap getContextData() {
        return contextData;
    }

    @Override
    public Serializable getContextData(ScopeType scope, String key) {
        if (contextData == null) {
            return null;
        }
        return contextData.getScopedValue(scope, key);
    }

    @Override
    public CoreSession getCoreSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach(boolean loadAll) {
    }

    @Override
    public void attach(String sid) {
    }

    @Override
    public String getCurrentLifeCycleState() {
        return lifecycleState;
    }

    @Override
    @Deprecated
    public DataModel getDataModel(String schema) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Map<String, DataModel> getDataModels() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Collection<DataModel> getDataModelsCollection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getFacets() {
        return facets;
    }

    @Override
    public Set<String> getDeclaredFacets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getSchemas() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getDeclaredSchemas() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentType getDocumentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLifeCyclePolicy() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public DocumentPart getPart(String schema) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public DocumentPart[] getParts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> getPropertyObjects(String schema) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getProperties(String schemaName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(String xpath) throws PropertyException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String schemaName, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getPropertyObject(String schema, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getPropertyValue(String xpath) throws PropertyException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Serializable> T getSystemProp(String systemProperty, Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersionLabel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCheckinComment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasFacet(String facet) {
        return facets.contains(facet);
    }

    @Override
    public boolean hasSchema(String schema) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addFacet(String facet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeFacet(String facet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDownloadable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLifeCycleLoaded() {
        return lifecycleState != null;
    }

    @Override
    public boolean isLocked() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProxy() {
        return isProxy;
    }

    @Override
    public boolean isImmutable() {
        return isImmutable;
    }

    @Override
    public boolean isDirty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVersionable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPrefetched(String xpath) {
        return false;
    }

    @Override
    public boolean isPrefetched(String schemaName, String name) {
        return false;
    }

    @Override
    public void prefetchCurrentLifecycleState(String lifecycle) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putContextData(String key, Serializable value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putContextData(ScopeType scope, String key, Serializable value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(int refreshFlags, String[] schemas) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setACP(ACP acp, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock setLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock getLockInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock removeLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPathInfo(String parentPath, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperties(String schemaName, Map<String, Object> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String schemaName, String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPropertyValue(String xpath, Serializable value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public Serializable getContextData(String key) {
        if (contextData == null) {
            return null;
        }
        return contextData.getScopedValue(key);
    }

    @Override
    public boolean isCheckedOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentRef checkIn(VersioningOption option, String checkinComment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersionSeriesId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLatestVersion() {
        return false;
    }

    @Override
    public boolean isMajorVersion() {
        return false;
    }

    @Override
    public boolean isLatestMajorVersion() {
        return false;
    }

    @Override
    public boolean isVersionSeriesCheckedOut() {
        return true;
    }

    @Override
    public String getChangeToken() {
        return null;
    }

    @Override
    public Map<String, String> getBinaryFulltext() {
        return null;
    }

    @Override
    public PropertyObjectResolver getObjectResolver(String xpath) {
        return DocumentPropertyObjectResolverImpl.create(this, xpath);
    }

}
