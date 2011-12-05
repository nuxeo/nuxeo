/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.CommentConstants;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskQueryConstant;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

public class CommentsModerationServiceImpl implements CommentsModerationService {

    private static final Log log = LogFactory.getLog(CommentsModerationService.class);

    @Override
    public void startModeration(CoreSession session, DocumentModel doc,
            String commentID, ArrayList<String> moderators)
            throws ClientException {
        TaskService taskService = getTaskService();
        if (moderators == null || moderators.isEmpty()) {
            throw new ClientException("No moderators defined");
        }
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(CommentsConstants.COMMENT_ID, commentID);
        vars.put(
                Task.TaskVariableName.needi18n.name(), "true");
        vars.put(
                Task.TaskVariableName.taskType.name(), CommentConstants.COMMENT_TASK_TYPE);

        vars.put(CreateTask.OperationTaskVariableName.createdFromCreateTaskOperation.name(),
                "false");
        vars.put(CreateTask.OperationTaskVariableName.acceptOperationChain.name(),
                CommentsConstants.ACCEPT_CHAIN_NAME);
        vars.put(CreateTask.OperationTaskVariableName.rejectOperationChain.name(),
                CommentsConstants.REJECT_CHAIN_NAME);

        taskService.createTask(session,
                (NuxeoPrincipal) session.getPrincipal(), doc,
                CommentsConstants.MODERATION_DIRECTIVE_NAME, moderators,
                false, null, null, null, vars, null);
    }

    public Task getModerationTask(TaskService taskService,
            CoreSession session, DocumentModel doc, String commentId)
            throws ClientException {
        String query = TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY;
        query = String.format(query, doc.getId(), session.getPrincipal().getName());
        String commentWhereClause = TaskQueryConstant.getVariableWhereClause(CommentsConstants.COMMENT_ID, commentId);
        List<DocumentModel> tasks = session.query(String.format("%s AND %s", query, commentWhereClause));

        if (tasks != null && !tasks.isEmpty()) {
            if (tasks.size() > 1) {
                log.error("There are several moderation workflows running, "
                        + "taking only first found");
            }
            Task task = tasks.get(0).getAdapter(Task.class);
            return task;
        }
        return null;
    }

    @Override
    public void approveComent(CoreSession session, DocumentModel doc,
            String commentId) throws ClientException {
        TaskService taskService = getTaskService();
        Task moderationTask = getModerationTask(taskService, session, doc,
                commentId);
        if (moderationTask == null) {
            // throw new ClientException("No moderation task found");
            session.followTransition(new IdRef(commentId),
                    CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);
        } else {
            taskService.acceptTask(session, (NuxeoPrincipal) session.getPrincipal(), moderationTask, null);
        }

        Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
        eventInfo.put("emailDetails", "test");
        notifyEvent(session, CommentsConstants.COMMENT_PUBLISHED, null, null,
                null, doc);
    }

    @Override
    public void rejectComment(CoreSession session, DocumentModel doc,
            String commentId) throws ClientException {
        TaskService taskService = getTaskService();
        Task moderationTask = getModerationTask(getTaskService(), session, doc,
                commentId);
        if (moderationTask == null) {
            // throw new ClientException("No moderation task found");
            session.followTransition(new IdRef(commentId),
                    CommentsConstants.REJECT_STATE);
        } else {
            taskService.rejectTask(session, (NuxeoPrincipal) session.getPrincipal(), moderationTask, null);
        }
    }

    @Override
    public void publishComment(CoreSession session, DocumentModel comment)
            throws ClientException {
        session.followTransition(comment.getRef(),
                CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);

        notifyEvent(session, CommentsConstants.COMMENT_PUBLISHED, null, null,
                null, comment);
    }

    protected static TaskService getTaskService() throws ClientException {
        try {
            return Framework.getService(TaskService.class);
        } catch (Exception e) {
            log.error(e);
            throw new ClientException(e);
        }
    }

    protected void notifyEvent(CoreSession session, String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm) throws ClientException {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        properties.put(CoreEventConstants.REPOSITORY_NAME,
                session.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID, session.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(session,
                session.getPrincipal(), dm);

        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        EventProducer evtProducer = null;

        try {
            evtProducer = Framework.getService(EventProducer.class);
        } catch (Exception e) {
            log.error("Unable to access EventProducer", e);
            return;
        }

        Event event = ctx.newEvent(eventId);

        try {
            evtProducer.fireEvent(event);
        } catch (Exception e) {
            log.error("Error while sending event", e);
        }
    }

}
