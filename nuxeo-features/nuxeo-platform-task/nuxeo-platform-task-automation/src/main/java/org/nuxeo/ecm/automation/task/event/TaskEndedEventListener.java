/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
            try (OperationContext ctx = new OperationContext(eventContext.getCoreSession())) {
                // run the given operation
                AutomationService os = Framework.getService(AutomationService.class);

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
