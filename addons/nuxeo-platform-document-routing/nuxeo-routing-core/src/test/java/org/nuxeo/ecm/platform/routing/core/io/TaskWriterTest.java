/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuno Cunha (ncunha@nuxeo.com)
 */

package org.nuxeo.ecm.platform.routing.core.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(WorkflowFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.routing.default", "org.nuxeo.ecm.platform.filemanager.api",
        "org.nuxeo.ecm.platform.filemanager.core" })
public class TaskWriterTest extends AbstractJsonWriterTest.External<TaskWriter, Task> {

    @Inject
    protected DocumentRoutingService documentRoutingService;

    @Inject
    protected CoreSession session;

    protected DocumentModel doc;

    protected Task task;

    public TaskWriterTest() {
        super(TaskWriter.class, Task.class);
    }

    @Before
    public void setUp() {
        doc = session.createDocumentModel("/", "src", "File");
        doc = session.createDocument(doc);
        session.save();

        String workflowModel = "SerialDocumentReview";
        String workflowInstanceId = documentRoutingService.createNewInstance(workflowModel,
                Collections.singletonList(doc.getId()), Collections.emptyMap(), session, true);

        List<Task> tasks = documentRoutingService.getTasks(null, null, workflowInstanceId, workflowModel, session);
        assertEquals(1, tasks.size());

        task = tasks.get(0);
    }

    @Test
    public void shouldWriteDefaultPropertiesWhenWorkflowIsRunningAndNoFetchersAreProvided() throws IOException {
        JsonAssert json = jsonAssert(task);
        assertTaskDefaultProperties(json);
    }

    @Test
    public void shouldOnlyWriteUserIdWhenActorsFetcherIsProvidedButActorDoesNotExist() throws IOException {
        task.setActors(Collections.singletonList("Unexisting User"));

        JsonAssert json = jsonAssert(task, RenderingContext.CtxBuilder.fetch("task", "actors").get());
        assertTaskDefaultProperties(json);

        JsonNode actorsNode = json.get("actors").getNode();
        assertEquals(1, actorsNode.size());

        JsonNode actor = actorsNode.get(0);
        assertEquals(1, actor.size());
        assertTrue(actor.has("id"));
        assertEquals("Unexisting User", actor.get("id").getTextValue());
    }

    @Test
    public void shouldOnlyWriteUserIdWhenActorsFetcherNotIsProvided() throws IOException {
        JsonAssert json = jsonAssert(task);
        assertTaskDefaultProperties(json);

        JsonNode actorsNode = json.get("actors").getNode();
        assertEquals(1, actorsNode.size());

        JsonNode actor = actorsNode.get(0);
        assertEquals(1, actor.size());
        assertTrue(actor.has("id"));
        assertEquals(session.getPrincipal().getName(), actor.get("id").getTextValue());
    }

    @Test
    public void shouldFetchUserPropertiesWhenActorsFetcherIsProvidedAndActorsExist() throws IOException {
        JsonAssert json = jsonAssert(task, RenderingContext.CtxBuilder.fetch("task", "actors").get());
        assertTaskDefaultProperties(json);

        JsonNode actorsNode = json.get("actors").getNode();
        assertEquals(1, actorsNode.size());

        JsonNode actor = actorsNode.get(0);
        assertThat(actor.size(), greaterThan(1));
        assertTrue(actor.has("entity-type"));
        assertEquals("user", actor.get("entity-type").getTextValue());
        assertTrue(actor.has("id"));
        assertEquals(session.getPrincipal().getName(), actor.get("id").getTextValue());
    }

    @Test
    public void shouldOnlyWriteDelegatedUserIdWhenActorsFetcherIsProvidedButDelegatedActorDoesNotExist()
            throws IOException {
        task.setDelegatedActors(Collections.singletonList("Unexisting User"));

        JsonAssert json = jsonAssert(task, RenderingContext.CtxBuilder.fetch("task", "actors").get());
        assertTaskDefaultProperties(json);

        JsonNode delegatedActorsNode = json.get("delegatedActors").getNode();
        assertEquals(1, delegatedActorsNode.size());

        JsonNode actor = delegatedActorsNode.get(0);
        assertEquals(1, actor.size());
        assertTrue(actor.has("id"));
        assertEquals("Unexisting User", actor.get("id").getTextValue());
    }

    @Test
    public void shouldOnlyWriteDelegatedUserIdWhenActorsFetcherNotIsProvided() throws IOException {
        task.setDelegatedActors(Collections.singletonList(session.getPrincipal().getName()));

        JsonAssert json = jsonAssert(task);
        assertTaskDefaultProperties(json);

        JsonNode delegatedActorsNode = json.get("delegatedActors").getNode();
        assertEquals(1, delegatedActorsNode.size());

        JsonNode actor = delegatedActorsNode.get(0);
        assertEquals(1, actor.size());
        assertTrue(actor.has("id"));
        assertEquals(session.getPrincipal().getName(), actor.get("id").getTextValue());
    }

