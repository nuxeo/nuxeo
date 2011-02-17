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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.LoginPage;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.PageFactory;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 * 
 */
public class TestLoginPage {

    @Test
    public void testLoginPage() {
        FirefoxProfile ffprofile = new FirefoxProfile();

        // Set english as default language
        ffprofile.setPreference("general.useragent.locale", "en");
        ffprofile.setPreference("intl.accept_languages", "en");
        // flag UserAgent as Selenium tester : this is used in Nuxeo
        ffprofile.setPreference("general.useragent.extra.nuxeo",
                "Nuxeo-Selenium-Tester");

        FirefoxDriver driver = new FirefoxDriver(ffprofile);
        driver.get("http://localhost:8080/nuxeo");
        LoginPage login = PageFactory.initElements(driver, LoginPage.class);
        driver.quit();
    }

}
