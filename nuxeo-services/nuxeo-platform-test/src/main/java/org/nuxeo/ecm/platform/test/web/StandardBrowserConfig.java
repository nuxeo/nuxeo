/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.platform.test.web;

import org.openqa.selenium.WebDriver;

public class StandardBrowserConfig implements BrowserConfig {

    // keep one driver for all tests - UGLY
    private static WebDriver driver;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (driver != null)
                    driver.quit();
            }
        });
    }

    public static StandardBrowserConfig fromAnnotation(Browser browser) {
        String host;
        String port = "8080";
        String addr = browser.value();
        int p = addr.indexOf(':');
        if (p == -1) {
            host = addr;
        } else {
            host = addr.substring(0, p);
            port = addr.substring(p+1);
        }
        return new StandardBrowserConfig(host, port, browser.type());
    }

    
    protected String host;
    protected String port;
    protected BrowserFamily family; 

    public StandardBrowserConfig() {
        this (System.getProperty("nuxeo.deploy.host",
        "localhost"),
        System.getProperty("nuxeo.deploy.port",
        "8080"),
        Enum.valueOf(BrowserFamily.class,
                System.getProperty("webdriver.browser", "HU"))
        );
    }
        
    public StandardBrowserConfig(String host, String port, BrowserFamily family) {
        this.host = host;
        this.port = port;
        this.family = family;
    }
    
    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }
    
    public BrowserFamily getFamily() {
        return family;
    }


    public WebDriver getDriver() {
        if (driver == null) {
            driver = family.getDriver();
        }
        return driver;
    }

    public String getId() {
        return family.toString();
    }

    public void resetDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

}