    @Test
    public void shouldFetchDelegatedUserPropertiesWhenActorsFetcherIsProvidedAndDelegatedActorsExist()
            throws IOException {
        task.setDelegatedActors(Collections.singletonList(session.getPrincipal().getName()));

        JsonAssert json = jsonAssert(task, RenderingContext.CtxBuilder.fetch("task", "actors").get());
        assertTaskDefaultProperties(json);

        JsonNode delegatedActorsNode = json.get("delegatedActors").getNode();
        assertEquals(1, delegatedActorsNode.size());

        JsonNode actor = delegatedActorsNode.get(0);
        assertThat(actor.size(), greaterThan(1));
        assertTrue(actor.has("entity-type"));
        assertEquals("user", actor.get("entity-type").getTextValue());
        assertTrue(actor.has("id"));
        assertEquals(session.getPrincipal().getName(), actor.get("id").getTextValue());
    }

    @Test
    public void shouldFetchDelegatedUserPropertiesWhenActorsFetcherIsProvidedAndPrefixedDelegatedActorsExist()
            throws IOException {
        task.setDelegatedActors(Arrays.asList("user:Administrator", "group:administrators"));

        JsonAssert json = jsonAssert(task, RenderingContext.CtxBuilder.fetch("task", "actors").get());
        assertTaskDefaultProperties(json);

        JsonNode delegatedActorsNode = json.get("delegatedActors").getNode();
        assertEquals(2, delegatedActorsNode.size());

        JsonNode actor = delegatedActorsNode.get(0);
        assertThat(actor.size(), greaterThan(1));
        assertTrue(actor.has("entity-type"));
        assertEquals("user", actor.get("entity-type").getTextValue());
        assertTrue(actor.has("id"));
        assertEquals(session.getPrincipal().getName(), actor.get("id").getTextValue());

        JsonNode group = delegatedActorsNode.get(1);
        assertThat(group.size(), greaterThan(1));
        assertTrue(group.has("entity-type"));
        assertEquals("group", group.get("entity-type").getTextValue());
        assertTrue(group.has("id"));
        assertEquals("administrators", group.get("id").getTextValue());
    }

    @Test
    public void shouldOnyWriteUserIdWhenInitiatorFetcherIsNotProvided() throws IOException {
        JsonAssert json = jsonAssert(task);
        assertTaskDefaultProperties(json);

        JsonNode initiatorNode = json.get("workflowInitiator").getNode();
        initiatorNode.isTextual();
        assertEquals(session.getPrincipal().getName(), initiatorNode.getTextValue());
    }

    @Test
    public void shouldFetchUserPropertiesWhenInitiatorFetcherIsProvided() throws IOException {
        JsonAssert json = jsonAssert(task, RenderingContext.CtxBuilder.fetch("task", "workflowInitiator").get());
        assertTaskDefaultProperties(json);

        JsonNode initiatorNode = json.get("workflowInitiator").getNode();
        initiatorNode.isObject();
        assertThat(initiatorNode.size(), greaterThan(1));

        assertTrue(initiatorNode.has("entity-type"));
        assertEquals("user", initiatorNode.get("entity-type").getTextValue());
        assertEquals(session.getPrincipal().getName(), initiatorNode.get("id").getTextValue());
    }

    @Test
    public void shouldFetchTargetDocumentPropertiesWhenTargetDocumentsFetcherIsProvided() throws IOException {
        JsonAssert json = jsonAssert(task, RenderingContext.CtxBuilder.fetch("task", "targetDocumentIds").get());
        assertTaskDefaultProperties(json);

        JsonNode docsNode = json.get("targetDocumentIds").getNode();
        assertEquals(1, docsNode.size());

        JsonNode document = docsNode.get(0);
        assertThat(document.size(), greaterThan(1));
        assertTrue(document.has("entity-type"));
        assertEquals("document", document.get("entity-type").getTextValue());
        assertTrue(document.has("uid"));
        assertEquals(doc.getId(), document.get("uid").getTextValue());
    }

    @Test
    public void shouldWriteDocumentIdWhenTargetDocumentsFetcherIsNotProvided() throws IOException {
        JsonAssert json = jsonAssert(task);
        assertTaskDefaultProperties(json);

        JsonNode docsNode = json.get("targetDocumentIds").getNode();
        assertEquals(1, docsNode.size());

        JsonNode document = docsNode.get(0);
        assertEquals(1, document.size());
        assertTrue(document.has("id"));
        assertEquals(doc.getId(), document.get("id").getTextValue());
    }

    @Test
    public void shouldNotWriteAnythingWhenTargetDocumentDoesNotExist() throws IOException {
        task.setTargetDocumentsIds(Collections.emptyList());

        JsonAssert json = jsonAssert(task);
        assertTaskDefaultProperties(json);

        JsonNode docsNode = json.get("targetDocumentIds").getNode();
        assertEquals(0, docsNode.size());
    }

