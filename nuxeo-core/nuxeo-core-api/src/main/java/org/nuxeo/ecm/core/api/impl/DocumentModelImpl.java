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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.impl;

import static org.apache.commons.lang.ObjectUtils.NULL;
import static org.nuxeo.ecm.core.schema.types.ComplexTypeImpl.canonicalXPath;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.InstanceRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterDescriptor;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterService;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.resolver.DocumentPropertyObjectResolverImpl;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.Prefetch;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Standard implementation of a {@link DocumentModel}.
 */
public class DocumentModelImpl implements DocumentModel, Cloneable {

    private static final long serialVersionUID = 1L;

    public static final String STRICT_LAZY_LOADING_POLICY_KEY = "org.nuxeo.ecm.core.strictlazyloading";

    public static final long F_VERSION = 16L;

    public static final long F_PROXY = 32L;

    public static final long F_IMMUTABLE = 256L;

    private static final Log log = LogFactory.getLog(DocumentModelImpl.class);

    protected String sid;

    protected DocumentRef ref;

    protected DocumentType type;

    // for tests, keep the type name even if no actual type is registered
    protected String typeName;

    /** Schemas including those from instance facets. */
    protected Set<String> schemas;

    /** Schemas including those from instance facets when the doc was read */
    protected Set<String> schemasOrig;

    /** Facets including those on instance. */
    protected Set<String> facets;

    /** Instance facets. */
    public Set<String> instanceFacets;

    /** Instance facets when the document was read. */
    public Set<String> instanceFacetsOrig;

    protected String id;

    protected Path path;

    protected Long pos;

    protected Map<String, DataModel> dataModels;

    protected DocumentRef parentRef;

    protected static final Lock LOCK_UNKNOWN = new Lock(null, null);

    protected Lock lock = LOCK_UNKNOWN;

    /** state is lifecycle, version stuff. */
    protected boolean isStateLoaded;

    // loaded if isStateLoaded
    protected String currentLifeCycleState;

    // loaded if isStateLoaded
    protected String lifeCyclePolicy;

    // loaded if isStateLoaded
    protected boolean isCheckedOut = true;

    // loaded if isStateLoaded
    protected String versionSeriesId;

    // loaded if isStateLoaded
    protected boolean isLatestVersion;

    // loaded if isStateLoaded
    protected boolean isMajorVersion;

    // loaded if isStateLoaded
    protected boolean isLatestMajorVersion;

    // loaded if isStateLoaded
    protected boolean isVersionSeriesCheckedOut;

    // loaded if isStateLoaded
    protected String checkinComment;

    // acp is not send between client/server
    // it will be loaded lazy first time it is accessed
    // and discarded when object is serialized
    protected transient ACP acp;

    // whether the acp was cached
    protected transient boolean isACPLoaded = false;

    // the adapters registered for this document - only valid on client
    protected transient HashMap<Class<?>, Object> adapters;

    /**
     * Flags: bitwise combination of {@link #F_VERSION}, {@link #F_PROXY}, {@link #F_IMMUTABLE}.
     */
    private long flags = 0L;

    protected String repositoryName;

    protected String sourceId;

    private ScopedMap contextData;

    // public for unit tests
    public Prefetch prefetch;

    private String detachedVersionLabel;

    protected static Boolean strictSessionManagement;

    protected DocumentModelImpl() {
    }

