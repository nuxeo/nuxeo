package org.nuxeo.ecm.spaces.core;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.SpaceProvider;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.VirtualUnivers;
import org.nuxeo.runtime.api.Framework;

@RunWith(JUnit4.class)
public class DocSpaceTest extends SQLRepositoryTestCase {

    private SpaceManager service;

    private VirtualUnivers univers;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.spaces.api");
        deployBundle("org.nuxeo.ecm.spaces.core");
        service = Framework.getService(SpaceManager.class);
        session = openSessionAs("Administrator");
        univers = new VirtualUnivers("main");
        deployContrib("org.nuxeo.ecm.spaces.core.test",
                "OSGI-INF/test1-spaces-contrib.xml");
    }

    @Test
    public void iCanGetSpaceManager() throws Exception {
        assertNotNull(service);
    }

    @Test
    public void iGetTheHomeSpaceProvider() throws Exception {

        List<SpaceProvider> spacesProvider = service.getSpacesProvider(univers);

        assertEquals(1, spacesProvider.size());
        List<Space> spaces = service.getSpacesForUnivers(univers, session);
        assertEquals(1, spaces.size());

        Space space = spaces.get(0);
        assertNotNull(space);
        assertEquals("Home", space.getTitle());
    }

    @Test
    public void iCanCreateAUrlGadget() throws Exception {

        List<Space> spaces = service.getSpacesForUnivers(univers, session);
        assertEquals(1, spaces.size());

        Space space = spaces.get(0);
        assertEquals(0, space.getGadgets().size());
        Gadget gadget = space.createGadget(new URL(
                "http://localhost:8080/gadgetDef.xml"));
        assertNotNull("gadget");

        List<Gadget> gadgets = space.getGadgets();
        assertEquals(1, gadgets.size());
        gadget = gadgets.get(0);
        assertEquals(null, gadget.getName());
        assertEquals("http://localhost:8080/gadgetDef.xml",
                gadget.getDefinitionUrl().toString());
    }

    @Test
    public void iCanChangePrefs() throws Exception {

        Space space = service.getSpacesForUnivers(univers, session).get(0);
        Gadget gadget = space.createGadget(new URL(
                "http://localhost:8080/gadgetDef.xml"));
        assertNotNull("gadget");

        Map<String, String> prefs = new HashMap<String, String>();
        prefs.put("pref", "test");
        gadget.setPreferences(prefs);
        space.save(gadget);

        String gadgetId = gadget.getId();

        DocumentModel doc = session.getDocument(new IdRef(gadgetId));
        gadget = doc.getAdapter(Gadget.class);

        assertEquals("test", gadget.getPref("pref"));

    }

}
