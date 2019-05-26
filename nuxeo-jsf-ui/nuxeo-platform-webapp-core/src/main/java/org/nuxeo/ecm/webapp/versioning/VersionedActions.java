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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.webapp.versioning;

import org.jboss.seam.annotations.Create;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.query.api.PageSelections;

/**
 * Exposes the actions that can be taken related to versioning and documents.
 *
 * @author Razvan Caraghin
 * @author Florent Guillaume
 */
public interface VersionedActions {

    /**
     * Factory accessor for currentDocument versionList.
     *
     * @return the selected version list as a {@link PageSelections<VersionModel>}
     */
    PageSelections<VersionModel> getVersionList();

    /**
     * Retrieves the versions for the current document.
     */
    void retrieveVersions();

    /**
     * Restored the document to the selected version. If there is no selected version it does nothing.
     *
     * @return the page that needs to be displayed next
     */
    String restoreToVersion(VersionModel selectedVersion);

    /**
     * Restores the version which id is returned by {@link #getSelectedVersionId()}.
     *
     * @return the view id
     * @since 5.6
     */
    String restoreToVersion();

    /**
     * Security check to enable or disable the restore button.
     *
     * @return permission check result
     */
    boolean getCanRestore();

    /**
     * Tells if the current selected document is checked out or not.
     */
    String getCheckedOut();

    /**
     * Changes the checked-out string.
     *
     * @param checkedOut
     */
    void setCheckedOut(String checkedOut);

    /**
     * Checks the document out.
     *
     * @return the next page
     */
    @SuppressWarnings({ "NonBooleanMethodNameMayNotStartWithQuestion" })
    String checkOut();

    /**
     * Checks the selected document in, with the selected version.
     */
    String checkIn();

    @Create
    void initialize();

    /**
     * When the user selects/changes other documents then we nullify the list of versions associated with the document
     * so that the factory method gets called when the list is used.
     * <p>
     * This way we achieve lazy loading of data from backend - only when its needed and not loading it when the event is
     * fired.
     */
    void resetVersions();

    /**
     * View an older version of the document.
     */
    String viewArchivedVersion(VersionModel selectedVersion);

    /**
     * Navigates to the version which id is returned by {@link #getSelectedVersionId()}.
     *
     * @return the view id
     * @since 5.6
     */
    String viewArchivedVersion();

    DocumentModel getSourceDocument();

    DocumentModel getSourceDocument(DocumentModel document);

    /**
     * Check if a version can be removed. It won't be possible if a proxy is pointing to it.
     */
    boolean canRemoveArchivedVersion(VersionModel selectedVersion);

    /**
     * Check if the currently selected versions can be removed. It won't be possible if a proxy is pointing to one of
     * them.
     *
     * @return true if can remove selected archived versions
     * @since 5.6
     */
    boolean getCanRemoveSelectedArchivedVersions();

    /**
     * Remove an archived version.
     *
     * @param selectedVersion the version model to remove
     */
    String removeArchivedVersion(VersionModel selectedVersion);

    /**
     * Remove currently selected archived versions.
     *
     * @since 5.6
     */
    String removeSelectedArchivedVersions();

    /**
     * Gets currently selected version id.
     *
     * @since 5.6
     */
    String getSelectedVersionId();

    /**
     * Sets currently selected version id.
     *
     * @since 5.6
     */
    void setSelectedVersionId(String selectedVersionId);
}
