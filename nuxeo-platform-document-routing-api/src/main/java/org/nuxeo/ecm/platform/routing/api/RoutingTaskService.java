/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.task.Task;

/**
 * A small service adding Routing behavior to tasks.
 */
public interface RoutingTaskService {

    /**
     * Marks the tasks as Routing tasks.
     * <p>
     * This allows the related documents to be adapted to {@link RoutingTask}.
     *
     * @param session the session
     * @param tasks the tasks
     */
    void makeRoutingTasks(CoreSession session, List<Task> tasks)
            throws ClientException;

    /**
     * Ends a task
     *
     * @param session
     * @param task
     * @param data
     * @throws DocumentRouteException
     */
    void endTask(CoreSession session, Task task, Map<String, Object> data)
            throws DocumentRouteException;
}
