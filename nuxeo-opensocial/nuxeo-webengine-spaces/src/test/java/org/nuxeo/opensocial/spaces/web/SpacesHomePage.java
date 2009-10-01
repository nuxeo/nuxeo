package org.nuxeo.opensocial.spaces.web;

import org.openqa.selenium.WebDriver;

import com.leroymerlin.corp.fr.nuxeo.testing.web.pages.AbstractPage;

public class SpacesHomePage extends AbstractPage {

  public SpacesHomePage() {
    super(null);
  }
  public SpacesHomePage(WebDriver driver) {
    super(driver);
  }
  public boolean hasAdminLink() {

    return containsElement("//a[contains(@href, 'admin')]");
  }
}
