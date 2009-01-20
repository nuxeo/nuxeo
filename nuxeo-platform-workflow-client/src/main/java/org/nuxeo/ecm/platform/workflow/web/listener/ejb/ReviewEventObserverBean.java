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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: ReviewEventObserverBean.java 28960 2008-01-11 13:37:02Z tdelprat $
 */

package org.nuxeo.ecm.platform.workflow.web.listener.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;
import org.nuxeo.ecm.platform.workflow.web.api.DocumentTaskActions;
import org.nuxeo.ecm.platform.workflow.web.api.DocumentWorkflowActions;
import org.nuxeo.ecm.platform.workflow.web.api.ReviewEventObserver;
import org.nuxeo.ecm.platform.workflow.web.api.WorkItemsListsActions;
import org.nuxeo.ecm.platform.workflow.web.api.WorkflowBeansDelegate;
import org.nuxeo.ecm.webapp.dashboard.DashboardActions;

/**
 * Review event observer.
 * <p>
 * Seam component that deals with events which requires invalidations of review
 * related seam listeners variables.
 * <p>
 * This Seam component do exist because Seam seems to have some problems when
 * the sender and recipient is the same stateful bean (not the case with POJO)
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("reviewEventObserver")
@Scope(CONVERSATION)
public class ReviewEventObserverBean implements ReviewEventObserver {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ReviewEventObserverBean.class);

    protected transient NavigationContext navigationContext;

    protected transient WorkflowBeansDelegate workflowBeansDelegate;

    protected transient DocumentWorkflowActions documentWorkflowActions;

    protected transient DocumentTaskActions documentTaskActions;

    protected transient WorkItemsListsActions workItemsListsActions;

    protected transient DashboardActions dashboardActions;

    @Observer( value={ EventNames.WF_INIT }, create=true, inject=false)
    @BypassInterceptors
    public void init() {
        log.debug("WF Seam Event Observer created");
    }

    @Observer( value={ EventNames.DOCUMENT_CHANGED }, create=false)
    public void updateCurrentLevelAfterDocumentChanged()
            throws WMWorkflowException {
        if (getDocumentWorkflowActions() != null) {
            getDocumentWorkflowActions().updateCurrentLevelAfterDocumentChanged();
        }
        // XXX TD : Not usefull, DashBoard already listen to WF events !
        // Invalidate user dashboard items
        /*
        if (dashboardActions != null) {
            try {
                dashboardActions.invalidateDashboardItems();
            } catch (ClientException e) {
                throw new WMWorkflowException(e.getMessage());
            }
        }*/
    }

    @Observer(value = {EventNames.DOCUMENT_SELECTION_CHANGED}, create = false)
    @BypassInterceptors
    public void onDocumentChanged() {
        invalidateContextVariables();
        invalidateTasksContextVariables();
        try {
            invalidateWorkItemsListsMap();
        } catch (WorkItemsListException e) {
            log.error("Error during WorkItems invalidation", e);
        }
    }

    @Observer(value={EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED}, create=false)
    public void updateDocumentRights() throws WMWorkflowException {
        if (getDocumentWorkflowActions() != null) {
            // check if workflow is started before updating rights
            if (getDocumentWorkflowActions().isWorkflowStarted()) {
                getDocumentWorkflowActions().updateDocumentRights();
            }
        }
    }

    @Observer( value={ EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_STOP, EventNames.WORKFLOW_TASK_REJECTED,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.WORKFLOW_TASK_REMOVED }, create=false)
    public void invalidateContextVariables() {
        if (getDocumentWorkflowActions() != null) {
            getDocumentWorkflowActions().invalidateContextVariables();
        }
    }

    @Observer( value={ EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_START, EventNames.WORKFLOW_TASK_STOP,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            EventNames.WORKFLOW_TASK_REMOVED, EventNames.WORKFLOW_TASK_REJECTED }, create=false)
    public void invalidateTasksContextVariables() {
        if (getDocumentTaskActions() != null) {
            getDocumentTaskActions().invalidateContextVariables();
        }
    }

    @Observer( value={ EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_STOP, EventNames.WORKFLOW_TASK_REMOVED,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.WORK_ITEMS_LIST_ADDED,
            EventNames.WORK_ITEMS_LIST_REMOVED }, create=false)
    public void invalidateWorkItemsListsMap() throws WorkItemsListException {
        if (getWorkItemsListsActions() != null) {
            getWorkItemsListsActions().invalidateWorkItemsListsMap();
        }
    }

    @Observer( value={ EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_COMPLETED, EventNames.WORKFLOW_TASK_STOP,
            EventNames.WORKFLOW_TASK_REMOVED,
            EventNames.WORKFLOW_TASK_REJECTED,
            EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            EventNames.WORK_ITEMS_LIST_LOADED }, create=false)
    public void unlockDocument() throws WMWorkflowException {
        // :XXX: Here automatically unlock the document to avoid deadlocks. We
        // could certainly do something better than that though.
        if (getWorkflowBeansDelegate() != null) {
            try {
                DocumentModel dm = getCurrentDocument();
                if (dm != null) {
                    WorkflowDocumentSecurityManager mgr = getWorkflowBeansDelegate().getWFSecurityManagerBean();
                    if (mgr != null) {
                        mgr.unlockDocument(dm.getRef());
                    }
                }
            } catch (ClientException ce) {
                log.error(ce.getMessage());
            }
        }
    }

    private DocumentModel getCurrentDocument() {
        return getNavigationContext().getCurrentDocument();
    }

    @Observer( value={ EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED }, create=false)
    public void updateDocumentLifecycle() throws ClientException {
        getCurrentDocument().prefetchCurrentLifecycleState(null);
    }

    protected NavigationContext getNavigationContext() {
        if (navigationContext == null) {
            navigationContext = (NavigationContext) Component.getInstance("navigationContext", ScopeType.CONVERSATION);
        }
        return navigationContext;
    }

    protected WorkflowBeansDelegate getWorkflowBeansDelegate() {
        if (workflowBeansDelegate == null) {
            workflowBeansDelegate = (WorkflowBeansDelegate) Component.getInstance("workflowBeansDelegate");
        }
        return workflowBeansDelegate;
    }

    protected DocumentWorkflowActions getDocumentWorkflowActions() {
        if (documentWorkflowActions == null) {
            documentWorkflowActions = (DocumentWorkflowActions) Component.getInstance("documentWorkflowActions");
        }
        return documentWorkflowActions;
    }

    protected DocumentTaskActions getDocumentTaskActions() {
        if (documentTaskActions == null) {
            documentTaskActions = (DocumentTaskActions) Component.getInstance("documentTaskActions");
        }
        return documentTaskActions;
    }

    protected WorkItemsListsActions getWorkItemsListsActions() {
        if (workItemsListsActions == null) {
            workItemsListsActions = (WorkItemsListsActions) Component.getInstance("workItemsListsActions");
        }
        return workItemsListsActions;
    }

    protected DashboardActions getDashboardActions() {
        if (dashboardActions == null) {
            dashboardActions = (DashboardActions) Component.getInstance("dashboardActions");
        }
        return dashboardActions;
    }

}
