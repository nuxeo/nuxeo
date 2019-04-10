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

import org.apache.chemistry.opencmis.client.api.ChangeEvent;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.PagingIterable;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.commons.api.Ace;
import org.apache.chemistry.opencmis.commons.api.CmisBinding;
import org.apache.chemistry.opencmis.commons.api.ContentStream;
import org.apache.chemistry.opencmis.commons.api.ObjectData;
import org.apache.chemistry.opencmis.commons.api.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.api.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;

/**
 * Nuxeo Persistent Session, having a direct connection to a Nuxeo CoreSession.
 */
public class NuxeoSession implements Session {

    public static final OperationContext DEFAULT_CONTEXT = new OperationContextImpl(
            null, false, true, false, IncludeRelationships.NONE, null, true,
            null, true);

    private final CoreSession coreSession;

    private final String repositoryId;

    private final ObjectFactory objectFactory;

    private final CmisBinding binding;

    private OperationContext defaultContext = DEFAULT_CONTEXT;

    private RepositoryInfo repositoryInfo;

    public NuxeoSession(CoreSession coreSession, NuxeoRepository repository) {
        this.coreSession = coreSession;
        repositoryId = repository.getId();
        objectFactory = new NuxeoObjectFactory(this);

        NuxeoCmisService service = new NuxeoCmisService(coreSession, repository);
        NuxeoService nuxeoService = new NuxeoService(service);
        binding = new NuxeoBinding(nuxeoService);
    }

    protected CoreSession getCoreSession() {
        return coreSession;
    }

    public void clear() {
    }

    public void cancel() {
        throw new UnsupportedOperationException();
    }

    public void save() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void close() {
    }

    public void setDefaultContext(OperationContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    public OperationContext getDefaultContext() {
        return defaultContext;
    }

    protected String getRepositoryId() {
        return coreSession.getRepositoryName();
    }

    public ObjectId createObjectId(String id) {
        return new ObjectIdImpl(id);
    }

    public ObjectId createDocument(Map<String, ?> properties,
            ObjectId folderId, ContentStream contentStream,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createFolder(Map<String, ?> properties, ObjectId folderId,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public OperationContext createOperationContext(Set<String> filter,
            boolean includeAcls, boolean includeAllowableActions,
            boolean includePolicies, IncludeRelationships includeRelationships,
            Set<String> renditionFilter, boolean includePathSegments,
            String orderBy, boolean cacheEnabled) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createPolicy(Map<String, ?> properties, ObjectId folderId,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createRelationship(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, ObjectId folderId,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public PagingIterable<Document> getCheckedOutDocs(int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public PagingIterable<Document> getCheckedOutDocs(OperationContext context,
            int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public PagingIterable<ChangeEvent> getContentChanges(String changeLogToken,
            int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Locale getLocale() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CmisObject getObject(ObjectId objectId) {
        return getObject(objectId, getDefaultContext());
    }

    public CmisObject getObject(ObjectId objectId, OperationContext context) {
        if (objectId == null || objectId.getId() == null) {
            throw new IllegalArgumentException("Missing object or ID");
        }
        if (context == null) {
            throw new IllegalArgumentException("Missing operation context");
        }
        ObjectData objectData = binding.getObjectService().getObject(
                getRepositoryId(), objectId.getId(), context.getFilterString(),
                context.isIncludeAllowableActions(),
                context.getIncludeRelationships(),
                context.getRenditionFilterString(),
                context.isIncludePolicies(), context.isIncludeAcls(), null);
        return objectFactory.convertObject(objectData, context);
    }

    public CmisObject getObjectByPath(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CmisObject getObjectByPath(String path, OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public CmisBinding getBinding() {
        return binding;
    }

    public RepositoryInfo getRepositoryInfo() {
        return binding.getRepositoryService().getRepositoryInfo(repositoryId,
                null);
    }

    public Folder getRootFolder() {
        return getRootFolder(getDefaultContext());
    }

    public Folder getRootFolder(OperationContext context) {
        String id = getRepositoryInfo().getRootFolderId();
        CmisObject folder = getObject(createObjectId(id), context);
        if (!(folder instanceof Folder)) {
            throw new CmisRuntimeException("Root object is not a Folder but: "
                    + folder.getClass().getName());
        }
        return (Folder) folder;
    }

    public PagingIterable<ObjectType> getTypeChildren(String typeId,
            boolean includePropertyDefinitions, int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectType getTypeDefinition(String typeId) {
        TypeDefinition typeDefinition = binding.getRepositoryService().getTypeDefinition(
                getRepositoryId(), typeId, null);
        return objectFactory.convertTypeDefinition(typeDefinition);
    }

    public List<Tree<ObjectType>> getTypeDescendants(String typeId, int depth,
            boolean includePropertyDefinitions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public PagingIterable<QueryResult> query(String statement,
            boolean searchAllVersions, int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public PagingIterable<QueryResult> query(String statement,
            boolean searchAllVersions, OperationContext context,
            int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
