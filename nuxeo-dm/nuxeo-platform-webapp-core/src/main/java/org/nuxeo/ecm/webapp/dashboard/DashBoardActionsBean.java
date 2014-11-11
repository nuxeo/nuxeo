/*
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DashBoardActionsBean.java 29574 2008-01-23 16:08:12Z gracinet $
 */

package org.nuxeo.ecm.webapp.dashboard;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.EmptyResultsProvider;
import org.nuxeo.ecm.platform.jbpm.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.jbpm.dashboard.DocumentProcessItem;
import org.nuxeo.ecm.platform.jbpm.dashboard.WorkflowDashBoard;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
/**
 * Dash board actions.
 * <p>
 * Those actions are related to the current authenticated principal.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Name("dashboardActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class DashBoardActionsBean implements DashboardActions {

    private static final long serialVersionUID = 7737098220471277412L;

    private static final Log log = LogFactory.getLog(DashBoardActionsBean.class);

    protected static final String REVIEW_TAB_ID = "TAB_CONTENT_REVIEW";

    // Result providers
    protected static final String BOARD_LATEST_MODIFIED = "DOMAIN_DOCUMENTS";

    protected static final String BOARD_LATEST_PUBLISHED = "DOMAIN_PUBLISHED_DOCUMENTS";

    protected static final String BOARD_USER = "USER_DOCUMENTS";

    protected static final String BOARD_WORKSPACES = "USER_WORKSPACES";

    protected static final String BOARD_SITES = "USER_SITES";

    protected static final String BOARD_SECTIONS = "USER_SECTIONS";

    @In
    protected transient Context eventContext;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(required = false)
    protected transient Principal currentUser;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @In(create=true, required=false)
    protected WorkflowDashBoard workflowDashBoardActions;

    @RequestParameter("sortColumn")
    protected String newSortColumn;

    protected SortInfo sortInfo;


    @Factory(value = "currentUserTasks", scope = ScopeType.EVENT)
    public Collection<DashBoardItem> computeDashboardItems()
            throws ClientException {
        if (workflowDashBoardActions==null) {
            return new ArrayList<DashBoardItem>();
        }
        return workflowDashBoardActions.computeDashboardItems();
    }

    @Factory(value = "currentUserProcesses", scope = ScopeType.EVENT)
    public Collection<DocumentProcessItem> computeDocumentProcessItems()
            throws ClientException {

           if (workflowDashBoardActions==null) {
               return new ArrayList<DocumentProcessItem>();
           }
           return workflowDashBoardActions.computeDocumentProcessItems();
    }

    public void invalidateDocumentProcessItems() {
           if (workflowDashBoardActions==null) {
               return;
           }
           workflowDashBoardActions.invalidateDocumentProcessItems();
    }

    public void invalidateDashboardItems() {
        if (workflowDashBoardActions==null) {
            return;
        }
        workflowDashBoardActions.invalidateDashboardItems();
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
    }

    public String viewDashboard() {
        return "user_dashboard";
    }

    public PagedDocumentsProvider getResultsProvider(String name,
            SortInfo sortInfo) throws ClientException,
            ResultsProviderFarmUserException {

        String location = navigationContext.getCurrentDomainPath();
        if (location == null) {
            return new EmptyResultsProvider();
        }

        String templates = location + "/templates";
        Object[] params;
        if (BOARD_USER.equals(name)) {
            params = new Object[] { currentUser.getName() };
        } else if (BOARD_LATEST_MODIFIED.equals(name)) {
            params = new Object[] { location, templates };
        } else if (BOARD_LATEST_PUBLISHED.equals(name)) {
            params = new Object[] { location };
        } else if (BOARD_WORKSPACES.equals(name)) {
            params = new Object[] { templates };
        } else if (BOARD_SITES.equals(name)) {
            params = new Object[] { templates };
        } else if (BOARD_SECTIONS.equals(name)) {
            params = null;
        } else {
            throw new ClientException("Unknown board: " + name);
        }
        PagedDocumentsProvider provider;
        try {
            provider = getQmDocuments(name, params, sortInfo);
        } catch (Exception e) {
            log.error("sorted query failed");
            log.debug(e);
            log.error("retrying without sort parameters");
            provider = getQmDocuments(name, params, null);
        }
        provider.setName(name);
        return provider;
    }

    public PagedDocumentsProvider getResultsProvider(String name)
            throws ClientException, ResultsProviderFarmUserException {
        return getResultsProvider(name, null);
    }

    protected PagedDocumentsProvider getQmDocuments(String qmName,
            Object[] params, SortInfo sortInfo) throws ClientException {
        return queryModelActions.get(qmName).getResultsProvider(
                documentManager, params, sortInfo);
    }

    public String navigateToDocumentTab(DocumentModel dm)
            throws ClientException {
        String view = navigationContext.navigateToDocument(dm);
        webActions.resetTabList();
        webActions.setCurrentTabAction(REVIEW_TAB_ID);
        return view;
    }

    public DocumentModelList getLastModifiedDocuments() throws ClientException {
        return resultsProvidersCache.get("DOMAIN_DOCUMENTS").getCurrentPage();
    }

    public DocumentModelList getUserDocuments() {
        return null;
    }

    public DocumentModelList getUserWorkspaces() {
        return null;
    }

    public String refreshDashboardItems() {
        if (workflowDashBoardActions==null) {
            return null;
        }
        return workflowDashBoardActions.refreshDashboardItems();
    }

    public String refreshDocumentProcessItems() {
        if (workflowDashBoardActions==null) {
            return null;
        }
        return workflowDashBoardActions.refreshDocumentProcessItems();

    }

    public String refreshProvider(String providerName) {
        resultsProvidersCache.invalidate(providerName);
        return null;
    }

    public String doSearch() {
        return null;
    }

    public SortInfo getSortInfo() {
        return sortInfo;
    }
}
