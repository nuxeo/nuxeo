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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.RoutingTaskService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @deprecated since 5.6, use DocumentRoutingService instead
 */
@Deprecated
public class RoutingTaskServiceImpl extends DefaultComponent implements RoutingTaskService {

    @Override
    public void makeRoutingTasks(CoreSession session, List<Task> tasks) {
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        routing.makeRoutingTasks(session, tasks);
    }

    @Override
    public void endTask(CoreSession session, Task task, Map<String, Object> data, String status)
            throws DocumentRouteException {
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        routing.endTask(session, task, data, status);
    }

    @Override
    public List<DocumentModel> getWorkflowInputDocuments(CoreSession session, Task task) throws DocumentRouteException {
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        return routing.getWorkflowInputDocuments(session, task);
    }

}
