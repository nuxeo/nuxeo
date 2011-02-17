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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

/**
 * Base functions for all pages.
 */
public abstract class AbstractTest {

    protected static FirefoxDriver driver;

    @BeforeClass
    public static void initDriver() {
        FirefoxProfile profile = new FirefoxProfile();

        // Set english as default language
        profile.setPreference("general.useragent.locale", "en");
        profile.setPreference("intl.accept_languages", "en");

        // flag UserAgent as Selenium tester: this is used in Nuxeo
        profile.setPreference("general.useragent.extra.nuxeo",
                "Nuxeo-Selenium-Tester");

        driver = new FirefoxDriver(profile);
    }

    @AfterClass
    public static void quitDriver() {
        driver.close();
        driver = null;
    }

}
