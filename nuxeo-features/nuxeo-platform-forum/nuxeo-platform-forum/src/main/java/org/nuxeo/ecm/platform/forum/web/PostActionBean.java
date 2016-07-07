/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     ${user}
 *
 * $Id
 */

package org.nuxeo.ecm.platform.forum.web;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.automation.task.CreateTask;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.comment.web.CommentManagerActions;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.forum.web.api.PostAction;
import org.nuxeo.ecm.platform.forum.web.api.ThreadAction;
import org.nuxeo.ecm.platform.forum.workflow.ForumConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.service.DocumentTaskProvider;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This action listener is used to create a Post inside a Thread and also to handle the moderation cycle on Post.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
@Name("postAction")
@Scope(ScopeType.CONVERSATION)
public class PostActionBean implements PostAction {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PostActionBean.class);

    @In(create = true)
    protected ThreadAction threadAction;

    @In(create = true)
    protected transient CommentManagerActions commentManagerActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient TaskService taskService;

    @In(required = false)
    protected RepositoryLocation currentServerLocation;

    @In(create = true)
    protected transient Principal currentUser;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    // the id of the comment to delete
    @RequestParameter
    protected String deletePostId;

    protected String title;

    protected String text;

    protected String filename;

    protected Blob fileContent;

    @Override
    public boolean checkWritePermissionOnThread() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            return documentManager.hasPermission(currentDocument.getRef(), SecurityConstants.READ_WRITE);
        } else {
            log.error("Cannot check write permission on thread: " + "no current document found");
        }
        return false;
    }

    /**
     * Adds the post to the thread and starts the moderation WF on the post created.
     */
    @Override
    public String addPost() {
        DocumentModel dm = documentManager.createDocumentModel("Post");

        dm.setProperty("post", "author", commentManagerActions.getPrincipalName());

        dm.setProperty("post", "title", title);
        dm.setProperty("post", "text", text);
        dm.setProperty("post", "creationDate", new Date());
        dm.setProperty("post", "filename", filename);
        dm.setProperty("post", "fileContent", fileContent);

        // save it to the repository
        dm = commentManagerActions.addComment(dm);

        if (threadAction.isCurrentThreadModerated() && !threadAction.isPrincipalModerator()) {
            // start moderation workflow + warn user that post
            // won't be displayed until moderation kicks in
            startModeration(dm);
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("label.comment.waiting_approval"));
        } else {
            // publish post
            DocumentRef postRef = dm.getRef();
            if (documentManager.hasPermission(postRef, SecurityConstants.WRITE_LIFE_CYCLE)) {
                documentManager.followTransition(postRef, ForumConstants.TRANSITION_TO_PUBLISHED_STATE);
                documentManager.save();
            } else {
                // Here user only granted with read rights should be able to
                // create a post => open a system session to put it in
                // published
                // state
                try (CoreSession systemSession = CoreInstance.openCoreSessionSystem(currentServerLocation.getName())) {
                    // follow transition
                    systemSession.followTransition(dm.getRef(), ForumConstants.TRANSITION_TO_PUBLISHED_STATE);
                    systemSession.save();
                }
            }
            // NXP-1262 display the message only when about to publish
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("label.comment.added.sucess"));
        }

        // force comment manager to reload posts
        commentManagerActions.documentChanged();
        cleanContextVariables();

        return navigationContext.navigateToDocument(getParentThread());
    }

    @Override
    public String cancelPost() {
        cleanContextVariables();
        commentManagerActions.cancelComment();
        return navigationContext.navigateToDocument(getParentThread());
    }

    @Override
    public String deletePost() {
        if (deletePostId == null) {
            throw new NuxeoException("No id for post to delete");
        }

        DocumentModel thread = getParentThread();
        DocumentModel post = documentManager.getDocument(new IdRef(deletePostId));

        if (threadAction.isThreadModerated(thread)
                && ForumConstants.PENDING_STATE.equals(post.getCurrentLifeCycleState())) {
            Task task = getModerationTask(thread, deletePostId);
            if (task != null) {
                taskService.deleteTask(documentManager, task.getId());
            }
        }
        commentManagerActions.deleteComment(deletePostId);

        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_ENDED);

        return navigationContext.navigateToDocument(getParentThread());
    }

    @Override
    public String rejectPost(DocumentModel post) {
        DocumentModel thread = getParentThread();

        Task moderationTask = getModerationTask(thread, post.getId());
        if (moderationTask == null) {
            throw new NuxeoException("No moderation task found");
        }

        taskService.rejectTask(documentManager, (NuxeoPrincipal) currentUser, moderationTask, null);

        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);

        // force comment manager to reload posts
        commentManagerActions.documentChanged();

        return navigationContext.navigateToDocument(getParentThread());
    }

    /**
     * Ends the task on a post.
     */
    @Override
    public String approvePost(DocumentModel post) {
        DocumentModel thread = getParentThread();

        Task moderationTask = getModerationTask(thread, post.getId());
        if (moderationTask == null) {
            throw new NuxeoException("No moderation task found");
        }
        taskService.acceptTask(documentManager, (NuxeoPrincipal) currentUser, moderationTask, null);

        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_TASK_COMPLETED);

        // force comment manager to reload posts
        commentManagerActions.documentChanged();

        return navigationContext.navigateToDocument(getParentThread());
    }

    @Override
    public DocumentModel getParentThread() {
        return navigationContext.getCurrentDocument();
    }

    @Override
    public boolean isPostPublished(DocumentModel post) {
        boolean published = false;
        if (post != null && ForumConstants.PUBLISHED_STATE.equals(post.getCurrentLifeCycleState())) {
            published = true;
        }
        return published;
    }

    /**
     * Starts the moderation on given Post.
     */
    @SuppressWarnings("unchecked")
    protected void startModeration(DocumentModel post) {

        DocumentModel thread = getParentThread();
        List<String> moderators = (ArrayList<String>) thread.getProperty("thread", "moderators");

        if (moderators == null || moderators.isEmpty()) {
            throw new NuxeoException("No moderators defined");
        }

        Map<String, String> vars = new HashMap<String, String>();
        vars.put(ForumConstants.COMMENT_ID, post.getId());
        vars.put(CreateTask.OperationTaskVariableName.createdFromCreateTaskOperation.name(), "false");
        vars.put(Task.TaskVariableName.needi18n.name(), "true");
        vars.put(Task.TaskVariableName.taskType.name(), ForumConstants.FORUM_TASK_TYPE);

        vars.put(CreateTask.OperationTaskVariableName.acceptOperationChain.name(), CommentsConstants.ACCEPT_CHAIN_NAME);
        vars.put(CreateTask.OperationTaskVariableName.rejectOperationChain.name(), CommentsConstants.REJECT_CHAIN_NAME);

        taskService.createTask(documentManager, (NuxeoPrincipal) currentUser, thread,
                ForumConstants.MODERATION_TASK_NAME, moderators, false, ForumConstants.MODERATION_TASK_NAME, null,
                null, vars, null);
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_NEW_STARTED);

    }

    protected Task getModerationTask(DocumentModel thread, String postId) {
        List<Task> tasks = DocumentTaskProvider.getTasks("GET_FORUM_MODERATION_TASKS", documentManager, false, null,
                thread.getId(), postId);
        if (tasks != null && !tasks.isEmpty()) {
            if (tasks.size() > 1) {
                log.error("There are several moderation workflows running, " + "taking only first found");
            }
            Task task = tasks.get(0);
            return task;
        }
        return null;
    }

    protected void cleanContextVariables() {
        fileContent = null;
        filename = null;
        text = null;
        title = null;
    }

    // getters/setters

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public Blob getFileContent() {
        return fileContent;
    }

    @Override
    public void setFileContent(Blob fileContent) {
        this.fileContent = fileContent;
    }

    /**
     * Gets the title of the post for creation purpose. If the post to be created reply to a previous post, the title of
     * the new post comes with the previous title, and a prefix (i.e : Re : Previous Title).
     */
    @Override
    public String getTitle() {

        String previousId = commentManagerActions.getSavedReplyCommentId();
        if (previousId != null && !"".equals(previousId)) {
            DocumentModel previousPost = documentManager.getDocument(new IdRef(previousId));

            // Test to ensure that previous comment got the "post" schema
            if (previousPost.getDataModel("post") != null) {
                String previousTitle = (String) previousPost.getProperty("post", "title");
                String prefix = resourcesAccessor.getMessages().get("label.forum.post.title.prefix");
                title = prefix + previousTitle;
            }
        }
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

}
