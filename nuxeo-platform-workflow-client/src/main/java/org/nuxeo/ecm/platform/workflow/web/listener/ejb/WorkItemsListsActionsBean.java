/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: WorkItemsListsActionsBean.java 28960 2008-01-11 13:37:02Z tdelprat $
 */

package org.nuxeo.ecm.platform.workflow.web.listener.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListEntry;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListsManager;
import org.nuxeo.ecm.platform.workflow.web.adapter.ProcessModel;
import org.nuxeo.ecm.platform.workflow.web.api.WorkItemsListsActions;
import org.nuxeo.ecm.platform.workflow.web.api.WorkflowBeansDelegate;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * Seam listener that deals with work items lists.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("workItemsListsActions")
@Scope(CONVERSATION)
public class WorkItemsListsActionsBean extends InputController implements
        WorkItemsListsActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WorkItemsListsActionsBean.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected WebActions webActions;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true, required = false)
    protected ProcessModel reviewModel;

    @In(create = true)
    protected WorkflowBeansDelegate workflowBeansDelegate;

    protected List<SelectItem> userWorkItemsListsItems;

    // User UI input
    // Used for the new work items list creation
    public String newWorkItemsListName;

    // User UI input
    // Used while loading or deleting a given work items list
    public String workItemsListsEntrySelectionName;

    @Create
    public void init() {
        Events.instance().raiseEvent(EventNames.WF_INIT);
    }

    @Factory(value = "userWorkItemsListsItems", scope = EVENT)
    public List<SelectItem> computeWorkItemsListsMap()
            throws WorkItemsListException {

        if (userWorkItemsListsItems == null) {
            userWorkItemsListsItems = new ArrayList<SelectItem>();

            String processName = reviewModel.getProcessInstanceName();
            String participantName = currentUser.getName();

            WorkItemsListsManager wiLists = workflowBeansDelegate.getWorkItemsLists();
            List<WorkItemsListEntry> entries = wiLists.getWorkItemListsFor(
                    participantName, processName);

            for (WorkItemsListEntry entry : entries) {
                String label = entry.getName();
                String id = String.valueOf(entry.getEntryId());
                userWorkItemsListsItems.add(new SelectItem(id,
                        (label != null ? label : id)));
            }
        }
        return userWorkItemsListsItems;
    }

    private String getValueFor(ActionEvent event, String paramName) {
        String value = null;
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            Map<String, String> map = context.getExternalContext().getRequestParameterMap();
            value = map.get(paramName);
        } catch (Exception e) {

        }
        return value;
    }

    public void invalidateWorkItemsListsMap() throws WorkItemsListException {
        userWorkItemsListsItems = null;
    }

    public String createWorkItemsList() throws WorkItemsListException {

        WorkItemsListsManager wiLists = workflowBeansDelegate.getWorkItemsLists();

        String participantName = currentUser.getName();
        if (participantName == null) {
            throw new WorkItemsListException(
                    "No participant name in context... Cancelling...");
        }

        String pid = reviewModel.getProcessInstanceId();
        if (pid == null) {
            throw new WorkItemsListException(
                    "No process to load the work items list against... Cancelling !");
        }

        String name = newWorkItemsListName;
        if (name == null) {
            throw new WorkItemsListException(
                    "No name submitted for the creation of the work items list... Cancelling");
        }

        // Check that the name has not been choosen yet by user
        if (wiLists.getWorkItemListEntryByName(participantName, name) == null) {
            wiLists.saveWorkItemsListFor(pid, participantName, name);
            Events.instance().raiseEvent(EventNames.WORK_ITEMS_LIST_ADDED);
            rebuildTabsList();
        } else {
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.workflow.workitem.lists.duplicate.name"));
            FacesMessages.instance().add(message);
        }
        newWorkItemsListName = null;
        return null;
    }

    public String deleteWorkItemsList() throws WorkItemsListException {

        WorkItemsListsManager wiLists = workflowBeansDelegate.getWorkItemsLists();

        if (workItemsListsEntrySelectionName != null) {
            try {
                wiLists.deleteWorkItemsListById(Integer.valueOf(workItemsListsEntrySelectionName));
            } catch (ClassCastException cce) {
                throw new WorkItemsListException(cce);
            }
        } else {
            throw new WorkItemsListException(
                    "No work item id specified for deletion...Cancelling !");
        }

        workItemsListsEntrySelectionName = null;

        Events.instance().raiseEvent(EventNames.WORK_ITEMS_LIST_REMOVED);

        rebuildTabsList();
        return null;
    }

    public String loadWorkItemsList() throws WorkItemsListException {

        WorkItemsListsManager wiLists = workflowBeansDelegate.getWorkItemsLists();

        String pid = reviewModel.getProcessInstanceId();
        if (pid == null) {
            throw new WorkItemsListException(
                    "No process to load the work items list against... Cancelling !");
        }

        int entryId;
        if (workItemsListsEntrySelectionName != null) {
            try {
                entryId = Integer.valueOf(workItemsListsEntrySelectionName);
            } catch (ClassCastException cce) {
                throw new WorkItemsListException(cce);
            }
        } else {
            throw new WorkItemsListException(
                    "No selected work items list selected... cancelling");
        }

        wiLists.restoreWorkItemsListFor(pid, entryId, false, true);

        Events.instance().raiseEvent(
                EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);

        rebuildTabsList();
        return null;
    }

    public String loadWorkItemsListMerging() throws WorkItemsListException {

        WorkItemsListsManager wiLists = workflowBeansDelegate.getWorkItemsLists();

        String pid = reviewModel.getProcessInstanceId();
        if (pid == null) {
            throw new WorkItemsListException(
                    "No process to load the work items list against... Cancelling !");
        }

        int entryId;
        if (workItemsListsEntrySelectionName != null) {
            try {
                entryId = Integer.valueOf(workItemsListsEntrySelectionName);
            } catch (ClassCastException cce) {
                throw new WorkItemsListException(cce);
            }
        } else {
            throw new WorkItemsListException(
                    "No selected work items list selected... cancelling");
        }

        wiLists.restoreWorkItemsListFor(pid, entryId, true, true);

        Events.instance().raiseEvent(
                EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);

        rebuildTabsList();
        return null;
    }

    public String getNewWorkItemsListName() {
        return newWorkItemsListName;
    }

    public void setNewWorkItemsListName(String newWorkItemsListName) {
        this.newWorkItemsListName = newWorkItemsListName;
    }

    public String getWorkItemsListsEntrySelectionName() {
        return workItemsListsEntrySelectionName;
    }

    public void setWorkItemsListsEntrySelectionName(String name) {
        this.workItemsListsEntrySelectionName = name;
    }

    protected void rebuildTabsList() {
        Action currentTab = webActions.getCurrentTabAction();
        webActions.resetTabList();
        webActions.setCurrentTabAction(currentTab);
    }

}
