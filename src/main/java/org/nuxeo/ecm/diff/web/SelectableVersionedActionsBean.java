/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.DEPLOYMENT;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.diff.documentsLists.VersionDocumentsListsConstants;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.versioning.VersionedActions;
import org.nuxeo.ecm.webapp.versioning.VersionedActionsBean;

/**
 * Overrides the Nuxeo default implementation of {@link VersionedActions} to
 * wrap the {@link VersionModel} objects of the {@code versionModelList} into
 * {@link VersionModelSelection} objects. This enables the selection of a
 * {@link VersionModel} in the archived versions listing.
 * <p>
 * Also manages the selection of a version in the listing.
 *
 * @author Antoine Taillefer
 */
@Name("versionedActions")
@Scope(CONVERSATION)
@Install(precedence = DEPLOYMENT)
public class SelectableVersionedActionsBean extends VersionedActionsBean {

    private static final long serialVersionUID = 492979333999931506L;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    /**
     * Wraps {@link VersionModel} objects into {link
     * {@link VersionModelSelection} objects.
     */
    @Override
    public void retrieveVersions() throws ClientException {

        super.retrieveVersions();
        List<VersionModel> selectableVersionModelList = new ArrayList<VersionModel>();
        for (VersionModel versionModel : versionModelList) {
            selectableVersionModelList.add(new VersionModelSelection(
                    versionModel, isVersionSelected(versionModel)));
        }
        versionModelList = selectableVersionModelList;
    }

    /**
     * Handle version row selection event after having ensured that the
     * navigation context stills points to currentDocumentRef to protect against
     * browsers' back button errors.
     *
     * @param versionModelSelection the version model selection
     * @param requestedCurrentDocRef the requested current doc ref
     * @throws ClientException if currentDocRef is not a valid document
     */
    public void checkCurrentDocAndProcessVersionSelectRow(
            VersionModelSelection versionModelSelection,
            String requestedCurrentDocRef) throws ClientException {

        DocumentRef requestedCurrentDocumentRef = new IdRef(
                requestedCurrentDocRef);
        DocumentRef currentDocumentRef = null;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            currentDocumentRef = currentDocument.getRef();
        }
        if (!requestedCurrentDocumentRef.equals(currentDocumentRef)) {
            navigationContext.navigateToRef(requestedCurrentDocumentRef);
        }
        processVersionSelectRow(versionModelSelection);
    }

    /**
     * Checks if the {@code versionModel} is selected.
     *
     * @param versionModel the version model
     * @return true, if the {@versionModel} is selected
     * @throws ClientException the client exception
     */
    protected final boolean isVersionSelected(VersionModel versionModel)
            throws ClientException {

        List<DocumentModel> currentVersionSelection = documentsListsManager.getWorkingList(VersionDocumentsListsConstants.CURRENT_VERSION_SELECTION);
        if (currentVersionSelection != null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                DocumentModel version = documentManager.getDocumentWithVersion(
                        currentDocument.getRef(), versionModel);
                if (version != null) {
                    return currentVersionSelection.contains(version);
                }
            }
        }
        return false;
    }

    /**
     * Processes the version selection row.
     *
     * @param versionModelSelection the version model selection
     * @throws ClientException the client exception
     */
    protected final void processVersionSelectRow(
            VersionModelSelection versionModelSelection) throws ClientException {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            throw new ClientException(
                    "Cannot process verison select row since current document is null.");
        }

        DocumentModel version = documentManager.getDocumentWithVersion(
                currentDocument.getRef(), versionModelSelection);
        if (version == null) {
            throw new ClientException(
                    "Cannot process version select row since selected version document is null.");
        }

        if (Boolean.TRUE.equals(versionModelSelection.isSelected())) {
            documentsListsManager.addToWorkingList(
                    VersionDocumentsListsConstants.CURRENT_VERSION_SELECTION,
                    version);
        } else {
            documentsListsManager.removeFromWorkingList(
                    VersionDocumentsListsConstants.CURRENT_VERSION_SELECTION,
                    version);
        }
    }
}
