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

package org.nuxeo.ecm.webapp.contentbrowser;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;
import org.nuxeo.ecm.webapp.table.model.DocModelTableModel;

/**
 * Provides contentRoot specific actions.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public interface ContentRootsActions extends StatefulBaseLifeCycle {

    void initialize();

    @Destroy
    @Remove
    @PermitAll
    void destroy();

    String display();

    /**
     * Called when a workspace {@link DocumentModel} is selected. It saves the
     * selected info on the context.
     *
     * @return the page that is going to be displayed next
     */
    String selectWorkspace() throws ClientException;

    /**
     * Called when a section {@link DocumentModel} is selected. It saves the
     * selected info on the context.
     *
     * @return the page that is going to be displayed next
     */
    String selectSection() throws ClientException;

    /**
     * Called when user wants to edit a selected document.
     *
     * @return the edit page
     */
    String editWorkspace() throws ClientException;

    /**
     * Called when the user finishes editing.
     *
     * @return the page after edit
     */
    String updateWorkspace() throws ClientException;

    /**
     * Gets the workspaces contained in the selected domain.
     */
    void getWorkspaces() throws ClientException;

    /**
     * Gets the sections contained in the selected domain.
     */
    void getSections() throws ClientException;

    List<DocumentModel> getContentRootDocuments() throws ClientException;

    /**
     * Indicates if the current user can create a workspace.
     */
    Boolean getCanAddWorkspaces() throws ClientException;

    /**
     * When the user selects other content root documents/domains then we
     * nullify the list of workspace type documents so that the factory method
     * gets called when the list is used.
     * <p>
     * This way we achieve lazy loading of data from backend - only when its
     * needed and not loading it when the event is fired.
     */
    void resetTableModel();

    /**
     * When the user selects other domains then we nullify the list of content
     * root documents so that the factory method gets called when the list is
     * used.
     * <p>
     * This way we achieve lazy loading of data from backend - only when its
     * needed and not loading it when the event is fired.
     */
    void resetContentRootDocuments();

    String cancel();

    DocModelTableModel getWorkspacesTableModel() throws ClientException;

    DocModelTableModel getSectionsTableModel() throws ClientException;

    DocModelTableModel reconstructWorkspacesTableModel() throws ClientException;

    DocModelTableModel reconstructSectionsTableModel() throws ClientException;

    boolean getAdministrator();

    @WebRemote
    void selectAllRows(boolean checked) throws ClientException;

}
