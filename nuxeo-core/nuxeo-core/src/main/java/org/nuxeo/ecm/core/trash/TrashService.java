/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.trash;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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
    boolean folderAllowsDelete(DocumentModel folder) throws ClientException;

    /**
     * Is at least one doc deletable according to its container?
     *
     * @param docs the documents
     * @return {@code true} if one doc is in a folder that allows its children
     *         to be deleted
     */
    boolean checkDeletePermOnParents(List<DocumentModel> docs)
            throws ClientException;

    /**
     * Is at least one doc deletable?
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @param checkProxies {@code true} to count proxies as non-deletable
     * @return {@code true} if at least one doc is deletable
     */
    boolean canDelete(List<DocumentModel> docs, Principal principal,
            boolean checkProxies) throws ClientException;

    /**
     * Are all documents purgeable/undeletable?
     * <p>
     * Documents need to be in the deleted lifecycle state for this to be true,
     * in addition to the standard permission checks.
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @return {@code true} if the documents are purgeable/undeletable
     */
    boolean canPurgeOrUndelete(List<DocumentModel> docs, Principal principal)
            throws ClientException;

    /**
     * Gets the trash info for a list of documents.
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @param checkProxies {@code true} to count proxies as non-deletable
     * @param checkDeleted {@code true} if documents have to be in the deleted
     *            state to be considered (otherwise forbidden)
     * @return the trash info
     */
    TrashInfo getTrashInfo(List<DocumentModel> docs, Principal principal,
            boolean checkProxies, boolean checkDeleted) throws ClientException;

    /**
     * Gets the closest document's ancestor above all the paths.
     * <p>
     * This is used to find what safe document to redirect to when deleting
     * some.
     *
     * @param doc the document
     * @param paths the paths
     * @return the closer document above doc and above all the paths
     */
    DocumentModel getAboveDocument(DocumentModel doc, Set<Path> paths)
            throws ClientException;

    /**
     * Moves documents to the trash, or directly deletes them if their lifecycle
     * does not allow trash use.
     *
     * @param docs the documents to trash
     */
    void trashDocuments(List<DocumentModel> docs) throws ClientException;

    /**
     * Purges (completely deletes) documents .
     *
     * @param session the session
     * @param docRefs the documents to purge
     */
    void purgeDocuments(CoreSession session, List<DocumentRef> docRefs)
            throws ClientException;

    /**
     * Undeletes documents (and ancestors if needed to make them visible).
     * <p>
     * Also fires async events to undelete the children.
     *
     * @param docs the documents to undelete
     * @return the set of ancestors whose children have been undeleted (for UI
     *         notification)
     */
    Set<DocumentRef> undeleteDocuments(List<DocumentModel> docs)
            throws ClientException;

}
