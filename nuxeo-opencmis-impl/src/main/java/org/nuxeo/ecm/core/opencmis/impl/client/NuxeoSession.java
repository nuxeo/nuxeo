/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import static org.apache.chemistry.opencmis.commons.impl.Constants.RENDITION_NONE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.ChangeEvent;
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
import org.apache.chemistry.opencmis.client.runtime.QueryResultImpl;
import org.apache.chemistry.opencmis.client.runtime.QueryStatementImpl;
import org.apache.chemistry.opencmis.client.runtime.util.AbstractPageFetcher;
import org.apache.chemistry.opencmis.client.runtime.util.CollectionIterable;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.BulkUpdateObjectIdAndChangeToken;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BulkUpdateObjectIdAndChangeTokenImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Nuxeo Persistent Session, having a direct connection to a Nuxeo {@link CoreSession}.
 */
public class NuxeoSession implements Session {

    private static final long serialVersionUID = 1L;

    public static final OperationContext DEFAULT_CONTEXT = new OperationContextImpl(null, false, true, false,
            IncludeRelationships.NONE, null, true, null, true, 10);

    private final CoreSession coreSession;

    private final String repositoryId;

    protected final NuxeoObjectFactory objectFactory;

    private final CmisService service;

    private final NuxeoBinding binding;

    private OperationContext defaultContext = DEFAULT_CONTEXT;

    public NuxeoSession(NuxeoBinding binding, CallContext context) {
        this.coreSession = binding.getCoreSession();
        repositoryId = context.getRepositoryId();
        objectFactory = new NuxeoObjectFactory(this);
        service = binding.service;
        this.binding = binding;
    }

    @Override
    public NuxeoObjectFactory getObjectFactory() {
        return objectFactory;
    }

    @Override
    public NuxeoBinding getBinding() {
        return binding;
    }

    public CmisService getService() {
        return service;
    }

    protected CoreSession getCoreSession() {
        return coreSession;
    }

    @Override
    public void clear() {
    }

    public void save() {
        coreSession.save();
    }

    @Override
    public void setDefaultContext(OperationContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    @Override
    public OperationContext getDefaultContext() {
        return defaultContext;
    }

    @Override
    public Map<String, String> getSessionParameters() {
        return Collections.emptyMap();
    }

    protected String getRepositoryId() {
        return coreSession.getRepositoryName();
    }

    @Override
    public ObjectId createObjectId(String id) {
        return new ObjectIdImpl(id);
    }

    @Override
    public ObjectId createDocument(Map<String, ?> properties, ObjectId folderId, ContentStream contentStream,
            VersioningState versioningState) {
        return createDocument(properties, folderId, contentStream, versioningState, null, null, null);
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
        return objectFactory.convertProperties(properties, type, null, null);
    }

    @Override
    public ObjectId createDocument(Map<String, ?> properties, ObjectId folderId, ContentStream contentStream,
            VersioningState versioningState, List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        String id = service.createDocument(repositoryId, convertProperties(properties), folderId == null ? null
                : folderId.getId(), contentStream, versioningState, objectFactory.convertPolicies(policies),
                objectFactory.convertAces(addAces), objectFactory.convertAces(removeAces), null);
        return createObjectId(id);
    }

    @Override
    public ObjectId createFolder(Map<String, ?> properties, ObjectId folderId) {
        return createFolder(properties, folderId, null, null, null);
    }

    @Override
    public ObjectId createFolder(Map<String, ?> properties, ObjectId folderId, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        String id = service.createFolder(repositoryId, convertProperties(properties), folderId == null ? null
                : folderId.getId(), objectFactory.convertPolicies(policies), objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), null);
        return createObjectId(id);
    }

    @Override
    public OperationContext createOperationContext() {
        return new OperationContextImpl();
    }

