/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin, Antoine Taillefer
 */
package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * @author Laurent Doguin
 * @since 5.5
 */
public interface TaskProvider extends Serializable {

    /**
     * @return A list of task instances where the current user is an actor. Doesn't take into account tasks that were
     *         delegated to this user.
     * @throws IllegalStateException If the currentUser is null.
     */
    List<Task> getCurrentTaskInstances(CoreSession coreSession);

    /**
     * Provide @param sortInfo to handle sort page-provider contributions (see {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    List<Task> getCurrentTaskInstances(CoreSession coreSession, List<SortInfo> sortInfos);

    /**
     * Returns a list of task instances assigned to one of the actors in the list or to its pool. Doesn't take into
     * account tasks that were delegated to these users. The query is done in unrestricted mode and so the documents
     * linked to the tasks are detached.
     *
     * @since 5.5
     * @param actors a list used as actorId to retrieve the tasks.
     */
    List<Task> getCurrentTaskInstances(List<String> actors, CoreSession coreSession);

    /**
     * Provide @param sortInfo to handle sort page-provider contributions (see {@link #getCurrentTaskInstances})
     *
     * @since 5.9.3
     */
    List<Task> getCurrentTaskInstances(List<String> actors, CoreSession coreSession, List<SortInfo> sortInfos);

    /**
     * Returns the list of task instances associated with this document for which the user is the actor or belongs to
     * the pooled actor list. Doesn't take into account tasks that were delegated to this user.
     * <p>
     * If the user is null, then it returns all task instances for the document. The query is done in unrestricted mode
     * and so the documents linked to the tasks are detached.
     *
     * @param dm the document.
     */
    List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user, CoreSession coreSession);

    /**
     * Returns the list of task instances associated with this document assigned to one of the actor in the list or its
     * pool. Doesn't take into account tasks that were delegated to these users. The query is done in unrestricted mode
     * and so the documents linked to the tasks are detached.
     */
    List<Task> getTaskInstances(DocumentModel dm, List<String> actors, CoreSession coreSession);

    /**
     * Returns the list of task instances associated with this document assigned to one of the actor in the list or its
     * pool. If the parameter {@code includeDelegatedTasks} is true, takes into account tasks that were delegated to
     * these users. The query is done in unrestricted mode and so the documents linked to the tasks are detached.
     *
     * @since 5.8
     */
    List<Task> getTaskInstances(DocumentModel dm, List<String> actors, boolean includeDelegatedTasks,
            CoreSession session);

    /**
     * Returns the list of task instances associated assigned to the current user.
     * Takes into account tasks that were delegated to this user.
     * The query is done in unrestricted mode and so the documents linked to the tasks are detached.
     *
     * @since 7.4
     */
    List<Task> getAllCurrentTaskInstances(CoreSession session, List<SortInfo> sortInfos);


    /**
     * Returns all the tasks instances for the given {@code processId}.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the tasks are detached.
     *
     * @since 5.6
     */
    List<Task> getAllTaskInstances(String processId, CoreSession session);

    /**
     * Returns all the tasks instances for the given {@code processId} and where the user is the actor or belongs to the
     * pooled actor list. Doesn't take into account tasks that were delegated to this user.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the tasks are detached.
     *
     * @since 5.6
     */
    List<Task> getAllTaskInstances(String processId, NuxeoPrincipal user, CoreSession session);

    /**
     * Returns all the tasks instances for the given {@code processId} which assigned to one of the actor in the list or
     * its pool. Doesn't take into account tasks that were delegated to these users.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the tasks are detached.
     *
     * @since 5.6
     */
    List<Task> getAllTaskInstances(String processId, List<String> actors, CoreSession session);

    /**
     * Ends the task
     *
     * @since 5.6
     * @param coreSession the session to use when notifying and resolving of referenced document for notification.
     * @param principal principal used when notifying
     * @param task the instance to end
     * @param comment string added to the task comments and used as a notification comment
     * @param eventName the core event name to use when notifying
     * @param isValidated boolean marker to state if the task was validated or rejected
     * @throws NuxeoException when trying to end a task without being granted the right to do so, or when any other
     *             error occurs
     * @return the name of the Seam event to raise
     */
    String endTask(CoreSession coreSession, NuxeoPrincipal principal, Task task, String comment, String eventName,
            boolean isValidated);

    /**
     * Returns all the tasks instances for the given {@code processId} originating from the given {@code nodeId}.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the tasks are detached.
     *
     * @since 5.7
     */
    List<Task> getAllTaskInstances(String processId, String nodeId, CoreSession session);

}
