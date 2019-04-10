/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.operation.CompleteTaskOperation;
import org.nuxeo.ecm.platform.routing.core.api.operation.GetOpenTasksOperation;
import org.nuxeo.ecm.platform.routing.core.api.operation.SetWorkflowVar;
import org.nuxeo.ecm.platform.routing.core.api.operation.StartWorkflowOperation;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.webengine.test.WebEngineFeatureCore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ WorkflowFeature.class, WebEngineFeatureCore.class })
public class WorkflowOperationsTest extends AbstractGraphRouteTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentRoutingService routing;

    @Inject
    protected AutomationService automationService;

    @Before
    public void setUp() throws Exception {
        assertNotNull(routing);
        routing.invalidateRouteModelsCache();
        doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "file");
        doc = session.createDocument(doc);
        routeDoc = createRoute("myroute", session);
    }

    @Test
    public void testStartWorkflowOperation() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1 = session.saveDocument(node1);
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        validate(routeDoc, session);
        OperationContext ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        // test start workflow with
        // variables in json format

        Properties jsonProperties = new Properties();
        jsonProperties.put("stringfield", "test");
        jsonProperties.put("myassignees", "[\"x\", \"y\"]");
        jsonProperties.put(
                "datefield",
                (String) Scripting.newExpression(
                        "org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)").eval(ctx));
        jsonProperties.put("ids", "[1, 2, 3]");
        OperationChain startWorkflowChain = new OperationChain("startWorkflow");
        startWorkflowChain.add(StartWorkflowOperation.ID).set("id", "myroute").set("variables", jsonProperties);
        automationService.run(ctx, startWorkflowChain);
        session.save();

        DocumentModel routeInstance = session.getDocument(new IdRef((String) ctx.get("WorkflowId")));
        GraphRoute graph = routeInstance.getAdapter(GraphRoute.class);
        Map<String, Serializable> vars = graph.getVariables();
        assertEquals(routeInstance.getPropertyValue("fctroute1:stringfield"), "test");
        assertEquals(vars.get("stringfield"), "test");
        String[] assignesVar = (String[]) vars.get("myassignees");
        assertEquals(2, assignesVar.length);
        assertEquals("x", assignesVar[0]);
        assertEquals("y", assignesVar[1]);
        Long[] ids = (Long[]) vars.get("ids");
        assertEquals(3, ids.length);
        assertEquals(Long.valueOf(1L), ids[0]);
        assertEquals(Long.valueOf(2L), ids[1]);
        assertEquals(Long.valueOf(3L), ids[2]);

        Calendar varDate = Calendar.getInstance();
        varDate.setTime((Date) graph.getVariables().get("datefield"));
        assertEquals(Calendar.getInstance().get(Calendar.DAY_OF_MONTH), varDate.get(Calendar.DAY_OF_MONTH));

        // test SetWorkflowVar on the same context with StartWorkflow
        OperationChain setWorkflowVar = new OperationChain("setVar");
        setWorkflowVar.add(SetWorkflowVar.ID).set("name", "stringfield").set("value", "test1");
        automationService.run(ctx, setWorkflowVar);
        session.save();

        routeInstance = session.getDocument(routeInstance.getRef());
        graph = routeInstance.getAdapter(GraphRoute.class);
        assertEquals(graph.getVariables().get("stringfield"), "test1");

        // test SetWorkflowVar in new context

        ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        setWorkflowVar = new OperationChain("setWVar");
        setWorkflowVar.add(SetWorkflowVar.ID).set("workflowInstanceId", routeInstance.getId()).set("name",
                "stringfield").set("value", "test2");
        automationService.run(ctx, setWorkflowVar);
        session.save();

        routeInstance = session.getDocument(routeInstance.getRef());
        graph = routeInstance.getAdapter(GraphRoute.class);
        assertEquals(graph.getVariables().get("stringfield"), "test2");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTasksOperations() throws Exception {
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.addFacet("FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "NodeVariables[\"button\"] == \"trans1\"", "testchain_title1"));
        // task properties
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");
        String[] users = { session.getPrincipal().getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        node1 = session.saveDocument(node1);
        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute instance = instantiateAndRun(session);

        session.save();
        // invoke GetOpenedTasks operation without parameters
        OperationContext ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        List<DocumentModel> tasks = (List<DocumentModel>) automationService.run(ctx, GetOpenTasksOperation.ID,
                new HashMap<String, Object>());
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // invoke GetOpenedTasks operation with parameters
        ctx = new OperationContext();
        ctx.setCoreSession(session);
        DocumentModelList docs = new DocumentModelListImpl();
        docs.add(doc);

        ctx.setInput(docs);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("username", users[0]);
        params.put("nodeId", "node1");
        params.put("processId", instance.getDocument().getId());
        tasks = (List<DocumentModel>) automationService.run(ctx, GetOpenTasksOperation.ID, params);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // invoke CompleteTaskOperation to end the task
        // send node and workflow vars
        ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(tasks);

        Properties workflowVars = new Properties();
        workflowVars.put("stringfield", "completeTaskTest");
        workflowVars.put("myassignees", "[\"xx\", \"yy\"]");

        Properties nodeVars = new Properties();
        nodeVars.put("stringfield2", "testNodeVar");

        OperationChain completeTask = new OperationChain(CompleteTaskOperation.ID);
        completeTask.add(CompleteTaskOperation.ID).set("status", "trans1").set("workflowVariables", workflowVars).set(
                "nodeVariables", nodeVars);
        automationService.run(ctx, completeTask);

        session.save();
        DocumentModel routeInstance = session.getDocument(new IdRef(instance.getDocument().getId()));
        GraphRoute graph = routeInstance.getAdapter(GraphRoute.class);
        Map<String, Serializable> vars = graph.getVariables();
        assertEquals(routeInstance.getPropertyValue("fctroute1:stringfield"), "completeTaskTest");
        assertEquals(vars.get("stringfield"), "completeTaskTest");
        String[] assignesVar = (String[]) vars.get("myassignees");
        assertEquals(2, assignesVar.length);
        assertEquals("xx", assignesVar[0]);
        assertEquals("yy", assignesVar[1]);

        GraphNode n1 = graph.getNode("node1");
        assertEquals("testNodeVar", n1.getVariables().get("stringfield2"));

        // invoke GetTaskOperation to check that there are no more open tasks
        ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        tasks = (List<DocumentModel>) automationService.run(ctx, GetOpenTasksOperation.ID,
                new HashMap<String, Object>());
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
    }

}
