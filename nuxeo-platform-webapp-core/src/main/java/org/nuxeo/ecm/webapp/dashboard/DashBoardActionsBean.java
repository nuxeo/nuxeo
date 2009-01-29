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
import java.util.List;

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
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
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

    @In(create = true)
    protected transient JbpmService jbpmService;

    @RequestParameter("sortColumn")
    protected String newSortColumn;

    protected SortInfo sortInfo;

    protected Collection<DocumentProcessItem> currentUserProcesses;

    protected Collection<DashBoardItem> currentUserTasks;

    @Factory(value = "currentUserTasks", scope = ScopeType.PAGE)
    public Collection<DashBoardItem> computeDashboardItems()
            throws ClientException {
        if (currentUserTasks == null) {
            currentUserTasks = new ArrayList<DashBoardItem>();
            NuxeoPrincipal pal = (NuxeoPrincipal) currentUser;
            List<TaskInstance> tasks = jbpmService.getCurrentTaskInstances(pal,
                    null);
            if (tasks != null) {
                for (TaskInstance task : tasks) {
                    try {
                        if (task.hasEnded() || task.isCancelled()) {
                            continue;
                        }
                        DocumentModel doc = jbpmService.getDocumentModel(task,
                                pal);
                        if (doc != null) {
                            currentUserTasks.add(new DashBoardItemImpl(task,
                                    doc));
                        } else {
                            log.error(String.format(
                                    "User '%s' has a task of type '%s' on an "
                                            + "unexisting or unvisible document",
                                    currentUser.getName(), task.getName()));
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
        return currentUserTasks;
    }

    @Factory(value = "currentUserProcesses", scope = ScopeType.PAGE)
    public Collection<DocumentProcessItem> computeDocumentProcessItems()
            throws ClientException {
        if (currentUserProcesses == null) {
            currentUserProcesses = new ArrayList<DocumentProcessItem>();
            NuxeoPrincipal pal = (NuxeoPrincipal) currentUser;
            List<ProcessInstance> processes = jbpmService.getCurrentProcessInstances(
                    (NuxeoPrincipal) currentUser, null);
            if (processes != null) {
                for (ProcessInstance process : processes) {
                    try {
                        if (process.hasEnded()) {
                            continue;
                        }
                        DocumentModel doc = jbpmService.getDocumentModel(
                                process, pal);
                        if (doc != null) {
                            currentUserProcesses.add(new DocumentProcessItemImpl(
                                    process, doc));
                        } else {
                            log.error(String.format(
                                    "User '%s' has a process of type '%s' on an "
                                            + "unexisting or unvisible document",
                                    currentUser.getName(),
                                    process.getProcessDefinition().getName()));
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
        return currentUserProcesses;
    }

    @Observer(value = { JbpmEventNames.WORKFLOW_ENDED,
            JbpmEventNames.WORKFLOW_NEW_STARTED,
            JbpmEventNames.WORKFLOW_TASK_STOP,
            JbpmEventNames.WORKFLOW_TASK_REJECTED,
            JbpmEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            JbpmEventNames.WORKFLOW_TASK_COMPLETED,
            JbpmEventNames.WORKFLOW_TASK_REMOVED,
            JbpmEventNames.WORK_ITEMS_LIST_LOADED,
            JbpmEventNames.WORKFLOW_TASKS_COMPUTED,
            JbpmEventNames.WORKFLOW_ABANDONED,
            org.nuxeo.ecm.webapp.helpers.EventNames.DOMAIN_SELECTION_CHANGED }, create = false, inject = false)
    @BypassInterceptors
    public void invalidateDocumentProcessItems() throws ClientException {
        currentUserProcesses = null;
    }

    @Observer(value = { JbpmEventNames.WORKFLOW_ENDED,
            JbpmEventNames.WORKFLOW_NEW_STARTED,
            JbpmEventNames.WORKFLOW_TASK_START,
            JbpmEventNames.WORKFLOW_TASK_STOP,
            JbpmEventNames.WORKFLOW_TASK_REJECTED,
            JbpmEventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            JbpmEventNames.WORKFLOW_TASK_COMPLETED,
            JbpmEventNames.WORKFLOW_TASK_REMOVED,
            JbpmEventNames.WORK_ITEMS_LIST_LOADED,
            JbpmEventNames.WORKFLOW_TASKS_COMPUTED,
            JbpmEventNames.WORKFLOW_ABANDONED,
            org.nuxeo.ecm.webapp.helpers.EventNames.DOMAIN_SELECTION_CHANGED }, create = false, inject = false)
    @BypassInterceptors
    public void invalidateDashboardItems() throws ClientException {
        currentUserTasks = null;
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

        Object[] params;
        String location = navigationContext.getCurrentDomainPath();
        String templates = location + "/templates";
        if (BOARD_USER.equals(name)) {
            params = new Object[] { currentUser.getName() };
        } else if (BOARD_LATEST_MODIFIED.equals(name)) {
            params = new Object[] { location, templates };
        } else if (BOARD_LATEST_PUBLISHED.equals(name)) {
            params = new Object[] { location };
        } else if (BOARD_WORKSPACES.equals(name)) {
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
        try {
            return queryModelActions.get(qmName).getResultsProvider(
                    documentManager, params, sortInfo);
        } catch (QueryException e) {
            throw new ClientException(String.format("Invalid search query. "
                    + "Check the \"%s\" QueryModel configuration", qmName), e);
        }
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

    public DocumentModelList getUserDocuments() throws ClientException {
        return null;
    }

    public DocumentModelList getUserWorkspaces() throws ClientException {
        return null;
    }

    public String refreshDashboardItems() throws ClientException {
        currentUserTasks = null;
        return null;
    }

    public String refreshDocumentProcessItems() throws ClientException {
        currentUserProcesses = null;
        return null;
    }

    public String doSearch() throws ClientException {
        return null;
    }

    public SortInfo getSortInfo() {
        return sortInfo;
    }
}
