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
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ExecutionTypeValues.graph;

import java.io.Serializable;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.operation.SetWorkflowVar;
import org.nuxeo.ecm.platform.routing.core.api.operation.StartWorkflowOperation;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

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
public class WorkflowOperationsTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentRoutingService routing;

    @Inject
    protected AutomationService automationService;

    // a doc, associated to the route
    protected DocumentModel doc;

    // the route model we'll use
    protected DocumentModel routeDoc;

    @Before
    public void setUp() throws Exception {
        assertNotNull(routing);
        doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "file");
        doc = session.createDocument(doc);
        routeDoc = createRoute("myroute");
    }

    protected DocumentModel createRoute(String name) throws ClientException,
            PropertyException {
        routeDoc = session.createDocumentModel("/", name,
                DOCUMENT_ROUTE_DOCUMENT_TYPE);
        routeDoc.setPropertyValue(EXECUTION_TYPE_PROPERTY_NAME, graph.name());
        routeDoc.setPropertyValue("dc:title", name);
        routeDoc.setPropertyValue(ATTACHED_DOCUMENTS_PROPERTY_NAME,
                (Serializable) Collections.singletonList(doc.getId()));
        routeDoc = session.createDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1 = session.saveDocument(node1);
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET,
                "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        return routing.validateRouteModel(
                routeDoc.getAdapter(DocumentRoute.class), session).getDocument();
    }

    protected DocumentModel createNode(DocumentModel route, String name)
            throws ClientException, PropertyException {
        DocumentModel node = session.createDocumentModel(
                route.getPathAsString(), name, "RouteNode");
        node.setPropertyValue(GraphNode.PROP_NODE_ID, name);
        return session.createDocument(node);
    }

    @Test
    public void testOperations() throws Exception {
        OperationContext ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        // test start workflow
        OperationChain startWorkflowChain = new OperationChain("startWorkflow");
        startWorkflowChain.add(StartWorkflowOperation.ID).set("id", "myroute").set(
                "variables", "stringfield=test");
        automationService.run(ctx, startWorkflowChain);
        session.save();
        DocumentModel routeInstance = session.getDocument(new IdRef(
                (String) ctx.get("WorkflowId")));
        GraphRoute graph = routeInstance.getAdapter(GraphRoute.class);
        assertEquals(graph.getVariables().get("stringfield"), "test");

        // test SetWorkflowVar

        ctx = new OperationContext();
        ctx.setCoreSession(session);
        ctx.setInput(doc);
        // test start workflow
        OperationChain setWorkflowVar = new OperationChain("startWorkflow");
        setWorkflowVar.add(SetWorkflowVar.ID).set("workflowInstanceId",
                routeInstance.getId()).set("name", "stringfield").set("value",
                "test2");
        automationService.run(ctx, setWorkflowVar);
        session.save();
        routeInstance = session.getDocument(routeInstance.getRef());
        graph = routeInstance.getAdapter(GraphRoute.class);
        assertEquals(graph.getVariables().get("stringfield"), "test2");
    }

}
