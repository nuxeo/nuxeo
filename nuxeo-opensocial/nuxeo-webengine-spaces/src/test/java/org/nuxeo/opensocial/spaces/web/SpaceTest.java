package org.nuxeo.opensocial.spaces.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.webengine.WebEngine;
import org.openqa.selenium.WebDriver;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.URLChecker;
import com.leroymerlin.corp.fr.nuxeo.testing.web.BrowserConfig;
import com.leroymerlin.corp.fr.nuxeo.testing.web.BrowserRunner;
import com.leroymerlin.corp.fr.nuxeo.testing.web.pages.WebEngineHomePage;

@RunWith(BrowserRunner.class)
public class SpaceTest {

    @Inject
    private BrowserConfig browserConfig;

    private WebDriver webDriver;


    @Inject
    public SpaceTest(TestRuntimeHarness harness, WebEngine we) throws Exception {
        harness.deployBundle("org.nuxeo.ecm.spaces.api");
        harness.deployBundle("org.nuxeo.ecm.spaces.core");
        harness.deployBundle("org.nuxeo.opensocial.webengines.spaces");
    }

    private WebEngineHomePage home;

    @Before
    public void login() {
        webDriver = browserConfig.getTestDriver();
        home = new WebEngineHomePage(webDriver, "localhost", "11111");
        home.reload();
        if (!home.isLogged()) {
            home.loginAs("Administrator", "Administrator");
        }
    }

    @Test
    public void spacesApplicationAppearsWhenIDeployThisBundle()
            throws Exception {

        assertTrue(home.hasApplication("spaces"));
    }

    // @Test
    public void spacesAdminApplicationIsAccessible() throws Exception {
        SpacesHomePage page = home.goToApplication("spaces",
                SpacesHomePage.class);
        assertNotNull(page);
        assertTrue(page.hasAdminLink());

    }

    // @Test
    public void iCanServeTheGadgetDefinition() throws Exception {
        URLChecker checker = new URLChecker();
        URL url = new URL("http://localhost:11111/spaces");
        assertTrue(checker.checkUrlContentAndStatusOK(url));
        // assertTrue(checker.getContentOf(url)
        // .contains("Hello World!"));
    }

}
