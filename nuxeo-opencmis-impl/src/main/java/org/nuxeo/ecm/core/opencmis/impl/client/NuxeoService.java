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

import org.apache.chemistry.opencmis.commons.api.Acl;
import org.apache.chemistry.opencmis.commons.api.AclService;
import org.apache.chemistry.opencmis.commons.api.AllowableActions;
import org.apache.chemistry.opencmis.commons.api.ContentStream;
import org.apache.chemistry.opencmis.commons.api.DiscoveryService;
import org.apache.chemistry.opencmis.commons.api.ExtensionsData;
import org.apache.chemistry.opencmis.commons.api.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.api.Holder;
import org.apache.chemistry.opencmis.commons.api.MultiFilingService;
import org.apache.chemistry.opencmis.commons.api.NavigationService;
import org.apache.chemistry.opencmis.commons.api.ObjectData;
import org.apache.chemistry.opencmis.commons.api.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.api.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.api.ObjectList;
import org.apache.chemistry.opencmis.commons.api.ObjectParentData;
import org.apache.chemistry.opencmis.commons.api.ObjectService;
import org.apache.chemistry.opencmis.commons.api.PolicyService;
import org.apache.chemistry.opencmis.commons.api.Properties;
import org.apache.chemistry.opencmis.commons.api.RelationshipService;
import org.apache.chemistry.opencmis.commons.api.RenditionData;
import org.apache.chemistry.opencmis.commons.api.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.api.RepositoryService;
import org.apache.chemistry.opencmis.commons.api.TypeDefinition;
import org.apache.chemistry.opencmis.commons.api.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.api.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.api.VersioningService;
import org.apache.chemistry.opencmis.commons.api.server.CallContext;
import org.apache.chemistry.opencmis.commons.api.server.CmisService;
import org.apache.chemistry.opencmis.commons.api.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.apache.chemistry.opencmis.server.spi.ObjectInfoHolder;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;

/**
 * Local client service delegating to the server view of the services.
 */
public class NuxeoService implements CmisService {

    public static final String BINDING_JAVA = "java";

    private final NuxeoCmisService service;

    private final CallContext context = new CallContextImpl(BINDING_JAVA, null,
            false);

    private final ObjectInfoHolder objectInfos = null;

    public NuxeoService(NuxeoCmisService service) {
        this.service = service;
    }

    public void close() {
        service.close();
    }

    public ObjectInfo getObjectInfo(String repositoryId, String objectId) {
        return null;
    }

