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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.task.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that will launch another operation when a task is accepted or rejected
 *
 * @author Anahide Tchertchian
 * @since 5.5
 */
public class TaskEndedEventListener implements EventListener {

    private static final Log log = LogFactory.getLog(TaskEndedEventListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext eventContext = event.getContext();
        Task.optionalTask(eventContext).ifPresent(task -> handleTask(eventContext, task));
    }

    private void handleTask(EventContext eventContext, Task task) {
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
                OperationContext ctx = new OperationContext(eventContext.getCoreSession());
                if (eventContext instanceof DocumentEventContext) {
                    ctx.setInput(((DocumentEventContext) eventContext).getSourceDocument());
                    ctx.put(OperationTaskVariableName.taskDocument.name(), task.getDocument());
                }
                try {
                    os.run(ctx, chain);
                } catch (InvalidChainException e) {
                    log.error("Unknown chain: " + chain);
                }
            } catch (OperationException t) {
                log.error(t, t);
            }
        }
    }
}
