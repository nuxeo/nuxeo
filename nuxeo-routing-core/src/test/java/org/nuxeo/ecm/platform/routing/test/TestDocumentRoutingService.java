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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteTableElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.RouteModelResourceType;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteAlredayLockedException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;

/**
 * @author arussel
 *
 */
public class TestDocumentRoutingService extends DocumentRoutingTestCase {

    protected File tmp;

    @Override
    @After
    public void tearDown() throws Exception {
        if (tmp != null) {
            tmp.delete();
        }
        super.tearDown();
    }

    @Test
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Deprecated
    public void testAddStepToDraftRoute() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        session.save();
        assertNotNull(route);
        DocumentModel step = session.createDocumentModel(
                route.getDocument().getPathAsString(), "step31bis",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step31bis");
        DocumentModelList stepFolders = session.query("Select * From Document WHERE dc:title = 'parallel1'");
        assertEquals(1, stepFolders.size());
        DocumentModel parallel1 = stepFolders.get(0);
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), "step32",
                step.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        DocumentModelList parallel1Childs = service.getOrderedRouteElement(
                parallel1.getId(), session);
        assertEquals(3, parallel1Childs.size());
        step = parallel1Childs.get(1);
        assertEquals("step31bis", step.getTitle());

        step = session.createDocumentModel(
                route.getDocument().getPathAsString(), "step33",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step33");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), null,
                step.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRoute(route, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(),
                session);
        assertEquals(4, parallel1Childs.size());
        step = parallel1Childs.get(3);
        assertEquals("step33", step.getTitle());

        step = session.createDocumentModel(
                route.getDocument().getPathAsString(), "step30",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step30");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), 0,
                step.getAdapter(DocumentRouteElement.class), session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(),
                session);
        service.unlockDocumentRoute(route, session);
        assertEquals(5, parallel1Childs.size());
        step = parallel1Childs.get(0);
        assertEquals("step30", step.getTitle());

        step = session.createDocumentModel(
                route.getDocument().getPathAsString(), "step34",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step34");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), 5,
                step.getAdapter(DocumentRouteElement.class), session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(),
                session);
        service.unlockDocumentRoute(route, session);
        assertEquals(6, parallel1Childs.size());
        step = parallel1Childs.get(5);
        assertEquals("step34", step.getTitle());

        step = session.createDocumentModel(
                route.getDocument().getPathAsString(), "step33bis",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step33bis");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), 5,
                step.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRoute(route, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(),
                session);
        assertEquals(7, parallel1Childs.size());
        step = parallel1Childs.get(5);
        assertEquals("step33bis", step.getTitle());
    }

    @Test
    public void testAddSameNamedStepToRunningRoute() throws Exception {
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        DocumentModelList childrens = session.getChildren(route.getDocument().getRef());
        String firstStepId = childrens.get(0).getId();
        String secondStepId = childrens.get(1).getId();
        String folderId = childrens.get(2).getId();
        service.lockDocumentRoute(route, session);
        DocumentModel newStep = session.createDocumentModel(
                route.getDocument().getPathAsString(), "step1",
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
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        service.lockDocumentRoute(route, session);
        service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        route = service.createNewInstance(route, new ArrayList<String>(),
                session, true);
        session.save();
        assertNotNull(route);
        DocumentModel step = session.createDocumentModel(
                route.getDocument().getPathAsString(), "step31bis",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME,
                "step31bis");
        DocumentModelList stepFolders = session.query("Select * From Document WHERE dc:title = 'parallel1' and ecm:currentLifeCycleState = 'ready'");
        assertEquals(1, stepFolders.size());
        DocumentModel parallel1 = stepFolders.get(0);
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), "step32",
                step.getAdapter(DocumentRouteElement.class), session);
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
        route = session.getDocument(route.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        assertTrue(route.isDone());
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testRemoveStep() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        DocumentModel stepFolder = session.getDocument(new PathRef(
                WORKSPACES_PATH + "/" + ROUTE1 + "/parallel1/"));
        DocumentModelList childs = service.getOrderedRouteElement(
                stepFolder.getId(), session);
        assertEquals(2, childs.size());

        DocumentModel step32 = session.getDocument(new PathRef(WORKSPACES_PATH
                + "/" + ROUTE1 + "/parallel1/step32"));
        assertNotNull(step32);
        service.lockDocumentRoute(route, session);
        service.removeRouteElement(
                step32.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRoute(route, session);
        childs = service.getOrderedRouteElement(stepFolder.getId(), session);
        assertEquals(1, childs.size());
    }

    @Test
    public void testSaveInstanceAsNewModel() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        route = service.createNewInstance(route, new ArrayList<String>(),
                session, true);
        assertNotNull(route);
        session.save();
        CoreSession managersSession = openSessionAs("routeManagers");
        DocumentModel step = managersSession.getChildren(
                route.getDocument().getRef()).get(0);
        service.lockDocumentRoute(route, managersSession);
        service.removeRouteElement(step.getAdapter(DocumentRouteElement.class),
                managersSession);
        service.unlockDocumentRoute(route, managersSession);
        DocumentRoute newModel = service.saveRouteAsNewModel(route,
                managersSession);
        assertNotNull(newModel);
        assertEquals("(COPY) route1",
                newModel.getDocument().getPropertyValue("dc:title"));
        closeSession(managersSession);
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testRemoveStepFromLockedRoute() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        DocumentModel stepFolder = session.getDocument(new PathRef(
                WORKSPACES_PATH + "/" + ROUTE1 + "/parallel1/"));
        DocumentModelList childs = service.getOrderedRouteElement(
                stepFolder.getId(), session);
        assertEquals(2, childs.size());

        DocumentModel step32 = session.getDocument(new PathRef(WORKSPACES_PATH
                + "/" + ROUTE1 + "/parallel1/step32"));
        assertNotNull(step32);
        service.lockDocumentRoute(route, session);
        // grant everyting permission on the route to jdoe
        DocumentModel routeModel = route.getDocument();
        ACP acp = routeModel.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, true));
        acp.addACL(localACL);
        routeModel.setACP(acp, true);
        session.saveDocument(routeModel);
        session.save();

