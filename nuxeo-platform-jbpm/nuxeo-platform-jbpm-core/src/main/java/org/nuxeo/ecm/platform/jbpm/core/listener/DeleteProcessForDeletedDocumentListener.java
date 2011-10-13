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
package org.nuxeo.ecm.platform.jbpm.core.listener;

import java.util.List;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that deletes process instance and related tasks when the process is
 * attached to a document that is being deleted. It also deletes related tasks
 * of the document out of process.
 * 
 * @author arussel
 */
public class DeleteProcessForDeletedDocumentListener implements EventListener {
    private JbpmService jbpmService;

    public JbpmService getJbpmService() {
        if (jbpmService == null) {
            try {
                jbpmService = Framework.getService(JbpmService.class);
            } catch (Exception e) {
                throw new ClientRuntimeException("JbpmService is not deployed",
                        e);
            }
        }
        return jbpmService;
    }

    public void handleEvent(Event event) throws ClientException {
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel dm = context.getSourceDocument();
            NuxeoPrincipal principal = (NuxeoPrincipal) context.getPrincipal();
            List<ProcessInstance> processes = getJbpmService().getProcessInstances(
                    dm, principal, null);
            if(!processes.isEmpty()){
                for (ProcessInstance process : processes) {
                    getJbpmService().deleteProcessInstance(principal,
                            Long.valueOf(process.getId()));
                }
            }
            List<TaskInstance> tasks = getJbpmService().getTaskInstances(dm,
                    principal, null);
            if (!tasks.isEmpty()) {
                for (TaskInstance task : tasks) {
                    getJbpmService().deleteTaskInstance(principal,
                            Long.valueOf(task.getId()));
                }
            }
        }
    }

}
