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
import java.net.URL;
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
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteAlredayLockedException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestDocumentRoutingService extends DocumentRoutingTestCase {

    protected File tmp;

    @After
    public void tearDown() throws Exception {
        if (tmp != null) {
            tmp.delete();
        }
    }

    @Test
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Deprecated
    public void testAddStepToDraftRoute() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        session.save();
        assertNotNull(route);
        DocumentModel step = session.createDocumentModel(route.getDocument().getPathAsString(), "step31bis",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, "step31bis");
        DocumentModelList stepFolders = session.query("Select * From Document WHERE dc:title = 'parallel1'");
        assertEquals(1, stepFolders.size());
        DocumentModel parallel1 = stepFolders.get(0);
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), "step32", step.getAdapter(DocumentRouteElement.class),
                session);
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        DocumentModelList parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        assertEquals(3, parallel1Childs.size());
        step = parallel1Childs.get(1);
        assertEquals("step31bis", step.getTitle());

        step = session.createDocumentModel(route.getDocument().getPathAsString(), "step33",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, "step33");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), null, step.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRoute(route, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        assertEquals(4, parallel1Childs.size());
        step = parallel1Childs.get(3);
        assertEquals("step33", step.getTitle());

        step = session.createDocumentModel(route.getDocument().getPathAsString(), "step30",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, "step30");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), 0, step.getAdapter(DocumentRouteElement.class), session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        service.unlockDocumentRoute(route, session);
        assertEquals(5, parallel1Childs.size());
        step = parallel1Childs.get(0);
        assertEquals("step30", step.getTitle());

        step = session.createDocumentModel(route.getDocument().getPathAsString(), "step34",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, "step34");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), 5, step.getAdapter(DocumentRouteElement.class), session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        service.unlockDocumentRoute(route, session);
        assertEquals(6, parallel1Childs.size());
        step = parallel1Childs.get(5);
        assertEquals("step34", step.getTitle());

        step = session.createDocumentModel(route.getDocument().getPathAsString(), "step33bis",
                DocumentRoutingConstants.STEP_DOCUMENT_TYPE);
        step.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, "step33bis");
        service.lockDocumentRoute(route, session);
        service.addRouteElementToRoute(parallel1.getRef(), 5, step.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRoute(route, session);
        parallel1Childs = service.getOrderedRouteElement(parallel1.getId(), session);
        assertEquals(7, parallel1Childs.size());
        step = parallel1Childs.get(5);
        assertEquals("step33bis", step.getTitle());
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testRemoveStep() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        DocumentModel stepFolder = session.getDocument(new PathRef(WORKSPACES_PATH + "/" + ROUTE1 + "/parallel1/"));
        DocumentModelList childs = service.getOrderedRouteElement(stepFolder.getId(), session);
        assertEquals(2, childs.size());

        DocumentModel step32 = session.getDocument(new PathRef(WORKSPACES_PATH + "/" + ROUTE1 + "/parallel1/step32"));
        assertNotNull(step32);
        service.lockDocumentRoute(route, session);
        service.removeRouteElement(step32.getAdapter(DocumentRouteElement.class), session);
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
        session.save();
        waitForAsyncExec();
        route = service.createNewInstance(route, new ArrayList<String>(), session, true);
        assertNotNull(route);
        session.save();
        try (CloseableCoreSession managersSession = CoreInstance.openCoreSession(session.getRepositoryName(), "routeManagers")) {
            DocumentModel step = managersSession.getChildren(route.getDocument().getRef()).get(0);
            service.lockDocumentRoute(route, managersSession);
            service.removeRouteElement(step.getAdapter(DocumentRouteElement.class), managersSession);
            service.unlockDocumentRoute(route, managersSession);
            DocumentRoute newModel = service.saveRouteAsNewModel(route, managersSession);
            assertNotNull(newModel);
            assertEquals("(COPY) route1", newModel.getDocument().getPropertyValue("dc:title"));
        }
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testRemoveStepFromLockedRoute() throws Exception {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        DocumentModel stepFolder = session.getDocument(new PathRef(WORKSPACES_PATH + "/" + ROUTE1 + "/parallel1/"));
        DocumentModelList childs = service.getOrderedRouteElement(stepFolder.getId(), session);
        assertEquals(2, childs.size());

        DocumentModel step32 = session.getDocument(new PathRef(WORKSPACES_PATH + "/" + ROUTE1 + "/parallel1/step32"));
        assertNotNull(step32);
        service.lockDocumentRoute(route, session);
        // grant everyting permission on the route to jdoe
        DocumentModel routeModel = route.getDocument();
        ACP acp = routeModel.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, true));
        acp.addACL(localACL);
        routeModel.setACP(acp, true);
        session.saveDocument(routeModel);
        session.save();

        try (CloseableCoreSession jdoeSession = CoreInstance.openCoreSession(session.getRepositoryName(), "jdoe")) {
            Exception e = null;
            try {
                service.lockDocumentRoute(route, jdoeSession);
            } catch (DocumentRouteAlredayLockedException e2) {
                e = e2;
            }
            assertNotNull(e);
        }

        // service.lockDocumentRoute(route, session);
        service.removeRouteElement(step32.getAdapter(DocumentRouteElement.class), session);
        service.unlockDocumentRoute(route, session);

        try (CloseableCoreSession jdoeSession = CoreInstance.openCoreSession(session.getRepositoryName(), "jdoe")) {
            Exception e = null;
            try {
                service.unlockDocumentRoute(route, jdoeSession);
            } catch (DocumentRouteNotLockedException e2) {
                e = e2;
            }
            assertNotNull(e);
            childs = service.getOrderedRouteElement(stepFolder.getId(), jdoeSession);
            assertEquals(1, childs.size());
        }
    }

    @Test
    public void testCreateNewInstance() throws Exception {
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
        session.save();
        assertEquals("validated", route.getDocument().getCurrentLifeCycleState());
        assertEquals("validated", session.getChildren(route.getDocument().getRef()).get(0).getCurrentLifeCycleState());
        session.save();
        waitForAsyncExec();
        DocumentRoute routeInstance = service.createNewInstance(routeModel, Collections.singletonList(doc1.getId()),
                session, true);
        assertTrue(routeInstance.isDone());

        // check that we don't get route instances when querying for models
        String routeDocId = service.getRouteModelDocIdWithId(session, ROUTE1);
        DocumentModel doc = session.getDocument(new IdRef(routeDocId));
        route = doc.getAdapter(DocumentRoute.class);

        assertNotNull(route);
        // this API does not restrict itself to models actually
        routes = service.getAvailableDocumentRoute(session);
        assertEquals(2, routes.size());
    }

    @Test
    public void testGetAvailableDocumentRouteModel() {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        session.save();
        List<DocumentRoute> routes = service.getAvailableDocumentRoute(session);
        assertEquals(1, routes.size());
    }

    @Test
    public void testRouteModel() {
        DocumentModel folder = createDocumentModel(session, "TestFolder", "Folder", "/");
        session.save();
        assertNotNull(folder);
        setPermissionToUser(folder, "jdoe", SecurityConstants.WRITE);
        DocumentModel route = createDocumentRouteModel(session, ROUTE1, folder.getPathAsString());
        session.save();
        assertNotNull(route);
        service.lockDocumentRoute(route.getAdapter(DocumentRoute.class), session);
        route = service.validateRouteModel(route.getAdapter(DocumentRoute.class), session).getDocument();
        session.save();
        service.unlockDocumentRouteUnrestrictedSession(route.getAdapter(DocumentRoute.class), session);
        route = session.getDocument(route.getRef());
        assertEquals("validated", route.getCurrentLifeCycleState());

        try (CloseableCoreSession jdoeSession = CoreInstance.openCoreSession(session.getRepositoryName(), "jdoe")) {
            assertFalse(jdoeSession.hasPermission(route.getRef(), SecurityConstants.WRITE));
            assertTrue(jdoeSession.hasPermission(route.getRef(), SecurityConstants.READ));
        }
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testConditionalFolderContainerModel() {
        DocumentRoute route = createDocumentRouteWithConditionalFolder(session, ROUTE1);
        DocumentModel conditionalStepFolder = session.getChild(route.getDocument().getRef(), "conditionalStep2");
        DocumentModelList children = service.getOrderedRouteElement(conditionalStepFolder.getId(), session);
        assertEquals(DocumentRoutingConstants.STEP_DOCUMENT_TYPE, children.get(0).getType());
        DocumentModel branch1 = children.get(1);
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE, branch1.getType());
        assertEquals("executeIfOption1", session.getChildren(branch1.getRef()).get(0).getName());
        DocumentModel branch2 = children.get(2);
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE, branch2.getType());
        assertEquals("executeIfOption2", session.getChildren(branch2.getRef()).get(0).getName());
    }

    @Test
    public void testGetRouteElements() {
        DocumentRoute route = createDocumentRoute(session, ROUTE1);
        assertNotNull(route);
        List<DocumentRouteTableElement> elements = service.getRouteElements(route, session);
        assertNotNull(elements);
        assertEquals(4, elements.size());
        for (DocumentRouteTableElement element : elements) {
            assertEquals(1, element.getRouteMaxDepth());
        }
        assertEquals(1, elements.get(2).getFirstChildFolders().size());
        assertEquals(0, elements.get(3).getFirstChildFolders().size());
        assertEquals(2, elements.get(2).getFirstChildFolders().get(0).getTotalChildCount());
        assertEquals(4, elements.get(0).getRouteTable().getTotalChildCount());
    }

    @Test
    @Deprecated
    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    public void testExecuteSimpleRouteWithConditionalStep() {
        DocumentRoute route = createDocumentRouteWithConditionalFolder(session, ROUTE1);
        DocumentModel conditionalStepFolder = session.getChild(route.getDocument().getRef(), "conditionalStep2");
        DocumentModelList children = service.getOrderedRouteElement(conditionalStepFolder.getId(), session);
        assertEquals(DocumentRoutingConstants.STEP_DOCUMENT_TYPE, children.get(0).getType());
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE, children.get(1).getType());
        assertEquals(DocumentRoutingConstants.STEP_FOLDER_DOCUMENT_TYPE, children.get(2).getType());
        // set first option to be executed on this route in case of the
        // conditional folder
        conditionalStepFolder.setPropertyValue(DocumentRoutingConstants.STEP_TO_BE_EXECUTED_NEXT_PROPERTY_NAME, "1");
        session.saveDocument(conditionalStepFolder);
        service.lockDocumentRoute(route, session);
        route = service.validateRouteModel(route, session);
        DocumentRef routeRef = route.getDocument().getRef();
        service.unlockDocumentRouteUnrestrictedSession(route, session);
        session.save();
        assertEquals("validated", route.getDocument().getCurrentLifeCycleState());
        assertEquals("validated", session.getChildren(routeRef).get(0).getCurrentLifeCycleState());
        DocumentModel doc1 = createTestDocument("test1", session);
        session.save();
        waitForAsyncExec();

        try (CloseableCoreSession managerSession = CoreInstance.openCoreSession(session.getRepositoryName(), "routeManagers")) {
            route = managerSession.getDocument(routeRef).getAdapter(DocumentRoute.class);
            DocumentRoute routeInstance = service.createNewInstance(route, Collections.singletonList(doc1.getId()),
                    managerSession, true);
            managerSession.save();
            waitForAsyncExec();
            assertTrue(routeInstance.isDone());

            // check if branch no 1 in the optional folder was executed
            children = managerSession.getChildren(routeInstance.getDocument().getRef(),
                    DocumentRoutingConstants.CONDITIONAL_STEP_DOCUMENT_TYPE);

            children = service.getOrderedRouteElement(children.get(0).getId(), managerSession);
            // branch executed in done
            assertEquals("done", children.get(1).getCurrentLifeCycleState());
            // branch not executed is now in canceled state
            assertEquals("canceled", children.get(2).getCurrentLifeCycleState());
        }
    }

    protected void setPermissionToUser(DocumentModel doc, String username, String... perms) {
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
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

}
