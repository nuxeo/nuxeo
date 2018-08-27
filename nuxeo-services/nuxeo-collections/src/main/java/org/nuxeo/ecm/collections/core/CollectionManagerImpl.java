/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionLocationService;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.collections.core.listener.CollectionAsynchrnonousQuery;
import org.nuxeo.ecm.collections.core.worker.DuplicateCollectionMemberWork;
import org.nuxeo.ecm.collections.core.worker.RemoveFromCollectionWork;
import org.nuxeo.ecm.collections.core.worker.RemovedAbstractWork;
import org.nuxeo.ecm.collections.core.worker.RemovedCollectionMemberWork;
import org.nuxeo.ecm.collections.core.worker.RemovedCollectionWork;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.9.3
 */
public class CollectionManagerImpl extends DefaultComponent implements CollectionManager {

    private static final String PERMISSION_ERROR_MESSAGE = "Privilege '%s' is not granted to '%s'";

    public static void disableEvents(final DocumentModel doc) {
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        doc.putContextData(CollectionConstants.DISABLE_NOTIFICATION_SERVICE, true);
        doc.putContextData(CollectionConstants.DISABLE_AUDIT_LOGGER, true);
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, true);
    }

    @Override
    public void addToCollection(final DocumentModel collection, final DocumentModel documentToBeAdded,
            final CoreSession session) throws DocumentSecurityException {
        checkCanAddToCollection(collection, documentToBeAdded, session);
        final Map<String, Serializable> props = new HashMap<>();
        props.put(CollectionConstants.COLLECTION_REF_EVENT_CTX_PROP, collection.getRef());
        fireEvent(documentToBeAdded, session, CollectionConstants.BEFORE_ADDED_TO_COLLECTION, props);
        Collection colAdapter = collection.getAdapter(Collection.class);
        colAdapter.addDocument(documentToBeAdded.getId());
        collection.getCoreSession().saveDocument(colAdapter.getDocument());

        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {

                documentToBeAdded.addFacet(CollectionConstants.COLLECTABLE_FACET);

                // We want to disable the following listener on a
                // collection member when it is added to a collection
                disableEvents(documentToBeAdded);

                CollectionMember docAdapter = documentToBeAdded.getAdapter(CollectionMember.class);
                docAdapter.addToCollection(collection.getId());
                DocumentModel addedDoc = session.saveDocument(docAdapter.getDocument());
                fireEvent(addedDoc, session, CollectionConstants.ADDED_TO_COLLECTION, props);
            }

        }.runUnrestricted();
    }

    @Override
    public void addToCollection(final DocumentModel collection, final List<DocumentModel> documentListToBeAdded,
            final CoreSession session) {
        for (DocumentModel documentToBeAdded : documentListToBeAdded) {
            addToCollection(collection, documentToBeAdded, session);
        }
    }

    @Override
    public void addToNewCollection(final String newTitle, final String newDescription,
            final DocumentModel documentToBeAdded, final CoreSession session) {
        addToCollection(createCollection(newTitle, newDescription, documentToBeAdded, session), documentToBeAdded,
                session);
    }

    @Override
    public void addToNewCollection(final String newTitle, final String newDescription,
            final List<DocumentModel> documentListToBeAdded, CoreSession session) {
        DocumentModel newCollection = createCollection(newTitle, newDescription, documentListToBeAdded.get(0), session);
        for (DocumentModel documentToBeAdded : documentListToBeAdded) {
            addToCollection(newCollection, documentToBeAdded, session);
        }
    }

    @Override
    public boolean canAddToCollection(final DocumentModel collection, final CoreSession session) {
        return isCollection(collection)
                && session.hasPermission(collection.getRef(), SecurityConstants.WRITE_PROPERTIES);
    }

    @Override
    public boolean canManage(final DocumentModel collection, final CoreSession session) {
        return isCollection(collection) && session.hasPermission(collection.getRef(), SecurityConstants.EVERYTHING);
    }

    public void checkCanAddToCollection(final DocumentModel collection, final DocumentModel documentToBeAdded,
            final CoreSession session) {
        if (!isCollectable(documentToBeAdded)) {
            throw new IllegalArgumentException(
                    String.format("Document %s is not collectable", documentToBeAdded.getTitle()));
        }
        checkCanCollectInCollection(collection, session);
    }

    /**
     * @since 8.4
     */
    protected void checkCanCollectInCollection(final DocumentModel collection, final CoreSession session) {
        if (!isCollection(collection)) {
            throw new IllegalArgumentException(String.format("Document %s is not a collection", collection.getTitle()));
        }
        if (!session.hasPermission(collection.getRef(), SecurityConstants.WRITE_PROPERTIES)) {
            throw new DocumentSecurityException(String.format(PERMISSION_ERROR_MESSAGE,
                    CollectionConstants.CAN_COLLECT_PERMISSION, session.getPrincipal().getName()));
        }
    }

    protected DocumentModel createCollection(final String newTitle, final String newDescription,
            final DocumentModel context, final CoreSession session) {
        DocumentModel defaultCollections = getUserDefaultCollections(session);
        Map<String, Object> options = new HashMap<>();
        options.put(CoreEventConstants.PARENT_PATH, defaultCollections.getPath().toString());
        options.put(CoreEventConstants.DESTINATION_NAME, newTitle);
        options.put(CoreEventConstants.DESTINATION_NAME, newTitle);
        DocumentModel newCollection = session.createDocumentModel(CollectionConstants.COLLECTION_TYPE, options);

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        newCollection.setPathInfo(defaultCollections.getPath().toString(), pss.generatePathSegment(newTitle));
        newCollection.setPropertyValue("dc:title", newTitle);
        newCollection.setPropertyValue("dc:description", newDescription);
        return session.createDocument(newCollection);
    }

    @Override
    @Deprecated
    public DocumentModel getUserDefaultCollections(final DocumentModel context, final CoreSession session) {
        return getUserDefaultCollections(session);
    }

    @Override
    public DocumentModel getUserDefaultCollections(final CoreSession session) {
        return Framework.getService(CollectionLocationService.class)
                                                            .getUserDefaultCollectionsRoot(session);
    }

    @Override
    public List<DocumentModel> getVisibleCollection(final DocumentModel collectionMember, final CoreSession session) {
        return getVisibleCollection(collectionMember, CollectionConstants.MAX_COLLECTION_RETURNED, session);
    }

    @Override
    public List<DocumentModel> getVisibleCollection(final DocumentModel collectionMember, int maxResult,
            CoreSession session) {
        List<DocumentModel> result = new ArrayList<>();
        if (isCollected(collectionMember)) {
            CollectionMember collectionMemberAdapter = collectionMember.getAdapter(CollectionMember.class);
            List<String> collectionIds = collectionMemberAdapter.getCollectionIds();
            for (int i = 0; i < collectionIds.size() && result.size() < maxResult; i++) {
                final String collectionId = collectionIds.get(i);
                DocumentRef documentRef = new IdRef(collectionId);
                if (session.exists(documentRef) && session.hasPermission(documentRef, SecurityConstants.READ)) {
                    DocumentModel collection = session.getDocument(documentRef);
                    if (!collection.isTrashed() && !collection.isVersion()) {
                        result.add(collection);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasVisibleCollection(final DocumentModel collectionMember, CoreSession session) {
        CollectionMember collectionMemberAdapter = collectionMember.getAdapter(CollectionMember.class);
        List<String> collectionIds = collectionMemberAdapter.getCollectionIds();
        for (final String collectionId : collectionIds) {
            DocumentRef documentRef = new IdRef(collectionId);
            if (session.exists(documentRef) && session.hasPermission(documentRef, SecurityConstants.READ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCollectable(final DocumentModel doc) {
        return !doc.hasFacet(CollectionConstants.NOT_COLLECTABLE_FACET);
    }

    @Override
    public boolean isCollected(final DocumentModel doc) {
        return doc.hasFacet(CollectionConstants.COLLECTABLE_FACET);
    }

    @Override
    public boolean isCollection(final DocumentModel doc) {
        return doc.hasFacet(CollectionConstants.COLLECTION_FACET);
    }

    @Override
    public boolean isInCollection(DocumentModel collection, DocumentModel document, CoreSession session) {
        if (isCollected(document)) {
            final CollectionMember collectionMemberAdapter = document.getAdapter(CollectionMember.class);
            return collectionMemberAdapter.getCollectionIds().contains(collection.getId());
        }
        return false;
    }

    @Override
    public void processCopiedCollection(final DocumentModel collection) {
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        List<String> documentIds = collectionAdapter.getCollectedDocumentIds();

        int i = 0;
        while (i < documentIds.size()) {
            int limit = (int) (((i + CollectionAsynchrnonousQuery.MAX_RESULT) > documentIds.size()) ? documentIds.size()
                    : (i + CollectionAsynchrnonousQuery.MAX_RESULT));
            DuplicateCollectionMemberWork work = new DuplicateCollectionMemberWork(collection.getRepositoryName(),
                    collection.getId(), documentIds.subList(i, limit), i);
            WorkManager workManager = Framework.getService(WorkManager.class);
            workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);

            i = limit;
        }
    }

    @Override
    public void processRemovedCollection(final DocumentModel collection) {
        final WorkManager workManager = Framework.getService(WorkManager.class);
        final RemovedAbstractWork work = new RemovedCollectionWork();
        work.setDocument(collection.getRepositoryName(), collection.getId());
        workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
    }

    @Override
    public void processRemovedCollectionMember(final DocumentModel collectionMember) {
        final WorkManager workManager = Framework.getService(WorkManager.class);
        final RemovedAbstractWork work = new RemovedCollectionMemberWork();
        work.setDocument(collectionMember.getRepositoryName(), collectionMember.getId());
        workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
    }

    @Override
    public void processRestoredCollection(DocumentModel collection, DocumentModel version) {
        final Set<String> collectionMemberIdsToBeRemoved = new TreeSet<>(
                collection.getAdapter(Collection.class).getCollectedDocumentIds());
        collectionMemberIdsToBeRemoved.removeAll(version.getAdapter(Collection.class).getCollectedDocumentIds());

        final Set<String> collectionMemberIdsToBeAdded = new TreeSet<>(
                version.getAdapter(Collection.class).getCollectedDocumentIds());
        collectionMemberIdsToBeAdded.removeAll(collection.getAdapter(Collection.class).getCollectedDocumentIds());

        int i = 0;
        while (i < collectionMemberIdsToBeRemoved.size()) {
            int limit = (int) (((i + CollectionAsynchrnonousQuery.MAX_RESULT) > collectionMemberIdsToBeRemoved.size())
                    ? collectionMemberIdsToBeRemoved.size() : (i + CollectionAsynchrnonousQuery.MAX_RESULT));
            RemoveFromCollectionWork work = new RemoveFromCollectionWork(collection.getRepositoryName(),
                                                                         collection.getId(), new ArrayList<>(collectionMemberIdsToBeRemoved).subList(i, limit), i);
            WorkManager workManager = Framework.getService(WorkManager.class);
            workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);

            i = limit;
        }
        i = 0;
        while (i < collectionMemberIdsToBeAdded.size()) {
            int limit = (int) (((i + CollectionAsynchrnonousQuery.MAX_RESULT) > collectionMemberIdsToBeAdded.size())
                    ? collectionMemberIdsToBeAdded.size() : (i + CollectionAsynchrnonousQuery.MAX_RESULT));
            DuplicateCollectionMemberWork work = new DuplicateCollectionMemberWork(collection.getRepositoryName(),
                                                                                   collection.getId(), new ArrayList<>(collectionMemberIdsToBeAdded).subList(i, limit), i);
            WorkManager workManager = Framework.getService(WorkManager.class);
            workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);

            i = limit;
        }
    }

    @Override
    public void removeAllFromCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeRemoved, final CoreSession session) {
        for (DocumentModel documentToBeRemoved : documentListToBeRemoved) {
            removeFromCollection(collection, documentToBeRemoved, session);
        }
    }

    @Override
    public void removeFromCollection(final DocumentModel collection, final DocumentModel documentToBeRemoved,
            final CoreSession session) {
        checkCanAddToCollection(collection, documentToBeRemoved, session);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CollectionConstants.COLLECTION_REF_EVENT_CTX_PROP, new IdRef(collection.getId()));
        fireEvent(documentToBeRemoved, session, CollectionConstants.BEFORE_REMOVED_FROM_COLLECTION, props);
        Collection colAdapter = collection.getAdapter(Collection.class);
        colAdapter.removeDocument(documentToBeRemoved.getId());
        collection.getCoreSession().saveDocument(colAdapter.getDocument());

        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() {
                doRemoveFromCollection(documentToBeRemoved, collection.getId(), session);
            }

        }.runUnrestricted();
    }

    @Override
    public void doRemoveFromCollection(DocumentModel documentToBeRemoved, String collectionId, CoreSession session) {
        // We want to disable the following listener on a
        // collection member when it is removed from a collection
        disableEvents(documentToBeRemoved);

        CollectionMember docAdapter = documentToBeRemoved.getAdapter(CollectionMember.class);
        docAdapter.removeFromCollection(collectionId);
        DocumentModel removedDoc = session.saveDocument(docAdapter.getDocument());
        Map<String, Serializable> props = new HashMap<>();
        props.put(CollectionConstants.COLLECTION_REF_EVENT_CTX_PROP, new IdRef(collectionId));
        fireEvent(removedDoc, session, CollectionConstants.REMOVED_FROM_COLLECTION, props);
    }

    @Override
    public DocumentModel createCollection(final CoreSession session, String title, String description, String path) {
        DocumentModel newCollection;
        // Test if the path is null or empty
        if (StringUtils.isEmpty(path)) {
            // A default collection is created with the given name
            newCollection = createCollection(title, description, null, session);
        } else {
            // If the path does not exist, an exception is thrown
            if (!session.exists(new PathRef(path))) {
                throw new NuxeoException(String.format("Path \"%s\" specified in parameter not found", path));
            }
            // Create a new collection in the given path
            DocumentModel collectionModel = session.createDocumentModel(path, title,
                    CollectionConstants.COLLECTION_TYPE);
            collectionModel.setPropertyValue("dc:title", title);
            collectionModel.setPropertyValue("dc:description", description);
            newCollection = session.createDocument(collectionModel);
        }
        return newCollection;
    }

    protected void fireEvent(DocumentModel doc, CoreSession session, String eventName,
            Map<String, Serializable> props) {
        EventService eventService = Framework.getService(EventService.class);
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, session.getSessionId());
        ctx.setProperty("category", DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        ctx.setProperties(props);
        Event event = ctx.newEvent(eventName);
        eventService.fireEvent(event);
    }

    @Override
    public boolean moveMembers(final CoreSession session, final DocumentModel collection, final DocumentModel member1,
            final DocumentModel member2) {
        checkCanCollectInCollection(collection, session);
        Collection collectionAdapter = collection.getAdapter(Collection.class);
        boolean result = collectionAdapter.moveMembers(member1.getId(), member2 != null ? member2.getId() : null);
        if (result) {
            session.saveDocument(collectionAdapter.getDocument());
        }
        return result;
    }

}
