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

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.persistence.PreRemove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityPolicyBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;

/**
 * Dash board actions.
 *
 * <p>
 * Those actions are related to the current authenticated principal.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("dashboardActions")
@Scope(SESSION)
@Install(precedence = FRAMEWORK)
public class DashBoardActionsBean extends InputController implements
        DashboardActions, ResultsProviderFarm {

    private static final long serialVersionUID = 7737098220471277412L;

    private static final Log log = LogFactory.getLog(DashBoardActionsBean.class);

    protected static final String REVIEW_TAB_ID = "TAB_CONTENT_REVIEW";

    // Result providers
    protected static final String BOARD_LATEST_MODIFIED = "DOMAIN_DOCUMENTS";

    protected static final String BOARD_LATEST_PUBLISHED = "DOMAIN_PUBLISHED_DOCUMENTS";

    protected static final String BOARD_USER = "USER_DOCUMENTS";

    protected static final String BOARD_WORKSPACES = "USER_WORKSPACES";

    protected static final String BOARD_SECTIONS = "USER_SECTIONS";

    protected transient WorkflowDocumentRelationBusinessDelegate wdocBusinessDelegate;

    protected transient WorkflowDocumentSecurityPolicyBusinessDelegate wDocSecuPolicyBusinessDelegate;

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

    transient protected Collection<DashBoardItem> dashboardItems;

    transient protected Collection<DocumentProcessItem> documentProcessItems;

    @RequestParameter("sortColumn")
    protected String newSortColumn;

    protected SortInfo sortInfo;

    public DashBoardActionsBean() {
        initializeBd();
    }

    private void initializeBd() {
        wdocBusinessDelegate = new WorkflowDocumentRelationBusinessDelegate();
        wDocSecuPolicyBusinessDelegate = new WorkflowDocumentSecurityPolicyBusinessDelegate();
    }

    @PostActivate
    public void readState() {
        log.debug("@PostActivate");
        initializeBd();
    }

    @PreRemove
    public void saveState() {
        log.debug("@PreRemove");
    }

    @PrePassivate
    public void prePassivate() {
        log.debug("@Prepassivate");
    }

    @Observer( value={ EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_STOP, EventNames.WORKFLOW_TASK_REJECTED,
            EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.WORKFLOW_TASK_REMOVED,
            EventNames.WORK_ITEMS_LIST_LOADED,
            EventNames.WORKFLOW_TASKS_COMPUTED,
            org.nuxeo.ecm.webapp.helpers.EventNames.DOMAIN_SELECTION_CHANGED }, create=false, inject=false)
    public void invalidateDocumentProcessItems() throws ClientException {
        documentProcessItems = null;
    }

    @Observer( value={ EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_START, EventNames.WORKFLOW_TASK_STOP,
            EventNames.WORKFLOW_TASK_REJECTED,
            EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.WORKFLOW_TASK_REMOVED,
            EventNames.WORK_ITEMS_LIST_LOADED,
            EventNames.WORKFLOW_TASKS_COMPUTED,
            org.nuxeo.ecm.webapp.helpers.EventNames.DOMAIN_SELECTION_CHANGED }, create=false, inject=false)
    public void invalidateDashboardItems() throws ClientException {
        dashboardItems = null;
    }

    @Factory(value = "dashboardActions_documentProcessItems", scope = EVENT)
    public Collection<DocumentProcessItem> computeDocumentProcessItems()
            throws ClientException {

        if (documentProcessItems != null) {
            return documentProcessItems;
        }

        documentProcessItems = new ArrayList<DocumentProcessItem>();
        try {

            WAPI wapi;
            WorkflowDocumentRelationManager wDoc;
            try {
                wapi = WAPIBusinessDelegate.getWAPI();
                wDoc = wdocBusinessDelegate.getWorkflowDocument();
            } catch (Exception e) {
                throw new ClientException(e);
            }

            Principal principal = documentManager.getPrincipal();

            List<String> creators = new ArrayList<String>();
            if (principal instanceof NuxeoPrincipal) {
                creators = ((NuxeoPrincipal) principal).getAllGroups();
            }
            creators.add(principal.getName());

            List<WMProcessInstance> procs = new ArrayList<WMProcessInstance>();

            procs.addAll(wapi.getProcessInstanceForCreators(creators));

            for (WMProcessInstance proc : procs) {

                String pid = proc.getId();

                DocumentRef[] docRefs = wDoc.getDocumentRefsFor(pid);
                for (DocumentRef docRef : docRefs) {
                    DocumentModel dm;
                    try {
                        dm = documentManager.getDocument(docRef);
                    } catch (ClientException ce) {
                        log.error("Associated document doesn't exist anymore... Skipping work item");
                        continue;
                    }

                    String title = (String) dm.getProperty("dublincore",
                            "title");
                    if (title == null) {
                        title = String.valueOf(docRef.hashCode());
                    }

                    DocumentProcessItem item = new DocumentProcessItemImpl(dm,
                            proc);
                    documentProcessItems.add(item);
                }
            }
        } catch (ClientException ce) {
            throw EJBExceptionHandler.wrapException(ce);
        }
        return documentProcessItems;
    }

    @Factory(value = "dashboardActions_dashboardItems", scope = EVENT)
    public Collection<DashBoardItem> computeDashboardItems()
            throws ClientException {

        if (dashboardItems != null) {
            return dashboardItems;
        }

        dashboardItems = new ArrayList<DashBoardItem>();
        try {

            WAPI wapi;
            WorkflowDocumentRelationManager wDoc;
            WorkflowDocumentSecurityPolicyManager wDocSecuPolicy;
            try {
                wapi = WAPIBusinessDelegate.getWAPI();
                wDoc = wdocBusinessDelegate.getWorkflowDocument();
                wDocSecuPolicy = wDocSecuPolicyBusinessDelegate.getWorkflowDocumentRightsPolicyManager();
            } catch (Exception e) {
                throw new ClientException(e);
            }

            Principal principal = documentManager.getPrincipal();

            List<String> groupNames = new ArrayList<String>();
            if (principal instanceof NuxeoPrincipal) {
                groupNames = ((NuxeoPrincipal) principal).getAllGroups();
            }

            List<WMParticipant> participants = new ArrayList<WMParticipant>();
            for(String name : groupNames) {
                participants.add(new WMParticipantImpl(name));
            }
            participants.add(new WMParticipantImpl(principal.getName()));
            Collection<WMWorkItemInstance> workItems = wapi.getWorkItemsFor(
                    participants, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);

            List<DocumentRef> alreadyIn = new ArrayList<DocumentRef>();
            Set<String> pids = new HashSet<String>();
            for(WMWorkItemInstance workItem : workItems) {
                pids.add(workItem.getProcessInstance().getId());
            }
            Map<String, List<DocumentModel>> pidDmMap = getDocumentModelsForPids(
                    wDoc, pids);
            for(WMWorkItemInstance workItem : workItems) {
                // Check if the user has an action to perform.
                WorkflowDocumentSecurityPolicy policy = wDocSecuPolicy.getWorkflowDocumentSecurityPolicyFor(workItem.getProcessInstance().getName());
                if (policy != null) {
                    boolean canManage = policy.selectThisItem(workItem);
                    if (!canManage) {
                        continue;
                    }
                }

                String authorName = workItem.getProcessInstance().getAuthorName();
                String workflowType = workItem.getProcessInstance().getName();

                for (DocumentModel dm : pidDmMap.get(workItem.getProcessInstance().getId())) {
                    DocumentRef docRef = dm.getRef();
                    if (alreadyIn.contains(docRef)) {
                        continue;
                    }

                    String title = (String) dm.getProperty("dublincore",
                            "title");
                    if (title == null) {
                        title = String.valueOf(docRef.hashCode());
                    }
                    DashBoardItemImpl dashBoardItem = new DashBoardItemImpl(
                            workItem, dm, title);

                    dashBoardItem.setAuthorName(authorName);
                    dashBoardItem.setWorkflowType(workflowType);
                    dashboardItems.add(dashBoardItem);

                    // Do not duplicate in the future.
                    alreadyIn.add(docRef);
                }
            }
        } catch (WMWorkflowException we) {
            throw EJBExceptionHandler.wrapException(we);
        } catch (ClientException ce) {
            throw EJBExceptionHandler.wrapException(ce);
        }
        return dashboardItems;
    }

    private Map<String, List<DocumentModel>> getDocumentModelsForPids(
            WorkflowDocumentRelationManager wDoc, Set<String> pids)
            throws ClientException {
        Map<String, List<String>> pidUuidsList = wDoc.getDocumentModelsPids(pids);
        Set<String> uuids = new HashSet<String>();
        for(Map.Entry<String, List<String>> entry : pidUuidsList.entrySet()) {
            for(String s: entry.getValue()) {
                uuids.add(s);
            }
        }
        DocumentModelList documentModelList = getDocumentModelListForIds(uuids);
        Map<String, DocumentModel> dmMap = new HashMap<String, DocumentModel>();
        for(DocumentModel dm : documentModelList) {
            dmMap.put(dm.getId(), dm);
        }
        Map<String, List<DocumentModel>> pidDmMap = new HashMap<String, List<DocumentModel>>();
        for(String pid : pidUuidsList.keySet()) {
            List<String> dmIds = pidUuidsList.get(pid);
            List<DocumentModel> dms = new ArrayList<DocumentModel>();
            for(String id : dmIds) {
                dms.add(dmMap.get(id));
            }
            pidDmMap.put(pid, dms);
        }
        return pidDmMap;
    }

    private DocumentModelList getDocumentModelListForIds(Set<String> uuids)
            throws ClientException {
        if(uuids == null || uuids.isEmpty()) {
            return new DocumentModelListImpl();
        }
        DocumentRef[] refs = new DocumentRef[uuids.size()];
        int i = 0;
        for (String uuid : uuids) {
            refs[i++] = new IdRef(uuid);
        }
        return documentManager.getDocuments(refs);
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
    }

    protected DocumentRef getDocumentRefForItem(String itemId) {
        DocumentRef docRef = null;

        if (dashboardItems == null) {
            return docRef;
        }

        for (DashBoardItem item : dashboardItems) {
            if (item.getId().equals(itemId)) {
                docRef = item.getDocRef();
                log.debug("Found docRef for dashboardEnabled id=" + itemId);
                break;
            } else {
                log.error("Didn't find docRef for dashboardEnabled id="
                        + itemId);
            }
        }
        return docRef;
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
                params = new Object[] { location, templates};
        } else if (BOARD_LATEST_PUBLISHED.equals(name)) {
            params = new Object [] {location};
        } else if (BOARD_WORKSPACES.equals(name)) {
            params = new Object[] {templates};
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
            return queryModelActions.get(qmName).getResultsProvider(params,
                    sortInfo);
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
        dashboardItems = null;
        eventContext.remove("dashboardActions_dashboardItems");
        return null;
    }

    public String refreshDocumentProcessItems() throws ClientException {
        documentProcessItems = null;
        eventContext.remove("dashboardActions_documentProcessItems");
        return null;
    }

    public String doSearch() throws ClientException {
        return null;
    }

    public SortInfo getSortInfo() {
        return sortInfo;
    }
}
