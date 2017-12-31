/*
 * (C) Copyright 2009-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

// sets step chainId to setWaiting instead of setDone
@Deploy("org.nuxeo.ecm.platform.routing.core.test")
public class TestDocumentRoutingServiceWait extends DocumentRoutingTestCase {

    protected File tmp;

    @After
    public void tearDown() throws Exception {
        if (tmp != null) {
            tmp.delete();
        }
    }

    @Test
    public void testAddSameNamedStepToRunningRoute() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        DocumentModelList childrens = session.getChildren(route.getDocument().getRef());
        String firstStepId = childrens.get(0).getId();
        String secondStepId = childrens.get(1).getId();
        String folderId = childrens.get(2).getId();
        service.lockDocumentRoute(route, session);
        DocumentModel newStep = session.createDocumentModel(route.getDocument().getPathAsString(), "step1",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        service.addRouteElementToRoute(route.getDocument().getRef(), null,
                newStep.getAdapter(DocumentRouteElement.class), session);
        session.save();
        assertNotNull(route);
        childrens = session.getChildren(route.getDocument().getRef());
        assertEquals(4, childrens.size());
        assertEquals(firstStepId, childrens.get(0).getId());
        assertEquals(secondStepId, childrens.get(1).getId());
        assertEquals(folderId, childrens.get(2).getId());
        // the new step's name should be step1.xxxxxx
        assertTrue(!"step1".equals(childrens.get(3).getName()));
    }

    @Test
    public void testAddStepToRunningRoute() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        service.lockDocumentRoute(route, session);
        service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        route = service.createNewInstance(route, new ArrayList<String>(), session, true);
        session.save();
        assertNotNull(route);
        DocumentModel step = session.createDocumentModel(route.getDocument().getPathAsString(), "step31bis",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, "step31bis");
        DocumentModelList stepFolders = session.query("Select * From Document WHERE dc:title = 'parallel1' and ecm:currentLifeCycleState = 'ready'");
        assertEquals(1, stepFolders.size());
        DocumentModel parallel1 = stepFolders.get(0);
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), "step32", step.getAdapter(DocumentRouteElement.class),
                session);
        service.unlockDocumentRoute(route, session);
        assertNotNull(route);
        assertFalse(route.isDone());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(3, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(2, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        assertFalse(route.isDone());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        assertEquals(0, waiting.size());
        route = session.getDocument(route.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(route.isDone());
    }

    @Test
    public void testDocumentRouteWithWaitState() throws Exception {
        CounterListener.resetCouner();
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRoute(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated", route.getDocument().getCurrentLifeCycleState());
        assertEquals("validated", session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel, Collections.singletonList(doc1.getId()),
                session, true);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(1, service.getDocumentRoutesForAttachedDocument(session, doc1.getId()).size());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(2, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        assertFalse(routeInstance.isDone());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        assertEquals(0, waiting.size());
        routeInstance = session.getDocument(routeInstance.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(routeInstance.isDone());
        assertEquals((6/* route */ + 4 /* number of steps */ * 3 /*
                                                                  * number of event per waiting step
                                                                  */ + 2 /* workflow started + first task audit */),
                CounterListener.getCounter());
    }

    @Test
    public void testCancelRoute() throws Exception {
        CounterListener.resetCouner();
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRoute(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated", route.getDocument().getCurrentLifeCycleState());
        assertEquals("validated", session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel, Collections.singletonList(doc1.getId()),
                session, true);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(1, service.getDocumentRoutesForAttachedDocument(session, doc1.getId()).size());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        routeInstance.cancel(session);
        assertTrue(routeInstance.isCanceled());
        DocumentModelList children = session.getChildren(routeInstance.getDocument().getRef());
        while (true) {
            for (DocumentModel doc : children) {
                assertTrue(doc.getCurrentLifeCycleState().equals("canceled"));
            }
            children = new DocumentModelListImpl();
            for (DocumentModel doc : children) {
                children.addAll(session.getChildren(doc.getRef()));
            }
            if (children.isEmpty()) {
                break;
            }
        }
    }

    @Test
    public void testDocumentRouteWithStepBack() throws Exception {
        CounterListener.resetCouner();
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRoute(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated", route.getDocument().getCurrentLifeCycleState());
        assertEquals("validated", session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel, Collections.singletonList(doc1.getId()),
                session, true);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(1, service.getDocumentRoutesForAttachedDocument(session, doc1.getId()).size());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        String firstStepId = waiting.get(0);
        // run first step
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        // undo second step
        String secondStepId = waiting.get(0);
        DocumentRouteStep step = WaitingStepRuntimePersister.getStep(secondStepId, session);
        assertTrue(step.canUndoStep(session));
        step = step.undo(session);
        assertTrue(step.isReady());
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        // restart route
        routeInstance.run(session);
        // undo second and first step
        DocumentRouteStep firstStep = WaitingStepRuntimePersister.getStep(firstStepId, session);
        DocumentRouteStep secondStep = WaitingStepRuntimePersister.getStep(secondStepId, session);
        secondStep = secondStep.undo(session);
        firstStep = firstStep.undo(session);
        assertTrue(secondStep.isReady());
        assertTrue(firstStep.isReady());
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        // restart route
        routeInstance.run(session);
        // run first step
        WaitingStepRuntimePersister.resumeStep(firstStepId, session);
        // run second step
        WaitingStepRuntimePersister.resumeStep(secondStepId, session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(2, waiting.size());
        // run third (parallel) step
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        assertFalse(routeInstance.isDone());
        // run fourth (parallel) step
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        assertEquals(0, waiting.size());
        routeInstance = session.getDocument(routeInstance.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(routeInstance.isDone());
        assertFalse(routeInstance.canUndoStep(session));
    }

    @Test
    public void testDocumentRouteWithWaitStateAndSecurity() throws Exception {
        CounterListener.resetCouner();

        // bob creates the route and validates it
        DocumentRef routeInstanceRef;
        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            DocumentRoute route = createDocumentRoute(bobSession, ROUTE1);
            assertNotNull(route);
            bobSession.save();
            List<DocumentRoute> routes = service.getAvailableDocumentRoute(bobSession);
            assertEquals(1, routes.size());
            DocumentRoute routeModel = routes.get(0);
            DocumentModel doc1 = createTestDocument("test1", bobSession);
            bobSession.save();
            service.lockDocumentRoute(route, bobSession);
            route = service.validateRouteModel(route, bobSession);
            service.unlockDocumentRouteUnrestrictedSession(route, bobSession);
            assertEquals("validated", route.getDocument().getCurrentLifeCycleState());
            assertEquals("validated",
                    bobSession.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
            bobSession.save();
            waitForAsyncExec();
            DocumentRoute routeInstance = service.createNewInstance(routeModel,
                    Collections.singletonList(doc1.getId()), bobSession, true);
            routeInstanceRef = routeInstance.getDocument().getRef();
        }

        // jack checks he can't do anything on it
        try (CloseableCoreSession jackSession = CoreInstance.openCoreSession(session.getRepositoryName(), "jack")) {
            assertFalse(jackSession.exists(routeInstanceRef));
        }

        // bob finishes the route
        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            DocumentRoute routeInstance = bobSession.getDocument(routeInstanceRef).getAdapter(DocumentRoute.class);
            List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
            WaitingStepRuntimePersister.resumeStep(waiting.get(0), bobSession);
            waiting = WaitingStepRuntimePersister.getRunningStepIds();
            assertEquals(1, waiting.size());
            WaitingStepRuntimePersister.resumeStep(waiting.get(0), bobSession);
            waiting = WaitingStepRuntimePersister.getRunningStepIds();
            assertEquals(2, waiting.size());
            WaitingStepRuntimePersister.resumeStep(waiting.get(0), bobSession);
            waiting = WaitingStepRuntimePersister.getRunningStepIds();
            assertEquals(1, waiting.size());
            assertFalse(routeInstance.isDone());
            WaitingStepRuntimePersister.resumeStep(waiting.get(0), bobSession);
            assertEquals(0, waiting.size());
            routeInstance = bobSession.getDocument(routeInstanceRef).getAdapter(DocumentRoute.class);
            assertTrue(routeInstance.isDone());
            assertEquals((6/* route */ + 4 /* number of steps */ * 3 /* number of event per waiting step */
                    + 2 /* workflow started + first task audit */), CounterListener.getCounter());
        }
    }

    @Test
    public void testExecuteRouteWithWaitStateAndConditionalStep() throws Exception {
        DocumentRoute route = createDocumentRouteWithConditionalFolder(session, ROUTE1);
        session.save();
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRoute(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated", route.getDocument().getCurrentLifeCycleState());
        assertEquals("validated", session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel, Collections.singletonList(doc1.getId()),
                session, true);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(1, service.getDocumentRoutesForAttachedDocument(session, doc1.getId()).size());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        // step 1
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        // first step in conditionalFolder, conditionalStep2 running,
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeDecisionalStep(waiting.get(0), session, "2");
        // executeIfOption2 is running
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        assertEquals("executeIfOption2", session.getDocument(new IdRef(waiting.get(0))).getName());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        // step 3 is run
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(0, waiting.size());
        routeInstance = session.getDocument(routeInstance.getDocument().getRef()).getAdapter(DocumentRoute.class);
        assertTrue(routeInstance.isDone());
    }

    protected void waitForAsyncExec() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

}
