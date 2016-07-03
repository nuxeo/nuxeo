/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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
 *     Antoine Taillefer
 *     Anahide Tchertchian
 *     Guillaume Renard
 *     Mathieu Guillaume
 *     Julien Carsique
 */
package org.nuxeo.functionaltests.drivers;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

/**
 * Driver provider for firefox.
 *
 * @since 8.3
 */
public class RemoteFirefoxDriverProvider implements DriverProvider {

    public static final String WEBDRIVER_URL = System.getProperty("webdriverURL", "http://localhost:4444/wd/hub");

    protected RemoteWebDriver driver;

    @Override
    public RemoteWebDriver init(DesiredCapabilities dc) throws Exception {

        FirefoxProfile profile = FirefoxDriverProvider.getProfile();

        JavaScriptError.addExtension(profile);

        dc.setCapability(FirefoxDriver.PROFILE, profile);
        driver = new RemoteWebDriver(new java.net.URL(WEBDRIVER_URL), dc);
        driver.setFileDetector(new LocalFileDetector());
        return driver;
    }

    @Override
    public RemoteWebDriver get() {
        return driver;
    }

    @Override
    public void quit() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

}
