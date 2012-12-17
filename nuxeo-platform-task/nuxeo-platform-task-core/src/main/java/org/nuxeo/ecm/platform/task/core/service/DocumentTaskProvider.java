/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.task.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskProvider;
import org.nuxeo.ecm.platform.task.TaskQueryConstant;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;

/**
 * @author Laurent Doguin
 * @author Antoine Taillefer
 * @since 5.5
 */
public class DocumentTaskProvider implements TaskProvider {

    private static final long serialVersionUID = 1L;

    private final static Log log = LogFactory.getLog(DocumentTaskProvider.class);

    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession)
            throws ClientException {

        // Get tasks for current user
        // We need to build the task actors list: prefixed and unprefixed names
        // of the principal and all its groups
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        List<String> actors = TaskActorsHelper.getTaskActors(principal);

        return getCurrentTaskInstances(actors, coreSession);
    }

    /**
     * Returns a list of task instances assigned to one of the actors in the
     * list or to its pool.
     *
     * @param actors a list used as actorId to retrieve the tasks.
     * @param filter
     * @return
     * @throws ClientException
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors,
            CoreSession coreSession) throws ClientException {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<Task>();
        }
        String userNames = TaskQueryConstant.formatStringList(actors);
        String query = String.format(
                TaskQueryConstant.GET_TASKS_FOR_ACTORS_QUERY, userNames);
        return queryTasksUnrestricted(query, coreSession);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user,
            CoreSession coreSession) throws ClientException {
        String query;
        if (user == null) {
            query = String.format(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENT_QUERY,
                    dm.getId());
        } else {
            List<String> actors = TaskActorsHelper.getTaskActors(user);
            String userNames = TaskQueryConstant.formatStringList(actors);
            query = String.format(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY,
                    dm.getId(), userNames);
        }
        return queryTasksUnrestricted(query, coreSession);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors,
            CoreSession coreSession) throws ClientException {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<Task>();
        }
        String userNames = TaskQueryConstant.formatStringList(actors);
        String query = String.format(
                TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY,
                dm.getId(), userNames);
        return queryTasksUnrestricted(query, coreSession);
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, CoreSession session)
            throws ClientException {
        String query = String.format(
                TaskQueryConstant.GET_TASKS_FOR_PROCESS_ID_QUERY, processId);
        return queryTasksUnrestricted(query, session);
    }

    @Override
    public List<Task> getAllTaskInstances(String processId,
            NuxeoPrincipal user, CoreSession session) throws ClientException {
        List<String> actors = TaskActorsHelper.getTaskActors(user);
        return getAllTaskInstances(processId, actors, session);
    }

    @Override
    public List<Task> getAllTaskInstances(String processId,
            List<String> actors, CoreSession session) throws ClientException {
        String userNames = TaskQueryConstant.formatStringList(actors);
        String query = String.format(
                TaskQueryConstant.GET_TASKS_FOR_PROCESS_ID_AND_ACTORS_QUERY,
                processId, userNames);
        return queryTasksUnrestricted(query, session);
    }

    protected List<Task> queryTasksUnrestricted(final String query,
            CoreSession session) throws ClientException {
        final List<Task> tasks = new ArrayList<Task>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                DocumentModelList taskDocuments = session.query(query);
                tasks.addAll(wrapDocModelInTask(taskDocuments, true));
            }
        }.runUnrestricted();
        return tasks;
    }

    public static List<Task> wrapDocModelInTask(DocumentModelList taskDocuments)
            throws ClientException {
        return wrapDocModelInTask(taskDocuments, false);
    }

    /**
     * Converts a {@link DocumentModelList} to a list of {@link Task}s.
     *
     * @param detach if {@code true}, detach each document before converting it
     *            to a {@code Task}.
     */
    public static List<Task> wrapDocModelInTask(
            DocumentModelList taskDocuments, boolean detach)
            throws ClientException {
        List<Task> tasks = new ArrayList<Task>();
        for (DocumentModel doc : taskDocuments) {
            if (detach) {
                doc.detach(true);
            }
            tasks.add(doc.getAdapter(Task.class));
        }
        return tasks;
    }

    @Override
    public String endTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment, String eventName, boolean isValidated)
            throws ClientException {

        // put user comment on the task
        if (!StringUtils.isEmpty(comment)) {
            task.addComment(principal.getName(), comment);
        }

        // end the task, adding boolean marker that task was validated or
        // rejected
        task.setVariable(TaskService.VariableName.validated.name(),
                String.valueOf(isValidated));
        task.end(coreSession);
        coreSession.saveDocument(task.getDocument());
        // notify
        Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
        ArrayList<String> notificationRecipients = new ArrayList<String>();
        notificationRecipients.add(task.getInitiator());
        notificationRecipients.addAll(task.getActors());
        eventProperties.put(NotificationConstants.RECIPIENTS_KEY,
                notificationRecipients);
        // try to resolve document when notifying
        DocumentModel document = null;
        String docId = task.getVariable(TaskService.VariableName.documentId.name());
        String docRepo = task.getVariable(TaskService.VariableName.documentRepositoryName.name());
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

        TaskEventNotificationHelper.notifyEvent(coreSession, document,
                principal, task, eventName, eventProperties, comment, null);

        String seamEventName = isValidated ? TaskEventNames.WORKFLOW_TASK_COMPLETED
                : TaskEventNames.WORKFLOW_TASK_REJECTED;
        return seamEventName;
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, String nodeId,
            CoreSession session) throws ClientException {
        String query = String.format(
                TaskQueryConstant.GET_TASKS_FOR_PROCESS_ID_AND_NODE_ID_QUERY,
                processId, nodeId);
        return queryTasksUnrestricted(query, session);
    }
}
