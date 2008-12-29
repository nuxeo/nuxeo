/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core;

import java.util.Set;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * 
 */
public interface TaskManagementService {
    TaskInstance createTaskInstance(String taskName, String actorId);

    TaskInstance createTaskInstance(String taskName, Set<String> pooledActors);

    TaskInstance getTaskInstance(long id);

    void updateTaskInstance(TaskInstance task);

    /**
     * Remove the task from the database.
     * 
     * @param id The task id.
     */
    void deleteTaskInstance(long id);

    Set<TaskInstance> getUnfinishedTaskAssignedTo(String actorsId);

    Set<TaskInstance> getUnfinishedTaskAssignedTo(Set<String> actorsIds);

    Set<TaskInstance> getUnassignedTaskPooledTo(Set<String> groupIds);
}
