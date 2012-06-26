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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.EXECUTION_TYPE_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ExecutionTypeValues.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.RoutingTaskService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({
        "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory.sql", //
        "org.nuxeo.ecm.platform.userworkspace.core", //
        "org.nuxeo.ecm.platform.userworkspace.types", //
        "org.nuxeo.ecm.platform.task.api", "org.nuxeo.ecm.platform.task.core",
        "org.nuxeo.ecm.platform.task.testing",
        "org.nuxeo.ecm.platform.routing.core" //

})
@LocalDeploy({
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-sql-directories-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-operations-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/test-graph-types-contrib.xml" })
public class GraphRouteTest {

    protected static final String TYPE_ROUTE_NODE = "RouteNode";

    @Inject
    protected FeaturesRunner featuresRunner;

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentRoutingService routing;

    // init userManager now for early user tables creation (cleaner debug)
    @Inject
    protected UserManager userManager;

    @Inject
    protected RoutingTaskService routingTaskService;

    @Inject
    protected TaskService taskService;

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

    protected CoreSession openSession(NuxeoPrincipal principal)
            throws ClientException {
        CoreFeature coreFeature = featuresRunner.getFeature(CoreFeature.class);
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        return coreFeature.getRepository().getRepositoryHandler().openSession(
                ctx);
    }

    protected void closeSession(CoreSession session) {
        CoreInstance.getInstance().close(session);
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

    protected Map<String, Serializable> button(String name, String label,
            String filter) {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_BTN_NAME, name);
        m.put(GraphNode.PROP_BTN_LABEL, label);
        m.put(GraphNode.PROP_BTN_FILTER, filter);
        return m;
    }

    protected void setButtons(DocumentModel node,
            Map<String, Serializable>... buttons) throws ClientException {
        node.setPropertyValue(GraphNode.PROP_TASK_BUTTONS,
                (Serializable) Arrays.asList(buttons));
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

    @Test
    public void testAutomationChainVariableChange() throws Exception {
        // route model var
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET,
                "FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        // node model
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN,
                "testchain_stringfield");
        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN,
                "testchain_stringfield2");
        // node model var
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1 = session.saveDocument(node1);
        DocumentRoute route = instantiateAndRun();
        assertTrue(route.isDone());

        // check route instance var
        DocumentModel r = route.getDocument();
        String s = (String) r.getPropertyValue("stringfield");
        assertEquals("foo", s);
        // Calendar d = (Calendar) r.getPropertyValue("datefield");
        // assertEquals("XXX", d);

        // check node instance var
        // must be admin to get children, due to rights restrictions
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        CoreSession ses = openSession(admin);
        DocumentModel c = ses.getChildren(r.getRef()).get(0);
        s = (String) c.getPropertyValue("stringfield2");
        assertEquals("bar", s);
        closeSession(ses);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleTransition() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "true", "testchain_title1"),
                transition("trans2", "node2", "false", "testchain_title2"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun();

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testResume() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans12", "node2", "true", "testchain_title1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(node2, transition("trans23", "node3", "true"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3");
        node3.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node3 = session.saveDocument(node3);

        DocumentRoute route = instantiateAndRun();

        assertFalse(route.isDone());

        // now resume, as if the task was actually executed
        Map<String, Object> data = new HashMap<String, Object>();
        routing.resumeInstance(route.getDocument().getRef(), session, "node2",
                data);

        route.getDocument().refresh();
        assertTrue(route.isDone());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeAll() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "true", "testchain_title1"),
                transition("trans2", "node2", "true", "testchain_descr1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun();

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());
        assertEquals("descr 1", doc.getPropertyValue("dc:description"));
        assertEquals("rights 1", doc.getPropertyValue("dc:rights"));
    }

    // a few more nodes before the merge
    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeAll2() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node2", "true"),
                transition("trans2", "node3", "true"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        setTransitions(node2, transition("trans1", "node4", "true"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3");
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node3, transition("trans2", "node4", "true"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4");
        node4.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node4.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node4 = session.saveDocument(node4);

        DocumentRoute route = instantiateAndRun();

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());
        assertEquals("descr 1", doc.getPropertyValue("dc:description"));
        assertEquals("rights 1", doc.getPropertyValue("dc:rights"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeOne() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2", "true"),
                transition("trans13", "node3", "true"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        setTransitions(node2, transition("trans25", "node5", "true"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3");
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node3, transition("trans34", "node4", "true"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4");
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr2");
        setTransitions(node4, transition("trans45", "node5", "true"));
        node4 = session.saveDocument(node4);

        DocumentModel node5 = createNode(routeDoc, "node5");
        node5.setPropertyValue(GraphNode.PROP_MERGE, "one");
        node5.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node5.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node5 = session.saveDocument(node5);

        DocumentRoute route = instantiateAndRun();

        assertTrue(route.isDone());

        doc.refresh();
        assertEquals("title 1", doc.getTitle());
        assertEquals("descr 1", doc.getPropertyValue("dc:description"));
        // didn't go up to descr 2, which was canceled
        assertEquals("rights 1", doc.getPropertyValue("dc:rights"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeWithLoopTransition() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "true", "testchain_title1"),
                transition("trans2", "node2", "true", "testchain_descr1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        setTransitions(node2, transition("transloop", "node1", "false"));
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun();

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());
        assertEquals("descr 1", doc.getPropertyValue("dc:description"));
        assertEquals("rights 1", doc.getPropertyValue("dc:rights"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRouteWithTasks() throws Exception {

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(
                node1,
                transition("trans1", "node2",
                        "Context[\"button\"] == \"trans1\"", "testchain_title1"));

        // task properties
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users = { user1.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun();

        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Map<String, Object> data = new HashMap<String, Object>();
        CoreSession sessionUser1 = openSession(user1);
        routingTaskService.endTask(sessionUser1, tasks.get(0), data, "trans1");
        closeSession(sessionUser1);
        // end task and verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        session = openSession(admin);
        route = session.getDocument(route.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        assertTrue(route.isDone());
    }

    @Test
    public void testEvaluateTaskAssigneesFromVariable() throws Exception {
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        NuxeoPrincipal user2 = userManager.getPrincipal("myuser1");
        assertNotNull(user2);

        List<String> assignees = new ArrayList<String>();
        assignees.add(user1.getName());
        assignees.add(user2.getName());

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET,
                "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc.setPropertyValue("fctroute1:assignees",
                (Serializable) assignees);

        routeDoc = session.saveDocument(routeDoc);

        DocumentModel node1 = createNode(routeDoc, "node1");
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION,
                "testPermission");

        // add a workflow variables with name "assignees"
        setTransitions(node1,
                transition("trans1", "node2", "true", "testchain_title1"));
        node1.setPropertyValue("rnode:taskAssigneesExpr",
                "Context[\"assignees\"]");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2");
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun();

        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        Task ts = tasks.get(0);
        assertEquals(2, ts.getActors().size());

        Map<String, Object> data = new HashMap<String, Object>();
        CoreSession sessionUser2 = openSession(user2);
        routingTaskService.endTask(sessionUser2, tasks.get(0), data, "trans1");
        closeSession(sessionUser2);
        // end task and verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        session = openSession(admin);
        route = session.getDocument(route.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        assertTrue(route.isDone());
        assertTrue(session.hasPermission(user1, doc.getRef(), "testPermission"));
        assertTrue(session.hasPermission(user2, doc.getRef(), "testPermission"));

    }
}
