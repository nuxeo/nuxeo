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

import java.util.List;
import java.util.Set;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Service containing the logic about deleting/purging/undeleting a document.
 *
 * @deprecated since 10.2, use {@link org.nuxeo.ecm.core.api.trash.TrashService} instead.
 */
@Deprecated
public interface TrashService extends org.nuxeo.ecm.core.api.trash.TrashService {

    /**
     * Can a child of the folder be trashed?
     *
     * @param folder the folder
     * @return {@code true} if the folder allows its children to be trashed
     * @deprecated since 10.1 only used in JSF part, no replacement
     */
    @Deprecated
    boolean folderAllowsDelete(DocumentModel folder);

    /**
     * Is at least one doc deletable according to its container?
     *
     * @param docs the documents
     * @return {@code true} if one doc is in a folder that allows its children to be trashed
     * @deprecated since 10.1 only used in JSF part, no replacement
     */
    @Deprecated
    boolean checkDeletePermOnParents(List<DocumentModel> docs);

    /**
     * Is at least one doc deletable?
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @param checkProxies {@code true} to count proxies as non-deletable
     * @return {@code true} if at least one doc is deletable
     * @deprecated since 10.1 only used in JSF part, no replacement
     */
    @Deprecated
    boolean canDelete(List<DocumentModel> docs, NuxeoPrincipal principal, boolean checkProxies);

    /**
     * Are all documents purgeable/undeletable?
     * <p>
     * Documents need to be in the trash for this to be true, in addition to the standard permission checks.
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @return {@code true} if the documents are purgeable/undeletable
     * @deprecated since 10.1, use {@link #canPurgeOrUntrash(List, NuxeoPrincipal)}
     */
    @Deprecated
    default boolean canPurgeOrUndelete(List<DocumentModel> docs, NuxeoPrincipal principal) {
        return canPurgeOrUntrash(docs, principal);
    }

    /**
     * Gets the trash info for a list of documents.
     *
     * @param docs the documents
     * @param principal the current user (to check locks)
     * @param checkProxies {@code true} to count proxies as non-deletable
     * @param checkDeleted {@code true} if documents have to be in the trashed state to be considered (otherwise
     *            forbidden)
     * @return the trash info
     * @deprecated since 10.1 only used in JSF part, no replacement
     */
    @Deprecated
    TrashInfo getTrashInfo(List<DocumentModel> docs, NuxeoPrincipal principal, boolean checkProxies, boolean checkDeleted);

    /**
     * Gets the closest document's ancestor above all the paths.
     * <p>
     * This is used to find what safe document to redirect to when deleting some.
     *
     * @param doc the document
     * @param paths the paths
     * @return the closer document above doc and above all the paths
     * @deprecated since 10.1 only used in JSF part, use {@link #getAboveDocument(DocumentModel, NuxeoPrincipal)}
     *             instead.
     */
    @Deprecated
    DocumentModel getAboveDocument(DocumentModel doc, Set<Path> paths);

    /**
     * Undeletes documents (and ancestors if needed to make them visible).
     * <p>
     * Also fires async events to untrash the children.
     *
     * @param docs the documents to undelete
     * @return the set of ancestors whose children have been untrashed (for UI notification)
     * @deprecated since 10.1 use {@link #untrashDocuments(List)} instead
     */
    @Deprecated
    Set<DocumentRef> undeleteDocuments(List<DocumentModel> docs);

}
