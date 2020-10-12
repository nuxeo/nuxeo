/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.routing.core.io;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(WorkflowFeature.class)
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-route-contrib.xml")
public class TaskWriterActionContextTest extends AbstractJsonWriterTest.External<TaskWriter, Task> {

    @Inject
    protected DocumentRoutingService documentRoutingService;

    @Inject
    protected CoreSession session;

    public TaskWriterActionContextTest() {
        super(TaskWriter.class, Task.class);
    }

    @Test
    public void shouldEvaluateTaskActionAvailabilityBasedOnWorkflowVars() throws IOException {
        assertCorrectAction(false, "validate");
        assertCorrectAction(true, "reject");
    }

    protected void assertCorrectAction(boolean condition, String expectedAction) throws IOException {
        DocumentModel doc = session.createDocumentModel("/", "src", "File");
        doc = session.createDocument(doc);
        session.save();
        String workflowModel = "test_Wf";
        String workflowInstanceId = documentRoutingService.createNewInstance("test_Wf",
                Collections.singletonList(doc.getId()), Collections.emptyMap(), session, true);

        List<Task> tasks = documentRoutingService.getTasks(null, null, workflowInstanceId, workflowModel, session);
        assertEquals(1, tasks.size());
        Task task = tasks.get(0);

        Map<String, Object> data = new HashMap<>();
        Map<String, Serializable> subData = new HashMap<>();
        subData.put("test_boolean", condition);
        data.put(Constants.VAR_WORKFLOW, subData);
        data.put(Constants.VAR_WORKFLOW_NODE, subData);
        documentRoutingService.endTask(session, task, data, "approve");

        tasks = documentRoutingService.getTasks(null, null, workflowInstanceId, workflowModel, session);
        assertEquals(1, tasks.size());
        task = tasks.get(0);

        JsonAssert taskActions = jsonAssert(task).get("taskInfo.taskActions");

        taskActions.isArray();
        taskActions.length(1);
        taskActions.get(0).has("name").isText().isEquals(expectedAction);
    }

}
