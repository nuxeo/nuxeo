/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nicolas Ulrich
 *
 */

package org.nuxeo.ecm.platform.jbpm.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskListService;
import org.nuxeo.ecm.platform.jbpm.TaskList;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

@Name("jbpmTaskListActions")
@Scope(ScopeType.CONVERSATION)
public class JbpmTaskListActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient JbpmActions jbpmActions;

    @In(create = true)
    protected transient JbpmService jbpmService;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected String selectedListId;

    public String getSelectedListId() {
        return selectedListId;
    }

    public void setSelectedListId(String selectedListId) {
        this.selectedListId = selectedListId;
    }

    protected String listName;

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    @Factory(value = "availableLists", scope = ScopeType.EVENT)
    public List<TaskList> availableListsFactory() throws Exception {
        JbpmTaskListService service = Framework.getService(JbpmTaskListService.class);
        List<TaskList> lists = service.getTaskLists(documentManager);

        return lists;
    }

    public void createTaskList() throws Exception {

        if (listName == null || listName.equals("")) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.review.list.chooseaname"));
            return;
        }

        JbpmTaskListService service = Framework.getService(JbpmTaskListService.class);
        TaskList list = service.createTaskList(documentManager, listName);

        List<VirtualTaskInstance> virtualTasks = jbpmActions.getCurrentVirtualTasks();
        if (virtualTasks == null) {
            virtualTasks = new ArrayList<VirtualTaskInstance>();
        }

        for (VirtualTaskInstance virtualTaskInstance : virtualTasks) {
            list.addTask(virtualTaskInstance);
        }

        service.saveTaskList(documentManager, list);

        listName = null;

        facesMessages.add(
                StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("label.review.list.created"));
    }

    public void deleteTaskList() throws Exception {
        if (selectedListId == null) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.review.list.notselected"));
            return;
        }

        JbpmTaskListService service = Framework.getService(JbpmTaskListService.class);
        service.deleteTaskList(documentManager, selectedListId);

        facesMessages.add(
                StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("label.review.list.deleted"));
    }

    public void loadTaskList() throws Exception {
        if (selectedListId == null) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.review.list.notselected"));
            return;
        }

        ProcessInstance pi = jbpmActions.getCurrentProcess();

        if (pi != null && jbpmActions.getCanManageParticipants()) {

            List<VirtualTaskInstance> virtualTasks = jbpmActions.getCurrentVirtualTasks();
            if (virtualTasks == null) {
                virtualTasks = new ArrayList<VirtualTaskInstance>();
            }

            // Get TaskList Service
            JbpmTaskListService service = Framework.getService(JbpmTaskListService.class);
            TaskList list = service.getTaskList(documentManager, selectedListId);

            if (list == null) {
                return;
            }

            // Add all participant in the virtual tasks list
            for (VirtualTaskInstance task : list.getTasks()) {
                virtualTasks.add(task);
            }

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.list.added"));
        }
    }

}
