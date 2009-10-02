package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.junit.Assert;
import org.junit.runners.Parameterized;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * A specialized runner for JQUnit test pages. This runner is used to embed
 * testing of JQUnit test pages within Unit and similar frameworks (eg. maven's
 * Surefire plugin). To use it, one needs to:
 * <ol>
 * <li>write tests following jqUnit conventions and put them in the
 * <code>src/test/resources</code> directory so that they can be found within
 * <code>target/test-classes</code>,</li>
 * <li>create a JUNit 4 test case class properly annotated for the jqUnit page.
 * the following code sample is a skeleton than can be properly populated.</li>
 * </ol>
 * 
 * <pre>
 * &#064;RunWith(JQUnitRunner.class)
 * &#064;JQUnitRunner.Page(&quot;test-etatflash&quot;)
 * public class EtatFlashTest {
 *   private JQTest jqTest;
 * 
 *   &#064;Parameters
 *   public static Collection&lt;JQTest[]&gt; jqtests() {
 *     return JQUnitRunner.load(EtatFlashTest.class);
 *   }
 * 
 *   public EtatFlashTest(JQTest test) {
 *     this.jqTest = test;
 *   }
 * 
 *   &#064;Test
 *   public void test() {
 *     jqTest.assertPass();
 *   }
 * }
 * </pre>
 * 
 * @author nono
 * 
 */
public class JQUnitRunner extends Parameterized {

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Page {

    String value();

  }

  public JQUnitRunner(Class<?> clazz) throws Throwable {
    super(clazz);
  }

  public static class JQTest {

    private final HtmlElement element;
    private AssertionFailedError exception;

    public JQTest(HtmlElement element) {
      this.element = element;
      if (element.getAttribute("class") == null
          || element.getAttribute("class")
              .contains("fail"))
        this.exception = new AssertionFailedError(
            ((HtmlElement) element.getByXPath("//li[@class='fail']")
                .get(0)).asText());
    }

    public void assertPass() {
      if (this.exception != null)
        throw this.exception;
    }

    @Override
    public String toString() {
      return element.asText();
    }

  }

  private static final String basedir = System.getProperty("basedir", ".");

  public static List<JQTest[]> load(Class<?> clazz) {
    WebClient client = new WebClient(BrowserVersion.INTERNET_EXPLORER_6);
    client.setJavaScriptEnabled(true);
    List<JQTest[]> jqTests = new ArrayList<JQTest[]>();
    String testPagePath = null;
    Page page = clazz.getAnnotation(Page.class);
    if (page != null) {
      testPagePath = page.value();
    }
    File f = new File(basedir);
    f = new File(f, "/target/test-classes/" + testPagePath + ".html");
    Assert.assertTrue("File " + f.getAbsolutePath() + " does not exists",
        f.exists());
    String uri = "file://" + f.getAbsolutePath();
    HtmlPage page2;
    try {
      page2 = client.getPage(uri);
    } catch (Exception e) {
      e.printStackTrace();
      return jqTests;
    }
    List<?> byXPath = page2.getByXPath("//ol[@id='tests']/li");
    for (Object element : byXPath) {
      JQTest jqTest = new JQTest((HtmlElement) element);
      jqTests.add(new JQTest[] { jqTest });
    }
    return jqTests;
  }
}
