package org.nuxeo.ecm.webengine.test.web.finder;

import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class XPathFinder implements Finder<WebElement> {

  private final String xpath;
  private final WebDriver webDriver;

  public XPathFinder(String xpath, WebDriver webDriver) {
    this.xpath = xpath;
    this.webDriver = webDriver;
  }

  public WebElement find() throws NoSuchElementException {
    return webDriver.findElement(By.xpath(xpath));
  }

  @Override
  public String toString() {
    return xpath.toString();
  }

}
