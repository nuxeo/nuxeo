/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.task.core.listener;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
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

    private TaskService taskService;

    public TaskService getTaskService() {
        if (taskService == null) {
            try {
                taskService = Framework.getService(TaskService.class);
            } catch (Exception e) {
                throw new ClientRuntimeException("JbpmService is not deployed",
                        e);
            }
        }
        return taskService;
    }

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel dm = context.getSourceDocument();
            CoreSession coreSession = context.getCoreSession();
            List<Task> tasks = getTaskService().getTaskInstances(dm,
                    (NuxeoPrincipal) null, coreSession);
            if (!tasks.isEmpty()) {
                for (Task task : tasks) {
                    getTaskService().deleteTask(coreSession,
                            task.getId());
                }
            }
        }
    }

}
