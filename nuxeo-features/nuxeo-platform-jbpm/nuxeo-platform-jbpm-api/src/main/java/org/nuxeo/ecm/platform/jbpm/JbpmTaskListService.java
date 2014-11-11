/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nicolas Ulrich
 *
 */

package org.nuxeo.ecm.platform.jbpm;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * The Task List service maintains the lists of tasks that are used in
 * Workflow.
 *
 * The lists are saved in the Personal Workspace.
 *
 * @author nulrich
 *
 */
public interface JbpmTaskListService {

    /**
     * Creates a new task list.
     *
     * @param listName the name of the new list
     * @return the created list
     * @throws ClientException
     */
    TaskList createTaskList(CoreSession session, String listName)
            throws ClientException;

    /**
     * Saves the list.
     *
     * @param list to save
     * @throws ClientException
     */
    void saveTaskList(CoreSession session, TaskList list)
            throws ClientException;

    /**
     * Retrieves a task list from it name.
     *
     * @param Name of the list to load
     * @return
     * @throws ClientException
     */
    TaskList getTaskList(CoreSession session, String listUUId)
            throws ClientException;

    /**
     * Delete a list of participant.
     *
     * @param session
     * @param listName The name of the list to delete
     * @throws ClientException
     */
    void deleteTaskList(CoreSession session, String listUUId)
            throws ClientException;

    /**
     * Retrieve all the task lists of the current user
     *
     * @param session
     * @return Task lists of the current user
     * @throws ClientException
     */
    List<TaskList> getTaskLists(CoreSession documentManager) throws ClientException;

}
