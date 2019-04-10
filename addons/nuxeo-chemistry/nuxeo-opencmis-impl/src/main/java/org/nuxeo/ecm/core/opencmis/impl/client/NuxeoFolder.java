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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Item;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.util.AbstractPageFetcher;
import org.apache.chemistry.opencmis.client.runtime.util.CollectionIterable;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Live local CMIS Folder, which is backed by a Nuxeo folderish document.
 */
public class NuxeoFolder extends NuxeoFileableObject implements Folder {

    public NuxeoFolder(NuxeoSession session, NuxeoObjectData data,
            ObjectType type) {
        super(session, data, type);
    }

    @Override
    public Document createDocument(Map<String, ?> properties,
            ContentStream contentStream, VersioningState versioningState) {
        return createDocument(properties, contentStream, versioningState, null,
                null, null, session.getDefaultContext());
    }

    @Override
    public Document createDocument(Map<String, ?> properties,
            ContentStream contentStream, VersioningState versioningState,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        String id = service.createDocument(getRepositoryId(),
                session.convertProperties(properties), getId(), contentStream,
                versioningState, objectFactory.convertPolicies(policies),
                objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), null);
        // must now refetch doc
        return (Document) session.getObject(new ObjectIdImpl(id), context);
    }

    @Override
    public NuxeoDocument createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, VersioningState versioningState) {
        return createDocumentFromSource(source, properties, versioningState,
                null, null, null, session.getDefaultContext());
    }

    @Override
    public NuxeoDocument createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, VersioningState versioningState,
            List<Policy> policies, List<Ace> addACEs, List<Ace> removeACEs,
            OperationContext context) {
        if (source == null || source.getId() == null) {
            throw new CmisInvalidArgumentException("Invalid source: " + source);
        }
        if (context == null) {
            context = session.getDefaultContext();
        }
        NuxeoObjectData newData = nuxeoCmisService.copy(source.getId(), getId(),
                properties, type, versioningState, policies, addACEs,
                removeACEs, context);
        return (NuxeoDocument) session.getObjectFactory().convertObject(
                newData, context);
    }

    @Override
    public Folder createFolder(Map<String, ?> properties) {
        return createFolder(properties, null, null, null,
                session.getDefaultContext());
    }

    @Override
    public Folder createFolder(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Policy createPolicy(Map<String, ?> properties) {
        return createPolicy(properties, null, null, null,
                session.getDefaultContext());
    }

    @Override
    public Policy createPolicy(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        throw new CmisNotSupportedException();
    }

    @Override
    public Item createItem(Map<String, ?> properties) {
        return createItem(properties, null, null, null,
                session.getDefaultContext());
    }

    @Override
    public Item createItem(Map<String, ?> properties, List<Policy> policies,
            List<Ace> addAces, List<Ace> removeAces, OperationContext context) {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<String> deleteTree(boolean allVersions, UnfileObject unfile,
            boolean continueOnFailure) {
        FailedToDeleteData failed = service.deleteTree(getRepositoryId(),
                getId(), Boolean.valueOf(allVersions), unfile,
                Boolean.valueOf(continueOnFailure), null);
        if (failed == null || failed.getIds() == null
                || failed.getIds().isEmpty()) {
            return null;
        }
        return failed.getIds();
    }

    @Override
    public List<ObjectType> getAllowedChildObjectTypes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs() {
        return getCheckedOutDocs(session.getDefaultContext());
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs(OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<CmisObject> getChildren() {
        return getChildren(session.getDefaultContext());
    }

    @Override
    public ItemIterable<CmisObject> getChildren(final OperationContext context) {
        AbstractPageFetcher<CmisObject> pageFetcher = new AbstractPageFetcher<CmisObject>(
                context.getMaxItemsPerPage()) {
            @Override
            protected Page<CmisObject> fetchPage(long skipCount) {
                List<CmisObject> items = new ArrayList<CmisObject>();
                DocumentModelList children;
                try {
                    children = nuxeoCmisService.getCoreSession().getChildren(
                            data.doc.getRef());
                } catch (ClientException e) {
                    throw new CmisRuntimeException(e.toString(), e);
                }
                long totalItems = 0;
                long skip = skipCount;
                // TODO orderBy
                for (DocumentModel child : children) {
                    if (nuxeoCmisService.isFilteredOut(child)) {
                        continue;
                    }
                    totalItems++;
                    if (skip > 0) {
                        skip--;
                        continue;
                    }
                    if (items.size() > maxNumItems) {
                        continue;
                    }
                    NuxeoObjectData data = new NuxeoObjectData(service, child,
                            context);
                    CmisObject ob = objectFactory.convertObject(data, context);
                    items.add(ob);
                }
                return new Page<CmisObject>(items, totalItems,
                        totalItems > skipCount + items.size());
            }
        };
        return new CollectionIterable<CmisObject>(pageFetcher);
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int depth) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int depth,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder getFolderParent() {
        if (isRootFolder()) {
            return null;
        }
        List<Folder> parents = getParents();
        if (parents == null || parents.isEmpty()) {
            return null;
        }
        return parents.get(0);
    }

    @Override
    public String getParentId() {
        try {
            CoreSession coreSession = data.doc.getCoreSession();
            DocumentModel parent = coreSession.getParentDocument(new IdRef(
                    getId()));
            if (parent == null || nuxeoCmisService.isFilteredOut(parent)) {
                return null;
            }
            return parent.getId();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int depth) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int depth,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
        return data.doc.getPathAsString();
    }

    @Override
    public boolean isRootFolder() {
        return data.doc.getPath().isRoot();
    }

}
