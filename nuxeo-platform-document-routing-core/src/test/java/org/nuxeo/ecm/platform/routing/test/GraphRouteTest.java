/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ExecutionTypeValues.graph;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.platform.userworkspace.core", //
        "org.nuxeo.ecm.platform.userworkspace.types", //
        "org.nuxeo.ecm.platform.routing.core", //
})
@LocalDeploy({
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-sql-directories-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-operations-contrib.xml" })
public class GraphRouteTest {

    protected static final String TYPE_ROUTE_NODE = "RouteNode";

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentRoutingService routing;

    // init userManager now for early user tables creation (cleaner debug)
    @Inject
    protected UserManager userManager;

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

    @After
    public void tearDown() {
        // breakpoint here to examine database after test
    }

    protected DocumentModel createRoute(String name) throws ClientException,
            PropertyException {
        DocumentModel route = session.createDocumentModel("/", name,
                DOCUMENT_ROUTE_DOCUMENT_TYPE);
        route.setPropertyValue(EXECUTION_TYPE_PROPERTY_NAME, graph.name());
        route.setPropertyValue("dc:title", name);
        route.setPropertyValue(ATTACHED_DOCUMENTS_PROPERTY_NAME,
                (Serializable) Collections.singletonList(doc.getId()));
        return session.createDocument(route);
    }

    protected DocumentModel createNode(DocumentModel route, String name)
            throws ClientException, PropertyException {
        DocumentModel node = session.createDocumentModel(
                route.getPathAsString(), name, TYPE_ROUTE_NODE);
        node.setPropertyValue(GraphNode.PROP_NODE_ID, name);
        return session.createDocument(node);
    }

    protected Map<String, Serializable> transition(String name, String target,
            String condition) throws ClientException {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_TRANS_NAME, name);
        m.put(GraphNode.PROP_TRANS_TARGET, target);
        m.put(GraphNode.PROP_TRANS_CONDITION, condition);
        return m;
    }

    protected Map<String, Serializable> transition(String name, String target,
            String condition, String chainId) throws ClientException {
        Map<String, Serializable> m = transition(name, target, condition);
        m.put(GraphNode.PROP_TRANS_CHAIN, chainId);
        return m;
    }

    protected void setTransitions(DocumentModel node,
            Map<String, Serializable>... transitions) throws ClientException {
        node.setPropertyValue(GraphNode.PROP_TRANSITIONS,
                (Serializable) Arrays.asList(transitions));
    }

    protected DocumentRoute instantiateAndRun() throws ClientException {
        // route model
        DocumentRoute route = routeDoc.getAdapter(DocumentRoute.class);
        // draft -> validated
        route = routing.validateRouteModel(route, session);
        // create instance and start
        route = routing.createNewInstance(route, doc.getId(), session, true);
        return route;
    }

    @Test
    public void testExceptionIfNoStartNode() throws Exception {
        // route
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun();
            fail("Should throw because no start node");
        } catch (ClientRuntimeException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("No start node for graph"));
        }
    }

    @Test
    public void testExceptionIfNoTrueTransition() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun();
            fail("Should throw because no transition is true");
        } catch (ClientRuntimeException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("No transition evaluated to true"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExceptionIfTransitionIsNotBoolean() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node1", "'notaboolean'"));
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun();
            fail("Should throw because transition condition is no bool");
        } catch (ClientRuntimeException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("does not evaluate to a boolean"));
        }
    }

    @Test
    public void testOneNodeStartStop() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1 = session.saveDocument(node1);
        DocumentRoute route = instantiateAndRun();
        assertTrue(route.isDone());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExceptionIfLooping() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node1", "true"));
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun();
            fail("Should throw because execution is looping");
        } catch (ClientRuntimeException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Execution is looping"));
        }
    }

    @Test
    public void testAutomationChains() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_title2");
        node1 = session.saveDocument(node1);
        DocumentRoute route = instantiateAndRun();
        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 2", doc.getTitle());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleTransition() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "true", "testchain_title1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun();

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());
    }

}
