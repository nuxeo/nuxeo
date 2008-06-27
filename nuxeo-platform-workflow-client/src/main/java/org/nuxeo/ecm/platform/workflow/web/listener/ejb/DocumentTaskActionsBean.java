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
 * $Id:ContentHistoryActionsBean.java 4487 2006-10-19 22:27:14Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.web.listener.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.audit.api.AuditEventTypes;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMWorkItemInstanceImpl;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventCategories;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventTypes;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityConstants;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.platform.workflow.web.adapter.ProcessModel;
import org.nuxeo.ecm.platform.workflow.web.api.DocumentTaskActions;
import org.nuxeo.ecm.platform.workflow.web.api.WorkflowBeansDelegate;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;

/**
 * Document task actions bean.
 * <p>
 * Deals with tasks related to a particular document.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */

@Name("documentTaskActions")
@Scope(CONVERSATION)
public class DocumentTaskActionsBean extends InputController implements
        DocumentTaskActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentTaskActionsBean.class);

    @In
    protected transient Context eventContext;

    @In(create = true)
    protected WorkflowBeansDelegate workflowBeansDelegate;

    @In(create = true, required = false)
    protected ProcessModel reviewModel;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true)
    protected PrincipalListManager principalListManager;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected List<WMWorkItemInstance> documentTasks;

    protected Boolean canManageWorkflow;

    // Parameters coming from the UI

    public String selectedTaskDirective;

    public Date selectedTaskDueDate;

    public String selectedTaskInsertionLevel;

    protected static enum INSERTION_LEVELS {
        current, below,
    }

    public String userComment;

    public String taskActionComment;

    @RequestParameter("workflowTaskInstanceId")
    protected String workflowTaskInstanceId;

    public String getUserComment() {
        return userComment;
    }

    public void setUserComment(String userComment) {
        this.userComment = userComment;
    }

    // @Destroy
    public void destroy() {
        log.debug("Removing SEAM component...");
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public Principal getPrincipal() throws WMWorkflowException {
        return currentUser;
    }

    public String assignTask(WMWorkItemInstance taskInstance,
            String principalName, boolean isGroup) throws WMWorkflowException {

        String destination = "document_review";

        try {
            WAPI wapi = workflowBeansDelegate.getWAPIBean();

            wapi.assignWorkItem(taskInstance.getId(), new WMParticipantImpl(
                    principalName));

        } catch (WMWorkflowException we) {
            // :XXX
        }

        // Notify
        Events.instance().raiseEvent(
                EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);
        String comment = "=> " + principalName + " ( " + userComment + " )";

        Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();

        eventInfo.put("recipients", isGroup ? "group:" : "user:"
                + principalName);
        // directive
        eventInfo.put("directive", taskInstance.getDirective());

        notifyEvent(WorkflowEventTypes.WORKFLOW_TASK_ASSIGNED, comment,
                reviewModel.getProcessInstanceName(), eventInfo);
        Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);

        return destination;
    }

    public String createTaskFor() throws WMWorkflowException {

        List<String> selectedUsers = principalListManager.getSelectedUsers();

        if (selectedUsers == null || selectedUsers.isEmpty()) {
            return null;
        }

        if (selectedUsers.isEmpty()) {
            return null;
        }

        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        String pid = reviewModel.getProcessInstanceId();
        String pname = reviewModel.getProcessInstanceName();
        String directive = selectedTaskDirective;
        Date dueDate = selectedTaskDueDate;
        String comment = userComment;
        WMWorkItemDefinition tdef = getWorkflowTaskDefinition(pid);

        String chosenInsertionLevel = selectedTaskInsertionLevel;
        int insertionLevel;
        boolean deleteCurrent = false;
        if (INSERTION_LEVELS.current.name().equals(chosenInsertionLevel)) {
            insertionLevel = reviewModel.getReviewCurrentLevel();
            deleteCurrent = true;
        } else {
            // default to below
            insertionLevel = reviewModel.getReviewCurrentLevel() + 1;
        }

        // update level for all tasks below
        int insertionNumber = selectedUsers.size();
        Map<String, Serializable> insertionProps = new HashMap<String, Serializable>();

        // XXX Appends if variable has been already invalidated...
        if (documentTasks == null) {
            computeDocumentTasks();
        }

        if (documentTasks == null) {
            throw new WMWorkflowException(
                    "Problem while computing tasks...Cannot create new tasks");
        }

        for (WMWorkItemInstance wi : documentTasks) {
            boolean updateOrder = true;
            int formerOrder = wi.getOrder();
            // delete task at current level
            if (insertionLevel == formerOrder && deleteCurrent) {
                updateOrder = false;
                wapi.removeWorkItem(wi.getId());
            }
            if (updateOrder && formerOrder >= insertionLevel) {
                insertionProps.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER,
                        formerOrder + insertionNumber);
                wapi.updateWorkItemAttributes(wi.getId(), insertionProps);
            }
        }

        for (String user : selectedUsers) {

            String principalName = null;
            boolean isGroup;
            try {
                if (principalListManager.getPrincipalType(user) == PrincipalListManager.USER_TYPE) {
                    NuxeoPrincipal nxPrincipal = userManager.getPrincipal(user);
                    if (nxPrincipal != null) {
                        principalName = nxPrincipal.getName();
                    }
                    isGroup = false;
                } else {
                    NuxeoGroup nxGroup = userManager.getGroup(user);
                    if (nxGroup != null) {
                        principalName = nxGroup.getName();
                    }
                    isGroup = true;
                }
            } catch (ClientException e) {
                continue;
            }

            if (principalName != null) {

                // Create a new task
                Map<String, Serializable> props = getTaskProperties(
                        insertionLevel, directive, dueDate, comment);
                WMWorkItemInstance ti = wapi.createWorkItem(pid, tdef.getId(),
                        props);

                notifyEvent(WorkflowEventTypes.WORKFLOW_TASK_CREATED,
                        userComment, pname, null);

                // Assign the task to the selected user.
                assignTask(ti, principalName, isGroup);

                // Start task
                startTask(ti.getId());
            }

            // For the moment we don't put them all at the same review level
            // because of UI limitation.
            if (reviewModel.getReviewType().equals(
                    WorkflowConstants.WORKFLOW_REVIEW_TYPE_SERIE)) {
                insertionLevel += 1;
            }

        }

        if (deleteCurrent) {
            // update rights
            Events.instance().raiseEvent(
                    EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);
        }

        Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_START);
        Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);

        // Invalidate context
        cleanContext();
        invalidateContextVariables();

        // Reset the selected users and filter
        principalListManager.resetSelectedUserList();
        principalListManager.resetSearchFilter();

        rebuildTabsList();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(
                        "label.review.added.reviewer"));

        return null;
    }

    public void cleanContext() {
        selectedTaskDirective = null;
        selectedTaskDueDate = null;
        selectedTaskInsertionLevel = null;
        userComment = null;
        taskActionComment = null;
    }

    /**
     * Prepares task properties from user entries and context.
     *
     * @param directive TODO
     * @param dueDate TODO
     * @param comment TODO
     *
     * @return a map from string to serializable.
     */
    public Map<String, Serializable> getTaskProperties(int order,
            String directive, Date dueDate, String comment) {
        Map<String, Serializable> props = new HashMap<String, Serializable>();

        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_DIRECTIVE, directive);
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_DUE_DATE, dueDate);
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_COMMENT, comment);
        props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER, order);

        return props;
    }

    protected WMWorkItemDefinition getWorkflowTaskDefinition(String pid)
            throws WMWorkflowException {
        WMWorkItemDefinition tdef;
        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        Set<WMWorkItemDefinition> tdefs = wapi.getWorkItemDefinitionsFor(pid);
        if (tdefs.isEmpty()) {
            tdef = null;
        } else {
            tdef = tdefs.iterator().next();
        }
        return tdef;
    }

    public String endTask(String taskId) throws WMWorkflowException {
        return endTask(taskId, null);
    }

    public String endTask(String taskId, String transition)
            throws WMWorkflowException {
        if (taskId == null) {
            log.error("Task identifier is null. Cancelling....");
            return null;
        }

        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        String comment = "=> " + currentUser.getName() + " ( "
                + taskActionComment + " )";

        notifyEvent(WorkflowEventTypes.WORKFLOW_TASK_ENDED, comment,
                reviewModel.getProcessInstanceName(), null);

        WMWorkItemInstance wi = wapi.endWorkItem(taskId, transition);

        // If task has been rejected in the past.
        if (wi.isRejected()) {
            Map<String, Serializable> variables = new HashMap<String, Serializable>();
            variables.put(WorkflowConstants.WORKFLOW_TASK_PROP_REJECTED, false);
            wapi.updateWorkItemAttributes(taskId, variables);
        }

        if (reviewModel.getReviewType().equals(
                WorkflowConstants.WORKFLOW_REVIEW_TYPE_SERIE)) {

            Map<String, Serializable> vars = new HashMap<String, Serializable>();
            vars.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL,
                    getNextReviewLevel());
            vars.put(WorkflowConstants.WORKFLOW_FORMER_REVIEW_LEVEL,
                    reviewModel.getReviewCurrentLevel());
            wapi.updateProcessInstanceAttributes(
                    reviewModel.getProcessInstanceId(), vars);
        }

        Events.instance().raiseEvent(
                EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);
        Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_STOP);
        Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);
        Events.instance().raiseEvent(EventNames.DOCUMENT_SELECTION_CHANGED);
        Events.instance().raiseEvent(
                EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED);

        // Here the process is over. Let's redirect to the default view.
        String pid = reviewModel.getProcessInstanceId();
        WMProcessInstance pi = wapi.getProcessInstanceById(pid, null);
        if (pi == null
                || pi.getState().equals(
                        WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE)) {
            try {
                webActions.resetTabList();
                DocumentModel currentDoc = getCurrentDocument();
                if (!checkPermissions(currentDoc)) {
                    // FIXME error while navigating to Home, problem with
                    // CoreSession
                    // return navigationContext.goHome();
                    return "home";
                } else {
                    // NXP-1998: don't redirect to the document when it's
                    // deleted
                    String lifeCyle = currentDoc.getCurrentLifeCycleState();
                    if ("deleted".equals(lifeCyle)) {
                        return "home";
                    } else {
                        return navigationContext.navigateToDocument(currentDoc);
                    }
                }
            } catch (ClientException ce) {
                ce.printStackTrace();
            }
        }

        cleanContext();
        invalidateContextVariables();

        rebuildTabsList();
        return null;
    }

    @Factory(value = "documentTasks", scope = EVENT)
    public List<WMWorkItemInstance> computeDocumentTasks()
            throws WMWorkflowException {

        if (documentTasks == null) {

            log.debug("RECOMPUTE TASK LIST.........................");

            if (getCurrentDocument() != null) {
                WAPI wapi = workflowBeansDelegate.getWAPIBean();
                WorkflowDocumentRelationManager wDoc = workflowBeansDelegate.getWorkflowDocumentBean();
                String[] instanceIds = wDoc.getWorkflowInstanceIdsFor(getCurrentDocument().getRef());
                for (String instanceId : instanceIds) {
                    // Fetch all tasks
                    Collection<WMWorkItemInstance> taskInstances = wapi.listWorkItems(
                            instanceId, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);

                    // :XXX: Temporarily for DEBUG
                    // We need an object taking care of task list formatting.
                    // i.e : map from int -> list of task instance to represent
                    // levels.
                    // Here we only support one item per level in case of
                    // ordered
                    // review
                    List<WMWorkItemInstance> sortedTaskInstances = new ArrayList<WMWorkItemInstance>();
                    for (WMWorkItemInstance wTask : taskInstances) {
                        // Do not show cancelled task (i.e : user removed it)
                        if (wTask.isCancelled()) {
                            continue;
                        }
                        boolean inserted = false;
                        for (WMWorkItemInstance sTask : sortedTaskInstances) {
                            int index = sortedTaskInstances.indexOf(sTask);
                            if (wTask.getOrder() < sTask.getOrder()
                                    || (wTask.getOrder() == sTask.getOrder() && wTask.getCreationDate().before(
                                            sTask.getCreationDate()))) {
                                sortedTaskInstances.add(index, wTask);
                                inserted = true;
                                break;
                            }
                        }
                        if (!inserted) {
                            sortedTaskInstances.add(wTask);
                        }
                    }
                    documentTasks = sortedTaskInstances;
                }
            }
        }
        // Events.instance().raiseEvent(EventNames.WORKFLOW_TASKS_COMPUTED);
        return documentTasks;
    }

    public void removeTask(String taskId) throws WMWorkflowException {

        if (taskId == null) {
            log.error("taskId is null. Cancelling....");
            return;
        }

        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        WorkflowDocumentSecurityManager wsecu = workflowBeansDelegate.getWFSecurityManagerBean();

        // ACL update
        // :XXX: Should be done within the assignment handler.
        WMWorkItemInstance workflowTaskInstance = wapi.getWorkItemById(taskId);
        String pid = workflowTaskInstance.getProcessInstance().getId();
        wsecu.denyPrincipal(getCurrentDocument().getRef(),
                workflowTaskInstance.getParticipantName(),
                WorkflowDocumentSecurityConstants.WORKFLOW_PARTICIPANT, pid);
        Events.instance().raiseEvent(
                EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);

        wapi.removeWorkItem(taskId);

        // :XXX: move this somewhere else...
        if (reviewModel.getReviewType().equals(
                WorkflowConstants.WORKFLOW_REVIEW_TYPE_SERIE)) {

            // Ensure ordering. Fill up blanks.
            Collection<WMWorkItemInstance> allTasks = wapi.listWorkItems(
                    reviewModel.getProcessInstanceId(),
                    WMWorkItemState.WORKFLOW_TASK_STATE_ALL);

            // Sort out tasks
            List<WMWorkItemInstance> allSortedTasks = new ArrayList<WMWorkItemInstance>();
            for (WMWorkItemInstance wii : allTasks) {
                // Do not show cancelled task (i.e : user remove it)
                if (wii.isCancelled()) {
                    continue;
                }
                boolean inserted = false;
                for (WMWorkItemInstance sTask : allSortedTasks) {
                    // Do not include cancelled tasks here.
                    int index = allSortedTasks.indexOf(sTask);
                    if (wii.getOrder() < sTask.getOrder()
                            || wii.getCreationDate().before(
                                    sTask.getCreationDate())) {
                        allSortedTasks.add(index, wii);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    allSortedTasks.add(wii);
                }
            }

            // fill blanks.
            Map<String, Serializable> props = new HashMap<String, Serializable>();
            for (int i = 0; i < allSortedTasks.size() - 1; i++) {
                if ((allSortedTasks.get(i + 1).getOrder() - allSortedTasks.get(
                        i).getOrder()) <= 1) {
                    continue;
                } else {
                    String wiiId = allSortedTasks.get(i + 1).getId();
                    int newOrder = allSortedTasks.get(i).getOrder() + 1;

                    props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER,
                            newOrder);
                    wapi.updateWorkItemAttributes(wiiId, props);

                    // Only id and order matters here.
                    WMWorkItemInstance updated = new WMWorkItemInstanceImpl(
                            wiiId, newOrder);
                    allSortedTasks.remove(i + 1);
                    allSortedTasks.add(i + 1, updated);
                }
            }
        }

        String comment = "=> " + currentUser.getName() + " ( "
                + taskActionComment + " )";

        notifyEvent(WorkflowEventTypes.WORKFLOW_TASK_REMOVED, comment,
                reviewModel.getProcessInstanceName(), null);
        Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_REMOVED);
        Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);
        Events.instance().raiseEvent(EventNames.DOCUMENT_SELECTION_CHANGED);
        Events.instance().raiseEvent(
                EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED);
    }

    public String removeOneTask(ActionEvent event) {
        String value = getValueFor(event, "workflowTaskInstanceId");
        if (value != null) {
            try {
                removeTask(value);
                cleanContext();
                invalidateContextVariables();
            } catch (WMWorkflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        rebuildTabsList();
        return null;
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

    public String getSelectedTaskDirective() {
        return selectedTaskDirective;
    }

    public void setSelectedTaskDirective(String selectedTaskDirective) {
        this.selectedTaskDirective = selectedTaskDirective;
    }

    public Date getSelectedTaskDueDate() {
        if (selectedTaskDueDate == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            selectedTaskDueDate = calendar.getTime();
        }
        return selectedTaskDueDate;
    }

    public void setSelectedTaskDueDate(Date selectedTaskDueDate) {
        this.selectedTaskDueDate = selectedTaskDueDate;
    }

    public String getSelectedTaskInsertionLevel() {
        if (selectedTaskInsertionLevel == null) {
            // default to "below"
            return INSERTION_LEVELS.below.name();
        }
        return selectedTaskInsertionLevel;
    }

    public void setSelectedTaskInsertionLevel(String selectedTaskInsertionLevel) {
        this.selectedTaskInsertionLevel = selectedTaskInsertionLevel;
    }

    protected void notifyEvent(String eventId, String comment, String category,
            Map<String, Serializable> eventInfo) throws WMWorkflowException {

        DocumentMessageProducer producer = workflowBeansDelegate.getDocumentMessageProducer();
        Map<String, Serializable> props = eventInfo == null ? new HashMap<String, Serializable>()
                : eventInfo;

        if (documentTasks != null) {
            StringBuilder recipients = new StringBuilder();
            for (WMWorkItemInstance instance : documentTasks) {
                String participantName = instance.getParticipantName();
                try {
                    boolean isUser = principalListManager.getPrincipalType(participantName) == PrincipalListManager.USER_TYPE;
                    participantName = (isUser ? "user:" : "group:")
                            + participantName;
                } catch (ClientException e) {
                    // TODO Auto-generated catch block
                    // e.printStackTrace(); }
                }
                int nextReviewLevel = reviewModel.getReviewCurrentLevel() + 1;
                if (nextReviewLevel == instance.getOrder()) {
                    recipients.append(participantName + "|");
                }
            }
            String recipient = null;
            if (recipients.toString().trim().length() > 0) {
                recipient = recipients.toString().substring(0,
                        recipients.lastIndexOf("|"));
            }

            props.put("recipients", recipient);
        }

        props.put("workflowType", reviewModel.getProcessInstanceName());

        String currentLifeCycleState;
        try {
            currentLifeCycleState = getCurrentDocument().getCurrentLifeCycleState();
        } catch (ClientException ce) {
            throw new WMWorkflowException(ce.getMessage());
        }
        props.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);

        DocumentModel dm = getCurrentDocument();
        CoreEvent event = new CoreEventImpl(eventId, dm, props,
                workflowBeansDelegate.getWAPIBean().getParticipant(),
                category != null ? category
                        : WorkflowEventCategories.EVENT_WORKFLOW_CATEGORY,
                comment);

        DocumentMessage msg = new DocumentMessageImpl(dm, event);
        producer.produce(msg);
    }

    public String startTask(String taskIdentifier) throws WMWorkflowException {
        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        wapi.startWorkItem(taskIdentifier);

        notifyEvent(WorkflowEventTypes.WORKFLOW_TASK_STARTED, userComment,
                reviewModel.getProcessInstanceName(), null);
        Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_START);
        Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);
        Events.instance().raiseEvent(EventNames.DOCUMENT_SELECTION_CHANGED);
        Events.instance().raiseEvent(
                EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED);

        // Invalidate context
        cleanContext();
        rebuildTabsList();
        return null;
    }

    public void rejectTask(String taskId) throws WMWorkflowException {

        if (taskId == null) {
            log.error("taskId is null. Cancelling....");
            return;
        }
        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        wapi.rejectWorkItem(taskId);

        // Update workflow current level
        // :FIXME: hardcoded here.
        if (reviewModel.getReviewType().equals(
                WorkflowConstants.WORKFLOW_REVIEW_TYPE_SERIE)) {
            Map<String, Serializable> vars = new HashMap<String, Serializable>();
            vars.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL,
                    getFormertReviewLevel());
            vars.put(WorkflowConstants.WORKFLOW_FORMER_REVIEW_LEVEL,
                    reviewModel.getReviewCurrentLevel());
            wapi.updateProcessInstanceAttributes(
                    reviewModel.getProcessInstanceId(), vars);
        }

        String comment = "=> " + currentUser.getName() + " ( "
                + taskActionComment + " )";

        notifyEvent(WorkflowEventTypes.WORKFLOW_TASK_REJECTED, comment,
                reviewModel.getProcessInstanceName(), null);
        Events.instance().raiseEvent(
                EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);
        Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_REJECTED);
        Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);
        Events.instance().raiseEvent(EventNames.DOCUMENT_SELECTION_CHANGED);
        Events.instance().raiseEvent(
                EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED);
    }

    public String rejectOneTask() {

        // Handle mandatory comment here since we use several commandLinks for
        // now within the same form sharing the same comment textarea.
        // Therefore, we can simply use the jsf control...
        // Of course it remains a temporary solution.
        if (taskActionComment == null || taskActionComment.trim().length() <= 0) {
            // :XXX: Should be error but the error severity is not yet well
            // integrated Nuxeo5 side.
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.review.user.comment.mandatory"));

            FacesMessages.instance().add("taskActionCommentSerial", message);
            FacesMessages.instance().add("taskActionCommentParallel", message);
            /* Rux NXP-1374: added the blue message visible in top */
            FacesMessage message1 = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.user.comment.mandatory"));
            FacesMessages.instance().add(message1);
            return null;
        }

        if (workflowTaskInstanceId != null) {
            try {
                rejectTask(workflowTaskInstanceId);
                cleanContext();
                invalidateContextVariables();
            } catch (WMWorkflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        rebuildTabsList();
        return checkPermissions(getCurrentDocument()) ? null
                : navigationContext.goHome();
    }

    /**
     * Computes the next max review level.
     *
     * @return the next max review level
     */
    // XXX AT: useless since NXGED-1167 + NXGED-1165: do not insert at the end
    // of the road map
    public int getNextMaxReviewLevel() {
        int level = 0;
        if (reviewModel.getReviewType().equals(
                WorkflowConstants.WORKFLOW_REVIEW_TYPE_PARALLEL)) {
            return level;
        }
        if (documentTasks != null) {
            for (WMWorkItemInstance ti : documentTasks) {
                if (ti.getOrder() > level) {
                    level = ti.getOrder();
                }
            }
        }
        // Increment level
        level += 1;
        return level;
    }

    /**
     * Computes the next review level.
     *
     * @return the next review level
     */
    protected int getNextReviewLevel() {
        int level = reviewModel.getReviewCurrentLevel();
        return level + 1;
    }

    /**
     * Computes the former review level.
     *
     * @return the former review level
     */
    protected int getFormertReviewLevel() {
        int level = reviewModel.getReviewCurrentLevel();
        return level - 1;
    }

    @Factory(value = "canManageWorkflow", scope = EVENT)
    public Boolean getCanManageWorkflow() {

        if (canManageWorkflow != null) {
            return canManageWorkflow;
        }

        // Fallback for initial creation.
        if (reviewModel == null) {
            canManageWorkflow = true;
            return canManageWorkflow;
        }

        canManageWorkflow = false;

        try {

            String pid = reviewModel.getProcessInstanceId();
            String pname = reviewModel.getProcessInstanceName();
            WorkflowDocumentSecurityPolicy policy = getSecuPolicy(pname);
            if (policy != null) {

                Principal principal = currentUser;

                canManageWorkflow = policy.canManageWorkflow(pid, principal);
                if (!canManageWorkflow) {
                    if (principal instanceof NuxeoPrincipal) {
                        List<String> groupNames = ((NuxeoPrincipal) principal).getGroups();
                        for (String groupName : groupNames) {
                            canManageWorkflow = policy.canManageWorkflow(pid,
                                    new WMParticipantImpl(groupName));
                            if (canManageWorkflow) {
                                break;
                            }
                        }
                    }
                }
            } else {
                // Fallback when no bound security policies.
                log.warn("No security policies found for process with name="
                        + pname
                        + " No restrictions are applied...Configuration issue ?");
                canManageWorkflow = true;
            }

        } catch (WMWorkflowException we) {
            log.error(we.getMessage());
        }

        return canManageWorkflow;
    }

    public String getTaskActionComment() {
        return taskActionComment;
    }

    public void setTaskActionComment(String taskActionComment) {
        this.taskActionComment = taskActionComment;
    }

    private DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    public boolean canApproveWorkItem(WMWorkItemInstance wi) {

        boolean granted = false;

        if (wi == null) {
            return granted;
        }

        if (currentUser != null) {
            try {
                String processName = wi.getProcessInstance().getName();
                WorkflowDocumentSecurityPolicy policy = getSecuPolicy(processName);
                granted = policy.canEndWorkItem(currentUser, wi);
                if (!granted) {
                    if (currentUser instanceof NuxeoPrincipal) {
                        List<String> groupNames = ((NuxeoPrincipal) currentUser).getAllGroups();
                        for (String groupName : groupNames) {
                            granted = policy.canEndWorkItem(
                                    new WMParticipantImpl(groupName), wi);
                            if (granted) {
                                break;
                            }
                        }
                    }
                }
            } catch (WMWorkflowException we) {
                log.error("An error occured while fetching the security policy...");
            }
        }

        return granted;
    }

    public boolean canRejectWorkItem(WMWorkItemInstance wi) {

        boolean granted = false;

        if (wi == null) {
            return granted;
        }

        if (currentUser != null) {
            try {
                String processName = wi.getProcessInstance().getName();
                WorkflowDocumentSecurityPolicy policy = getSecuPolicy(processName);
                granted = policy.canRejectWorkItem(currentUser, wi);
                if (!granted) {
                    if (currentUser instanceof NuxeoPrincipal) {
                        List<String> groupNames = ((NuxeoPrincipal) currentUser).getAllGroups();
                        for (String groupName : groupNames) {
                            granted = policy.canRejectWorkItem(
                                    new WMParticipantImpl(groupName), wi);
                            if (granted) {
                                break;
                            }
                        }
                    }
                }
            } catch (WMWorkflowException we) {
                log.error("An error occured while fetching the security policy...");
            }
        }

        return granted;
    }

    public boolean canRemoveWorkItem(WMWorkItemInstance wi) {

        boolean granted = false;

        if (wi == null) {
            return granted;
        }

        if (currentUser != null) {
            try {
                String processName = wi.getProcessInstance().getName();
                WorkflowDocumentSecurityPolicy policy = getSecuPolicy(processName);
                granted = policy.canRemoveWorkItem(currentUser, wi);
                if (!granted) {
                    if (currentUser instanceof NuxeoPrincipal) {
                        List<String> groupNames = ((NuxeoPrincipal) currentUser).getAllGroups();
                        for (String groupName : groupNames) {
                            granted = policy.canRemoveWorkItem(
                                    new WMParticipantImpl(groupName), wi);
                            if (granted) {
                                break;
                            }
                        }
                    }
                }
            } catch (WMWorkflowException we) {
                log.error("An error occured while fetching the security policy...");
            }
        }

        return granted;
    }

    /**
     * Returns the security policy bound to the current workflow.
     *
     * @return
     * @throws WMWorkflowException
     */
    private WorkflowDocumentSecurityPolicy getSecuPolicy(String processName)
            throws WMWorkflowException {
        WorkflowDocumentSecurityPolicyManager manager = workflowBeansDelegate.getWorkflowDocumentSecurityPolicy();
        return manager.getWorkflowDocumentSecurityPolicyFor(processName);
    }

    public void invalidateContextVariables() {
        documentTasks = null;
        canManageWorkflow = null;

        // Factory has a event scope thus let's ensure Seam will reinvoke the
        // factory after the redirect which happends in the same request.
        eventContext.remove("documentTasks");
        eventContext.remove("canManageWorkflow");
    }

    public boolean canMoveDownWorkItem(WMWorkItemInstance wi) {

        boolean granted = false;

        if (wi == null) {
            return granted;
        }

        if (currentUser != null) {
            try {
                String processName = wi.getProcessInstance().getName();
                WorkflowDocumentSecurityPolicy policy = getSecuPolicy(processName);
                granted = policy.canMoveDown(currentUser, wi);
                if (!granted) {
                    if (currentUser instanceof NuxeoPrincipal) {
                        List<String> groupNames = ((NuxeoPrincipal) currentUser).getAllGroups();
                        for (String groupName : groupNames) {
                            granted = policy.canMoveDown(new WMParticipantImpl(
                                    groupName), wi);
                            if (granted) {
                                break;
                            }
                        }
                    }
                }
            } catch (WMWorkflowException we) {
                log.error("An error occured while fetching the security policy...");
            }
        }

        return granted;
    }

    public boolean canMoveUpWorkItem(WMWorkItemInstance wi) {

        boolean granted = false;

        if (wi == null) {
            return granted;
        }

        if (currentUser != null) {
            try {
                String processName = wi.getProcessInstance().getName();
                WorkflowDocumentSecurityPolicy policy = getSecuPolicy(processName);
                granted = policy.canMoveUp(currentUser, wi);
                if (!granted) {
                    if (currentUser instanceof NuxeoPrincipal) {
                        List<String> groupNames = ((NuxeoPrincipal) currentUser).getAllGroups();
                        for (String groupName : groupNames) {
                            granted = policy.canMoveUp(new WMParticipantImpl(
                                    groupName), wi);
                            if (granted) {
                                break;
                            }
                        }
                    }
                }
            } catch (WMWorkflowException we) {
                log.error("An error occured while fetching the security policy...");
            }
        }

        return granted;
    }

    public String moveWorkItemUp(String wiid) throws WMWorkflowException {

        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        WMWorkItemInstance theOne = wapi.getWorkItemById(wiid);

        if (theOne != null) {

            int offset = -1;
            for (int i = 0; i < documentTasks.size(); i++) {
                // Here work items are already sorted out.
                if (documentTasks.get(i).getId().equals(theOne.getId())) {
                    offset = i;
                }
            }

            // Work item found.
            if (offset > -1) {

                // Increase the order of the one
                Map<String, Serializable> props = new HashMap<String, Serializable>();
                props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER,
                        theOne.getOrder() + 1);
                wapi.updateWorkItemAttributes(theOne.getId(), props);

                // Swap with the one after
                List<WMWorkItemInstance> next = new ArrayList<WMWorkItemInstance>();
                for (WMWorkItemInstance wi : documentTasks) {
                    if (wi.getOrder() == theOne.getOrder() + 1) {
                        next.add(wi);
                    }
                }

                props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER,
                        theOne.getOrder());
                for (WMWorkItemInstance wi : next) {
                    wapi.updateWorkItemAttributes(wi.getId(), props);
                }

            }

            Events.instance().raiseEvent(
                    EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);
        }

        invalidateContextVariables();

        rebuildTabsList();
        return null;
    }

    public String moveWorkItemDown(String wiid) throws WMWorkflowException {

        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        WMWorkItemInstance theOne = wapi.getWorkItemById(wiid);

        if (theOne != null) {

            int offset = -1;
            for (int i = 0; i < documentTasks.size(); i++) {
                // Here work items are already sorted out.
                if (documentTasks.get(i).getId().equals(theOne.getId())) {
                    offset = i;
                }
            }

            // Work item found.
            if (offset > -1) {

                // Increase the order of the one
                Map<String, Serializable> props = new HashMap<String, Serializable>();
                props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER,
                        theOne.getOrder() - 1);
                wapi.updateWorkItemAttributes(theOne.getId(), props);

                // Swap with the one after
                List<WMWorkItemInstance> next = new ArrayList<WMWorkItemInstance>();
                for (WMWorkItemInstance wi : documentTasks) {
                    if (wi.getOrder() == theOne.getOrder() - 1) {
                        next.add(wi);
                    }
                }

                props.put(WorkflowConstants.WORKFLOW_TASK_PROP_ORDER,
                        theOne.getOrder());
                for (WMWorkItemInstance wi : next) {
                    wapi.updateWorkItemAttributes(wi.getId(), props);
                }

            }

            Events.instance().raiseEvent(
                    EventNames.WORKFLOW_USER_ASSIGNMENT_CHANGED);
        }

        invalidateContextVariables();
        rebuildTabsList();
        return null;
    }

    protected void rebuildTabsList() {
        Action currentTab = webActions.getCurrentTabAction();
        webActions.resetTabList();
        webActions.setCurrentTabAction(currentTab);
    }

    /**
     * Checks if the current user can read the current document.
     *
     * @return {@code true} if he can access the document, {@code false}
     *         otherwise.
     */
    private boolean checkPermissions(DocumentModel doc) {
        try {
            return documentManager.hasPermission(doc.getRef(),
                    SecurityConstants.READ);
        } catch (ClientException e) {
            // error => deny access
            log.error("error during checkPermissions", e);
            return false;
        }
    }

}
