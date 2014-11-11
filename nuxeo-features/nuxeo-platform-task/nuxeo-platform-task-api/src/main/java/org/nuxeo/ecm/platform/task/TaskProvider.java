/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin, Antoine Taillefer
 */
package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Laurent Doguin
 * @since 5.5
 */
public interface TaskProvider extends Serializable {

    /**
     *
     * @param coreSession
     * @return A list of task instances visible by the coreSession's user.
     * @throws IllegalStateException If the currentUser is null.
     */
    List<Task> getCurrentTaskInstances(CoreSession coreSession)
            throws ClientException;

    /**
     * Returns a list of task instances assigned to one of the actors in the
     * list or to its pool.
     *
     * @param actors a list used as actorId to retrieve the tasks.
     * @param coreSession
     * @return
     * @throws ClientException
     */
    List<Task> getCurrentTaskInstances(List<String> actors,
            CoreSession coreSession) throws ClientException;

    /**
     * Returns the list of task instances associated with this document for
     * which the user is the actor or belongs to the pooled actor list.
     * <p>
     * If the user is null, then it returns all task instances for the document.
     *
     * @param dm the document.
     * @param user
     * @param coreSession
     * @return
     * @throws ClientException
     */
    List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user,
            CoreSession coreSssion) throws ClientException;

    /**
     * Returns the list of task instances associated with this document assigned
     * to one of the actor in the list or its pool.
     *
     * @param dm
     * @param actors
     * @param coreSession
     * @return
     * @throws ClientException
     */
    List<Task> getTaskInstances(DocumentModel dm, List<String> actors,
            CoreSession coreSession) throws ClientException;

    /**
     * Returns all the tasks instances for the given {@code processId}.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the
     * tasks are detached.
     *
     * @since 5.6
     */
    List<Task> getAllTaskInstances(String processId, CoreSession session)
            throws ClientException;

    /**
     * Returns all the tasks instances for the given {@code processId} and where
     * the user is the actor or belongs to the pooled actor list.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the
     * tasks are detached.
     *
     * @since 5.6
     */
    List<Task> getAllTaskInstances(String processId, NuxeoPrincipal user,
            CoreSession session) throws ClientException;

    /**
     * Returns all the tasks instances for the given {@code processId} which
     * assigned to one of the actor in the list or its pool.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the
     * tasks are detached.
     *
     * @since 5.6
     */
    List<Task> getAllTaskInstances(String processId, List<String> actors,
            CoreSession session) throws ClientException;

    /**
     * Ends the task
     *
     * @since 5.6
     *
     * @param coreSession the session to use when notifying and resolving of
     *            referenced document for notification.
     * @param principal principal used when notifying
     * @param task the instance to end
     * @param comment string added to the task comments and used as a
     *            notification comment
     * @param eventName the core event name to use when notifying
     * @param isValidated boolean marker to state if the task was validated or
     *            rejected
     * @throws ClientException when trying to end a task without being granted
     *             the right to do so (see
     *             {@link #canEndTask(NuxeoPrincipal, Task)}), or when any other
     *             error occurs
     * @return the name of the Seam event to raise
     */
    String endTask(CoreSession coreSession, NuxeoPrincipal principal,
            Task task, String comment, String eventName, boolean isValidated)
            throws ClientException;

    /**
     * Returns all the tasks instances for the given {@code processId}
     * originating from the given {@code nodeId}.
     * <p>
     * The query is done in unrestricted mode and so the documents linked to the
     * tasks are detached.
     *
     * @since 5.7
     */
    List<Task> getAllTaskInstances(String processId, String nodeId,
            CoreSession session) throws ClientException;
}
