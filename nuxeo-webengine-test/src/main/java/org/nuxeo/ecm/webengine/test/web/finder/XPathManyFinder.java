package org.nuxeo.ecm.webengine.test.web.finder;

import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class XPathManyFinder implements Finder<List<WebElement>> {

  private final WebDriver driver;
  private final String xpath;

  public XPathManyFinder(String xpath, WebDriver driver) {
    this.xpath = xpath;
    this.driver = driver;
  }

  public List<WebElement> find() throws NoSuchElementException {
    List<WebElement> elements = driver.findElements(By.xpath(xpath));
    if (elements.size() == 0)
      throw new org.openqa.selenium.NoSuchElementException(
          "No elements for xpath '" + xpath + "'");
    return elements;
  }

  @Override
  public String toString() {
    return xpath.toString();
  }

}
