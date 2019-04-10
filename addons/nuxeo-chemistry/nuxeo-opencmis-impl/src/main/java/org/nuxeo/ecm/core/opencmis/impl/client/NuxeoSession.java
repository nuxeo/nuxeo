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
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;

/**
 * Nuxeo Persistent Session, having a direct connection to a Nuxeo CoreSession.
 */
public class NuxeoSession implements Session {

    public static final OperationContext DEFAULT_CONTEXT = new OperationContextImpl(
            null, false, true, false, IncludeRelationships.NONE, null, true,
            null, true, 10);

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

    @Override
    public void clear() {
    }

    @Override
    public void cancel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void close() {
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
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
    public ObjectId createPolicy(Map<String, ?> properties, ObjectId folderId,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
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
    public ItemIterable<ChangeEvent> getContentChanges(String changeLogToken) {
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

    @Override
    public CmisObject getObjectByPath(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public CmisObject getObjectByPath(String path, OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectFactory getObjectFactory() {
        return objectFactory;
    }

    @Override
    public CmisBinding getBinding() {
        return binding;
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return binding.getRepositoryService().getRepositoryInfo(repositoryId,
                null);
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
        TypeDefinition typeDefinition = binding.getRepositoryService().getTypeDefinition(
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

}
