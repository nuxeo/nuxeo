package org.nuxeo.opensocial.spaces.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class SpacesTypesTest {


    @Inject
    private CoreSession session;

    @Inject
    public SpacesTypesTest(TestRuntimeHarness harness) throws Exception {
        //TODO: Add a harness in nuxeo-test-util
        harness.deployBundle("org.nuxeo.ecm.platform.types.api");
        harness.deployBundle("org.nuxeo.ecm.platform.types.core");
        harness.deployBundle("org.nuxeo.ecm.actions");
        harness.deployContrib("org.nuxeo.ecm.webapp.core","OSGI-INF/ecm-types-contrib.xml");
        harness.deployContrib("org.nuxeo.ecm.webapp.core","OSGI-INF/actions-contrib.xml");
        harness.deployContrib("org.nuxeo.ecm.spaces.core","OSGI-INF/spaces-core-contrib.xml");
        harness.deployBundle("org.nuxeo.opensocial.spaces.web");


    }


    @Test
    public void leTypeExiste() throws Exception {
        TypeManager docTypeService = Framework.getService(TypeManager.class);
        Type type = docTypeService.getType("Space");
        assertNotNull(type);

        assertEquals("A collaborative dashboard", type.getDescription());
        assertEquals("view_documents", type.getDefaultView());
        assertEquals("Collaborative", type.getCategory());
        assertEquals("/icons/Space.png", type.getIcon());
        assertEquals("/icons/Space.png", type.getBigIcon());


    }

    @Test
    public void iCanAddASpaceInAWorkspace() throws Exception {
        TypeManager docTypeService = Framework.getService(TypeManager.class);
        Type workspace = docTypeService.getType("Workspace");
        Type space = docTypeService.getType("Space");
        assertNotNull(workspace);
        assertTrue(docTypeService.getAllowedSubTypes("Workspace").contains(space));
    }

    @Test
    public void tabdashActionExists() throws Exception {
        ActionManager am = Framework.getService(ActionManager.class);
        assertNotNull(am);
        assertNotNull(am.getAction("TAB_DASH"));
    }

    @Test
    public void dashBoardShouldAppearInTabs() throws Exception {

        DocumentModel doc = session.createDocumentModel("/", "testspace", "Space");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "Test space");
        doc.setPropertyValue("dc:created", new Date());
        session.saveDocument(doc);
        session.save();

        assertTrue(session.exists(new PathRef("/testspace")));


        ActionContext ac = new ActionContext();
        ac.setCurrentDocument(doc);
        ac.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());
        ac.setDocumentManager(session);
        ActionManager am = Framework.getService(ActionManager.class);
        assertNotNull(am);

        Action action = am.getAction("TAB_DASH");
        assertTrue(am.getActions("VIEW_ACTION_LIST",ac).contains(action));

        action = am.getAction("TAB_CONTENT");
        assertNotNull(action);
        assertFalse(am.getActions("VIEW_ACTION_LIST",ac).contains(action));

        session.removeDocument(doc.getRef());
        session.save();
    }

    @Test
    public void contentTabStillAppearsForWorkspaces() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "ws", "Folder");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "Test space");
        doc.setPropertyValue("dc:created", new Date());
        session.saveDocument(doc);
        session.save();

        assertTrue(session.exists(new PathRef("/ws")));


        ActionContext ac = new ActionContext();
        ac.setCurrentDocument(doc);
        ac.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());
        ac.setDocumentManager(session);
        ActionManager am = Framework.getService(ActionManager.class);
        assertNotNull(am);

        Action action = am.getAction("TAB_CONTENT");
        assertNotNull(action);
        assertTrue(am.getActions("VIEW_ACTION_LIST",ac).contains(action));



        session.removeDocument(doc.getRef());
        session.save();
    }

}
