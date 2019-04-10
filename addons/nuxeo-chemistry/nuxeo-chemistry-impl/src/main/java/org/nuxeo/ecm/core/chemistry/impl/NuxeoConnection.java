/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.chemistry.ACE;
import org.apache.chemistry.ACLPropagation;
import org.apache.chemistry.BaseType;
import org.apache.chemistry.CMISObject;
import org.apache.chemistry.CMISRuntimeException;
import org.apache.chemistry.Connection;
import org.apache.chemistry.ConstraintViolationException;
import org.apache.chemistry.ContentAlreadyExistsException;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.Inclusion;
import org.apache.chemistry.ListPage;
import org.apache.chemistry.NameConstraintViolationException;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.ObjectNotFoundException;
import org.apache.chemistry.Paging;
import org.apache.chemistry.Policy;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.Relationship;
import org.apache.chemistry.Rendition;
import org.apache.chemistry.Repository;
import org.apache.chemistry.SPI;
import org.apache.chemistry.StreamNotSupportedException;
import org.apache.chemistry.Tree;
import org.apache.chemistry.Type;
import org.apache.chemistry.Unfiling;
import org.apache.chemistry.Updatability;
import org.apache.chemistry.VersioningState;
import org.apache.chemistry.impl.base.BaseRepository;
import org.apache.chemistry.impl.simple.SimpleData;
import org.apache.chemistry.impl.simple.SimpleListPage;
import org.apache.chemistry.impl.simple.SimpleObjectEntry;
import org.apache.chemistry.impl.simple.SimpleObjectId;
import org.apache.chemistry.impl.simple.SimpleTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.security.SecurityPolicyService;
import org.nuxeo.runtime.api.Framework;

public class NuxeoConnection implements Connection, SPI {

    private static final Log log = LogFactory.getLog(NuxeoConnection.class);

    protected static final String DC_TITLE = "dc:title";

    protected final NuxeoRepository repository;

    protected final CoreSession session;

    protected final boolean ownSession;

    protected final NuxeoFolder rootFolder;

    /** map of lowercase type name to actual Nuxeo type name */
    protected final Map<String, String> queryTypeNames;

    /** map of Nuxeo and CMIS prop names to NXQL names */
    protected final Map<String, String> queryPropNames;

    /** hide HiddenInNavigation and deleted objects */
    protected final Filter documentFilter;

    /**
     * Creates a Chemistry connection given a Chemistry repository and
     * connection parameters.
     * <p>
     * Usually called directly by {@link NuxeoRepository#getConnection}.
     *
     * @param repository the Chemistry repository
     * @param params the connection parameters
     */
    public NuxeoConnection(NuxeoRepository repository,
            Map<String, Serializable> params) {
        this(repository, createSession(repository.getId(), params), true);
    }

    /**
     * Creates a Chemistry connection given an already open Nuxeo session.
     * <p>
     * The Nuxeo session will <em>not</em> be closed when this connection is
     * closed (the session belongs to the caller).
     *
     * @param repository the Chemistry repository
     * @param session the Nuxeo session
     */
    public NuxeoConnection(NuxeoRepository repository, CoreSession session) {
        this(repository, session, false);
    }

