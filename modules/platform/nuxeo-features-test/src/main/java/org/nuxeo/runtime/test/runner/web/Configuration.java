/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner.web;

import org.openqa.selenium.WebDriver;

/**
 * WebDriver test configuration that can be configured either from system
 * properties or for annotations.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Configuration {

    /**
     * The current driver
     */
    protected WebDriver driver;

    /**
     * Custom factory to create the driver
     */
    protected DriverFactory factory;

    /**
     * Port to use for parameter replacement in the home URL.
     *
     * @since 10.10
     */
    protected int port;

    /**
     * Initial URL (the one to be used by the home page)
     */
    protected String home;

    /**
     * The home page class
     */
    protected Class<? extends WebPage> homePageClass;

    public Configuration(DriverFactory factory) {
        this.factory = factory;
    }

    protected WebDriver createDriver() {
        return factory.createDriver();
    }

    protected void disposeDriver(WebDriver driver) {
        factory.disposeDriver(driver);
    }

    public BrowserFamily getBrowserFamily() {
        return factory.getBrowserFamily();
    }

    public void setFactory(DriverFactory factory) {
        resetDriver();
        this.factory = factory;
    }

    public DriverFactory getFactory() {
        return factory;
    }

    protected static final String PORT_PLACEHOLDER = "${PORT}";

    /**
     * Sets the port to use for parameter replacement in the home URL.
     *
     * @since 10.10
     */
    public void setPort(int port) {
        this.port = port;
    }

    public void setHome(String url) {
        home = url;
    }

    public String getHome() {
        String url = home;
        if (url != null && url.contains(PORT_PLACEHOLDER)) {
            url = url.replace(PORT_PLACEHOLDER, String.valueOf(port));
        }
        return url;
    }

    public void setHomePageClass(Class<? extends WebPage> homePageClass) {
        this.homePageClass = homePageClass;
    }

    public Class<? extends WebPage> getHomePageClass() {
        return homePageClass;
    }

    public void home() {
        if (getHome() != null && driver != null) {
            driver.get(getHome());
        }
    }

    public WebDriver getDriver() {
        if (driver == null) {
            driver = createDriver();
            home();
        }
        return driver;
    }


    public void resetDriver() {
        if (driver != null) {
            driver.quit();
            disposeDriver(driver);
            driver = null;
        }
    }

}
