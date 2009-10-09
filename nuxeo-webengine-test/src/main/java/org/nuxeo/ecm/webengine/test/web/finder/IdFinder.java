package org.nuxeo.ecm.webengine.test.web.finder;

import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class IdFinder implements Finder<WebElement> {

  private final String id;
  private final WebDriver driver;

  public IdFinder(String id, WebDriver driver) {
    this.id = id;
    this.driver = driver;
  }

  public WebElement find() throws NoSuchElementException {
    return driver.findElement(By.id(id));
  }

  @Override
  public String toString() {
    return id;
  }

}
