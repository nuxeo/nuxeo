/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
