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
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.impl.CompoundFilter;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.impl.LifeCycleFilter;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoObject.NuxeoFolder;
import org.nuxeo.ecm.core.schema.FacetNames;

/**
 * Nuxeo implementation of the CMIS Services.
 */
public class NuxeoCmisService implements CmisService {

    public static final String REPOSITORY = "NuxeService.Repository";

    private static final String rootFolderId = "root-folder-id";

    protected NuxeoRepository repository; // TODO one per repositoryId

    private CoreSession coreSession;

    /** hide HiddenInNavigation and deleted objects */
    protected final Filter documentFilter;

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
        return repository.getTypeDefinition(typeId);

    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId,
            String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(
            String repositoryId, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        DocumentModel doc;
        try {
            DocumentRef docRef = new IdRef(objectId);
            if (!coreSession.exists(docRef)) {
                throw new CmisObjectNotFoundException(objectId);
            }
            doc = coreSession.getDocument(docRef);
            if (isFilteredOut(doc)) {
                throw new CmisObjectNotFoundException(objectId);
            }
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }

        ObjectData od = new NuxeoObjectData(repository, doc, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension);
        // TODO fillInformationForAtomLinks(od, objectInfos);
        return od;
    }

    protected Filter getDocumentFilter() {
        return documentFilter;
    }

    protected boolean isFilteredOut(DocumentModel doc) throws ClientException {
        return !documentFilter.accept(doc);
    }

    private DocumentModel createDocumentModel(ObjectId folder,
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
        if (properties != null //
                && (p = properties.getProperties()) != null //
                && (d = p.get(PropertyIds.OBJECT_TYPE_ID)) != null) {
            typeId = (String) d.getFirstValue();
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
                throw new CmisRuntimeException();
            }
        }
        TypeDefinition type = repository.getTypeDefinition(typeId);
        if (type == null || type.getBaseTypeId() != baseType) {
            throw new IllegalArgumentException(typeId);
        }
        DocumentModel doc = createDocumentModel(folder, type);
        NuxeoObjectData object = new NuxeoObjectData(repository, doc, null,
                null, null, null, null, null, null);
        updateProperties(object, null, type, properties, true);
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
            TypeDefinition type, Properties properties, boolean creation) {
        // TODO changeToken
        // NuxeoObjectData entry = getObjectEntry(object);
        // if (entry == null) {
        // throw new CmisObjectNotFoundException(object.getId());
        // }
        Map<String, PropertyData<?>> p;
        if (properties != null && (p = properties.getProperties()) != null) {
            for (Entry<String, PropertyData<?>> en : p.entrySet()) {
                setObjectProperty(object, en.getKey(), en.getValue(), type,
                        creation);
            }
        }
    }

    protected <T> void setObjectProperty(NuxeoObjectData object, String key,
            PropertyData<T> d, TypeDefinition type, boolean creation) {
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        NuxeoObjectData object = createObject(properties, new ObjectIdImpl(
                folderId), BaseTypeId.CMIS_DOCUMENT, contentStream);
        return object.getId();
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteContentStream(String repositoryId,
            Holder<String> objectId, Holder<String> changeToken,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId,
            String objectId, Boolean allVersions, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects,
            Boolean continueOnFailure, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectId,
            String targetFolderId, String sourceFolderId,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentStream(String repositoryId, Holder<String> objectId,
            Boolean overwriteFlag, Holder<String> changeToken,
            ContentStream contentStream, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProperties(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, Properties properties,
            ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
            Holder<String> changeLogToken, Boolean includeProperties,
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
    public void checkIn(String repositoryId, Holder<String> objectId,
            Boolean major, Properties properties, ContentStream contentStream,
            String checkinComment, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        checkRepositoryId(repositoryId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkOut(String repositoryId, Holder<String> objectId,
            ExtensionsData extension, Holder<Boolean> contentCopied) {
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
