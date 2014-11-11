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
 * $Id: DashboardActions.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.webapp.dashboard;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.jboss.seam.Seam;
import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.jbpm.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.jbpm.dashboard.DocumentProcessItem;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;

/**
 * Dashboard actions listener.
 * <p>
 * Handles user documents an tasks.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface DashboardActions extends Serializable, ResultsProviderFarm {

    /**
     * Invalidates dashboard items.
     *
     * @see Seam Observer
     */
    void invalidateDashboardItems();

    /**
     * Computes dashboard items.
     *
     * @see Seam factory
     *
     * @return a collection of dashboard items.
     */
    Collection<DashBoardItem> computeDashboardItems() throws ClientException;

    /**
     * Computes the document process items.
     *
     * @see Seam factory
     *
     * @return a collection
     * @throws ClientException
     */
    Collection<DocumentProcessItem> computeDocumentProcessItems()
            throws ClientException;

    /**
     * Invalidates document process items.
     *
     * @see Seam Observer
     */
    void invalidateDocumentProcessItems();

    /**
     * View dashboard.
     */
    String viewDashboard();

    /**
     * Computes the list of documents recently edited by the current user.
     */
    DocumentModelList getUserDocuments();

    /**
     * Computes the list of documents recently modified in the current domain if
     * any.
     *
     * @return XXX
     * @throws ClientException
     */
    DocumentModelList getLastModifiedDocuments() throws ClientException;

    /**
     * Computes the list of workspaces the user has the right to see.
     */
    DocumentModelList getUserWorkspaces();

    /**
     * Navigates to the a given tab.
     *
     * @param dm document model
     * @return a navigation id.
     */
    String navigateToDocumentTab(DocumentModel dm) throws ClientException;

    String refreshDashboardItems();

    String refreshDocumentProcessItems();

    String doSearch();

    SortInfo getSortInfo();

    void invalidateDomainBoundInfo() throws ClientException;

    void invalidateDomainResultProviders() throws ClientException;

    DocumentModel getSelectedDomain() throws ClientException;

    List<DocumentModel> getAvailableDomains() throws ClientException;

    void invalidateAvailableDomains() throws ClientException;

    String getSelectedDomainId() throws ClientException;

    void setSelectedDomainId(String selectedDomainId) throws ClientException;

    String submitSelectedDomainChange() throws ClientException;

    @Destroy
    @Remove
    @PermitAll
    void destroy();

}
