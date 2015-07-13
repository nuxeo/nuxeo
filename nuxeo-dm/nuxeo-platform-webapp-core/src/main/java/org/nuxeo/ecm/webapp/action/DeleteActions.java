/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
     * Checks if the document is "deleted".
     */
    boolean getCanRestoreCurrentDoc();

}
