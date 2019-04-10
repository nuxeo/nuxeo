/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.routing.dm.operation;

import java.util.ArrayList;
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
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.dm.adapter.TaskStep;
import org.nuxeo.ecm.platform.routing.dm.api.RoutingTaskConstants;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Creates a routing task
 *
 * @author ldoguin
 * @since 5.6
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
@Operation(id = CreateRoutingTask.ID, category = Constants.CAT_SERVICES, label = "Create task", since = "5.6", description = "Enable to create a routingTask bound to a route and its document. "
        + "In <b>accept operation chain</b> and <b>reject operation chain</b> fields, "
        + "you can put the operation chain ID of your choice among the one you contributed. "
        + "Those operations will be executed when the user validates the task, "
        + "depending on  whether he accepts or rejects the task. "
        + "Extra (String) properties can be set on the taskVariables from the input document or from the step.", addToStudio = false)
public class CreateRoutingTask {

    public static final String ID = "Workflow.CreateRoutingTask";

    private static final Log log = LogFactory.getLog(CreateRoutingTask.class);

    public enum OperationTaskVariableName {
        acceptOperationChain, rejectOperationChain, createdFromCreateTaskOperation, taskDocuments
    }

    public static final String STEP_PREFIX = "StepTask:";

    public static final String DOCUMENT_PREFIX = "Document:";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession coreSession;

    @Context
    UserManager userManager;

    @Context
    protected TaskService taskService;

    @Context
    protected DocumentRoutingService routing;

    @Param(name = "accept operation chain", required = false, order = 4)
    protected String acceptOperationChain;

    @Param(name = "reject operation chain", required = false, order = 5)
    protected String rejectOperationChain;

    @Param(name = "mappingTaskVariables", required = false)
    protected Properties mappingTaskVariables;

    @Param(name = "mappingProperties", required = false)
    protected Properties mappingProperties;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel createTask(DocumentModel document) throws OperationException {
        NuxeoPrincipal pal = coreSession.getPrincipal();

        DocumentRouteStep step = (DocumentRouteStep) ctx.get(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY);
        DocumentModel stepDocument = step.getDocument();
        TaskStep taskStep = stepDocument.getAdapter(TaskStep.class);
        List<String> actors = taskStep.getActors();

        if (actors.isEmpty()) {
            // no actors: do nothing
            log.debug("No actors could be resolved => do not create any task");
            return document;
        }

        // create the task, passing operation chains in task variables
        Map<String, String> taskVariables = new HashMap<String, String>();
        taskVariables.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, step.getDocument().getId());
        taskVariables.put(OperationTaskVariableName.createdFromCreateTaskOperation.name(), "true");
        if (!StringUtils.isEmpty(acceptOperationChain)) {
            taskVariables.put(OperationTaskVariableName.acceptOperationChain.name(), acceptOperationChain);
        }
        if (!StringUtils.isEmpty(rejectOperationChain)) {
            taskVariables.put(OperationTaskVariableName.rejectOperationChain.name(), rejectOperationChain);
        }

        // disable notification service
        taskVariables.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE, "true");

        if (routing == null) {
            throw new OperationException("Service routingTaskService not found");
        }
        if (mappingTaskVariables != null) {
            mapPropertiesToTaskVariables(taskVariables, stepDocument, document, mappingTaskVariables);
        }
        // TODO: call method with number of comments after NXP-8068 is merged
        List<Task> tasks = taskService.createTask(coreSession, pal, document, taskStep.getName(),
                actors, false, taskStep.getDirective(), null, taskStep.getDueDate(), taskVariables, null);
        routing.makeRoutingTasks(coreSession, tasks);
        DocumentModelList docList = new DocumentModelListImpl(tasks.size());
        for (Task task : tasks) {
            docList.add(((mappingProperties == null) ? (task.getDocument()) : mapPropertiesToTaskDocument(coreSession,
                    stepDocument, task.getDocument(), document, mappingProperties)));
        }

        // all the actors should be able to validate the step creating the task
        for (String actor : actors) {
            step.setCanReadStep(coreSession, actor);
            step.setCanValidateStep(coreSession, actor);
            step.setCanUpdateStep(coreSession, actor);
        }
        ctx.put(OperationTaskVariableName.taskDocuments.name(), docList);

        ctx.put(RoutingTaskConstants.ROUTING_TASK_ACTORS_KEY, new StringList(getAllActors(actors)));
        return document;
    }

    protected void mapPropertiesToTaskVariables(Map<String, String> taskVariables, DocumentModel stepDoc,
            DocumentModel inputDoc, Properties mappingProperties) {
        for (Map.Entry<String, String> prop : mappingProperties.entrySet()) {
            String getter = prop.getKey();
            String setter = prop.getValue();
            DocumentModel setterDoc;
            if (setter.startsWith(DOCUMENT_PREFIX)) {
                setterDoc = inputDoc;
                setter = setter.substring(DOCUMENT_PREFIX.length());
            } else if (setter.startsWith(STEP_PREFIX)) {
                setterDoc = stepDoc;
                setter = setter.substring(STEP_PREFIX.length());
            } else {
                throw new NuxeoException("Unknown setter prefix: " + setter);
            }
            try {
                taskVariables.put(getter, (String) setterDoc.getPropertyValue(setter));
            } catch (PropertyException e) {
                log.error("Could not map property on the task document in the taskVariables ", e);
            }
        }
    }

    DocumentModel mapPropertiesToTaskDocument(CoreSession session, DocumentModel stepDoc, DocumentModel taskDoc,
            DocumentModel inputDoc, Properties mappingProperties) {
        for (Map.Entry<String, String> prop : mappingProperties.entrySet()) {
            String getter = prop.getKey();
            String setter = prop.getValue();
            DocumentModel setterDoc;
            if (setter.startsWith(DOCUMENT_PREFIX)) {
                setterDoc = inputDoc;
                setter = setter.substring(DOCUMENT_PREFIX.length());
            } else if (setter.startsWith(STEP_PREFIX)) {
                setterDoc = stepDoc;
                setter = setter.substring(STEP_PREFIX.length());
            } else {
                throw new NuxeoException("Unknown setter prefix: " + setter);
            }
            try {
                taskDoc.setPropertyValue(getter, setterDoc.getPropertyValue(setter));
            } catch (PropertyException e) {
                log.error("Could not map property on the task document in the taskVariables ", e);
            }
        }
        return session.saveDocument(taskDoc);
    }

    protected List<String> getAllActors(List<String> actors) {
        List<String> allActors = new ArrayList<String>();
        for (String actor : actors) {
            if (userManager.getGroup(actor) != null) {
                List<String> allSimpleUsers = userManager.getUsersInGroupAndSubGroups(actor);
                for (String string : allSimpleUsers) {
                    if (!allActors.contains(string)) {
                        allActors.add(string);
                    }
                }
                continue;
            }
            if (!allActors.contains(actor)) {
                allActors.add(actor);
            }
        }
        return allActors;
    }

}