    @Test
    public void shouldWriteTaskSpecificInfoWhenWorkflowIsRunningAndNoFetchersAreProvided() throws IOException {
        task.addComment("An Author", "A Comment");

        JsonAssert json = jsonAssert(task);
        assertTaskDefaultProperties(json);

        json.get("workflowInstanceId").isEquals(task.getProcessId());
        json.get("workflowModelName").isEquals("SerialDocumentReview");
        json.get("workflowInitiator").isEquals(session.getPrincipal().getName());
        json.get("workflowLifeCycleState").isEquals("running");
        json.get("workflowTitle").isEquals("wf.serialDocumentReview.SerialDocumentReview");
        json.get("state").isEquals("opened");
        json.get("name").isEquals("wf.serialDocumentReview.chooseParticipants");
        json.get("directive").isEquals("wf.serialDocumentReview.pleaseSelect");

        assertEquals(1, json.get("comments").getNode().size());
        JsonNode commentNode = json.get("comments").getNode().get(0);
        assertEquals("An Author", commentNode.get("author").getTextValue());
        assertEquals("A Comment", commentNode.get("text").getTextValue());

        json.get("variables").properties(3);
        json.has("variables.comment").isNull();
        json.has("variables.participants").isArray().length(0);
        json.has("variables.validationOrReview").isText().isEquals("validation");

        json.get("taskInfo.allowTaskReassignment").isFalse();
    }

    @Test
    public void shouldNotWriteWorkflowAndNodeSpecificInfoWhenThereIsNoWorkflowRunning() throws IOException {
        // Create a task not related to a workflow instance
        List<Task> tasks = Framework.getService(TaskService.class).createTask(session,
                (NuxeoPrincipal) session.getPrincipal(), doc, "testNoWorkflowTask",
                Collections.singletonList("Administrator"), false, "some directive", "some comment", new Date(),
                Collections.emptyMap(), null);
        assertEquals(1, tasks.size());

        JsonAssert json = jsonAssert(tasks.get(0));
        assertDetachedTaskDefaultProperties(json);
    }

    protected static void assertTaskDefaultProperties(JsonAssert json) throws IOException {
        json.isObject();
        json.properties(20);
        json.has("entity-type").isEquals("task");
        json.has("id").isText();
        json.has("name").isText();
        json.has("workflowInstanceId").isText();
        json.has("workflowModelName").isText();
        json.has("workflowInitiator").notNull();
        json.has("workflowTitle").isText();
        json.has("workflowLifeCycleState").isText();
        json.has("graphResource").isText();
        json.has("state").isText();
        json.has("directive").isText();
        json.has("created").isText();
        json.has("dueDate").isText();
        json.has("nodeName").isText();
        json.has("targetDocumentIds").isArray();
        json.has("actors").isArray();
        json.has("delegatedActors").isArray();
        json.has("comments").isArray();
        assertCommentsSectionStructure(json);

        json.has("variables").isObject();
        json.has("taskInfo").isObject().properties(4);
        json.has("taskInfo.allowTaskReassignment").isBool();
        json.has("taskInfo.taskActions").isArray();
        json.get("taskInfo.taskActions").getNode().getElements().forEachRemaining(element -> {
            assertTrue(element.isObject());
            assertTrue(element.has("name"));
            assertTrue(element.get("name").isTextual());
            assertTrue(element.has("url"));
            assertTrue(element.get("url").isTextual());
            assertTrue(element.has("label"));
            assertTrue(element.get("label").isTextual());
        });
        json.has("taskInfo.layoutResource").isObject().properties(2);
        json.has("taskInfo.layoutResource.name").isText();
        json.has("taskInfo.layoutResource.url").isText();
        json.get("taskInfo.schemas").isArray();
        json.get("taskInfo.schemas").getNode().getElements().forEachRemaining(element -> {
            assertTrue(element.isObject());
            assertTrue(element.has("name"));
            assertTrue(element.get("name").isTextual());
            assertTrue(element.has("url"));
            assertTrue(element.get("url").isTextual());
        });
    }

    protected static void assertDetachedTaskDefaultProperties(JsonAssert json) throws IOException {
        json.isObject();
        json.properties(14);
        json.has("entity-type").isEquals("task");
        json.has("id").isText();
        json.has("name").isText();
        json.has("workflowInstanceId").isNull();
        json.hasNot("workflowModelName");
        json.hasNot("workflowInitiator");
        json.hasNot("workflowTitle");
        json.hasNot("workflowLifeCycleState");
        json.hasNot("graphResource");
        json.has("state").isText();
        json.has("directive").isText();
        json.has("created").isText();
        json.has("dueDate").isText();
        json.has("nodeName").isNull();
        json.has("targetDocumentIds").isArray();
        json.has("actors").isArray();
        json.has("delegatedActors").isArray();
        json.has("comments").isArray();
        assertCommentsSectionStructure(json);

        json.has("variables").isObject();
        json.hasNot("taskInfo");
    }

    protected static void assertCommentsSectionStructure(JsonAssert json) throws IOException {
        json.has("comments").getNode().getElements().forEachRemaining(element -> {
            assertTrue(element.isObject());
            assertTrue(element.has("author"));
            assertTrue(element.get("author").isTextual());
            assertTrue(element.has("text"));
            assertTrue(element.get("text").isTextual());
            assertTrue(element.has("date"));
            assertTrue(element.get("date").isTextual());
        });
    }
}