    protected NuxeoConnection(NuxeoRepository repository, CoreSession session,
            boolean ownSession) {
        if (!repository.getId().equals(session.getRepositoryName())) {
            throw new IllegalArgumentException(
                    "Session does not correspond to repository");
        }
        this.repository = repository;
        this.session = session;
        this.ownSession = ownSession;
        try {
            rootFolder = new NuxeoFolder(session.getRootDocument(), this,
                    BaseRepository.ROOT_FOLDER_NAME);
        } catch (ClientException e) {
            throw new CMISRuntimeException("Could not get root document", e);
        }

        // preprocess type names for queries
        queryTypeNames = new HashMap<String, String>();
        queryPropNames = new HashMap<String, String>();
        for (Type type : repository.getTypeDescendants(null)) {
            String tname = type.getQueryName();
            String nxname;
            if (tname.equals(BaseType.FOLDER.getId().toLowerCase())) {
                nxname = "Folder";
            } else if (tname.equals(BaseType.DOCUMENT.getId().toLowerCase())) {
                nxname = "Document";
            } else {
                nxname = tname;
            }
            queryTypeNames.put(tname.toLowerCase(), nxname);
            for (PropertyDefinition pd : type.getPropertyDefinitions()) {
                String id = pd.getId();
                queryPropNames.put(id, id);
            }
        }
        // actual NXQL mapping for some CMIS system props
        queryPropNames.putAll(NuxeoProperty.propertyNameToNXQL);

        Filter facetFilter = new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION,
                false);
        Filter lcFilter = new LifeCycleFilter(LifeCycleConstants.DELETED_STATE,
                false);
        documentFilter = new CompoundFilter(facetFilter, lcFilter);
    }

    protected static CoreSession createSession(String repositoryId,
            Map<String, Serializable> params) {
        // TODO authentication
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        if (params != null) {
            context.putAll(params);
            context.put("username", params.get(Repository.PARAM_USERNAME));
        }
        if (context.get("username") == null) {
            context.put("username", "Administrator");
        }
        try {
            return CoreInstance.getInstance().open(repositoryId, context);
        } catch (ClientException e) {
            throw new CMISRuntimeException("Could not connect", e);
        }
    }

    public SPI getSPI() {
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If this connection was created from a Nuxeo session, then this method
     * does nothing.
     */
    public void close() {
        if (ownSession) {
            CoreInstance.getInstance().close(session);
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    /*
     * ----- Factories -----
     */

    public ObjectId newObjectId(String id) {
        return new SimpleObjectId(id);
    }

    public ObjectEntry newObjectEntry(String typeId) {
        DocumentModel doc = new DocumentModelImpl(typeId);
        // doc = session.createDocumentModel(typeId);
        // doc.setPathInfo(((NuxeoObjectEntry) folder).doc.getPathAsString(),
        // "");
        return new NuxeoObjectEntry(doc, this);
    }

    private DocumentModel createDoc(String typeId, ObjectId folder,
            BaseType baseType) {
        if (typeId == null) {
            throw new IllegalArgumentException("Missing object type id");
        }
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != baseType) {
            throw new IllegalArgumentException(typeId);
        }
        DocumentModel doc;
        try {
            String typeName = ((NuxeoType) type).getNuxeoTypeName();
            doc = session.createDocumentModel(typeName);
        } catch (ClientException e) {
            throw new IllegalArgumentException(type.getId());
        }
        if (folder != null) {
            DocumentModel parentDoc;
            if (folder instanceof NuxeoFolder) {
                parentDoc = ((NuxeoFolder) folder).doc;
            } else {
                try {
                    parentDoc = session.getDocument(new IdRef(folder.getId()));
                } catch (ClientException e) {
                    throw new CMISRuntimeException("Cannot create object", e);
                }
            }
            doc.setPathInfo(parentDoc.getPathAsString(), typeId);
        }
        return doc;
    }

    public Document newDocument(String typeId, Folder folder) {
        return new NuxeoDocument(createDoc(typeId, folder, BaseType.DOCUMENT),
                this);
    }

    public Folder newFolder(String typeId, Folder folder) {
        return new NuxeoFolder(createDoc(typeId, folder, BaseType.FOLDER), this);
    }

    public Relationship newRelationship(String typeId) {
        return new NuxeoRelationship(createDoc(typeId, null,
                BaseType.RELATIONSHIP), this);
    }

    public Policy newPolicy(String typeId, Folder folder) {
        return new NuxeoPolicy(createDoc(typeId, folder, BaseType.POLICY), this);
    }

    /*
     * ----- Navigation Services -----
     */

    protected List<Tree<ObjectEntry>> getTreeChildren(ObjectId entry,
            int depth, Inclusion inclusion, String orderBy, BaseType baseType) {
        List<Tree<ObjectEntry>> children = new ArrayList<Tree<ObjectEntry>>();
        for (ObjectEntry child : getChildren(entry, inclusion, orderBy, null)) {
            BaseType childBaseType = child.getBaseType();
            if (baseType != null && baseType != childBaseType) {
                continue;
            }
            List<Tree<ObjectEntry>> c;
            if (childBaseType != BaseType.FOLDER || depth == 1) {
                c = null;
            } else {
                c = getTreeChildren(child, depth - 1, inclusion, orderBy,
                        baseType);
            }
            children.add(new SimpleTree<ObjectEntry>(child, c));
        }
        return children;
    }

    public Tree<ObjectEntry> getFolderTree(ObjectId folder, int depth,
            Inclusion inclusion) {
        return new SimpleTree<ObjectEntry>(null, getTreeChildren(folder, depth,
                inclusion, null, BaseType.FOLDER));
    }

    public Tree<ObjectEntry> getDescendants(ObjectId folder, int depth,
            String orderBy, Inclusion inclusion) {
        return new SimpleTree<ObjectEntry>(null, getTreeChildren(folder, depth,
                inclusion, orderBy, null));
    }

    public ListPage<ObjectEntry> getChildren(ObjectId folder,
            Inclusion inclusion, String orderBy, Paging paging) {
        // TODO orderBy
        DocumentModelList docs;
        try {
            String id = folder.getId();
            DocumentRef docRef = new IdRef(id);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(id);
            }
            DocumentModel doc = session.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new ObjectNotFoundException(id);
            }
            if (!doc.isFolder()) {
                throw new IllegalArgumentException("Not a folder: " + id);
            }
            docs = session.getChildren(docRef, null, getDocumentFilter(), null);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
        List<ObjectEntry> all = new ArrayList<ObjectEntry>(docs.size());
        for (DocumentModel child : docs) {
            all.add(new NuxeoObjectEntry(child, this));
        }
        return SimpleListPage.fromPaging(all, paging);
    }

    protected Filter getDocumentFilter() {
        return documentFilter;
    }

    protected boolean isFilteredOut(DocumentModel doc) throws ClientException {
        return !documentFilter.accept(doc);
    }

    public ObjectEntry getFolderParent(ObjectId folder, String filter) {
        return getParent(folder, filter);
    }

    public Collection<ObjectEntry> getObjectParents(ObjectId object,
            String filter) {
        return Collections.singleton(getParent(object, filter));
    }

    protected ObjectEntry getParent(ObjectId object, String filter) {
        // TODO filter
        String objectId = object.getId();
        if (repository.getInfo().getRootFolderId().getId().equals(objectId)) {
            return null;
        }
        try {
            DocumentRef docRef = new IdRef(objectId);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(objectId);
            }
            DocumentModel parent = session.getParentDocument(docRef);
            return new NuxeoObjectEntry(parent, this);
        } catch (DocumentSecurityException e) {
            // cannot access parent TODO is this really the best we can do?
            return null;
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public ListPage<ObjectEntry> getCheckedOutDocuments(ObjectId folder,
            Inclusion inclusion, Paging paging) {
        // TODO Auto-generated method stub
        return SimpleListPage.emptyList();
    }

    /*
     * ----- Object Services -----
     */

    // returns null if not found
    protected NuxeoObjectEntry getObjectEntry(ObjectId object) {
        if (object instanceof NuxeoObjectEntry) {
            return (NuxeoObjectEntry) object;
        }
        try {
            DocumentRef docRef = new IdRef(object.getId());
            if (!session.exists(docRef)) {
                return null;
            }
            DocumentModel doc = session.getDocument(docRef);
            if (isFilteredOut(doc)) {
                return null;
            }
            return new NuxeoObjectEntry(doc, this);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e);
        }
    }

    public ObjectId createDocumentFromSource(ObjectId source, ObjectId folder,
            Map<String, Serializable> properties,
            VersioningState versioningState)
            throws NameConstraintViolationException {
        // TODO versioningState
        if (folder == null) {
            throw new ConstraintViolationException("Unfiling not supported");
        }
        try {
            String id = source.getId();
            DocumentRef docRef = new IdRef(id);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(id);
            }
            String folderId = folder.getId();
            IdRef folderRef = new IdRef(folderId);
            if (!session.exists(folderRef)) {
                throw new ObjectNotFoundException(folderId);
            }
            DocumentModel doc = session.copy(docRef, folderRef, null);
            NuxeoObjectEntry entry = new NuxeoObjectEntry(doc, this);
            if (properties != null) {
                entry = updateProperties(entry, null, properties, true);
                entry.save();
            }
            session.save();
            return entry;
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public ObjectId createDocument(Map<String, Serializable> properties,
            ObjectId folder, ContentStream contentStream,
            VersioningState versioningState) {
        // TODO versioningState
        if (folder == null) {
            throw new IllegalArgumentException("Missing folder");
        }
        // if a cmis:document is requested, create a File instead
        if (BaseType.DOCUMENT.getId().equals(properties.get(Property.TYPE_ID))) {
            properties = new HashMap<String, Serializable>(properties);
            properties.put(Property.TYPE_ID, "File");
        }
        return createObject(properties, folder, BaseType.DOCUMENT,
                contentStream);
    }

    public ObjectId createFolder(Map<String, Serializable> properties,
            ObjectId folder) {
        if (folder == null) {
            throw new IllegalArgumentException("Missing folder");
        }
        return createObject(properties, folder, BaseType.FOLDER, null);
    }

    public ObjectId createRelationship(Map<String, Serializable> properties) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createPolicy(Map<String, Serializable> properties,
            ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    // create and save session
    protected ObjectId createObject(Map<String, Serializable> properties,
            ObjectId folder, BaseType baseType, ContentStream contentStream) {
        String typeId = (String) properties.get(Property.TYPE_ID);
        DocumentModel doc = createDoc(typeId, folder, baseType);
        ObjectEntry entry = new NuxeoObjectEntry(doc, this, true);
        updateProperties(entry, null, properties, true);
        try {
            if (contentStream != null) {
                try {
                    NuxeoProperty.setContentStream(doc, contentStream, true);
                } catch (ContentAlreadyExistsException e) {
                    // cannot happen, overwrite = true
                }
            }
            doc = session.createDocument(doc);
            session.save();
        } catch (ClientException e) {
            throw new CMISRuntimeException("Cannot create", e);
        } catch (IOException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
        return NuxeoObject.construct(doc, this);
    }

    public Set<QName> getAllowableActions(ObjectId object) {
        ObjectEntry entry = getObjectEntry(object);
        if (entry == null) {
            throw new ObjectNotFoundException(object.getId());
        }
        return entry.getAllowableActions();
    }

    public ObjectEntry getObject(ObjectId objectId, Inclusion inclusion) {
        return getProperties(objectId, inclusion);
    }

    public ObjectEntry getProperties(ObjectId object, Inclusion inclusion) {
        // TODO filter, includeAllowableActions, includeRelationships
        return getObjectEntry(object);
    }

    public ObjectEntry getObjectByPath(String path, Inclusion inclusion) {
        // TODO Inclusion
        try {
            DocumentRef docRef = new PathRef(path);
            if (!session.exists(docRef)) {
                return null;
            }
            DocumentModel doc = session.getDocument(docRef);
            if (isFilteredOut(doc)) {
                return null;
            }
            return new NuxeoObjectEntry(doc, this);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public Folder getFolder(String path) {
        try {
            DocumentRef docRef = new PathRef(path);
            if (!session.exists(docRef)) {
                return null;
            }
            DocumentModel doc = session.getDocument(docRef);
            if (isFilteredOut(doc)) {
                return null;
            }
            return new NuxeoFolder(doc, this);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public CMISObject getObject(ObjectId object) {
        DocumentModel doc;
        try {
            DocumentRef docRef = new IdRef(object.getId());
            if (!session.exists(docRef)) {
                return null;
            }
            doc = session.getDocument(docRef);
            if (isFilteredOut(doc)) {
                return null;
            }
        } catch (ClientException e) {
            throw new RuntimeException(e); // TODO
        }
        switch (repository.getType(NuxeoType.mappedId(doc.getType())).getBaseType()) {
        case DOCUMENT:
            return new NuxeoDocument(doc, this);
        case FOLDER:
            return new NuxeoFolder(doc, this);
        case RELATIONSHIP:
            return new NuxeoRelationship(doc, this);
        case POLICY:
            return new NuxeoPolicy(doc, this);
        default:
            throw new AssertionError();
        }
    }

    public ListPage<Rendition> getRenditions(ObjectId object,
            Inclusion inclusion, Paging paging) {
        return SimpleListPage.emptyList();
    }

    public boolean hasContentStream(ObjectId document) {
        try {
            return getContentStream(document, null) != null;
        } catch (StreamNotSupportedException e) {
            return false;
        } catch (IOException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public ContentStream getContentStream(ObjectId object,
            String contentStreamId) throws IOException {
        // TODO contentStreamId
        DocumentModel doc;
        try {
            String objectId = object.getId();
            DocumentRef docRef = new IdRef(objectId);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(objectId);
            }
            doc = session.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new ObjectNotFoundException(objectId);
            }
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
        return NuxeoProperty.getContentStream(doc);
    }

    public ObjectId setContentStream(ObjectId document,
            ContentStream contentStream, boolean overwrite) throws IOException,
            ContentAlreadyExistsException {
        DocumentModel doc;
        try {
            String documentId = document.getId();
            DocumentRef docRef = new IdRef(documentId);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(documentId);
            }
            doc = session.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new ObjectNotFoundException(documentId);
            }
            NuxeoProperty.setContentStream(doc, contentStream, overwrite);
            session.saveDocument(doc);
            session.save();
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
        return document;
    }

    public ObjectId deleteContentStream(ObjectId document) {
        try {
            return setContentStream(document, null, true);
        } catch (ContentAlreadyExistsException e) {
            // cannot happen, overwrite = true;
            return null;
        } catch (IOException e) {
            // cannot happen, contentStream = null;
            return null;
        }
    }

    public ObjectId updateProperties(ObjectId object, String changeToken,
            Map<String, Serializable> properties) {
        NuxeoObjectEntry entry = updateProperties(object, changeToken,
                properties, false);
        try {
            entry.save();
            session.save();
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
        return entry;
    }

    protected NuxeoObjectEntry updateProperties(ObjectId object,
            String changeToken, Map<String, Serializable> properties,
            boolean creation) {
        // TODO changeToken
        NuxeoObjectEntry entry = getObjectEntry(object);
        if (entry == null) {
            throw new ObjectNotFoundException(object.getId());
        }
        for (Entry<String, Serializable> en : properties.entrySet()) {
            String key = en.getKey();
            Type type = getRepository().getType(entry.getTypeId());
            PropertyDefinition prop = type.getPropertyDefinition(key);
            if (prop == null) {
                log.error("Unknown property, ignored: " + key);
                continue;
            }
            Updatability updatability = prop.getUpdatability();
            if (updatability == Updatability.READ_ONLY
                    || (updatability == Updatability.ON_CREATE && !creation)) {
                // log.error("Read-only property, ignored: " + key);
                continue;
            }
            if (Property.TYPE_ID.equals(key)
                    || Property.LAST_MODIFICATION_DATE.equals(key)) {
                continue;
            }
            entry.setValue(key, en.getValue());
        }
        // set dc:title from name if missing
        try {
            if (entry.getValue(DC_TITLE) == null) {
                entry.setValue(DC_TITLE, entry.getValue(Property.NAME));
            }
        } catch (IllegalArgumentException e) {
            // ignore, no dc:title
        }
        return entry;
    }

    public ObjectId moveObject(ObjectId object, ObjectId targetFolder,
            ObjectId sourceFolder) {
        String id = object.getId();
        if (repository.getInfo().getRootFolderId().getId().equals(id)) {
            throw new ConstraintViolationException("Cannot move root");
        }
        try {
            DocumentRef docRef = new IdRef(id);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(id);
            }
            DocumentModel parent = session.getParentDocument(docRef);
            String sourceFolderId;
            if (sourceFolder == null) {
                sourceFolderId = parent.getId();
            } else {
                // check it's there
                sourceFolderId = sourceFolder.getId();
                if (!parent.getId().equals(sourceFolderId)) {
                    throw new ConstraintViolationException("Object " + id
                            + " is not filed in " + sourceFolderId);
                }
            }
            DocumentModel doc = session.move(docRef, new IdRef(
                    targetFolder.getId()), null);
            session.save();
            // update entry if possible, otherwise return just id
            if (object instanceof NuxeoObjectEntry) {
                NuxeoObjectEntry noe = (NuxeoObjectEntry) object;
                noe.setDocumentModel(doc);
                return noe;
            } else {
                return newObjectId(doc.getId());
            }
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public void deleteObject(ObjectId object, boolean allVersions) {
        String objectId = object.getId();
        if (repository.getInfo().getRootFolderId().getId().equals(objectId)) {
            throw new IllegalArgumentException("Cannot delete root");
        }
        try {
            DocumentRef docRef = new IdRef(objectId);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(objectId);
            }
            NuxeoObjectEntry entry = getObjectEntry(object);
            if (entry == null) {
                throw new ObjectNotFoundException(objectId);
            }
            if (entry.getBaseType() == BaseType.FOLDER) {
                // check that there are no children left
                DocumentModelList docs = session.getChildren(docRef, null,
                        getDocumentFilter(), null);
                if (docs.size() > 0) {
                    throw new ConstraintViolationException(
                            "Cannot delete non-empty folder: " + objectId);
                }
            }
            session.removeDocument(docRef);
            session.save();
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public Collection<ObjectId> deleteTree(ObjectId folder, Unfiling unfiling,
            boolean continueOnFailure) {
        if (unfiling == Unfiling.UNFILE) {
            throw new ConstraintViolationException("Unfiling not supported");
        }
        String folderId = folder.getId();
        if (repository.getInfo().getRootFolderId().getId().equals(folderId)) {
            throw new IllegalArgumentException("Cannot delete root");
        }
        try {
            DocumentRef docRef = new IdRef(folderId);
            if (!session.exists(docRef)) {
                throw new ObjectNotFoundException(folderId);
            }
            NuxeoObjectEntry entry = getObjectEntry(folder);
            if (entry == null) {
                throw new ObjectNotFoundException(folderId);
            }
            if (entry.getBaseType() != BaseType.FOLDER) {
                throw new IllegalArgumentException("Not a folder: " + folderId);
            }
            session.removeDocument(docRef);
            session.save();
            return Collections.emptyList();
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    public void addObjectToFolder(ObjectId object, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removeObjectFromFolder(ObjectId object, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Discovery Services -----
     */

    public ListPage<ObjectEntry> query(String statement,
            boolean searchAllVersions, Inclusion inclusion, Paging paging) {

        IterableQueryResult iterable;
        try {
            iterable = session.queryAndFetch(statement, CMISQLQueryMaker.TYPE,
                    this);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }

        iterable = getPolicyFilteredMaps(iterable);

        if (paging != null && paging.skipCount > 0) {
            iterable.skipTo(paging.skipCount);
        }
        Iterator<Map<String, Serializable>> it = iterable.iterator();

        int maxItems = paging == null || paging.maxItems == 0 ? -1
                : paging.maxItems;
        SimpleListPage<ObjectEntry> page = new SimpleListPage<ObjectEntry>();
        while (it.hasNext()) {
            Map<String, Serializable> next = it.next();
            SimpleData data = new SimpleData(null, null);
            String typeId = null;
            for (Entry<String, Serializable> entry : next.entrySet()) {
                String key = entry.getKey();
                Serializable value = entry.getValue();
                if (key.endsWith(Property.TYPE_ID)) {
                    // mapper doesn't postprocess the cmis:objectTypeId
                    // do type replacement
                    value = NuxeoType.mappedId((String) value);
                    if (typeId == null) {
                        typeId = (String) value;
                    }
                }
                if (value != null) {
                    data.put(key, value);
                }
            }
            // synthesize cmis:baseTypeId
            if (typeId != null) {
                Type type = repository.getType(typeId);
                if (type != null) {
                    data.put(Property.BASE_TYPE_ID, type.getBaseType().getId());
                }
            }
            page.add(new SimpleObjectEntry(data, this));
            if (--maxItems == 0) {
                break;
            }
        }
        page.setHasMoreItems(it.hasNext());
        page.setNumItems((int) iterable.size());
        iterable.close();
        return page;
    }

    public Collection<CMISObject> query(String statement,
            boolean searchAllVersions) {
        IterableQueryResult iterable;
        try {
            iterable = session.queryAndFetch(statement, CMISQLQueryMaker.TYPE,
                    this, Boolean.TRUE); // add system cols
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }

        List<String> ids = getPolicyFilteredIds(iterable);
        List<CMISObject> results = new ArrayList<CMISObject>(ids.size());
        for (String id : ids) {
            DocumentModel doc;
            try {
                doc = session.getDocument(new IdRef(id));
                // already filtered out in query
            } catch (ClientException e) {
                log.error("Cannot fetch document: " + id, e);
                continue;
            }
            results.add(NuxeoObject.construct(doc, this));
        }
        iterable.close();
        return results;
    }

    /**
     * Restricts through policies and returns a list of ids.
     */
    protected List<String> getPolicyFilteredIds(IterableQueryResult iterable) {
        SecurityPolicyService service;
        try {
            service = Framework.getService(SecurityPolicyService.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
        // restrict through policies
        String permission = SecurityConstants.BROWSE;
        boolean filterPolicies = service.arePoliciesRestrictingPermission(permission);
        List<String> ids = new ArrayList<String>();
        row: //
        for (Map<String, Serializable> map : iterable) {
            boolean gotId = false;
            for (Entry<String, Serializable> entry : map.entrySet()) {
                String key = entry.getKey(); // was canonicalized
                if (key.equals(Property.ID) || key.endsWith('.' + Property.ID)) {
                    // check security on all objects
                    // TODO don't check relations
                    String id = (String) entry.getValue();
                    if (filterPolicies) {
                        boolean ok;
                        try {
                            ok = session.hasPermission(new IdRef(id),
                                    permission);
                        } catch (ClientException e) {
                            log.error("Cannot fetch document: " + id, e);
                            ok = false;
                        }
                        if (!ok) {
                            // skip document for which policy restricts access
                            continue row;
                        }
                    }
                    if (!gotId) {
                        ids.add(id);
                        gotId = true;
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Restricts through policies and returns an iterable of maps.
     */
    protected IterableQueryResult getPolicyFilteredMaps(
            IterableQueryResult iterable) {
        SecurityPolicyService service;
        try {
            service = Framework.getService(SecurityPolicyService.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
        String permission = SecurityConstants.BROWSE;
        boolean filterPolicies = service.arePoliciesRestrictingPermission(permission);
        if (!filterPolicies) {
            return iterable;
        }
        List<Map<String, Serializable>> list = new ArrayList<Map<String, Serializable>>();
        row: //
        for (Map<String, Serializable> map : iterable) {
            for (Entry<String, Serializable> entry : map.entrySet()) {
                String key = entry.getKey(); // was canonicalized
                if (key.equals(Property.ID) || key.endsWith('.' + Property.ID)) {
                    // check security on all objects
                    // TODO don't check relations
                    String id = (String) entry.getValue();
                    boolean ok;
                    try {
                        ok = session.hasPermission(new IdRef(id), permission);
                    } catch (ClientException e) {
                        log.error("Cannot fetch document: " + id, e);
                        ok = false;
                    }
                    if (!ok) {
                        // skip document for which policy restricts access
                        continue row;
                    }
                }
            }
            list.add(map);
        }
        // we're replacing the iterable, close the old one
        iterable.close();
        return new ListQueryResult(list);
    }

    /**
     * IterableQueryResult backed by a simple list.
     */
    public static class ListQueryResult implements IterableQueryResult,
            Iterator<Map<String, Serializable>> {

        public final List<Map<String, Serializable>> list;

        public int pos;

        public ListQueryResult(List<Map<String, Serializable>> list) {
            this.list = list;
            pos = 0;
        }

        public Iterator<Map<String, Serializable>> iterator() {
            return this;
        }

        public long pos() {
            return pos;
        }

        public long size() {
            return list.size();
        }

        public void skipTo(long pos) {
            if (pos < 0 || pos > list.size()) {
                throw new NoSuchElementException();
            }
            this.pos = (int) pos;
        }

        public void close() {
        }

        public boolean hasNext() {
            return pos < list.size();
        }

        public Map<String, Serializable> next() {
            try {
                return list.get((int) pos++);
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public ListPage<ObjectEntry> getChangeLog(String changeLogToken,
            boolean includeProperties, Paging paging,
            String[] lastChangeLogToken) {
        lastChangeLogToken[0] = null;
        return SimpleListPage.emptyList();
    }

    /*
     * ----- Versioning Services -----
     */

    public ObjectId checkOut(ObjectId document, boolean[] contentCopied) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void cancelCheckOut(ObjectId document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId checkIn(ObjectId document,
            Map<String, Serializable> properties, ContentStream contentStream,
            boolean major, String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Map<String, Serializable> getPropertiesOfLatestVersion(
            String versionSeriesId, boolean major, String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAllVersions(String versionSeriesId,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Relationship Services -----
     */

    public ListPage<ObjectEntry> getRelationships(ObjectId object,
            String typeId, boolean includeSubRelationshipTypes,
            Inclusion inclusion, Paging paging) {
        // TODO Auto-generated method stub
        return SimpleListPage.emptyList();
    }

    /*
     * ----- Policy Services -----
     */

    public void applyPolicy(ObjectId object, ObjectId policy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(ObjectId object, ObjectId policy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAppliedPolicies(ObjectId policy,
            String filter) {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    /*
     * ----- ACL Services -----
     */

    public List<ACE> getACL(ObjectId object, boolean onlyBasicPermissions,
            boolean[] exact) {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    public List<ACE> applyACL(ObjectId object, List<ACE> addACEs,
            List<ACE> removeACEs, ACLPropagation propagation, boolean[] exact,
            String[] changeToken) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
