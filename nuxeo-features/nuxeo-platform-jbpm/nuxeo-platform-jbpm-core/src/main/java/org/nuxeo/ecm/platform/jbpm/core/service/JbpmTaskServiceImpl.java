/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.jbpm.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the {@link JbpmTaskService}
 *
 * @author Anahide Tchertchian
 */
public class JbpmTaskServiceImpl implements JbpmTaskService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(JbpmTaskServiceImpl.class);

    public void createTask(CoreSession coreSession, NuxeoPrincipal principal,
            DocumentModel document, String taskName,
            List<String> prefixedActorIds, boolean createOneTaskPerActor,
            String directive, String comment, Date dueDate,
            Map<String, Serializable> taskVariables) throws NuxeoJbpmException {
        if (createOneTaskPerActor) {
            for (String prefixedActorId : prefixedActorIds) {
                createTask(coreSession, principal, document, taskName,
                        Collections.singletonList(prefixedActorId), false,
                        directive, comment, dueDate, taskVariables);
            }
        } else {
            try {
                String[] prefixedActorIdsArray = prefixedActorIds.toArray(new String[prefixedActorIds.size()]);

                // create the task
                TaskInstance task = new TaskInstance();
                task.setName(taskName);
                task.setCreate(new Date());
                task.setPooledActors(prefixedActorIdsArray);
                task.setDueDate(dueDate);
                if (!StringUtils.isEmpty(comment)) {
                    task.addComment(new Comment(principal.getName(), comment));
                }

                // add variables
                Map<String, Serializable> variables = new HashMap<String, Serializable>();
                variables.put(JbpmService.VariableName.documentId.name(),
                        document.getId());
                variables.put(
                        JbpmService.VariableName.documentRepositoryName.name(),
                        document.getRepositoryName());
                variables.put(JbpmService.VariableName.initiator.name(),
                        principal.getName());
                variables.put(JbpmService.TaskVariableName.directive.name(),
                        directive);
                variables.put(TaskVariableName.createdFromTaskService.name(),
                        "true");
                if (taskVariables != null) {
                    variables.putAll(taskVariables);
                }
                task.setVariables(variables);

                // save the task
                getJbpmService().saveTaskInstances(
                        Collections.singletonList(task));

                // notify
                Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
                ArrayList<String> notificationRecipients = new ArrayList<String>();
                notificationRecipients.add(getTaskInitiator(task));
                notificationRecipients.addAll(prefixedActorIds);
                eventProperties.put(
                        NotificationConstants.RECIPIENTS_KEY,
                        notificationRecipients.toArray(new String[notificationRecipients.size()]));

                notifyEvent(coreSession, document, principal, task,
                        JbpmEventNames.WORKFLOW_TASK_ASSIGNED, eventProperties,
                        comment, null);
                notifyEvent(coreSession, document, principal, task,
                        JbpmEventNames.WORKFLOW_TASK_ASSIGNED, eventProperties,
                        comment, null);
            } catch (ClientException e) {
                throw new NuxeoJbpmException(e);
            }
        }
    }

    public void acceptTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment) throws NuxeoJbpmException {
        endTask(coreSession, principal, task, comment,
                JbpmEventNames.WORKFLOW_TASK_COMPLETED, true);
    }

    public void rejectTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment) throws NuxeoJbpmException {
        endTask(coreSession, principal, task, comment,
                JbpmEventNames.WORKFLOW_TASK_REJECTED, false);
    }

    @SuppressWarnings("unchecked")
    public void endTask(CoreSession coreSession, NuxeoPrincipal principal,
            TaskInstance task, String comment, String eventName,
            boolean isValidated) throws NuxeoJbpmException {
        if (!canEndTask(principal, task)) {
            throw new NuxeoJbpmException(String.format(
                    "User with id '%s' cannot end this task",
                    principal.getName()));
        }
        try {
            JbpmService jbpmService = getJbpmService();
            // put user comment on the task
            if (!StringUtils.isEmpty(comment)) {
                // FIXME: all this is buggy right now, need to check with AR
                // task.addComment(new Comment(principal.getName(), comment));
                // jbpmService.saveTaskInstances(Collections.singletonList(task));
                // AddCommentOperation addCommentOperation = new
                // AddCommentOperation(
                // task.getId(), NuxeoPrincipal.PREFIX
                // + principal.getName(), comment);
                // jbpmService.executeJbpmOperation(addCommentOperation);
            }

            // end the task, adding boolean marker that task was validated or
            // rejected
            Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
            taskVariables.put(JbpmService.TaskVariableName.validated.name(),
                    String.valueOf(isValidated));
            // set variable on task directly too
            task.setVariable(JbpmService.TaskVariableName.validated.name(),
                    String.valueOf(isValidated));
            jbpmService.endTask(Long.valueOf(task.getId()), null, taskVariables,
                    null, null, principal);

            // notify
            Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
            ArrayList<String> notificationRecipients = new ArrayList<String>();
            notificationRecipients.add(getTaskInitiator(task));
            notificationRecipients.addAll(task.getPooledActors());
            eventProperties.put(NotificationConstants.RECIPIENTS_KEY,
                    notificationRecipients);
            // try to resolve document when notifying
            DocumentModel document = null;
            String docId = (String) task.getVariable(JbpmService.VariableName.documentId.name());
            String docRepo = (String) task.getVariable(JbpmService.VariableName.documentRepositoryName.name());
            if (coreSession.getRepositoryName().equals(docRepo)) {
                try {
                    document = coreSession.getDocument(new IdRef(docId));
                } catch (Exception e) {
                    log.error(
                            String.format(
                                    "Could not fetch document with id '%s:%s' for notification",
                                    docRepo, docId), e);
                }
            } else {
                log.error(String.format(
                        "Could not resolve document for notification: "
                                + "document is on repository '%s' and given session is on "
                                + "repository '%s'", docRepo,
                        coreSession.getRepositoryName()));
            }

            notifyEvent(coreSession, document, principal, task, eventName,
                    eventProperties, comment, null);

        } catch (Exception e) {
            throw new NuxeoJbpmException(e);
        }
    }

    public boolean canEndTask(NuxeoPrincipal principal, TaskInstance task)
            throws NuxeoJbpmException {
        if (task != null && (!task.isCancelled() && !task.hasEnded())) {
            return principal.isAdministrator()
                    || principal.getName().equals(getTaskInitiator(task))
                    || isTaskAssignedToUser(task, principal);
        }
        return false;
    }

    protected String getTaskInitiator(TaskInstance task) {
        return (String) task.getVariable(JbpmService.VariableName.initiator.name());
    }

    @SuppressWarnings("unchecked")
    protected boolean isTaskAssignedToUser(TaskInstance task,
            NuxeoPrincipal user) {
        if (task != null && user != null) {
            // user actors
            List<String> actors = new ArrayList<String>();
            List<String> groups = user.getAllGroups();
            String actorId = NuxeoPrincipal.PREFIX + user.getName();
            actors.add(actorId);
            for (String s : groups) {
                actors.add(NuxeoGroup.PREFIX + s);
            }

            // task actors
            if (actors.contains(task.getActorId())) {
                return true;
            }
            // pooled actor
            Set<PooledActor> pooled = task.getPooledActors();
            if (pooled != null) {
                for (PooledActor pa : pooled) {
                    if (actors.contains(pa.getActorId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected JbpmService getJbpmService() {
        try {
            return Framework.getLocalService(JbpmService.class);
        } catch (Exception e) {
            throw new IllegalStateException("Jbpm service is not deployed.", e);
        }
    }

    protected EventProducer getEventProducer() throws ClientException {
        try {
            return Framework.getService(EventProducer.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected void notifyEvent(CoreSession coreSession, DocumentModel document,
            NuxeoPrincipal principal, TaskInstance task, String eventId,
            Map<String, Serializable> properties, String comment,
            String category) throws ClientException {
        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        EventContext eventContext = null;
        if (document != null) {
            properties.put(CoreEventConstants.REPOSITORY_NAME,
                    document.getRepositoryName());
            properties.put(CoreEventConstants.SESSION_ID,
                    coreSession.getSessionId());
            properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    document.getCurrentLifeCycleState());
            eventContext = new DocumentEventContext(coreSession, principal,
                    document);
        } else {
            eventContext = new EventContextImpl(coreSession, principal);
        }
        properties.put(DocumentEventContext.COMMENT_PROPERTY_KEY, comment);
        properties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, category);
        properties.put(JbpmTaskService.TASK_INSTANCE_EVENT_PROPERTIES_KEY, task);
        String disableNotif = (String) task.getVariable(JbpmEventNames.DISABLE_NOTIFICATION_SERVICE);
        if (disableNotif != null
                && Boolean.TRUE.equals(Boolean.valueOf(disableNotif))) {
            properties.put(JbpmEventNames.DISABLE_NOTIFICATION_SERVICE,
                    Boolean.TRUE);
        }
        eventContext.setProperties(properties);

        Event event = eventContext.newEvent(eventId);
        getEventProducer().fireEvent(event);
    }

}
