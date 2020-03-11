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

import java.io.IOException;
import java.io.InputStream;
import java.util.function.UnaryOperator;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.attachment.AttachmentHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
        case HTML_UNIT:
            return new HtmlUnitDriverFactory();
        case HTML_UNIT_JS:
        default:
            return new HtmlUnitJsDriverFactory();
        }
    }

    class FirefoxDriverFactory implements DriverFactory {
        @Override
        public WebDriver createDriver() {
            FirefoxProfile profile = new FirefoxProfile();
            String dir = "target/downloads";
            profile.setPreference("browser.download.defaultFolder", dir);
            profile.setPreference("browser.download.downloadDir", dir);
            profile.setPreference("browser.download.lastDir", dir);
            profile.setPreference("browser.download.dir", dir);
            profile.setPreference("browser.download.useDownloadDir", "true");
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "text/json");
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download manager.useWindow", "false");
            DesiredCapabilities dc = DesiredCapabilities.firefox();
            dc.setCapability(FirefoxDriver.PROFILE, profile);
            return new FirefoxDriver(dc);
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
            // ff.manage().setSpeed(Speed.FAST);
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
            // driver.manage().setSpeed(Speed.FAST);
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

        Attachment attachment;

        final AttachmentHandler attachmentHandler = new AttachmentHandler() {

            @Override
            public void handleAttachment(Page page) {
                attachment = new Attachment() {
                    @Override
                    public String getFilename() {
                        String filename = getSuggestedFilename();
                        if (filename != null) {
                            return filename;
                        }
                        String path = page.getUrl().getPath();
                        return path.substring(path.lastIndexOf('/') + 1);
                    }

                    @Override
                    public InputStream getContent() throws IOException {
                        return page.getWebResponse().getContentAsStream();
                    }

                    public String getSuggestedFilename() {
                        final WebResponse response = page.getWebResponse();
                        final String disp = response.getResponseHeaderValue("Content-Disposition");
                        int start = disp.indexOf("filename=");
                        if (start == -1) {
                            return null;
                        }
                        start += "filename=".length();
                        int end = disp.indexOf(';', start);
                        if (end == -1) {
                            end = disp.length();
                        }
                        if (disp.charAt(start) == '"' && disp.charAt(end - 1) == '"') {
                            start++;
                            end--;
                        }
                        return disp.substring(start, end);
                    }
                };
            }
        };

        UnaryOperator<WebClient> customizer() {
            return client -> {
                client.setAttachmentHandler(attachmentHandler);
                return client;
            };
        }

        boolean jsEnabled() {
            return false;
        }

        class CustomizableHtmlUnitDriver extends HtmlUnitDriver implements TakesAttachment {

            @Override
            public WebClient getWebClient() {
                return super.getWebClient();
            }

            protected CustomizableHtmlUnitDriver() {
                super(jsEnabled());
            }

            @Override
            protected WebClient modifyWebClient(WebClient client) {
                return customizer().apply(client);
            }

            @Override
            public Attachment getAttachment() throws WebDriverException {
                return attachment;
            }

        }

        @Override
        public WebDriver createDriver() {
            return new CustomizableHtmlUnitDriver();
        }

        @Override
        public void disposeDriver(WebDriver driver) {
        }

        @Override
        public BrowserFamily getBrowserFamily() {
            return BrowserFamily.this;
        }

        @Override
        public void waitForAjax(WebDriver driver) {
            return;
        }

    }

    class HtmlUnitJsDriverFactory extends HtmlUnitDriverFactory {
        @Override
        boolean jsEnabled() {
            return true;
        }

        @Override
        UnaryOperator<WebClient> customizer() {
            UnaryOperator<WebClient> setJavascript = client -> {
                client.getOptions().setThrowExceptionOnScriptError(false);
                client.setAjaxController(new NicelyResynchronizingAjaxController());
                return client;
            };
            return client -> setJavascript.apply(super.customizer().apply(client));
        }

        @Override
        public void waitForAjax(WebDriver driver) {
            ((CustomizableHtmlUnitDriver) driver).getWebClient().waitForBackgroundJavaScript(30000);
        }

    }

}
