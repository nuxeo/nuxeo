/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.WORKFLOW_FORCE_RESUME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.api.operation.BulkRestartWorkflow;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.State;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.TaskInfo;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class GraphRouteTest extends AbstractGraphRouteTest {

    protected static final String TYPE_ROUTE_NODE = "RouteNode";

    @Inject
    protected CoreFeature coreFeature;

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
    protected TaskService taskService;

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

    @After
    public void tearDown() {
        // breakpoint here to examine database after test
    }

    protected CloseableCoreSession openSession(NuxeoPrincipal principal) {
        return coreFeature.openCoreSession(principal);
    }

    protected Map<String, Serializable> keyvalue(String key, String value) {
        Map<String, Serializable> m = new HashMap<String, Serializable>();
        m.put(GraphNode.PROP_KEYVALUE_KEY, key);
        m.put(GraphNode.PROP_KEYVALUE_VALUE, value);
        return m;
    }

    protected void setSubRouteVariables(DocumentModel node,
            @SuppressWarnings("unchecked") Map<String, Serializable>... keyvalues) {
        node.setPropertyValue(GraphNode.PROP_SUB_ROUTE_VARS, (Serializable) Arrays.asList(keyvalues));
    }

    @Test
    public void testExceptionIfNoStartNode() throws Exception {
        // route
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun(session);
            fail("Should throw because no start node");
        } catch (DocumentRouteException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("No start node for graph"));
        }
    }

    @Test
    public void testExceptionIfNoTrueTransition() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun(session);
            fail("Should throw because no transition is true");
        } catch (DocumentRouteException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("No transition evaluated to true"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExceptionIfTransitionIsNotBoolean() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node1", "'notaboolean'"));
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun(session);
            fail("Should throw because transition condition is no bool");
        } catch (DocumentRouteException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("does not evaluate to a boolean"));
        }
    }

    @Test
    public void testOneNodeStartStop() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1 = session.saveDocument(node1);
        DocumentRoute route = instantiateAndRun(session);
        assertTrue(route.isDone());
    }

    @Test
    public void testStartWithMap() throws Exception {
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1 = session.saveDocument(node1);
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("stringfield", "ABC");
        DocumentRoute route = instantiateAndRun(session, Collections.singletonList(doc.getId()), map);
        assertTrue(route.isDone());
        String v = (String) route.getDocument().getPropertyValue("fctroute1:stringfield");
        assertEquals("ABC", v);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExceptionIfLooping() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node1"));
        node1 = session.saveDocument(node1);
        try {
            instantiateAndRun(session);
            fail("Should throw because execution is looping");
        } catch (DocumentRouteException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("Execution is looping"));
        }
    }

    @Test
    public void testAutomationChains() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_title2");
        node1 = session.saveDocument(node1);
        DocumentRoute route = instantiateAndRun(session);
        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 2", doc.getTitle());
    }

    @Test
    public void testAutomationChainVariableChange() throws Exception {
        // route model var
        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        // node model
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_stringfield");
        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_stringfield2");
        // node model var
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1 = session.saveDocument(node1);
        DocumentRoute route = instantiateAndRun(session);
        assertTrue(route.isDone());

        // check route instance var
        DocumentModel r = route.getDocument();
        String s = (String) r.getPropertyValue("fctroute1:stringfield");
        assertEquals("foo", s);
        // Calendar d = (Calendar) r.getPropertyValue("datefield");
        // assertEquals("XXX", d);

        // check node instance var
        // must be admin to get children, due to rights restrictions
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession ses = openSession(admin)) {
            DocumentModel c = ses.getChildren(r.getRef()).get(0);
            s = (String) c.getPropertyValue("stringfield2");
            assertEquals("bar", s);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleTransition() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node2", "true", "testchain_title1"),
                transition("trans2", "node2", "false", "testchain_title2"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun(session);

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());

        // check start/end dates and counts
        DocumentModel doc1 = ((GraphRoute) route).getNode("node1").getDocument();
        assertEquals(Long.valueOf(1), doc1.getPropertyValue("rnode:count"));
        assertNotNull(doc1.getPropertyValue("rnode:startDate"));
        assertNotNull(doc1.getPropertyValue("rnode:endDate"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testResume() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2", "true", "testchain_title1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(node2, transition("trans23", "node3"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node3 = session.saveDocument(node3);

        DocumentRoute route = instantiateAndRun(session);

        assertFalse(route.isDone());

        // now resume, as if the task was actually executed
        routing.resumeInstance(route.getDocument().getId(), "node2", null, null, session);

        route.getDocument().refresh();
        assertTrue(route.isDone());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancel() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2", "true", "testchain_title1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(node2, transition("trans23", "node3"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node3 = session.saveDocument(node3);

        DocumentModelList cancelledTasks = session.query("Select * from TaskDoc where ecm:currentLifeCycleState = 'cancelled'");
        assertEquals(0, cancelledTasks.size());

        DocumentRoute route = instantiateAndRun(session);

        assertFalse(route.isDone());

        List<Task> tasks = taskService.getTaskInstances(doc, (NuxeoPrincipal) null, session);
        assertEquals(1, tasks.size());

        route.cancel(session);
        route.getDocument().refresh();
        assertTrue(route.isCanceled());
        session.save();

        tasks = taskService.getTaskInstances(doc, (NuxeoPrincipal) null, session);
        assertEquals(0, tasks.size());
        cancelledTasks = session.query("Select * from TaskDoc where ecm:currentLifeCycleState = 'cancelled'");
        assertEquals(1, cancelledTasks.size());
        DocumentRef routeRef = route.getDocument().getRef();

        routing.cleanupDoneAndCanceledRouteInstances(session.getRepositoryName(), 0);
        session.save();
        assertFalse(session.exists(routeRef));
        for (DocumentModel cancelledTask : cancelledTasks) {
            assertFalse(session.exists(cancelledTask.getRef()));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeAll() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node2", "true", "testchain_title1"),
                transition("trans2", "node2", "true", "testchain_descr1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun(session);

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
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node2"), transition("trans2", "node3"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        setTransitions(node2, transition("trans1", "node4"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node3, transition("trans2", "node4"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4", session);
        node4.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node4.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node4 = session.saveDocument(node4);

        DocumentRoute route = instantiateAndRun(session);

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());
        assertEquals("descr 1", doc.getPropertyValue("dc:description"));
        assertEquals("rights 1", doc.getPropertyValue("dc:rights"));
    }

    // a few more nodes before the merge
    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeAllWithTasks() throws Exception {
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);

        NuxeoPrincipal user3 = userManager.getPrincipal("myuser3");
        assertNotNull(user3);

        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node2"), transition("trans2", "node3"),
                transition("trans3", "node4"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        setTransitions(node2, transition("trans1", "node5"));

        node2.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node2.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users = { user1.getName() };
        node2.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node2, button("btn1", "label-btn1", "filterrr"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node3, transition("trans2", "node5"));

        node3.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node3.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node3.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users2 = { user2.getName() };
        node3.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users2);
        setButtons(node1, button("btn2", "label-btn2", "filterrr"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4", session);
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node4, transition("trans3", "node5"));

        node4.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node4.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node4.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users3 = { user3.getName() };
        node4.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users3);
        setButtons(node1, button("btn2", "label-btn2", "filterrr"));
        node4 = session.saveDocument(node4);

        DocumentModel node5 = createNode(routeDoc, "node5", session);
        node5.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node5.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node5.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node5 = session.saveDocument(node5);

        DocumentRoute route = instantiateAndRun(session);

        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Map<String, Object> data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            // task assignees have READ on the route instance
            assertNotNull(sessionUser1.getDocument(route.getDocument().getRef()));
            routing.endTask(sessionUser1, tasks.get(0), data, "trans1");
        }

        tasks = taskService.getTaskInstances(doc, user2, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            // task assignees have READ on the route instance
            assertNotNull(sessionUser2.getDocument(route.getDocument().getRef()));
            routing.endTask(sessionUser2, tasks.get(0), data, "trans2");
        }

        // end task and verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession session = openSession(admin)) {
            route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            assertFalse(route.isDone());
        }

        tasks = taskService.getTaskInstances(doc, user3, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser3 = openSession(user3)) {
            // task assignees have READ on the route instance
            assertNotNull(sessionUser3.getDocument(route.getDocument().getRef()));
            routing.endTask(sessionUser3, tasks.get(0), data, "trans3");
        }

        // end task and verify that route was done
        admin = new UserPrincipal("admin", null, false, true);

        try (CloseableCoreSession session = openSession(admin)) {
            route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            assertTrue(route.isDone());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeOne() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2"), transition("trans13", "node3"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        setTransitions(node2, transition("trans25", "node5"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node3, transition("trans34", "node4"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4", session);
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr2");
        setTransitions(node4, transition("trans45", "node5"));
        node4 = session.saveDocument(node4);

        DocumentModel node5 = createNode(routeDoc, "node5", session);
        node5.setPropertyValue(GraphNode.PROP_MERGE, "one");
        node5.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node5.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node5 = session.saveDocument(node5);

        DocumentRoute route = instantiateAndRun(session);

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
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node2", "true", "testchain_title1"),
                transition("trans2", "node2", "true", "testchain_descr1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        setTransitions(node2, transition("transloop", "node1", "false"));
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun(session);

        assertTrue(route.isDone());
        doc.refresh();
        assertEquals("title 1", doc.getTitle());
        assertEquals("descr 1", doc.getPropertyValue("dc:description"));
        assertEquals("rights 1", doc.getPropertyValue("dc:rights"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForkMergeWithTasksAndLoopTransitions() throws Exception {

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);

        // Create nodes
        DocumentModel startNode = createNode(routeDoc, "startNode", session);
        startNode.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(startNode, transition("transToParallel1", "parallelNode1"),
                transition("transToParallel2", "parallelNode2"));
        startNode = session.saveDocument(startNode);

        DocumentModel parallelNode1 = createNode(routeDoc, "parallelNode1", session);
        parallelNode1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users1 = { user1.getName() };
        parallelNode1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users1);
        setTransitions(parallelNode1, transition("transLoop", "parallelNode1", "NodeVariables[\"button\"] ==\"loop\""),
                transition("transToMerge", "mergeNode", "NodeVariables[\"button\"] ==\"toMerge\""));
        parallelNode1 = session.saveDocument(parallelNode1);

        DocumentModel parallelNode2 = createNode(routeDoc, "parallelNode2", session);
        parallelNode2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users2 = { user2.getName() };
        parallelNode2.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users2);
        setTransitions(
                parallelNode2,
                transition("transLoop", "parallelNode2", "NodeVariables[\"button\"] ==\"loop\""),
                transition("transToMerge", "mergeNode",
                        "NodeVariables[\"tasks\"].getNumberEndedWithStatus(\"toMerge\") ==1"));
        parallelNode2 = session.saveDocument(parallelNode2);

        DocumentModel mergeNode = createNode(routeDoc, "mergeNode", session);
        mergeNode.setPropertyValue(GraphNode.PROP_MERGE, "all");
        mergeNode.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        mergeNode.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users1);
        setTransitions(mergeNode, transition("transLoop", "startNode", "NodeVariables[\"button\"] ==\"loop\""),
                transition("transEnd", "endNode", "NodeVariables[\"button\"] ==\"end\""));
        mergeNode = session.saveDocument(mergeNode);

        DocumentModel endNode = createNode(routeDoc, "endNode", session);
        endNode.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        endNode = session.saveDocument(endNode);

        // Start route
        DocumentRoute route = instantiateAndRun(session);

        // Make user1 end his parallel task (1st time)
        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Map<String, Object> data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "toMerge");
        }

        // Make user2 end his parallel task (1st time)
        tasks = taskService.getTaskInstances(doc, user2, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            routing.endTask(sessionUser2, tasks.get(0), data, "toMerge");
        }

        // Make user1 end the merge task choosing the "loop" transition
        tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "loop");
        }

        // Make user1 end his parallel task (2nd time)
        tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "toMerge");
        }

        // Make user2 end his parallel task (2nd time)
        tasks = taskService.getTaskInstances(doc, user2, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            routing.endTask(sessionUser2, tasks.get(0), data, "toMerge");
        }

        // Make user1 end the merge task choosing the "end" transition
        tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "end");
        }

        // Check that route is done
        session.save();
        route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(route.isDone());
        GraphRoute graph = route.getDocument().getAdapter(GraphRoute.class);
        assertEquals(1, graph.getNode("parallelNode1").getEndedTasksInfo().size());
        assertEquals(1, graph.getNode("parallelNode1").getProcessedTasksInfo().size());
        assertEquals(1, graph.getNode("parallelNode2").getEndedTasksInfo().size());
        assertEquals(1, graph.getNode("parallelNode2").getProcessedTasksInfo().size());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTwoForkMergeWithLoopTransition() throws Exception {
        DocumentModel fork1 = createNode(routeDoc, "fork1", session);
        fork1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(fork1, transition("trans1", "fork2"), transition("trans2", "task3"));
        fork1 = session.saveDocument(fork1);

        DocumentModel fork2 = createNode(routeDoc, "fork2", session);
        setTransitions(fork2, transition("trans3", "task1"), transition("trans4", "task2"));
        fork2 = session.saveDocument(fork2);

        DocumentModel task1 = createNode(routeDoc, "task1", session);
        task1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(task1, transition("trans5", "merge1"));
        task1 = session.saveDocument(task1);

        DocumentModel task2 = createNode(routeDoc, "task2", session);
        task2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(task2, transition("trans6", "merge1"));
        task2 = session.saveDocument(task2);

        DocumentModel task3 = createNode(routeDoc, "task3", session);
        task3.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(task3, transition("trans7", "merge2"));
        task3 = session.saveDocument(task3);

        DocumentModel merge1 = createNode(routeDoc, "merge1", session);
        merge1.setPropertyValue(GraphNode.PROP_MERGE, "all");
        setTransitions(merge1, transition("transloop", "merge2"));
        merge1 = session.saveDocument(merge1);

        DocumentModel merge2 = createNode(routeDoc, "merge2", session);
        merge2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        setTransitions(merge2, transition("transloop", "fork1", "false"), transition("trans8", "stop"));
        merge2 = session.saveDocument(merge2);

        DocumentModel stop = createNode(routeDoc, "stop", session);
        stop.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        stop = session.saveDocument(stop);

        DocumentRoute route = instantiateAndRun(session);
        GraphRoute graph = (GraphRoute) route;

        // check that we found only one loop transition
        assertFalse(graph.getNode("fork1").getOutputTransitions().get(0).loop);
        assertFalse(graph.getNode("fork1").getOutputTransitions().get(1).loop);
        assertFalse(graph.getNode("fork2").getOutputTransitions().get(0).loop);
        assertFalse(graph.getNode("fork2").getOutputTransitions().get(1).loop);
        assertFalse(graph.getNode("task1").getOutputTransitions().get(0).loop);
        assertFalse(graph.getNode("task2").getOutputTransitions().get(0).loop);
        assertFalse(graph.getNode("task3").getOutputTransitions().get(0).loop);
        assertFalse(graph.getNode("merge1").getOutputTransitions().get(0).loop);
        assertTrue(graph.getNode("merge2").getOutputTransitions().get(0).loop);
        assertFalse(graph.getNode("merge2").getOutputTransitions().get(1).loop);

        assertEquals(State.SUSPENDED, graph.getNode("task1").getState());
        assertEquals(State.SUSPENDED, graph.getNode("task2").getState());
        assertEquals(State.SUSPENDED, graph.getNode("task3").getState());

        routing.resumeInstance(route.getDocument().getId(), "task1", null, null, session);

        // refresh state
        graph = (GraphRoute) session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertEquals(State.READY, graph.getNode("task1").getState());
        assertEquals(State.SUSPENDED, graph.getNode("task2").getState());
        assertEquals(State.SUSPENDED, graph.getNode("task3").getState());

        routing.resumeInstance(route.getDocument().getId(), "task2", null, null, session);

        // refresh state
        graph = (GraphRoute) session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertEquals(State.READY, graph.getNode("task1").getState());
        assertEquals(State.READY, graph.getNode("task2").getState());
        assertEquals(State.SUSPENDED, graph.getNode("task3").getState());

        routing.resumeInstance(route.getDocument().getId(), "task3", null, null, session);

        // refresh state
        graph = (GraphRoute) session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertEquals(State.READY, graph.getNode("task1").getState());
        assertEquals(State.READY, graph.getNode("task2").getState());
        assertEquals(State.READY, graph.getNode("task3").getState());

        assertTrue(((DocumentRoute) graph).isDone());
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    // see NXP-10538
    public void testForkWithLoopFromParallelToFork() throws Exception {

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);

        // Create nodes
        DocumentModel startNode = createNode(routeDoc, "startNode", session);
        startNode.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        startNode.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users1 = { user1.getName() };
        startNode.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users1);
        setTransitions(startNode,
                transition("transToParallel1", "parallelNode1", "NodeVariables[\"button\"] ==\"validate\""),
                transition("transToParallel2", "parallelNode2", "NodeVariables[\"button\"] ==\"validate\""));
        startNode = session.saveDocument(startNode);

        DocumentModel parallelNode1 = createNode(routeDoc, "parallelNode1", session);
        parallelNode1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        parallelNode1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users1);
        setTransitions(parallelNode1, transition("transLoop", "startNode", "NodeVariables[\"button\"] ==\"loop\""),
                transition("transToMerge", "mergeNode", "NodeVariables[\"button\"] ==\"toMerge\""));
        parallelNode1 = session.saveDocument(parallelNode1);

        DocumentModel parallelNode2 = createNode(routeDoc, "parallelNode2", session);
        parallelNode2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users2 = { user2.getName() };
        parallelNode2.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users2);
        setTransitions(parallelNode2,
                transition("transToMerge", "mergeNode", "NodeVariables[\"button\"] ==\"toMerge\""));
        parallelNode2 = session.saveDocument(parallelNode2);

        DocumentModel mergeNode = createNode(routeDoc, "mergeNode", session);
        mergeNode.setPropertyValue(GraphNode.PROP_MERGE, "all");
        startNode.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        mergeNode = session.saveDocument(mergeNode);

        // Start route
        DocumentRoute route = instantiateAndRun(session);

        // Make user1 validate the start task (1st time)
        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Map<String, Object> data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "validate");
        }

        // Make user1 loop to the start task
        tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "loop");
        }

        // Make user1 validate the start task (2nd time)
        tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "validate");
        }

        // Make user1 validate his parallel task
        tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "toMerge");
        }

        // Make user2 end his parallel task
        tasks = taskService.getTaskInstances(doc, user2, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            routing.endTask(sessionUser2, tasks.get(0), data, "toMerge");
        }

        // Check that route is done
        session.save();
        route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(route.isDone());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRouteWithTasks() throws Exception {

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(
                node1,
                transition("trans1", "node2",
                        "NodeVariables[\"button\"] == \"trans1\" && WorkflowFn.timeSinceWorkflowWasStarted()>=0",
                        "testchain_title1"));

        // task properties

        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");
        String[] users = { user1.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");

        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentModelList doneTasks = session.query("Select * from TaskDoc where ecm:currentLifeCycleState = 'ended'");
        assertEquals(0, doneTasks.size());

        DocumentRoute route = instantiateAndRun(session);

        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Map<String, Object> data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            // task assignees have READ on the route instance
            assertNotNull(sessionUser1.getDocument(route.getDocument().getRef()));
            Task task1 = tasks.get(0);
            assertEquals("MyTaskDoc", task1.getDocument().getType());
            List<DocumentModel> docs = routing.getWorkflowInputDocuments(sessionUser1, task1);
            assertEquals(doc.getId(), docs.get(0).getId());
            routing.endTask(sessionUser1, tasks.get(0), data, "trans1");
        }

        // end task and verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession session = openSession(admin)) {
            DocumentRef routeRef = route.getDocument().getRef();
            route = session.getDocument(routeRef).getAdapter(DocumentRoute.class);

            assertTrue(route.isDone());
            assertEquals("test", route.getDocument().getPropertyValue("fctroute1:globalVariable"));
            doneTasks = session.query("Select * from TaskDoc where ecm:currentLifeCycleState = 'ended'");
            assertEquals(1, doneTasks.size());
            routing.cleanupDoneAndCanceledRouteInstances(session.getRepositoryName(), 0);
            session.save();
            assertFalse(session.exists(routeRef));
            for (DocumentModel doneTask : doneTasks) {
                assertFalse(session.exists(doneTask.getRef()));
            }
        }
    }

    /**
     * @since 9.3
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSetVarOnTransitionAndCheckVarWithInputChain() throws Exception {

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2",
                        "true",
                        "test_setGlobalvariable"));

        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");
        String[] users = { user1.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_globalVarAssert");
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(node2, transition("trans23", "node3", "true"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node3 = session.saveDocument(node3);

        DocumentModelList doneTasks = session.query("Select * from TaskDoc where ecm:currentLifeCycleState = 'ended'");
        assertEquals(0, doneTasks.size());

        instantiateAndRun(session);

        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertEquals(1, tasks.size());

        Map<String, Object> data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            routing.endTask(sessionUser1, tasks.get(0), data, "trans1");
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEvaluateTaskAssigneesFromVariable() throws Exception {
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");
        List<String> assignees = Arrays.asList(user1.getName(), user2.getName());

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc.setPropertyValue("fctroute1:myassignees", (Serializable) assignees);
        routeDoc = session.saveDocument(routeDoc);

        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        setTransitions(node1, transition("trans1", "node2", "true", "testchain_title1"));
        // add a workflow variables with name "myassignees"
        node1.setPropertyValue("rnode:taskAssigneesExpr", "WorkflowVariables[\"myassignees\"]");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun(session);

        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        Task ts = tasks.get(0);
        assertEquals(2, ts.getActors().size());

        // check permissions set during task
        assertTrue(session.hasPermission(user1, doc.getRef(), "Write"));
        assertTrue(session.hasPermission(user2, doc.getRef(), "Write"));

        // end task

        Map<String, Object> data = new HashMap<String, Object>();
        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            routing.endTask(sessionUser2, tasks.get(0), data, "trans1");
        }

        // verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession session = openSession(admin)) {
            route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            assertTrue(route.isDone());

            // permissions are reset
            assertFalse(session.hasPermission(user1, doc.getRef(), "Write"));
            assertFalse(session.hasPermission(user2, doc.getRef(), "Write"));
        }
    }

    /**
     * Check that when running as non-Administrator the assignees are set correctly.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testComputedTaskAssignees() throws Exception {
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");

        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        setTransitions(node1, transition("trans1", "node2", "true", "testchain_title1"));
        // add a workflow node assignees expression
        node1.setPropertyValue("rnode:taskAssigneesExpr", "\"myuser1\"");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        session.save();

        // another session as user2
        List<Task> tasks;
        DocumentRoute route;
        try (CloseableCoreSession session2 = openSession(user2)) {
            route = instantiateAndRun(session2);

            tasks = taskService.getTaskInstances(doc, user1, session2);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            Task ts = tasks.get(0);
            assertEquals(1, ts.getActors().size());
            session2.save(); // flush invalidations
        }

        // process task as user1
        try (CloseableCoreSession session1 = openSession(user1)) {
            routing.endTask(session1, tasks.get(0), new HashMap<String, Object>(), "trans1");
        }

        // verify that route was done
        session.save(); // process invalidations
        route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(route.isDone());
        assertFalse(session.hasPermission(user1, doc.getRef(), "Write"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDynamicallyComputeDueDate() throws PropertyException {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        setTransitions(node1, transition("trans1", "node2", "true", "testchain_title1"));

        node1.setPropertyValue("rnode:taskAssigneesExpr", "\"Administrator\"");
        node1.setPropertyValue("rnode:taskDueDateExpr", "CurrentDate.days(1)");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        session.save();
        instantiateAndRun(session);
        session.save();
        List<Task> tasks = taskService.getTaskInstances(doc, session.getPrincipal(), session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        Task ts = tasks.get(0);
        Calendar currentDate = Calendar.getInstance();
        Calendar taskDueDate = Calendar.getInstance();
        taskDueDate.setTime(ts.getDueDate());
        int tomorrow = currentDate.get(Calendar.DAY_OF_YEAR) + 1;
        int due = taskDueDate.get(Calendar.DAY_OF_YEAR);
        if (due != 1) {
            assertEquals(tomorrow, due);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWorkflowInitiatorAndTaskActor() throws Exception {
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);

        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans1", "node2", "true", "test_setGlobalVariableToWorkflowInitiator"));
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, new String[] { user2.getName() });
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);
        session.save();

        // start workflow as user1

        DocumentRef routeDocRef;
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            DocumentRoute route = instantiateAndRun(sessionUser1);
            routeDocRef = route.getDocument().getRef();
        }

        // check user2 tasks

        List<Task> tasks = taskService.getTaskInstances(doc, user2, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // continue task as user2

        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            // task assignees have READ on the route instance
            assertNotNull(sessionUser2.getDocument(routeDocRef));
            Task task = tasks.get(0);
            List<DocumentModel> docs = routing.getWorkflowInputDocuments(sessionUser2, task);
            assertEquals(doc.getId(), docs.get(0).getId());
            Map<String, Object> data = new HashMap<String, Object>();
            routing.endTask(sessionUser2, tasks.get(0), data, "trans1");
        }

        // verify things
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession sessionAdmin = openSession(admin)) {
            DocumentRoute route = sessionAdmin.getDocument(routeDocRef).getAdapter(DocumentRoute.class);
            assertTrue(route.isDone());
            Serializable v = route.getDocument().getPropertyValue("fctroute1:globalVariable");
            assertEquals("myuser1", v);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRestartWorkflowOperation() throws Exception {
        assertEquals("file", doc.getTitle());
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2", "true", "testchain_title1"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(node2, transition("trans23", "node3"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node3 = session.saveDocument(node3);

        DocumentRoute route = instantiateAndRun(session);
        assertFalse(route.isDone());

        List<Task> tasks = taskService.getTaskInstances(doc, (NuxeoPrincipal) null, session);
        assertEquals(1, tasks.size());

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("testChain");
        chain.add(BulkRestartWorkflow.ID).set("workflowId", routeDoc.getTitle());
        automationService.run(ctx, chain);
        // process invalidations from automation context
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // query for all the workflows
        DocumentModelList workflows = session.query(String.format(
                "Select * from DocumentRoute where docri:participatingDocuments/* IN ('%s') and ecm:currentLifeCycleState = 'running'",
                doc.getId()));
        assertEquals(1, workflows.size());
        String restartedWorkflowId = workflows.get(0).getId();
        assertFalse(restartedWorkflowId.equals(route.getDocument().getId()));

        chain.add(BulkRestartWorkflow.ID).set("workflowId", routeDoc.getTitle()).set("nodeId", "node2");
        automationService.run(ctx, chain);
        // process invalidations from automation context
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // query for all the workflows
        workflows = session.query(String.format(
                "Select * from DocumentRoute where docri:participatingDocuments/* IN ('%s') and ecm:currentLifeCycleState = 'running'",
                doc.getId()));
        assertEquals(1, workflows.size());
        assertFalse(restartedWorkflowId.equals(workflows.get(0).getId()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMergeOneWhenHavinOpenedTasks() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2"), transition("trans13", "node3"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, "true");
        setTransitions(node2, transition("trans25", "node5"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node3, transition("trans34", "node4"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4", session);
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr2");
        setTransitions(node4, transition("trans45", "node5"));
        node4.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        node4.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, new String[] { user1.getName() });
        node4 = session.saveDocument(node4);

        DocumentModel node5 = createNode(routeDoc, "node5", session);
        node5.setPropertyValue(GraphNode.PROP_MERGE, "one");
        node5.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node5.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node5 = session.saveDocument(node5);

        DocumentRoute route = instantiateAndRun(session);
        session.save(); // process invalidations
        // verify that there are 2 open tasks
        List<Task> tasks = taskService.getAllTaskInstances(route.getDocument().getId(), session);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        tasks = taskService.getAllTaskInstances(route.getDocument().getId(), user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        // process one of the tasks
        try (CloseableCoreSession session1 = openSession(user1)) {
            routing.endTask(session1, tasks.get(0), new HashMap<String, Object>(), null);
        }

        // verify that route was done
        session.save(); // process invalidations
        route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        // verify that the merge one canceled the other tasks
        tasks = taskService.getAllTaskInstances(route.getDocument().getId(), session);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
        DocumentModelList cancelledTasks = session.query("Select * from TaskDoc where ecm:currentLifeCycleState = 'cancelled'");
        assertEquals(1, cancelledTasks.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testForceResumeOnMerge() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2"), transition("trans13", "node3"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_title1");
        node2.setPropertyValue(GraphNode.PROP_HAS_TASK, "true");
        setTransitions(node2, transition("trans25", "node5"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr1");
        setTransitions(node3, transition("trans34", "node4"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4", session);
        node4.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_descr2");
        setTransitions(node4, transition("trans45", "node5"));
        node4 = session.saveDocument(node4);

        DocumentModel node5 = createNode(routeDoc, "node5", session);
        node5.setPropertyValue(GraphNode.PROP_MERGE, "all");
        node5.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "testchain_rights1");
        node5.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node5 = session.saveDocument(node5);

        DocumentRoute route = instantiateAndRun(session);
        // force resume on normal node, shouldn't change anything
        Map<String, Object> data = Collections.<String, Object> singletonMap(WORKFLOW_FORCE_RESUME, Boolean.TRUE);
        routing.resumeInstance(route.getDocument().getId(), "node2", data, null, session);
        session.save();
        assertEquals("done", session.getDocument(route.getDocument().getRef()).getCurrentLifeCycleState());

        // force resume on merge on Waiting, but it shouldn't work
        // since the type of merge is all
        routeDoc = session.getDocument(routeDoc.getRef());
        route = instantiateAndRun(session);
        GraphRoute graph = (GraphRoute) route;
        GraphNode nodeMerge = graph.getNode("node5");
        assertTrue(State.WAITING.equals(nodeMerge.getState()));

        data = Collections.<String, Object> singletonMap(WORKFLOW_FORCE_RESUME, Boolean.TRUE);
        routing.resumeInstance(route.getDocument().getId(), "node5", data, null, session);
        session.save();

        // verify that the route is still running
        assertEquals("running", session.getDocument(route.getDocument().getRef()).getCurrentLifeCycleState());

        // change merge type on the route instance and force resume again
        nodeMerge.getDocument().setPropertyValue(GraphNode.PROP_MERGE, "one");
        session.saveDocument(nodeMerge.getDocument());
        routing.resumeInstance(route.getDocument().getId(), "node5", data, null, session);
        session.save();

        assertEquals("done", session.getDocument(route.getDocument().getRef()).getCurrentLifeCycleState());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRouteWithExclusiveNode() throws Exception {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_EXECUTE_ONLY_FIRST_TRANSITION, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2", "true", "testchain_title1"),
                transition("trans13", "node3", "true", "testchain_title2"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        setTransitions(node2, transition("trans24", "node4", "true"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        setTransitions(node3, transition("trans34", "node4", "true"));
        node3 = session.saveDocument(node3);

        DocumentModel node4 = createNode(routeDoc, "node4", session);
        node4.setPropertyValue(GraphNode.PROP_MERGE, "one");
        node4.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node4 = session.saveDocument(node4);

        DocumentRoute route = instantiateAndRun(session);
        assertTrue(route.isDone());

        session.save();

        // check that trans12 was executed and not trans13
        DocumentModel docR = session.getDocument(doc.getRef());
        assertEquals("title 1", docR.getTitle());
    }

    @SuppressWarnings("unchecked")
    protected void createWorkflowWithSubRoute(String subRouteModelId) throws PropertyException {
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1, transition("trans12", "node2"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_SUB_ROUTE_MODEL_EXPR, subRouteModelId);
        setTransitions(node2, transition("trans23", "node3"));
        setSubRouteVariables(node2, keyvalue("stringfield", "foo"), keyvalue("globalVariable", "expr:bar@{4+3}baz"));
        node2 = session.saveDocument(node2);

        DocumentModel node3 = createNode(routeDoc, "node3", session);
        node3.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node3 = session.saveDocument(node3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSubRouteNotSuspending() throws Exception {

        // create the sub-route

        DocumentModel subRouteDoc = createRoute("subroute", session);
        subRouteDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        subRouteDoc = session.saveDocument(subRouteDoc);

        DocumentModel subNode1 = createNode(subRouteDoc, "subnode1", session);
        subNode1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(subNode1, transition("trans12", "subnode2", "true", "testchain_title_subroute"));
        subNode1 = session.saveDocument(subNode1);

        DocumentModel subNode2 = createNode(subRouteDoc, "subnode2", session);
        subNode2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        subNode2 = session.saveDocument(subNode2);

        validate(subRouteDoc, session);

        // create the base workflow

        createWorkflowWithSubRoute(subRouteDoc.getName());

        // start the main workflow
        DocumentRoute route = instantiateAndRun(session);

        // check that it's finished immediately
        assertTrue(route.isDone());
        // check that transition got the correct variables
        doc.refresh();
        assertEquals(route.getDocument().getId() + " node2 foo bar7baz", doc.getTitle());
    }

    @SuppressWarnings("unchecked")
    public void createRouteAndSuspendingSubRoute() throws Exception {

        // create the sub-route

        DocumentModel subRouteDoc = createRoute("subroute", session);
        subRouteDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        subRouteDoc = session.saveDocument(subRouteDoc);

        DocumentModel subNode1 = createNode(subRouteDoc, "subnode1", session);
        subNode1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(subNode1, transition("trans12", "subnode2", "true", "testchain_title_subroute"));
        subNode1 = session.saveDocument(subNode1);

        DocumentModel subNode2 = createNode(subRouteDoc, "subnode2", session);
        subNode2.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        setTransitions(subNode2, transition("trans23", "subnode3"));
        subNode2 = session.saveDocument(subNode2);

        DocumentModel subNode3 = createNode(subRouteDoc, "subnode3", session);
        subNode3.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        subNode3 = session.saveDocument(subNode3);

        validate(subRouteDoc, session);

        // create the base workflow

        createWorkflowWithSubRoute(subRouteDoc.getName());
    }

    @Test
    public void testSubRouteSuspending() throws Exception {
        createRouteAndSuspendingSubRoute();

        // start the main workflow
        DocumentRoute route = instantiateAndRun(session);

        // check that it's suspended on node 2
        assertFalse(route.isDone());
        DocumentModel n2 = session.getChild(route.getDocument().getRef(), "node2");
        assertNotNull(n2);
        assertEquals(State.SUSPENDED.getLifeCycleState(), n2.getCurrentLifeCycleState());

        // check that transition got the correct variables
        doc.refresh();
        assertEquals(route.getDocument().getId() + " node2 foo bar7baz", doc.getTitle());

        // find the sub-route instance
        String subid = (String) n2.getPropertyValue(GraphNode.PROP_SUB_ROUTE_INSTANCE_ID);
        assertNotNull(subid);
        DocumentModel subrdoc = session.getDocument(new IdRef(subid));
        DocumentRoute subr = subrdoc.getAdapter(DocumentRoute.class);
        assertFalse(subr.isDone());

        // resume the sub-route node
        routing.resumeInstance(subid, "subnode2", null, null, session);
        // check sub-route done
        subrdoc.refresh();
        assertTrue(subr.isDone());
        // check main workflow also resumed and done
        route.getDocument().refresh();
        assertTrue(route.isDone());
    }

    @Test
    public void testSubRouteCancel() throws Exception {
        createRouteAndSuspendingSubRoute();

        // start the main workflow
        DocumentRoute route = instantiateAndRun(session);

        // check that it's suspended on node 2
        assertFalse(route.isDone());
        DocumentModel n2 = session.getChild(route.getDocument().getRef(), "node2");
        assertNotNull(n2);
        assertEquals(State.SUSPENDED.getLifeCycleState(), n2.getCurrentLifeCycleState());

        // cancel the main workflow
        route.cancel(session);
        route.getDocument().refresh();
        assertTrue(route.isCanceled());

        // find the sub-route instance
        String subid = (String) n2.getPropertyValue(GraphNode.PROP_SUB_ROUTE_INSTANCE_ID);
        assertNotNull(subid);
        DocumentModel subrdoc = session.getDocument(new IdRef(subid));
        DocumentRoute subr = subrdoc.getAdapter(DocumentRoute.class);

        // check hat it's canceled as well
        assertTrue(subr.isCanceled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelTasksWhenWorkflowDone() throws Exception {
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node12", "NodeVariables[\"button\"] == \"trans1\"", "testchain_title1"),
                transition("trans1", "node22", "NodeVariables[\"button\"] == \"trans1\"", "testchain_title1"),
                transition("trans2", "node2", "true", ""));

        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        String[] users = { "Administrator" };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        node1 = session.saveDocument(node1);

        DocumentModel node12 = createNode(routeDoc, "node12", session);
        setTransitions(node1,
                transition("trans12", "node2", "NodeVariables[\"button\"] == \"trans12\"", "testchain_title1"));

        node12.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node12 = session.saveDocument(node12);

        DocumentModel node22 = createNode(routeDoc, "node22", session);
        setTransitions(node1,
                transition("trans22", "node2", "NodeVariables[\"button\"] == \"trans22\"", "testchain_title1"));

        node22.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node22 = session.saveDocument(node22);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);
        DocumentRoute route = instantiateAndRun(session);
        session.save();

        List<Task> tasks = taskService.getAllTaskInstances(route.getDocument().getId(), session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        routing.endTask(session, tasks.get(0), new HashMap<String, Object>(), "trans1");
        route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(route.isDone());
        session.save();

        tasks = taskService.getAllTaskInstances(route.getDocument().getId(), session);
        assertEquals(0, tasks.size());
        DocumentModelList cancelledTasks = session.query("Select * from TaskDoc where ecm:currentLifeCycleState = 'cancelled'");
        assertEquals(2, cancelledTasks.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRouteWithMultipleTasks() throws Exception {
        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);
        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);
        NuxeoPrincipal user3 = userManager.getPrincipal("myuser3");
        assertNotNull(user3);

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(
                node1,
                transition("trans1", "node2", "NodeVariables[\"tasks\"].getNumberEndedWithStatus(\"trans1\") ==1",
                        "testchain_title1"));

        // task properties
        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node1.setPropertyValue(GraphNode.PROP_HAS_MULTIPLE_TASKS, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");

        // pass 3 assignees to create 3 tasks at this node
        String[] users = { user1.getName(), user2.getName(), user3.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");

        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        // run workflow
        DocumentRoute route = instantiateAndRun(session);
        GraphRoute graph = route.getDocument().getAdapter(GraphRoute.class);

        // verify that there are 3 tasks created from this node
        List<Task> tasks = taskService.getAllTaskInstances(route.getDocument().getId(), "node1", session);
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        assertEquals(3, graph.getNode("node1").getTasksInfo().size());

        // end first task as user 1
        Map<String, Object> data = new HashMap<String, Object>();
        Task task1;
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            assertNotNull(sessionUser1.getDocument(route.getDocument().getRef()));
            tasks = taskService.getTaskInstances(doc, user1, sessionUser1);
            assertEquals(1, tasks.size());
            task1 = tasks.get(0);
            assertEquals("MyTaskDoc", task1.getDocument().getType());
            List<DocumentModel> docs = routing.getWorkflowInputDocuments(sessionUser1, task1);
            assertEquals(doc.getId(), docs.get(0).getId());
            routing.endTask(sessionUser1, tasks.get(0), data, "faketrans1");
        }

        // verify that route was not done, as there are still 2
        // open tasks
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession session = openSession(admin)) {
            route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            graph = route.getDocument().getAdapter(GraphRoute.class);
            assertFalse(route.isDone());
            assertEquals(1, graph.getNode("node1").getEndedTasksInfo().size());
            assertEquals(1, graph.getNode("node1").getProcessedTasksInfo().size());
        }

        // end task2 as user 2
        data = new HashMap<String, Object>();
        data.put("comment", "testcomment");
        Task task2;
        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            assertNotNull(sessionUser2.getDocument(route.getDocument().getRef()));

            tasks = taskService.getTaskInstances(doc, user2, sessionUser2);
            assertEquals(1, tasks.size());
            task2 = tasks.get(0);
            assertEquals("MyTaskDoc", task2.getDocument().getType());
            List<DocumentModel> docs = routing.getWorkflowInputDocuments(sessionUser2, task2);
            assertEquals(doc.getId(), docs.get(0).getId());
            routing.endTask(sessionUser2, tasks.get(0), data, "trans1");
        }

        // verify that route is not done yet, 2 tasks were done but there is
        // still one open
        try (CloseableCoreSession session = openSession(admin)) {
            route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            graph = route.getDocument().getAdapter(GraphRoute.class);
            assertFalse(route.isDone());
            assertEquals(2, graph.getNode("node1").getEndedTasksInfo().size());
        }

        // cancel the last open task, resume the route and verify that route is
        // done now
        tasks = taskService.getTaskInstances(doc, user3, session);
        assertEquals(1, tasks.size());
        Task task3 = tasks.get(0);

        routing.cancelTask(session, task3.getId());
        routing.resumeInstance(route.getDocument().getId(), "node1", null, null, session);

        route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        graph = route.getDocument().getAdapter(GraphRoute.class);

        assertTrue(route.isDone());
        assertEquals(3, graph.getNode("node1").getEndedTasksInfo().size());
        assertEquals(2, graph.getNode("node1").getProcessedTasksInfo().size());

        // also verify that the actor and the comment where updated on the node
        // when the tasks were completed or canceled
        GraphNode graphNode1 = graph.getNode("node1");
        List<GraphNode.TaskInfo> tasksInfo = graphNode1.getTasksInfo();
        assertEquals(tasksInfo.size(), 3);
        int task1Index = 0;
        int task2Index = 1;
        int task3Index = 2;
        for (TaskInfo taskInfo : tasksInfo) {
            if (taskInfo.getTaskDocId().equals(task1.getId())) {
                task1Index = tasksInfo.indexOf(taskInfo);
            }
            if (taskInfo.getTaskDocId().equals(task2.getId())) {
                task2Index = tasksInfo.indexOf(taskInfo);
            }
            if (taskInfo.getTaskDocId().equals(task3.getId())) {
                task3Index = tasksInfo.indexOf(taskInfo);
            }
        }

        assertEquals("myuser1", tasksInfo.get(task1Index).getActor());
        assertEquals("myuser2", tasksInfo.get(task2Index).getActor());
        // task3 was canceled as an admin
        assertEquals("Administrator", tasksInfo.get(task3Index).getActor());

        assertEquals("faketrans1", tasksInfo.get(task1Index).getStatus());
        assertEquals("trans1", tasksInfo.get(task2Index).getStatus());
        assertEquals(null, tasksInfo.get(task3Index).getStatus());

        assertEquals("testcomment", tasksInfo.get(task2Index).getComment());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTasksReassignment() throws Exception {

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "NodeVariables[\"button\"] == \"trans1\"", "testchain_title1"));

        // task properties

        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_ALLOW_TASK_REASSIGNMENT, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");
        String[] users = { user1.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");

        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun(session);
        session.save();
        // check that the user1 has one task assigned
        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        Task task1 = tasks.get(0);

        Map<String, Object> data = new HashMap<String, Object>();
        // open session as user1 to reassign the task
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            assertEquals("MyTaskDoc", task1.getDocument().getType());
            List<DocumentModel> docs = routing.getWorkflowInputDocuments(sessionUser1, task1);
            assertEquals(doc.getId(), docs.get(0).getId());
            // user1 has Write on the document following the workflow
            assertTrue(sessionUser1.hasPermission(docs.get(0).getRef(), "Write"));
            // reassign task to user2

            List<String> newActors = new ArrayList<String>();
            newActors.add("myuser2");
            routing.reassignTask(sessionUser1, task1.getId(), newActors, "Reassigned");
            sessionUser1.save();

            // check that user1 doesn't have Write permission any more on
            // documents following the workflow
            docs = routing.getWorkflowInputDocuments(sessionUser1, task1);
            assertFalse(sessionUser1.hasPermission(docs.get(0).getRef(), "Write"));
            // check that user1 can no longer access the task
            assertFalse(sessionUser1.hasPermission(task1.getDocument().getRef(), "Read"));
        }

        // open session as User2
        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            // check he has a task assigned
            tasks = taskService.getTaskInstances(doc, user2, sessionUser2);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            Task task2 = tasks.get(0);
            assertEquals(1, task2.getActors().size());
            assertEquals("myuser2", task2.getActors().get(0));

            List<DocumentModel> docs = routing.getWorkflowInputDocuments(sessionUser2, task1);
            // user2 has now Write on the document following the workflow
            assertTrue(sessionUser2.hasPermission(docs.get(0).getRef(), "Write"));
            routing.endTask(sessionUser2, tasks.get(0), data, "trans1");
        }

        // end task and verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession adminSession = openSession(admin)) {
            route = adminSession.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            assertTrue(route.isDone());
            assertEquals("test", route.getDocument().getPropertyValue("fctroute1:globalVariable"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTasksDelegation() throws Exception {

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        NuxeoPrincipal user2 = userManager.getPrincipal("myuser2");
        assertNotNull(user2);

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "NodeVariables[\"button\"] == \"trans1\"", "testchain_title1"));

        // task properties

        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_rights1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");
        String[] users = { user1.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");

        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun(session);
        session.save();
        // check that the user1 has one task assigned
        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        Task task1 = tasks.get(0);

        Map<String, Object> data = new HashMap<String, Object>();
        // open session as user1 to delegate the task
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            assertEquals("MyTaskDoc", task1.getDocument().getType());
            List<DocumentModel> docs = routing.getWorkflowInputDocuments(sessionUser1, task1);
            assertEquals(doc.getId(), docs.get(0).getId());
            // user1 has Write on the document following the workflow
            assertTrue(sessionUser1.hasPermission(docs.get(0).getRef(), "Write"));
            // delegate task to user2

            List<String> newActors = new ArrayList<String>();
            newActors.add("myuser2");
            routing.delegateTask(sessionUser1, task1.getId(), newActors, "Delegated");
            sessionUser1.save();

            // check that user1 still have Write permission on documents
            // following the workflow
            docs = routing.getWorkflowInputDocuments(sessionUser1, task1);
            assertTrue(sessionUser1.hasPermission(docs.get(0).getRef(), "Write"));
            // check that user1 still can access the task
            assertTrue(sessionUser1.hasPermission(task1.getDocument().getRef(), "Read"));
        }

        List<DocumentModel> docs;
        // open session as User2
        try (CloseableCoreSession sessionUser2 = openSession(user2)) {
            // check the user doesn't have a task assigned
            tasks = taskService.getTaskInstances(doc, user2, sessionUser2);
            assertNotNull(tasks);
            assertEquals(0, tasks.size());

            // check that the user can get the task as a delegate
            tasks = taskService.getTaskInstances(doc, Arrays.asList(new String[] { "myuser2" }), true, sessionUser2);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());

            Task task2 = tasks.get(0);
            assertEquals(1, task2.getActors().size());
            assertEquals("myuser1", task2.getActors().get(0));

            assertEquals(1, task2.getDelegatedActors().size());
            assertEquals("myuser2", task2.getDelegatedActors().get(0));

            docs = routing.getWorkflowInputDocuments(sessionUser2, task2);
            // user2 has now Write on the document following the workflow
            assertTrue(sessionUser2.hasPermission(docs.get(0).getRef(), "Write"));
            routing.endTask(sessionUser2, task2, data, "trans1");
        }

        // end task and verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession adminSession = openSession(admin)) {
            route = adminSession.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            assertTrue(route.isDone());
            assertEquals("test", route.getDocument().getPropertyValue("fctroute1:globalVariable"));
            // user1 and user 2 have no longer Write on the document following
            // the
            // workflow
            assertFalse(adminSession.hasPermission(user2, docs.get(0).getRef(), "Write"));
            assertFalse(adminSession.hasPermission(user1, docs.get(0).getRef(), "Write"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWorkflowOnMultipleDocuments() throws Exception {
        // a doc, associated to the route
        DocumentModel doc2 = session.createDocumentModel("/", "file", "File");
        doc2.setPropertyValue("dc:title", "file");
        doc2 = session.createDocument(doc2);
        List<String> docIds = new ArrayList<String>();
        docIds.add(doc.getId());
        docIds.add(doc2.getId());

        NuxeoPrincipal user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);

        routeDoc.setPropertyValue(GraphRoute.PROP_VARIABLES_FACET, "FacetRoute1");
        routeDoc.addFacet("FacetRoute1");
        routeDoc = session.saveDocument(routeDoc);
        DocumentModel node1 = createNode(routeDoc, "node1", session);
        node1.setPropertyValue(GraphNode.PROP_VARIABLES_FACET, "FacetNode1");
        node1.setPropertyValue(GraphNode.PROP_START, Boolean.TRUE);
        setTransitions(node1,
                transition("trans1", "node2", "NodeVariables[\"button\"] == \"trans1\"", "testchain_title1"));

        // task properties

        node1.setPropertyValue(GraphNode.PROP_OUTPUT_CHAIN, "testchain_title1");
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES_PERMISSION, "Write");
        node1.setPropertyValue(GraphNode.PROP_INPUT_CHAIN, "test_setGlobalvariable");
        node1.setPropertyValue(GraphNode.PROP_HAS_TASK, Boolean.TRUE);
        node1.setPropertyValue(GraphNode.PROP_TASK_DOC_TYPE, "MyTaskDoc");
        String[] users = { user1.getName() };
        node1.setPropertyValue(GraphNode.PROP_TASK_ASSIGNEES, users);
        setButtons(node1, button("btn1", "label-btn1", "filterrr"));
        node1 = session.saveDocument(node1);

        DocumentModel node2 = createNode(routeDoc, "node2", session);
        node2.setPropertyValue(GraphNode.PROP_MERGE, "all");

        node2.setPropertyValue(GraphNode.PROP_STOP, Boolean.TRUE);
        node2 = session.saveDocument(node2);

        DocumentRoute route = instantiateAndRun(session, docIds, null);

        // test that we can fetch the task instances on both documents
        List<Task> tasks = taskService.getTaskInstances(doc, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        tasks = taskService.getTaskInstances(doc2, user1, session);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        List<DocumentModel> docs;
        try (CloseableCoreSession sessionUser1 = openSession(user1)) {
            Task task1 = tasks.get(0);
            assertEquals("MyTaskDoc", task1.getDocument().getType());
            docs = routing.getWorkflowInputDocuments(sessionUser1, task1);

            assertEquals(2, docs.size());
            // task assignees have WRITE on both documents following the
            // workflow
            assertTrue(sessionUser1.hasPermission(docs.get(0).getRef(), "Write"));
            assertTrue(sessionUser1.hasPermission(docs.get(1).getRef(), "Write"));

            Map<String, Object> data = new HashMap<String, Object>();
            routing.endTask(sessionUser1, tasks.get(0), data, "trans1");
        }

        // end task and verify that route was done
        NuxeoPrincipal admin = new UserPrincipal("admin", null, false, true);
        try (CloseableCoreSession adminSession = openSession(admin)) {
            route = adminSession.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
            assertTrue(route.isDone());
            assertEquals("test", route.getDocument().getPropertyValue("fctroute1:globalVariable"));

            // verify that the optput chain was executed on both docs
            doc = adminSession.getDocument(doc.getRef());
            assertEquals("title 1", doc.getTitle());

            doc2 = adminSession.getDocument(doc2.getRef());
            assertEquals("title 1", doc2.getTitle());

            // task assignees don't have WRITE any more on both documents
            // following
            // the workflow
            // workflow
            assertFalse(adminSession.hasPermission(user1, docs.get(0).getRef(), "Write"));
            assertFalse(adminSession.hasPermission(user1, docs.get(1).getRef(), "Write"));
        }
    }

}
