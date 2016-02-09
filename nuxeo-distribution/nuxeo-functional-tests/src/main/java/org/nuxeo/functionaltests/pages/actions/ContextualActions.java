/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href=mailto:vpasquier@nuxeo.com>Vladimir Pasquier</a>
 */
package org.nuxeo.functionaltests.pages.actions;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import com.google.common.base.Function;

/**
 * The document contextual actions
 */
public class ContextualActions extends AbstractPage {

    @FindBy(xpath = "//img[@alt=\"Lock\"]")
    public WebElement lockButton;

    @FindBy(xpath = "//img[@alt=\"Unlock\"]")
    public WebElement unlockButton;

    @FindBy(xpath = "//img[@alt=\"Follow this Document\"]")
    public WebElement followButton;

    @FindBy(xpath = "//img[@alt=\"Add to Worklist\"]")
    public WebElement addToWorklistButton;

    @FindBy(id = "nxw_permalinkAction_form:nxw_permalinkAction_link")
    public WebElement permaButton;

    public String permaBoxFocusName = "permalinkFocus";

    @FindBy(xpath = "//img[@alt=\"Export\"]")
    public WebElement exportButton;

    @FindBy(xpath = "//img[@alt=\"Add to Favorites\"]")
    public WebElement favoritesButton;

    public String xmlExportTitle = "XML Export";

    public ContextualActions(WebDriver driver) {
        super(driver);
    }

    public ContextualActions clickOnButton(WebElement button) {
        button.click();
        return asPage(ContextualActions.class);
    }

    /**
     * Clicks on "More" button, making sure we wait for content to be shown.
     *
     * @since 8.1
     */
    public ContextualActions openMore() {
        String xpath = "//ul[@id=\"nxw_documentActionsUpperButtons_dropDownMenu\"]/li";
        driver.findElement(By.xpath(xpath)).click();
        Locator.waitUntilGivenFunctionIgnoring(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return driver.findElement(By.xpath(xpath + "/ul")).isDisplayed();
            }
        }, StaleElementReferenceException.class);
        return asPage(ContextualActions.class);
    }

    /**
     * Clicks on "More" button, making sure we wait for content to be shown.
     *
     * @since 8.1
     */
    public ContextualActions closeFancyPermalinBox() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        driver.findElement(By.id("fancybox-close")).click();
        arm.end();
        return asPage(ContextualActions.class);
    }

}
