/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.math.BigInteger;
import java.util.ArrayList;
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
import org.apache.chemistry.opencmis.client.api.QueryStatement;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.QueryStatementImpl;
import org.apache.chemistry.opencmis.client.runtime.util.AbstractPageFetcher;
import org.apache.chemistry.opencmis.client.runtime.util.CollectionIterable;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.Properties;
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

    private static final long serialVersionUID = 1L;

    public static final OperationContext DEFAULT_CONTEXT = new OperationContextImpl(
            null, false, true, false, IncludeRelationships.NONE, null, true,
            null, true, 10);

    private final CoreSession coreSession;

    private final String repositoryId;

    protected final NuxeoObjectFactory objectFactory;

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

    /** Converts from an untyped map to a {@link Properties} object. */
    protected Properties convertProperties(Map<String, ?> properties) {
        if (properties == null) {
            return null;
        }
        // find type
        String typeId = (String) properties.get(PropertyIds.OBJECT_TYPE_ID);
        if (typeId == null) {
            throw new IllegalArgumentException("Missing type");
        }
        ObjectType type = getTypeDefinition(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type: " + typeId);
        }
        return objectFactory.convertProperties(properties, type, null);
    }

    @Override
    public ObjectId createDocument(Map<String, ?> properties,
            ObjectId folderId, ContentStream contentStream,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        String id = service.createDocument(repositoryId,
                convertProperties(properties), folderId == null ? null
                        : folderId.getId(), contentStream, versioningState,
                objectFactory.convertPolicies(policies),
                objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), null);
        return createObjectId(id);
    }

    @Override
    public ObjectId createFolder(Map<String, ?> properties, ObjectId folderId) {
        return createFolder(properties, folderId, null, null, null);
    }

    @Override
    public ObjectId createFolder(Map<String, ?> properties, ObjectId folderId,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        String id = service.createFolder(repositoryId,
                convertProperties(properties), folderId == null ? null
                        : folderId.getId(),
                objectFactory.convertPolicies(policies),
                objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), null);
        return createObjectId(id);
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
        String id = service.createRelationship(repositoryId,
                convertProperties(properties),
                objectFactory.convertPolicies(policies),
                objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), null);
        return createObjectId(id);
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

    @Override
    public CmisObject getObject(String objectId) {
        return getObject(objectId, getDefaultContext());
    }

    /** Gets a CMIS object given a Nuxeo {@link DocumentModel}. */
    public CmisObject getObject(DocumentModel doc, OperationContext context) {
        ObjectData data = new NuxeoObjectData(service, doc, context);
        return objectFactory.convertObject(data, context);
    }

    @Override
    public CmisObject getObject(ObjectId objectId, OperationContext context) {
        if (objectId == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        return getObject(objectId.getId(), context);
    }

    @Override
    public CmisObject getObject(String objectId, OperationContext context) {
        if (objectId == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        if (context == null) {
            throw new CmisInvalidArgumentException("Missing operation context");
        }
        NuxeoObjectData data = service.getObject(repositoryId, objectId,
                context.getFilterString(),
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
        ObjectData data = service.getObjectByPath(repositoryId, path,
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
        TypeDefinition typeDefinition = service.getTypeDefinition(repositoryId,
                typeId, null);
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

    public ItemIterable<CmisObject> queryObjects(String typeId, String where,
            boolean searchAllVersions, OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryStatement createQueryStatement(String statement) {
        return new QueryStatementImpl(this, statement);
    }

    @Override
    public ItemIterable<Relationship> getRelationships(final ObjectId objectId,
            final boolean includeSubRelationshipTypes,
            final RelationshipDirection relationshipDirection,
            final ObjectType type, final OperationContext context) {
        final String typeId = type == null ? null : type.getId();
        AbstractPageFetcher<Relationship> pageFetcher = new AbstractPageFetcher<Relationship>(
                context.getMaxItemsPerPage()) {
            @Override
            protected Page<Relationship> fetchPage(long skipCount) {
                ObjectList relations = service.getObjectRelationships(
                        repositoryId, objectId.getId(),
                        Boolean.valueOf(includeSubRelationshipTypes),
                        relationshipDirection, typeId, null, null,
                        BigInteger.valueOf(this.maxNumItems),
                        BigInteger.valueOf(skipCount), null);
                // convert objects
                List<Relationship> page = new ArrayList<Relationship>();
                if (relations.getObjects() != null) {
                    for (ObjectData data : relations.getObjects()) {
                        CmisObject ob;
                        if (data instanceof NuxeoObjectData) {
                            ob = objectFactory.convertObject(data, context);
                        } else {
                            ob = getObject(data.getId(), context);
                        }
                        if (!(ob instanceof Relationship)) {
                            // should not happen...
                            continue;
                        }
                        page.add((Relationship) ob);
                    }
                }
                return new Page<Relationship>(page, relations.getNumItems(),
                        relations.hasMoreItems());
            }
        };
        return new CollectionIterable<Relationship>(pageFetcher);
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

    @Override
    public void removeObjectFromCache(ObjectId objectId) {
    }

    @Override
    public void removeObjectFromCache(String objectId) {
    }

}
