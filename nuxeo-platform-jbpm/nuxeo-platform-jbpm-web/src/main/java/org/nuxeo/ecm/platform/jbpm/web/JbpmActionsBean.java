/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.web;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmSecurityPolicy;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.ecm.platform.jbpm.TaskListFilter;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.ecm.platform.jbpm.operations.AddCommentOperation;
import org.nuxeo.ecm.platform.jbpm.operations.GetRecipientsForTaskOperation;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * @author Anahide Tchertchian
 */
@Name("jbpmActions")
@Scope(ScopeType.CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class JbpmActionsBean extends DocumentContextBoundActionBean implements
        JbpmActions {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient JbpmService jbpmService;

    @In(create = true)
    protected transient JbpmHelper jbpmHelper;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient NuxeoPrincipal currentUser;

    protected Boolean canManageCurrentProcess;

    protected Boolean canManageParticipants;

    protected ProcessInstance currentProcess;

    protected String currentProcessInitiator;

    protected String currentProcessDestinationState;

    protected List<TaskInstance> currentTasks;

    protected ArrayList<VirtualTaskInstance> currentVirtualTasks;

    protected VirtualTaskInstance newVirtualTask;

    protected Boolean showAddVirtualTaskForm;

    protected Boolean formInEditMode = Boolean.FALSE;

    protected String userComment;

    public boolean getCanCreateProcess() throws ClientException {
        ProcessInstance currentProcess = getCurrentProcess();
        if (currentProcess == null) {
            // check write permissions on current doc
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                DocumentRef docRef = currentDoc.getRef();
                return documentManager.hasPermission(docRef,
                        SecurityConstants.WRITE);
            }
        }
        return false;
    }

    public boolean getCanManageProcess() throws ClientException {
        if (canManageCurrentProcess == null) {
            canManageCurrentProcess = Boolean.FALSE;
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                Boolean canWrite = jbpmService.getPermission(currentProcess,
                        JbpmSecurityPolicy.Action.write,
                        navigationContext.getCurrentDocument(), currentUser);
                if (canWrite != null) {
                    canManageCurrentProcess = canWrite;
                }
            }
        }
        return canManageCurrentProcess.booleanValue();
    }

    public boolean getCanManageParticipants() throws ClientException {
        if (canManageParticipants == null) {
            canManageParticipants = Boolean.FALSE;
            if (getCanManageProcess()) {
                canManageParticipants = Boolean.TRUE;
            } else {
                ProcessInstance pi = getCurrentProcess();
                if (pi != null) {
                    // check if user has a current task in this workflow
                    List<TaskInstance> tasks = jbpmService.getTaskInstances(
                            Long.valueOf(currentProcess.getId()), null, null);
                    if (tasks != null && !tasks.isEmpty()) {
                        JbpmHelper helper = new JbpmHelper();
                        NuxeoPrincipal pal = currentUser;
                        for (TaskInstance task : tasks) {
                            if (!task.isCancelled() && !task.hasEnded()
                                    && helper.isTaskAssignedToUser(task, pal)) {
                                canManageParticipants = Boolean.TRUE;
                                break;
                            }
                        }
                    }
                }
            }
            // NXP-3090: cannot manage participants after parallel wf is
            // started
            if (Boolean.TRUE.equals(canManageParticipants)) {
                if (isProcessStarted("choose-participant")
                        && "review_parallel".equals(getCurrentProcess().getProcessDefinition().getName())) {
                    canManageParticipants = Boolean.FALSE;
                }
            }
        }
        return canManageParticipants.booleanValue();
    }

    public boolean getCanEndTask(TaskInstance taskInstance)
            throws ClientException {
        if (taskInstance != null
                && (!taskInstance.isCancelled() && !taskInstance.hasEnded())) {
            JbpmHelper helper = new JbpmHelper();
            NuxeoPrincipal pal = currentUser;
            return pal.isAdministrator()
                    || pal.getName().equals(getCurrentProcessInitiator())
                    || helper.isTaskAssignedToUser(taskInstance, pal);
        }
        return false;
    }

    public String createProcessInstance(NuxeoPrincipal principal, String pd,
            DocumentModel dm, String endLifeCycle) throws ClientException {
        if (getCanCreateProcess()) {
            Map<String, Serializable> map = null;
            if (endLifeCycle != null && !endLifeCycle.equals("")
                    && !"null".equals(endLifeCycle)) {
                map = new HashMap<String, Serializable>();
                map.put(JbpmService.VariableName.endLifecycleTransition.name(),
                        endLifeCycle);
            }
            jbpmService.createProcessInstance(principal, pd, dm, map, null);
            notifyEventListeners(
                    JbpmEventNames.WORKFLOW_NEW_STARTED,
                    "",
                    new String[] { NuxeoPrincipal.PREFIX + principal.getName() });
            // TODO: add feedback?

            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_NEW_STARTED);
            resetCurrentData();
        }
        return null;
    }

    public ProcessInstance getCurrentProcess() throws ClientException {
        if (currentProcess == null) {
            List<ProcessInstance> processes = jbpmService.getProcessInstances(
                    navigationContext.getCurrentDocument(), currentUser, null);
            if (processes != null && !processes.isEmpty()) {
                currentProcess = processes.get(0);
            }
        }
        return currentProcess;
    }

    public String getCurrentProcessInitiator() throws ClientException {
        if (currentProcessInitiator == null) {
            currentProcessInitiator = "";
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                Object initiator = currentProcess.getContextInstance().getVariable(
                        JbpmService.VariableName.initiator.name());
                if (initiator instanceof String) {
                    currentProcessInitiator = (String) initiator;
                    if (currentProcessInitiator.startsWith(NuxeoPrincipal.PREFIX)) {
                        currentProcessInitiator = currentProcessInitiator.substring(NuxeoPrincipal.PREFIX.length());
                    }
                }
            }
        }
        return currentProcessInitiator;
    }

    public String getCurrentProcessDestinationState() throws ClientException {
        if (currentProcessDestinationState == null) {
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                Object destinationState = currentProcess.getContextInstance().getVariable(
                        JbpmService.VariableName.endLifecycleTransition.name());
                if (destinationState instanceof String) {
                    currentProcessDestinationState = (String) destinationState;
                }
            }
        }
        return currentProcessDestinationState;
    }

    public List<TaskInstance> getCurrentTasks(String... taskNames)
            throws ClientException {
        if (currentTasks == null) {
            currentTasks = new ArrayList<TaskInstance>();
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                currentTasks.addAll(jbpmService.getTaskInstances(
                        Long.valueOf(currentProcess.getId()), null,
                        new TaskListFilter(taskNames)));
            }
        }
        return currentTasks;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<VirtualTaskInstance> getCurrentVirtualTasks()
            throws ClientException {
        if (currentVirtualTasks == null) {
            currentVirtualTasks = new ArrayList<VirtualTaskInstance>();
            ProcessInstance currentProcess = getCurrentProcess();
            if (currentProcess != null) {
                Object participants = currentProcess.getContextInstance().getVariable(
                        JbpmService.VariableName.participants.name());
                if (participants != null && participants instanceof List) {
                    currentVirtualTasks.addAll((List<VirtualTaskInstance>) participants);
                }
            }
        }
        return currentVirtualTasks;
    }

    public boolean getShowAddVirtualTaskForm() throws ClientException {
        if (showAddVirtualTaskForm == null) {
            showAddVirtualTaskForm = Boolean.FALSE;
            if (getCurrentVirtualTasks().isEmpty()
                    && (currentTasks == null || currentTasks.isEmpty())) {
                showAddVirtualTaskForm = Boolean.TRUE;
            }
        }
        return showAddVirtualTaskForm.booleanValue();
    }

    public void toggleShowAddVirtualTaskForm(ActionEvent event)
            throws ClientException {
        showAddVirtualTaskForm = Boolean.valueOf(!getShowAddVirtualTaskForm());
    }

    public VirtualTaskInstance getNewVirtualTask() {
        if (newVirtualTask == null) {
            newVirtualTask = new VirtualTaskInstance();
        }
        return newVirtualTask;
    }

    public String addNewVirtualTask() throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && newVirtualTask != null && getCanManageParticipants()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks == null) {
                virtualTasks = new ArrayList<VirtualTaskInstance>();
            }
            virtualTasks.add(newVirtualTask);

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.added.reviewer"));

            resetCurrentData();
            // show create form again
            showAddVirtualTaskForm = Boolean.TRUE;
        }
        return null;
    }

    public String persistProcessInstance() throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null) {
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.edited"));

            resetCurrentData();
            formInEditMode = Boolean.FALSE;
        }
        return null;
    }

    public String changeVirtualTaskModification() {
        formInEditMode = Boolean.valueOf(!formInEditMode.booleanValue());
        if (!Boolean.TRUE.equals(formInEditMode)) {
            resetCurrentData();
        }
        return null;
    }

    public Boolean getFormInEditMode() {
        return formInEditMode;
    }

    public String moveDownVirtualTask(int index) throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && getCanManageParticipants()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks != null && index + 1 < virtualTasks.size()) {
                VirtualTaskInstance task = virtualTasks.remove(index);
                virtualTasks.add(index + 1, task);
            }

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.movedUp.reviewer"));

            // reset so that's reloaded
            resetCurrentData();
        }
        return null;
    }

    public String moveUpVirtualTask(int index) throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && getCanManageParticipants()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks != null && index - 1 < virtualTasks.size()) {
                VirtualTaskInstance task = virtualTasks.remove(index);
                virtualTasks.add(index - 1, task);
            }

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.movedDown.reviewer"));

            // reset so that's reloaded
            resetCurrentData();
        }
        return null;
    }

    public String removeVirtualTask(int index) throws ClientException {
        ProcessInstance pi = getCurrentProcess();
        if (pi != null && getCanManageParticipants()) {
            List<VirtualTaskInstance> virtualTasks = getCurrentVirtualTasks();
            if (virtualTasks != null && index < virtualTasks.size()) {
                virtualTasks.remove(index);
            }

            pi.getContextInstance().setVariable(
                    JbpmService.VariableName.participants.name(), virtualTasks);
            jbpmService.persistProcessInstance(pi);

            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.removed.reviewer"));

            // reset so that's reloaded
            resetCurrentData();
        }
        return null;
    }

    public void validateTaskDueDate(FacesContext context,
            UIComponent component, Object value) {
        final String DATE_FORMAT = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        String messageString = null;
        if (value != null) {
            Date today = null;
            Date dueDate = null;
            try {
                dueDate = dateFormat.parse(dateFormat.format((Date) value));
                today = dateFormat.parse(dateFormat.format(new Date()));
            } catch (ParseException e) {
                messageString = "label.workflow.error.date_parsing";
            }
            if (dueDate.before(today)) {
                messageString = "label.workflow.error.outdated_duedate";
            }
        }

        if (messageString != null) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.workflow.error.outdated_duedate"),
                    null);
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
            // also add global message?
            // context.addMessage(null, message);
        }

    }

    protected TaskInstance getStartTask(String taskName) throws ClientException {
        TaskInstance startTask = null;
        if (taskName != null) {
            // get task with that name on current process
            ProcessInstance pi = getCurrentProcess();
            if (pi != null) {
                List<TaskInstance> tasks = jbpmService.getTaskInstances(
                        Long.valueOf(currentProcess.getId()), null,
                        new TaskListFilter(taskName));
                if (tasks != null && !tasks.isEmpty()) {
                    // take first one found
                    startTask = tasks.get(0);
                }
            }
        }
        if (startTask == null) {
            throw new ClientException(
                    "No start task found on current process with name "
                            + taskName);
        }
        return startTask;
    }

    public boolean isProcessStarted(String startTaskName)
            throws ClientException {
        TaskInstance startTask = getStartTask(startTaskName);
        return startTask.hasEnded();
    }

    public String startProcess(String startTaskName) throws ClientException {
        if (getCanManageProcess()) {
            TaskInstance startTask = getStartTask(startTaskName);
            if (startTask.hasEnded()) {
                throw new ClientException("Process is already started");
            }
            // optim: pass participants as transient variables to avoid
            // lookup in the process instance
            Map<String, Serializable> transientVariables = new HashMap<String, Serializable>();
            transientVariables.put(
                    JbpmService.VariableName.participants.name(),
                    getCurrentVirtualTasks());
            transientVariables.put(JbpmService.VariableName.document.name(),
                    navigationContext.getCurrentDocument());
            transientVariables.put(JbpmService.VariableName.principal.name(),
                    currentUser);
            jbpmService.endTask(Long.valueOf(startTask.getId()), null, null,
                    null, transientVariables, currentUser);
            documentManager.save(); // process invalidations from handlers'
            // sessions
            resetCurrentData();
        }
        return null;
    }

    public String validateTask(final TaskInstance taskInstance,
            String transition) throws ClientException {
        if (taskInstance != null) {
            if (userComment != null && !"".equals(userComment)) {
                AddCommentOperation addCommentOperation = new AddCommentOperation(
                        taskInstance.getId(), NuxeoPrincipal.PREFIX
                                + currentUser.getName(), userComment);
                jbpmService.executeJbpmOperation(addCommentOperation);
            }
            // add marker that task was validated
            Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
            taskVariables.put(JbpmService.TaskVariableName.validated.name(),
                    Boolean.TRUE);
            jbpmService.endTask(Long.valueOf(taskInstance.getId()), transition,
                    taskVariables, null, getTransientVariables(), currentUser);
            documentManager.save(); // process invalidations from handlers'
            // sessions
            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.ended"));
            Set<String> recipients = getRecipientsFromTask(taskInstance);
            notifyEventListeners(JbpmEventNames.WORKFLOW_TASK_COMPLETED,
                    userComment, recipients.toArray(new String[] {}));
            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_COMPLETED);
            resetCurrentData();
        }
        return returnToCurrentDocOrHome();
    }

    public String rejectTask(final TaskInstance taskInstance, String transition)
            throws ClientException {
        if (taskInstance != null) {
            if (userComment != null && !"".equals(userComment)) {
                AddCommentOperation addCommentOperation = new AddCommentOperation(
                        taskInstance.getId(), NuxeoPrincipal.PREFIX
                                + currentUser.getName(), userComment);
                jbpmService.executeJbpmOperation(addCommentOperation);
            } else {
                facesMessages.add(
                        StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.review.task.enterComment"));
                return null;
            }
            // add marker that task was rejected
            Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
            taskVariables.put(JbpmService.TaskVariableName.validated.name(),
                    Boolean.FALSE);
            jbpmService.endTask(Long.valueOf(taskInstance.getId()), transition,
                    taskVariables, null, getTransientVariables(), currentUser);
            documentManager.save(); // process invalidations from handlers'
            // sessions
            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "label.review.task.ended"));
            Set<String> recipients = getRecipientsFromTask(taskInstance);
            notifyEventListeners(JbpmEventNames.WORKFLOW_TASK_REJECTED,
                    userComment, recipients.toArray(new String[] {}));
            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_REJECTED);
            resetCurrentData();
        }
        return returnToCurrentDocOrHome();
    }

    protected String returnToCurrentDocOrHome() throws ClientException {
        DocumentModel currentDocument;
        try {
            // re-fetch the document, it might have changed during the process
            currentDocument = navigationContext.getCurrentDocument();
            currentDocument = documentManager.getDocument(currentDocument.getRef());
            getCurrentProcess();
            String currentTabId = webActions.getCurrentTabId();
            navigationContext.setCurrentDocument(null);
            if (currentProcess == null || currentProcess.hasEnded()) {
                return navigationContext.navigateToDocument(currentDocument);
            }
            return webActions.setCurrentTabAndNavigate(currentDocument,
                    currentTabId);
        } catch (DocumentSecurityException e) {
            navigationContext.setCurrentDocument(null);
            return navigationContext.goHome();
        }
    }

    protected Map<String, Serializable> getTransientVariables() {
        Map<String, Serializable> transientVariables = new HashMap<String, Serializable>();
        transientVariables.put(JbpmService.VariableName.document.name(),
                navigationContext.getCurrentDocument());
        transientVariables.put(JbpmService.VariableName.principal.name(),
                currentUser);
        return transientVariables;
    }

    @SuppressWarnings("unchecked")
    protected Set<String> getRecipientsFromTask(final TaskInstance taskInstance)
            throws NuxeoJbpmException {
        GetRecipientsForTaskOperation operation = new GetRecipientsForTaskOperation(
                taskInstance.getId());
        return (Set<String>) jbpmService.executeJbpmOperation(operation);

    }

    // helper inner class to do the unrestricted abandon
    protected class UnrestrictedAbandon extends UnrestrictedSessionRunner {

        private final DocumentRef ref;

        private final Long processId;

        protected UnrestrictedAbandon(DocumentRef ref, Long processId) {
            super(documentManager);
            this.ref = ref;
            this.processId = processId;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel doc = session.getDocument(ref);
            ACP acp = doc.getACP();
            acp.removeACL(AbstractJbpmHandlerHelper.getProcessACLName(processId));
            session.setACP(doc.getRef(), acp, true);
            session.save();
        }
    }

    // helper inner class to do the unrestricted abandon
    protected class UnrestrictedEndProcess extends UnrestrictedSessionRunner {

        private final DocumentRef ref;

        public Set<String> recipients;

        protected UnrestrictedEndProcess(DocumentRef ref) {
            super(documentManager);
            this.ref = ref;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() throws ClientException {
            // end process and tasks
            List<TaskInstance> tis = jbpmService.getTaskInstances(
                    session.getDocument(ref), (NuxeoPrincipal) null, null);
            recipients = new HashSet<String>();
            for (TaskInstance ti : tis) {
                String actor = ti.getActorId();
                recipients.add(actor);
                Set<PooledActor> pooledActors = ti.getPooledActors();
                for (PooledActor pa : pooledActors) {
                    recipients.add(pa.getActorId());
                }
            }
        }
    }

    public String cancelCurrentProcess() throws ClientException {
        ProcessInstance currentProcess = getCurrentProcess();
        if (currentProcess != null) {
            // remove wf acls
            Long pid = Long.valueOf(currentProcess.getId());
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                UnrestrictedAbandon runner = new UnrestrictedAbandon(
                        currentDoc.getRef(), pid);
                runner.runUnrestricted();
            }

            // end process and tasks using unrestricted session
            UnrestrictedEndProcess endProcessRunner = new UnrestrictedEndProcess(
                    currentDoc.getRef());
            endProcessRunner.runUnrestricted();

            jbpmService.deleteProcessInstance(currentUser, pid);
            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "workflowProcessCanceled"));
            notifyEventListeners(JbpmEventNames.WORKFLOW_CANCELED, userComment,
                    endProcessRunner.recipients.toArray(new String[] {}));
            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_CANCELED);
            resetCurrentData();
        }
        webActions.resetCurrentTab();
        return null;
    }

    @SuppressWarnings("unchecked")
    public String abandonCurrentProcess() throws ClientException {
        ProcessInstance currentProcess = getCurrentProcess();
        if (currentProcess != null && getCanManageProcess()) {
            // remove wf acls
            Long pid = Long.valueOf(currentProcess.getId());
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                UnrestrictedAbandon runner = new UnrestrictedAbandon(
                        currentDoc.getRef(), pid);
                runner.runUnrestricted();
            }

            // end process and tasks
            List<TaskInstance> tis = jbpmService.getTaskInstances(
                    navigationContext.getCurrentDocument(),
                    (NuxeoPrincipal) null, null);
            Set<String> recipients = new HashSet<String>();
            for (TaskInstance ti : tis) {
                String actor = ti.getActorId();
                recipients.add(actor);
                Set<PooledActor> pooledActors = ti.getPooledActors();
                for (PooledActor pa : pooledActors) {
                    recipients.add(pa.getActorId());
                }
            }
            jbpmService.deleteProcessInstance(currentUser, pid);
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("workflowAbandoned"));
            notifyEventListeners(JbpmEventNames.WORKFLOW_ABANDONED,
                    userComment, recipients.toArray(new String[] {}));
            Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_ABANDONED);
            resetCurrentData();
        }
        return null;
    }

    public String getUserComment() {
        return userComment;
    }

    public void setUserComment(String comment) {
        this.userComment = comment;
    }

    protected static Set<String> CHECK_IN_TRANSITIONS = new HashSet<String>(
            Arrays.asList("approve"));

    protected boolean isCheckInTransition(String transition) {
        return CHECK_IN_TRANSITIONS.contains(transition);
    }

    public List<String> getAllowedStateTransitions(DocumentRef ref)
            throws ClientException {
        // break reference: core gives an unmodifiable collection unsuitable
        // for UI.
        List<String> res = new ArrayList<String>();
        for (String transition : documentManager.getAllowedStateTransitions(ref)) {
            if (isCheckInTransition(transition)) {
                res.add(transition + AbstractJbpmHandlerHelper.SUFFIX_MINOR);
                res.add(transition + AbstractJbpmHandlerHelper.SUFFIX_MAJOR);
            } else {
                res.add(transition);
            }
        }
        return res;
    }

    public void resetCurrentData() {
        canManageCurrentProcess = null;
        canManageParticipants = null;
        currentProcess = null;
        currentProcessInitiator = null;
        currentProcessDestinationState = null;
        currentTasks = null;
        currentVirtualTasks = null;
        newVirtualTask = null;
        showAddVirtualTaskForm = null;
        userComment = null;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetCurrentData();
    }

    public void notifyEventListeners(String name, String comment,
            String[] recipients) throws ClientException {
        jbpmService.notifyEventListeners(name, comment, recipients,
                documentManager, currentUser, getCurrentDocument());
    }

    @Override
    public boolean hasProcessDefinitions(String documentType) {
        Map<String, List<String>> conf = jbpmService.getTypeFilterConfiguration();
        if (conf != null) {
            List<String> defNames = conf.get(documentType);
            return defNames != null && !defNames.isEmpty();
        }
        return false;
    }
}
