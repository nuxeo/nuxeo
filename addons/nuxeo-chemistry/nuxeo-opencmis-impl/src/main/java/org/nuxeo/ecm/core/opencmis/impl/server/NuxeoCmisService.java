/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.Converter;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FailedToDeleteDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoFolder;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * Nuxeo implementation of the CMIS Services, on top of a {@link CoreSession}.
 */
public class NuxeoCmisService extends AbstractCmisService {

    protected NuxeoRepository repository; // TODO one per repositoryId

    private CoreSession coreSession;

    /** hide HiddenInNavigation and deleted objects */
    protected final Filter documentFilter;

    protected CallContext context;

    // constructed by binding
    public NuxeoCmisService(CoreSession coreSession, NuxeoRepository repository) {
        this.repository = repository;
        this.coreSession = coreSession;

        Filter facetFilter = new FacetFilter(FacetNames.HIDDEN_IN_NAVIGATION,
                false);
        Filter lcFilter = new LifeCycleFilter(LifeCycleConstants.DELETED_STATE,
                false);
        documentFilter = new CompoundFilter(facetFilter, lcFilter);
    }

    public NuxeoRepository getNuxeoRepository() {
        return repository;
    }

    public CoreSession getCoreSession() {
        return coreSession;
    }

    /**
     * Sets the call context.
     */
    public void setCallContext(CallContext context) {
        this.context = context;
    }

    @Override
    public void close() {
    }

    // called when servlet context is destroyed
    public void destroy() {
        close();
    }

    protected void checkRepositoryId(String repositoryId) {
        if (!repository.repositoryId.equals(repositoryId)) {
            throw new RuntimeException("Repository ID mismatch");
        }
    }

    @Override
    public RepositoryInfo getRepositoryInfo(String repositoryId,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO link LatestChangeLogToken to session state
        return repository.getRepositoryInfo(coreSession);
    }

