/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.impl;

import static org.nuxeo.ecm.core.schema.types.ComplexTypeImpl.canonicalXPath;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.resolver.DocumentPropertyObjectResolverImpl;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * A DocumentModel that can have any schema and is not made persistent by itself. A mockup to keep arbitrary schema
 * data.
 */
public class SimpleDocumentModel implements DocumentModel {

    private static final long serialVersionUID = 1L;

    protected final Map<String, DataModel> dataModels = new HashMap<>();

    protected final Set<String> schemas = new HashSet<>();

    protected final Set<String> facets = new HashSet<>();

    protected final Map<String, Serializable> contextData = new HashMap<>();

    protected final boolean anySchema;

    protected Path path;

    protected String type;

    /**
     * @deprecated since 11.1. Use {@link #empty()} instead.
     */
    @Deprecated(since = "11.1")
    public SimpleDocumentModel() {
        anySchema = true;
    }

    /**
     * Returns a new empty {@link SimpleDocumentModel} instance.
     *
     * @since 11.1
     */
    public static SimpleDocumentModel empty() {
        return new SimpleDocumentModel();
    }

    /**
     * @since 11.1
     */
    protected SimpleDocumentModel(DocumentType documentType) {
        anySchema = false;
        type = documentType.getName();
        initSchemas(List.of(documentType.getSchemaNames()));
    }

    /**
     * Returns a {@link SimpleDocumentModel} instance initialized with the given {@code type} and its related schemas.
     *
     * @since 11.1
     */
    public static SimpleDocumentModel ofType(String type) {
        SchemaManager service = Framework.getService(SchemaManager.class);
        DocumentType dType = service.getDocumentType(type);
        return new SimpleDocumentModel(dType);
    }

    /**
     * @deprecated since 11.1. Use {@link #ofSchemas(List)} instead.
     */
    @Deprecated(since = "11.1")
    public SimpleDocumentModel(List<String> schemas) {
        anySchema = false;
        initSchemas(schemas);
    }

    /**
     * Returns a {@link SimpleDocumentModel} instance initialized with the given {@code schemas}.
     *
     * @since 11.1
     */
    public static SimpleDocumentModel ofSchemas(List<String> schemas) {
        return new SimpleDocumentModel(schemas);
    }

    /**
     * @deprecated since 11.1. Use {@link #ofSchemas(String, String...)} instead.
     */
    @Deprecated(since = "11.1")
    public SimpleDocumentModel(String... schemas) {
        this(List.of(schemas));
    }

    /**
     * Returns a {@link SimpleDocumentModel} instance initialized with the given {@code schema} and optional
     * {@code schemas}.
     *
     * @since 11.1
     */
    public static SimpleDocumentModel ofSchemas(String schema, String... schemas) {
        return ofSchemas(Stream.concat(Stream.of(schema), Stream.of(schemas)).collect(Collectors.toList()));
    }

