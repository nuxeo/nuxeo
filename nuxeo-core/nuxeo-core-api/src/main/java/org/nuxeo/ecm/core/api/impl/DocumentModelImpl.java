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

package org.nuxeo.ecm.core.api.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ArrayMap;
import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.common.utils.Null;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DataModelMap;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterDescriptor;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterService;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeRef;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @version $Revision: 1.0 $
 */
@SuppressWarnings({ "SuppressionAnnotation" })
public class DocumentModelImpl implements DocumentModel, Cloneable {

    public static final long F_STORED = 1L;

    public static final long F_DETACHED = 2L;

    // reserved: 4, 8

    public static final long F_VERSION = 16L;

    public static final long F_PROXY = 32L;

    public static final long F_LOCKED = 64L;

    public static final long F_DIRTY = 128L;

    private static final long serialVersionUID = 4473357367146978325L;

    private static final Log log = LogFactory.getLog(DocumentModelImpl.class);

    protected String sid;

    protected DocumentRef ref;

    protected TypeRef<DocumentType> type;

    protected String[] declaredSchemas;

    protected Set<String> declaredFacets;

    protected String id;

    protected Path path;

    protected DataModelMap dataModels;

    protected DocumentRef parentRef;

    protected String lock;

    // acp is not send between client/server
    // it will be loaded lazy first time it is accessed
    // and discarded when object is serialized
    protected transient ACP acp;

    // whether the acp was cached
    protected transient boolean isACPLoaded = false;

    // the adapters registered for this document - only vaild on client
    protected transient ArrayMap<Class, Object> adapters;

    // flags : TODO
    // bit 0 - IS_STORED (1 if stored in repo, 0 otherwise)
    // bit 1 - IS_DETACHED (1 after deserialization, 0 otherwise)
    // bit 2 - 3: reserved for future use
    // bit 4: IS_VERSION (true if set)
    // bit 5: IS_PROXY (true if set)
    // bit 6: IS_LOCKED (true if set)
    // bit 7: IS_DIRTY (true if set)
    protected long flags = 0L;

    protected String repositoryName;

    protected String sourceId;

    private ScopedMap contextData;

    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    protected HashMap<String, Serializable> prefetch;

    private String currentLifeCycleState;

    private String lifeCyclePolicy;

    protected DocumentModelImpl() {
    }

    /**
     * Constructor to use a document model client side without referencing a
     * document.
     * <p>
     * It must at least contain the type.
     *
     * @param type String
     */
    public DocumentModelImpl(String type) {
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        dataModels = new DataModelMapImpl();
        contextData = new ScopedMap();
    }

    /**
     * Constructor to use a document model client side without referencing a
     * document.
     * <p>
     * It must at least contain the type.
     *
     * @param sid String
     * @param type String
     */
    public DocumentModelImpl(String sid, String type) {
        this(type);
        this.sid = sid;
    }

