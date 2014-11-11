/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Benoit Delbosc
 */
package org.nuxeo.functionaltests;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Clock;
import org.openqa.selenium.support.ui.SystemClock;

/**
 * Base functions for all pages.
 */
public abstract class AbstractTest {

    public static final String NUXEO_URL = "http://localhost:8080/nuxeo";

    private static final String FIREBUG_XPI = "firebug-1.6.2-fx.xpi";

    private static final String FIREBUG_VERSION = "1.6.2";

    private static final String FIREBUG_M2 = "firebug/firebug/1.6.2-fx";

    private static final String M2_REPO = "/.m2/repository/";

    private static final int LOAD_TIMEOUT_SECONDS = 5;

    private static final int AJAX_TIMEOUT_SECONDS = 5;

    protected static FirefoxDriver driver;

    protected static File tmp_firebug_xpi;

    @BeforeClass
    public static void initDriver() throws Exception {
        FirefoxProfile profile = new FirefoxProfile();

        // Set English as default language
        profile.setPreference("general.useragent.locale", "en");
        profile.setPreference("intl.accept_languages", "en");

        // flag UserAgent as Selenium tester: this is used in Nuxeo
        profile.setPreference("general.useragent.extra.nuxeo",
                "Nuxeo-Selenium-Tester");

        addFireBug(profile);

        driver = new FirefoxDriver(profile);
    }

    @AfterClass
    public static void quitDriver() throws InterruptedException {
        // Temporary code to take snapshots of the last page
        // TODO: snapshots only test on failure, prefix using the test name
        Thread.sleep(250);
        driver.saveScreenshot(new File("/tmp/screenshot-lastpage.png"));
        Thread.sleep(250);
        driver.saveScreenshot(new File("/tmp/screenshot-lastpage2.png"));

        if (driver != null) {
            driver.close();
            driver = null;
        }
        removeFireBug();
    }

    /**
     * Introspects the classpath and returns the list of files in it.
     *
     * @return
     * @throws Exception
     */
    protected static List<String> getClassLoaderFiles() throws Exception {
        ClassLoader cl = AbstractTest.class.getClassLoader();
        URL[] urls = null;
        if (cl instanceof URLClassLoader) {
            urls = ((URLClassLoader) cl).getURLs();
        } else if (cl.getClass().getName().equals(
                "org.apache.tools.ant.AntClassLoader")) {
            Method method = cl.getClass().getMethod("getClasspath");
            String cp = (String) method.invoke(cl);
            String[] paths = cp.split(File.pathSeparator);
            urls = new URL[paths.length];
            for (int i = 0; i < paths.length; i++) {
                urls[i] = new URL("file:" + paths[i]);
            }
        } else {
            System.err.println("Unknow classloader type: "
                    + cl.getClass().getName());
            return null;
        }
        // special case for maven surefire with useManifestOnlyJar
        if (urls.length == 1) {
            try {
                URI uri = urls[0].toURI();
                if (uri.getScheme().equals("file")
                        && uri.getPath().contains("surefirebooter")) {
                    JarFile jar = new JarFile(new File(uri));
                    try {
                        String cp = jar.getManifest().getMainAttributes().getValue(
                                Attributes.Name.CLASS_PATH);
                        if (cp != null) {
                            String[] cpe = cp.split(" ");
                            URL[] newUrls = new URL[cpe.length];
                            for (int i = 0; i < cpe.length; i++) {
                                // Don't need to add 'file:' with maven
                                // surefire >= 2.4.2
                                String newUrl = cpe[i].startsWith("file:") ? cpe[i]
                                        : "file:" + cpe[i];
                                newUrls[i] = new URL(newUrl);
                            }
                            urls = newUrls;
                        }
                    } finally {
                        jar.close();
                    }
                }
            } catch (Exception e) {
                // skip
            }
        }
        // turn into files
        List<String> files = new ArrayList<String>(urls.length);
        for (URL url : urls) {
            files.add(url.toURI().getPath());
        }
        return files;
    }

    protected static void addFireBug(FirefoxProfile profile) throws Exception {
        File xpi = null;
        List<String> clf = getClassLoaderFiles();
        for (String f : clf) {
            if (f.endsWith("/" + FIREBUG_XPI)) {
                xpi = new File(f);
            }
        }
        if (xpi == null) {
            // try to guess the location in the M2 repo
            for (String f : clf) {
                if (f.contains(M2_REPO)) {
                    String m2 = f.substring(0, f.indexOf(M2_REPO)
                            + M2_REPO.length());
                    xpi = new File(m2 + FIREBUG_M2 + "/" + FIREBUG_XPI);
                    break;
                }
            }
        }
        if (xpi == null || !xpi.exists()) {
            throw new RuntimeException(FIREBUG_XPI
                    + " not found in classloader or local M2 repository");
        }
        profile.addExtension(xpi);
        // avoid "first run" page
        profile.setPreference("extensions.firebug.currentVersion",
                FIREBUG_VERSION);
    }

