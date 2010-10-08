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
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class TestDocumentRoutingService extends DocumentRoutingTestCase {

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
        List<String> waiting = WaitingStepRuntimePersister.getStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getStepIds();
        assertEquals(2, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getStepIds();
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
        List<String> waiting = WaitingStepRuntimePersister.getStepIds();
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
        waiting = WaitingStepRuntimePersister.getStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getStepIds();
        assertEquals(2, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getStepIds();
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
        route = service.validateRouteModel(route.getAdapter(DocumentRoute.class),
                session).getDocument();
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
