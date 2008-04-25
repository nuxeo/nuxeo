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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.audit.api.AuditEventTypes;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventCategories;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventTypes;
import org.nuxeo.ecm.platform.workflow.document.api.WorkflowDocumentModificationConstants;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyConstants;
import org.nuxeo.ecm.platform.workflow.web.adapter.ProcessDocument;
import org.nuxeo.ecm.platform.workflow.web.adapter.ProcessModel;
import org.nuxeo.ecm.platform.workflow.web.api.DocumentTaskActions;
import org.nuxeo.ecm.platform.workflow.web.api.DocumentWorkflowActions;
import org.nuxeo.ecm.platform.workflow.web.api.WorkflowBeansDelegate;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;

/**
 * Workflow actions bean.
 * <p>
 * Deals with a document workflow related actions.
 * <p>
 * The tasks do have a dedicated action listener.
 *
 * @See org.nuxeo.ecm.platform.workflow.web.listener.DocumentTaskActionsBean
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("documentWorkflowActions")
@Scope(CONVERSATION)
public class DocumentWorkflowActionsBean implements DocumentWorkflowActions {

    private static final long serialVersionUID = -3103121887963826416L;

    private static final Log log = LogFactory.getLog(DocumentWorkflowActionsBean.class);

    private static final String APPROBATION_REVIEW_NAME = "document_review_approbation";

    @In
    protected transient Context eventContext;

    @In(create = true)
    protected WorkflowBeansDelegate workflowBeansDelegate;

    @In(required = true)
    protected RepositoryLocation currentServerLocation;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected PrincipalListManager principalListManager;

    @In(create = true)
    protected DocumentTaskActions documentTaskActions;

    protected String currentLifeCycleState;

    protected String lifeCyclePolicy;

    protected ProcessDocument processDocument;

    protected ProcessModel reviewModel; // XXX not generic enough name.

    protected boolean canStartWorkflow;

    protected Map<String, String> availableStateTransitionsMap;

    protected Map<String, String> workflowDefinitionsMap;

    /** Map from id to name => cache* */
    protected Map<String, String> workflowDefCache = new HashMap<String, String>();

    protected Map<String, String> reviewModificationPropertiesMap;

    protected Map<String, String> reviewVersioningPropertiesMap;

    protected Collection<String> availableStateTransitions;

    protected List<SelectItem> workItemDirectives;

    /** Set by the user from the interface. */
    protected String workflowDefinitionId;

    /** Set by the UI. */
    protected String lifeCycleDestinationStateTransition;

    /** Set by the UI. */
    protected String reviewModificationProperty = WorkflowDocumentModificationConstants.WORKFLOW_DOCUMENT_MODIFICATION_ALLOWED;

    /** Set by the UI. */
    protected String reviewVersioningProperty = WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_AUTO;

    protected Collection<WMProcessDefinition> definitions;

    protected String userComment;

    @In(required = false)
    protected List<WMWorkItemInstance> documentTasks;


    @Create
    public void init() {
        Events.instance().raiseEvent(EventNames.WF_INIT);
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public String getUserComment() {
        return userComment;
    }

    public void setUserComment(String userComment) {
        this.userComment = userComment;
    }

    public Collection<WMProcessDefinition> getAllowedDefinitions()
            throws WMWorkflowException {

        Collection<WMProcessDefinition> wdefs = new ArrayList<WMProcessDefinition>();

        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        WorkflowRulesManager wrules = workflowBeansDelegate.getWorkflowRulesBean();

        String docType = getCurrentDocument().getType();
        String state = getCurrentLifeCycleState();

        // :XXX: Apply path rules
        // String docPath = currentItem.getPath().makeAbsolute()
        // log.info("Doc path is :" + docPath);
        Collection<String> defIds = wrules.getAllowedWorkflowDefinitionNamesByDoctype(docType);

        for (String defId : defIds) {
            WMProcessDefinition wdef = wapi.getProcessDefinitionByName(defId);
            if (wdef != null) {
                String name = wdef.getName();
                if (name != null && state != null) {
                    // :XXX: sucky hardcoded stuffs here for GEIDE...
                    // Extend workflow rules to support this.
                    if (name.equals(APPROBATION_REVIEW_NAME)
                            && !state.equals("project")) {
                        continue;
                    }
                    wdefs.add(wdef);
                }
            } else {
                // Optimization !
                // Lazily remove entries at runtime to avoid having to recheck
                // in the future. It will be reloaded at startup since this is
                // defined within extension points though.
                log.info("Optimization : remove rules because definition no longer exist or is incorrect.");
                wrules.delRuleByType(defId, docType);
            }
        }
        return wdefs;
    }

    public Collection<WMProcessInstance> getWorkflowInstancesForDocument()
            throws WMWorkflowException {
        Collection<WMProcessInstance> procs = new ArrayList<WMProcessInstance>();

        WorkflowDocumentRelationManager wdoc = workflowBeansDelegate.getWorkflowDocumentBean();
        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        if (processDocument == null) {
            computeProcessDocument();
        }

        if (processDocument != null) {
            ProcessModel[] models = processDocument.getProcessInfo();
            String[] pids = new String[models.length];
            for (int i = 0; i < models.length; i++) {
                pids[i] = models[i].getProcessInstanceId();
            }
            for (String pid : pids) {
                WMProcessInstance proc = wapi.getProcessInstanceById(pid,
                        WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE);
                if (proc != null) {
                    procs.add(proc);
                } else {
                    log.info("Cleanup up worklfow document mapping....");
                    wdoc.deleteDocumentWorkflowRef(
                            getCurrentDocument().getRef(), pid);
                }
            }
        }
        return procs;
    }

    protected void rebuildTabsList() {
        Action currentTab = webActions.getCurrentTabAction();
        webActions.resetTabList();
        webActions.setCurrentTabAction(currentTab);
    }

    public String abandonWorkflow() throws WMWorkflowException {
        if (reviewModel != null) {
            WMProcessInstance pi = endWorkflow(reviewModel.getProcessInstanceId());
            if (pi == null
                    || pi.getState().equals(
                            WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE)) {
                // redirect to default page
                webActions.resetTabList();
                return null;
            }
        }
        rebuildTabsList();
        return null;
    }

    public String startOneWorkflow() throws WMWorkflowException {
        if (workflowDefinitionId != null) {
            startWorkflowById(workflowDefinitionId);
        }
        rebuildTabsList();
        return null;
    }

    public String startWorkflow(String wfname) throws WMWorkflowException {
        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        WMProcessDefinition wfdef = wapi.getProcessDefinitionByName(wfname);
        if (wfdef == null) {
            throw new WMWorkflowException("Unknown workflow " + wfname);
        }
        String jbpmId = wfdef.getId();
        startWorkflowById(jbpmId);
        rebuildTabsList();
        return null;
    }

    protected WMActivityInstance startWorkflowById(String wdefId)
            throws WMWorkflowException {

        WMActivityInstance workflowPath;

        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        try {
            // Build process variable map.
            Map<String, Serializable> processVariables = new HashMap<String, Serializable>();
            processVariables.put(WorkflowConstants.WORKFLOW_CREATOR,
                    currentUser.getName());

            processVariables.put(
                    WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE,
                    lifeCycleDestinationStateTransition);

            processVariables.put(
                    WorkflowConstants.DOCUMENT_MODIFICATION_POLICY,
                    reviewModificationProperty);

            processVariables.put(WorkflowConstants.DOCUMENT_VERSIONING_POLICY,
                    reviewVersioningProperty);

            processVariables.put(WorkflowConstants.DOCUMENT_REF,
                    getCurrentDocument().getRef());

            processVariables.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL, 0);

            processVariables.put(
                    WorkflowConstants.WORKFLOW_FORMER_REVIEW_LEVEL, 0);

            processVariables.put(WorkflowConstants.DOCUMENT_LOCATION_URI,
                    currentServerLocation.getName());

            log.info("About to start a process for participant="
                    + currentUser.getName());

            workflowPath = wapi.startProcess(wdefId, processVariables, null);
        } catch (WMWorkflowException we) {
            workflowPath = null;
            log.error("An error occurred while grabbing workflow definitions");
            we.printStackTrace();
        }

        // Broadcast a message.
        if (workflowPath != null) {
            Events.instance().raiseEvent(EventNames.WORKFLOW_NEW_STARTED);
            Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);
            Events.instance().raiseEvent(EventNames.DOCUMENT_SELECTION_CHANGED);
            Events.instance().raiseEvent(
                    EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED);
        }

        return workflowPath;
    }

    protected WMProcessInstance endWorkflow(String wid)
            throws WMWorkflowException {

        // Unlink the document to the process
        // :XXX: We might want to do that using JMS in the future.
        WorkflowDocumentRelationManager wDoc = workflowBeansDelegate.getWorkflowDocumentBean();

        try {
            wDoc.deleteDocumentWorkflowRef(getCurrentDocument().getRef(), wid);
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }

        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        // For later event broadcast
        WMProcessInstance wi = wapi.getProcessInstanceById(wid, null);
        String name = wi.getProcessDefinition().getName();

        WorkflowDocumentSecurityManager wfSecurityManager = workflowBeansDelegate.getWFSecurityManagerBean();

        // --------------------------------------------------------------
        // Remove ACL corresponding to the pid
        // --------------------------------------------------------------

        wfSecurityManager.removeACL(getCurrentDocument().getRef(),
                reviewModel.getProcessInstanceId());

        log.info("Deny WF rights ............ DONE");

        WMProcessInstance workflowInstance;
        try {
            workflowInstance = wapi.terminateProcessInstance(wid);
        } catch (WMWorkflowException we) {
            workflowInstance = null;
            log.error("An error occurred while grabbing workflow definitions");
            we.printStackTrace();
        }

        // Broadcast events
        if (workflowInstance != null) {
            // Prepare an event for notification
            notifyEvent(WorkflowEventTypes.WORKFLOW_ABANDONED, null,
                    userComment, name);

            Events.instance().raiseEvent(EventNames.WORKFLOW_ENDED);
            Events.instance().raiseEvent(AuditEventTypes.HISTORY_CHANGED);
            Events.instance().raiseEvent(EventNames.DOCUMENT_SELECTION_CHANGED);
            Events.instance().raiseEvent(
                    EventNames.CURRENT_DOCUMENT_LIFE_CYCLE_CHANGED);
        }

        userComment = null;

        // Recompute to get new state and fixtures properties.
        try {
            workflowInstance = wapi.getProcessInstanceById(
                    reviewModel.getProcessInstanceId(), null);
        } catch (Exception e) {
            workflowInstance = null;
        }

        invalidateContextVariables();
        return workflowInstance;
    }

    public String getLifeCycleDestinationStateTransition() {
        return lifeCycleDestinationStateTransition;
    }

    public void setLifeCycleDestinationStateTransition(
            String lifeCycleDestinationStateTransition) {
        this.lifeCycleDestinationStateTransition = lifeCycleDestinationStateTransition;
    }

    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    // XXX method name sucks => should be name instead of label
    public String getWorkflowDefinitionLabel() throws WMWorkflowException {
        String name = null;
        Map<String, String> wdefs = getWorkflowDefinitionsMap();
        for (String i18n : wdefs.keySet()) {
            String id = wdefs.get(i18n);
            if (id.equals(workflowDefinitionId)) {
                name = workflowDefCache.get(id);
                break;
            }
        }
        return name;
    }

    public void setAvailableStateTransitions(
            Collection<String> availableStateTransitions) {
        this.availableStateTransitions = availableStateTransitions;
    }

    protected void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category) throws WMWorkflowException {

        DocumentMessageProducer producer = workflowBeansDelegate.getDocumentMessageProducer();
        DocumentModel dm = getCurrentDocument();

        Map<String, Serializable> props = properties == null ? new HashMap<String, Serializable>()
                : properties;

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
                recipients.append(participantName + '|');
            }
            String recipient = null;
            if (recipients.toString().trim().length() > 0) {
                recipient = recipients.toString().substring(0,
                        recipients.lastIndexOf("|"));
            }

            props.put("recipients", recipient);
        }

        try {
            props.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    getCurrentDocument().getCurrentLifeCycleState());
        } catch (ClientException ce) {
            throw new WMWorkflowException(ce.getMessage());
        }

        CoreEvent event = new CoreEventImpl(eventId, dm, props,
                workflowBeansDelegate.getWAPIBean().getParticipant(),
                category != null ? category
                        : WorkflowEventCategories.EVENT_WORKFLOW_CATEGORY,
                comment);
        DocumentMessage msg = new DocumentMessageImpl(dm, event);
        producer.produce(msg);
    }

    public String getReviewModificationProperty() {
        return reviewModificationProperty;
    }

    public void setReviewModificationProperty(String reviewModificationProperty) {
        this.reviewModificationProperty = reviewModificationProperty;
    }

    public String getReviewVersioningProperty() {
        return reviewVersioningProperty;
    }

    public void setReviewVersioningProperty(String reviewVersioningProperty) {
        this.reviewVersioningProperty = reviewVersioningProperty;
    }

    @Factory(value = "workItemDirectives", scope = EVENT)
    public List<SelectItem> computeWorkitemDirectives() {

        if (workItemDirectives != null) {
            return workItemDirectives;
        }

        workItemDirectives = new ArrayList<SelectItem>();

        if (getCurrentDocument() == null) {
            log.debug("No currentItem defined. Cannot compute workflow mdofication policy");
            return workItemDirectives;
        }
        WAPI wapi;
        try {
            wapi = workflowBeansDelegate.getWAPIBean();
        } catch (WMWorkflowException we) {
            log.error("Impossible to compute the modification policy for currentItem");
            return workItemDirectives;
        }

        if (reviewModel == null) {
            log.debug("No process on document. Cannot compute workfow modification policy");
            return workItemDirectives;
        }

        Map<String, Serializable> variables = wapi.listProcessInstanceAttributes(reviewModel.getProcessInstanceId());
        String[] directiveIds = (String[]) variables.get(WorkflowConstants.WORKFLOW_DIRECTIVES);
        if (directiveIds != null) {
            for (String directiveId : directiveIds) {
                String label = messages.get(directiveId);
                workItemDirectives.add(new SelectItem(directiveId,
                        label != null ? label : directiveId));
            }
        }
        return workItemDirectives;
    }

    public List<SelectItem> getWorkItemDirectives() {
        return workItemDirectives;
    }

    public void setWorkItemDirectives(List<SelectItem> workItemDirectives) {
        this.workItemDirectives = workItemDirectives;
    }

    private DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    @Factory(value = "canStartWorkflow", scope = EVENT)
    public boolean canStartWorkflow() throws ClientException {
        if (documentManager != null) {
            canStartWorkflow = documentManager.hasPermission(
                    getCurrentDocument().getRef(), SecurityConstants.WRITE);
        }
        return canStartWorkflow;
    }

    @Factory(value = "processDocument", scope = EVENT)
    public ProcessDocument computeProcessDocument() {
        if (processDocument == null) {
            DocumentModel doc = getCurrentDocument();
            if (doc != null) {
                processDocument = getProcessDocumentFor(doc);
            }
        }
        return processDocument;
    }

    @Factory(value = "reviewModel", scope = EVENT)
    public ProcessModel computeReviewModel() {
        if (reviewModel == null) {
            reviewModel = getReviewModelFor(getCurrentDocument());
        }
        return reviewModel;
    }

    public ProcessModel getReviewModelFor(DocumentModel doc) {
        ProcessDocument procDoc = getProcessDocumentFor(doc);
        if (procDoc != null) {
            ProcessModel[] models = procDoc.getProcessInfo();
            for (ProcessModel model : models) {
                if (model.getProcessInstanceStatus().equals(
                        WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE)) {
                    // :XXX: only one review allowed per document => we take
                    // the first one => think about something better than that.
                    return model;
                }
            }
        }
        return null;
    }

    public ProcessDocument getProcessDocumentFor(DocumentModel doc) {
        if (doc != null) {
            // Adapt the document model for the processus.
            return doc.getAdapter(ProcessDocument.class, true);
        }
        return null;
    }

    public void invalidateContextVariables() {
        processDocument = null;
        reviewModel = null;
        currentLifeCycleState = null;
        availableStateTransitionsMap = null;

        canStartWorkflow = false;
        workItemDirectives = null;

        lifeCyclePolicy = null;
        reviewModificationPropertiesMap = null;
        reviewVersioningPropertiesMap = null;
        workflowDefinitionsMap = null;
        workflowDefCache = new HashMap<String, String>();

        // Factory has a event scope thus let's ensure Seam will reinvoke the
        // factory after the redirect which happends in the same request.
        eventContext.remove("processDocument");
        eventContext.remove("reviewModel");
        eventContext.remove("currentLifeCycleState");
        eventContext.remove("availableStateTransitionsMap");
        eventContext.remove("canStartWorkflow");
        eventContext.remove("workItemDirectives");
        eventContext.remove("lifeCyclePolicy");
        eventContext.remove("reviewModificationPropertiesMap");
        eventContext.remove("reviewVersioningPropertiesMap");
        eventContext.remove("workflowDefinitionsMap");
    }

    public void updateDocumentRights() throws WMWorkflowException {
        try {
            WAPI wapi = workflowBeansDelegate.getWAPIBean();
            WorkflowDocumentSecurityManager workflowSecurityManager = workflowBeansDelegate.getWFSecurityManagerBean();
            WorkflowDocumentSecurityPolicyManager secuPolicyManager = workflowBeansDelegate.getWorkflowDocumentSecurityPolicy();

            if (reviewModel == null) {
                computeReviewModel();
            }

            if (reviewModel == null) {
                log.error("Review model is null... Skipping document security update.");
                return;
            }

            String pid = reviewModel.getProcessInstanceId();
            WMProcessInstance pi = wapi.getProcessInstanceById(pid, null);
            if (pi != null
                    && !pi.getState().equals(
                            WorkflowConstants.WORKFLOW_INSTANCE_STATUS_INACTIVE)) {
                WorkflowDocumentSecurityPolicy policy = secuPolicyManager.getWorkflowDocumentSecurityPolicyFor(reviewModel.getProcessInstanceName());
                if (policy != null) {
                    List<UserEntry> userEntries = policy.getRules(pid, null);
                    workflowSecurityManager.setRules(
                            getCurrentDocument().getRef(), userEntries, pid);
                }
            }
        } catch (WMWorkflowException we) {
            // :XXX
        }
    }

    public void updateCurrentLevelAfterDocumentChanged()
            throws WMWorkflowException {
        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        if (reviewModel == null) {
            return;
        }

        String pid = reviewModel.getProcessInstanceId();

        if (pid == null) {
            return;
        }

        WMProcessInstance pi = wapi.getProcessInstanceById(pid,
                WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE);

        if (pi != null) {

            // :XXX: hardcoded
            if (pi.getName().equals(APPROBATION_REVIEW_NAME)) {
                // Change current review level to the beginning
                Map<String, Serializable> variables = new HashMap<String, Serializable>();
                variables.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL, 0);
                wapi.updateProcessInstanceAttributes(pid, variables);

                // Update rights.
                updateDocumentRights();

                // Notifications sake

                final Collection<WMWorkItemInstance> items = wapi.listWorkItems(
                        pid, WMWorkItemState.WORKFLOW_TASK_STATE_ALL);

                for (WMWorkItemInstance item : items) {
                    if (item.getOrder() != 0) {
                        // Since it goes back on top we are only interested
                        // about the top reviewers.
                        continue;
                    }

                    // XXX for now only one initiator
                    boolean isGroup = false;
                    final String principalName = item.getParticipantName();
                    final String comment = item.getComment();

                    // Notify
                    final Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();

                    eventInfo.put("recipients", isGroup ? "group:" : "user:"
                            + principalName);

                    notifyEvent(WorkflowEventTypes.WORKFLOW_TASK_RETURNED,
                            eventInfo, comment,
                            reviewModel.getProcessInstanceName());

                }
            }
        }
    }

    @Factory(value = "availableStateTransitionsMap", scope = EVENT)
    public Map<String, String> getAvailableStateTransitionsMap() {

        if (availableStateTransitionsMap != null) {
            return availableStateTransitionsMap;
        }

        availableStateTransitionsMap = new HashMap<String, String>();

        Collection<String> transitions = new ArrayList<String>();
        try {
            transitions = documentManager.getAllowedStateTransitions(getCurrentDocument().getRef());
        } catch (ClientException ce) {
            log.error("Impossible to get available state transitions...");
        }

        if (transitions != null) {
            for (String transition : transitions) {
                String label = messages.get(transition);
                availableStateTransitionsMap.put(label != null ? label
                        : transition, transition);
            }
        }
        return availableStateTransitionsMap;
    }

    @Factory(value = "workflowDefinitionsMap", scope = EVENT)
    public Map<String, String> getWorkflowDefinitionsMap() {

        if (workflowDefinitionsMap != null) {
            return workflowDefinitionsMap;
        }

        workflowDefinitionsMap = new LinkedHashMap<String, String>();

        log.debug("Recomputing workflow definitions list");

        Collection<WMProcessDefinition> definitions = null;
        try {
            definitions = getAllowedDefinitions();
        } catch (WMWorkflowException we) {
            log.error("Error retrieving workflow definitions: ", we);
        }
        if (definitions != null) {
            for (WMProcessDefinition definition : definitions) {
                // Reverse order for f:selectItems.
                String id = definition.getId();
                String name = definition.getName();
                String label = messages.get(name);
                workflowDefinitionsMap.put(label != null ? label : name, id);
                // Cache for further faster lookup
                workflowDefCache.put(id, name);
            }
        }
        return workflowDefinitionsMap;
    }

    @Factory(value = "reviewModificationPropertiesMap", scope = EVENT)
    public Map<String, String> getReviewModificationPropertiesMap() {

        if (reviewModificationPropertiesMap != null) {
            return reviewModificationPropertiesMap;
        }

        reviewModificationPropertiesMap = new HashMap<String, String>();

        Collection<String> props = new ArrayList<String>();
        props.add(WorkflowDocumentModificationConstants.WORKFLOW_DOCUMENT_MODIFICATION_ALLOWED);
        props.add(WorkflowDocumentModificationConstants.WORKFLOW_DOCUMENT_MODIFICATION_NOT_ALLOWED);

        for (String prop : props) {
            String label = messages.get(prop);
            reviewModificationPropertiesMap.put(label != null ? label : prop,
                    prop);
        }

        return reviewModificationPropertiesMap;
    }

    @Factory(value = "reviewVersioningPropertiesMap", scope = EVENT)
    public Map<String, String> getReviewVersioningPropertiesMap() {

        if (reviewVersioningPropertiesMap != null) {
            return reviewVersioningPropertiesMap;
        }

        reviewVersioningPropertiesMap = new HashMap<String, String>();

        Collection<String> props = new ArrayList<String>();
        props.add(WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_AUTO);
        props.add(WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_CASE_DEPENDENT);
        props.add(WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_NO_INCREMENT);

        for (String prop : props) {
            String label = messages.get(prop);
            reviewVersioningPropertiesMap.put(label != null ? label : prop,
                    prop);
        }

        return reviewVersioningPropertiesMap;
    }

    @Factory(value = "currentLifeCycleState", scope = EVENT)
    public String getCurrentLifeCycleState() throws WMWorkflowException {
        // :XXX: ensure invalidation and check if not null before recomputing.
        try {
            currentLifeCycleState = documentManager.getCurrentLifeCycleState(getCurrentDocument().getRef());
        } catch (ClientException ce) {
            throw new WMWorkflowException(ce.getMessage());
        }
        return currentLifeCycleState;
    }

    @Factory(value = "lifeCyclePolicy", scope = EVENT)
    public String getLifeCyclePolicy() throws WMWorkflowException {
        // :XXX: ensure invalidation and check if not null before recomputing.
        try {
            lifeCyclePolicy = documentManager.getLifeCyclePolicy(getCurrentDocument().getRef());
        } catch (ClientException ce) {
            throw new WMWorkflowException(ce.getMessage());
        }
        return lifeCyclePolicy;
    }

    public String startWorkflowCallback() throws WMWorkflowException {

        // Only if a process is up and running.
        if (reviewModel != null) {

            String name = reviewModel.getProcessInstanceName();
            String pid = reviewModel.getProcessInstanceId();

            WAPI wapi = workflowBeansDelegate.getWAPIBean();

            Map<String, Serializable> props = wapi.listProcessInstanceAttributes(pid);

            // Already started.
            if (isWorkflowStarted()) {
                log.debug("Workflow already started.");
                return null;
            }

            // Flag the process.
            props.put(WorkflowConstants.WORKFLOW_STARTED_FLAG, true);
            wapi.updateProcessInstanceAttributes(pid, props);

            // Prepare an event for notification
            Map<String, Serializable> eProps = new HashMap<String, Serializable>();
            // XXX AT: NXP-1647, this triggered a NPE and don't I get why we
            // don't use reviewModel.getReviewType anyway
            // eProps.put(
            // "reviewType",
            // wapi.getProcessDefinitionById(pid).getName());
            eProps.put("reviewType", name);
            // Only for approbation workflow
            log.debug("Send notification event");
            // XXX AT: why event is sent for any workflow then?
            notifyEvent(WorkflowEventTypes.APPROBATION_WORKFLOW_STARTED,
                    eProps, name, name);

            // update rights
            updateDocumentRights();

            notifyEvent(WorkflowEventTypes.WORKFLOW_STARTED, eProps, name, name);

            invalidateContextVariables();

        }
        return null;
    }

    public boolean isWorkflowStarted() throws WMWorkflowException {

        // XXX cache me

        boolean started = false;
        if (reviewModel != null) {

            String pid = reviewModel.getProcessInstanceId();

            WAPI wapi = workflowBeansDelegate.getWAPIBean();

            Map<String, Serializable> props = wapi.listProcessInstanceAttributes(pid);

            // Already started.
            Serializable flag = props.get(WorkflowConstants.WORKFLOW_STARTED_FLAG);
            if (flag != null && (Boolean) flag) {
                started = true;
            }
        }
        return started;
    }

    public boolean checkTaskList() throws WMWorkflowException {
        final List<WMWorkItemInstance> taskList = documentTaskActions.computeDocumentTasks();
        if (taskList.size() < 2) {
            return false;
        }

        final List<String> participantNameList = new ArrayList<String>();
        for (final WMWorkItemInstance workItemInstance : taskList) {
            participantNameList.add(workItemInstance.getParticipantName());
        }
        // two different reviewers
        final Set<String> participantNameSet = new TreeSet<String>(
                participantNameList);
        return participantNameSet.size() >= 2;
    }

}