    @Override
    public OperationContext createOperationContext(Set<String> filter, boolean includeAcls,
            boolean includeAllowableActions, boolean includePolicies, IncludeRelationships includeRelationships,
            Set<String> renditionFilter, boolean includePathSegments, String orderBy, boolean cacheEnabled,
            int maxItemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId createPolicy(Map<String, ?> properties, ObjectId folderId) {
        return createPolicy(properties, folderId, null, null, null);
    }

    @Override
    public ObjectId createPolicy(Map<String, ?> properties, ObjectId folderId, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId createRelationship(Map<String, ?> properties) {
        return createRelationship(properties, null, null, null);
    }

    @Override
    public ObjectId createRelationship(Map<String, ?> properties, List<Policy> policies, List<Ace> addAces,
            List<Ace> removeAces) {
        String id = service.createRelationship(repositoryId, convertProperties(properties),
                objectFactory.convertPolicies(policies), objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), null);
        return createObjectId(id);
    }

    @Override
    public ObjectId createItem(Map<String, ?> properties, ObjectId folderId, List<Policy> policies, List<Ace> addAces,
            List<Ace> removeAces) {
        throw new CmisNotSupportedException();
    }

    @Override
    public ObjectId createItem(Map<String, ?> properties, ObjectId folderId) {
        throw new CmisNotSupportedException();
    }

    @Override
    public ObjectId createPath(String newPath, Map<String, ?> properties) {
        return createPath(null, newPath, properties);
    }

    @Override
    public ObjectId createPath(ObjectId startFolderId, String newPath, Map<String, ?> properties) {
        return createPath(startFolderId, newPath, properties, null, null, null);
    }

    @Override
    public ObjectId createPath(String newPath, String typeId) {
        return createPath(null, newPath, typeId);
    }

    @Override
    public ObjectId createPath(ObjectId startFolderId, String newPath, String typeId) {
        Map<String, Object> properties = Collections.singletonMap(PropertyIds.OBJECT_TYPE_ID, typeId);
        return createPath(startFolderId, newPath, properties, null, null, null);
    }

    @Override
    public ObjectId createPath(ObjectId startFolderId, String newPath, Map<String, ?> properties, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        checkPath(newPath);
        if (newPath.length() == 1) {
            throw new CmisInvalidArgumentException("Cannot create root folder");
        }
        if (newPath.endsWith("/")) {
            throw new CmisInvalidArgumentException("Path cannot end with a slash");
        }
        if (properties == null || properties.isEmpty()) {
            throw new CmisInvalidArgumentException("Properties must not be empty");
        }
        if (!(properties.get(PropertyIds.OBJECT_TYPE_ID) instanceof String)) {
            throw new CmisInvalidArgumentException("Property cmis:objectTypeId not set or invalid");
        }
        StringBuilder nextPath = new StringBuilder();
        String[] segments;
        ObjectId lastFolderId = null;
        boolean create = false;

        // check start folder
        if (startFolderId != null && startFolderId.getId() != null) {
            if (startFolderId instanceof Folder) {
                Folder startFolder = (Folder) startFolderId;
                if (!startFolder.isRootFolder()) {
                    nextPath.append(startFolder.getPath());
                    lastFolderId = startFolder;
                }
            } else {
                String filter = PropertyIds.OBJECT_ID + ',' + PropertyIds.BASE_TYPE_ID + ',' + PropertyIds.PATH;
                ObjectData startFolderData = service.getObject(repositoryId, startFolderId.getId(), filter,
                        Boolean.FALSE, IncludeRelationships.NONE, RENDITION_NONE, Boolean.FALSE, Boolean.FALSE, null);
                if (startFolderData.getBaseTypeId() != BaseTypeId.CMIS_FOLDER) {
                    throw new CmisInvalidArgumentException("Start folder is not a folder");
                }
                if (startFolderData.getProperties() == null || startFolderData.getProperties().getProperties() == null
                        || startFolderData.getProperties().getProperties().get(PropertyIds.PATH) == null) {
                    throw new CmisInvalidArgumentException("Start folder has no path property");
                }
                String startPath = (String) startFolderData.getProperties()
                                                           .getProperties()
                                                           .get(PropertyIds.PATH)
                                                           .getFirstValue();
                if (!getRepositoryInfo().getRootFolderId().equals(startFolderData.getId())) {
                    nextPath.append(startPath);
                    lastFolderId = startFolderId;
                }
            }
            if (!newPath.startsWith(nextPath.toString())) {
                throw new CmisInvalidArgumentException("Start folder in not in the path");
            }
            segments = newPath.substring(nextPath.length()).split("/");
        } else {
            segments = newPath.split("/");
        }

        // create folders
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            if (create) {
                lastFolderId = createFolder(propertiesWithName(properties, segment), lastFolderId, policies, addAces,
                        removeAces);
            } else {
                try {
                    nextPath.append('/');
                    nextPath.append(segment);
                    String filter = PropertyIds.OBJECT_ID + ',' + PropertyIds.BASE_TYPE_ID;
                    ObjectData folderData = service.getObjectByPath(repositoryId, nextPath.toString(), filter,
                            Boolean.FALSE, IncludeRelationships.NONE, RENDITION_NONE, Boolean.FALSE, Boolean.FALSE,
                            null);
                    if (folderData.getBaseTypeId() != BaseTypeId.CMIS_FOLDER) {
                        throw new CmisConstraintException("Cannot create folder " + segment
                                + " because there is already an object with this name which is not a folder");
                    }
                    lastFolderId = new ObjectIdImpl(folderData.getId());
                } catch (CmisObjectNotFoundException e) {
                    if (lastFolderId == null) {
                        lastFolderId = new ObjectIdImpl(getRepositoryInfo().getRootFolderId());
                    }
                    lastFolderId = createFolder(propertiesWithName(properties, segment), lastFolderId, policies,
                            addAces, removeAces);
                    create = true;
                }
            }
        }
        return lastFolderId;
    }

