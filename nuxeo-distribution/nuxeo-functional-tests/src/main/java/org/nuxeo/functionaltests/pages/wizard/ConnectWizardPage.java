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
 *     Thierry Delprat
 */

package org.nuxeo.functionaltests.pages.wizard;

import static org.nuxeo.functionaltests.pages.wizard.IFrameHelper.CONNECT_IFRAME_URL_PATTERN;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Function;

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

    public ConnectWizardPage getLink(String text) {
        ConnectWizardPage wpage = getLink(ConnectWizardPage.class, text);
        if (!driver.getCurrentUrl().contains(CONNECT_IFRAME_URL_PATTERN)) {
            System.out.println("Oups, we are out of the frame !!!");
            driver.switchTo().frame("connectForm");
            return asPage(ConnectWizardPage.class);
        }
        return wpage;
    }

    public ConnectWizardPage submitWithError() {
        return next(ConnectWizardPage.class, new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return findElementWithTimeout(By.cssSelector(".warning.message li"), 5 * 1000) != null;
            }
        });
    }

    public <T extends AbstractPage> T getLink(Class<T> wizardPageClass,
            String text) {
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
