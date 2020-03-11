/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.task.core.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that deletes deletes related tasks of the document.
 *
 * @author arussel
 * @since 5.5
 */
public class DeleteTaskForDeletedDocumentListener implements EventListener {

    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link TaskService} instead.
     */
    @Deprecated
    public TaskService getTaskService() {
        return Framework.getService(TaskService.class);
    }

    @Override
    public void handleEvent(Event event) {
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
            TaskService taskService = Framework.getService(TaskService.class);
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel dm = context.getSourceDocument();
            CoreSession coreSession = context.getCoreSession();
            List<Task> tasks = taskService.getTaskInstances(dm, (NuxeoPrincipal) null, coreSession);
            if (!tasks.isEmpty()) {
                for (Task task : tasks) {
                    taskService.deleteTask(coreSession, task.getId());
                }
            }
        }
    }

}
