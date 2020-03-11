/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.task.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SortInfo;
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

    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession) {

        // Get tasks for current user
        // We need to build the task actors list: prefixed and unprefixed names
        // of the principal and all its groups
        NuxeoPrincipal principal = coreSession.getPrincipal();
        List<String> actors = TaskActorsHelper.getTaskActors(principal);

        return getCurrentTaskInstances(actors, coreSession);
    }

    /**
     * Provide @param sortInfo to handle sort page-provider contributions (see {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession, List<SortInfo> sortInfos) {

        // Get tasks for current user
        // We need to build the task actors list: prefixed and unprefixed names
        // of the principal and all its groups
        NuxeoPrincipal principal = coreSession.getPrincipal();
        List<String> actors = TaskActorsHelper.getTaskActors(principal);

        return getCurrentTaskInstances(actors, coreSession, sortInfos);
    }

    /**
     * Returns a list of task instances assigned to one of the actors in the list or to its pool.
     *
     * @param actors a list used as actorId to retrieve the tasks.
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors, CoreSession coreSession) {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<>();
        }
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_ACTORS_PP, coreSession, true, null, actors);
    }

    /**
     * Provide @param sortInfo to handle sort page-provider contributions (see {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors, CoreSession coreSession, List<SortInfo> sortInfos) {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<>();
        }
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_ACTORS_PP, coreSession, true, sortInfos, actors);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user, CoreSession coreSession) {
        if (user == null) {
            return getTasks(TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_PP, coreSession, true, null, dm.getId(),
                    dm.getId());
        } else {
            List<String> actors = TaskActorsHelper.getTaskActors(user);
            return getTasks(TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP, coreSession, true, null,
                    dm.getId(), dm.getId(), actors);
        }
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors, CoreSession coreSession) {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<>();
        }
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP, coreSession, true, null,
                dm.getId(), dm.getId(), actors);
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, CoreSession session) {
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_PROCESS_PP, session, true, null, processId);
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, NuxeoPrincipal user, CoreSession session) {
        List<String> actors = TaskActorsHelper.getTaskActors(user);
        return getAllTaskInstances(processId, actors, session);
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, List<String> actors, CoreSession session) {
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_PROCESS_AND_ACTORS_PP, session, true, null, processId, actors);
    }

    /**
     * Converts a {@link DocumentModelList} to a list of {@link Task}s.
     *
     * @since 6.0
     */
    public static List<Task> wrapDocModelInTask(List<DocumentModel> taskDocuments) {
        List<Task> tasks = new ArrayList<>();
        for (DocumentModel doc : taskDocuments) {
            tasks.add(doc.getAdapter(Task.class));
        }
        return tasks;
    }

    /**
     * @deprecated since 6.0, use {@link #wrapDocModelInTask(List)} instead.
     */
    @Deprecated
    public static List<Task> wrapDocModelInTask(DocumentModelList taskDocuments) {
        return wrapDocModelInTask(taskDocuments, false);
    }

    /**
     * Converts a {@link DocumentModelList} to a list of {@link Task}s.
     *
     * @param detach if {@code true}, detach each document before converting it to a {@code Task}.
     * @deprecated since 6.0, use {@link #wrapDocModelInTask(List)} instead.
     */
    @Deprecated
    public static List<Task> wrapDocModelInTask(DocumentModelList taskDocuments, boolean detach) {
        List<Task> tasks = new ArrayList<>();
        for (DocumentModel doc : taskDocuments) {
            if (detach) {
                doc.detach(true);
            }
            tasks.add(doc.getAdapter(Task.class));
        }
        return tasks;
    }

    @Override
    public String endTask(CoreSession coreSession, NuxeoPrincipal principal, Task task, String comment,
            String eventName, boolean isValidated) {

        // put user comment on the task
        if (!StringUtils.isEmpty(comment)) {
            task.addComment(principal.getName(), comment);
        }

        // end the task, adding boolean marker that task was validated or
        // rejected
        task.setVariable(TaskService.VariableName.validated.name(), String.valueOf(isValidated));
        task.end(coreSession);
        // make sure taskDoc is attached to prevent sending event with null session
        DocumentModel taskDocument = task.getDocument();
        if (taskDocument.getSessionId() == null) {
            taskDocument.attach(coreSession.getSessionId());
        }
        coreSession.saveDocument(taskDocument);
        if (StringUtils.isNotBlank(eventName)) {
            TaskEventNotificationHelper.notifyTaskEnded(coreSession, principal, task, comment, eventName, null);
        }
        return isValidated ? TaskEventNames.WORKFLOW_TASK_COMPLETED : TaskEventNames.WORKFLOW_TASK_REJECTED;
    }

    @Override
    public List<Task> getAllTaskInstances(String processId, String nodeId, CoreSession session) {
        return getTasks(TaskQueryConstant.GET_TASKS_FOR_PROCESS_AND_NODE_PP, session, true, null, processId, nodeId);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors, boolean includeDelegatedTasks,
            CoreSession session) {
        if (includeDelegatedTasks) {
            return getTasks(TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_OR_DELEGATED_ACTORS_PP, session,
                    true, null, dm.getId(), dm.getId(), actors, actors);
        } else {
            return getTasks(TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENTS_AND_ACTORS_PP, session, true, null,
                    dm.getId(), dm.getId(), actors);
        }
    }

    @Override
    public List<Task> getAllCurrentTaskInstances(CoreSession session, List<SortInfo> sortInfos) {
        // Get tasks for current user
        // We need to build the task actors list: prefixed and unprefixed names
        // of the principal and all its groups
        NuxeoPrincipal principal = session.getPrincipal();
        List<String> actors = TaskActorsHelper.getTaskActors(principal);

        return getTasks(TaskQueryConstant.GET_TASKS_FOR_ACTORS_OR_DELEGATED_ACTORS_PP, session, true, sortInfos, actors,
                actors);
    }

    /**
     * @since 6.0
     */
    @SuppressWarnings("unchecked")
    public static List<Task> getTasks(String pageProviderName, CoreSession session, boolean unrestricted,
            List<SortInfo> sortInfos, Object... params) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        if (ppService == null) {
            throw new RuntimeException("Missing PageProvider service");
        }
        Map<String, Serializable> props = new HashMap<>();
        // first retrieve potential props from definition
        PageProviderDefinition def = ppService.getPageProviderDefinition(pageProviderName);
        if (def != null) {
            Map<String, String> defProps = def.getProperties();
            if (defProps != null) {
                props.putAll(defProps);
            }
        }
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        if (unrestricted) {
            props.put(CoreQueryDocumentPageProvider.USE_UNRESTRICTED_SESSION_PROPERTY, Boolean.TRUE);
        }
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(pageProviderName,
                sortInfos, null, null, props, params);
        if (pp == null) {
            throw new NuxeoException("Page provider not found: " + pageProviderName);
        }
        return wrapDocModelInTask(pp.getCurrentPage());
    }

}
