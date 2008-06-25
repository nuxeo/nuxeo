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
 *     ${user}
 *
 * $Id
 */

package org.nuxeo.ecm.platform.forum.web;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.api.ECM;
import org.nuxeo.ecm.platform.api.login.SystemSession;
import org.nuxeo.ecm.platform.comment.web.CommentManagerActions;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.forum.web.api.PostAction;
import org.nuxeo.ecm.platform.forum.web.api.ThreadAction;
import org.nuxeo.ecm.platform.forum.workflow.ForumConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventCategories;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventTypes;
import org.nuxeo.ecm.platform.workflow.web.api.WorkflowBeansDelegate;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.dashboard.DashboardActions;

/**
 * This action listener is used to create a Post inside a Thread and also to
 * handle the moderation cycle on Post.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */

@Name("postAction")
@Scope(ScopeType.CONVERSATION)
public class PostActionBean extends InputController implements PostAction {

    static final String WRITE = "ReadWrite";

    private static final Log log = LogFactory.getLog(PostActionBean.class);

    private static final long serialVersionUID = 2948023661103514559L;

    @In(required = false)
    protected RepositoryLocation currentServerLocation;

    @In(create = true)
    protected WorkflowBeansDelegate workflowBeansDelegate;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true)
    protected transient CommentManagerActions commentManagerActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected ThreadAction threadAction;

    //NXP-1360 need it to invalidate Dashboard
    @In(required = false)
    protected transient DashboardActions dashboardActions;

    // the id of the comment to delete
    @RequestParameter
    protected String deletePostId;

    private String title;

    private String text;

    private String filename;

    private Blob fileContent;


    @Destroy
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Blob getFileContent() {
        return fileContent;
    }

    public void setFileContent(Blob fileContent) {
        this.fileContent = fileContent;
    }

    public boolean checkWritePermissionOnThread() {
        boolean allowed;
        try {
            allowed = documentManager.hasPermission(
                    navigationContext.getCurrentDocument().getRef(), WRITE);
        } catch (ClientException e) {
            e.printStackTrace();
            allowed = false;
        }
        return allowed;
    }

    /**
     * Add the post to the thread and start the WF the moderation on the post
     * created.
     */
    public String addPost() throws ClientException, WMWorkflowException {
        DocumentModel dm = documentManager.createDocumentModel("Post");

        dm.setProperty("post", "author",
                commentManagerActions.getPrincipalName());

        dm.setProperty("post", "title", title);
        dm.setProperty("post", "text", text);
        dm.setProperty("post", "creationDate", new Date());
        dm.setProperty("post", "filename", filename);
        dm.setProperty("post", "fileContent", fileContent);

        // this is the post we've just created
        dm = commentManagerActions.addComment(dm);

        /*NXP-1262 display the message only when about to publish
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(
                        "label.comment.added.sucess"));*/

        boolean publish = false;

        // We start the moderation, only if the thread has the moderated
        if (threadAction.isCurrentThreadModerated()) {
            // if the user is not a moderator we warn him that his post won't be
            // displayed
            if (!threadAction.isPrincipalModerator()) {
                // This will start the WF on our post
                startModeration(dm);
                facesMessages.add(FacesMessage.SEVERITY_INFO,
                        resourcesAccessor.getMessages().get(
                                "label.comment.waiting_approval"));
            } else {
                // If the user is a moderator, his post is automatically
                // published
                publish = true;
            }

        } else {
            publish = true;
        }

        dm = documentManager.getDocument(dm.getRef());
        if (publish) {
            //NXP-1262 display the message only when about to publish
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.comment.added.sucess"));
            if (documentManager.hasPermission(dm.getRef(),
                    SecurityConstants.WRITE_LIFE_CYCLE)) {
                documentManager.followTransition(dm.getRef(),
                        ForumConstants.TRANSITION_TO_PUBLISHED_STATE);

                documentManager.save();
            } else {
                try {
                    // Here user only granted with read rights should be able to
                    // create a post.
                    SystemSession session = new SystemSession();
                    session.login();

                    // Open a new repository session which will be
                    // unrestricted. We need to do this here since the
                    // document manager in Seam context has been initialized
                    // with caller principal rights.
                    CoreSession unrestrictedSession = ECM.getPlatform().openRepository(
                            currentServerLocation.getName());

                    // Follow transition
                    unrestrictedSession.followTransition(dm.getRef(),
                            ForumConstants.TRANSITION_TO_PUBLISHED_STATE);

                    // Close the unrestricted session.
                    CoreInstance.getInstance().close(unrestrictedSession);

                    // Logout the system session.
                    // Note, this is not necessary to take further actions
                    // here regarding the user session.
                    session.logout();

                } catch (Exception e) {
                    throw new ClientException(e.getMessage());
                }

            }
        }

        // To force comment manager to reload posts
        Events.instance().raiseEvent(
                org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SELECTION_CHANGED);

        cleanContextVariables();

        return navigationContext.navigateToDocument(getParentThread());
    }

    public String deletePost() throws ClientException, WMWorkflowException {

        DocumentModel post = documentManager.getDocument(new IdRef(deletePostId));

        boolean deletionModerated = false;

        if (threadAction.isCurrentThreadModerated()
                && ForumConstants.PENDING_STATE.equals(post.getCurrentLifeCycleState())) {
            String pid = getPidFor(post);
            if (pid != null) {
                WAPI wapi = workflowBeansDelegate.getWAPIBean();
                wapi.terminateProcessInstance(pid);
            }
            deletionModerated = true;
        }
        commentManagerActions.deleteComment(post.getRef().toString());
        //NXP-1360 if moderation on
        if (deletionModerated) {
            //NXP-1360 signal post was deleted, invalidate user dashboard items
            if (dashboardActions != null) {
                try {
                    dashboardActions.invalidateDashboardItems();
                } catch (ClientException e) {
                    throw new WMWorkflowException(e.getMessage());
                }
            }
        }
        return navigationContext.navigateToDocument(getParentThread());
    }

    public String cancelPost() throws ClientException {
        cleanContextVariables();
        commentManagerActions.cancelComment();
        return navigationContext.navigateToDocument(getParentThread());
    }

    private void cleanContextVariables() {
        fileContent = null;
        filename = null;
        text = null;
        title = null;
    }

    /**
     * Start the moderation on given Post.
     *
     * @param post
     * @return
     * @throws WMWorkflowException
     * @throws ClientException
     */
    public WMActivityInstance startModeration(DocumentModel post)
            throws WMWorkflowException, ClientException {

        List<String> moderators = getModeratorsOnParentThread();

        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        String processId = getModerationWorkflowId();

        if (moderators == null || moderators.isEmpty()) {
            log.error("Error : No moderators are defined on parent thread. Moderation won't start");
            return null;
        }
        WMActivityInstance workflowPath = null;
        try {
            Map<String, Serializable> vars = new HashMap<String, Serializable>();

            vars.put(WorkflowConstants.DOCUMENT_REF, post.getRef());
            vars.put(ForumConstants.THREAD_REF, getParentThread().getRef());
            vars.put(ForumConstants.FORUM_MODERATORS_LIST,
                    threadAction.getModerators().toArray());
            vars.put(WorkflowConstants.DOCUMENT_LOCATION_URI,
                    post.getRepositoryName());
            workflowPath = wapi.startProcess(processId, vars, null);
        } catch (WMWorkflowException we) {
            workflowPath = null;
            log.error("An error occurred while grabbing workflow definitions");
            we.printStackTrace();
        }
        if (workflowPath != null) {
            WMProcessDefinition def = wapi.getProcessDefinitionById(processId);
            String name = def.getName();
            notifyEvent(post, WorkflowEventTypes.WORKFLOW_STARTED, name, name);
            Events.instance().raiseEvent(EventNames.WORKFLOW_NEW_STARTED);
        }

        return workflowPath;
    }

    /**
     * Gets the WF id associated to the moderation process.
     */
    public String getModerationWorkflowId() throws WMWorkflowException {
        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        return wapi.getProcessDefinitionByName(
                ForumConstants.PROCESS_INSTANCE_NAME).getId();
    }

    /**
     * Gets the current task Id.
     *
     * @return
     * @throws WMWorkflowException
     */
    public Collection<WMWorkItemInstance> getCurrentTasksForPrincipal(
            String name) throws WMWorkflowException {

        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        Collection<WMWorkItemInstance> currentTasks = null;
        if (name != null && !"".equals(name)) {

            currentTasks = wapi.getWorkItemsFor(new WMParticipantImpl(
                    wapi.getParticipant().getName()), name);
        }

        return currentTasks;
    }

    public Collection<WMWorkItemInstance> getPublishTasksForPrincipal()
            throws WMWorkflowException {
        return getCurrentTasksForPrincipal(ForumConstants.PUBLISHED_STATE);
    }

    public Collection<WMWorkItemInstance> getPendingTasksForPrincipal()
            throws WMWorkflowException {
        return getCurrentTasksForPrincipal(ForumConstants.PENDING_STATE);
    }

    /**
     * Ends the task on a post.
     */
    public String approvePost(DocumentModel post) throws WMWorkflowException,
            ClientException {
        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        if (post != null) {
            String weed = getWorkItemIdFor(post);
            if (weed != null) {
                wapi.endWorkItem(weed,
                        ForumConstants.PROCESS_TRANSITION_TO_PUBLISH);
            }
        }

        // To force comment manager to reload posts
        Events.instance().raiseEvent(
                org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SELECTION_CHANGED);
        //NXP-1360 signal post was approved, invalidate user dashboard items
        if (dashboardActions != null) {
            try {
                dashboardActions.invalidateDashboardItems();
            } catch (ClientException e) {
                throw new WMWorkflowException(e.getMessage());
            }
        }

        return navigationContext.navigateToDocument(getParentThread());
    }

    public DocumentModel getParentThread() {
        return navigationContext.getCurrentDocument();
    }

    public List<String> getModeratorsOnParentThread() {
        List<String> moderators = (List<String>) getParentThread().getProperty(
                "thread", "moderators");
        if (moderators != null) {
            return moderators;
        }

        return null;
    }

    /**
     * Notify event to Core.
     *
     * @param doc
     * @param eventId
     * @param comment
     * @param category
     * @throws WMWorkflowException
     * @throws ClientException
     */
    protected void notifyEvent(DocumentModel doc, String eventId,
            String comment, String category) throws WMWorkflowException,
            ClientException {

        log.info("NotifyEvent for post moderation");

        DocumentMessageProducer producer = workflowBeansDelegate.getDocumentMessageProducer();
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreEventConstants.DOC_LIFE_CYCLE,
                doc.getCurrentLifeCycleState());
        CoreEvent event = new CoreEventImpl(eventId, doc, props, currentUser,
                category != null ? category
                        : WorkflowEventCategories.EVENT_WORKFLOW_CATEGORY,
                comment);

        DocumentMessage msg = new DocumentMessageImpl(doc, event);
        producer.produce(msg);
    }

    public boolean isPostPublished(DocumentModel post) throws ClientException {
        boolean published = false;
        if (post != null
                && ForumConstants.PUBLISHED_STATE.equals(post.getCurrentLifeCycleState())) {
            published = true;
        }
        return published;
    }

    /**
     * Gets the title of the post for creation purpose. If the post to be created
     * reply to a previous post, the title of the new post comes with the
     * previous title, and a prefix (i.e : Re : Previous Title).
     */
    public String getTitle() throws ClientException {

        String previousId = commentManagerActions.getSavedReplyCommentId();
        if (previousId != null && !"".equals(previousId)) {
            DocumentModel previousPost = documentManager.getDocument(new IdRef(
                    previousId));

            // Test to ensure that previous comment got the "post" schema
            if (previousPost.getDataModel("post") != null) {
                String previousTitle = (String) previousPost.getProperty(
                        "post", "title");
                String prefix = resourcesAccessor.getMessages().get(
                        "label.forum.post.title.prefix");
                title = prefix + previousTitle;
            }
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private WMWorkItemInstance getWorkItemsForUserFrom(
            Collection<WMWorkItemInstance> witems, DocumentModel post,
            String principalName) throws WMWorkflowException {

        WMWorkItemInstance witem = null;

        WAPI wapi = workflowBeansDelegate.getWAPIBean();

        for (WMWorkItemInstance item : witems) {
            String pid = item.getProcessInstance().getId();
            Map<String, Serializable> props = wapi.listProcessInstanceAttributes(pid);
            DocumentRef ref = (DocumentRef) props.get(WorkflowConstants.DOCUMENT_REF);
            if (ref != null && ref.equals(post.getRef())) {
                witem = item;
                break;
            }
        }
        return witem;
    }

    private WMWorkItemInstance getWorkItemFor(DocumentModel post)
            throws WMWorkflowException {

        if (post == null) {
            return null;
        }

        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        Collection<WMWorkItemInstance> witems = wapi.getWorkItemsFor(
                new WMParticipantImpl(currentUser.getName()), null);
        WMWorkItemInstance witem = getWorkItemsForUserFrom(witems, post, currentUser.getName());
        if (witem == null) {
            // Group resolution.
            if (currentUser instanceof NuxeoPrincipal) {
                List<String> groupNames = ((NuxeoPrincipal) currentUser).getAllGroups();
                for (String groupName : groupNames) {
                    witems = wapi.getWorkItemsFor(
                            new WMParticipantImpl(groupName), null);
                    witem = getWorkItemsForUserFrom(witems, post, groupName);
                    if (witem != null) {
                        break;
                    }
                }
            }
        }

        return witem;
    }

    private String getWorkItemIdFor(DocumentModel post)
            throws WMWorkflowException {

        if (post == null) {
            return null;
        }

        WMWorkItemInstance item = getWorkItemFor(post);
        String weed = null;
        if (item != null) {
            weed = item.getId();
        }

        return weed;
    }

    private String getPidFor(DocumentModel post) throws WMWorkflowException {
        String pid = null;

        WMWorkItemInstance item = getWorkItemFor(post);
        if (item != null) {
            pid = item.getProcessInstance().getId();
        }

        return pid;
    }

    public String rejectPost(DocumentModel post) throws WMWorkflowException,
            ClientException {
        WAPI wapi = workflowBeansDelegate.getWAPIBean();
        if (post != null) {
            String weed = getWorkItemIdFor(post);
            if (weed != null) {
                wapi.endWorkItem(weed,
                        ForumConstants.PROCESS_TRANSITION_TO_REJECTED);
            }
        }

        // To force comment manager to reload posts
        Events.instance().raiseEvent(
                org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SELECTION_CHANGED);
        //NXP-1360 signal post was rejected, invalidate user dashboard items
        if (dashboardActions != null) {
            try {
                dashboardActions.invalidateDashboardItems();
            } catch (ClientException e) {
                throw new WMWorkflowException(e.getMessage());
            }
        }

        return navigationContext.navigateToDocument(getParentThread());
    }

}
