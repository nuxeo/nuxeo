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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.openqa.selenium.Speed;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.PageFactory;

/**
 * Base functions for all pages.
 */
public abstract class AbstractTest {

    public static final String NUXEO_URL = "http://localhost:8080/nuxeo";

    private static final String FIREBUG_XPI = "firebug-1.6.2-fx.xpi";

    private static final String FIREBUG_VERSION = "1.6.2";

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
        // Set speed between user interaction: keyboard and mouse
        // Fast: 0, MEDIUM: 0.5s SLOW: 1s
        driver.manage().setSpeed(Speed.FAST);
    }

    @AfterClass
    public static void quitDriver() throws InterruptedException {
        // Temporary code to take snapshots of the last page
        // TODO: snapshots only test on failure, prefix using the test name
        driver.saveScreenshot(new File("/tmp/screenshot-lastpage.png"));
        Thread.currentThread().sleep(1000);
        driver.saveScreenshot(new File("/tmp/screenshot-lastpage2.png"));

        if (driver != null) {
            driver.close();
            driver = null;
        }
        removeFireBug();
    }

    protected static void addFireBug(FirefoxProfile profile) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL xpi_url = cl.getResource(FIREBUG_XPI);
        File xpi;
        if (xpi_url.getProtocol().equals("file")) {
            xpi = new File(xpi_url.getPath());
        } else {
            // copy to a file
            InputStream firebug = cl.getResourceAsStream(FIREBUG_XPI);
            if (firebug == null) {
                throw new RuntimeException(FIREBUG_XPI + " resource not found");
            }
            File tmp = File.createTempFile("nxfirebug", null);
            tmp.delete();
            tmp.mkdir();
            xpi = new File(tmp, FIREBUG_XPI);
            FileOutputStream out = new FileOutputStream(xpi);
            IOUtils.copy(firebug, out);
            firebug.close();
            out.close();
            tmp_firebug_xpi = xpi;
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
        return PageFactory.initElements(driver, pageClassToProxy);
    }

    public LoginPage getLoginPage() {
        return get(NUXEO_URL, LoginPage.class);
    }

    /**
     * Login as Administrator
     *
     * @return the Document base page (by default returned by nuxeo dm)
     */
    public DocumentBasePage login() {
        return login("Administrator", "Administrator");
    }

    public DocumentBasePage login(String username, String password) {
        DocumentBasePage documentBasePage = getLoginPage().login(username, password, DocumentBasePage.class);
        documentBasePage.checkUserConnected(username);
        return documentBasePage;
    }

}
