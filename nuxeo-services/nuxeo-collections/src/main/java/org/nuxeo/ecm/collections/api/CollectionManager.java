/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.collections.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.3
 */
public interface CollectionManager {

    /**
     * Add a document to a collection.
     *
     * @param collection the collection
     * @param documentToBeAdded the document to be added
     * @param session the core session
     */
    void addToCollection(final DocumentModel collection, final DocumentModel documentToBeAdded,
            final CoreSession session);

    /**
     * Add a list of document to a collection.
     *
     * @param collection the collection
     * @param documentListToBeAdded the list of document to be added
     * @param session the core session
     */
    void addToCollection(final DocumentModel collection, final List<DocumentModel> documentListToBeAdded,
            final CoreSession session);

    /**
     * Add a document to a new collection.
     *
     * @param newTitle the title of the new collection
     * @param newDescription the description of the new collection
     * @param documentToBeAdded the document to be added
     * @param session the core session
     */
    void addToNewCollection(String newTitle, String newDescription, DocumentModel documentToBeAdded, CoreSession session);

    /**
     * Add a list of document to a new collection.
     *
     * @param newTitle the title of the new collection
     * @param newDescription the description of the new collection
     * @param documentListToBeAdded the list of document to be added
     * @param session the core session
     */
    void addToNewCollection(String newTitle, String newDescription, List<DocumentModel> documentListToBeAdded,
            CoreSession documentManager);

    /**
     * Check that a document is a collection and that the given core session has permission to add document inside.
     *
     * @param collection the collection
     * @param session the core session
     * @return true if the given document is a Collection and the core session has permission to add document inside,
     *         false otherwise
     */
    boolean canAddToCollection(final DocumentModel collection, final CoreSession session);

    /**
     * Check that the given core session has permission to manage the collection.
     *
     * @param collection the collection
     * @param session the core session
     * @return true if the core session has permission to manage the collection
     */
    boolean canManage(final DocumentModel collection, final CoreSession session);

    /**
     * Get the list of collection of a document. The resulting list will only contain the collections visible by the
     * given core session (i.e. the collections on which the given core session has at least READ permission).
     *
     * @param collectionMember the document
     * @param session the core session
     * @return the list of visible collections the collectionMember belong to
     */
    List<DocumentModel> getVisibleCollection(final DocumentModel collectionMember, final CoreSession session);

    /**
     * Get the list of collection of a document. The resulting list will only contain the collections visible by the
     * given core session (i.e. the collections on which the given core session has at least READ permission). The
     * resulting list's size will be limited to masResult.
     *
     * @param collectionMember the document
     * @param maxResult the limit
     * @param session the core session
     * @return the list of maxResult first visible collections the collectionMember belong to
     */
    List<DocumentModel> getVisibleCollection(final DocumentModel collectionMember, final int maxResult,
            final CoreSession session);

    /**
     * Check that the given core session has READ permission on at least one collection of the given document.
     *
     * @param collectionMember the document
     * @param session the core session
     * @return true if collectionMember has at least one collection on which the session has READ permission
     */
    boolean hasVisibleCollection(final DocumentModel collectionMember, final CoreSession session);

    /**
     * Check that a document can be added to a collection.
     *
     * @param document the document
     * @return true if the document can be added to the collection
     */
    boolean isCollectable(final DocumentModel document);

    /**
     * Check that a document has already been added to a collection.
     *
     * @param document the document
     * @return true if the document has already been added to a collection
     */
    boolean isCollected(final DocumentModel document);

    /**
     * Check that a document is a collection.
     *
     * @param document the document
     * @return true if the document is a collection
     */
    boolean isCollection(final DocumentModel document);

    /**
     * Check whether a document is in a given collection.
     *
     * @param collection the collection
     * @param document the document to check
     * @param session the session
     * @since 5.9.4
     */
    boolean isInCollection(final DocumentModel collection, final DocumentModel document, final CoreSession session);

    /**
     * Move the member1 right after the member2 within the collection. If the member2 is null, then the member1 is
     * moved to first position of the collection.
     *
     * @param session the session
     * @param collection the collection
     * @param member1 the member1
     * @param member2 the member2
     * @return true if successfully moved
     * @since 8.4
     */
    boolean moveMembers(final CoreSession session, final DocumentModel collection, final DocumentModel member1,
            final DocumentModel member2);

    /**
     * Update all documents referenced by a collection to add a reference back the collection. This is used when a
     * creation is created by copy in order to tell the members of a copied collection that they also belongs to the
     * newly created collection.
     *
     * @param collection the collection
     */
    void processCopiedCollection(final DocumentModel collection);

    /**
     * Update all documents referenced by a collection to remove the reference to the collection. This is used after the
     * complete deletion of a collection.
     *
     * @param collection the collection
     */
    void processRemovedCollection(final DocumentModel collection);

    /**
     * Update all collections referenced by a document. This is used after the complete deletion of a document to remove
     * its reference from all collections it belongs to.
     *
     * @param collectionMember the document
     */
    void processRemovedCollectionMember(final DocumentModel collectionMember);

    /**
     * Restore the collection members of the version.
     *
     * @param collection the collection
     * @param collection the version
     *
     * @since 7.3
     */
    void processRestoredCollection(final DocumentModel collection, final DocumentModel version);

    /**
     * Remove a list of document from a given collection.
     *
     * @param collection the collection
     * @param documentListToBeRemoved the document to be removed
     * @param session the core session
     */
    void removeAllFromCollection(final DocumentModel collection, final List<DocumentModel> documentListToBeRemoved,
            final CoreSession session);

    /**
     * Remove a document from a collection.
     *
     * @param collection the collection
     * @param documentToBeRemoved the document to be removed
     * @param session the core session
     */
    void removeFromCollection(final DocumentModel collection, final DocumentModel documentToBeRemoved,
            final CoreSession session);

    /**
     * Create a collection with a given name, description and path.
     *
     * @param session
     * @param title
     * @param description
     * @param path
     * @return
     * @since 5.9.4
     */
    DocumentModel createCollection(final CoreSession session, String title, String description, String path);

    /**
     * @deprecated since 10.3 use {@link #getUserDefaultCollections(CoreSession)} instead
     * @since 6.0
     */
    @Deprecated
    DocumentModel getUserDefaultCollections(final DocumentModel context, final CoreSession session);

    /**
     * Get user collections root document.
     *
     * @param context contextual document
     * @param session the core session
     * @return the user collections root document
     * @since 10.3
     */
    DocumentModel getUserDefaultCollections(final CoreSession session);

    /**
     * @param documentToBeRemoved
     * @param collectionId
     * @param session
     * @since 6.0
     */
    void doRemoveFromCollection(DocumentModel documentToBeRemoved, String collectionId, CoreSession session);

}