        closeSession();
        session = openSessionAs("jdoe");
        Exception e = null;
        try {
            service.lockDocumentRoute(route, session);

        } catch (DocumentRouteAlredayLockedException e2) {
            e = e2;
        }
        assertNotNull(e);
        closeSession();
        openSession();
        // service.lockDocumentRoute(route, session);
        service.removeRouteElement(
                step32.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRoute(route, session);
        e = null;
        closeSession();
        session = openSessionAs("jdoe");
        try {
            service.unlockDocumentRoute(route, session);
        } catch (DocumentRouteNotLockedException e2) {
            e = e2;
        }
        assertNotNull(e);
        childs = service.getOrderedRouteElement(stepFolder.getId(), session);
        assertEquals(1, childs.size());
        closeSession();
    }

    @Test
    public void testCreateNewInstance() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        session.save();
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                Collections.singletonList(doc1.getId()), session, true);
        assertTrue(routeInstance.isDone());

        // check that we don't get route instances when querying for models
        String routeDocId = service.getRouteModelDocIdWithId(session, ROUTE1);
        DocumentModel doc = session.getDocument(new IdRef(routeDocId));
        route = doc.getAdapter(DocumentRoute.class);

        assertNotNull(route);
        // this API does not restrict itself to models actually
        routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(2, routes.size());
    }

    @Test
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
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                Collections.singletonList(doc1.getId()), session, true);
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

    @Test
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
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                Collections.singletonList(doc1.getId()), session, true);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(
                1,
                service.getDocumentRoutesForAttachedDocument(session,
                        doc1.getId()).size());
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
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                Collections.singletonList(doc1.getId()), session, true);
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

    @Test
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
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                Collections.singletonList(doc1.getId()), session, true);
        closeSession();
        openSession();
        routeInstance = session.getDocument(
                routeInstance.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        session.saveDocument(routeInstance.getDocument());
        session.save();
        closeSession();

        // jack checks he can't do anything on it
        session = openSessionAs("jack");
        assertFalse(session.exists(routeInstance.getDocument().getRef()));
        // routeInstance = session.getDocument(
        // routeInstance.getDocument().getRef()).getAdapter(
        // DocumentRoute.class);
        // assertFalse(routeInstance.canValidateStep(session));
        // List<String> waiting =
        // WaitingStepRuntimePersister.getRunningStepIds();
        // assertEquals(1, waiting.size());
        // boolean exception = false;
        // try {// jacks fails to resume the step
        // WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        // fail();
        // } catch (Exception e) {
        // exception = true;
        // }
        // assertTrue(exception);
        closeSession();
        // bob finishes the route

        session = openSessionAs("bob");
        routeInstance = session.getDocument(
                routeInstance.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
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

    @Test
    public void testGetAvailableDocumentRouteModel() throws ClientException {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
    }

    @Test
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
        service.lockDocumentRoute(route.getAdapter(DocumentRoute.class),
                session);
        route = service.validateRouteModel(
                route.getAdapter(DocumentRoute.class), session).getDocument();
        session.save();
        service.unlockDocumentRouteUnrestrictedSession(
                route.getAdapter(DocumentRoute.class), session);
        route = session.getDocument(route.getRef());
        assertEquals("validated", route.getCurrentLifeCycleState());
        closeSession();
        session = openSessionAs("jdoe");
        assertFalse(session.hasPermission(route.getRef(),
                SecurityConstants.WRITE));
        assertTrue(session.hasPermission(route.getRef(), SecurityConstants.READ));
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testConditionalFolderContainerModel() throws ClientException {
        DocumentRoute route = createDocumentRouteWithConditionalFolder(session,
                ROUTE1);
        DocumentModel conditionalStepFolder = session.getChild(
                route.getDocument().getRef(), "conditionalStep2");
        DocumentModelList children = service.getOrderedRouteElement(
                conditionalStepFolder.getId(), session);
        assertEquals(DocumentRoutingConstants.STEP_DOCUMENT_TYPE,
                children.get(0).getType());
        DocumentModel branch1 = children.get(1);
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE,
                branch1.getType());
        assertEquals("executeIfOption1",
                session.getChildren(branch1.getRef()).get(0).getName());
        DocumentModel branch2 = children.get(2);
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE,
                branch2.getType());
        assertEquals("executeIfOption2",
                session.getChildren(branch2.getRef()).get(0).getName());
    }

    @Test
    public void testGetRouteElements() throws ClientException {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        List<DocumentRouteTableElement> elements = service.getRouteElements(
                route, session);
        assertNotNull(elements);
        assertEquals(4, elements.size());
        for (DocumentRouteTableElement element : elements) {
            assertEquals(1, element.getRouteMaxDepth());
        }
        assertEquals(1, elements.get(2).getFirstChildFolders().size());
        assertEquals(0, elements.get(3).getFirstChildFolders().size());
        assertEquals(
                2,
                elements.get(2).getFirstChildFolders().get(0).getTotalChildCount());
        assertEquals(4, elements.get(0).getRouteTable().getTotalChildCount());
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testExecuteSimpleRouteWithConditionalStep()
            throws ClientException {
        DocumentRoute route = createDocumentRouteWithConditionalFolder(session,
                ROUTE1);
        DocumentModel conditionalStepFolder = session.getChild(
                route.getDocument().getRef(), "conditionalStep2");
        DocumentModelList children = service.getOrderedRouteElement(
                conditionalStepFolder.getId(), session);
        assertEquals(DocumentRoutingConstants.STEP_DOCUMENT_TYPE,
                children.get(0).getType());
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE,
                children.get(1).getType());
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE,
                children.get(2).getType());
        // set first option to be executed on this route in case of the
        // conditional folder
        conditionalStepFolder.setPropertyValue(
                DocumentRoutingConstants.STEP_TO_BE_EXECUTED_NEXT_PROPERTY_NAME,
                "1");
        session.saveDocument(conditionalStepFolder);
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        session.save();
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        waitForAsyncExec();
        closeSession();
        session = openSessionAs("routeManagers");
        route = session.getDocument(route.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        DocumentRoute routeInstance = service.createNewInstance(route,
                Collections.singletonList(doc1.getId()), session, true);
        session.save();
        waitForAsyncExec();
        assertTrue(routeInstance.isDone());

        // check if branch no 1 in the optional folder was executed
        children = session.getChildren(routeInstance.getDocument().getRef(),
                DocumentRoutingConstants.CONDITIONAL_STEP_DOCUMENT_TYPE);

        children = service.getOrderedRouteElement(children.get(0).getId(),
                session);
        // branch executed in done
        assertEquals("done", children.get(1).getCurrentLifeCycleState());
        // branch not executed is now in canceled state
        assertEquals("canceled", children.get(2).getCurrentLifeCycleState());
    }

    @Test
    public void testExecuteRouteWithWaitStateAndConditionalStep()
            throws Exception {
        deployBundle(TEST_BUNDLE);
        DocumentRoute route = createDocumentRouteWithConditionalFolder(session,
                ROUTE1);
        session.save();
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRouteModel(session);
        assertEquals(1, routes.size());
        DocumentRoute routeModel = routes.get(0);
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        assertEquals("validated",
                route.getDocument().getCurrentLifeCycleState());
        assertEquals(
                "validated",
                session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel,
                Collections.singletonList(doc1.getId()), session, true);
        assertNotNull(routeInstance);
        assertFalse(routeInstance.isDone());
        assertEquals(
                1,
                service.getDocumentRoutesForAttachedDocument(session,
                        doc1.getId()).size());
        List<String> waiting = WaitingStepRuntimePersister.getRunningStepIds();
        // step 1
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        // first step in conditionalFolder, conditionalStep2 running,
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeDecisionalStep(waiting.get(0),
                session, "2");
        // executeIfOption2 is running
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        assertEquals("executeIfOption2",
                session.getDocument(new IdRef(waiting.get(0))).getName());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        // step 3 is run
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(1, waiting.size());
        WaitingStepRuntimePersister.resumeStep(waiting.get(0), session);
        waiting = WaitingStepRuntimePersister.getRunningStepIds();
        assertEquals(0, waiting.size());
        routeInstance = session.getDocument(
                routeInstance.getDocument().getRef()).getAdapter(
                DocumentRoute.class);
        assertTrue(routeInstance.isDone());
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

    @Test
    public void testTemplateResourceExtensionParsing() throws Exception {
        deployBundle(TEST_BUNDLE);
        deployTestContrib(TEST_BUNDLE,
                "OSGI-INF/test-document-routing-route-models-template-resource-contrib.xml");
        List<URL> urls = service.getRouteModelTemplateResources();
        assertEquals(1, urls.size());
    }

    @Test
    public void testImportRouteModel() throws Exception {
        closeSession();
        deployBundle("org.nuxeo.runtime.reload");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.query.api");
        deployBundle("org.nuxeo.ecm.platform.task.core");
        deployBundle(TEST_BUNDLE);
        Framework.getLocalService(ReloadService.class).reloadRepository();
        openSession();
        // create an initial route to test that is override at import
        DocumentModel root = createDocumentModel(session,
                "document-route-models-root", "DocumentRouteModelsRoot", "/");
        assertNotNull(root);
        DocumentModel route = createDocumentModel(session, "myRoute",
                "DocumentRoute", "/document-route-models-root/");
        route.setPropertyValue("dc:coverage", "test");
        route = session.saveDocument(route);
        // set ACL to test that the ACLs are kept
        ACP acp = route.getACP();
        ACL acl = acp.getOrCreateACL("testrouting");
        acl.add(new ACE("testusername", "Write", true));
        acp.addACL(acl);
        route.setACP(acp, true);
        route = session.saveDocument(route);

        assertNotNull(route);
        assertEquals("test", route.getPropertyValue("dc:coverage"));

        DocumentModel node = createDocumentModel(session, "myNode",
                "RouteNode", "/document-route-models-root/myRoute");

        assertNotNull(node);

        // create a ZIP for the contrib
        tmp = File.createTempFile("nuxeoRoutingTest", ".zip");
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tmp));
        URL url = getClass().getResource("/routes/myRoute");
        File dir = new File(url.toURI().getPath());
        zipTree("", dir, false, zout);
        zout.finish();
        zout.close();

        RouteModelResourceType resource = new RouteModelResourceType();
        resource.setId("test");
        resource.setPath(tmp.getPath());
        resource.setUrl(tmp.toURI().toURL());
        service.registerRouteResource(resource, null);

        session.save();
        // trigger model creation (calls service.importRouteModel)
        fireFrameworkStarted();
        session.save(); // process invalidations

        DocumentModel modelsRoot = session.getDocument(new PathRef(
                "/document-route-models-root/"));
        assertNotNull(modelsRoot);
        route = session.getDocument(new PathRef(
                "/document-route-models-root/myRoute"));
        assertNotNull(route);

        String routeDocId = service.getRouteModelDocIdWithId(session, "myRoute");
        DocumentModel doc = session.getDocument(new IdRef(routeDocId));
        DocumentRoute model = doc.getAdapter(DocumentRoute.class);

        assertEquals(route.getId(), model.getDocument().getId());
        // test that document was overriden but the ACLs were kept
        ACL newAcl = route.getACP().getACL("testrouting");
        assertNotNull(newAcl);
        assertEquals(1, newAcl.getACEs().length);
        assertEquals("testusername", newAcl.getACEs()[0].getUsername());

        // Oracle makes no difference between null and blank
        assertTrue(StringUtils.isBlank((String) route.getPropertyValue("dc:coverage")));
        try {
            node = session.getDocument(new PathRef(
                    "/document-route-models-root/myRoute/myNode"));
        } catch (ClientException e) {
            node = null;
        }
        assertNull(node);
        assertEquals("DocumentRoute", route.getType());
        DocumentModel step1 = session.getDocument(new PathRef(
                "/document-route-models-root/myRoute/Step1"));
        assertNotNull(step1);
        assertEquals("RouteNode", step1.getType());
        DocumentModel step2 = session.getDocument(new PathRef(
                "/document-route-models-root/myRoute/Step2"));
        assertNotNull(step2);
        assertEquals("RouteNode", step2.getType());
    }

    protected void zipTree(String prefix, File root, boolean includeRoot,
            ZipOutputStream zout) throws IOException {
        if (includeRoot) {
            prefix += root.getName() + '/';
            zipDirectory(prefix, zout);
        }
        for (String name : root.list()) {
            File file = new File(root, name);
            if (file.isDirectory()) {
                zipTree(prefix, file, true, zout);
            } else {
                if (name.endsWith("~") || name.endsWith("#")
                        || name.endsWith(".bak")) {
                    continue;
                }
                name = prefix + name;
                zipFile(name, file, zout);
            }
        }
    }

    protected void zipDirectory(String entryName, ZipOutputStream zout)
            throws IOException {
        ZipEntry zentry = new ZipEntry(entryName);
        zout.putNextEntry(zentry);
        zout.closeEntry();
    }

    protected void zipFile(String entryName, File file, ZipOutputStream zout)
            throws IOException {
        ZipEntry zentry = new ZipEntry(entryName);
        zentry.setTime(file.lastModified());
        zout.putNextEntry(zentry);
        FileInputStream in = new FileInputStream(file);
        try {
            IOUtils.copy(in, zout);
        } finally {
            in.close();
        }
        zout.closeEntry();
    }

    protected void waitForAsyncExec() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }
}