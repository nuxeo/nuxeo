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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
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
import org.nuxeo.ecm.platform.comment.workflow.CommentWorkflowFilter;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmService.VariableName;
import org.nuxeo.runtime.api.Framework;


public class CommentsModerationServiceImpl implements CommentsModerationService {

    private static final Log log = LogFactory.getLog(CommentsModerationService.class);

    public void startModeration(CoreSession session, DocumentModel doc,
            String commentID, ArrayList<String> moderators)
            throws ClientException {
        JbpmService jbpmService = getJbpmService();
        if (moderators == null || moderators.isEmpty()) {
            throw new ClientException("No moderators defined");
        }
        Map<String, Serializable> vars = new HashMap<String, Serializable>();
        vars.put(VariableName.participants.name(), moderators);
        vars.put(CommentsConstants.COMMENT_ID, commentID);
        jbpmService.createProcessInstance(
                (NuxeoPrincipal) session.getPrincipal(),
                CommentsConstants.MODERATION_PROCESS, doc, vars, null);
    }

    public ProcessInstance getModerationProcess(JbpmService jbpmService,
            CoreSession session, DocumentModel doc, String commentId)
            throws ClientException {
        List<ProcessInstance> processes = jbpmService.getProcessInstances(doc,
                (NuxeoPrincipal) session.getPrincipal(),
                new CommentWorkflowFilter(commentId));
        if (processes != null && !processes.isEmpty()) {
            if (processes.size() > 1) {
                log.error("There are several moderation workflows running, "
                        + "taking only first found");
            }
            return processes.get(0);
        }
        return null;
    }

    public void approveComent(CoreSession session, DocumentModel doc,
            String commentId) throws ClientException {
        JbpmService jbpmService = getJbpmService();
        TaskInstance moderationTask = getModerationTask(jbpmService, session,
                doc, commentId);
        if (moderationTask == null) {
            //throw new ClientException("No moderation task found");
            session.followTransition(new IdRef(commentId), CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);
        } else {
        jbpmService.endTask(moderationTask.getId(),
                CommentsConstants.TRANSITION_TO_PUBLISHED_STATE, null, null,
                null, (NuxeoPrincipal) session.getPrincipal());
        }

        Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
        eventInfo.put("emailDetails", "test");
        notifyEvent(session, CommentsConstants.COMMENT_PUBLISHED, null, null, null, doc);
    }

    public void rejectComment(CoreSession session, DocumentModel doc,
            String commentId) throws ClientException {
        JbpmService jbpmService = getJbpmService();
        TaskInstance moderationTask = getModerationTask(jbpmService, session,
                doc, commentId);
        if (moderationTask == null) {
            //throw new ClientException("No moderation task found");
            session.followTransition(new IdRef(commentId), CommentsConstants.REJECT_STATE);
        } else {
            jbpmService.endTask(moderationTask.getId(),
                CommentsConstants.REJECT_STATE, null, null, null,
                (NuxeoPrincipal) session.getPrincipal());
        }
    }

    protected TaskInstance getModerationTask(JbpmService jbpmService,
            CoreSession session, DocumentModel doc, String commentId)
            throws ClientException {
        ProcessInstance process = getModerationProcess(jbpmService, session,
                doc, commentId);
        if (process != null) {
            Collection tasks = process.getTaskMgmtInstance().getTaskInstances();
            if (tasks != null && !tasks.isEmpty()) {
                if (tasks.size() > 1) {
                    log.error("There are several moderation tasks, "
                            + "taking only first found");
                }
                TaskInstance task = (TaskInstance) tasks.iterator().next();
                return task;
            }
        }
        return null;
    }

    public void publishComment(CoreSession session, DocumentModel comment)
            throws ClientException {
        session.followTransition(comment.getRef(),
                CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);

        notifyEvent(session, CommentsConstants.COMMENT_PUBLISHED, null, null, null, comment);
    }

    protected static JbpmService getJbpmService() throws ClientException {
        try {
            return Framework.getService(JbpmService.class);
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