    protected Map<String, Object> propertiesWithName(Map<String, ?> properties, String name) {
        Map<String, Object> newProperties = new HashMap<>(properties);
        newProperties.put(PropertyIds.NAME, name);
        return newProperties;
    }

    @Override
    public ObjectId createDocumentFromSource(ObjectId source, Map<String, ?> properties, ObjectId folderId,
            VersioningState versioningState) {
        return createDocumentFromSource(source, properties, folderId, versioningState, null, null, null);
    }

    @Override
    public ObjectId createDocumentFromSource(ObjectId source, Map<String, ?> properties, ObjectId folderId,
            VersioningState versioningState, List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
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
    public ChangeEvents getContentChanges(String changeLogToken, boolean includeProperties, long maxNumItems) {
        return getContentChanges(changeLogToken, includeProperties, maxNumItems, getDefaultContext());
    }

    @Override
    public ChangeEvents getContentChanges(String changeLogToken, boolean includeProperties, long maxNumItems,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<ChangeEvent> getContentChanges(String changeLogToken, boolean includeProperties) {
        return getContentChanges(changeLogToken, includeProperties, getDefaultContext());
    };

    @Override
    public ItemIterable<ChangeEvent> getContentChanges(String changeLogToken, boolean includeProperties,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    };

    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(ObjectId objectId) {
        return exists(objectId.getId());
    }

    @Override
    public boolean exists(String objectId) {
        try {
            service.getObject(repositoryId, objectId, PropertyIds.OBJECT_ID, Boolean.FALSE, IncludeRelationships.NONE,
                    RENDITION_NONE, Boolean.FALSE, Boolean.FALSE, null);
            return true;
        } catch (CmisObjectNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean existsPath(String parentPath, String name) {
        if (parentPath == null || !parentPath.startsWith("/")) {
            throw new CmisInvalidArgumentException("Invalid parent path: " + parentPath);
        }
        if (StringUtils.isEmpty(name)) {
            throw new CmisInvalidArgumentException("Invalid empty name: " + name);
        }
        StringBuilder path = new StringBuilder(parentPath);
        if (!parentPath.endsWith("/")) {
            path.append('/');
        }
        path.append(name);
        return existsPath(path.toString());
    }

    @Override
    public boolean existsPath(String path) {
        try {
            service.getObjectByPath(repositoryId, path, PropertyIds.OBJECT_ID, Boolean.FALSE, IncludeRelationships.NONE,
                    RENDITION_NONE, Boolean.FALSE, Boolean.FALSE, null);
            return true;
        } catch (CmisObjectNotFoundException e) {
            return false;
        }
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
        ObjectData data = service.getObject(repositoryId, objectId, context.getFilterString(),
                Boolean.valueOf(context.isIncludeAllowableActions()), context.getIncludeRelationships(),
                context.getRenditionFilterString(), Boolean.valueOf(context.isIncludePolicies()),
                Boolean.valueOf(context.isIncludeAcls()), null);
        return objectFactory.convertObject(data, context);
    }

    @Override
    public CmisObject getObjectByPath(String path) {
        return getObjectByPath(path, getDefaultContext());
    }

    @Override
    public CmisObject getObjectByPath(String parentPath, String name) {
        return getObjectByPath(parentPath, name, getDefaultContext());
    }

    @Override
    public CmisObject getObjectByPath(String parentPath, String name, OperationContext context) {
        if (parentPath == null || !parentPath.startsWith("/")) {
            throw new CmisInvalidArgumentException("Invalid parent path: " + parentPath);
        }
        if (StringUtils.isEmpty(name)) {
            throw new CmisInvalidArgumentException("Invalid empty name: " + name);
        }
        StringBuilder path = new StringBuilder(parentPath);
        if (!parentPath.endsWith("/")) {
            path.append('/');
        }
        path.append(name);
        return getObjectByPath(path.toString(), context);
    }

    @Override
    public CmisObject getObjectByPath(String path, OperationContext context) {
        if (path == null || !path.startsWith("/")) {
            throw new CmisInvalidArgumentException("Invalid path: " + path);
        }
        if (context == null) {
            throw new CmisInvalidArgumentException("Missing operation context");
        }
        ObjectData data = service.getObjectByPath(repositoryId, path, context.getFilterString(),
                Boolean.valueOf(context.isIncludeAllowableActions()), context.getIncludeRelationships(),
                context.getRenditionFilterString(), Boolean.valueOf(context.isIncludePolicies()),
                Boolean.valueOf(context.isIncludeAcls()), null);
        return getObjectFactory().convertObject(data, context);
    }

    protected String getObjectIdByPath(String path) {
        return service.getObjectByPath(repositoryId, path, PropertyIds.OBJECT_ID, Boolean.FALSE,
                IncludeRelationships.NONE, RENDITION_NONE, Boolean.FALSE, Boolean.FALSE, null).getId();
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
            throw new CmisRuntimeException("Root object is not a Folder but: " + folder.getClass().getName());
        }
        return (Folder) folder;
    }

    @Override
    public ItemIterable<ObjectType> getTypeChildren(String typeId, boolean includePropertyDefinitions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectType getTypeDefinition(String typeId) {
        TypeDefinition typeDefinition = service.getTypeDefinition(repositoryId, typeId, null);
        return objectFactory.convertTypeDefinition(typeDefinition);
    }

    @Override
    public ObjectType getTypeDefinition(String typeId, boolean useCache) {
        return getTypeDefinition(typeId);
    }

    @Override
    public List<Tree<ObjectType>> getTypeDescendants(String typeId, int depth, boolean includePropertyDefinitions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<QueryResult> query(String statement, boolean searchAllVersions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<QueryResult> query(final String statement, final boolean searchAllVersions,
            final OperationContext context) {
        AbstractPageFetcher<QueryResult> pageFetcher = new AbstractPageFetcher<QueryResult>(
                context.getMaxItemsPerPage()) {
            @Override
            protected Page<QueryResult> fetchPage(long skipCount) {
                ObjectList results = service.query(repositoryId, statement, Boolean.valueOf(searchAllVersions),
                        Boolean.valueOf(context.isIncludeAllowableActions()), context.getIncludeRelationships(),
                        context.getRenditionFilterString(), BigInteger.valueOf(maxNumItems),
                        BigInteger.valueOf(skipCount), null);
                // convert objects
                List<QueryResult> page = new ArrayList<QueryResult>();
                if (results.getObjects() != null) {
                    for (ObjectData data : results.getObjects()) {
                        page.add(new QueryResultImpl(NuxeoSession.this, data));
                    }
                }
                return new Page<QueryResult>(page, results.getNumItems(), results.hasMoreItems());
            }
        };
        return new CollectionIterable<QueryResult>(pageFetcher);
    }

    @Override
    public ItemIterable<CmisObject> queryObjects(String typeId, String where, boolean searchAllVersions,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryStatement createQueryStatement(String statement) {
        return new QueryStatementImpl(this, statement);
    }

    @Override
    public QueryStatement createQueryStatement(Collection<String> selectPropertyIds, Map<String, String> fromTypes,
            String whereClause, List<String> orderByPropertyIds) {
        return new QueryStatementImpl(this, selectPropertyIds, fromTypes, whereClause, orderByPropertyIds);
    }

    @Override
    public ItemIterable<Relationship> getRelationships(final ObjectId objectId,
            final boolean includeSubRelationshipTypes, final RelationshipDirection relationshipDirection,
            final ObjectType type, final OperationContext context) {
        final String typeId = type == null ? null : type.getId();
        AbstractPageFetcher<Relationship> pageFetcher = new AbstractPageFetcher<Relationship>(
                context.getMaxItemsPerPage()) {
            @Override
            protected Page<Relationship> fetchPage(long skipCount) {
                ObjectList relations = service.getObjectRelationships(repositoryId, objectId.getId(),
                        Boolean.valueOf(includeSubRelationshipTypes), relationshipDirection, typeId, null, null,
                        BigInteger.valueOf(maxNumItems), BigInteger.valueOf(skipCount), null);
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
                return new Page<Relationship>(page, relations.getNumItems(), relations.hasMoreItems());
            }
        };
        return new CollectionIterable<Relationship>(pageFetcher);
    }

    @Override
    public Acl getAcl(ObjectId objectId, boolean onlyBasicPermissions) {
        return service.getAcl(repositoryId, objectId.getId(), Boolean.valueOf(onlyBasicPermissions), null);
    }

    @Override
    public Acl setAcl(ObjectId objectId, List<Ace> aces) {
        return service.applyAcl(repositoryId, objectId.getId(), new AccessControlListImpl(aces), null);
    }

    @Override
    public Acl applyAcl(ObjectId objectId, List<Ace> addAces, List<Ace> removeAces, AclPropagation aclPropagation) {
        return service.applyAcl(repositoryId, objectId.getId(), new AccessControlListImpl(addAces),
                new AccessControlListImpl(removeAces), aclPropagation, null);
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

    @Override
    public void delete(ObjectId objectId) {
        delete(objectId, true);
    }

    @Override
    public void delete(ObjectId objectId, boolean allVersions) {
        service.deleteObject(repositoryId, objectId.getId(), Boolean.valueOf(allVersions), null);
    }

    @Override
    public void deleteByPath(String path) {
        deleteByPath(path, true);
    }

    @Override
    public void deleteByPath(String parentPath, String name) {
        deleteByPath(buildPath(parentPath, name), true);
    }

    @Override
    public void deleteByPath(String path, boolean allVersions) {
        checkPath(path);
        delete(new ObjectIdImpl(getObjectIdByPath(path)), allVersions);
    }

    @Override
    public List<String> deleteTree(ObjectId folderId, boolean allVersions, UnfileObject unfile,
            boolean continueOnFailure) {
        FailedToDeleteData res = service.deleteTree(repositoryId, folderId.getId(), Boolean.valueOf(allVersions),
                unfile, Boolean.valueOf(continueOnFailure), null);
        return res.getIds();
    }

    @Override
    public List<String> deleteTreebyPath(String parentPath, String name, boolean allVersions, UnfileObject unfile,
            boolean continueOnFailure) {
        return deleteTreebyPath(buildPath(parentPath, name), allVersions, unfile, continueOnFailure);
    }

    @Override
    public List<String> deleteTreebyPath(String path, boolean allVersions, UnfileObject unfile,
            boolean continueOnFailure) {
        checkPath(path);
        return deleteTree(new ObjectIdImpl(getObjectIdByPath(path)), allVersions, unfile, continueOnFailure);
    }

    /** Checks that the path is valid. */
    protected final void checkPath(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new CmisInvalidArgumentException("Missing path");
        }
        if (!path.startsWith("/")) {
            throw new CmisInvalidArgumentException("Path must start with a slash");
        }
    }

    /** Checks that the parent path and name are valid, and builds a full path from them. */
    protected String buildPath(String parentPath, String name) {
        checkPath(parentPath);
        if (StringUtils.isEmpty(name)) {
            throw new CmisInvalidArgumentException("Missing name");
        }
        if (name.startsWith("/")) {
            throw new CmisInvalidArgumentException("Name must not start with a slash");
        }
        StringBuilder path = new StringBuilder(parentPath.length() + name.length() + 1);
        path.append(parentPath);
        if (!parentPath.endsWith("/")) {
            path.append('/');
        }
        path.append(name);
        return path.toString();
    }

    @Override
    public ContentStream getContentStream(ObjectId docId) {
        return getContentStream(docId, null, null, null);
    }

    @Override
    public ContentStream getContentStream(ObjectId docId, String streamId, BigInteger offset, BigInteger length) {
        if (docId == null) {
            throw new CmisInvalidArgumentException("Missing object ID");
        }
        return service.getContentStream(repositoryId, docId.getId(), streamId, offset, length, null);
    }

    @Override
    public ContentStream getContentStreamByPath(String path) {
        return getContentStreamByPath(path, null, null, null);
    }

    @Override
    public ContentStream getContentStreamByPath(String path, String streamId, BigInteger offset, BigInteger length) {
        checkPath(path);
        return service.getContentStream(repositoryId, getObjectIdByPath(path), streamId, offset, length, null);
    }

    @Override
    public ObjectType createType(TypeDefinition type) {
        throw new CmisNotSupportedException();
    }

    @Override
    public ObjectType updateType(TypeDefinition type) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void deleteType(String typeId) {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(List<CmisObject> objects,
            Map<String, ?> properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds) {
        List<BulkUpdateObjectIdAndChangeToken> idts = new ArrayList<BulkUpdateObjectIdAndChangeToken>(objects.size());
        for (CmisObject object : objects) {
            idts.add(new BulkUpdateObjectIdAndChangeTokenImpl(object.getId(), object.getChangeToken()));
        }
        return service.bulkUpdateProperties(repositoryId, idts, convertProperties(properties), addSecondaryTypeIds,
                removeSecondaryTypeIds, null);
    }

    @Override
    public Document getLatestDocumentVersion(ObjectId objectId) {
        return getLatestDocumentVersion(objectId, false, getDefaultContext());
    }

    @Override
    public Document getLatestDocumentVersion(String objectId, OperationContext context) {
        if (objectId == null) {
            throw new IllegalArgumentException("Object ID must be set!");
        }
        return getLatestDocumentVersion(createObjectId(objectId), false, context);
    }

    @Override
    public Document getLatestDocumentVersion(String objectId, boolean major, OperationContext context) {
        if (objectId == null) {
            throw new IllegalArgumentException("Object ID must be set!");
        }
        return getLatestDocumentVersion(createObjectId(objectId), major, context);
    }

    @Override
    public Document getLatestDocumentVersion(String objectId) {
        if (objectId == null) {
            throw new IllegalArgumentException("Object ID must be set!");
        }
        return getLatestDocumentVersion(createObjectId(objectId), false, getDefaultContext());
    }

    @Override
    public Document getLatestDocumentVersion(ObjectId objectId, OperationContext context) {
        return getLatestDocumentVersion(objectId, false, context);
    }

    @Override
    /**
     * @See org.apache.chemistry.opencmis.client.runtime.SessionImpl
     */
    public Document getLatestDocumentVersion(ObjectId objectId, boolean major, OperationContext context) {
        if (objectId == null || objectId.getId() == null) {
            throw new IllegalArgumentException("Object ID must be set!");
        }

        if (context == null) {
            throw new IllegalArgumentException("Operation context must be set!");
        }

        CmisObject result = null;

        String versionSeriesId = null;

        // first attempt: if we got a Document object, try getting the version
        // series ID from it
        if (objectId instanceof Document) {
            versionSeriesId = ((Document) objectId).getVersionSeriesId();
        }

        // (the AtomPub and Browser binding don't need the version series ID ->
        // avoid roundtrip)

        // get the object
        ObjectData objectData = binding.getVersioningService().getObjectOfLatestVersion(getRepositoryId(),
                objectId.getId(), versionSeriesId, major, context.getFilterString(),
                context.isIncludeAllowableActions(), context.getIncludeRelationships(),
                context.getRenditionFilterString(), context.isIncludePolicies(), context.isIncludeAcls(), null);

        result = getObjectFactory().convertObject(objectData, context);

        // check result
        if (!(result instanceof Document)) {
            throw new IllegalArgumentException("Latest version is not a document!");
        }

        return (Document) result;
    }

    @Override
    public String getLatestChangeLogToken() {
        return getBinding().getRepositoryService().getRepositoryInfo(getRepositoryId(), null).getLatestChangeLogToken();
    }

}
