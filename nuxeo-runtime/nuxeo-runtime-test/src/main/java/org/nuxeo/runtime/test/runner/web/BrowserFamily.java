/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.openqa.selenium.Speed;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public enum BrowserFamily {

    FIREFOX, IE, CHROME, HTML_UNIT, HTML_UNIT_JS;

    public DriverFactory getDriverFactory() {
        switch (this) {
        case FIREFOX:
            return new FirefoxDriverFactory();
        case IE:
            return new IEDriverFactory();
        case CHROME:
            return new ChromeDriverFactory();
        case HTML_UNIT_JS:
            return new HtmlUnitJsDriverFactory();
        default:
            return new HtmlUnitDriverFactory();
        }
    }

    class FirefoxDriverFactory implements DriverFactory {
        @Override
        public WebDriver createDriver() {
            FirefoxDriver ff = new FirefoxDriver();
            ff.manage().setSpeed(Speed.FAST);
            return ff;
        }
        @Override
        public void disposeDriver(WebDriver driver) {
        }
        @Override
        public BrowserFamily getBrowserFamily() {
            return BrowserFamily.this;
        }
    }

    class ChromeDriverFactory implements DriverFactory {
        @Override
        public WebDriver createDriver() {
            ChromeDriver ff = new ChromeDriver();
            ff.manage().setSpeed(Speed.FAST);
            return ff;
        }
        @Override
        public void disposeDriver(WebDriver driver) {
        }
        @Override
        public BrowserFamily getBrowserFamily() {
            return BrowserFamily.this;
        }
    }

    class IEDriverFactory implements DriverFactory {
        @Override
        public WebDriver createDriver() {
            InternetExplorerDriver driver = new InternetExplorerDriver();
            driver.manage().setSpeed(Speed.FAST);
            return driver;
        }
        @Override
        public void disposeDriver(WebDriver driver) {
        }
        @Override
        public BrowserFamily getBrowserFamily() {
            return BrowserFamily.this;
        }
    }

    class HtmlUnitDriverFactory implements DriverFactory {
        @Override
        public WebDriver createDriver() {
            return new HtmlUnitDriver();
        }
        @Override
        public void disposeDriver(WebDriver driver) {
        }
        @Override
        public BrowserFamily getBrowserFamily() {
            return BrowserFamily.this;
        }
    }

    class HtmlUnitJsDriverFactory implements DriverFactory {
        @Override
        public WebDriver createDriver() {
            HtmlUnitDriver driver = new HtmlUnitDriver();
            driver.setJavascriptEnabled(true);
            return driver;
        }
        @Override
        public void disposeDriver(WebDriver driver) {
        }
        @Override
        public BrowserFamily getBrowserFamily() {
            return BrowserFamily.this;
        }
    }

//    private WebDriver _old_makeFirefoxDriver() {
//        String Xport = System.getProperty("nuxeo.xvfb.id", ":0");
//        File firefoxPath = new File(System.getProperty("firefox.path",
//                "/usr/bin/firefox"));
//        FirefoxBinary firefox = new FirefoxBinary(firefoxPath);
//        firefox.setEnvironmentProperty("DISPLAY", Xport);
//        WebDriver driver = new FirefoxDriver(firefox, null);
//        //driver.setVisible(false);
//        driver.manage().setSpeed(Speed.FAST);
//        return driver;
//    }

}
