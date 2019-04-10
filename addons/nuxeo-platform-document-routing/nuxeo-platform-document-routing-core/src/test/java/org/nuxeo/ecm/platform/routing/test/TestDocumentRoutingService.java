/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.test;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class TestDocumentRoutingService extends DocumentRoutingTestCase {

    public void testAddStepToRouteElement() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        DocumentModel step = session.createDocumentModel(DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step31bis");
        DocumentModelList stepFolders = session.query("Select * From Document WHERE dc:title = 'parallel1'");
        assertEquals(1, stepFolders.size());
        DocumentModel parallel1 = stepFolders.get(0);
        service.addRouteElementToRoute(parallel1.getRef(), "step32", step, session);
        DocumentModelList parallel1Childs = service.getOrderedRouteElement(
                parallel1.getId(), session);
        assertEquals(3, parallel1Childs.size());
        step = parallel1Childs.get(1);
        assertEquals("step31bis", step.getTitle());

        step = session.createDocumentModel(DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step33");
        service.addRouteElementToRoute(parallel1.getRef(), null, step, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        assertEquals(4, parallel1Childs.size());
        step = parallel1Childs.get(3);
        assertEquals("step33", step.getTitle());

        step = session.createDocumentModel(DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step30");
        service.addRouteElementToRoute(parallel1.getRef(), 0, step, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        assertEquals(5, parallel1Childs.size());
        step = parallel1Childs.get(0);
        assertEquals("step30", step.getTitle());

        step = session.createDocumentModel(DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step34");
        service.addRouteElementToRoute(parallel1.getRef(), 5, step, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        assertEquals(6, parallel1Childs.size());
        step = parallel1Childs.get(5);
        assertEquals("step34", step.getTitle());

        step = session.createDocumentModel(DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step33bis");
        service.addRouteElementToRoute(parallel1.getRef(), 5, step, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        assertEquals(7, parallel1Childs.size());
        step = parallel1Childs.get(5);
        assertEquals("step33bis", step.getTitle());
    }

    public void testRemoveStep() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        DocumentModel stepFolder = session.getDocument(new PathRef(
                WORKSPACES_PATH + "/" + ROUTE1 + "/parallel1/"));
        DocumentModelList childs = service.getOrderedRouteElement(stepFolder.getId(),
                session);
        assertEquals(2, childs.size());

        DocumentModel step32 = session.getDocument(new PathRef(WORKSPACES_PATH
                + "/" + ROUTE1 + "/parallel1/step32"));
        assertNotNull(step32);
        service.removeRouteElement(step32, session);
        childs = service.getOrderedRouteElement(stepFolder.getId(), session);
        assertEquals(1, childs.size());
    }

    public void testCreateNewInstance() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        route = service.validateRouteModel(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                doc1.getId(), session);
        assertTrue(routeInstance.isDone());
    }

    public void testDocumentRouteWithWaitState() throws Exception {
        CounterListener.resetCouner();
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        route = service.validateRouteModel(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                doc1.getId(), session);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(
                1,
                service.getDocumentRoutesForAttachedDocument(session,
                        doc1.getId()).size());
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
        routeInstance = session.getDocument(
                routeInstance.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        assertTrue(routeInstance.isDone());
        assertEquals(6/* route */+ 4 /* number of steps */* 3 /*
                                                               * number of event
                                                               * per waiting
                                                               * step
                                                               */,
                CounterListener.getCounter());
    }
    public void testCancelRoute() throws Exception {
        CounterListener.resetCouner();
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        route = service.validateRouteModel(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                doc1.getId(), session);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(
                1,
                service.getDocumentRoutesForAttachedDocument(session,
                        doc1.getId()).size());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        routeInstance.cancel(session);
        assertTrue(routeInstance.isCancelled());
        DocumentModelList children = session.getChildren(routeInstance.getDocument().getRef());
        while(true) {
            for(DocumentModel doc : children) {
                  assertTrue(doc.getCurrentLifeCycleState().equals("cancelled"));
            }
            children = new DocumentModelListImpl();
            for(DocumentModel doc : children) {
                children.addAll(session.getChildren(doc.getRef()));
            }
            if(children.isEmpty()) {
                break;
            }
        }
    }

    public void testDocumentRouteWithStepBack() throws Exception {
        CounterListener.resetCouner();
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        route = service.validateRouteModel(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                doc1.getId(), session);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(
                1,
                service.getDocumentRoutesForAttachedDocument(session,
                        doc1.getId()).size());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        String firstStepId = waiting.get(0);
        // run first step
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        // undo second step
        String secondStepId = waiting.get(0);
        DocumentRouteStep step = WaitingStepRuntimePersister.getStep(
                secondStepId, session);
        assertTrue(step.canUndoStep(session));
        step = step.undo(session);
        assertTrue(step.isReady());
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        // restart route
        routeInstance.run(session);
        // undo second and first step
        DocumentRouteStep firstStep = WaitingStepRuntimePersister.getStep(
                firstStepId, session);
        DocumentRouteStep secondStep = WaitingStepRuntimePersister.getStep(
                secondStepId, session);
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
        routeInstance = session.getDocument(
                routeInstance.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        assertTrue(routeInstance.isDone());
        assertFalse(routeInstance.canUndoStep(session));
    }

    public void testDocumentRouteWithWaitStateAndSecurity() throws Exception {
        // bob create the route and validate it
        CounterListener.resetCouner();
        closeSession();
        session = openSessionAs("bob");
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        route = service.validateRouteModel(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                doc1.getId(), session);
        closeSession();
        openSession();
        session.saveDocument(routeInstance.getDocument());
        session.save();
        closeSession();
        // jack checks he can't do anything on it
        session = openSessionAs("jack");
        assertFalse(routeInstance.canValidateStep(session));
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        boolean exception = false;
        try {// jacks fails to resume the step
            WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
            fail();
        } catch (Exception e) {
            exception = true;
        }
        assertTrue(exception);
        closeSession();
        // jack finishes the route
        session = openSessionAs("bob");
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
        routeInstance = session.getDocument(
                routeInstance.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        assertTrue(routeInstance.isDone());
        assertEquals(6/* route */+ 4 /* number of steps */* 3 /*
                                                               * number of event
                                                               * per waiting
                                                               * step
                                                               */,
                CounterListener.getCounter());
    }

    public void testGetAvailableDocumentRouteModel() throws ClientException {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
    }

    public void testRouteModel() throws ClientException {
        DocumentModel folder = createDocumentModel(session, "TestFolder",
                "Folder", "/");
        session.save();
        assertNotNull(folder);
        setPermissionToUser(folder, "jdoe", SecurityConstants.WRITE);
        DocumentModel route = createDocumentRouteModel(session, ROUTE1,
                folder.getPathAsString());
        session.save();
        assertNotNull(route);
        route = service.validateRouteModel(
                route.getAdapter(DocumentRoute.class), session).getDocument();
        session.save();
        route = session.getDocument(route.getRef());
        assertEquals("validated", route.getCurrentLifeCycleState());
        closeSession();
        session = openSessionAs("jdoe");
        assertFalse(session.hasPermission(route.getRef(),
                SecurityConstants.WRITE));
        assertTrue(session.hasPermission(route.getRef(), SecurityConstants.READ));
    }

    protected void setPermissionToUser(DocumentModel doc, String username,
            String... perms) throws ClientException {
        ACP acp = doc.getACP();
        if (acp == null) {
            acp = new ACPImpl();
        }
        UserEntry userEntry = new UserEntryImpl(username);
        for (String perm : perms) {
            userEntry.addPrivilege(perm, true, false);
        }
        acp.setRules("test", new UserEntry[] { userEntry });
        doc.setACP(acp, true);
        session.save();
    }

    protected void waitForAsyncExec() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }
}
