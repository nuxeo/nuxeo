/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */

package org.nuxeo.functionaltests.pages.wizard;

import static org.nuxeo.functionaltests.pages.wizard.IFrameHelper.CONNECT_IFRAME_URL_PATTERN;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ConnectWizardPage extends AbstractWizardPage {

    protected static final String REGISTER_DIV_LOCATOR = "//div[@class=\"CSS_CLASS\"]";

    public ConnectWizardPage(WebDriver driver) {
        super(driver);
        IFrameHelper.focusOnConnectFrame(driver);
    }

    @Override
    public String getTitle() {
        WebElement title = findElementWithTimeout(By.xpath("//h3"));
        return title.getText().trim();
    }

    public void exitIframe() {
        // XXX
    }

    public String getErrorMessage() {
        WebElement el = findElementWithTimeout(By.cssSelector("div.ui.warning.message"));
        if (el == null) {
            return null;
        }
        return el.getText().trim();
    }

    @Override
    protected String getNextButtonLocator() {
        return REGISTER_DIV_LOCATOR.replace("CSS_CLASS", "ui blue submit button btnNext");
    }

    @Override
    protected String getPreviousButtonLocator() {
        return REGISTER_DIV_LOCATOR.replace("CSS_CLASS", "ui blue submit button btnPrev");
    }

    public ConnectWizardPage openLink(String text) {
        ConnectWizardPage wpage = openLink(ConnectWizardPage.class, text);
        if (!driver.getCurrentUrl().contains(CONNECT_IFRAME_URL_PATTERN)) {
            System.out.println("Oups, we are out of the frame !!!");
            driver.switchTo().frame("connectForm");
            return asPage(ConnectWizardPage.class);
        }
        return wpage;
    }

    public ConnectWizardPage submitWithError() {
        return next(ConnectWizardPage.class,
                input -> findElementWithTimeout(By.cssSelector(".warning.message li"), 5 * 1000) != null);
    }

    public <T extends AbstractPage> T openLink(Class<T> wizardPageClass, String text) {
        WebElement link = findElementWithTimeout(By.linkText(text));
        if (link == null) {
            return null;
        }
        waitUntilEnabled(link);
        link.click();
        return asPage(wizardPageClass);
    }

    public String getTitle2() {
        WebElement title2 = findElementWithTimeout(By.xpath("//h2"));
        if (title2 == null) {
            return null;
        }
        return title2.getText();
    }

}
