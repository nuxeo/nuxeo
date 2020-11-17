/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.api;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.task.Task;

/**
 * A small service adding Routing behavior to tasks.
 * <p>
 *
 * @deprecated since 5.6, use DocumentRoutingService instead
 */
@Deprecated
public interface RoutingTaskService {

    /**
     * Marks the tasks as Routing tasks.
     * <p>
     * This allows the related documents to be adapted to {@code RoutingTask}.
     *
     * @param session the session
     * @param tasks the tasks
     * @deprecated since 5.6, use DocumentRoutingService instead
     */
    @Deprecated
    void makeRoutingTasks(CoreSession session, List<Task> tasks);

    /**
     * Ends a task
     *
     * @param status name of the button clicked to submit the task form
     * @deprecated since 5.6, use DocumentRoutingService instead
     */
    @Deprecated
    void endTask(CoreSession session, Task task, Map<String, Object> data, String status) throws DocumentRouteException;

    /**
     * Gets the documents following the workflow to which the given task belongs
     *
     * @deprecated since 5.6, use DocumentRoutingService instead
     */
    @Deprecated
    List<DocumentModel> getWorkflowInputDocuments(CoreSession session, Task task) throws DocumentRouteException;
}
