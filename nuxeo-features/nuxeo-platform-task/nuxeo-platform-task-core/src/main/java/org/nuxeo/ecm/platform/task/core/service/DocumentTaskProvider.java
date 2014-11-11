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
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskProvider;
import org.nuxeo.ecm.platform.task.TaskQueryConstant;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;
import org.nuxeo.runtime.api.Framework;

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
     * Provide @param sortInfo to handle sort page-provider contributions (see
     * {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession,
            List<SortInfo> sortInfos) throws ClientException {

        // Get tasks for current user
        // We need to build the task actors list: prefixed and unprefixed names
        // of the principal and all its groups
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        List<String> actors = TaskActorsHelper.getTaskActors(principal);

        return getCurrentTaskInstances(actors, coreSession, sortInfos);
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
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_ACTORS_PP, coreSession,
                true, null, actors);
    }

    /**
     * Provide @param sortInfo to handle sort page-provider contributions (see
     * {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors,
            CoreSession coreSession, List<SortInfo> sortInfos)
            throws ClientException {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<Task>();
        }
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_ACTORS_PP, coreSession,
                true, sortInfos, actors);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user,
            CoreSession coreSession) throws ClientException {
        if (user == null) {
            return getTasks(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_PP,
                    coreSession, true, null, dm.getId(), dm.getId());
        } else {
            List<String> actors = TaskActorsHelper.getTaskActors(user);
            return getTasks(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP,
                    coreSession, true, null, dm.getId(), dm.getId(), actors);
        }
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors,
            CoreSession coreSession) throws ClientException {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<Task>();
        }
        return getTasks(
                TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP,
                coreSession, true, null, dm.getId(), dm.getId(), actors);
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, CoreSession session)
            throws ClientException {
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_PROCESS_PP, session,
                true, null, processId);
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
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_PROCESS_AND_ACTORS_PP,
                session, true, null, processId, actors);
    }

    /**
     * Converts a {@link DocumentModelList} to a list of {@link Task}s.
     *
     * @since 5.9.6
     * @param taskDocuments
     */
    public static List<Task> wrapDocModelInTask(
            List<DocumentModel> taskDocuments) {
        List<Task> tasks = new ArrayList<Task>();
        for (DocumentModel doc : taskDocuments) {
            tasks.add(doc.getAdapter(Task.class));
        }
        return tasks;
    }

    /**
     * @deprecated since 5.9.6, use {@link #wrapDocModelInTask(List)} instead.
     */
    @Deprecated
    public static List<Task> wrapDocModelInTask(DocumentModelList taskDocuments)
            throws ClientException {
        return wrapDocModelInTask(taskDocuments, false);
    }

    /**
     * Converts a {@link DocumentModelList} to a list of {@link Task}s.
     *
     * @param detach if {@code true}, detach each document before converting it
     *            to a {@code Task}.
     * @deprecated since 5.9.6, use {@link #wrapDocModelInTask(List)} instead.
     */
    @Deprecated
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
        // make sure taskDoc is attached to prevent sending event with null session
        DocumentModel taskDocument = task.getDocument();
        if (taskDocument.getSessionId() == null) {
            taskDocument.attach(coreSession.getSessionId());
        }
        coreSession.saveDocument(taskDocument);
        // notify
        Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
        ArrayList<String> notificationRecipients = new ArrayList<String>();
        notificationRecipients.add(task.getInitiator());
        notificationRecipients.addAll(task.getActors());
        eventProperties.put(NotificationConstants.RECIPIENTS_KEY,
                notificationRecipients);
        // try to resolve document when notifying
        DocumentModel document = null;

        List<String> docIds = new ArrayList<String>();
        docIds.addAll(task.getTargetDocumentsIds());
        // handle compatibility with tasks created before 5.8
        String docId = task.getTargetDocumentId();
        if (!docIds.contains(docId)) {
            docIds.add(docId);
        }
        // also handle compatibility with deprecated jbpm tasks
        String docIdVar = task.getVariable(TaskService.VariableName.documentId.name());
        if (!docIds.contains(docIdVar)) {
            docIds.add(docIdVar);
        }
        String docRepo = task.getVariable(TaskService.VariableName.documentRepositoryName.name());
        List<DocumentModel> documents = new ArrayList<DocumentModel>();
        if (coreSession.getRepositoryName().equals(docRepo)) {
            try {
                for (String id : docIds) {
                    document = coreSession.getDocument(new IdRef(id));
                    documents.add(document);
                }

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
        boolean taskEndedByDelegatedActor = task.getDelegatedActors() != null
                && task.getDelegatedActors().contains(principal.getName());
        for (DocumentModel doc : documents) {
            TaskEventNotificationHelper.notifyEvent(coreSession, doc,
                    principal, task, eventName, eventProperties, comment, null);
            if (taskEndedByDelegatedActor) {
                TaskEventNotificationHelper.notifyEvent(
                        coreSession,
                        doc,
                        principal,
                        task,
                        eventName,
                        eventProperties,
                        String.format("Task ended by an delegated actor '%s' ",
                                principal.getName())
                                + (!StringUtils.isEmpty(comment) ? " with the following comment: "
                                        + comment
                                        : ""), null);
            }
        }
        String seamEventName = isValidated ? TaskEventNames.WORKFLOW_TASK_COMPLETED
                : TaskEventNames.WORKFLOW_TASK_REJECTED;
        return seamEventName;
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, String nodeId,
            CoreSession session) throws ClientException {
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_PROCESS_AND_NODE_PP,
                session, true, null, processId, nodeId);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors,
            boolean includeDelegatedTasks, CoreSession session)
            throws ClientException {
        if (includeDelegatedTasks) {
            return getTasks(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_OR_DELEGATED_ACTORS_PP,
                    session, true, null, dm.getId(), dm.getId(), actors, actors);
        } else {
            return getTasks(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP,
                    session, true, null, dm.getId(), dm.getId(), actors);
        }
    }

    /**
     * @since 5.9.6
     */
    @SuppressWarnings("unchecked")
    public static List<Task> getTasks(String pageProviderName,
            CoreSession session, boolean unrestricted,
            List<SortInfo> sortInfos, Object... params) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        if (ppService == null) {
            throw new RuntimeException("Missing PageProvider service");
        }
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        // first retrieve potential props from definition
        PageProviderDefinition def = ppService.getPageProviderDefinition(pageProviderName);
        if (def != null) {
            Map<String, String> defProps = def.getProperties();
            if (defProps != null) {
                props.putAll(defProps);
            }
        }
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        if (unrestricted) {
            props.put(
                    CoreQueryDocumentPageProvider.USE_UNRESTRICTED_SESSION_PROPERTY,
                    Boolean.TRUE);
        }
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                pageProviderName, sortInfos, null, null, props, params);
        if (pp == null) {
            throw new ClientException("Page provider not found: "
                    + pageProviderName);
        }
        return wrapDocModelInTask(pp.getCurrentPage());
    }

}