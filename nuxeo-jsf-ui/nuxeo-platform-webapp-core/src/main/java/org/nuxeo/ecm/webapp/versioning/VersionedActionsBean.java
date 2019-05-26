/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Razvan Caraghin
 *     Florent Guillaume
 *     Thierry Martins
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.webapp.versioning;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLISHED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SELECTION_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.query.api.PageSelection;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Deals with versioning actions.
 *
 * @author Razvan Caraghin
 * @author Florent Guillaume
 * @author Thierry Martins
 */
@Name("versionedActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class VersionedActionsBean implements VersionedActions, Serializable {

    private static final long serialVersionUID = 4472648747609642493L;

    private static final Log log = LogFactory.getLog(VersionedActionsBean.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    /** @since 5.6 */
    @In(create = true, required = false)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true, required = false)
    protected transient ResourcesAccessor resourcesAccessor;

    @In
    protected transient Context sessionContext;

    @In(create = true)
    protected transient DocumentVersioning documentVersioning;

    protected transient PageSelections<VersionModel> versionModelList;

    protected String selectedVersionId;

    protected String checkedOut;

    @Create
    @Override
    public void initialize() {
    }

    @Observer(value = { DOCUMENT_SELECTION_CHANGED, DOCUMENT_CHANGED, DOCUMENT_SUBMITED_FOR_PUBLICATION,
            DOCUMENT_PUBLISHED }, create = false)
    @BypassInterceptors
    @Override
    public void resetVersions() {
        versionModelList = null;
    }

    @Override
    @Factory(value = "versionList", scope = EVENT)
    public PageSelections<VersionModel> getVersionList() {
        if (versionModelList == null || versionModelList.getEntries() == null
                || versionModelList.getEntries().isEmpty()) {
            retrieveVersions();
        }
        return versionModelList;
    }

    @Override
    public void retrieveVersions() {
        /**
         * in case the document is a proxy,meaning is the result of a publishing,to have the history of the document
         * from which this proxy was created,first we have to get to the version that was created when the document was
         * publish,and to which the proxy document indicates,and then from that version we have to get to the root
         * document.
         */
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentModel doc;
        if (currentDocument.isProxy()) {
            DocumentRef ref = currentDocument.getRef();
            DocumentModel version = documentManager.getSourceDocument(ref);
            doc = documentManager.getSourceDocument(version.getRef());
        } else {
            doc = currentDocument;
        }
        List<PageSelection<VersionModel>> versionModelSelections = new ArrayList<>();
        for (VersionModel versionModel : documentVersioning.getItemVersioningHistory(doc)) {
            versionModelSelections.add(new PageSelection<>(versionModel, isVersionSelected(versionModel)));
        }
        versionModelList = new PageSelections<>(versionModelSelections);
    }

    /**
     * Checks if the {@code versionModel} is selected.
     *
     * @param versionModel the version model
     * @return true, if the {@versionModel} is selected
     */
    protected boolean isVersionSelected(VersionModel versionModel) {

        List<DocumentModel> currentVersionSelection = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_VERSION_SELECTION);
        if (currentVersionSelection != null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                DocumentModel version = documentManager.getDocumentWithVersion(currentDocument.getRef(), versionModel);
                if (version != null) {
                    return currentVersionSelection.contains(version);
                }
            }
        }
        return false;
    }

    /**
     * Restores the document to the selected version. If there is no selected version it does nothing.
     *
     * @return the page that needs to be displayed next
     */
    @Override
    public String restoreToVersion(VersionModel selectedVersion) {
        DocumentModel restoredDocument = documentManager.restoreToVersion(
                navigationContext.getCurrentDocument().getRef(), new IdRef(selectedVersion.getId()), true, true);
        documentManager.save();

        // same as edit basically
        // XXX AT: do edit events need to be sent?
        EventManager.raiseEventsOnDocumentChange(restoredDocument);
        return navigationContext.navigateToDocument(restoredDocument, "after-edit");
    }

    @Override
    public String restoreToVersion() {
        if (getSelectedVersionId() != null) {
            VersionModel selectedVersion = new VersionModelImpl();
            selectedVersion.setId(getSelectedVersionId());
            return restoreToVersion(selectedVersion);
        }
        return null;
    }

    @Override
    public String viewArchivedVersion(VersionModel selectedVersion) {
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument(), selectedVersion);
    }

    @Override
    public String viewArchivedVersion() {
        if (getSelectedVersionId() != null) {
            VersionModel selectedVersion = new VersionModelImpl();
            selectedVersion.setId(getSelectedVersionId());
            return viewArchivedVersion(selectedVersion);
        }
        return null;
    }

    @Override
    public boolean getCanRestore() {
        // TODO: should check for a specific RESTORE permission instead
        return documentManager.hasPermission(navigationContext.getCurrentDocument().getRef(),
                SecurityConstants.WRITE_VERSION);
    }

    /**
     * Tells if the current selected document is checked out or not.
     */
    @Override
    public String getCheckedOut() {
        if (documentManager.isCheckedOut(navigationContext.getCurrentDocument().getRef())) {
            checkedOut = "Checked-out";
        } else {
            checkedOut = "Checked-in";
        }
        return checkedOut;
    }

    @Override
    public void setCheckedOut(String checkedOut) {
        this.checkedOut = checkedOut;
    }

    /**
     * Checks the document out.
     *
     * @return the next page
     */
    @Override
    public String checkOut() {
        documentManager.checkOut(navigationContext.getCurrentDocument().getRef());
        return null;
    }

    /**
     * Checks the selected document in, with the selected version.
     */
    @Override
    public String checkIn() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        documentManager.checkIn(currentDocument.getRef(), null, null);
        retrieveVersions();
        return navigationContext.getActionResult(currentDocument, UserAction.AFTER_EDIT);
    }

    @Override
    public DocumentModel getSourceDocument() {
        return getSourceDocument(navigationContext.getCurrentDocument());
    }

    /**
     * @since 5.4
     */
    @Override
    public DocumentModel getSourceDocument(DocumentModel document) {
        return documentManager.getSourceDocument(document.getRef());
    }

    @Override
    public boolean canRemoveArchivedVersion(VersionModel selectedVersion) {
        DocumentRef docRef = navigationContext.getCurrentDocument().getRef();
        DocumentModel docVersion = documentManager.getDocumentWithVersion(docRef, selectedVersion);
        if (docVersion == null) {
            // it doesn't exist? don't remove. Still it is a problem
            log.warn("Unexpectedly couldn't find the version " + selectedVersion.getLabel());
            return false;
        }
        return documentManager.canRemoveDocument(docVersion.getRef());
    }

    /**
     * @since 5.6
     */
    @Override
    public boolean getCanRemoveSelectedArchivedVersions() {
        List<DocumentModel> currentVersionSelection = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_VERSION_SELECTION);
        if (currentVersionSelection != null && currentVersionSelection.size() > 0) {
            for (DocumentModel version : currentVersionSelection) {
                if (!documentManager.canRemoveDocument(version.getRef())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String removeArchivedVersion(VersionModel selectedVersion) {
        DocumentRef docRef = navigationContext.getCurrentDocument().getRef();
        DocumentModel docVersion = documentManager.getDocumentWithVersion(docRef, selectedVersion);
        if (docVersion == null) {
            // it doesn't exist? consider removed
            log.warn("Unexpectedly couldn't find the version " + selectedVersion.getLabel());
            return null;
        }
        documentManager.removeDocument(docVersion.getRef());
        documentManager.save();
        resetVersions();
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("feedback.versioning.versionRemoved"));
        return null;
    }

    /**
     * @since 5.6
     */
    @Override
    public String removeSelectedArchivedVersions() {

        List<DocumentModel> currentVersionSelection = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_VERSION_SELECTION);
        if (currentVersionSelection == null || currentVersionSelection.isEmpty()) {
            log.warn("Currently selected version list is null or empty, cannot remove any version.");
            return null;
        }
        for (DocumentModel version : currentVersionSelection) {
            if (version != null) {
                documentManager.removeDocument(version.getRef());
            }
        }
        documentManager.save();
        resetVersions();
        // remove from all lists
        documentsListsManager.removeFromAllLists(new ArrayList<>(currentVersionSelection));
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("feedback.versioning.versionsRemoved"));
        return null;
    }

    @Override
    public String getSelectedVersionId() {
        return selectedVersionId;
    }

    @Override
    public void setSelectedVersionId(String selectedVersionId) {
        this.selectedVersionId = selectedVersionId;
    }

}
