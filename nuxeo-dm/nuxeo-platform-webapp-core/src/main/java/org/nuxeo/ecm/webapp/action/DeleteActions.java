/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.action;

import java.util.List;

import javax.annotation.security.PermitAll;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface DeleteActions {

    /**
     * Definitively deletes selected documents.
     */
    String purgeSelection();

    /**
     * Definitively deletes selected documents of the given {@code listName}.
     */
    String purgeSelection(String listName);

    /**
     * Definitively deletes param documents.
     */
    String purgeSelection(List<DocumentModel> docsToPurge);

    /**
     * Moves to trash (delete state) the selected documents.
     */
    String deleteSelection();

    /**
     * Moves to trash (delete state) the selected sections.
     */
    String deleteSelectionSections();

    /**
     * Moves to trash (delete state) the documents.
     */
    String deleteSelection(List<DocumentModel> docsToDelete);

    /**
     * Undeletes the selected documents from trash (recycle bin).
     */
    String undeleteSelection();

    /**
     * Undeletes the args docs from trash (recycle bin).
     */
    String undeleteSelection(List<DocumentModel> docsToUndelete);

    boolean getCanDeleteItem(DocumentModel container);

    boolean getCanDelete();

    boolean getCanDelete(String listName);

    boolean getCanDeleteSections();

    boolean getCanPurge();

    boolean isTrashManagementEnabled();

    boolean checkDeletePermOnParents(List<DocumentModel> docsToDelete);

    @Destroy
    @PermitAll
    void destroy();

    void create();

    /**
     * Undeletes the current document and its children and his deleted parents.
     */
    void restoreCurrentDocument();

    /**
     * Checks if the document is "trashed".
     */
    boolean getCanRestoreCurrentDoc();

}