    /**
     * Constructor to be used by clients.
     * <p>
     * A client constructed data model must contain at least the path and the
     * type.
     *
     * @param parentPath
     * @param name
     * @param type
     */
    public DocumentModelImpl(String parentPath, String name, String type) {
        this(parentPath, name, type, null);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param parent DocumentModel
     * @param name String
     * @param type String
     */
    public DocumentModelImpl(DocumentModel parent, String name, String type) {
        this(parent.getPathAsString(), name, type, null);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param parent DocumentModel
     * @param name String
     * @param type String
     * @param data DataModelMap
     */
    public DocumentModelImpl(DocumentModel parent, String name, String type,
            DataModelMap data) {
        this(parent.getPathAsString(), name, type, data);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param parentPath
     * @param name
     * @param type
     * @param data allows to initialize a document with initial data
     */
    public DocumentModelImpl(String parentPath, String name, String type,
            DataModelMap data) {
        path = new Path(parentPath + '/' + name);
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        ref = new PathRef(parentPath, name);
        dataModels = data == null ? new DataModelMapImpl() : data;
        contextData = new ScopedMap();
    }

    /**
     * Constructor to be used on server side to create a document model.
     *
     * @param sid
     * @param type
     * @param id
     * @param path
     * @param docRef
     * @param parentRef
     * @param schemas
     * @param facets
     */
    public DocumentModelImpl(String sid, String type, String id, Path path,
            DocumentRef docRef, DocumentRef parentRef, String[] schemas,
            Set<String> facets) {
        this(sid, type, id, path, null, docRef, parentRef, schemas, facets);
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param sid String
     * @param type String
     * @param id String
     * @param path Path
     * @param lock String
     * @param docRef DocumentRef
     * @param parentRef DocumentRef
     * @param schemas String[]
     * @param facets
     */
    @Deprecated
    public DocumentModelImpl(String sid, String type, String id, Path path,
            String lock, DocumentRef docRef, DocumentRef parentRef,
            String[] schemas, Set<String> facets) {
        this.sid = sid;
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        this.id = id;
        this.path = path;
        this.ref = docRef;
        this.parentRef = parentRef;
        this.declaredSchemas = schemas;
        this.declaredFacets = facets;
        this.dataModels = new DataModelMapImpl();
        this.lock = lock;
        contextData = new ScopedMap();
    }

    /**
     * Constructor for DocumentModelImpl.
     *
     * @param sid String
     * @param type String
     * @param id String
     * @param path Path
     * @param lock String
     * @param docRef DocumentRef
     * @param parentRef DocumentRef
     * @param schemas String[]
     * @param facets
     * @param sourceId String
     * @param repositoryName String
     */
    public DocumentModelImpl(String sid, String type, String id, Path path,
            String lock, DocumentRef docRef, DocumentRef parentRef,
            String[] schemas, Set<String> facets, String sourceId,
            String repositoryName) {
        this.sid = sid;
        this.type = new TypeRef<DocumentType>(SchemaNames.DOCTYPES, type);
        this.id = id;
        this.path = path;
        this.ref = docRef;
        this.parentRef = parentRef;
        this.declaredSchemas = schemas;
        this.declaredFacets = facets;
        this.dataModels = new DataModelMapImpl();
        this.lock = lock;
        contextData = new ScopedMap();
        this.repositoryName = repositoryName;
        this.sourceId = sourceId;
    }

    public DocumentType getDocumentType() {
        return type.get();
   }

    /**
     * Gets the title from the dublincore schema.
     *
     * @return String
     * @see DocumentModel#getTitle()
     */
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

    /**
     * @return the sessionId
     *
     * @see DocumentModel#getSessionId()
     */
    public String getSessionId() {
        return sid;
    }

    /**
     * Method getRef.
     *
     * @return DocumentRef
     * @see DocumentModel#getRef()
     */
    public DocumentRef getRef() {
        return ref;
    }

    /**
     * Method getParentRef.
     *
     * @return DocumentRef
     * @see DocumentModel#getParentRef()
     */
    public DocumentRef getParentRef() {
        if (parentRef == null && path != null) {
            Path parentPath = path.removeLastSegments(1);
            parentRef = new PathRef(parentPath.toString());
        }
        return parentRef;
    }

    /**
     * Method getClient.
     *
     * @return CoreSession
     */
    public final CoreSession getClient() {
        if (sid == null) {
            throw new UnsupportedOperationException(
                    "Cannot load data models for client defined models");
        }
        CoreSession session = CoreInstance.getInstance().getSession(sid);
        if (session == null && sid != null && repositoryName != null) {
            // session was closed => open a new one
            try {
                RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                Repository repo = mgr.getRepository(repositoryName);
                session = repo.open();
                // set new session id
                sid = session.getSessionId();
            } catch (Exception e) {
                // do nothing
            }
        }
        return session;
    }

    /**
     * Detaches the documentImpl from its existing session, so that it can
     * survive beyond the session's closing.
     *
     * @param loadAll if {@code true}, load all data from the session before
     *            detaching
     */
    public void detach(boolean loadAll) throws ClientException {
        if (sid == null) {
            return;
        }
        if (loadAll && type != null) {
            DocumentType dt = type.get();
            if (dt != null) {
                for (String schema : dt.getSchemaNames()) {
                    if (!isSchemaLoaded(schema)) {
                        loadDataModel(schema);
                    }
                }
            }
        }
        sid = null;
    }

    /**
     * Lazily loads the given data model.
     *
     * @param schema
     * @return DataModel
     * @throws ClientException
     */
    protected final DataModel loadDataModel(String schema)
            throws ClientException {
        if (hasSchema(schema)) { // lazy data model
            if (sid == null) {
                DataModel dataModel = new DataModelImpl(schema); // supports non bound docs
                dataModels.put(schema, dataModel);
                return dataModel;
            }
            CoreSession client = getClient();
            DataModel dataModel = null;
            if (client != null && ref != null) {
                dataModel = client.getDataModel(ref, schema);
                dataModels.put(schema, dataModel);
            }
            return dataModel;
        }
        return null;
    }

    /**
     * Method getDataModel.
     *
     * @param schema String
     * @return DataModel
     * @see DocumentModel#getDataModel(String)
     */
    public DataModel getDataModel(String schema) {
        DataModel dataModel = dataModels.get(schema);
        if (dataModel == null) {
            try {
                dataModel = loadDataModel(schema);
            } catch (ClientException e) {
                // TODO: how to handle exceptions?
                log.error("ERROR getting the data model: " + schema + " for "
                        + ref,e);
            }
        }
        return dataModel;
    }

    /**
     * Method getDataModelsCollection.
     *
     * @see DocumentModel#getDataModelsCollection()
     */
    public Collection<DataModel> getDataModelsCollection() {
        return dataModels.values();
    }

    /**
     * Method addDataModel.
     *
     * @param dataModel DataModel
     */
    public void addDataModel(DataModel dataModel) {
        dataModels.put(dataModel.getSchema(), dataModel);
    }

    /**
     * Method getDeclaredSchemas.
     *
     * @return String[]
     * @see DocumentModel#getDeclaredSchemas()
     */
    public String[] getDeclaredSchemas() {
        return declaredSchemas;
    }

    /**
     * Method getDeclaredFacets.
     *
     * @see org.nuxeo.ecm.core.api.DocumentModel#getDeclaredFacets()
     */
    public Set<String> getDeclaredFacets() {
        return declaredFacets;
    }

    /**
     * Method getId.
     *
     * @return String
     * @see DocumentModel#getId()
     */
    public String getId() {
        return id;
    }

    /**
     * Method getName.
     *
     * @return String
     * @see DocumentModel#getName()
     */
    public String getName() {
        if (path != null) {
            return path.lastSegment();
        }
        return null;
    }

    /**
     * Method getPathAsString.
     *
     * @return String
     * @see DocumentModel#getPathAsString()
     */
    public String getPathAsString() {
        if (path != null) {
            return path.toString();
        }
        return null;
    }

    /**
     * Method getProperties.
     *
     * @param schemaName String
     * @see DocumentModel#getProperties(String)
     */
    public Map<String, Object> getProperties(String schemaName) {
        DataModel dm = getDataModel(schemaName);
        return dm == null ? null : dm.getMap();
    }

    /**
     * Gets property.
     * <p>
     * Get property is also consulting the prefetched properties.
     *
     * @param schemaName String
     * @param name String
     * @return Object
     * @see DocumentModel#getProperty(String, String)
     */
    public Object getProperty(String schemaName, String name) {
        DataModel dm = dataModels.get(schemaName);
        if (dm == null) { // no data model loaded
            // try prefetched props
            if (prefetch != null) {
                Object value = prefetch.get(schemaName + '.' + name);
                if (value != null) {
                    return value == Null.VALUE ? null : value;
                }
            }
            dm = getDataModel(schemaName);
        }
        return dm == null ? null : dm.getData(name);
    }

    /**
     * Method setPathInfo.
     *
     * @param parentPath String
     * @param name String
     * @see DocumentModel#setPathInfo(String, String)
     */
    public void setPathInfo(String parentPath, String name) {
        path = new Path(parentPath + '/' + name);
        ref = new PathRef(parentPath, name);
    }

    /**
     * Method getLock.
     *
     * @return String
     * @see DocumentModel#getLock()
     */
    public String getLock() {
        return lock;
    }

    /**
     * Method isLocked.
     *
     * @return boolean
     * @see DocumentModel#isLocked()
     */
    public boolean isLocked() {
        return lock != null;
    }

    /**
     * Method setLock.
     *
     * @param key String
     * @throws ClientException
     * @see DocumentModel#setLock(String)
     */
    public void setLock(String key) throws ClientException {
        getClient().setLock(ref, key);
        lock = key;
    }

    /**
     * Method unlock.
     *
     * @throws ClientException
     * @see DocumentModel#unlock()
     */
    public void unlock() throws ClientException {
        if (getClient().unlock(ref) != null) {
            lock = null;
        }
    }

    /**
     * Method getACP.
     *
     * @return ACP
     * @see DocumentModel#getACP()
     */
    public ACP getACP() {
        if (!isACPLoaded) { // lazy load
            try {
                acp = getClient().getACP(ref);
                isACPLoaded = true;
            } catch (Exception e) {
                // XXX this exception shouldn't be swallowed!
                log.error("ERROR getting the ACP for " + ref,e);
            }
        }
        return acp;
    }

    /**
     * Method setACP.
     *
     * @param acp ACP
     * @param overwrite boolean
     * @see DocumentModel#setACP(ACP, boolean)
     */
    public void setACP(ACP acp, boolean overwrite) {
        try {
            getClient().setACP(ref, acp, overwrite);
            isACPLoaded = false;
        } catch (Exception e) {
            // XXX this exception shouldn't be swallowed!
            log.error("ERROR setting the ACP for " + ref,e);
        }
    }

    /**
     * Method getType.
     *
     * @return String
     * @see DocumentModel#getType()
     */
    public String getType() {
        // TODO there are some DOcumentModel impl like DocumentMessageImpl which
        // use null types and extend this impl which is wrong - fix this -> type must never be null
        return type != null ? type.getName() : null;
    }

    /**
     * Method setProperties.
     *
     * @param schemaName String
     */
    public void setProperties(String schemaName, Map<String, Object> data) {
        DataModel dm = getDataModel(schemaName);
        if (dm != null) {
            dm.setMap(data);
        }
    }

    /**
     * Method setProperty.
     *
     * @param schemaName String
     * @param name String
     * @param value Object
     * @see DocumentModel#setProperty(String, String, Object)
     */
    public void setProperty(String schemaName, String name, Object value) {
        DataModel dm = getDataModel(schemaName);
        if (dm != null) {
            dm.setData(name, value);
        }
    }

    /**
     * Method hasSchema.
     *
     * @param schema String
     * @return boolean
     * @see DocumentModel#hasSchema(String)
     */
    public boolean hasSchema(String schema) {
        if (type == null) {
            return false;
        }
        DocumentType dt = type.get(); // some tests use dummy types. TODO: fix these tests? (TestDocumentModel)
        return dt == null ? false : dt.hasSchema(schema);
    }

    /**
     * Method hasFacet.
     *
     * @param facet String
     * @return boolean
     * @see DocumentModel#hasFacet(String)
     */
    public boolean hasFacet(String facet) {
        if (declaredFacets != null) {
            return declaredFacets.contains(facet);
        }
        return false;
    }

    /**
     * Method getPath.
     *
     * @return Path
     * @see DocumentModel#getPath()
     */
    public Path getPath() {
        return path;
    }

    /**
     * Method getDataModels.
     *
     * @return DataModelMap
     * @see DocumentModel#getDataModels()
     */
    public DataModelMap getDataModels() {
        return dataModels;
    }

    /**
     * Method copyContentInto.
     *
     * @param other DocumentModelImpl
     */
    public void copyContentInto(DocumentModelImpl other) {
        other.declaredSchemas = declaredSchemas;
        other.declaredFacets = declaredFacets;
        other.dataModels = dataModels;
    }

    /**
     * Method isFolder.
     *
     * @return boolean
     * @see DocumentModel#isFolder()
     */
    public boolean isFolder() {
        return hasFacet("Folderish");
    }

    /**
     * Method isVersionable.
     *
     * @return boolean
     * @see DocumentModel#isVersionable()
     */
    public boolean isVersionable() {
        return hasFacet("Versionable");
    }

    /**
     * Method isDownloadable.
     *
     * @return boolean
     * @see DocumentModel#isDownloadable()
     */
    public boolean isDownloadable() {
        if (hasFacet("Downloadable")) {
            // XXX find a better way to check size that does not depend on the
            // document schema
            Long size = (Long) getProperty("common", "size");
            if (size != null) {
                return size != 0;
            }
        }
        return false;
    }

    /**
     * Method getAdapter.
     *
     * @param itf Class<T>
     * @return T
     * @see DocumentModel#getAdapter(Class<T>)
     */
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
     * Lazy initialization for adapters because they don't survive the
     * serialization.
     */
    @SuppressWarnings("unchecked")
    private ArrayMap<Class, Object> getAdapters() {
        if (adapters == null) {
            adapters = new ArrayMap<Class, Object>();
        }

        return adapters;
    }

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

    /**
     * Method findAdapter.
     *
     * @param itf Class<T>
     * @return T
     */
    @SuppressWarnings("unchecked")
    private <T> T findAdapter(Class<T> itf) {
        DocumentAdapterService svc = (DocumentAdapterService) Framework.getRuntime().getComponent(
                DocumentAdapterService.NAME);
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
                    log.error("Document model cannot be adapted to " + itf
                            + " because it has no facet " + facet);
                }
            }
        } else {
            log.warn("DocumentAdapterService not available. Cannot get document model adaptor for "
                    + itf);
        }
        return null;
    }

    /**
     * Method followTransition.
     *
     * @param transition String
     * @return boolean
     * @throws ClientException
     * @see DocumentModel#followTransition(String)
     */
    public boolean followTransition(String transition) throws ClientException {

        // :FIXME: need to open a new session against the core.

        CoreSession client;

        try {
            client = getClient();
        } catch (UnsupportedOperationException usoe) {
            throw new ClientException(usoe);
        }

        boolean res = false;
        if (client != null) {
            try {
                res = client.followTransition(ref, transition);
            } catch (NullPointerException ne) {
                throw new ClientException(ne.getMessage(), ne);
            }
        } else {
            log.error("Cannot find bound core session....");
        }

        // Invalidate the prefetched value in this case.
        if (res) {
            currentLifeCycleState = null;
        }

        return res;
    }

    /**
     * Method getAllowedStateTransitions.
     *
     * @throws ClientException
     * @see org.nuxeo.ecm.core.api.DocumentModel#getAllowedStateTransitions()
     */
    public Collection<String> getAllowedStateTransitions()
            throws ClientException {

        // :FIXME: need to open a new session against the core.

        Collection<String> allowedStateTransitions = new ArrayList<String>();

        CoreSession client;
        try {
            client = getClient();
        } catch (UnsupportedOperationException usoe) {
            throw new ClientException(usoe);
        }

        if (client != null) {
            try {
                allowedStateTransitions = getClient().getAllowedStateTransitions(
                        ref);
            } catch (NullPointerException ne) {
                throw new ClientException(ne.getMessage(), ne);
            }
        } else {
            log.error("Cannot found bound core session....");
        }

        return allowedStateTransitions;
    }

    /**
     * Method getCurrentLifeCycleState.
     *
     * @return String
     * @throws ClientException
     * @see DocumentModel#getCurrentLifeCycleState()
     */
    public String getCurrentLifeCycleState() throws ClientException {

        if (currentLifeCycleState != null) {
            return currentLifeCycleState;
        }

        // document was just created => not life cycle yet
        if (sid == null) {
            return null;
        }

        // String currentLifeCycleState = null;
        CoreSession client;
        try {
            client = getClient();
        } catch (UnsupportedOperationException usoe) {
            throw new ClientException(usoe);
        }

        if (client != null) {
            try {
                currentLifeCycleState = getClient().getCurrentLifeCycleState(
                        ref);
            } catch (NullPointerException ne) {
                throw new ClientException(ne.getMessage(), ne);
            }
        } else {
            log.error("Cannot found bound core session....");
        }

        return currentLifeCycleState;
    }

    /**
     * Method getLifeCyclePolicy.
     *
     * @return String
     * @throws ClientException
     * @see DocumentModel#getLifeCyclePolicy()
     */
    public String getLifeCyclePolicy() throws ClientException {

        if (lifeCyclePolicy != null) {
            return lifeCyclePolicy;
        }

        // :FIXME: need to open a new session against the core.

        // String lifeCyclePolicy = null;

        CoreSession client;
        try {
            client = getClient();
        } catch (UnsupportedOperationException usoe) {
            throw new ClientException(usoe);
        }

        if (client != null) {
            try {
                lifeCyclePolicy = getClient().getLifeCyclePolicy(ref);
            } catch (NullPointerException ne) {
                throw new ClientException(ne.getMessage(), ne);
            }
        } else {
            log.error("Cannot found bound core session....");
        }

        return lifeCyclePolicy;
    }

    /**
     * Method isVersion.
     *
     * @return boolean
     * @see DocumentModel#isVersion()
     */
    public boolean isVersion() {
        return (flags & F_VERSION) != 0;
    }

    public boolean isProxy() {
        return (flags & F_PROXY) != 0;
    }

    /**
     * Method setIsVersion.
     *
     * @param isVersion boolean
     */
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

    /**
     * Method getContextData.
     *
     * @return ScopedMap
     * @see DocumentModel#getContextData()
     */
    public ScopedMap getContextData() {
        return contextData;
    }

    /**
     * Method getContextData.
     *
     * @param scope ScopeType
     * @param key String
     * @return Serializable
     * @see DocumentModel#getContextData(ScopeType, String)
     */
    public Serializable getContextData(ScopeType scope, String key) {
        return contextData.getScopedValue(scope, key);
    }

    /**
     * Method putContextData.
     *
     * @param scope ScopeType
     * @param key String
     * @param value Serializable
     * @see DocumentModel#putContextData(ScopeType, String, Serializable)
     */
    public void putContextData(ScopeType scope, String key, Serializable value) {
        contextData.putScopedValue(scope, key, value);
    }

    /**
     * Method getContextData.
     *
     * @param key String
     * @return Serializable
     * @see DocumentModel#getContextData(String)
     */
    public Serializable getContextData(String key) {
        return contextData.getScopedValue(key);
    }

    /**
     * Method putContextData.
     *
     * @param key String
     * @param value Serializable
     * @see DocumentModel#putContextData(String, Serializable)
     */
    public void putContextData(String key, Serializable value) {
        contextData.putScopedValue(key, value);
    }

    /**
     * Method copyContextData.
     *
     * @param otherDocument DocumentModel
     * @see DocumentModel#copyContextData(DocumentModel)
     */
    public void copyContextData(DocumentModel otherDocument) {
        ScopedMap otherMap = otherDocument.getContextData();
        if (otherMap != null) {
            contextData.putAll(otherMap);
        }
    }

    /**
     * Method copyContent.
     *
     * @param sourceDoc DocumentModel
     * @see DocumentModel#copyContent(DocumentModel)
     */
    public void copyContent(DocumentModel sourceDoc) {
        String[] schemas = sourceDoc.getDeclaredSchemas();
        declaredSchemas = schemas == null ? null : schemas.clone();
        Set<String> facets = sourceDoc.getDeclaredFacets();
        declaredFacets = facets == null ? null : new HashSet<String>(facets);

        DataModelMap newDataModels = new DataModelMapImpl();
        for (String key : sourceDoc.getDocumentType().getSchemaNames()) {
            DataModel oldDM = sourceDoc.getDataModel(key);
            DataModel newDM = cloneDataModel(oldDM);
            newDataModels.put(key, newDM);
        }
        dataModels = newDataModels;
    }

    public Object cloneField(Field field, String key, Object value) {
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
                List<Object> clonedList = new ArrayList<Object>(list.size());
                for (Object o : list) {
                    clonedList.add(cloneField(lfield, null, o));
                }
                clone = clonedList;
            } else {
                Class<?> klass = JavaTypes.getClass(ftype);
                if (klass.isPrimitive()) {
                    clone = PrimitiveArrays.toPrimitiveArray(list, klass);
                } else {
                    clone = list.toArray((Object[]) Array.newInstance(klass,
                            list.size()));
                }
            }
        } else { // complex type
            ComplexType ctype = (ComplexType) type;
            if (ctype.getName().equals(TypeConstants.CONTENT)) { // if a blob
                Blob blob = (Blob) value; // TODO
                clone = blob;
            } else { // a map
                Map<String, Object> map = (Map<String, Object>) value;
                Map<String, Object> clonedMap = new HashMap<String, Object>();
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

    public DataModel cloneDataModel(Schema schema, DataModel data) {
        DataModel dm = new DataModelImpl(schema.getName());
        for (Field field : schema.getFields()) {
            String key = field.getName().getLocalName();
            Object value = data.getData(key);
            if (value == null) {
                continue;
            }
            Object clone = cloneField(field, key, value);
            dm.setData(key, clone);
        }
        return dm;
    }

    public DataModel cloneDataModel(DataModel data) {
        SchemaManager mgr = TypeService.getSchemaManager();
        return cloneDataModel(mgr.getSchema(data.getSchema()), data);
    }

    /**
     * Method getCacheKey.
     *
     * @return String
     * @see DocumentModel#getCacheKey()
     */
    public String getCacheKey() {
        // UUID - sessionId
        String key = id + '-' + sid;
        // :FIXME: Assume a dublin core schema => enough for us right now.
        Calendar timeStamp = (Calendar) getProperty("dublincore", "modified");

        if (timeStamp != null) {
            key = key + '-' + String.valueOf(timeStamp.getTimeInMillis());
        }
        return key;
    }

    /**
     * Method getRepositoryName.
     *
     * @return String
     * @see DocumentModel#getRepositoryName()
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Method getSourceId.
     *
     * @return String
     * @see DocumentModel#getSourceId()
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Method getVersionLabel.
     *
     * @return String
     * @see DocumentModel#getVersionLabel()
     */
    public String getVersionLabel() {
        return (String) contextData.getScopedValue("version.label");
    }

    /**
     * Method isSchemaLoaded.
     *
     * @param name String
     * @return boolean
     */
    public boolean isSchemaLoaded(String name) {
        return dataModels.containsKey(name);
    }

    /**
     * @param id String
     * @param value Object
     */
    // TODO: id is schema.field and not prefix:field
    public void prefetchProperty(String id, Object value) {
        if (prefetch == null) {
            prefetch = new HashMap<String, Serializable>();
        }
        Serializable sValue = (Serializable) value;
        prefetch.put(id, value == null ? Null.VALUE : sValue);
    }

    public void prefetchCurrentLifecycleState(String lifecycle) {
        currentLifeCycleState = lifecycle;
    }

    public void prefetchLifeCyclePolicy(String lifeCyclePolicy) {
        this.lifeCyclePolicy = lifeCyclePolicy;
    }

    public void setFlags(long flags) {
        this.flags |= flags;
    }

    public void clearFlags(long flags) {
        this.flags &= ~flags;
    }

    public void clearFlags() {
        flags = 0L;
    }

    public long getFlags() {
        return flags;
    }

    public boolean hasFlags(long flags) {
        return (this.flags & flags) == flags;
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
        final StringBuilder buf = new StringBuilder();

        buf.append(DocumentModelImpl.class.getSimpleName());
        buf.append(" {");
        buf.append(" -title: ");
        buf.append(getProperty("dublincore", "title"));
        buf.append(", sessionId: ");
        buf.append(sid);
        buf.append(", doc id: ");
        buf.append(id);
        buf.append(", name: ");
        buf.append(getName());
        buf.append(", path: ");
        buf.append(path);
        buf.append(", ref: ");
        buf.append(ref);
        buf.append(", parent ref: ");
        buf.append(getParentRef());
        buf.append(", data models: ");
        buf.append(dataModels);
        buf.append(", declaredFacets: ");
        buf.append(declaredFacets);
        buf.append(", declaredSchemas: ");
        buf.append(declaredSchemas);
        buf.append('}');

        return buf.toString();
    }

    public Map<String, Serializable> getPrefetch() {
        return prefetch;
    }

    public <T extends Serializable> T getSystemProp(String systemProperty, Class<T> type)
            throws ClientException, DocumentException {
        CoreSession client = getClient();
        return client.getDocumentSystemProp(ref, systemProperty, type);
    }

    public boolean isLifeCycleLoaded() {
        return currentLifeCycleState != null;
    }

    public DocumentPart getPart(String schema) {
        DataModel dm = getDataModel(schema);
        if (dm != null) {
            return ((DataModelImpl)dm).getDocumentPart();
        }
        return null; // TODO thrown an exception?
    }

    public DocumentPart[] getParts() {
        DocumentType type = null;
        try {
            type = Framework.getService(SchemaManager.class).getDocumentType(getType());
        } catch (Exception e) {
            log.error(e);
        }
        Collection<Schema> schemas = type.getSchemas();
        int size = schemas.size();
        DocumentPart[] parts = new DocumentPart[size];
        int i=0;
        for (Schema schema : schemas) {
            DataModel dm = getDataModel(schema.getName());
            parts[i++] = ((DataModelImpl) dm).getDocumentPart();
        }
        return parts;
    }

    public Property getProperty(String xpath) throws PropertyException {
        Path path = new Path(xpath);
        if (path.segmentCount() == 0) {
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String segment =path.segment(0);
        int p = segment.indexOf(':');
        if (p == -1) { // support also other schema paths? like schema.property
            // allow also unprefixed schemas -> make a search for the first matching schema having a property with same name as path segment 0
            DocumentPart[] parts = getParts();
            for (DocumentPart part : parts) {
                if (part.getSchema().hasField(segment)) {
                    return part.resolvePath(path.toString());
                }
            }
            // could not find any matching schema
            throw new PropertyNotFoundException(xpath, "Schema not specified");
        }
        String prefix = segment.substring(0, p);
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        Schema schema = mgr.getSchemaFromPrefix(prefix);
        if (schema == null) {
            schema = mgr.getSchema(prefix);
            if (schema == null) {
                throw new PropertyNotFoundException(xpath, "No such schema: " + prefix);
            }
            // workaround for a schema prefix bug -> XPATH lookups in DocumentPart must use prefixed
            // names for schema with prefixes and non prefixed names for the rest o schemas.
            // Until then we used the name as the prefix but we must remove it since it is not a valid prefix:
            // NXP-1913
            String[] segments = path.segments();
            segments[0] = segments[0].substring(p+1);
            path = Path.createFromSegments(segments);
        }

        DocumentPart part = getPart(schema.getName());
        if (part == null) {
            throw new PropertyNotFoundException(xpath,
                    "Document dont' implement schema: " + prefix);
        }
        return part.resolvePath(path.toString());
    }

    public Serializable getPropertyValue(String path) throws PropertyException {
        return getProperty(path).getValue();
    }

    public void setPropertyValue(String path, Serializable value) throws PropertyException {
        getProperty(path).setValue(value);
    }

    public DocumentModel clone() throws CloneNotSupportedException {
        DocumentModelImpl dm = (DocumentModelImpl)super.clone();
//        dm.id =id;
//        dm.acp = acp;
//        dm.currentLifeCycleState = currentLifeCycleState;
//        dm.lifeCyclePolicy = lifeCyclePolicy;
//        dm.declaredSchemas = declaredSchemas; // schemas are immutable so we don't clone the array
//        dm.flags = flags;
//        dm.repositoryName = repositoryName;
//        dm.ref = ref;
//        dm.parentRef = parentRef;
//        dm.path = path; // path is immutable
//        dm.isACPLoaded = isACPLoaded;
//        dm.prefetch = dm.prefetch; // prefetch can be shared
//        dm.lock = lock;
//        dm.sourceId =sourceId;
//        dm.sid = sid;
//        dm.type = type;
        dm.declaredFacets = new HashSet<String>(declaredFacets); // facets should be clones too - they are not immutable
        // context data is keeping contextual info so it is reseted
        dm.contextData = new ScopedMap();

        // copy parts
        dm.dataModels = new DataModelMapImpl();
        for (Map.Entry<String,DataModel> entry : dataModels.entrySet()) {
            String key = entry.getKey();
            DataModel data = entry.getValue();
            DataModelImpl newData = new DataModelImpl(key, data.getMap());
            dm.dataModels.put(key, newData);
        }
        return dm;
    }

    public void reset() {
        if (dataModels != null) dataModels.clear();
        if (prefetch != null) prefetch.clear();
        isACPLoaded = false;
        acp = null;
        currentLifeCycleState = null;
        lifeCyclePolicy = null;
    }

}
