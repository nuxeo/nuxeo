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
package org.nuxeo.ecm.core.trash;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Service containing the logic about deleting/purging/undeleting a document.
 */
public interface TrashService {

    /**
     * Can a child of the folder be deleted?
     *
     * @param folder the folder
     * @return {@code true} if the folder allows its children to be deleted
     */
    boolean folderAllowsDelete(DocumentModel folder);

    /**
     * Is at least one doc deletable according to its container?
     *
     * @param docs the documents
     * @return {@code true} if one doc is in a folder that allows its children to be deleted
     */
    boolean checkDeletePermOnParents(List<DocumentModel> docs);

    /**
     * Is at least one doc deletable?
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @param checkProxies {@code true} to count proxies as non-deletable
     * @return {@code true} if at least one doc is deletable
     */
    boolean canDelete(List<DocumentModel> docs, Principal principal, boolean checkProxies);

    /**
     * Are all documents purgeable/undeletable?
     * <p>
     * Documents need to be in the deleted lifecycle state for this to be true, in addition to the standard permission
     * checks.
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @return {@code true} if the documents are purgeable/undeletable
     */
    boolean canPurgeOrUndelete(List<DocumentModel> docs, Principal principal);

    /**
     * Gets the trash info for a list of documents.
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @param checkProxies {@code true} to count proxies as non-deletable
     * @param checkDeleted {@code true} if documents have to be in the deleted state to be considered (otherwise
     *            forbidden)
     * @return the trash info
     */
    TrashInfo getTrashInfo(List<DocumentModel> docs, Principal principal, boolean checkProxies, boolean checkDeleted);

    /**
     * Gets the closest document's ancestor above all the paths.
     * <p>
     * This is used to find what safe document to redirect to when deleting some.
     *
     * @param doc the document
     * @param paths the paths
     * @return the closer document above doc and above all the paths
     */
    DocumentModel getAboveDocument(DocumentModel doc, Set<Path> paths);

    /**
     * Moves documents to the trash, or directly deletes them if their lifecycle does not allow trash use.
     * <p>
     * Do nothing if the document current state is deleted.
     * <p>
     * Placeless documents are deleted immediately.
     *
     * @param docs the documents to trash
     */
    void trashDocuments(List<DocumentModel> docs);

    /**
     * Purges (completely deletes) documents .
     *
     * @param session the session
     * @param docRefs the documents to purge
     */
    void purgeDocuments(CoreSession session, List<DocumentRef> docRefs);

    /**
     * Undeletes documents (and ancestors if needed to make them visible).
     * <p>
     * Also fires async events to undelete the children.
     *
     * @param docs the documents to undelete
     * @return the set of ancestors whose children have been undeleted (for UI notification)
     */
    Set<DocumentRef> undeleteDocuments(List<DocumentModel> docs);

    /**
     * Get all documents from the trash of the current document.
     *
     * @since 7.1
     * @param currentDoc The current/parent document of trash document.
     * @return All documents in the trash of the current document.
     */
    DocumentModelList getDocuments(DocumentModel currentDoc);

    /**
     * Gets all document references from the trash of a Folderish document that can be purged.
     *
     * @since 10.1
     * @param parent the Folderish document
     * @return all the purgeable documents in the trash of the parent document
     */
    List<DocumentRef> getPurgeableDocumentRefs(DocumentModel parent);

    /**
     * Mangles the name of a document to avoid collisions with non-trashed documents when it's in the trash.
     *
     * @param doc the document
     * @return
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

}
