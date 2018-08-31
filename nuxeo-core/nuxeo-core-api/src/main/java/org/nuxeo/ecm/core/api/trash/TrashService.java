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
package org.nuxeo.ecm.core.api.trash;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Service containing the logic about deleting/purging/undeleting a document.
 */
public interface TrashService {

    /***
     * Event for a document trashed by the user. Triggers an async listener that trashes its children too.
     *
     * @since 10.1
     */
    String DOCUMENT_TRASHED = "documentTrashed";

    /***
     * Event for a document untrashed by the user. Triggers an async listener that untrashes its children too.
     *
     * @since 10.1
     */
    String DOCUMENT_UNTRASHED = "documentUntrashed";

    /**
     * Key for {@link DocumentModel#getContextData(String)} which skips the renaming during trash/untrash mechanism when
     * the value is {@link Boolean#TRUE}.
     *
     * @since 10.1
     */
    String DISABLE_TRASH_RENAMING = "skipTrashRenaming";

    /**
     * Configuration property to enable backward compatibility to forward the call to the service on
     * followTransition("deleted").
     *
     * @since 10.2
     */
    String IS_TRASHED_FROM_DELETE_TRANSITION = "org.nuxeo.isTrashed.from.deleteTransition";

    /**
     * @return whether or not the input {@link DocumentRef} is trashed.
     * @since 10.1
     */
    boolean isTrashed(CoreSession session, DocumentRef doc);

    /**
     * Are all documents purgeable/undeletable?
     * <p>
     * Documents need to be in the trash for this to be true, in addition to the standard permission checks.
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @return {@code true} if the documents are purgeable/undeletable
     */
    boolean canPurgeOrUntrash(List<DocumentModel> docs, Principal principal);

    /**
     * Is document purgeable/untrashable?
     * <p>
     * Documents need to be in the trash for this to be true, in addition to the standard permission checks.
     *
     * @param doc the document
     * @param principal the current user (to check locks)
     * @return {@code true} if the documents are purgeable/untrashable
     */
    default boolean canPurgeOrUntrash(DocumentModel doc, Principal principal) {
        return canPurgeOrUntrash(Collections.singletonList(doc), principal);
    }

    /**
     * Gets the first non trashed ancestor.
     * <p>
     * This is used to find what safe document to redirect to when deleting one.
     *
     * @param doc the trashed document
     * @param principal the current user
     * @return the first non trashed ancestor
     */
    DocumentModel getAboveDocument(DocumentModel doc, Principal principal);

    /**
     * Moves documents to the trash.
     * <p>
     * Do nothing if the document current state is trashed.
     * <p>
     * Proxies are removed.
     * <p>
     * Since 10.3, sublevels are trashed asynchronously using BAF.
     *
     * @param docs the documents to trash
     */
    void trashDocuments(List<DocumentModel> docs);

    /**
     * Moves document to the trash.
     * <p>
     * Do nothing if the document current state is trashed.
     * <p>
     * Proxies are removed.
     * <p>
     * Since 10.3, sublevels are trashed asynchronously using BAF.
     *
     * @param doc the document to trash
     * @since 10.1
     */
    default void trashDocument(DocumentModel doc) {
        trashDocuments(Collections.singletonList(doc));
    }

    /**
     * Purges (completely deletes) documents.
     *
     * @param session the session
     * @param docRefs the documents to purge
     */
    void purgeDocuments(CoreSession session, List<DocumentRef> docRefs);

    /**
     * Purges (completely deletes) trashed documents under the given parent.
     *
     * @param parent The parent document of trashed documents.
     * @since 10.1
     */
    void purgeDocumentsUnder(DocumentModel parent);

    /**
     * Unmoves documents from the trash.
     * <p>
     * Also fires async events to untrash the children.
     *
     * @param docs the documents to untrash
     */
    void untrashDocuments(List<DocumentModel> docs);

    /**
     * Unmoves document from the trash.
     * <p>
     * Also fires async events to untrash the children.
     *
     * @param doc the document to untrash
     * @since 10.1
     */
    default void untrashDocument(DocumentModel doc) {
        untrashDocuments(Collections.singletonList(doc));
    }

    /**
     * Get all documents from the trash of the current document.
     *
     * @since 7.1
     * @param parent The parent document of trash document.
     * @return All documents in the trash of the current document.
     */
    DocumentModelList getDocuments(DocumentModel parent);

    /**
     * Mangles the name of a document to avoid collisions with non-trashed documents when it's in the trash.
     *
     * @param doc the document
     * @since 7.3
     */
    String mangleName(DocumentModel doc);

    /**
     * Unmangles the name of a document in the trash to find its un-trashed name.
     *
     * @param doc the trashed document
     * @return the unmangled name
     * @since 7.3
     */
    String unmangleName(DocumentModel doc);

    /**
     * Features of the implementation of the service.
     *
     * @see TrashService#hasFeature
     * @since 10.1
     */
    enum Feature {
        /** Trashed state is deduced from lifeCycle. */
        TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE,
        /** Trashed state currently in migration. */
        TRASHED_STATE_IN_MIGRATION,
        /** Trashed state is a dedicated property. */
        TRASHED_STATE_IS_DEDICATED_PROPERTY,
    }

    /**
     * Checks if a feature is available.
     *
     * @since 10.1
     */
    boolean hasFeature(Feature feature);

}