    protected static void removeFireBug() {
        if (tmp_firebug_xpi != null) {
            tmp_firebug_xpi.delete();
            tmp_firebug_xpi.getParentFile().delete();
        }
    }

    public static <T> T get(String url, Class<T> pageClassToProxy) {
        driver.get(url);
        return asPage(pageClassToProxy);
    }

    public static <T> T asPage(Class<T> pageClassToProxy) {
        T page = instantiatePage(driver, pageClassToProxy);
        PageFactory.initElements(new VariableElementLocatorFactory(driver,
                AJAX_TIMEOUT_SECONDS), page);
        // check all required WebElements on the page and wait for their
        // loading
        List<String> fieldNames = new ArrayList<String>();
        List<WrapsElement> elements = new ArrayList<WrapsElement>();
        for (Field field : pageClassToProxy.getDeclaredFields()) {
            if (field.getAnnotation(Required.class) != null) {
                try {
                    field.setAccessible(true);
                    fieldNames.add(field.getName());
                    elements.add((WrapsElement) field.get(page));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Clock clock = new SystemClock();
        long end = clock.laterBy(SECONDS.toMillis(LOAD_TIMEOUT_SECONDS));
        String notLoaded = null;
        while (clock.isNowBefore(end)) {
            notLoaded = anyElementNotLoaded(elements, fieldNames);
            if (notLoaded == null) {
                return page;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        throw new NoSuchElementException("Timeout loading page "
                + pageClassToProxy.getSimpleName() + " missing element "
                + notLoaded);
    }

    protected static String anyElementNotLoaded(List<WrapsElement> proxies,
            List<String> fieldNames) {
        for (int i = 0; i < proxies.size(); i++) {
            WrapsElement proxy = proxies.get(i);
            try {
                // method implemented in LocatingElementHandler
                proxy.getWrappedElement();
            } catch (NoSuchElementException e) {
                return fieldNames.get(i);
            }
        }
        return null;
    }

    // private in PageFactory...
    protected static <T> T instantiatePage(WebDriver driver,
            Class<T> pageClassToProxy) {
        try {
            try {
                Constructor<T> constructor = pageClassToProxy.getConstructor(WebDriver.class);
                return constructor.newInstance(driver);
            } catch (NoSuchMethodException e) {
                return pageClassToProxy.newInstance();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by, int timeout)
            throws NoSuchElementException {
        Clock clock = new SystemClock();
        long end = clock.laterBy(timeout);
        NoSuchElementException lastException = null;
        while (clock.isNowBefore(end)) {
            try {
                WebElement element = driver.findElement(by);
                if (element != null) {
                    return element;
                }
            } catch (NoSuchElementException e) {
                lastException = e;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        throw new NoSuchElementException(String.format(
                "Couldn't find element '%s' after timeout", by), lastException);
    }

    /**
     * Finds the first {@link WebElement} using the given method, with a
     * timeout.
     *
     * @param by the locating mechanism
     * @param timeout the timeout in milliseconds
     * @return the first matching element on the current page, if found
     * @throws NoSuchElementException when not found
     */
    public static WebElement findElementWithTimeout(By by)
            throws NoSuchElementException {
        return findElementWithTimeout(by, LOAD_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     * @param timeout the timeout in milliseconds
     */
    public static void waitUntilEnabled(final WebElement element, int timeout)
            throws NotFoundException {
        Clock clock = new SystemClock();
        long end = clock.laterBy(timeout);
        while (clock.isNowBefore(end)) {
            if (element.isEnabled()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        throw new NotFoundException("Element not enabled after timeout: "
                + element);
    }

    /**
     * Waits until an element is enabled, with a timeout.
     *
     * @param element the element
     */
    public static void waitUntilEnabled(WebElement element)
            throws NotFoundException {
        waitUntilEnabled(element, AJAX_TIMEOUT_SECONDS * 1000);
    }

    public LoginPage getLoginPage() {
        return get(NUXEO_URL, LoginPage.class);
    }

    /**
     * Login as Administrator
     *
     * @return the Document base page (by default returned by nuxeo dm)
     * @throws UserNotConnectedException
     */
    public DocumentBasePage login() throws UserNotConnectedException {
        return login("Administrator", "Administrator");
    }

    public DocumentBasePage login(String username, String password)
            throws UserNotConnectedException {
        DocumentBasePage documentBasePage = getLoginPage().login(username,
                password, DocumentBasePage.class);
        documentBasePage.checkUserConnected(username);
        return documentBasePage;
    }

    /**
     * Login using an invalid credential.
     *
     * @param username
     * @param password
     * @return
     */
    public LoginPage loginInvalid(String username, String password) {
        LoginPage loginPage = getLoginPage().login(username, password,
                LoginPage.class);
        return loginPage;
    }

}