    protected final void initSchemas(List<String> schemas) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        for (String schema : schemas) {
            Schema s = schemaManager.getSchema(schema);
            DocumentPart part = new DocumentPartImpl(s);
            dataModels.put(schema, new DataModelImpl(part));
            this.schemas.add(schema);
        }
    }

    protected DataModel getDataModelInternal(String schema) {
        DataModel dm = dataModels.get(schema);
        if (dm == null && anySchema) {
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            Schema s = schemaManager.getSchema(schema);
            DocumentPart part = new DocumentPartImpl(s);
            dm = new DataModelImpl(part);
            dataModels.put(schema, dm);
            schemas.add(schema);
        }
        return dm;
    }

    @Override
    public String[] getSchemas() {
        Set<String> keys = dataModels.keySet();
        return keys.toArray(new String[0]);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Object getProperty(String schemaName, String name) {
        DataModel dm = getDataModelInternal(schemaName);
        return dm != null ? dm.getData(name) : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Property getPropertyObject(String schema, String name) {
        DocumentPart part = getPart(schema);
        return part == null ? null : part.get(name);
    }

    @Override
    public void setProperty(String schemaName, String name, Object value) {
        if (name.contains(":")) {
            name = name.substring(name.indexOf(':'));
        }
        getDataModelInternal(schemaName).setData(name, value);
    }

    @Override
    public Map<String, Object> getProperties(String schemaName) {
        return getDataModelInternal(schemaName).getMap();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setProperties(String schemaName, Map<String, Object> data) {
        DataModel dm = getDataModelInternal(schemaName);
        dm.setMap(data);
        // force dirty for updated properties
        for (String field : data.keySet()) {
            dm.setDirty(field);
        }
    }

    @Override
    public Map<String, Serializable> getContextData() {
        return contextData;
    }

    @Override
    public Serializable getContextData(String key) {
        return contextData.get(key);
    }

    @Override
    public void putContextData(String key, Serializable value) {
        contextData.put(key, value);
    }

    @Override
    public void copyContextData(DocumentModel otherDocument) {
        contextData.putAll(otherDocument.getContextData());
    }

    @Override
    @SuppressWarnings("deprecation")
    public Property getProperty(String xpath) throws PropertyException {
        if (xpath == null) {
            throw new PropertyNotFoundException("null", "Invalid null xpath");
        }
        String cxpath = canonicalXPath(xpath);
        if (cxpath.isEmpty()) {
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String schemaName = DocumentModelImpl.getXPathSchemaName(cxpath, schemas, null);
        if (schemaName == null) {
            if (cxpath.indexOf(':') != -1) {
                throw new PropertyNotFoundException(xpath, "No such schema");
            } else {
                throw new PropertyNotFoundException(xpath);
            }

        }
        DocumentPart part = getPart(schemaName);
        if (part == null) {
            throw new PropertyNotFoundException(xpath);
        }
        // cut prefix
        String partPath = cxpath.substring(cxpath.indexOf(':') + 1);
        try {
            Property property = part.resolvePath(partPath);
            // force dirty for updated properties
            property.setForceDirty(true);
            return property;
        } catch (PropertyNotFoundException e) {
            throw new PropertyNotFoundException(xpath, e.getDetail());
        }
    }

    @Override
    public Serializable getPropertyValue(String xpath) throws PropertyException {
        return getProperty(xpath).getValue();
    }

    @Override
    public void setPropertyValue(String xpath, Serializable value) {
        getProperty(xpath).setValue(value);
    }

    @Override
    public DocumentType getDocumentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException();
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
    public DocumentRef getRef() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentRef getParentRef() {
        if (path == null) {
            return null;
        }
        if (!path.isAbsolute()) {
            return null;
        }
        return new PathRef(path.removeLastSegments(1).toString());
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return path == null ? null : path.lastSegment();
    }

    @Override
    public Long getPos() {
        return null;
    }

    @Override
    public String getPathAsString() {
        return path == null ? null : path.toString();
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType() {
        return type;
    }

    /**
     * @deprecated since 11.1. Use {@link #ofType(String)}.
     */
    @Deprecated(since = "11.1")
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Set<String> getFacets() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Collection<DataModel> getDataModelsCollection() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Map<String, DataModel> getDataModels() {
        return dataModels;
    }

    @Override
    @Deprecated
    public DataModel getDataModel(String schema) {
        return getDataModelInternal(schema);
    }

    @Override
    public void setPathInfo(String parentPath, String name) {
        path = new Path(parentPath == null ? name : parentPath + '/' + name);
    }

    @Override
    public boolean isLocked() {
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
    public ACP getACP() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setACP(ACP acp, boolean overwrite) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSchema(String schema) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasFacet(String facet) {
        return facets.contains(facet);
    }

    @Override
    public boolean addFacet(String facet) {
        if (facet == null) {
            throw new IllegalArgumentException("Null facet");
        }
        if (facets.contains(facet)) {
            return false;
        }
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        CompositeType facetType = schemaManager.getFacet(facet);
        if (facetType == null) {
            throw new IllegalArgumentException("No such facet: " + facet);
        }
        // add it
        facets.add(facet);
        schemas.addAll(Arrays.asList(facetType.getSchemaNames()));

        for (Schema schema : facetType.getSchemas()) {
            DocumentPart part = new DocumentPartImpl(schema);
            dataModels.put(schema.getName(), new DataModelImpl(part));
        }

        return true;
    }

    @Override
    public boolean removeFacet(String facet) {
        // not implemented for now because logic is complex as we need to know initial type/schema
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTrashed() {
        return LifeCycleConstants.DELETED_STATE.equals(getCurrentLifeCycleState());
    }

    @Override
    public boolean isFolder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVersionable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDownloadable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProxy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImmutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirty() {
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
    public String getCurrentLifeCycleState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLifeCyclePolicy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean followTransition(String transition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getAllowedStateTransitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyContent(DocumentModel sourceDoc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRepositoryName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCacheKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceId() {
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
    public boolean isLifeCycleLoaded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Serializable> T getSystemProp(String systemProperty, Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public DocumentPart getPart(String schema) {
        DataModel dm = getDataModel(schema);
        if (dm != null) {
            return ((DataModelImpl) dm).getDocumentPart();
        }
        return null;
    }

    @Override
    @Deprecated
    public DocumentPart[] getParts() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Collection<Property> getPropertyObjects(String schema) {
        DocumentPart part = getPart(schema);
        return part == null ? Collections.emptyList() : part.getChildren();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(int refreshFlags, String[] schemas) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel clone() {
        throw new UnsupportedOperationException();
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
    public DocumentRef checkIn(VersioningOption option, String description) {
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