    @Override
    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        TypeDefinition type = repository.getTypeDefinition(typeId);
        // clone
        return Converter.convert(Converter.convert(type));

    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId,
            String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        TypeDefinitionList children = repository.getTypeChildren(typeId,
                includePropertyDefinitions, maxItems, skipCount);
        // clone
        return Converter.convert(Converter.convert(children));
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(
            String repositoryId, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    protected DocumentModel getDocumentModel(DocumentRef docRef) {
        try {
            if (!coreSession.exists(docRef)) {
                throw new CmisObjectNotFoundException(docRef.toString());
            }
            DocumentModel doc = coreSession.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new CmisObjectNotFoundException(docRef.toString());
            }
            return doc;
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        DocumentModel doc = getDocumentModel(new IdRef(objectId));
        ObjectData data = new NuxeoObjectData(repository, doc, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension);
        if (context.isObjectInfoRequired()) {
            addObjectInfo(getObjectInfo(repositoryId, data));
        }
        return data;
    }

    protected Filter getDocumentFilter() {
        return documentFilter;
    }

    /**
     * Checks if the doc should be ignored because it is "invisible" (deleted,
     * hidden in navigation).
     */
    public boolean isFilteredOut(DocumentModel doc) {
        return !documentFilter.accept(doc);
    }

    protected DocumentModel createDocumentModel(ObjectId folder,
            TypeDefinition type) {
        DocumentModel doc;
        try {
            doc = coreSession.createDocumentModel(type.getLocalName());
        } catch (ClientException e) {
            throw new IllegalArgumentException(type.getId());
        }
        if (folder != null) {
            DocumentModel parentDoc;
            if (folder instanceof NuxeoFolder) {
                parentDoc = ((NuxeoFolder) folder).data.doc;
            } else {
                try {
                    parentDoc = coreSession.getDocument(new IdRef(
                            folder.getId()));
                } catch (ClientException e) {
                    throw new CmisRuntimeException("Cannot create object", e);
                }
            }
            String name = type.getId(); // default name based on id
            doc.setPathInfo(parentDoc.getPathAsString(), name);
        }
        return doc;
    }

    // create and save session
    protected NuxeoObjectData createObject(Properties properties,
            ObjectId folder, BaseTypeId baseType, ContentStream contentStream) {
        String typeId;
        Map<String, PropertyData<?>> p;
        PropertyData<?> d;
        TypeDefinition type = null;
        if (properties != null //
                && (p = properties.getProperties()) != null //
                && (d = p.get(PropertyIds.OBJECT_TYPE_ID)) != null) {
            typeId = (String) d.getFirstValue();
            if (baseType == null) {
                type = repository.getTypeDefinition(typeId);
                if (type == null) {
                    throw new IllegalArgumentException(typeId);
                }
                baseType = type.getBaseTypeId();
            }
        } else {
            typeId = null;
        }
        if (typeId == null) {
            switch (baseType) {
            case CMIS_DOCUMENT:
                typeId = "File"; // TODO constant
                break;
            case CMIS_FOLDER:
                typeId = BaseTypeId.CMIS_FOLDER.value();
                break;
            case CMIS_POLICY:
                throw new CmisRuntimeException("Cannot create policy");
            case CMIS_RELATIONSHIP:
                throw new CmisRuntimeException("Cannot create relationship");
            default:
                throw new CmisRuntimeException("No base type");
            }
        }
        if (type == null) {
            type = repository.getTypeDefinition(typeId);
        }
        if (type == null || type.getBaseTypeId() != baseType) {
            throw new IllegalArgumentException(typeId);
        }
        DocumentModel doc = createDocumentModel(folder, type);
        NuxeoObjectData object = new NuxeoObjectData(repository, doc, null,
                null, null, null, null, null, null);
        updateProperties(object, null, properties, true);
        try {
            if (contentStream != null) {
                try {
                    NuxeoPropertyData.setContentStream(doc, contentStream, true);
                } catch (CmisContentAlreadyExistsException e) {
                    // cannot happen, overwrite = true
                }
            }
            object.doc = coreSession.createDocument(doc);
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException("Cannot create", e);
        } catch (IOException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
        return object;
    }

    protected void updateProperties(NuxeoObjectData object, String changeToken,
            Properties properties, boolean creation) {
        // TODO changeToken
        Map<String, PropertyData<?>> p;
        if (properties != null && (p = properties.getProperties()) != null) {
            for (Entry<String, PropertyData<?>> en : p.entrySet()) {
                setObjectProperty(object, en.getKey(), en.getValue(),
                        object.getTypeDefinition(), creation);
            }
        }
    }

    protected <T> void setObjectProperty(NuxeoObjectData object, String key,
            PropertyData<T> d, TypeDefinition type, boolean creation) {
        @SuppressWarnings("unchecked")
        PropertyDefinition<T> pd = (PropertyDefinition<T>) type.getPropertyDefinitions().get(
                key);
        if (pd == null) {
            throw new CmisRuntimeException("Unknown property: " + key);
            // log.error("Unknown property, ignored: " + key);
            // continue;
        }
        Updatability updatability = pd.getUpdatability();
        if (updatability == Updatability.READONLY
                || (updatability == Updatability.ONCREATE && !creation)) {
            // log.error("Read-only property, ignored: " + key);
            return;
        }
        if (PropertyIds.OBJECT_TYPE_ID.equals(key)
                || PropertyIds.LAST_MODIFICATION_DATE.equals(key)) {
            return;
        }
        // TODO avoid constructing property object just to set value
        NuxeoPropertyDataBase<T> np = (NuxeoPropertyDataBase<T>) NuxeoPropertyData.construct(
                object, pd);
        if (d == null) {
            np.setValue(null);
        } else if (pd.getCardinality() == Cardinality.SINGLE) {
            np.setValue(d.getFirstValue());
        } else {
            np.setValue(d.getValues());
        }
    }

    @Override
    public String create(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        NuxeoObjectData object = createObject(properties, new ObjectIdImpl(
                folderId), null, contentStream);
        return object.getId();
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        // TODO versioningState, policies, addAces, removeAces
        checkRepositoryId(repositoryId);
        NuxeoObjectData object = createObject(properties, new ObjectIdImpl(
                folderId), BaseTypeId.CMIS_DOCUMENT, contentStream);
        return object.getId();
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        // TODO policies, addAces, removeAces
        checkRepositoryId(repositoryId);
        NuxeoObjectData object = createObject(properties, new ObjectIdImpl(
                folderId), BaseTypeId.CMIS_FOLDER, null);
        return object.getId();
    }

    @Override
    public String createPolicy(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String createRelationship(String repositoryId,
            Properties properties, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String createDocumentFromSource(String repositoryId,
            String sourceId, Properties properties, String folderId,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        if (folderId == null) {
            // no unfileable objects for now
            throw new CmisInvalidArgumentException("Invalid null folder ID");
        }
        DocumentModel doc = getDocumentModel(new IdRef(sourceId));
        DocumentModel folder = getDocumentModel(new IdRef(folderId));
        try {
            DocumentModel copyDoc = coreSession.copy(doc.getRef(),
                    folder.getRef(), null);
            NuxeoObjectData copy = new NuxeoObjectData(repository, copyDoc,
                    null, null, null, null, null, null, null);
            if (properties != null && properties.getPropertyList() != null
                    && !properties.getPropertyList().isEmpty()) {
                updateProperties(copy, null, properties, false);
                copy.doc = coreSession.saveDocument(copyDoc);
            }
            coreSession.save();
            return copy.getId();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void deleteContentStream(String repositoryId,
            Holder<String> objectIdHolder, Holder<String> changeTokenHolder,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        setContentStream(repositoryId, objectIdHolder, Boolean.TRUE,
                changeTokenHolder, null, extension);
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects,
            Boolean continueOnFailure, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        if (unfileObjects == UnfileObject.UNFILE) {
            throw new CmisConstraintException("Unfiling not supported");
        }
        if (repository.getRootFolderId(coreSession).equals(folderId)) {
            throw new CmisInvalidArgumentException("Cannot delete root");
        }
        try {
            IdRef docRef = new IdRef(folderId);
            DocumentModel doc = getDocumentModel(docRef);
            if (!doc.isFolder()) {
                throw new CmisInvalidArgumentException("Not a folder: "
                        + folderId);
            }
            coreSession.removeDocument(docRef);
            coreSession.save();
            // TODO returning null fails in opencmis 0.1.0 due to
            // org.apache.chemistry.opencmis.client.runtime.PersistentFolderImpl.deleteTree
            return new FailedToDeleteDataImpl();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public AllowableActions getAllowableActions(String repositoryId,
            String objectId, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentStream getContentStream(String repositoryId, String objectId,
            String streamId, BigInteger offset, BigInteger length,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);

        if (streamId != null) {
            throw new CmisInvalidArgumentException("Invalid stream id: "
                    + streamId);
        }
        DocumentModel doc = getDocumentModel(new IdRef(objectId));
        ContentStream cs = NuxeoPropertyData.getContentStream(doc);
        if (cs != null) {
            return cs;
        }
        throw new CmisConstraintException("No content stream: " + objectId);
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);

        DocumentModel doc = getDocumentModel(new PathRef(path));
        ObjectData data = new NuxeoObjectData(repository, doc, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension);
        if (context.isObjectInfoRequired()) {
            addObjectInfo(getObjectInfo(repositoryId, data));
        }
        return data;
    }

    @Override
    public Properties getProperties(String repositoryId, String objectId,
            String filter, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<RenditionData> getRenditions(String repositoryId,
            String objectId, String renditionFilter, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectInfo getObjectInfo(String repositoryId, String objectId) {
        checkRepositoryId(repositoryId);
        return super.getObjectInfo(repositoryId, objectId);
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectIdHolder,
            String targetFolderId, String sourceFolderId,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        String objectId;
        if (objectIdHolder == null
                || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        if (repository.getRootFolderId(coreSession).equals(objectId)) {
            throw new CmisConstraintException("Cannot move root");
        }
        if (targetFolderId == null) {
            throw new CmisInvalidArgumentException("Missing target folder ID");
        }
        try {
            DocumentRef docRef = new IdRef(objectId);
            getDocumentModel(docRef); // check exists and not deleted
            DocumentModel parent = coreSession.getParentDocument(docRef);
            if (isFilteredOut(parent)) {
                throw new CmisObjectNotFoundException("No parent: " + objectId);
            }
            if (sourceFolderId == null) {
                sourceFolderId = parent.getId();
            } else {
                // check it's the actual parent
                if (!parent.getId().equals(sourceFolderId)) {
                    throw new CmisInvalidArgumentException("Object " + objectId
                            + " is not filed in " + sourceFolderId);
                }
            }
            IdRef targetRef = new IdRef(targetFolderId);
            DocumentModel target = getDocumentModel(targetRef);
            if (!target.isFolder()) {
                throw new CmisInvalidArgumentException(
                        "Target is not a folder: " + targetFolderId);
            }
            coreSession.move(docRef, targetRef, null);
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void setContentStream(String repositoryId,
            Holder<String> objectIdHolder, Boolean overwriteFlag,
            Holder<String> changeTokenHolder, ContentStream contentStream,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        String objectId;
        if (objectIdHolder == null
                || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }

        DocumentModel doc = getDocumentModel(new IdRef(objectId));
        // TODO test doc checkout state
        try {
            NuxeoPropertyData.setContentStream(doc, contentStream,
                    !Boolean.FALSE.equals(overwriteFlag));
            coreSession.saveDocument(doc);
            coreSession.save();
        } catch (CmisBaseException e) {
            throw e;
        } catch (Exception e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void updateProperties(String repositoryId,
            Holder<String> objectIdHolder, Holder<String> changeTokenHolder,
            Properties properties, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        String objectId;
        if (objectIdHolder == null
                || (objectId = objectIdHolder.getValue()) == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        DocumentModel doc = getDocumentModel(new IdRef(objectId));
        NuxeoObjectData object = new NuxeoObjectData(repository, doc, null,
                null, null, null, null, null, null);
        String changeToken = changeTokenHolder == null ? null
                : changeTokenHolder.getValue();
        updateProperties(object, changeToken, properties, false);
        try {
            coreSession.saveDocument(doc);
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl addAces,
            Acl removeAces, AclPropagation aclPropagation,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl aces,
            AclPropagation aclPropagation) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl getAcl(String repositoryId, String objectId,
            Boolean onlyBasicPermissions, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectList getContentChanges(String repositoryId,
            Holder<String> changeLogTokenHolder, Boolean includeProperties,
            String filter, Boolean includePolicyIds, Boolean includeAcl,
            BigInteger maxItems, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectList query(String repositoryId, String statement,
            Boolean searchAllVersions, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void addObjectToFolder(String repositoryId, String objectId,
            String folderId, Boolean allVersions, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        if (folderId != null) {
            // check it's the actual parent
            try {
                DocumentModel folder = getDocumentModel(new IdRef(folderId));
                DocumentModel parent = coreSession.getParentDocument(new IdRef(
                        objectId));
                if (!parent.getId().equals(folder.getId())) {
                    throw new CmisInvalidArgumentException("Object " + objectId
                            + " is not filed in  " + folderId);
                }
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
        deleteObject(repositoryId, objectId, Boolean.FALSE, extension);
    }

    @Override
    public ObjectList getCheckedOutDocs(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        if (folderId == null) {
            throw new CmisInvalidArgumentException("Null folderId");
        }
        return getChildrenInternal(repositoryId, folderId, filter, orderBy,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment,
                maxItems == null ? -1 : maxItems.intValue(),
                skipCount == null ? -1 : skipCount.intValue(), false,
                context.isObjectInfoRequired() ? this : null);
    }

    protected ObjectInFolderList getChildrenInternal(String repositoryId,
            String folderId, String filter, String orderBy,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, int maxItems, int skipCount,
            boolean folderOnly, ObjectInfoHandler objectInfos) {
        ObjectInFolderListImpl result = new ObjectInFolderListImpl();
        List<ObjectInFolderData> list = new ArrayList<ObjectInFolderData>();
        DocumentModel folder = getDocumentModel(new IdRef(folderId));
        if (!folder.isFolder()) {
            return null;
        }
        DocumentModelList children;
        try {
            children = coreSession.getChildren(folder.getRef());
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
        for (DocumentModel child : children) {
            if (isFilteredOut(child)) {
                continue;
            }
            if (folderOnly && !child.isFolder()) {
                continue;
            }
            NuxeoObjectData data = new NuxeoObjectData(repository, child,
                    filter, includeAllowableActions, includeRelationships,
                    renditionFilter, Boolean.FALSE, Boolean.FALSE, null);
            ObjectInFolderDataImpl oifd = new ObjectInFolderDataImpl();
            oifd.setObject(data);
            if (Boolean.TRUE.equals(includePathSegment)) {
                oifd.setPathSegment(child.getName());
            }
            list.add(oifd);
            if (objectInfos != null) {
                objectInfos.addObjectInfo(getObjectInfo(repositoryId, data));
            }
        }
        result.setObjects(list);
        // TODO orderBy
        // TODO maxItems, skipCount
        result.setHasMoreItems(Boolean.FALSE);
        result.setNumItems(BigInteger.valueOf(list.size()));
        if (objectInfos != null) {
            objectInfos.addObjectInfo(getObjectInfo(repositoryId, folderId));
        }
        return result;
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        if (folderId == null) {
            throw new CmisInvalidArgumentException("Null folderId");
        }
        int levels = depth == null ? 2 : depth.intValue(); // default 2
        if (levels == 0) {
            throw new CmisInvalidArgumentException("Invalid depth: 0");
        }
        return getDescendantsInternal(repositoryId, folderId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment, 0, levels, false,
                context.isObjectInfoRequired() ? this : null);
    }

    protected List<ObjectInFolderContainer> getDescendantsInternal(
            String repositoryId, String folderId, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegments, int level, int maxLevels,
            boolean folderOnly, ObjectInfoHandler objectInfos) {
        if (maxLevels != -1 && level >= maxLevels) {
            return null;
        }
        ObjectInFolderList children = getChildrenInternal(repositoryId,
                folderId, filter, null, includeAllowableActions,
                includeRelationships, renditionFilter, includePathSegments, -1,
                0, folderOnly, objectInfos);
        if (children == null) {
            return Collections.emptyList();
        }
        List<ObjectInFolderContainer> res = new ArrayList<ObjectInFolderContainer>(
                children.getObjects().size());
        for (ObjectInFolderData child : children.getObjects()) {
            ObjectInFolderContainerImpl oifc = new ObjectInFolderContainerImpl();
            oifc.setObject(child);
            // recurse
            List<ObjectInFolderContainer> subChildren = getDescendantsInternal(
                    repositoryId, child.getObject().getId(), filter,
                    includeAllowableActions, includeRelationships,
                    renditionFilter, includePathSegments, level + 1, maxLevels,
                    folderOnly, objectInfos);
            if (subChildren != null) {
                oifc.setChildren(subChildren);
            }
            res.add(oifc);
        }
        return res;
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        List<ObjectParentData> parents = getObjectParentsInternal(repositoryId,
                folderId, filter, null, null, null, Boolean.TRUE, true);
        return parents.isEmpty() ? null : parents.get(0).getObject();
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId,
            String objectId, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        return getObjectParentsInternal(repositoryId, objectId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includeRelativePathSegment, false);
    }

    protected List<ObjectParentData> getObjectParentsInternal(
            String repositoryId, String objectId, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, boolean folderOnly) {
        String pathSegment;
        String parentId;
        try {
            DocumentRef docRef = new IdRef(objectId);
            if (!coreSession.exists(docRef)) {
                throw new CmisObjectNotFoundException(objectId);
            }
            DocumentModel doc = coreSession.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new CmisObjectNotFoundException(objectId);
            }
            if (folderOnly && !doc.isFolder()) {
                throw new CmisInvalidArgumentException("Not a folder: "
                        + objectId);
            }
            pathSegment = doc.getName();
            DocumentModel parent = coreSession.getParentDocument(docRef);
            if (parent == null || isFilteredOut(parent)) {
                return Collections.emptyList();
            }
            parentId = parent.getId();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }

        ObjectData od = getObject(repositoryId, parentId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                Boolean.FALSE, Boolean.FALSE, null);
        ObjectParentDataImpl opd = new ObjectParentDataImpl(od);
        if (!Boolean.FALSE.equals(includeRelativePathSegment)) {
            opd.setRelativePathSegment(pathSegment);
        }
        return Collections.<ObjectParentData> singletonList(opd);
    }

    @Override
    public void applyPolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectData> getAppliedPolicies(String repositoryId,
            String objectId, String filter, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectList getObjectRelationships(String repositoryId,
            String objectId, Boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, String typeId,
            String filter, Boolean includeAllowableActions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelCheckOut(String repositoryId, String objectId,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkIn(String repositoryId, Holder<String> objectIdHolder,
            Boolean major, Properties properties, ContentStream contentStream,
            String checkinComment, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkOut(String repositoryId, Holder<String> objectIdHolder,
            ExtensionsData extension, Holder<Boolean> contentCopiedHolder) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectData> getAllVersions(String repositoryId,
            String objectId, String versionSeriesId, String filter,
            Boolean includeAllowableActions, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectData getObjectOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties getPropertiesOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteObject(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        try {
            DocumentRef docRef = new IdRef(objectId);
            DocumentModel doc = getDocumentModel(docRef);
            if (doc.isFolder()) {
                // check that there are no children left
                DocumentModelList docs = coreSession.getChildren(docRef, null,
                        getDocumentFilter(), null);
                if (docs.size() > 0) {
                    throw new CmisConstraintException(
                            "Cannot delete non-empty folder: " + objectId);
                }
            }
            coreSession.removeDocument(doc.getRef());
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId,
            String objectId, Boolean allVersions, ExtensionsData extension) {
        deleteObject(repositoryId, objectId, allVersions, extension);
    }

}
