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

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
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
import org.nuxeo.ecm.platform.workflow.web.api.ejb.remote.ReviewEventObserverRemote;
import org.nuxeo.ecm.platform.workflow.web.listener.ejb.local.ReviewEventObserverLocal;
import org.nuxeo.ecm.webapp.dashboard.DashboardActions;

/**
 * Review event observer.
 *
 * <p>
 * Seam component that deals with events which requires invalidations of review
 * related seam listeners variables.
 * </p>
 *
 * <p>
 * This Seam component do exist because Seam seems to have some problems when
 * the sender and recipient is the same stateful bean (not the case with POJO)
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("reviewEventObserver")
@Scope(CONVERSATION)
public class ReviewEventObserverBean implements ReviewEventObserver {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ReviewEventObserverBean.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WorkflowBeansDelegate workflowBeansDelegate;

    @In(required = false)
    protected transient DocumentWorkflowActions documentWorkflowActions;

    @In(required = false)
    protected transient DocumentTaskActions documentTaskActions;

    @In(required = false)
    protected transient WorkItemsListsActions workItemsListsActions;

    @In(required = false)
    protected transient DashboardActions dashboardActions;

    @Observer( value={ EventNames.WF_INIT }, create=true, inject=false)
    public void init()
    {
    	log.debug("WF Seam Event Observer created");
    }

    @Observer( value={ EventNames.DOCUMENT_CHANGED }, create=false)
    public void updateCurrentLevelAfterDocumentChanged()
            throws WMWorkflowException {
        if (documentWorkflowActions != null) {
            documentWorkflowActions.updateCurrentLevelAfterDocumentChanged();
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

    @Observer(value={EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED}, create=false)
    public void updateDocumentRights() throws WMWorkflowException {
        if (documentWorkflowActions != null) {
            // check if workflow is started before updating rights
            if (documentWorkflowActions.isWorkflowStarted()) {
                documentWorkflowActions.updateDocumentRights();
            }
        }
    }

    @Observer( value={ EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_STOP, EventNames.WORKFLOW_TASK_REJECTED,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.WORKFLOW_TASK_REMOVED }, create=false)
    public void invalidateContextVariables() {
        if (documentWorkflowActions != null) {
            documentWorkflowActions.invalidateContextVariables();
        }
    }

    @Observer( value={ EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_START, EventNames.WORKFLOW_TASK_STOP,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED,
            EventNames.WORKFLOW_TASK_REMOVED, EventNames.WORKFLOW_TASK_REJECTED }, create=false)
    public void invalidateTasksContextVariables() {
        if (documentTaskActions != null) {
            documentTaskActions.invalidateContextVariables();
        }
    }

    @Observer( value={ EventNames.WORKFLOW_ENDED, EventNames.WORKFLOW_NEW_STARTED,
            EventNames.WORKFLOW_TASK_STOP, EventNames.WORKFLOW_TASK_REMOVED,
            EventNames.WORKFLOW_TASK_COMPLETED,
            EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.WORK_ITEMS_LIST_ADDED,
            EventNames.WORK_ITEMS_LIST_REMOVED }, create=false)
    public void invalidateWorkItemsListsMap() throws WorkItemsListException {
        if (workItemsListsActions != null) {
            workItemsListsActions.invalidateWorkItemsListsMap();
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
        if (workflowBeansDelegate != null) {
            try {
                DocumentModel dm = getCurrentDocument();
                if (dm != null) {
                    WorkflowDocumentSecurityManager mgr = workflowBeansDelegate.getWFSecurityManagerBean();
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
        return navigationContext.getCurrentDocument();
    }

    @Observer( value={ EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED }, create=false)
    public void updateDocumentLifecycle() throws ClientException {
        getCurrentDocument().prefetchCurrentLifecycleState(null);
    }

}
