package org.nuxeo.ecm.spaces.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.SpaceProvider;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.VirtualUnivers;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({"org.nuxeo.ecm.spaces.api", "org.nuxeo.ecm.spaces.core"})
@LocalDeploy({"org.nuxeo.ecm.spaces.core:OSGI-INF/test1-spaces-contrib.xml"})
public class DocSpaceTest {

    @Inject private SpaceManager service;

    @Inject CoreSession session;

    private VirtualUnivers univers =
    		new VirtualUnivers("main");

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