    public RepositoryInfo getRepositoryInfo(String repositoryId,
            ExtensionsData extension) {
        return service.getRepositoryInfo(context, repositoryId, extension);
    }

    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        return service.getRepositoryInfos(context, extension);
    }

    public TypeDefinitionList getTypeChildren(String repositoryId,
            String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.getTypeChildren(context, repositoryId, typeId,
                includePropertyDefinitions, maxItems, skipCount, extension);
    }

    public TypeDefinition getTypeDefinition(String repositoryId, String typeId,
            ExtensionsData extension) {
        return service.getTypeDefinition(context, repositoryId, typeId,
                extension);
    }

    public List<TypeDefinitionContainer> getTypeDescendants(
            String repositoryId, String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, ExtensionsData extension) {
        return service.getTypeDescendants(context, repositoryId, typeId, depth,
                includePropertyDefinitions, extension);
    }

    public Acl applyAcl(String repositoryId, String objectId, Acl addAces,
            Acl removeAces, AclPropagation aclPropagation,
            ExtensionsData extension) {
        // TODO add / remove ACEs
        throw new UnsupportedOperationException();
    }

    // ---------

    public Acl applyAcl(String repositoryId, String objectId, Acl aces,
            AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Acl getAcl(String repositoryId, String objectId,
            Boolean onlyBasicPermissions, ExtensionsData extension) {
        return service.getAcl(context, repositoryId, objectId,
                onlyBasicPermissions, extension);
    }

    public ObjectList getContentChanges(String repositoryId,
            Holder<String> changeLogToken, Boolean includeProperties,
            String filter, Boolean includePolicyIds, Boolean includeAcl,
            BigInteger maxItems, ExtensionsData extension) {
        return service.getContentChanges(context, repositoryId, changeLogToken,
                includeProperties, filter, includePolicyIds, includeAcl,
                maxItems, extension, objectInfos);
    }

    public ObjectList query(String repositoryId, String statement,
            Boolean searchAllVersions, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.query(context, repositoryId, statement,
                searchAllVersions, includeAllowableActions,
                includeRelationships, renditionFilter, maxItems, skipCount,
                extension);
    }

    public void addObjectToFolder(String repositoryId, String objectId,
            String folderId, Boolean allVersions, ExtensionsData extension) {
        service.addObjectToFolder(context, repositoryId, objectId, folderId,
                allVersions, extension, objectInfos);
    }

    public void removeObjectFromFolder(String repositoryId, String objectId,
            String folderId, ExtensionsData extension) {
        service.removeObjectFromFolder(context, repositoryId, objectId,
                folderId, extension, objectInfos);
    }

    public ObjectList getCheckedOutDocs(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.getCheckedOutDocs(context, repositoryId, folderId,
                filter, orderBy, includeAllowableActions, includeRelationships,
                renditionFilter, maxItems, skipCount, extension, objectInfos);
    }

    public ObjectInFolderList getChildren(String repositoryId, String folderId,
            String filter, String orderBy, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        return service.getChildren(context, repositoryId, folderId, filter,
                orderBy, includeAllowableActions, includeRelationships,
                renditionFilter, includePathSegment, maxItems, skipCount,
                extension, objectInfos);
    }

    public List<ObjectInFolderContainer> getDescendants(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        return service.getDescendants(context, repositoryId, folderId, depth,
                filter, includeAllowableActions, includeRelationships,
                renditionFilter, includePathSegment, extension, objectInfos);
    }

    public ObjectData getFolderParent(String repositoryId, String folderId,
            String filter, ExtensionsData extension) {
        return service.getFolderParent(context, repositoryId, folderId, filter,
                extension, objectInfos);
    }

    public List<ObjectInFolderContainer> getFolderTree(String repositoryId,
            String folderId, BigInteger depth, String filter,
            Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePathSegment, ExtensionsData extension) {
        return service.getFolderTree(context, repositoryId, folderId, depth,
                filter, includeAllowableActions, includeRelationships,
                renditionFilter, includePathSegment, extension, objectInfos);
    }

    public List<ObjectParentData> getObjectParents(String repositoryId,
            String objectId, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includeRelativePathSegment, ExtensionsData extension) {
        return service.getObjectParents(context, repositoryId, objectId,
                filter, includeAllowableActions, includeRelationships,
                renditionFilter, includeRelativePathSegment, extension,
                objectInfos);
    }

    public String create(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public String createDocument(String repositoryId, Properties properties,
            String folderId, ContentStream contentStream,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        return service.createDocument(context, repositoryId, properties,
                folderId, contentStream, versioningState, policies, addAces,
                removeAces, extension);
    }

    public String createDocumentFromSource(String repositoryId,
            String sourceId, Properties properties, String folderId,
            VersioningState versioningState, List<String> policies,
            Acl addAces, Acl removeAces, ExtensionsData extension) {
        return service.createDocumentFromSource(context, repositoryId,
                sourceId, properties, folderId, versioningState, policies,
                addAces, removeAces, extension);
    }

    public String createFolder(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return service.createFolder(context, repositoryId, properties,
                folderId, policies, addAces, removeAces, extension);
    }

    public String createPolicy(String repositoryId, Properties properties,
            String folderId, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return service.createPolicy(context, repositoryId, properties,
                folderId, policies, addAces, removeAces, extension);
    }

    public String createRelationship(String repositoryId,
            Properties properties, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        return service.createRelationship(context, repositoryId, properties,
                policies, addAces, removeAces, extension);
    }

    public void deleteContentStream(String repositoryId,
            Holder<String> objectId, Holder<String> changeToken,
            ExtensionsData extension) {
        service.deleteContentStream(context, repositoryId, objectId,
                changeToken, extension);
    }

    public void deleteObject(String repositoryId, String objectId,
            Boolean allVersions, ExtensionsData extension) {
        service.deleteObjectOrCancelCheckOut(context, repositoryId, objectId,
                allVersions, extension);
    }

    public void deleteObjectOrCancelCheckOut(String repositoryId,
            String objectId, Boolean allVersions, ExtensionsData extension) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public FailedToDeleteData deleteTree(String repositoryId, String folderId,
            Boolean allVersions, UnfileObject unfileObjects,
            Boolean continueOnFailure, ExtensionsData extension) {
        return service.deleteTree(context, repositoryId, folderId, allVersions,
                unfileObjects, continueOnFailure, extension);
    }

    public AllowableActions getAllowableActions(String repositoryId,
            String objectId, ExtensionsData extension) {
        return service.getAllowableActions(context, repositoryId, objectId,
                extension);
    }

    public ContentStream getContentStream(String repositoryId, String objectId,
            String streamId, BigInteger offset, BigInteger length,
            ExtensionsData extension) {
        return service.getContentStream(context, repositoryId, objectId,
                streamId, offset, length, extension);
    }

    public ObjectData getObject(String repositoryId, String objectId,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return service.getObject(context, repositoryId, objectId, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension, objectInfos);
    }

    public ObjectData getObjectByPath(String repositoryId, String path,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        return service.getObjectByPath(context, repositoryId, path, filter,
                includeAllowableActions, includeRelationships, renditionFilter,
                includePolicyIds, includeAcl, extension, objectInfos);
    }

    public Properties getProperties(String repositoryId, String objectId,
            String filter, ExtensionsData extension) {
        return service.getProperties(context, repositoryId, objectId, filter,
                extension);
    }

    public List<RenditionData> getRenditions(String repositoryId,
            String objectId, String renditionFilter, BigInteger maxItems,
            BigInteger skipCount, ExtensionsData extension) {
        return service.getRenditions(context, repositoryId, objectId,
                renditionFilter, maxItems, skipCount, extension);
    }

    public void moveObject(String repositoryId, Holder<String> objectId,
            String targetFolderId, String sourceFolderId,
            ExtensionsData extension) {
        service.moveObject(context, repositoryId, objectId, targetFolderId,
                sourceFolderId, extension, objectInfos);
    }

    public void setContentStream(String repositoryId, Holder<String> objectId,
            Boolean overwriteFlag, Holder<String> changeToken,
            ContentStream contentStream, ExtensionsData extension) {
        service.setContentStream(context, repositoryId, objectId,
                overwriteFlag, changeToken, contentStream, extension);
    }

    public void updateProperties(String repositoryId, Holder<String> objectId,
            Holder<String> changeToken, Properties properties,
            ExtensionsData extension) {
        // TODO acl null?
        service.updateProperties(context, repositoryId, objectId, changeToken,
                properties, null, extension, objectInfos);
    }

    public void applyPolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        service.applyPolicy(context, repositoryId, policyId, objectId,
                extension, objectInfos);
    }

    public List<ObjectData> getAppliedPolicies(String repositoryId,
            String objectId, String filter, ExtensionsData extension) {
        return service.getAppliedPolicies(context, repositoryId, objectId,
                filter, extension, objectInfos);
    }

    public void removePolicy(String repositoryId, String policyId,
            String objectId, ExtensionsData extension) {
        service.removePolicy(context, repositoryId, policyId, objectId,
                extension);
    }

    public ObjectList getObjectRelationships(String repositoryId,
            String objectId, Boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, String typeId,
            String filter, Boolean includeAllowableActions,
            BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return service.getObjectRelationships(context, repositoryId, objectId,
                includeSubRelationshipTypes, relationshipDirection, typeId,
                filter, includeAllowableActions, maxItems, skipCount,
                extension, objectInfos);
    }

    public void cancelCheckOut(String repositoryId, String objectId,
            ExtensionsData extension) {
        service.cancelCheckOut(context, repositoryId, objectId, extension);
    }

    public void checkIn(String repositoryId, Holder<String> objectId,
            Boolean major, Properties properties, ContentStream contentStream,
            String checkinComment, List<String> policies, Acl addAces,
            Acl removeAces, ExtensionsData extension) {
        service.checkIn(context, repositoryId, objectId, major, properties,
                contentStream, checkinComment, policies, addAces, removeAces,
                extension, objectInfos);
    }

    public void checkOut(String repositoryId, Holder<String> objectId,
            ExtensionsData extension, Holder<Boolean> contentCopied) {
        service.checkOut(context, repositoryId, objectId, extension,
                contentCopied, objectInfos);
    }

    public List<ObjectData> getAllVersions(String repositoryId,
            String objectId, String versionSeriesId, String filter,
            Boolean includeAllowableActions, ExtensionsData extension) {
        return service.getAllVersions(context, repositoryId, versionSeriesId,
                filter, includeAllowableActions, extension, objectInfos);
    }

    public ObjectData getObjectOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        // TODO objectId
        return service.getObjectOfLatestVersion(context, repositoryId,
                versionSeriesId, major, filter, includeAllowableActions,
                includeRelationships, renditionFilter, includePolicyIds,
                includeAcl, extension, objectInfos);
    }

    public Properties getPropertiesOfLatestVersion(String repositoryId,
            String objectId, String versionSeriesId, Boolean major,
            String filter, ExtensionsData extension) {
        // TODO objectId
        return service.getPropertiesOfLatestVersion(context, repositoryId,
                versionSeriesId, major, filter, extension);

    }

}
