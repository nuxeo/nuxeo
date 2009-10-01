package org.nuxeo.opensocial.webengine.gadgets;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebEngine;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.URLChecker;

@RunWith(NuxeoRunner.class)
public class GadgetAppTest {

    @Inject
    private CoreSession session;

    @Inject
    public GadgetAppTest(TestRuntimeHarness harness, WebEngine we)
            throws Exception {
        harness.deployBundle("org.nuxeo.opensocial.gadgets.core");
        harness.deployBundle("org.nuxeo.opensocial.webengine.gadgets");
        harness.deployBundle("org.nuxeo.opensocial.webengine.gadgets.test");
        assertNotNull(we);

    }

    @Test
    public void iCanServeTheGadgetDefinition() throws Exception {
        URLChecker checker = new URLChecker();
        URL url = new URL(
                "http://localhost:11111/gadgets/manager/hello/hello/hello.xml");
        assertTrue(checker.checkUrlContentAndStatusOK(url));

    }

    @Test
    public void iCanServeGwtResources() throws Exception {
        URLChecker checker = new URLChecker();
        URL url = new URL(
                "http://localhost:11111/gadgets/resources/container.css");
        assertTrue(checker.checkUrlContentAndStatusOK(url));
        assertTrue(checker.getContentOf(url).contains("GWT Container"));

        url = new URL(
                "http://localhost:11111/gadgets/resources/org.nuxeo.opensocial.container.ContainerEntryPoint.nocache.js");
        assertTrue(checker.checkUrlContentAndStatusOK(url));

    }

    @Test
    public void iCanServeGwtExtResources() throws Exception {
        URLChecker checker = new URLChecker();
        URL url = new URL(
                "http://localhost:11111/gadgets/resources/js/ext/adapter/ext/ext-base.js");
        assertTrue(checker.checkUrlContentAndStatusOK(url));

    }

    @Test
    public void nonExistingGwtResources() throws Exception {
        URLChecker checker = new URLChecker();
        try {
            URL url = new URL(
                    "http://localhost:11111/gadgets/resources/nonExistingRessource");
            checker.checkUrlContentAndStatusOK(url);
            fail("should have thrown an exception");
        } catch (IOException e) {
            assertTrue(true);
        }

    }

}
