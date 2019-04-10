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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.ChangeEvents;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.util.EmptyItemIterable;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;

/**
 * Nuxeo Persistent Session, having a direct connection to a Nuxeo
 * {@link CoreSession}.
 */
public class NuxeoSession implements Session {

    public static final OperationContext DEFAULT_CONTEXT = new OperationContextImpl(
            null, false, true, false, IncludeRelationships.NONE, null, true,
            null, true, 10);

    private final CoreSession coreSession;

    private final String repositoryId;

    private final NuxeoObjectFactory objectFactory;

    private final NuxeoCmisService service;

    private final NuxeoBinding binding;

    private OperationContext defaultContext = DEFAULT_CONTEXT;

    public NuxeoSession(CoreSession coreSession, NuxeoRepository repository,
            CallContext context) {
        this.coreSession = coreSession;
        repositoryId = repository.getId();
        objectFactory = new NuxeoObjectFactory(this);

        service = new NuxeoCmisService(repository, context, coreSession);
        binding = new NuxeoBinding(service);
    }

    @Override
    public NuxeoObjectFactory getObjectFactory() {
        return objectFactory;
    }

    @Override
    public NuxeoBinding getBinding() {
        return binding;
    }

    public NuxeoCmisService getService() {
        return service;
    }

    protected CoreSession getCoreSession() {
        return coreSession;
    }

    @Override
    public void clear() {
    }

    public void save() {
        try {
            coreSession.save();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void setDefaultContext(OperationContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    @Override
    public OperationContext getDefaultContext() {
        return defaultContext;
    }

    protected String getRepositoryId() {
        return coreSession.getRepositoryName();
    }

    @Override
    public ObjectId createObjectId(String id) {
        return new ObjectIdImpl(id);
    }

    @Override
    public ObjectId createDocument(Map<String, ?> properties,
            ObjectId folderId, ContentStream contentStream,
            VersioningState versioningState) {
        return createDocument(properties, folderId, contentStream,
                versioningState, null, null, null);
    }

    @Override
    public ObjectId createDocument(Map<String, ?> properties,
            ObjectId folderId, ContentStream contentStream,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId createFolder(Map<String, ?> properties, ObjectId folderId) {
        return createFolder(properties, folderId, null, null, null);
    }

    @Override
    public ObjectId createFolder(Map<String, ?> properties, ObjectId folderId,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationContext createOperationContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationContext createOperationContext(Set<String> filter,
            boolean includeAcls, boolean includeAllowableActions,
            boolean includePolicies, IncludeRelationships includeRelationships,
            Set<String> renditionFilter, boolean includePathSegments,
            String orderBy, boolean cacheEnabled, int maxItemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId createPolicy(Map<String, ?> properties, ObjectId folderId) {
        return createPolicy(properties, folderId, null, null, null);
    }

    @Override
    public ObjectId createPolicy(Map<String, ?> properties, ObjectId folderId,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId createRelationship(Map<String, ?> properties) {
        return createRelationship(properties, null, null, null);
    }

    @Override
    public ObjectId createRelationship(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, ObjectId folderId,
            VersioningState versioningState) {
        return createDocumentFromSource(source, properties, folderId,
                versioningState, null, null, null);
    }

    @Override
    public ObjectId createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, ObjectId folderId,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs(OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ChangeEvents getContentChanges(String changeLogToken,
            boolean includeProperties, long maxNumItems) {
        return getContentChanges(changeLogToken, includeProperties,
                maxNumItems, getDefaultContext());
    }

    @Override
    public ChangeEvents getContentChanges(String changeLogToken,
            boolean includeProperties, long maxNumItems,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public CmisObject getObject(ObjectId objectId) {
        return getObject(objectId, getDefaultContext());
    }

    /** Gets a CMIS object given a Nuxeo {@link DocumentModel}. */
    public CmisObject getObject(DocumentModel doc, OperationContext context) {
        ObjectData data = new NuxeoObjectData(service, doc, context);
        return objectFactory.convertObject(data, context);
    }

    @Override
    public CmisObject getObject(ObjectId objectId, OperationContext context) {
        if (objectId == null || objectId.getId() == null) {
            throw new CmisInvalidArgumentException("Missing object or ID");
        }
        if (context == null) {
            throw new CmisInvalidArgumentException("Missing operation context");
        }
        NuxeoObjectData data = service.getObject(getRepositoryId(),
                objectId.getId(), context.getFilterString(),
                Boolean.valueOf(context.isIncludeAllowableActions()),
                context.getIncludeRelationships(),
                context.getRenditionFilterString(),
                Boolean.valueOf(context.isIncludePolicies()),
                Boolean.valueOf(context.isIncludeAcls()), null);
        return objectFactory.convertObject(data, context);
    }

    @Override
    public CmisObject getObjectByPath(String path) {
        return getObjectByPath(path, getDefaultContext());
    }

    @Override
    public CmisObject getObjectByPath(String path, OperationContext context) {
        if (path == null || !path.startsWith("/")) {
            throw new CmisInvalidArgumentException("Invalid path: " + path);
        }
        if (context == null) {
            throw new CmisInvalidArgumentException("Missing operation context");
        }
        ObjectData data = service.getObjectByPath(getRepositoryId(), path,
                context.getFilterString(),
                Boolean.valueOf(context.isIncludeAllowableActions()),
                context.getIncludeRelationships(),
                context.getRenditionFilterString(),
                Boolean.valueOf(context.isIncludePolicies()),
                Boolean.valueOf(context.isIncludeAcls()), null);
        return getObjectFactory().convertObject(data, context);
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return service.getRepositoryInfo(repositoryId, null);
    }

    @Override
    public Folder getRootFolder() {
        return getRootFolder(getDefaultContext());
    }

    @Override
    public Folder getRootFolder(OperationContext context) {
        String id = getRepositoryInfo().getRootFolderId();
        CmisObject folder = getObject(createObjectId(id), context);
        if (!(folder instanceof Folder)) {
            throw new CmisRuntimeException("Root object is not a Folder but: "
                    + folder.getClass().getName());
        }
        return (Folder) folder;
    }

    @Override
    public ItemIterable<ObjectType> getTypeChildren(String typeId,
            boolean includePropertyDefinitions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectType getTypeDefinition(String typeId) {
        TypeDefinition typeDefinition = service.getTypeDefinition(
                getRepositoryId(), typeId, null);
        return objectFactory.convertTypeDefinition(typeDefinition);
    }

    @Override
    public List<Tree<ObjectType>> getTypeDescendants(String typeId, int depth,
            boolean includePropertyDefinitions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<QueryResult> query(String statement,
            boolean searchAllVersions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<QueryResult> query(String statement,
            boolean searchAllVersions, OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<Relationship> getRelationships(ObjectId objectId,
            boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, ObjectType type,
            OperationContext context) {
        return EmptyItemIterable.instance();
    }

    @Override
    public Acl getAcl(ObjectId objectId, boolean onlyBasicPermissions) {
        throw new CmisNotSupportedException();
    }

    @Override
    public Acl applyAcl(ObjectId objectId, List<Ace> addAces,
            List<Ace> removeAces, AclPropagation aclPropagation) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void applyPolicy(ObjectId objectId, ObjectId... policyIds) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void removePolicy(ObjectId objectId, ObjectId... policyIds) {
        throw new CmisNotSupportedException();
    }

}
