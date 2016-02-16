/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class ITTestDrivers extends AbstractTest {

    @SuppressWarnings("hiding")
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
        ScreenshotTaker taker = new ScreenshotTaker();
        assertTrue(taker.takeScreenshot(driver, "firefox").delete());
        assertTrue(taker.dumpPageSource(driver, "firefox").delete());
        quitDriver();
        stopProxy();
    }

    @Ignore("Chrome not used in tests + chromedriver being a bit too finicky")
    @Test
    public void testGoogleChrome() throws Exception {
        initChromeDriver();
        driver.get("http://google.com");
        ScreenshotTaker taker = new ScreenshotTaker();
        assertTrue(taker.takeScreenshot(driver, "google-chrome").delete());
        assertTrue(taker.dumpPageSource(driver, "google-chrome").delete());
        quitDriver();
        stopProxy();
    }

}
