/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.comment.workflow.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.task.CreateTask;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.CommentConstants;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.service.DocumentTaskProvider;
import org.nuxeo.runtime.api.Framework;

public class CommentsModerationServiceImpl implements CommentsModerationService {

    private static final Log log = LogFactory.getLog(CommentsModerationService.class);

    @Override
    public void startModeration(CoreSession session, DocumentModel doc, String commentID, ArrayList<String> moderators)
            {
        TaskService taskService = Framework.getService(TaskService.class);
        if (moderators == null || moderators.isEmpty()) {
            throw new NuxeoException("No moderators defined");
        }
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(CommentsConstants.COMMENT_ID, commentID);
        vars.put(Task.TaskVariableName.needi18n.name(), "true");
        vars.put(Task.TaskVariableName.taskType.name(), CommentConstants.COMMENT_TASK_TYPE);

        vars.put(CreateTask.OperationTaskVariableName.createdFromCreateTaskOperation.name(), "false");
        vars.put(CreateTask.OperationTaskVariableName.acceptOperationChain.name(), CommentsConstants.ACCEPT_CHAIN_NAME);
        vars.put(CreateTask.OperationTaskVariableName.rejectOperationChain.name(), CommentsConstants.REJECT_CHAIN_NAME);

        taskService.createTask(session, session.getPrincipal(), doc, CommentsConstants.MODERATION_DIRECTIVE_NAME,
                moderators, false, null, null, null, vars, null);
    }

    public Task getModerationTask(TaskService taskService, CoreSession session, DocumentModel doc, String commentId)
            {
        List<Task> tasks = DocumentTaskProvider.getTasks("GET_COMMENT_MODERATION_TASKS", session, false, null,
                doc.getId(), session.getPrincipal().getName(), commentId);
        if (tasks != null && !tasks.isEmpty()) {
            if (tasks.size() > 1) {
                log.error("There are several moderation workflows running, " + "taking only first found");
            }
            Task task = tasks.get(0);
            return task;
        }
        return null;
    }

    @Override
    public void approveComent(CoreSession session, DocumentModel doc, String commentId) {
        TaskService taskService = Framework.getService(TaskService.class);
        Task moderationTask = getModerationTask(taskService, session, doc, commentId);
        if (moderationTask == null) {
            session.followTransition(new IdRef(commentId), CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);
        } else {
            taskService.acceptTask(session, session.getPrincipal(), moderationTask, null);
        }

        Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
        eventInfo.put("emailDetails", "test");
        notifyEvent(session, CommentsConstants.COMMENT_PUBLISHED, null, null, null, doc);
    }

    @Override
    public void rejectComment(CoreSession session, DocumentModel doc, String commentId) {
        TaskService taskService = Framework.getService(TaskService.class);
        Task moderationTask = getModerationTask(taskService, session, doc, commentId);
        if (moderationTask == null) {
            session.followTransition(new IdRef(commentId), CommentsConstants.REJECT_STATE);
        } else {
            taskService.rejectTask(session, session.getPrincipal(), moderationTask, null);
        }
    }

    @Override
    public void publishComment(CoreSession session, DocumentModel comment) {
        session.followTransition(comment.getRef(), CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);

        notifyEvent(session, CommentsConstants.COMMENT_PUBLISHED, null, null, null, comment);
    }

    protected void notifyEvent(CoreSession session, String eventId, Map<String, Serializable> properties,
            String comment, String category, DocumentModel dm) {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        properties.put(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID, session.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE, dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), dm);

        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        EventProducer evtProducer = Framework.getService(EventProducer.class);
        Event event = ctx.newEvent(eventId);
        evtProducer.fireEvent(event);
    }

}
