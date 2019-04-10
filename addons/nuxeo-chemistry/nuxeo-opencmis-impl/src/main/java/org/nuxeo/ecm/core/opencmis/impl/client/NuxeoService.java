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
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.math.BigInteger;
import java.util.List;

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
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;

/**
 * Local client service delegating to the server view of the services.
 */
public class NuxeoService implements CmisService {

    public static final String BINDING_JAVA = "java";

    private final NuxeoCmisService service;

    public NuxeoService(NuxeoCmisService service) {
        this.service = service;
    }

    @Override
    public void close() {
        service.close();
    }

    @Override
    public ObjectInfo getObjectInfo(String repositoryId, String objectId) {
        return null;
    }

    @Override
    public RepositoryInfo getRepositoryInfo(String repositoryId,
            ExtensionsData extension) {
        return service.getRepositoryInfo(repositoryId, extension);
    }

    @Override
    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        return service.getRepositoryInfos(extension);
    }

    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId,
            String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.getTypeChildren(repositoryId, typeId,
                includePropertyDefinitions, maxItems, skipCount, extension);
    }

    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId,
            ExtensionsData extension) {
        return service.getTypeDefinition(repositoryId, typeId, extension);
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(
            String repositoryId, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, ExtensionsData extension) {
        return service.getTypeDescendants(repositoryId, typeId, depth,
                includePropertyDefinitions, extension);
    }

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl addAces,
            Acl removeAces, AclPropagation aclPropagation,
            ExtensionsData extension) {
        // TODO add / remove ACEs
        throw new UnsupportedOperationException();
    }

    // ---------

    @Override
    public Acl applyAcl(String repositoryId, String objectId, Acl aces,
            AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl getAcl(String repositoryId, String objectId,
            Boolean onlyBasicPermissions, ExtensionsData extension) {
        return service.getAcl(repositoryId, objectId, onlyBasicPermissions,
                extension);
    }

    @Override
    public ObjectList getContentChanges(String repositoryId,
            Holder<String> changeLogToken, Boolean includeProperties,
            String filter, Boolean includePolicyIds, Boolean includeAcl,
            BigInteger maxItems, ExtensionsData extension) {
        return service.getContentChanges(repositoryId, changeLogToken,
                includeProperties, filter, includePolicyIds, includeAcl,
                maxItems, extension);
    }

    @Override
    public ObjectList query(String repositoryId, String statement,
            Boolean searchAllVersions, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.query(repositoryId, statement, searchAllVersions,
                includeAllowableActions, includeRelationships, renditionFilter,
                maxItems, skipCount, extension);
    }

    @Override
    public void addObjectToFolder(String repositoryId, String objectId,
            String folderId, Boolean allVersions, ExtensionsData extension) {
        service.addObjectToFolder(repositoryId, objectId, folderId,
                allVersions, extension);
    }

    @Override
    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        service.removeObjectFromFolder(repositoryId, objectId, folderId,
                extension);
    }

    @Override
    public ObjectList getCheckedOutDocs(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.getCheckedOutDocs(repositoryId, folderId, filter,
                orderBy, includeAllowableActions, includeRelationships,
                renditionFilter, maxItems, skipCount, extension);
    }

    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        return service.getChildren(repositoryId, folderId, filter, orderBy,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment, maxItems, skipCount, extension);
    }

    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        return service.getDescendants(repositoryId, folderId, depth, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment, extension);
    }

    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        return service.getFolderParent(repositoryId, folderId, filter,
                extension);
    }

    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        return service.getFolderTree(repositoryId, folderId, depth, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePathSegment, extension);
    }

    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId,
            String objectId, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        return service.getObjectParents(repositoryId, objectId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includeRelativePathSegment, extension);
    }

    @Override
    public String create(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        return service.createDocument(repositoryId, properties, folderId,
                contentStream, versioningState, policies, addAces, removeAces,
                extension);
    }

    @Override
    public String createDocumentFromSource(String repositoryId,
            String sourceId, Properties properties, String folderId,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        return service.createDocumentFromSource(repositoryId, sourceId,
                properties, folderId, versioningState, policies, addAces,
                removeAces, extension);
    }

    @Override
    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return service.createFolder(repositoryId, properties, folderId,
                policies, addAces, removeAces, extension);
    }

    @Override
    public String createPolicy(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return service.createPolicy(repositoryId, properties, folderId,
                policies, addAces, removeAces, extension);
    }

    @Override
    public String createRelationship(String repositoryId,
            Properties properties, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return service.createRelationship(repositoryId, properties, policies,
                addAces, removeAces, extension);
    }

    @Override
    public void deleteContentStream(String repositoryId,
            Holder<String> objectId, Holder<String> changeToken,
            ExtensionsData extension) {
        service.deleteContentStream(repositoryId, objectId, changeToken,
                extension);
    }

    @Override
    public void deleteObject(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        service.deleteObjectOrCancelCheckOut(repositoryId, objectId,
                allVersions, extension);
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId,
            String objectId, Boolean allVersions, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects,
            Boolean continueOnFailure, ExtensionsData extension) {
        return service.deleteTree(repositoryId, folderId, allVersions,
                unfileObjects, continueOnFailure, extension);
    }

    @Override
    public AllowableActions getAllowableActions(String repositoryId,
            String objectId, ExtensionsData extension) {
        return service.getAllowableActions(repositoryId, objectId, extension);
    }

    @Override
    public ContentStream getContentStream(String repositoryId, String objectId,
            String streamId, BigInteger offset, BigInteger length,
            ExtensionsData extension) {
        return service.getContentStream(repositoryId, objectId, streamId,
                offset, length, extension);
    }

    @Override
    public ObjectData getObject(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return service.getObject(repositoryId, objectId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension);
    }

    @Override
    public ObjectData getObjectByPath(String repositoryId, String path,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return service.getObjectByPath(repositoryId, path, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension);
    }

    @Override
    public Properties getProperties(String repositoryId, String objectId,
            String filter, ExtensionsData extension) {
        return service.getProperties(repositoryId, objectId, filter, extension);
    }

    @Override
    public List<RenditionData> getRenditions(String repositoryId,
            String objectId, String renditionFilter, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        return service.getRenditions(repositoryId, objectId, renditionFilter,
                maxItems, skipCount, extension);
    }

    @Override
    public void moveObject(String repositoryId, Holder<String> objectId,
            String targetFolderId, String sourceFolderId,
            ExtensionsData extension) {
        service.moveObject(repositoryId, objectId, targetFolderId,
                sourceFolderId, extension);
    }

    @Override
    public void setContentStream(String repositoryId, Holder<String> objectId,
            Boolean overwriteFlag, Holder<String> changeToken,
            ContentStream contentStream, ExtensionsData extension) {
        service.setContentStream(repositoryId, objectId, overwriteFlag,
                changeToken, contentStream, extension);
    }

    @Override
    public void updateProperties(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, Properties properties,
            ExtensionsData extension) {
        // TODO acl null?
        service.updateProperties(repositoryId, objectId, changeToken,
                properties, extension);
    }

    @Override
    public void applyPolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        service.applyPolicy(repositoryId, policyId, objectId, extension);
    }

    @Override
    public List<ObjectData> getAppliedPolicies(String repositoryId,
            String objectId, String filter, ExtensionsData extension) {
        return service.getAppliedPolicies(repositoryId, objectId, filter,
                extension);
    }

    @Override
    public void removePolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        service.removePolicy(repositoryId, policyId, objectId, extension);
    }

    @Override
    public ObjectList getObjectRelationships(String repositoryId,
            String objectId, Boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, String typeId,
            String filter, Boolean includeAllowableActions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.getObjectRelationships(repositoryId, objectId,
                includeSubRelationshipTypes, relationshipDirection, typeId,
                filter, includeAllowableActions, maxItems, skipCount, extension);
    }

    @Override
    public void cancelCheckOut(String repositoryId, String objectId,
            ExtensionsData extension) {
        service.cancelCheckOut(repositoryId, objectId, extension);
    }

    @Override
    public void checkIn(String repositoryId, Holder<String> objectId,
            Boolean major, Properties properties, ContentStream contentStream,
            String checkinComment, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        service.checkIn(repositoryId, objectId, major, properties,
                contentStream, checkinComment, policies, addAces, removeAces,
                extension);
    }

    @Override
    public void checkOut(String repositoryId, Holder<String> objectId,
            ExtensionsData extension, Holder<Boolean> contentCopied) {
        service.checkOut(repositoryId, objectId, extension, contentCopied);
    }

    @Override
    public List<ObjectData> getAllVersions(String repositoryId,
            String objectId, String versionSeriesId, String filter,
            Boolean includeAllowableActions, ExtensionsData extension) {
        return service.getAllVersions(repositoryId, objectId, versionSeriesId,
                filter, includeAllowableActions, extension);
    }

    @Override
    public ObjectData getObjectOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return service.getObjectOfLatestVersion(repositoryId, objectId,
                versionSeriesId, major, filter, includeAllowableActions,
                includeRelationships, renditionFilter, includePolicyIds,
                includeAcl, extension);
    }

    @Override
    public Properties getPropertiesOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, ExtensionsData extension) {
        return service.getPropertiesOfLatestVersion(repositoryId, objectId,
                versionSeriesId, major, filter, extension);

    }

}
