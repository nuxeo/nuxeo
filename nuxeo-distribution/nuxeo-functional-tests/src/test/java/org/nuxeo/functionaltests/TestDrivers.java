/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * @since 5.7
 */
public class TestDrivers extends AbstractTest {

    @Rule
    public LogTestWatchman watchman = new LogTestWatchman(driver, NUXEO_URL) {

        @Override
        protected void logOnServer(String message) {
            // Don't call Nuxeo systemLog API
        }

    };

    @BeforeClass
    public static void initDriver() throws Exception {
        // Do not init driver
    }

    @Test
    public void testFireFox() throws Exception {
        initFirefoxDriver();
        watchman.setDriver(driver);
        watchman.setServerURL(NUXEO_URL);
        driver.get("http://google.com");
        assertTrue(watchman.takeScreenshot("firefox").delete());
        assertTrue(watchman.dumpPageSource("firefox").delete());
        quitDriver();
        removeFireBug();
        stopProxy();
    }

    @Ignore("Chrome not used in tests + chromedriver being a bit too finicky")
    @Test
    public void testGoogleChrome() throws Exception {
        initChromeDriver();
        driver.get("http://google.com");
        assertTrue(watchman.takeScreenshot("google-chrome").delete());
        assertTrue(watchman.dumpPageSource("google-chrome").delete());
        quitDriver();
        stopProxy();
    }

}
