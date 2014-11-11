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
 * $Id: PublishingActionsListenerBean.java 28957 2008-01-11 13:36:52Z tdelprat $
 */

package org.nuxeo.ecm.platform.publishing;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.persistence.PreRemove;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.publishing.api.PublishActions;
import org.nuxeo.ecm.platform.publishing.api.PublishingActionsListener;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.publishing.workflow.PublishingConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMParticipant;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.dashboard.DashboardActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Publishing actions listener. Listens to publish/reject
 * document actions.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("publishingActions")
@Scope(CONVERSATION)
public class PublishingActionsListenerBean extends InputController implements
        PublishingActionsListener {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishingActionsListenerBean.class);

    @In(create = true, required = false)
    private CoreSession documentManager;

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient DashboardActions dashboardActions;

    @In(create = true)
    protected PublishActions publishActions;

    @In
    protected transient Context eventContext;

    @In(create = true)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String rejectPublishingComment;

    private transient WorkflowDocumentRelationBusinessDelegate wfDocRelBD;

    private transient WAPI wapi;

    public PublishingActionsListenerBean() {
        initializeBD();
    }

    protected void initializeBD() {
        try {
            wapi = WAPIBusinessDelegate.getWAPI();
        } catch (WMWorkflowException e) {
            log.error("Cannot get WAPIBean...");
        }
        wfDocRelBD = new WorkflowDocumentRelationBusinessDelegate();
    }

    protected void destroyBD() {
        wapi = null;
        wfDocRelBD = null;
    }

    protected Map<String, String> getMessages() {
        return messages;
    }

    @PostActivate
    public void readState() {
        log.debug("@PostActivate");
        initializeBD();
    }

    @PreRemove
    public void saveState() {
        log.debug("@PreRemove");
    }

    @PrePassivate
    public void prePassivate() {
        log.debug("@Prepassivate");
        destroyBD();
    }

    @Remove
    @Destroy
    public void destroy() {
        log.debug("destroy()");
    }

    public String publishDocument() throws PublishingException {
        WMParticipant creator;
        try {
            PublishingTasks tasks = new PublishingTasks(navigationContext.getCurrentDocument(), currentUser);
            WMWorkItemInstance wi = tasks.getPublishingWorkItem();
            if (wi == null) {
                throw new PublishingException(
                        "No publishing task found for user="
                                + currentUser.getName());
            }
            creator = wi.getProcessInstance().getAuthor();
            wapi.endWorkItem(wi.getId(),
                    PublishingConstants.WORKFLOW_TRANSITION_TO_PUBLISH);
        } catch (WMWorkflowException e) {
            throw new PublishingException(e);
        }

        DocumentModel sourceDocument;
        // Notify approve publish event
        try {
            DocumentModel currentDocument = getCurrentDocument();

            CoreSession session = null;
            LoginContext context = null;
            Repository repository = null;
            try {
                context = Framework.login();
                RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
                repository = repositoryMgr.getRepository(currentDocument.getRepositoryName());
                session = repository.open();
            } catch (Exception e) {
                throw new ClientException(e);
            }

            String proxySourceId = session.getDocument(
                    new IdRef(currentDocument.getSourceId())).getSourceId();
            sourceDocument = session.getDocument(new IdRef(
                    proxySourceId));
            try {
                if (repository != null && session != null) {
                    repository.close(session);
                }
                if (context != null) {
                    context.logout();
                }
            } catch (Exception e) {
                throw new ClientException(e);
            }

            DocumentModel section = documentManager.getParentDocument(currentDocument.getRef());
            Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
            eventInfo.put("targetSection", section.getName());
            eventInfo.put("proxy", currentDocument);
            eventInfo.put("sectionPath", section.getPathAsString());
            eventInfo.put(WorkflowConstants.WORKFLOW_CREATOR, creator.getName());
            notifyEvent(
                    org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLICATION_APPROVED,
                    eventInfo, rejectPublishingComment, sourceDocument);
        } catch (ClientException e) {
            throw new PublishingException(e);
        }

        // Here to invalidate the dashboard items.
        Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_STOP);

        Events.instance().raiseEvent(
                org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SELECTION_CHANGED);

        webActions.resetTabList();

        rejectPublishingComment = null;

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("document_published"),
                resourcesAccessor.getMessages().get(sourceDocument.getType()));

        return null;
    }

    public String rejectDocument() throws PublishingException {

        // Handle mandatory comment here since we use several commandLinks for
        // now within the same form sharing the same comment textarea.
        // Therefore, we can simply use the jsf control...
        // Of course it remains a temporary solution.
        if (rejectPublishingComment == null
                || rejectPublishingComment.trim().length() <= 0) {
            // :XXX: Should be error but the error severity is not yet well
            // integrated Nuxeo5 side.
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.publishing.reject.user.comment.mandatory"));

            FacesMessages.instance().add("rejectPublishingComment", message);
            return null;
        }

        // Compute parent before deleting the document.
        DocumentModel currentDocument = getCurrentDocument();
        DocumentModel parent;
        WMParticipant creator;
        try {
            parent = documentManager.getDocument(currentDocument.getParentRef());
        } catch (ClientException ce) {
            throw new PublishingException(ce);
        }

        try {
            PublishingTasks tasks = new PublishingTasks(navigationContext.getCurrentDocument(), currentUser);
            WMWorkItemInstance wi = tasks.getPublishingWorkItem();
            if (wi == null) {
                throw new PublishingException(
                        "No publishing task found for user="
                                + currentUser.getName());
            }
            creator = wi.getProcessInstance().getAuthor();
            wapi.endWorkItem(wi.getId(),
                    PublishingConstants.WORKFLOW_TRANSITION_TO_REJECT);
        } catch (WMWorkflowException e) {
            throw new PublishingException(e);
        }

        // Notify reject event
        DocumentModel sourceDocument;
        try {


            CoreSession session = null;
            LoginContext context = null;
            Repository repository = null;
            try {
                context = Framework.login();
                RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
                repository = repositoryMgr.getRepository(currentDocument.getRepositoryName());
                session = repository.open();
            } catch (Exception e) {
                throw new ClientException(e);
            }

            String proxySourceId = session.getDocument(
                    new IdRef(currentDocument.getSourceId())).getSourceId();
            sourceDocument = session.getDocument(new IdRef(
                    proxySourceId));
            try {
                if (repository != null && session != null) {
                    repository.close(session);
                }
                if (context != null) {
                    context.logout();
                }
            } catch (Exception e) {
                throw new ClientException(e);
            }

            String section = documentManager.getDocument(
                    getCurrentDocument().getParentRef()).getName();

            Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
            eventInfo.put("targetSection", section);
            eventInfo.put("proxy", currentDocument);
            eventInfo.put(WorkflowConstants.WORKFLOW_CREATOR, creator.getName());

            notifyEvent(
                    org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLICATION_REJECTED,
                    eventInfo, rejectPublishingComment, sourceDocument);
        } catch (ClientException e) {
            throw new PublishingException(e);
        }

        // Here to invalidate the dashboard items.
        Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_STOP);

        // Redirect to the parent since the workflow is expected to delete the
        // document that has been submited.

        navigationContext.resetCurrentContext();
        Events.instance().raiseEvent(
                org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SELECTION_CHANGED);

        String view = null;
        try {
            view = navigationContext.navigateToDocument(parent);
        } catch (ClientException e) {
            log.error("An error occured while redirecting.");
        }

        rejectPublishingComment = null;

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("document_rejected"),
                resourcesAccessor.getMessages().get(sourceDocument.getType()));

        return view;
    }



    protected DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    public boolean isProxy() {
        // XXX code taken from LockActionsBeanx
        boolean result = false;
        DocumentModel document = navigationContext.getCurrentContentRoot();
        if (document != null) {
            String parentDocumentType = document.getType();
            // FIXME AT: types shouldn't be harcoded here
            /* Rux NXP-1879: Multiple types can be suitable for publishing. Is the FIXME
             * fixed now?
             */
            result = publishActions.getSectionRootTypes().contains(parentDocumentType) ||
                    publishActions.getSectionTypes().contains(parentDocumentType);
        }
        return result;
    }

    public boolean canManagePublishing() throws PublishingException {
        // Current document is a proxy and the current user has a publishing
        // task.
        PublishingTasks tasks = new PublishingTasks(navigationContext.getCurrentDocument(), currentUser);
        return isProxy() && tasks.getPublishingWorkItem() != null;
    }

    public String getRejectPublishingComment() {
        return rejectPublishingComment;
    }

    public void setRejectPublishingComment(String rejectPublishingComment) {
        this.rejectPublishingComment = rejectPublishingComment;
    }

    private void notifyEvent(String eventId, Map<String, Serializable> infoMap,
            String comment, DocumentModel documentModel) throws ClientException {
        publishActions.notifyEvent(eventId, infoMap, comment, null,
                documentModel);
    }

}
