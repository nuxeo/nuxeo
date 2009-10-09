package org.nuxeo.ecm.webengine.test.web;

import org.openqa.selenium.WebDriver;

public interface BrowserConfig {

  WebDriver getTestDriver();

  String getHost();

  String getPort();

  String getBrowser();

}