package org.nuxeo.ecm.webengine.test.web.pages;

import org.openqa.selenium.WebDriver;

public interface WebPage {
  void setPort(String port);
  void setHost(String host);
  void setDriver(WebDriver driver);
}
