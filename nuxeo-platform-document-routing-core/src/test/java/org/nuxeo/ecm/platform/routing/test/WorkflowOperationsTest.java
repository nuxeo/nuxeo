/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.operation.CompleteTaskOperation;
import org.nuxeo.ecm.platform.routing.core.api.operation.GetOpenTasksOperation;
import org.nuxeo.ecm.platform.routing.core.api.operation.SetWorkflowVar;
import org.nuxeo.ecm.platform.routing.core.api.operation.StartWorkflowOperation;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class,
        AutomationFeature.class })
@Deploy({
        "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.platform.userworkspace.core", //
        "org.nuxeo.ecm.platform.userworkspace.types", //
        "org.nuxeo.ecm.platform.task.api", //
        "org.nuxeo.ecm.platform.task.core", //
        "org.nuxeo.ecm.platform.task.testing",
        "org.nuxeo.ecm.platform.routing.api",
        "org.nuxeo.ecm.platform.routing.core" //
})
@LocalDeploy({
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-operations-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-types-contrib.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD)
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
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET,
                "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        validate(routeDoc, session);
        OperationContext ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        // test start workflow
        OperationChain startWorkflowChain = new OperationChain("startWorkflow");
        startWorkflowChain.add(StartWorkflowOperation.ID).set("id", "myroute").set(
                "variables", "stringfield=test");
        automationService.run(ctx, startWorkflowChain);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();

        DocumentModel routeInstance = session.getDocument(new IdRef(
                (String) ctx.get("WorkflowId")));
        GraphRoute graph = routeInstance.getAdapter(GraphRoute.class);
        assertEquals(graph.getVariables().get("stringfield"), "test");

        // test SetWorkflowVar on the same context with StartWorkflow
        OperationChain setWorkflowVar = new OperationChain("setVar");
        setWorkflowVar.add(SetWorkflowVar.ID).set("name", "stringfield").set(
                "value", "test1");
        automationService.run(ctx, setWorkflowVar);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();

        routeInstance = session.getDocument(routeInstance.getRef());
        graph = routeInstance.getAdapter(GraphRoute.class);
        assertEquals(graph.getVariables().get("stringfield"), "test1");

        // test SetWorkflowVar in new context

        ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        setWorkflowVar = new OperationChain("setWVar");
        setWorkflowVar.add(SetWorkflowVar.ID).set("workflowInstanceId",
                routeInstance.getId()).set("name", "stringfield").set("value",
                "test2");
        automationService.run(ctx, setWorkflowVar);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();

        routeInstance = session.getDocument(routeInstance.getRef());
        graph = routeInstance.getAdapter(GraphRoute.class);
        assertEquals(graph.getVariables().get("stringfield"), "test2");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTasksOperations() throws Exception {
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(
                node1,
                transition("trans1", "node2",
                        "NodeVariables[\"button\"] == \"trans1\"",
                        "testchain_title1"));
        // task properties
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");
        String[] users = { ((NuxeoPrincipal) session.getPrincipal()).getName() };
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
        List<DocumentModel> tasks = (List<DocumentModel>) automationService.run(
                ctx, GetOpenTasksOperation.ID,
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
        tasks = (List<DocumentModel>) automationService.run(ctx,
                GetOpenTasksOperation.ID, params);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // invoke CompleteTaskOperation to end the task
        ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(tasks);
        params = new HashMap<String, Object>();
        params.put("status", "trans1");
        automationService.run(ctx, CompleteTaskOperation.ID, params);

        session.save();
        // invoke GetTaskOperation to check that there are no more open tasks
        ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        tasks = (List<DocumentModel>) automationService.run(ctx,
                GetOpenTasksOperation.ID,
                new HashMap<String, Object>());
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
    }
}