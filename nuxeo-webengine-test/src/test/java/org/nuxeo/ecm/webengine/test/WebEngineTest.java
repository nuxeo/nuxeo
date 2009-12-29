package org.nuxeo.ecm.webengine.test;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.test.web.BrowserConfig;
import org.nuxeo.ecm.webengine.test.web.BrowserRunner;
import org.nuxeo.ecm.webengine.test.web.pages.WebEngineHomePage;
import org.openqa.selenium.WebDriver;

import com.google.inject.Inject;

@RunWith(BrowserRunner.class)
public class WebEngineTest {

  @Inject
  private  CoreSession session;

  @Inject
  private WebEngine we;

  @Inject
  private BrowserConfig browserConfig;

  private WebDriver webDriver;

  private WebEngineHomePage home;

  @Before
  public void login() {
    webDriver = browserConfig.getTestDriver();
    home = new WebEngineHomePage(webDriver, "localhost","11111");
    home.reload();
  }

  @Test
  public void iCanRunWebEngine() throws Exception {
    assertTrue(home.hasApplication("Admin"));
    assertFalse(home.isLogged());

    home.loginAs("Administrator", "Administrator");
    assertTrue(home.isLogged());
  }

}
