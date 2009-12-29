package org.nuxeo.ecm.webengine.test.web.pages;

import java.util.List;
import java.util.Random;

import org.nuxeo.ecm.webengine.test.web.finder.Finder;
import org.nuxeo.ecm.webengine.test.web.finder.IdFinder;
import org.nuxeo.ecm.webengine.test.web.finder.LinkFinder;
import org.nuxeo.ecm.webengine.test.web.finder.XPathFinder;
import org.nuxeo.ecm.webengine.test.web.finder.XPathManyFinder;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class AbstractPage implements WebPage {

  private static final Random random = new Random();
  private static final int TIME_SLICE = 200;
  private WebDriver driver;
  protected String host;
  protected String port;



  public void setDriver(WebDriver driver) {
    this.driver = driver;
  }

  public void enterTextWithId(String texte, String inputId) {
    WebElement user = waitForId(inputId, 4000);
    user.clear();
    user.sendKeys(texte);
  }

  public void enterTextWithXpath(String text, String xpath) {
    WebElement user = waitFor(xpath, 4000);
    user.clear();
    user.sendKeys(text);
  }

  public void click(String xpath) {
    waitFor(xpath, 4000).click();
  }

  public void click(String xpath, int millis) {
    waitFor(xpath, 2000).click();
    try {
      Thread.currentThread();
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }

  public void clickId(String id) {
    waitForId(id, 4000).click();
  }

  public AbstractPage(WebDriver driver) {
    this.driver = driver;
  }
  public AbstractPage(WebDriver driver,String host,String port) {
    this.driver = driver;
    this.host=host;
    this.port=port;
  }

  public WebDriver getDriver() {
    return driver;
  }

  public boolean containsElement(String xpath) {
    try {
      waitForOneOrMore(xpath, 5000);
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  protected <T> T doWait(int limit, Finder<T> finder) {
    T t = null;
    int nombreDeSlices = limit / TIME_SLICE;
    while (t == null && nombreDeSlices > 0) {
      try {
        t = finder.find();
      } catch (NoSuchElementException e) {
        nombreDeSlices--;
        try {
          Thread.sleep(TIME_SLICE);
        } catch (InterruptedException e1) {
        }
      }
    }
    if (t == null)
      throw new NoSuchElementException("Failed to find '" + finder.toString()
          + "'");
    return t;
  }

  protected WebElement waitFor(String xpath, int limit) {
    return doWait(limit, new XPathFinder(xpath, getDriver()));
  }

  protected List<WebElement> waitForOneOrMore(String xpath, int limit) {
    return doWait(limit, new XPathManyFinder(xpath, getDriver()));
  }

  protected WebElement waitForId(String id, int limit) {
    return doWait(limit, new IdFinder(id, getDriver()));
  }

  protected WebElement waitForLink(String linkPart, int limit) {
    return doWait(limit, new LinkFinder(linkPart, getDriver()));
  }

  public boolean containsLink(String linkPart) {
    try {
      waitForLink(linkPart, 5000);
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }


  public final void setHost(String host) {
    this.host = host;
  }

  public final void setPort(String port) {
    this.port = port;
  }

  public final String getHost() {
    return host;
  }

  public final String getPort() {
    return port;
  }

  public void visit(String uriPath) {
    getDriver().get(getFullPath(uriPath));
  }

  private String getFullPath(String uri) {
    return "http://" + host + ":" + port + "/" + uri;
  }



  public  <T extends AbstractPage> T visit(String path, Class<T> serviceClass) throws InstantiationException, IllegalAccessException {

    T clazz = serviceClass.newInstance();
    clazz.setHost(this.getHost());
    clazz.setPort(this.getPort());
    clazz.setDriver(this.getDriver());

    clazz.visit(path);

    return clazz;

 }

}
