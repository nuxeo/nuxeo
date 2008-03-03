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
 * $Id$
 */

package org.nuxeo.ecm.webapp.versioning;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * Exposes the actions that can be taken related to versioning and documents.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
public interface VersionedActions extends StatefulBaseLifeCycle {

    /**
     * Factory accessor for currentDocument versionList.
     *
     * @return list of VersionModel
     * @throws ClientException
     */
    List<VersionModel> getVersionList() throws ClientException;

    /**
     * Retrieves the versions for the current document.
     *
     * @throws ClientException
     *
     */
    void retrieveVersions() throws ClientException;

    /**
     * Restored the document to the selected version. If there is no selected
     * version it does nothing.
     *
     * @return the page that needs to be displayed next
     * @throws ClientException
     */
    String restoreToVersion(VersionModel selectedVersion) throws ClientException;

    /**
     * Security check to enable or disable the restore button.
     *
     * @return permission check result
     * @throws ClientException
     */
    boolean getCanRestore() throws ClientException;

    /**
     * Destroys the seam component.
     *
     */
    void destroy();

    /**
     * Tells if the current selected document is checked out or not.
     *
     * @return
     * @throws ClientException
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
     * @throws ClientException
     */
    @SuppressWarnings({"NonBooleanMethodNameMayNotStartWithQuestion"})
    String checkOut() throws ClientException;

    /**
     * Checks the selected document in, with the selected version.
     *
     * @return
     * @throws ClientException
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
     * @return
     * @throws ClientException
     */
    String viewArchivedVersion(VersionModel selectedVersion) throws ClientException;

    DocumentModel getSourceDocument() throws ClientException;

}
