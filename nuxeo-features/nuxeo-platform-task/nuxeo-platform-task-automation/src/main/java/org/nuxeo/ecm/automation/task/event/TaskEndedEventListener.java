/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.task.event;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.task.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that will launch another operation when a task is accepted or
 * rejected
 *
 * @author Anahide Tchertchian
 * @since 5.5
 */
public class TaskEndedEventListener implements EventListener {

    private static final Log log = LogFactory.getLog(TaskEndedEventListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext eventContext = event.getContext();
        Serializable property = eventContext.getProperty(TaskService.TASK_INSTANCE_EVENT_PROPERTIES_KEY);
        if (property == null || !(property instanceof Task)) {
            // do nothing
            return;
        }
        Task task = (Task) property;

        Boolean validated = Boolean.valueOf(task.getVariable(TaskService.VariableName.validated.name()));

        String chain;
        if (validated) {
            chain = task.getVariable(OperationTaskVariableName.acceptOperationChain.name());
        } else {
            chain = task.getVariable(OperationTaskVariableName.rejectOperationChain.name());
        }

        if (!StringUtils.isEmpty(chain)) {
            try {
                // run the given operation
                AutomationService os = Framework.getService(AutomationService.class);
                OperationContext ctx = new OperationContext(
                        eventContext.getCoreSession());
                if (eventContext instanceof DocumentEventContext) {
                    ctx.setInput(((DocumentEventContext) eventContext).getSourceDocument());
                    ctx.put(OperationTaskVariableName.taskDocument.name(), task.getDocument());
                }
                try {
                    os.run(ctx, chain);
                } catch (InvalidChainException e) {
                    log.error("Unknown chain: " + chain);
                }
            } catch (Throwable t) {
                log.error(t, t);
            }
        }
    }
}
