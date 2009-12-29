package org.nuxeo.ecm.webengine.test.web.finder;

import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LinkFinder implements Finder<WebElement> {

  private final String linkPart;
  private final WebDriver driver;

  public LinkFinder(String linkPart, WebDriver driver) {
    this.linkPart = linkPart;
    this.driver = driver;

  }

  public WebElement find() throws NoSuchElementException {
    return driver.findElement(By.partialLinkText(this.linkPart));
  }



  @Override
  public String toString() {
    return linkPart;
  }

}
