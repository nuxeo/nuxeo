/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.automation.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;

/**
 * Creates a task
 *
 * @author Anahide Tchertchian
 * @since 5.5
 */
@Operation(id = CreateTask.ID, category = Constants.CAT_SERVICES, label = "Create task", since = "5.3.2", description = "Enable to create a task bound to the document. "
        + "<p><b>Directive</b>, <b>comment</b> and <b>due date</b> will be displayed in the task list of the user. "
        + "In <b>accept operation chain</b> and <b>reject operation chain</b> fields, "
        + "you can put the operation chain ID of your choice among the one you contributed. "
        + "Those operations will be executed when the user validates the task, "
        + "depending on  whether he accepts or rejects the task. "
        + "You have to specify a variable name (the <b>key for ... </b> parameter) to resolve target users and groups to which the task will be assigned. "
        + "You can use Get Users and Groups to update a context variable with some users and groups. "
        + "If you check <b>create one task per actor</b>, each of the actors will have a task to achieve, "
        + "versus \"the first who achieve the task makes it disappear for the others\".</p>", aliases = {
                "Workflow.CreateTask" })
public class CreateTask {

    public static final String ID = "Task.Create";

    private static final Log log = LogFactory.getLog(CreateTask.class);

    public enum OperationTaskVariableName {
        acceptOperationChain, rejectOperationChain, createdFromCreateTaskOperation, taskDocument
    }

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession coreSession;

    @Context
    protected TaskService TaskService;

    @Param(name = "task name", required = true, order = 0)
    protected String taskName;

    @Param(name = "due date", required = false, order = 1)
    protected Date dueDate;

    @Param(name = "directive", required = false, order = 2)
    protected String directive;

    @Param(name = "comment", required = false, order = 3)
    protected String comment;

    @Param(name = "accept operation chain", required = false, order = 4)
    protected String acceptOperationChain;

    @Param(name = "reject operation chain", required = false, order = 5)
    protected String rejectOperationChain;

    @Param(name = "variable name for actors prefixed ids", required = false, order = 6)
    protected String keyForActors;

    @Param(name = "additional list of actors prefixed ids", required = false, order = 7)
    protected StringList additionalPrefixedActors;

    @Param(name = "create one task per actor", required = false, values = "true", order = 8)
    protected boolean createOneTaskPerActor = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    @SuppressWarnings("unchecked")
    public DocumentModel run(DocumentModel document) throws OperationException {
        NuxeoPrincipal pal = coreSession.getPrincipal();

        List<String> prefixedActorIds = new ArrayList<>();
        Object actors = ctx.get(keyForActors);
        if (actors != null) {
            boolean throwError = false;
            try {
                if (actors instanceof List) {
                    prefixedActorIds.addAll((List<String>) actors);
                } else if (actors instanceof String[]) {
                    for (String actor : (String[]) actors) {
                        prefixedActorIds.add(actor);
                    }
                } else if (actors instanceof String) {
                    prefixedActorIds.add((String) actors);
                } else {
                    throwError = true;
                }
            } catch (ClassCastException e) {
                throwError = true;
            }
            if (throwError) {
                throw new OperationException(String.format("Invalid key to retrieve a list, array or single "
                        + "string of prefixed actor " + "ids '%s', value is not correct: %s", keyForActors, actors));
            }
        }

        if (additionalPrefixedActors != null) {
            prefixedActorIds.addAll(additionalPrefixedActors);
        }

        if (prefixedActorIds.isEmpty()) {
            // no actors: do nothing
            log.debug("No actors could be resolved => do not create any task");
            return document;
        }

        // create the task, passing operation chains in task variables
        Map<String, String> taskVariables = new HashMap<>();
        taskVariables.put(OperationTaskVariableName.createdFromCreateTaskOperation.name(), "true");
        if (!StringUtils.isEmpty(acceptOperationChain)) {
            taskVariables.put(OperationTaskVariableName.acceptOperationChain.name(), acceptOperationChain);
        }
        if (!StringUtils.isEmpty(rejectOperationChain)) {
            taskVariables.put(OperationTaskVariableName.rejectOperationChain.name(), rejectOperationChain);
        }

        // disable notification service
        taskVariables.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE, "true");

        if (TaskService == null) {
            throw new OperationException("Service jbpmTaskService not found");
        }
        TaskService.createTask(coreSession, pal, document, taskName, prefixedActorIds, createOneTaskPerActor, directive,
                comment, dueDate, taskVariables, null);

        coreSession.save();

        return document;
    }

}
