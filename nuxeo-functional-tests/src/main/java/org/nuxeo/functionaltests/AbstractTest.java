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
 */
package org.nuxeo.functionaltests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

/**
 * Base functions for all pages.
 */
public abstract class AbstractTest {

    private static final String FIREBUG_XPI = "firebug-1.6.2-fx.xpi";

    private static final String FIREBUG_VERSION = "1.6.2";

    protected static FirefoxDriver driver;

    protected static File tmp_firebug_xpi;

    @BeforeClass
    public static void initDriver() throws Exception {
        FirefoxProfile profile = new FirefoxProfile();

        // Set english as default language
        profile.setPreference("general.useragent.locale", "en");
        profile.setPreference("intl.accept_languages", "en");

        // flag UserAgent as Selenium tester: this is used in Nuxeo
        profile.setPreference("general.useragent.extra.nuxeo",
                "Nuxeo-Selenium-Tester");

        addFireBug(profile);

        driver = new FirefoxDriver(profile);
    }

    @AfterClass
    public static void quitDriver() {
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

}
