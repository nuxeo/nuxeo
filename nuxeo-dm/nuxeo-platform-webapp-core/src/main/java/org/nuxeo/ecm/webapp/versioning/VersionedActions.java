/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Razvan Caraghin
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.versioning;

import java.util.List;

import org.jboss.seam.annotations.Create;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;

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
     * @return list of VersionModel
     */
    List<VersionModel> getVersionList() throws ClientException;

    /**
     * Retrieves the versions for the current document.
     */
    void retrieveVersions() throws ClientException;

    /**
     * Restored the document to the selected version. If there is no selected
     * version it does nothing.
     *
     * @return the page that needs to be displayed next
     */
    String restoreToVersion(VersionModel selectedVersion)
            throws ClientException;

    /**
     * Security check to enable or disable the restore button.
     *
     * @return permission check result
     */
    boolean getCanRestore() throws ClientException;

    /**
     * Tells if the current selected document is checked out or not.
     */
    String getCheckedOut() throws ClientException;

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
    @SuppressWarnings( { "NonBooleanMethodNameMayNotStartWithQuestion" })
    String checkOut() throws ClientException;

    /**
     * Checks the selected document in, with the selected version.
     */
    String checkIn() throws ClientException;

    @Create
    void initialize();

    /**
     * When the user selects/changes other documents then we nullify the list of
     * versions associated with the document so that the factory method gets
     * called when the list is used.
     * <p>
     * This way we achieve lazy loading of data from backend - only when its
     * needed and not loading it when the event is fired.
     *
     */
    void resetVersions();

    /**
     * View an older version of the document.
     */
    String viewArchivedVersion(VersionModel selectedVersion)
            throws ClientException;

    DocumentModel getSourceDocument() throws ClientException;

    DocumentModel getSourceDocument(DocumentModel document) throws ClientException;

    /**
     * Check if a version can be removed. It won't be possible if a proxy is
     * pointing to it.
     */
    boolean canRemoveArchivedVersion(VersionModel selectedVersion);

    /**
     * Remove an archived version.
     *
     * @param selectedVersion the version model to remove
     */
    String removeArchivedVersion(VersionModel selectedVersion)
            throws ClientException;
}
