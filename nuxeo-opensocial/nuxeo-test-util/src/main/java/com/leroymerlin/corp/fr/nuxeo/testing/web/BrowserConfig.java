package com.leroymerlin.corp.fr.nuxeo.testing.web;

import org.openqa.selenium.WebDriver;

public interface BrowserConfig {

  WebDriver getTestDriver();

  String getHost();

  String getPort();

  String getBrowser();

}