    /**
     * Constructor to use a document model client side without referencing a document.
     * <p>
     * It must at least contain the type.
     */
    public DocumentModelImpl(String typeName) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        if (schemaManager == null) {
            throw new NullPointerException("No registered SchemaManager");
        }
        type = schemaManager.getDocumentType(typeName);
        this.typeName = typeName;
        dataModels = new HashMap<>();
        contextData = new ScopedMap();
        instanceFacets = new HashSet<>();
        instanceFacetsOrig = new HashSet<>();
        facets = new HashSet<>();
        schemas = new HashSet<>();
        schemasOrig = new HashSet<>();
    }

    /**
     * Constructor to be used by clients.
     * <p>
     * A client constructed data model must contain at least the path and the type.
     */
    public DocumentModelImpl(String parentPath, String name, String type) {
        this(type);
        String fullPath = parentPath == null ? name : parentPath + (parentPath.endsWith("/") ? "" : "/") + name;
        path = new Path(fullPath);
        ref = new PathRef(fullPath);
        instanceFacets = new HashSet<>();
        instanceFacetsOrig = new HashSet<>();
        facets = new HashSet<>();
        schemas = new HashSet<>();
        if (getDocumentType() != null) {
            facets.addAll(getDocumentType().getFacets());
        }
        schemas = computeSchemas(getDocumentType(), instanceFacets, false);
        schemasOrig = new HashSet<>(schemas);
    }

    /**
     * Constructor.
     * <p>
     * The lock parameter is unused since 5.4.2.
     *
     * @param facets the per-instance facets
     */
    // TODO check if we use it
    public DocumentModelImpl(String sid, String type, String id, Path path, Lock lock, DocumentRef docRef,
            DocumentRef parentRef, String[] schemas, Set<String> facets, String sourceId, String repositoryName) {
        this(sid, type, id, path, docRef, parentRef, schemas, facets, sourceId, repositoryName, false);
    }

    public DocumentModelImpl(String sid, String type, String id, Path path, DocumentRef docRef, DocumentRef parentRef,
            String[] schemas, Set<String> facets, String sourceId, String repositoryName, boolean isProxy) {
        this(type);
        this.sid = sid;
        this.id = id;
        this.path = path;
        ref = docRef;
        this.parentRef = parentRef;
        instanceFacets = facets == null ? new HashSet<>() : new HashSet<>(facets);
        instanceFacetsOrig = new HashSet<>(instanceFacets);
        this.facets = new HashSet<>(instanceFacets);
        if (getDocumentType() != null) {
            this.facets.addAll(getDocumentType().getFacets());
        }
        if (schemas == null) {
            this.schemas = computeSchemas(getDocumentType(), instanceFacets, isProxy);
        } else {
            this.schemas = new HashSet<>(Arrays.asList(schemas));
        }
        schemasOrig = new HashSet<>(this.schemas);
        this.repositoryName = repositoryName;
        this.sourceId = sourceId;
        setIsProxy(isProxy);
    }

    /**
     * Recomputes effective schemas from a type + instance facets.
     */
    public static Set<String> computeSchemas(DocumentType type, Collection<String> instanceFacets, boolean isProxy) {
        Set<String> schemas = new HashSet<>();
        if (type != null) {
            schemas.addAll(Arrays.asList(type.getSchemaNames()));
        }
        TypeProvider typeProvider = Framework.getLocalService(SchemaManager.class);
        for (String facet : instanceFacets) {
            CompositeType facetType = typeProvider.getFacet(facet);
            if (facetType != null) { // ignore pseudo-facets like Immutable
                schemas.addAll(Arrays.asList(facetType.getSchemaNames()));
            }
        }
        if (isProxy) {
            for (Schema schema : typeProvider.getProxySchemas(type.getName())) {
                schemas.add(schema.getName());
            }
        }
        return schemas;
    }

    public DocumentModelImpl(DocumentModel parent, String name, String type) {
        this(parent.getPathAsString(), name, type);
    }

    @Override
    public DocumentType getDocumentType() {
        return type;
    }

    /**
     * Gets the title from the dublincore schema.
     *
     * @see DocumentModel#getTitle()
     */
    @Override
    public String getTitle() {
        String title = (String) getProperty("dublincore", "title");
        if (title != null) {
            return title;
        }
        title = getName();
        if (title != null) {
            return title;
        }
        return id;
    }

    @Override
    public String getSessionId() {
        return sid;
    }

    @Override
    public DocumentRef getRef() {
        return ref;
    }

    @Override
    public DocumentRef getParentRef() {
        if (parentRef == null && path != null) {
            if (path.isAbsolute()) {
                Path parentPath = path.removeLastSegments(1);
                parentRef = new PathRef(parentPath.toString());
            }
            // else keep parentRef null
        }
        return parentRef;
    }

    @Override
    public CoreSession getCoreSession() {
        if (sid == null) {
            return null;
        }
        try {
            return Framework.getService(CoreSessionService.class).getCoreSession(sid);
        } catch (RuntimeException e) {
            String messageTemp = "Try to get session closed %s. Document path %s, user connected %s";
            NuxeoPrincipal principal = ClientLoginModule.getCurrentPrincipal();
            String username = principal == null ? "null" : principal.getName();
            String message = String.format(messageTemp, sid, getPathAsString(), username);
            log.error(message);
            throw e;
        }
    }

    protected boolean useStrictSessionManagement() {
        if (strictSessionManagement == null) {
            strictSessionManagement = Boolean.valueOf(Framework.isBooleanPropertyTrue(STRICT_LAZY_LOADING_POLICY_KEY));
        }
        return strictSessionManagement.booleanValue();
    }

    protected CoreSession getTempCoreSession() {
        if (sid != null) {
            // detached docs need a tmp session anyway
            if (useStrictSessionManagement()) {
                throw new NuxeoException("Document " + id + " is bound to a closed CoreSession, can not reconnect");
            }
        }
        return CoreInstance.openCoreSession(repositoryName);
    }

    protected abstract class RunWithCoreSession<T> {
        public CoreSession session;

        public abstract T run();

        public T execute() {
            session = getCoreSession();
            if (session != null) {
                return run();
            } else {
                session = getTempCoreSession();
                try {
                    return run();
                } finally {
                    try {
                        session.save();
                    } finally {
                        session.close();
                    }
                }
            }
        }
    }

    @Override
    public void detach(boolean loadAll) {
        if (sid == null) {
            return;
        }
        try {
            if (loadAll) {
                for (String schema : schemas) {
                    if (!isSchemaLoaded(schema)) {
                        loadDataModel(schema);
                    }
                }
                // fetch ACP too if possible
                if (ref != null) {
                    getACP();
                }
                detachedVersionLabel = getVersionLabel();
                // load some system info
                isCheckedOut();
                getCurrentLifeCycleState();
                getLockInfo();
            }
        } finally {
            sid = null;
        }
    }

    @Override
    public void attach(String sid) {
        if (this.sid != null) {
            throw new NuxeoException("Cannot attach a document that is already attached");
        }
        this.sid = sid;
    }

    /**
     * Lazily loads the given data model.
     */
    protected DataModel loadDataModel(String schema) {

        if (log.isTraceEnabled()) {
            log.trace("lazy loading of schema " + schema + " for doc " + toString());
        }

        if (!schemas.contains(schema)) {
            return null;
        }
        if (!schemasOrig.contains(schema)) {
            // not present yet in persistent document
            DataModel dataModel = new DataModelImpl(schema);
            dataModels.put(schema, dataModel);
            return dataModel;
        }
        if (sid == null) {
            // supports non bound docs
            DataModel dataModel = new DataModelImpl(schema);
            dataModels.put(schema, dataModel);
            return dataModel;
        }
        if (ref == null) {
            return null;
        }
        // load from session
        if (getCoreSession() == null && useStrictSessionManagement()) {
            log.warn("DocumentModel " + id + " is bound to a null or closed session, "
                    + "lazy loading is not available");
            return null;
        }
        TypeProvider typeProvider = Framework.getLocalService(SchemaManager.class);
        final Schema schemaType = typeProvider.getSchema(schema);
        DataModel dataModel = new RunWithCoreSession<DataModel>() {
            @Override
            public DataModel run() {
                return session.getDataModel(ref, schemaType);
            }
        }.execute();
        dataModels.put(schema, dataModel);
        return dataModel;
    }

    @Override
    @Deprecated
    public DataModel getDataModel(String schema) {
        DataModel dataModel = dataModels.get(schema);
        if (dataModel == null) {
            dataModel = loadDataModel(schema);
        }
        return dataModel;
    }

    @Override
    @Deprecated
    public Collection<DataModel> getDataModelsCollection() {
        return dataModels.values();
    }

    public void addDataModel(DataModel dataModel) {
        dataModels.put(dataModel.getSchema(), dataModel);
    }

    @Override
    public String[] getSchemas() {
        return schemas.toArray(new String[schemas.size()]);
    }

    @Override
    @Deprecated
    public String[] getDeclaredSchemas() {
        return getSchemas();
    }

    @Override
    public boolean hasSchema(String schema) {
        return schemas.contains(schema);
    }

    @Override
    public Set<String> getFacets() {
        return Collections.unmodifiableSet(facets);
    }

    @Override
    public boolean hasFacet(String facet) {
        return facets.contains(facet);
    }

    @Override
    @Deprecated
    public Set<String> getDeclaredFacets() {
        return getFacets();
    }

    @Override
    public boolean addFacet(String facet) {
        if (facet == null) {
            throw new IllegalArgumentException("Null facet");
        }
        if (facets.contains(facet)) {
            return false;
        }
        TypeProvider typeProvider = Framework.getLocalService(SchemaManager.class);
        CompositeType facetType = typeProvider.getFacet(facet);
        if (facetType == null) {
            throw new IllegalArgumentException("No such facet: " + facet);
        }
        // add it
        facets.add(facet);
        instanceFacets.add(facet);
        schemas.addAll(Arrays.asList(facetType.getSchemaNames()));
        return true;
    }

    @Override
    public boolean removeFacet(String facet) {
        if (facet == null) {
            throw new IllegalArgumentException("Null facet");
        }
        if (!instanceFacets.contains(facet)) {
            return false;
        }
        // remove it
        facets.remove(facet);
        instanceFacets.remove(facet);

        // find the schemas that were dropped
        Set<String> droppedSchemas = new HashSet<>(schemas);
        schemas = computeSchemas(getDocumentType(), instanceFacets, isProxy());
        droppedSchemas.removeAll(schemas);

        // clear these datamodels
        for (String s : droppedSchemas) {
            dataModels.remove(s);
        }

        return true;
    }

    protected static Set<String> inferFacets(Set<String> facets, DocumentType documentType) {
        if (facets == null) {
            facets = new HashSet<>();
            if (documentType != null) {
                facets.addAll(documentType.getFacets());
            }
        }
        return facets;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        if (path != null) {
            return path.lastSegment();
        }
        return null;
    }

    @Override
    public Long getPos() {
        return pos;
    }

    /**
     * Sets the document's position in its containing folder (if ordered). Used internally during construction.
     *
     * @param pos the position
     * @since 6.0
     */
    public void setPosInternal(Long pos) {
        this.pos = pos;
    }

    @Override
    public String getPathAsString() {
        if (path != null) {
            return path.toString();
        }
        return null;
    }

    @Override
    public Map<String, Object> getProperties(String schemaName) {
        DataModel dm = getDataModel(schemaName);
        return dm == null ? null : dm.getMap();
    }

    @Override
    public Object getProperty(String schemaName, String name) {
        // look in prefetch
        if (prefetch != null) {
            Serializable value = prefetch.get(schemaName, name);
            if (value != NULL) {
                return value;
            }
        }
        // look in datamodels
        DataModel dm = dataModels.get(schemaName);
        if (dm == null) {
            dm = getDataModel(schemaName);
        }
        return dm == null ? null : dm.getData(name);
    }

    @Override
    public Property getPropertyObject(String schema, String name) {
        DocumentPart part = getPart(schema);
        return part == null ? null : part.get(name);
    }

    @Override
    public void setPathInfo(String parentPath, String name) {
        path = new Path(parentPath == null ? name : parentPath + '/' + name);
        ref = new PathRef(parentPath, name);
    }

    @Override
    public boolean isLocked() {
        return getLockInfo() != null;
    }

    @Override
    public Lock setLock() {
        Lock newLock = new RunWithCoreSession<Lock>() {
            @Override
            public Lock run() {
                return session.setLock(ref);
            }
        }.execute();
        lock = newLock;
        return lock;
    }

    @Override
    public Lock getLockInfo() {
        if (lock != LOCK_UNKNOWN) {
            return lock;
        }
        // no lock if not tied to a session
        CoreSession session = getCoreSession();
        if (session == null) {
            return null;
        }
        lock = session.getLockInfo(ref);
        return lock;
    }

    @Override
    public Lock removeLock() {
        Lock oldLock = new RunWithCoreSession<Lock>() {
            @Override
            public Lock run() {
                return session.removeLock(ref);
            }
        }.execute();
        lock = null;
        return oldLock;
    }

    @Override
    public boolean isCheckedOut() {
        if (!isStateLoaded) {
            if (getCoreSession() == null) {
                return true;
            }
            refresh(REFRESH_STATE, null);
        }
        return isCheckedOut;
    }

    @Override
    public void checkOut() {
        getCoreSession().checkOut(ref);
        isStateLoaded = false;
        // new version number, refresh content
        refresh(REFRESH_CONTENT_IF_LOADED, null);
    }

    @Override
    public DocumentRef checkIn(VersioningOption option, String description) {
        DocumentRef versionRef = getCoreSession().checkIn(ref, option, description);
        isStateLoaded = false;
        // new version number, refresh content
        refresh(REFRESH_CONTENT_IF_LOADED, null);
        return versionRef;
    }

    @Override
    public String getVersionLabel() {
        if (detachedVersionLabel != null) {
            return detachedVersionLabel;
        }
        if (getCoreSession() == null) {
            return null;
        }
        return getCoreSession().getVersionLabel(this);
    }

    @Override
    public String getVersionSeriesId() {
        if (!isStateLoaded) {
            refresh(REFRESH_STATE, null);
        }
        return versionSeriesId;
    }

    @Override
    public boolean isLatestVersion() {
        if (!isStateLoaded) {
            refresh(REFRESH_STATE, null);
        }
        return isLatestVersion;
    }

    @Override
    public boolean isMajorVersion() {
        if (!isStateLoaded) {
            refresh(REFRESH_STATE, null);
        }
        return isMajorVersion;
    }

    @Override
    public boolean isLatestMajorVersion() {
        if (!isStateLoaded) {
            refresh(REFRESH_STATE, null);
        }
        return isLatestMajorVersion;
    }

    @Override
    public boolean isVersionSeriesCheckedOut() {
        if (!isStateLoaded) {
            refresh(REFRESH_STATE, null);
        }
        return isVersionSeriesCheckedOut;
    }

    @Override
    public String getCheckinComment() {
        if (!isStateLoaded) {
            refresh(REFRESH_STATE, null);
        }
        return checkinComment;
    }

    @Override
    public ACP getACP() {
        if (!isACPLoaded) { // lazy load
            acp = new RunWithCoreSession<ACP>() {
                @Override
                public ACP run() {
                    return session.getACP(ref);
                }
            }.execute();
            isACPLoaded = true;
        }
        return acp;
    }

    @Override
    public void setACP(final ACP acp, final boolean overwrite) {
        new RunWithCoreSession<Object>() {
            @Override
            public Object run() {
                session.setACP(ref, acp, overwrite);
                return null;
            }
        }.execute();
        isACPLoaded = false;
    }

    @Override
    public String getType() {
        return typeName;
    }

    @Override
    public void setProperties(String schemaName, Map<String, Object> data) {
        DataModel dm = getDataModel(schemaName);
        if (dm != null) {
            dm.setMap(data);
            clearPrefetch(schemaName);
        }
    }

    @Override
    public void setProperty(String schemaName, String name, Object value) {
        DataModel dm = getDataModel(schemaName);
        if (dm == null) {
            return;
        }
        dm.setData(name, value);
        clearPrefetch(schemaName);
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public Map<String, DataModel> getDataModels() {
        return dataModels;
    }

    @Override
    public boolean isFolder() {
        return hasFacet(FacetNames.FOLDERISH);
    }

    @Override
    public boolean isVersionable() {
        return hasFacet(FacetNames.VERSIONABLE);
    }

    @Override
    public boolean isDownloadable() {
        if (hasFacet(FacetNames.DOWNLOADABLE)) {
            // TODO find a better way to check size that does not depend on the
            // document schema
            Long size = (Long) getProperty("common", "size");
            if (size != null) {
                return size.longValue() != 0;
            }
        }
        return false;
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) {
        for (DocumentPart dp : getParts()) {
            ((DocumentPartImpl) dp).visitChildren(visitor, arg);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> itf) {
        T facet = (T) getAdapters().get(itf);
        if (facet == null) {
            facet = findAdapter(itf);
            if (facet != null) {
                adapters.put(itf, facet);
            }
        }
        return facet;
    }

    /**
     * Lazy initialization for adapters because they don't survive the serialization.
     */
    private Map<Class<?>, Object> getAdapters() {
        if (adapters == null) {
            adapters = new HashMap<>();
        }

        return adapters;
    }

    @Override
    public <T> T getAdapter(Class<T> itf, boolean refreshCache) {
        T facet;

        if (!refreshCache) {
            facet = getAdapter(itf);
        } else {
            facet = findAdapter(itf);
        }

        if (facet != null) {
            getAdapters().put(itf, facet);
        }
        return facet;
    }

    @SuppressWarnings("unchecked")
    private <T> T findAdapter(Class<T> itf) {
        DocumentAdapterService svc = Framework.getService(DocumentAdapterService.class);
        if (svc != null) {
            DocumentAdapterDescriptor dae = svc.getAdapterDescriptor(itf);
            if (dae != null) {
                String facet = dae.getFacet();
                if (facet == null) {
                    // if no facet is specified, accept the adapter
                    return (T) dae.getFactory().getAdapter(this, itf);
                } else if (hasFacet(facet)) {
                    return (T) dae.getFactory().getAdapter(this, itf);
                } else {
                    // TODO: throw an exception
                    log.error("Document model cannot be adapted to " + itf + " because it has no facet " + facet);
                }
            }
        } else {
            log.warn("DocumentAdapterService not available. Cannot get document model adaptor for " + itf);
        }
        return null;
    }

    @Override
    public boolean followTransition(final String transition) {
        boolean res = new RunWithCoreSession<Boolean>() {
            @Override
            public Boolean run() {
                return Boolean.valueOf(session.followTransition(ref, transition));
            }
        }.execute().booleanValue();
        // Invalidate the prefetched value in this case.
        if (res) {
            currentLifeCycleState = null;
        }
        return res;
    }

    @Override
    public Collection<String> getAllowedStateTransitions() {
        return new RunWithCoreSession<Collection<String>>() {
            @Override
            public Collection<String> run() {
                return session.getAllowedStateTransitions(ref);
            }
        }.execute();
    }

    @Override
    public String getCurrentLifeCycleState() {
        if (currentLifeCycleState != null) {
            return currentLifeCycleState;
        }
        // document was just created => not life cycle yet
        if (sid == null) {
            return null;
        }
        currentLifeCycleState = new RunWithCoreSession<String>() {
            @Override
            public String run() {
                return session.getCurrentLifeCycleState(ref);
            }
        }.execute();
        return currentLifeCycleState;
    }

    @Override
    public String getLifeCyclePolicy() {
        if (lifeCyclePolicy != null) {
            return lifeCyclePolicy;
        }
        // String lifeCyclePolicy = null;
        lifeCyclePolicy = new RunWithCoreSession<String>() {
            @Override
            public String run() {
                return session.getLifeCyclePolicy(ref);
            }
        }.execute();
        return lifeCyclePolicy;
    }

    @Override
    public boolean isVersion() {
        return (flags & F_VERSION) != 0;
    }

    @Override
    public boolean isProxy() {
        return (flags & F_PROXY) != 0;
    }

    @Override
    public boolean isImmutable() {
        return (flags & F_IMMUTABLE) != 0;
    }

    public void setIsVersion(boolean isVersion) {
        if (isVersion) {
            flags |= F_VERSION;
        } else {
            flags &= ~F_VERSION;
        }
    }

    public void setIsProxy(boolean isProxy) {
        if (isProxy) {
            flags |= F_PROXY;
        } else {
            flags &= ~F_PROXY;
        }
    }

    public void setIsImmutable(boolean isImmutable) {
        if (isImmutable) {
            flags |= F_IMMUTABLE;
        } else {
            flags &= ~F_IMMUTABLE;
        }
    }

    @Override
    public boolean isDirty() {
        for (DataModel dm : dataModels.values()) {
            DocumentPart part = ((DataModelImpl) dm).getDocumentPart();
            if (part.isDirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ScopedMap getContextData() {
        return contextData;
    }

    @Override
    public Serializable getContextData(ScopeType scope, String key) {
        return contextData.getScopedValue(scope, key);
    }

    @Override
    public void putContextData(ScopeType scope, String key, Serializable value) {
        contextData.putScopedValue(scope, key, value);
    }

    @Override
    public Serializable getContextData(String key) {
        return contextData.getScopedValue(key);
    }

    @Override
    public void putContextData(String key, Serializable value) {
        contextData.putScopedValue(key, value);
    }

    @Override
    public void copyContextData(DocumentModel otherDocument) {
        ScopedMap otherMap = otherDocument.getContextData();
        if (otherMap != null) {
            contextData.putAll(otherMap);
        }
    }

    @Override
    public void copyContent(DocumentModel sourceDoc) {
        computeFacetsAndSchemas(((DocumentModelImpl) sourceDoc).instanceFacets);
        Map<String, DataModel> newDataModels = new HashMap<>();
        for (String key : schemas) {
            DataModel oldDM = sourceDoc.getDataModel(key);
            DataModel newDM;
            if (oldDM != null) {
                newDM = cloneDataModel(oldDM);
            } else {
                // create an empty datamodel
                Schema schema = Framework.getService(SchemaManager.class).getSchema(key);
                newDM = new DataModelImpl(new DocumentPartImpl(schema));
            }
            newDataModels.put(key, newDM);
        }
        dataModels = newDataModels;
    }

    @SuppressWarnings("unchecked")
    public static Object cloneField(Field field, String key, Object value) {
        // key is unused
        Object clone;
        Type type = field.getType();
        if (type.isSimpleType()) {
            // CLONE TODO
            if (value instanceof Calendar) {
                Calendar newValue = (Calendar) value;
                clone = newValue.clone();
            } else {
                clone = value;
            }
        } else if (type.isListType()) {
            ListType ltype = (ListType) type;
            Field lfield = ltype.getField();
            Type ftype = lfield.getType();
            List<Object> list;
            if (value instanceof Object[]) { // these are stored as arrays
                list = Arrays.asList((Object[]) value);
            } else {
                list = (List<Object>) value;
            }
            if (ftype.isComplexType()) {
                List<Object> clonedList = new ArrayList<>(list.size());
                for (Object o : list) {
                    clonedList.add(cloneField(lfield, null, o));
                }
                clone = clonedList;
            } else {
                Class<?> klass = JavaTypes.getClass(ftype);
                if (klass.isPrimitive()) {
                    clone = PrimitiveArrays.toPrimitiveArray(list, klass);
                } else {
                    clone = list.toArray((Object[]) Array.newInstance(klass, list.size()));
                }
            }
        } else {
            // complex type
            ComplexType ctype = (ComplexType) type;
            if (TypeConstants.isContentType(ctype)) { // if a blob
                Blob blob = (Blob) value; // TODO
                clone = blob;
            } else {
                // a map, regular complex type
                Map<String, Object> map = (Map<String, Object>) value;
                Map<String, Object> clonedMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object v = entry.getValue();
                    String k = entry.getKey();
                    if (v == null) {
                        continue;
                    }
                    clonedMap.put(k, cloneField(ctype.getField(k), k, v));
                }
                clone = clonedMap;
            }
        }
        return clone;
    }

    public static DataModel cloneDataModel(Schema schema, DataModel data) {
        DataModel dm = new DataModelImpl(schema.getName());
        for (Field field : schema.getFields()) {
            String key = field.getName().getLocalName();
            Object value;
            try {
                value = data.getData(key);
            } catch (PropertyException e1) {
                continue;
            }
            if (value == null) {
                continue;
            }
            Object clone = cloneField(field, key, value);
            dm.setData(key, clone);
        }
        return dm;
    }

    public DataModel cloneDataModel(DataModel data) {
        TypeProvider typeProvider = Framework.getLocalService(SchemaManager.class);
        return cloneDataModel(typeProvider.getSchema(data.getSchema()), data);
    }

    @Override
    public String getCacheKey() {
        // UUID - sessionId
        String key = id + '-' + sid + '-' + getPathAsString();
        // assume the doc holds the dublincore schema (enough for us right now)
        if (hasSchema("dublincore")) {
            Calendar timeStamp = (Calendar) getProperty("dublincore", "modified");
            if (timeStamp != null) {
                // remove milliseconds as they are not stored in some
                // databases, which could make the comparison fail just after a
                // document creation (see NXP-8783)
                timeStamp.set(Calendar.MILLISECOND, 0);
                key += '-' + String.valueOf(timeStamp.getTimeInMillis());
            }
        }
        return key;
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    public boolean isSchemaLoaded(String name) {
        return dataModels.containsKey(name);
    }

    @Override
    public boolean isPrefetched(String xpath) {
        return prefetch != null && prefetch.isPrefetched(xpath);
    }

    @Override
    public boolean isPrefetched(String schemaName, String name) {
        return prefetch != null && prefetch.isPrefetched(schemaName, name);
    }

    /**
     * Sets prefetch information.
     * <p>
     * INTERNAL: This method is not in the public interface.
     *
     * @since 5.5
     */
    public void setPrefetch(Prefetch prefetch) {
        this.prefetch = prefetch;
    }

    @Override
    public void prefetchCurrentLifecycleState(String lifecycle) {
        currentLifeCycleState = lifecycle;
    }

    @Override
    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        this.lifeCyclePolicy = lifeCyclePolicy;
    }

    @Override
    // need this for tree in RCP clients
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DocumentModelImpl) {
            DocumentModel documentModel = (DocumentModel) obj;
            String id = documentModel.getId();
            if (id != null) {
                return id.equals(this.id);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        String title = id;
        if (getDataModels().containsKey("dublincore")) {
            title = getTitle();
        }
        return getClass().getSimpleName() + '(' + id + ", path=" + path + ", title=" + title + ')';
    }

    @Override
    public <T extends Serializable> T getSystemProp(final String systemProperty, final Class<T> type) {
        return new RunWithCoreSession<T>() {
            @Override
            public T run() {
                return session.getDocumentSystemProp(ref, systemProperty, type);
            }
        }.execute();
    }

    @Override
    public boolean isLifeCycleLoaded() {
        return currentLifeCycleState != null;
    }

    @Override
    @Deprecated
    public DocumentPart getPart(String schema) {
        DataModel dm = getDataModel(schema);
        if (dm != null) {
            return ((DataModelImpl) dm).getDocumentPart();
        }
        return null; // TODO thrown an exception?
    }

    @Override
    @Deprecated
    public DocumentPart[] getParts() {
        // DocumentType type = getDocumentType();
        // type = Framework.getService(SchemaManager.class).getDocumentType(
        // getType());
        // Collection<Schema> schemas = type.getSchemas();
        // Set<String> allSchemas = getAllSchemas();
        DocumentPart[] parts = new DocumentPart[schemas.size()];
        int i = 0;
        for (String schema : schemas) {
            DataModel dm = getDataModel(schema);
            parts[i++] = ((DataModelImpl) dm).getDocumentPart();
        }
        return parts;
    }

    @Override
    public Collection<Property> getPropertyObjects(String schema) {
        DocumentPart part = getPart(schema);
        return part == null ? Collections.emptyList() : part.getChildren();
    }

    @Override
    public Property getProperty(String xpath) {
        if (xpath == null) {
            throw new PropertyNotFoundException("null", "Invalid null xpath");
        }
        String cxpath = canonicalXPath(xpath);
        if (cxpath.isEmpty()) {
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String schemaName = getXPathSchemaName(cxpath, schemas, null);
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
            return part.resolvePath(partPath);
        } catch (PropertyNotFoundException e) {
            throw new PropertyNotFoundException(xpath, e.getDetail());
        }
    }

    public static String getXPathSchemaName(String xpath, Set<String> docSchemas, String[] returnName) {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        // find first segment
        int i = xpath.indexOf('/');
        String prop = i == -1 ? xpath : xpath.substring(0, i);
        int p = prop.indexOf(':');
        if (p != -1) {
            // prefixed
            String prefix = prop.substring(0, p);
            Schema schema = schemaManager.getSchemaFromPrefix(prefix);
            if (schema == null) {
                // try directly with prefix as a schema name
                schema = schemaManager.getSchema(prefix);
                if (schema == null) {
                    return null;
                }
            }
            if (returnName != null) {
                returnName[0] = prop.substring(p + 1);
            }
            return schema.getName();
        } else {
            // unprefixed
            // search for the first matching schema having a property
            // with the same name as the first path segment
            for (String schemaName : docSchemas) {
                Schema schema = schemaManager.getSchema(schemaName);
                if (schema != null && schema.hasField(prop)) {
                    if (returnName != null) {
                        returnName[0] = prop;
                    }
                    return schema.getName();
                }
            }
            return null;
        }
    }

    @Override
    public Serializable getPropertyValue(String xpath) throws PropertyException {
        if (prefetch != null) {
            Serializable value = prefetch.get(xpath);
            if (value != NULL) {
                return value;
            }
        }
        return getProperty(xpath).getValue();
    }

    @Override
    public void setPropertyValue(String xpath, Serializable value) throws PropertyException {
        getProperty(xpath).setValue(value);
        clearPrefetchXPath(xpath);
    }

    private void clearPrefetch(String schemaName) {
        if (prefetch != null) {
            prefetch.clearPrefetch(schemaName);
            if (prefetch.isEmpty()) {
                prefetch = null;
            }
        }
    }

    protected void clearPrefetchXPath(String xpath) {
        if (prefetch != null) {
            String schemaName = prefetch.getXPathSchema(xpath, getDocumentType());
            if (schemaName != null) {
                clearPrefetch(schemaName);
            }
        }
    }

    @Override
    public DocumentModel clone() throws CloneNotSupportedException {
        DocumentModelImpl dm = (DocumentModelImpl) super.clone();
        // dm.id =id;
        // dm.acp = acp;
        // dm.currentLifeCycleState = currentLifeCycleState;
        // dm.lifeCyclePolicy = lifeCyclePolicy;
        // dm.declaredSchemas = declaredSchemas; // schemas are immutable so we
        // don't clone the array
        // dm.flags = flags;
        // dm.repositoryName = repositoryName;
        // dm.ref = ref;
        // dm.parentRef = parentRef;
        // dm.path = path; // path is immutable
        // dm.isACPLoaded = isACPLoaded;
        // dm.prefetch = dm.prefetch; // prefetch can be shared
        // dm.lock = lock;
        // dm.sourceId =sourceId;
        // dm.sid = sid;
        // dm.type = type;
        dm.facets = new HashSet<String>(facets); // facets
        // should be
        // clones too -
        // they are not
        // immutable
        // context data is keeping contextual info so it is reseted
        dm.contextData = new ScopedMap();

        // copy parts
        dm.dataModels = new HashMap<>();
        for (Map.Entry<String, DataModel> entry : dataModels.entrySet()) {
            String key = entry.getKey();
            DataModel data = entry.getValue();
            DataModelImpl newData = new DataModelImpl(key, data.getMap());
            for (String name : data.getDirtyFields()) {
                newData.setDirty(name);
            }
            dm.dataModels.put(key, newData);
        }
        return dm;
    }

    @Override
    public void reset() {
        if (dataModels != null) {
            dataModels.clear();
        }
        prefetch = null;
        isACPLoaded = false;
        acp = null;
        currentLifeCycleState = null;
        lifeCyclePolicy = null;
    }

    @Override
    public void refresh() {
        detachedVersionLabel = null;

        refresh(REFRESH_DEFAULT, null);
    }

    @Override
    public void refresh(int refreshFlags, String[] schemas) {
        if (id == null) {
            // not yet saved
            return;
        }
        if ((refreshFlags & REFRESH_ACP_IF_LOADED) != 0 && isACPLoaded) {
            refreshFlags |= REFRESH_ACP;
            // we must not clean the REFRESH_ACP_IF_LOADED flag since it is
            // used
            // below on the client
        }

        if ((refreshFlags & REFRESH_CONTENT_IF_LOADED) != 0) {
            refreshFlags |= REFRESH_CONTENT;
            Collection<String> keys = dataModels.keySet();
            schemas = keys.toArray(new String[keys.size()]);
        }

        DocumentModelRefresh refresh = getCoreSession().refreshDocument(ref, refreshFlags, schemas);

        if ((refreshFlags & REFRESH_PREFETCH) != 0) {
            prefetch = refresh.prefetch;
        }
        if ((refreshFlags & REFRESH_STATE) != 0) {
            currentLifeCycleState = refresh.lifeCycleState;
            lifeCyclePolicy = refresh.lifeCyclePolicy;
            isCheckedOut = refresh.isCheckedOut;
            isLatestVersion = refresh.isLatestVersion;
            isMajorVersion = refresh.isMajorVersion;
            isLatestMajorVersion = refresh.isLatestMajorVersion;
            isVersionSeriesCheckedOut = refresh.isVersionSeriesCheckedOut;
            versionSeriesId = refresh.versionSeriesId;
            checkinComment = refresh.checkinComment;
            isStateLoaded = true;
        }
        acp = null;
        isACPLoaded = false;
        if ((refreshFlags & REFRESH_ACP) != 0) {
            acp = refresh.acp;
            isACPLoaded = true;
        }

        if ((refreshFlags & (REFRESH_CONTENT | REFRESH_CONTENT_LAZY)) != 0) {
            dataModels.clear();
            computeFacetsAndSchemas(refresh.instanceFacets);
        }
        if ((refreshFlags & REFRESH_CONTENT) != 0) {
            DocumentPart[] parts = refresh.documentParts;
            if (parts != null) {
                for (DocumentPart part : parts) {
                    DataModelImpl dm = new DataModelImpl(part);
                    dataModels.put(dm.getSchema(), dm);
                }
            }
        }
    }

    /**
     * Recomputes all facets and schemas from the instance facets.
     *
     * @since 7.1
     */
    protected void computeFacetsAndSchemas(Set<String> instanceFacets) {
        this.instanceFacets = instanceFacets;
        instanceFacetsOrig = new HashSet<>(instanceFacets);
        facets = new HashSet<>(instanceFacets);
        facets.addAll(getDocumentType().getFacets());
        if (isImmutable()) {
            facets.add(FacetNames.IMMUTABLE);
        }
        schemas = computeSchemas(getDocumentType(), instanceFacets, isProxy());
        schemasOrig = new HashSet<>(schemas);
    }

    @Override
    public String getChangeToken() {
        if (!hasSchema("dublincore")) {
            return null;
        }
        try {
            Calendar modified = (Calendar) getPropertyValue("dc:modified");
            if (modified != null) {
                return String.valueOf(modified.getTimeInMillis());
            }
        } catch (PropertyException e) {
            log.error("Error while retrieving dc:modified", e);
        }
        return null;
    }

    /**
     * Sets the document id. May be useful when detaching from a repo and attaching to another one or when unmarshalling
     * a documentModel from a XML or JSON representation
     *
     * @since 5.7.2
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Map<String, String> getBinaryFulltext() {
        CoreSession session = getCoreSession();
        if (session == null) {
            return null;
        }
        return session.getBinaryFulltext(ref);
    }

    @Override
    public PropertyObjectResolver getObjectResolver(String xpath) {
        return DocumentPropertyObjectResolverImpl.create(this, xpath);
    }

    /**
     * Replace the content by it's the reference if the document is live and not dirty.
     *
     * @see org.nuxeo.ecm.core.event.EventContext
     * @since 7.10
     */
    private Object writeReplace() throws ObjectStreamException {
        if (!TransactionHelper.isTransactionActive()) { // protect from no transaction
            Transaction tx = TransactionHelper.suspendTransaction();
            try {
                TransactionHelper.startTransaction();
                try {
                    return writeReplace();
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            } finally {
                if (tx != null) {
                    TransactionHelper.resumeTransaction(tx);
                }
            }
        }
        if (isDirty()) {
            return this;
        }
        CoreSession session = getCoreSession();
        if (session == null) {
            return this;
        }
        if (!session.exists(ref)) {
            return this;
        }
        return new InstanceRef(this, session.getPrincipal());
    }

    /**
     * Legacy code: Explicitly detach the document to send the document as an event context parameter.
     *
     * @see org.nuxeo.ecm.core.event.EventContext
     * @since 7.10
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        CoreSession session = getCoreSession();
        detach(session != null && ref != null && session.exists(ref));
        stream.defaultWriteObject();
    }

}
