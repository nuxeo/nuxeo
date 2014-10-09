/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
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
     * @throws ClientException
     *
     */
    void addToCollection(final DocumentModel collection,
            final DocumentModel documentToBeAdded, final CoreSession session)
            throws ClientException;

    /**
     * Add a list of document to a collection.
     *
     * @param collection the collection
     * @param documentListToBeAdded the list of document to be added
     * @param session the core session
     * @throws ClientException
     *
     */
    void addToCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeAdded,
            final CoreSession session) throws ClientException;

    /**
     * Add a document to a new collection.
     *
     * @param newTitle the title of the new collection
     * @param newDescription the description of the new collection
     * @param documentToBeAdded the document to be added
     * @param session the core session
     * @throws ClientException
     *
     */
    void addToNewCollection(String newTitle, String newDescription,
            DocumentModel documentToBeAdded, CoreSession session)
            throws ClientException;

    /**
     * Add a list of document to a new collection.
     *
     *
     * @param newTitle the title of the new collection
     * @param newDescription the description of the new collection
     * @param documentListToBeAdded the list of document to be added
     * @param session the core session
     * @throws ClientException
     *
     */
    void addToNewCollection(String newTitle, String newDescription,
            List<DocumentModel> documentListToBeAdded,
            CoreSession documentManager) throws ClientException;

    /**
     * Check that a document is a collection and that the given core session has
     * permission to add document inside.
     *
     * @param collection the collection
     * @param session the core session
     * @return true if the given document is a Collection and the core session
     *         has permission to add document inside, false otherwise
     * @throws ClientException
     *
     */
    boolean canAddToCollection(final DocumentModel collection,
            final CoreSession session) throws ClientException;

    /**
     * Check that the given core session has permission to manage the
     * collection.
     *
     * @param collection the collection
     * @param session the core session
     * @return true if the core session has permission to manage the collection
     * @throws ClientException
     *
     */
    boolean canManage(final DocumentModel collection, final CoreSession session)
            throws ClientException;

    /**
     * Get the list of collection of a document. The resulting list will only
     * contain the collections visible by the given core session (i.e. the
     * collections on which the given core session has at least READ
     * permission).
     *
     * @param collectionMember the document
     * @param session the core session
     * @return the list of visible collections the collectionMember belong to
     * @throws ClientException
     *
     */
    List<DocumentModel> getVisibleCollection(
            final DocumentModel collectionMember, final CoreSession session)
            throws ClientException;

    /**
     * Get the list of collection of a document. The resulting list will only
     * contain the collections visible by the given core session (i.e. the
     * collections on which the given core session has at least READ
     * permission). The resulting list's size will be limited to masResult.
     *
     * @param collectionMember the document
     * @param maxResult the limit
     * @param session the core session
     * @return the list of maxResult first visible collections the
     *         collectionMember belong to
     * @throws ClientException
     *
     */
    List<DocumentModel> getVisibleCollection(
            final DocumentModel collectionMember, final int maxResult,
            final CoreSession session) throws ClientException;

    /**
     * Check that the given core session has READ permission on at least one
     * collection of the given document.
     *
     * @param collectionMember the document
     * @param session the core session
     * @return true if collectionMember has at least one collection on which the
     *         session has READ permission
     * @throws ClientException
     *
     */
    boolean hasVisibleCollection(final DocumentModel collectionMember,
            final CoreSession session) throws ClientException;

    /**
     * Check that a document can be added to a collection.
     *
     * @param document the document
     * @return true if the document can be added to the collection
     *
     */
    boolean isCollectable(final DocumentModel document);

    /**
     * Check that a document has already been added to a collection.
     *
     * @param document the document
     * @return true if the document has already been added to a collection
     *
     */
    boolean isCollected(final DocumentModel document);

    /**
     * Check that a document is a collection.
     *
     * @param document the document
     * @return true if the document is a collection
     *
     */
    boolean isCollection(final DocumentModel document);

    /**
     * Check whether a document is in a given collection.
     *
     * @param collection the collection
     * @param document the document to check
     * @param session the session
     * @throws ClientException
     *
     * @since 5.9.4
     */
    boolean isInCollection(final DocumentModel collection,
            final DocumentModel document, final CoreSession session)
            throws ClientException;

    /**
     * Update all documents referenced by a collection to add a reference back
     * the collection. This is used when a creation is created by copy in order
     * to tell the members of a copied collection that they also belongs to the
     * newly created collection.
     *
     * @param collection the collection
     * @throws ClientException
     *
     */
    void processCopiedCollection(final DocumentModel collection)
            throws ClientException;

    /**
     * Update all documents referenced by a collection to remove the reference
     * to the collection. This is used after the complete deletion of a
     * collection.
     *
     * @param collection the collection
     *
     */
    void processRemovedCollection(final DocumentModel collection);

    /**
     * Update all collections referenced by a document. This is used after the
     * complete deletion of a document to remove its reference from all
     * collections it belongs to.
     *
     * @param collectionMember the document
     *
     */
    void processRemovedCollectionMember(final DocumentModel collectionMember);

    /**
     * Remove a list of document from a given collection.
     *
     * @param collection the collection
     * @param documentListToBeRemoved the document to be removed
     * @param session the core session
     * @throws ClientException
     *
     */
    void removeAllFromCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeRemoved,
            final CoreSession session) throws ClientException;

    /**
     * Remove a document from a collection.
     *
     * @param collection the collection
     * @param documentToBeRemoved the document to be removed
     * @param session the core session
     * @throws ClientException
     *
     */
    void removeFromCollection(final DocumentModel collection,
            final DocumentModel documentToBeRemoved, final CoreSession session)
            throws ClientException;

    /**
     * Create a collection with a given name, description and path.
     *
     * @param session
     * @param title
     * @param description
     * @param path
     * @return
     * @throws ClientException
     *
     * @since 5.9.4
     */
    DocumentModel createCollection(final CoreSession session, String title,
            String description, String path) throws ClientException;

    /**
     * Get user collections root document.
     *
     * @param context contextual document
     * @param session the core session
     * @return the user collections root document
     * @throws ClientException
     *
     * @since 5.9.6
     */
    DocumentModel getUserDefaultCollections(final DocumentModel context,
            final CoreSession session) throws ClientException;

    /**
     * @param documentToBeRemoved
     * @param collectionId
     * @param session
     *
     * @since 5.9.6
     */
    void doRemoveFromCollection(DocumentModel documentToBeRemoved,
            String collectionId, CoreSession session);
}
